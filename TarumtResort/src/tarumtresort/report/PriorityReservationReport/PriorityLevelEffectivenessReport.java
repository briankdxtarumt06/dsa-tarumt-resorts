package tarumtresort.report.PriorityReservationReport;

import java.time.LocalDateTime;
import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;
import tarumtresort.entity.PriorityReservation;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.enums.PriorityLevel;
import tarumtresort.entity.enums.ReservationStatus;
import tarumtresort.entity.enums.RoomType;
import tarumtresort.report.ReportChart;
import tarumtresort.utility.Ansi;

// Author: Lee Boon Yew
/**
 * PRIORITY LEVEL EFFECTIVENESS REPORT
 *
 * Management question: is the priority level actually buying the guest faster
 * service, or is the ranking cosmetic?
 *
 * One row per priority level, measured on the things that only exist because
 * this module exists - where the tier lands in the VIP queue, how often it is
 * overtaken by later arrivals, and whether its service-level promise held.
 *
 * Queue positions are computed over the FULL active queue and the filters
 * then select which records are reported on. That is deliberate: a tier's
 * average position is only meaningful relative to the real queue, not
 * relative to a filtered subset of it.
 *
 * Dependencies: PriorityReservation + Reservation + ReservationTimestamps.
 * Filters: registration date range, reservation status, room type requested.
 */
public class PriorityLevelEffectivenessReport {

    private final ListInterface<PriorityReservation> priorityList;
    private final ListInterface<Reservation> reservationList;

    public PriorityLevelEffectivenessReport(ListInterface<PriorityReservation> priorityList,
            ListInterface<Reservation> reservationList) {
        this.priorityList = priorityList == null ? new DoublyLinkedList<>() : priorityList;
        this.reservationList = reservationList == null ? new DoublyLinkedList<>() : reservationList;
    }

    public Result generate(LocalDateTime from, LocalDateTime to,
            ReservationStatus statusFilter, RoomType roomTypeFilter) {

        // SORT: index the reservations by id so the join below can binary search
        PriorityReportSupport.ReservationIndex index = new PriorityReportSupport.ReservationIndex(reservationList);

        // SORT: build the full VIP queue by rank desc, then FIFO within a tier
        ListInterface<PriorityReportSupport.QueueEntry> queue = new DoublyLinkedList<>();
        for (int i = 0; i < priorityList.size(); i++) {
            PriorityReservation priority = priorityList.get(i);
            if (priority == null || priority.isDeleted()) {
                continue;
            }
            // SEARCH: binary search the sorted index instead of a linear scan
            Reservation reservation = index.find(priority.getReservationId());
            if (reservation == null || reservation.isDeleted()) {
                continue;
            }
            queue.addSorted(new PriorityReportSupport.QueueEntry(priority, reservation));
        }
        PriorityReportSupport.assignPositionsAndDisplacement(queue);

        // aggregate the filtered records into one bucket per tier
        TierRow[] buckets = new TierRow[PriorityLevel.values().length];
        int totalRecords = 0;
        int totalOverridden = 0;
        int totalBreaches = 0;
        int totalMeasured = 0;
        PriorityReportSupport.QueueEntry worstCase = null;

        for (int i = 0; i < queue.size(); i++) {
            PriorityReportSupport.QueueEntry entry = queue.get(i);
            if (!passesFilters(entry, from, to, statusFilter, roomTypeFilter)) {
                continue;
            }
            PriorityLevel level = entry.getLevel();
            if (level == null) {
                continue;
            }
            if (buckets[level.ordinal()] == null) {
                buckets[level.ordinal()] = new TierRow(level);
            }
            TierRow row = buckets[level.ordinal()];
            row.accumulate(entry);

            totalRecords++;
            if (entry.isOverridden()) {
                totalOverridden++;
            }
            Long waited = entry.waitingMinutes();
            if (waited != null) {
                totalMeasured++;
                if (entry.breachedSla()) {
                    totalBreaches++;
                }
                Long worstWait = worstCase == null ? null : worstCase.waitingMinutes();
                if (worstWait == null || waited > worstWait) {
                    worstCase = entry;
                }
            }
        }

        // SORT: tier rows by rank descending, via compareTo + addSorted
        ListInterface<TierRow> rows = new DoublyLinkedList<>();
        for (TierRow bucket : buckets) {
            if (bucket != null) {
                rows.addSorted(bucket);
            }
        }

        return new Result(
                toTable(rows),
                buildCharts(rows),
                buildSummary(rows, totalRecords, totalOverridden, totalBreaches, totalMeasured, worstCase),
                totalRecords);
    }

