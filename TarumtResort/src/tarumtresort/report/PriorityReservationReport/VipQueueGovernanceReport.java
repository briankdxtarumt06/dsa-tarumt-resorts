package tarumtresort.report.PriorityReservationReport;

import java.time.LocalDateTime;
import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;
import tarumtresort.entity.PriorityReservation;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.Staff;
import tarumtresort.entity.enums.PriorityLevel;
import tarumtresort.report.ReportChart;
import tarumtresort.utility.Ansi;

// Author: Lee Boon Yew
/**
 * VIP QUEUE AND OVERRIDE GOVERNANCE REPORT
 *
 * Management question: who is bypassing the loyalty tier rules, and at whose
 * expense?
 *
 * A staff override is the one place in this system where a person can hand
 * edit the service order. This report is the audit trail for that power. Each
 * row is one queued reservation in service order, and the GUESTS DISPLACED
 * column turns a placement into a cost counted in real guests - the number of
 * people who were already waiting and got pushed behind this record.
 *
 * That converts "Amir approved 3 overrides" into "Amir's overrides pushed 27
 * guests down the queue", which is the number management can actually act on.
 *
 * Dependencies: PriorityReservation + Reservation + Staff.
 * Filters: registration date range, minimum priority level, override scope.
 */
public class VipQueueGovernanceReport {

    private static final int REASON_WIDTH = 18;

    private final ListInterface<PriorityReservation> priorityList;
    private final ListInterface<Reservation> reservationList;
    private final ListInterface<Staff> staffList;

    public VipQueueGovernanceReport(ListInterface<PriorityReservation> priorityList,
            ListInterface<Reservation> reservationList,
            ListInterface<Staff> staffList) {
        this.priorityList = priorityList == null ? new DoublyLinkedList<>() : priorityList;
        this.reservationList = reservationList == null ? new DoublyLinkedList<>() : reservationList;
        this.staffList = staffList == null ? new DoublyLinkedList<>() : staffList;
    }

    /**
     * @param minLevel      rank threshold, e.g. GOLD means "GOLD and above". null = no threshold
     * @param overrideScope 0 = all records, 1 = overridden only, 2 = non-overridden only
     */
    public Result generate(LocalDateTime from, LocalDateTime to,
            PriorityLevel minLevel, int overrideScope) {

        // SORT: index both lookup sets so the joins below can binary search
        PriorityReportSupport.ReservationIndex reservationIndex = new PriorityReportSupport.ReservationIndex(reservationList);
        PriorityReportSupport.StaffIndex staffIndex = new PriorityReportSupport.StaffIndex(staffList);

        // SORT: the VIP queue itself - rank desc, then FIFO within a tier
        ListInterface<PriorityReportSupport.QueueEntry> queue = new DoublyLinkedList<>();
        for (int i = 0; i < priorityList.size(); i++) {
            PriorityReservation priority = priorityList.get(i);
            if (priority == null || priority.isDeleted()) {
                continue;
            }
            // SEARCH: binary search rather than a linear scan per record
            Reservation reservation = reservationIndex.find(priority.getReservationId());
            if (reservation == null || reservation.isDeleted()) {
                continue;
            }
            queue.addSorted(new PriorityReportSupport.QueueEntry(priority, reservation));
        }
        PriorityReportSupport.assignPositionsAndDisplacement(queue);
        int inversions = PriorityReportSupport.countPriorityInversions(queue);
        int queueDepth = queue.size();

        // select the rows this run reports on; positions stay true to the
        // full queue so a filtered view never invents a better position
        ListInterface<PriorityReportSupport.QueueEntry> shown = new DoublyLinkedList<>();
        int overriddenCount = 0;
        int emergencyGrants = 0;
        int unjustified = 0;
        int displacedByOverrides = 0;
        PriorityReportSupport.QueueEntry biggestDisplacer = null;

        ListInterface<StaffTally> tallies = new DoublyLinkedList<>();

        for (int i = 0; i < queueDepth; i++) {
            PriorityReportSupport.QueueEntry entry = queue.get(i);
            if (!passesFilters(entry, from, to, minLevel, overrideScope)) {
                continue;
            }
            shown.addBack(entry);

            if (entry.isOverridden()) {
                overriddenCount++;
                displacedByOverrides += entry.getGuestsDisplaced();

                if (entry.getLevel() == PriorityLevel.EMERGENCY) {
                    emergencyGrants++;
                }
                String reason = entry.getPriority().getOverrideReason();
                if (reason == null || reason.trim().isEmpty()) {
                    unjustified++;
                }
                if (biggestDisplacer == null
                        || entry.getGuestsDisplaced() > biggestDisplacer.getGuestsDisplaced()) {
                    biggestDisplacer = entry;
                }
                recordTally(tallies, staffIndex, entry);
            }
        }

        // SORT: authorisers by guests displaced desc, via compareTo + addSorted
        ListInterface<StaffTally> rankedTallies = new DoublyLinkedList<>();
        for (int i = 0; i < tallies.size(); i++) {
            rankedTallies.addSorted(tallies.get(i));
        }

        return new Result(
                toTable(shown, staffIndex),
                buildCharts(rankedTallies),
                buildSummary(shown, rankedTallies, queueDepth, overriddenCount, emergencyGrants,
                        unjustified, displacedByOverrides, biggestDisplacer, inversions),
                shown.size());
    }

    // -------------------- filtering --------------------

    private boolean passesFilters(PriorityReportSupport.QueueEntry entry, LocalDateTime from, LocalDateTime to,
            PriorityLevel minLevel, int overrideScope) {
        if (!inRange(entry.getRegisteredAt(), from, to)) {
            return false;
        }
        if (minLevel != null && entry.rank() < minLevel.getRank()) {
            return false;
        }
        if (overrideScope == 1 && !entry.isOverridden()) {
            return false;
        }
        if (overrideScope == 2 && entry.isOverridden()) {
            return false;
        }
        return true;
    }

    private boolean inRange(LocalDateTime value, LocalDateTime from, LocalDateTime to) {
        if (value == null) {
            return false;
        }
        if (from != null && value.isBefore(from)) {
            return false;
        }
        if (to != null && value.isAfter(to)) {
            return false;
        }
        return true;
    }

    // -------------------- staff tally --------------------

    // linear search over a short list of authorisers, then accumulate
    private void recordTally(ListInterface<StaffTally> tallies, PriorityReportSupport.StaffIndex staffIndex,
            PriorityReportSupport.QueueEntry entry) {
        String staffId = entry.getPriority().getOverriddenBy();
        StaffTally tally = null;
        for (int i = 0; i < tallies.size(); i++) {
            if (tallies.get(i).staffId.equalsIgnoreCase(staffId)) {
                tally = tallies.get(i);
                break;
            }
        }
        if (tally == null) {
            tally = new StaffTally(staffId, staffIndex.nameOf(staffId));
            tallies.addBack(tally);
        }
        tally.overrides++;
        tally.guestsDisplaced += entry.getGuestsDisplaced();
        if (entry.getLevel() != null && entry.getLevel().getRank() > tally.highestRank) {
            tally.highestRank = entry.getLevel().getRank();
            tally.highestLevel = entry.getLevel();
        }
    }

    // -------------------- table --------------------

    private String[][] toTable(ListInterface<PriorityReportSupport.QueueEntry> shown, PriorityReportSupport.StaffIndex staffIndex) {
        String[][] table = new String[shown.size() + 1][8];
        // headers kept short so the rendered table stays inside DOC_WIDTH (132)
        table[0] = new String[] { "Pos", "Reservation", "Priority", "Displaced",
                "Wait (min)", "Room Type", "Authorised By", "Override Reason" };
        for (int i = 0; i < shown.size(); i++) {
            PriorityReportSupport.QueueEntry entry = shown.get(i);
            Reservation reservation = entry.getReservation();
            Long waited = entry.waitingMinutes();
            PriorityReservation priority = entry.getPriority();

            table[i + 1] = new String[] {
                    String.valueOf(entry.getPosition()),
                    entry.getReservationId(),
                    entry.getLevel() == null ? "-" : entry.getLevel().name(),
                    String.valueOf(entry.getGuestsDisplaced()),
                    waited == null ? "-" : String.valueOf(waited),
                    (reservation == null || reservation.getRoomTypeRequested() == null)
                            ? "-" : reservation.getRoomTypeRequested().name(),
                    entry.isOverridden() ? staffIndex.nameOf(priority.getOverriddenBy()) : "-",
                    entry.isOverridden() ? truncate(priority.getOverrideReason()) : "-"
            };
        }
        return table;
    }