    // -------------------- filtering --------------------

    private boolean passesFilters(PriorityReportSupport.QueueEntry entry, LocalDateTime from, LocalDateTime to,
            ReservationStatus statusFilter, RoomType roomTypeFilter) {
        if (!inRange(entry.getRegisteredAt(), from, to)) {
            return false;
        }
        if (statusFilter != null && entry.getStatus() != statusFilter) {
            return false;
        }
        Reservation reservation = entry.getReservation();
        if (roomTypeFilter != null
                && (reservation == null || reservation.getRoomTypeRequested() != roomTypeFilter)) {
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

    // -------------------- table --------------------

    private String[][] toTable(ListInterface<TierRow> rows) {
        String[][] table = new String[rows.size() + 1][10];
        // headers kept short so the rendered table stays inside DOC_WIDTH (132)
        table[0] = new String[] { "Tier", "Rank", "Records", "Avg Q.Pos",
                "Best/Wst", "Avg Serve", "Waiting Now", "Overtaken", "Breaches",
                "Tier/Ovr" };
        for (int i = 0; i < rows.size(); i++) {
            TierRow row = rows.get(i);
            table[i + 1] = new String[] {
                    row.level.name(),
                    String.valueOf(row.level.getRank()),
                    String.valueOf(row.total),
                    row.total == 0 ? "-" : String.format("%.1f", row.averagePosition()),
                    row.total == 0 ? "-" : (row.bestPosition + " / " + row.worstPosition),
                    row.servedCount == 0 ? "-" : String.valueOf(Math.round(row.averageServeTime())),
                    row.stillWaitingCount() == 0 ? "-" : String.valueOf(Math.round(row.averageWaitingNow())),
                    String.valueOf(row.timesOvertaken),
                    String.valueOf(row.slaBreaches),
                    (row.total - row.fromOverride) + " / " + row.fromOverride
            };
        }
        return table;
    }

    // -------------------- charts --------------------

    private ListInterface<ReportChart> buildCharts(ListInterface<TierRow> rows) {
        ListInterface<ReportChart> charts = new DoublyLinkedList<>();

        // Both charts should slope upward from the top tier down. Any tier
        // that breaks the slope is the finding management needs to see.
        ReportChart positions = new ReportChart("Average Queue Position by Priority Tier (lower is better)");
        for (int i = 0; i < rows.size(); i++) {
            TierRow row = rows.get(i);
            positions.addBar(row.level.name(), row.averagePosition(),
                    "(" + row.total + " record" + (row.total == 1 ? "" : "s") + ")");
        }
        charts.addBack(positions);

        // completed waits only - mixing in the still-growing elapsed time of
        // unserved guests would make the bars drift upward as the day goes on
        ReportChart waits = new ReportChart("Average Time to Service by Priority Tier (min)");
        for (int i = 0; i < rows.size(); i++) {
            TierRow row = rows.get(i);
            waits.addBar(row.level.name(), row.averageServeTime(),
                    "(target " + PriorityReportSupport.slaTargetMinutes(row.level) + " min)");
        }
        charts.addBack(waits);

        return charts;
    }

    // -------------------- summary --------------------

    private String[] buildSummary(ListInterface<TierRow> rows, int totalRecords,
            int totalOverridden, int totalBreaches, int totalMeasured,
            PriorityReportSupport.QueueEntry worstCase) {

        double compliance = totalMeasured == 0
                ? 100 : (double) (totalMeasured - totalBreaches) / totalMeasured * 100;
        double overrideShare = totalRecords == 0
                ? 0 : (double) totalOverridden / totalRecords * 100;

        TierRow mostOvertaken = null;
        for (int i = 0; i < rows.size(); i++) {
            TierRow row = rows.get(i);
            if (mostOvertaken == null || row.timesOvertaken > mostOvertaken.timesOvertaken) {
                mostOvertaken = row;
            }
        }

        String worstText = "-";
        if (worstCase != null) {
            Long waited = worstCase.waitingMinutes();
            worstText = worstCase.getReservationId()
                    + " (" + (worstCase.getLevel() == null ? "-" : worstCase.getLevel().name())
                    + ", queue position " + worstCase.getPosition()
                    + ", waited " + (waited == null ? "-" : waited) + " min)";
        }

        return new String[] {
                Ansi.bold("Rank Correlation: ") + rankCorrelation(rows),
                Ansi.bold("Total Priority Records: ") + totalRecords
                        + " across " + rows.size() + " tier" + (rows.size() == 1 ? "" : "s"),
                Ansi.bold("SLA Compliance Rate: ") + String.format("%.1f%%", compliance)
                        + " (" + totalBreaches + " breach" + (totalBreaches == 1 ? "" : "es")
                        + " of " + totalMeasured + " measured)",
                Ansi.bold("Most Overtaken Tier: ")
                        + (mostOvertaken == null || mostOvertaken.timesOvertaken == 0
                                ? "None - no tier was passed by a later arrival"
                                : mostOvertaken.level.name() + " (" + mostOvertaken.timesOvertaken
                                        + " time" + (mostOvertaken.timesOvertaken == 1 ? "" : "s") + ")"),
                Ansi.bold("Worst Individual Case: ") + worstText,
                Ansi.bold("Override-Granted Share: ") + String.format("%.1f%%", overrideShare)
                        + " (" + totalOverridden + " of " + totalRecords
                        + " record" + (totalRecords == 1 ? "" : "s") + " set by staff)"
        };
    }

    /**
     * The headline finding. Rows arrive ordered by rank descending, so a
     * working priority system produces average wait times that never fall as
     * the rank falls. The first tier that is served FASTER than the tier above
     * it is the point where the ranking stops being honoured.
     */
    private String rankCorrelation(ListInterface<TierRow> rows) {
        TierRow previous = null;
        int compared = 0;
        for (int i = 0; i < rows.size(); i++) {
            TierRow row = rows.get(i);
            // only tiers with a completed service can be compared: a guest who
            // is still queuing has no final wait yet, and their elapsed time
            // keeps rising, which would flip this verdict as the day wears on
            if (row.servedCount == 0) {
                continue;
            }
            compared++;
            if (previous != null && row.averageServeTime() < previous.averageServeTime()) {
                return "BROKEN AT " + row.level.name()
                        + " - served faster (" + Math.round(row.averageServeTime()) + " min) than "
                        + previous.level.name() + " (" + Math.round(previous.averageServeTime()) + " min)";
            }
            previous = row;
        }
        if (compared < 2) {
            return "NOT ENOUGH DATA - fewer than two tiers have a completed service";
        }
        return "HONOURED - time to service rises as priority falls (" + compared + " tiers compared)";
    }

    // -------------------- row --------------------

    private static class TierRow implements Comparable<TierRow> {

        private final PriorityLevel level;
        private int total;
        private int fromOverride;
        private long positionSum;
        private int bestPosition = Integer.MAX_VALUE;
        private int worstPosition;
        private long waitSum;
        private int measuredCount;
        // completed waits only (registration -> assigned). Kept apart from the
        // figures above because a still-waiting guest's elapsed time keeps
        // growing, so averaging the two together makes tiers with unserved
        // guests look slower purely because the clock moved on.
        private long servedWaitSum;
        private int servedCount;
        private int timesOvertaken;
        private int slaBreaches;

        TierRow(PriorityLevel level) {
            this.level = level;
        }

        void accumulate(PriorityReportSupport.QueueEntry entry) {
            total++;
            if (entry.isOverridden()) {
                fromOverride++;
            }
            int position = entry.getPosition();
            positionSum += position;
            bestPosition = Math.min(bestPosition, position);
            worstPosition = Math.max(worstPosition, position);
            timesOvertaken += entry.getTimesOvertaken();

            Long waited = entry.waitingMinutes();
            if (waited != null) {
                waitSum += waited;
                measuredCount++;
                if (PriorityReportSupport.isServed(entry.getStatus())) {
                    servedWaitSum += waited;
                    servedCount++;
                }
                if (entry.breachedSla()) {
                    slaBreaches++;
                }
            }
        }

        double averagePosition() {
            return total == 0 ? 0 : (double) positionSum / total;
        }

        double averageWait() {
            return measuredCount == 0 ? 0 : (double) waitSum / measuredCount;
        }

        /**
         * Average time actually taken to serve this tier. Only completed
         * waits count, so the figure is stable and comparable between tiers
         * no matter how long the report is run after the data was captured.
         */
        double averageServeTime() {
            return servedCount == 0 ? 0 : (double) servedWaitSum / servedCount;
        }

        int stillWaitingCount() {
            return measuredCount - servedCount;
        }

        /** How long the guests still in the queue have been waiting so far. */
        double averageWaitingNow() {
            int waitingRecords = stillWaitingCount();
            return waitingRecords == 0 ? 0 : (double) (waitSum - servedWaitSum) / waitingRecords;
        }

        // highest rank first, matching addSorted order
        @Override
        public int compareTo(TierRow other) {
            return Integer.compare(other.level.getRank(), level.getRank());
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