    private String truncate(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            return "(none given)";
        }
        String trimmed = reason.trim();
        return trimmed.length() <= REASON_WIDTH
                ? trimmed
                : trimmed.substring(0, REASON_WIDTH - 3) + "...";
    }

    // -------------------- charts --------------------

    private ListInterface<ReportChart> buildCharts(ListInterface<StaffTally> tallies) {
        ListInterface<ReportChart> charts = new DoublyLinkedList<>();

        ReportChart counts = new ReportChart("Overrides Authorised by Staff");
        ReportChart impact = new ReportChart("Guests Displaced by Override Authoriser");
        for (int i = 0; i < tallies.size(); i++) {
            StaffTally tally = tallies.get(i);
            String label = firstName(tally.staffName);
            counts.addBar(label, tally.overrides,
                    "(" + tally.overrides + " override" + (tally.overrides == 1 ? "" : "s") + ")");
            impact.addBar(label, tally.guestsDisplaced,
                    "(" + tally.guestsDisplaced + " guest"
                            + (tally.guestsDisplaced == 1 ? "" : "s") + " pushed back)");
        }
        charts.addBack(counts);
        charts.addBack(impact);
        return charts;
    }

    // the vertical chart slots are narrow, so a full name will not fit
    private String firstName(String name) {
        if (name == null || name.isEmpty()) {
            return "-";
        }
        int space = name.indexOf(' ');
        return space > 0 ? name.substring(0, space) : name;
    }

    // -------------------- summary --------------------

    private String[] buildSummary(ListInterface<PriorityReportSupport.QueueEntry> shown,
            ListInterface<StaffTally> tallies, int queueDepth, int overriddenCount,
            int emergencyGrants, int unjustified, int displacedByOverrides,
            PriorityReportSupport.QueueEntry biggestDisplacer, int inversions) {

        double overrideRate = shown.isEmpty()
                ? 0 : (double) overriddenCount / shown.size() * 100;

        String mostActive = "-";
        if (!tallies.isEmpty()) {
            StaffTally top = tallies.get(0);
            mostActive = top.staffName + " (" + top.staffId + ") - "
                    + top.overrides + " override" + (top.overrides == 1 ? "" : "s")
                    + ", " + top.guestsDisplaced + " guest"
                    + (top.guestsDisplaced == 1 ? "" : "s") + " displaced"
                    + (top.highestLevel == null ? "" : ", highest " + top.highestLevel.name());
        }

        String biggestText = "-";
        if (biggestDisplacer != null) {
            biggestText = biggestDisplacer.getReservationId()
                    + " (" + (biggestDisplacer.getLevel() == null
                            ? "-" : biggestDisplacer.getLevel().name())
                    + " at position " + biggestDisplacer.getPosition()
                    + ", jumped " + biggestDisplacer.getGuestsDisplaced() + " guest"
                    + (biggestDisplacer.getGuestsDisplaced() == 1 ? "" : "s")
                    + ") - " + truncate(biggestDisplacer.getPriority().getOverrideReason());
        }

        return new String[] {
                Ansi.bold("Queue Depth: ") + queueDepth + " active priority reservation"
                        + (queueDepth == 1 ? "" : "s") + " (" + shown.size() + " shown after filters)",
                Ansi.bold("Override Rate: ") + String.format("%.1f%%", overrideRate)
                        + " (" + overriddenCount + " of " + shown.size()
                        + " record" + (shown.size() == 1 ? "" : "s") + " set by staff)",
                Ansi.bold("Emergency Grants: ") + emergencyGrants
                        + " (highest-risk override type - bypasses loyalty tier entirely)",
                Ansi.bold("Total Guests Displaced by Overrides: ") + displacedByOverrides,
                Ansi.bold("Most Active Authoriser: ") + mostActive,
                Ansi.bold("Highest Single Displacement: ") + biggestText,
                Ansi.bold("Unjustified Overrides: ") + unjustified
                        + " (no reason recorded)",
                Ansi.bold("Priority Inversions Detected: ") + inversions
                        + (inversions == 0
                                ? " - queue order is correct"
                                : " - QUEUE ORDER IS WRONG, investigate immediately")
        };
    }

    // -------------------- row --------------------

    private static class StaffTally implements Comparable<StaffTally> {

        private final String staffId;
        private final String staffName;
        private int overrides;
        private int guestsDisplaced;
        private int highestRank = Integer.MIN_VALUE;
        private PriorityLevel highestLevel;

        StaffTally(String staffId, String staffName) {
            this.staffId = staffId == null ? "-" : staffId;
            this.staffName = staffName;
        }

        // most guests displaced first, then most overrides, then by id
        @Override
        public int compareTo(StaffTally other) {
            int comparison = Integer.compare(other.guestsDisplaced, guestsDisplaced);
            if (comparison != 0) {
                return comparison;
            }
            comparison = Integer.compare(other.overrides, overrides);
            if (comparison != 0) {
                return comparison;
            }
            return staffId.compareToIgnoreCase(other.staffId);
        }
    }

    // -------------------- result --------------------

    public static class Result {

        private final String[][] table;
        private final ListInterface<ReportChart> charts;
        private final String[] summary;
        private final int recordCount;

        Result(String[][] table, ListInterface<ReportChart> charts,
                String[] summary, int recordCount) {
            this.table = table;
            this.charts = charts == null ? new DoublyLinkedList<>() : charts;
            this.summary = summary;
            this.recordCount = recordCount;
        }

        public String[][] getTable() {
            return table;
        }

        public ListInterface<ReportChart> getCharts() {
            return charts;
        }

        public String[] getSummary() {
            return summary;
        }

        public int getRecordCount() {
            return recordCount;
        }

        public boolean isEmpty() {
            return table == null || table.length <= 1;
        }
    }
}
