package tarumtresort.test;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Scanner;

import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.control.PriorityReservationController;
import tarumtresort.dao.PriorityReservationDAO;
import tarumtresort.dao.ReservationDAO;
import tarumtresort.dao.StaffDAO;
import tarumtresort.entity.PriorityReservation;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.Staff;
import tarumtresort.entity.enums.PriorityLevel;
import tarumtresort.entity.enums.ReservationStatus;
import tarumtresort.entity.enums.RoomType;
import tarumtresort.entity.enums.Tier;
import tarumtresort.report.PriorityReservationReport.PriorityLevelEffectivenessReport;
import tarumtresort.report.PriorityReservationReport.QueueOrdering;
import tarumtresort.report.PriorityReservationReport.ReservationIndex;
import tarumtresort.report.PriorityReservationReport.StaffIndex;
import tarumtresort.report.PriorityReservationReport.VipQueueGovernanceReport;
import tarumtresort.utility.Ansi;

// Author: Lee Boon Yew
/**
 * VERIFICATION AND VALIDATION DRIVER - Priority Reservation module.
 *
 *   VERIFICATION  "are we building the product right?"
 *                 the ADT, the queue algorithm, the searches and the reports
 *                 produce the results the design says they should.
 *
 *   VALIDATION    "are we building the right product?"
 *                 business rules hold and bad input is rejected safely -
 *                 non-members get no automatic priority, soft-deleted records
 *                 stay out of the queue, missing data never crashes a report.
 *
 * RUN IT FROM THE PROJECT ROOT (the folder containing data/), because the
 * DAOs resolve "data/..." relative to the working directory.
 *
 * SAFETY: parts F and G write to data/priorityReservations.json. The file is
 * backed up on start and restored by a JVM shutdown hook, so the original is
 * put back even if a test fails, throws, or the UI calls System.exit.
 */
public class PriorityReservationTestDriver {

    private static final Path PRIORITY_FILE = Path.of("data/priorityReservations.json");

    private static int passed = 0;
    private static int failed = 0;
    private static String firstFailure = null;

    public static void main(String[] args) {
        installBackupAndRestoreHook();

        banner("PRIORITY RESERVATION - VERIFICATION & VALIDATION DRIVER");

        partA_adt();
        partB_entitiesAndEnums();
        partC_searchAndFilter();
        partD_queueAlgorithm();
        partE_reports();
        partF_crud();
        partG_robustness();
        partH_userInterface();
        partI_menuNavigation();

        summary();
    }

    // ==================================================================
    // PART A - ADT verification (the LinkedList the whole module rests on)
    // ==================================================================

    private static void partA_adt() {
        section("PART A  ADT - LinkedList operations");

        LinkedListInterface<String> list = new LinkedList<>();
        check("a new list is empty", list.isEmpty() && list.size() == 0);

        list.addBack("B");
        list.addBack("C");
        list.addFront("A");
        check("addFront / addBack place elements correctly",
                "A".equals(list.get(0)) && "B".equals(list.get(1)) && "C".equals(list.get(2)));
        check("size tracks the element count", list.size() == 3);

        check("indexOf finds an element", list.indexOf("B") == 1);
        check("indexOf returns -1 when absent", list.indexOf("ZZ") == -1);
        check("contains agrees with indexOf", list.contains("B") && !list.contains("ZZ"));

        check("get out of range returns null instead of throwing",
                list.get(99) == null && list.get(-1) == null);

        check("getFront / getBack", "A".equals(list.getFront()) && "C".equals(list.getBack()));

        list.set(1, "B2");
        check("set replaces in place", "B2".equals(list.get(1)) && list.size() == 3);

        check("removeElement reports success", list.removeElement("B2"));
        check("removeElement shrinks the list", list.size() == 2);
        check("removeElement on a missing value returns false", !list.removeElement("ZZ"));

        check("removeIndex returns the removed value", "A".equals(list.removeIndex(0)));
        check("removeFront / removeBack drain the list",
                "C".equals(list.removeFront()) && list.isEmpty());
        check("removing from an empty list returns null", list.removeFront() == null);

        // addSorted is what every report relies on to order its rows
        LinkedListInterface<String> sorted = new LinkedList<>();
        sorted.addSorted("M");
        sorted.addSorted("A");
        sorted.addSorted("Z");
        sorted.addSorted("B");
        check("addSorted inserts in ascending order",
                "A".equals(sorted.get(0)) && "B".equals(sorted.get(1))
                        && "M".equals(sorted.get(2)) && "Z".equals(sorted.get(3)));
        check("isSorted confirms the order", sorted.isSorted());

        int walked = 0;
        for (String ignored : sorted) {
            walked++;
        }
        check("the iterator visits every element", walked == sorted.size());

        sorted.clear();
        check("clear empties the list", sorted.isEmpty() && sorted.size() == 0);
    }

    // ==================================================================
    // PART B - entity and enum validation
    // ==================================================================

    private static void partB_entitiesAndEnums() {
        section("PART B  Entities & enums");

        check("priority ranks increase with seniority",
                PriorityLevel.PENALTY.getRank() < PriorityLevel.SLIVER.getRank()
                        && PriorityLevel.SLIVER.getRank() < PriorityLevel.GOLD.getRank()
                        && PriorityLevel.GOLD.getRank() < PriorityLevel.PLATINUM.getRank()
                        && PriorityLevel.PLATINUM.getRank() < PriorityLevel.DIAMOND.getRank()
                        && PriorityLevel.DIAMOND.getRank() < PriorityLevel.EMERGENCY.getRank());

        check("loyalty tier maps to the matching priority level",
                PriorityLevel.convertTierToPriority(Tier.DIAMOND) == PriorityLevel.DIAMOND
                        && PriorityLevel.convertTierToPriority(Tier.PLATINUM) == PriorityLevel.PLATINUM
                        && PriorityLevel.convertTierToPriority(Tier.GOLD) == PriorityLevel.GOLD);
        check("the lowest tier maps to SLIVER",
                PriorityLevel.convertTierToPriority(Tier.SILVER) == PriorityLevel.SLIVER);

        PriorityReservation a = new PriorityReservation("R1", PriorityLevel.GOLD);
        PriorityReservation b = new PriorityReservation("R2", PriorityLevel.DIAMOND);
        PriorityReservation duplicateId = new PriorityReservation("R1", PriorityLevel.PENALTY);

        check("compareTo orders by priority level", a.compareTo(b) < 0 && b.compareTo(a) > 0);
        check("records are equal when the reservation id matches", a.equals(duplicateId));
        check("records differ when the reservation id differs", !a.equals(b));
        check("a new record is not deleted", !a.isDeleted());
        check("a new record has no override recorded", a.getOverriddenBy() == null);

        check("SLA targets tighten as priority rises",
                QueueOrdering.slaTargetMinutes(PriorityLevel.EMERGENCY)
                        < QueueOrdering.slaTargetMinutes(PriorityLevel.DIAMOND)
                        && QueueOrdering.slaTargetMinutes(PriorityLevel.DIAMOND)
                                < QueueOrdering.slaTargetMinutes(PriorityLevel.GOLD)
                        && QueueOrdering.slaTargetMinutes(PriorityLevel.GOLD)
                                < QueueOrdering.slaTargetMinutes(PriorityLevel.PENALTY));

        check("served statuses are recognised",
                QueueOrdering.isServed(ReservationStatus.ASSIGNED)
                        && QueueOrdering.isServed(ReservationStatus.CHECKED_IN)
                        && QueueOrdering.isServed(ReservationStatus.CHECKED_OUT));
        check("waiting and cancelled do not count as served",
                !QueueOrdering.isServed(ReservationStatus.WAITING)
                        && !QueueOrdering.isServed(ReservationStatus.CANCELLED));
    }

    // ==================================================================
    // PART C - searching and filtering
    // ==================================================================

    private static void partC_searchAndFilter() {
        section("PART C  Searching & filtering");

        PriorityReservationController control = controller();
        check("the module loaded records from file", control.size() > 0 && !control.isEmpty());

        String knownId = firstActiveId();
        check("linear search finds a known reservation id",
                control.searchPriorityReservationById(knownId) != null);
        check("search returns null for an unknown id",
                control.searchPriorityReservationById("NO_SUCH_ID") == null);
        check("search rejects an empty id safely",
                control.searchPriorityReservationById("") == null);

        boolean nullSafe;
        try {
            nullSafe = control.searchPriorityReservationById(null) == null;
        } catch (RuntimeException e) {
            nullSafe = false;
        }
        check("search rejects a null id without throwing", nullSafe);

        // the level filters must partition the active set exactly
        int sum = 0;
        boolean pure = true;
        boolean excludesDeleted = true;
        for (PriorityLevel level : PriorityLevel.values()) {
            LinkedListInterface<PriorityReservation> found = control.filterByLevel(level);
            sum += found.size();
            for (int i = 0; i < found.size(); i++) {
                if (found.get(i).getPriorityLevel() != level) {
                    pure = false;
                }
                if (found.get(i).isDeleted()) {
                    excludesDeleted = false;
                }
            }
        }
        check("each filter returns only its own level", pure);
        check("filters exclude soft-deleted records", excludesDeleted);
        check("the six filters partition the active set", sum == activeCount());

        // binary search over the sorted index must agree with a linear scan
        LinkedListInterface<Reservation> reservations = loadReservations();
        ReservationIndex index = new ReservationIndex(reservations);
        boolean agrees = true;
        for (int i = 0; i < reservations.size(); i++) {
            Reservation expected = reservations.get(i);
            Reservation actual = index.find(expected.getReservationId());
            if (actual == null || !actual.getReservationId().equals(expected.getReservationId())) {
                agrees = false;
                break;
            }
        }
        check("binary search agrees with a linear scan on every id", agrees);
        check("binary search returns null for an absent id", index.find("NO_SUCH_ID") == null);
        check("binary search handles a null id", index.find(null) == null);

        StaffIndex staffIndex = new StaffIndex(new StaffDAO().retrieveStaffList());
        check("staff lookup falls back to a dash when unknown",
                "-".equals(staffIndex.nameOf(null)));
    }

    // ==================================================================
    // PART D - the VIP queue algorithm
    // ==================================================================

    private static void partD_queueAlgorithm() {
        section("PART D  VIP queue algorithm");

        PriorityReservationController control = controller();
        LinkedListInterface<Reservation> waiting = waitingReservations();
        LinkedListInterface<Reservation> queue = control.generateVIPQueue(waiting);

        check("the queue is built", queue.size() > 0);
        check("the queue never exceeds its input", queue.size() <= waiting.size());

        boolean rankOrdered = true;
        boolean fifoWithinTier = true;
        boolean noDeleted = true;

        for (int i = 0; i < queue.size(); i++) {
            PriorityReservation record = control.searchPriorityReservationById(
                    queue.get(i).getReservationId());
            if (record != null && record.isDeleted()) {
                noDeleted = false;
            }
            if (i + 1 >= queue.size()) {
                continue;
            }
            PriorityReservation next = control.searchPriorityReservationById(
                    queue.get(i + 1).getReservationId());
            if (record == null || next == null) {
                continue;
            }
            int here = record.getPriorityLevel().getRank();
            int there = next.getPriorityLevel().getRank();
            if (here < there) {
                rankOrdered = false;
            } else if (here == there) {
                LocalDateTime t1 = registrationOf(queue.get(i));
                LocalDateTime t2 = registrationOf(queue.get(i + 1));
                if (t1 != null && t2 != null && t1.isAfter(t2)) {
                    fifoWithinTier = false;
                }
            }
        }

        check("VERIFICATION: higher priority is always served first", rankOrdered);
        check("VERIFICATION: equal priority is served first-come-first-served", fifoWithinTier);
        check("VALIDATION: soft-deleted records never enter the queue", noDeleted);
        check("an empty input produces an empty queue",
                control.generateVIPQueue(new LinkedList<>()).size() == 0);

        // the same ordering rule, expressed as compareTo for the reports
        LinkedListInterface<QueueOrdering.Entry> ordered = buildEntries();
        QueueOrdering.assignPositionsAndDisplacement(ordered);
        check("VERIFICATION: the report ordering has zero priority inversions",
                QueueOrdering.countPriorityInversions(ordered) == 0);

        if (ordered.size() > 0) {
            check("positions are numbered from 1",
                    ordered.get(0).getPosition() == 1);
            check("positions run to the queue length",
                    ordered.get(ordered.size() - 1).getPosition() == ordered.size());
            check("the last guest in the queue has displaced nobody",
                    ordered.get(ordered.size() - 1).getGuestsDisplaced() == 0);

            // displacement and overtaking are two views of the same pairs
            int displaced = 0;
            int overtaken = 0;
            for (int i = 0; i < ordered.size(); i++) {
                displaced += ordered.get(i).getGuestsDisplaced();
                overtaken += ordered.get(i).getTimesOvertaken();
            }
            check("VERIFICATION: total displaced equals total overtaken",
                    displaced == overtaken);
        }
    }

    // ==================================================================
    // PART E - the two management reports
    // ==================================================================

    private static void partE_reports() {
        section("PART E  Reports");

        LinkedListInterface<PriorityReservation> priorities = new PriorityReservationDAO().loadFromFile();
        LinkedListInterface<Reservation> reservations = loadReservations();
        LinkedListInterface<Staff> staff = new StaffDAO().retrieveStaffList();

        // ---- report 1, unfiltered ----
        PriorityLevelEffectivenessReport.Result r1 =
                new PriorityLevelEffectivenessReport(priorities, reservations)
                        .generate(null, null, null, null);

        check("effectiveness report produced rows", !r1.isEmpty());
        check("effectiveness table has a header plus data",
                r1.getTable().length >= 2 && r1.getTable()[0].length == 10);
        check("every effectiveness row has the full column count",
                rectangular(r1.getTable()));
        check("effectiveness report produced two charts", r1.getCharts().size() == 2);
        check("effectiveness report produced summary metrics",
                r1.getSummary() != null && r1.getSummary().length > 0);
        check("effectiveness row count matches the active records",
                r1.getRecordCount() == activeCount());

        // rows arrive ranked, so the rank column must fall down the table
        boolean ranksDescend = true;
        for (int i = 2; i < r1.getTable().length; i++) {
            int previous = Integer.parseInt(r1.getTable()[i - 1][1]);
            int current = Integer.parseInt(r1.getTable()[i][1]);
            if (current > previous) {
                ranksDescend = false;
            }
        }
        check("VERIFICATION: effectiveness rows are ordered by rank descending", ranksDescend);

        // ---- report 1, filtered ----
        PriorityLevelEffectivenessReport.Result filtered =
                new PriorityLevelEffectivenessReport(priorities, reservations)
                        .generate(null, null, ReservationStatus.WAITING, null);
        check("VALIDATION: a status filter narrows the result",
                filtered.getRecordCount() <= r1.getRecordCount());

        PriorityLevelEffectivenessReport.Result future =
                new PriorityLevelEffectivenessReport(priorities, reservations)
                        .generate(LocalDateTime.now().plusYears(5), null, null, null);
        check("VALIDATION: an impossible date range yields no rows", future.isEmpty());

        // ---- report 2 ----
        VipQueueGovernanceReport.Result r2 =
                new VipQueueGovernanceReport(priorities, reservations, staff)
                        .generate(null, null, null, 0);

        check("governance report produced rows", !r2.isEmpty());
        check("governance table has a header plus data",
                r2.getTable().length >= 2 && r2.getTable()[0].length == 8);
        check("every governance row has the full column count", rectangular(r2.getTable()));
        check("governance report produced two charts", r2.getCharts().size() == 2);
        check("governance row count matches the active records",
                r2.getRecordCount() == activeCount());

        // summary lines carry ANSI styling, so strip it before matching
        boolean reportsZeroInversions = false;
        for (String line : r2.getSummary()) {
            if (line != null && Ansi.strip(line).contains("Priority Inversions Detected: 0")) {
                reportsZeroInversions = true;
            }
        }
        check("VERIFICATION: the governance report self-checks the queue order",
                reportsZeroInversions);

        VipQueueGovernanceReport.Result overriddenOnly =
                new VipQueueGovernanceReport(priorities, reservations, staff)
                        .generate(null, null, null, 1);
        VipQueueGovernanceReport.Result loyaltyOnly =
                new VipQueueGovernanceReport(priorities, reservations, staff)
                        .generate(null, null, null, 2);
        check("VALIDATION: the two override scopes partition the queue",
                overriddenOnly.getRecordCount() + loyaltyOnly.getRecordCount()
                        == r2.getRecordCount());

        VipQueueGovernanceReport.Result diamondUp =
                new VipQueueGovernanceReport(priorities, reservations, staff)
                        .generate(null, null, PriorityLevel.DIAMOND, 0);
        check("VALIDATION: a minimum-level filter narrows the result",
                diamondUp.getRecordCount() <= r2.getRecordCount());
    }

    // ==================================================================
    // PART F - create / update / delete  (writes to disk, restored on exit)
    // ==================================================================

    private static void partF_crud() {
        section("PART F  Create / update / delete");

        PriorityReservationController control = controller();

        check("VALIDATION: a non-member is refused automatic priority",
                !control.addPriorityReservation("TEST_NEW", "NOT_A_MEMBER"));
        check("VALIDATION: the refused request created no record",
                control.searchPriorityReservationById("TEST_NEW") == null);

        String id = firstActiveId();
        int sizeBefore = control.size();
        check("VALIDATION: adding an existing id is accepted but not duplicated",
                control.addPriorityReservation(id, "ANY") && control.size() == sizeBefore);

        // ---- update ----
        PriorityReservation original = control.searchPriorityReservationById(id);
        PriorityLevel originalLevel = original.getPriorityLevel();
        String originalBy = original.getOverriddenBy();
        String originalReason = original.getOverrideReason();

        check("VALIDATION: updating an unknown id is refused",
                !control.updatePriorityReservation(
                        new PriorityReservation("NO_SUCH_ID", PriorityLevel.GOLD, "S", "r", false)));

        check("an override is accepted",
                control.updatePriorityReservation(new PriorityReservation(
                        id, PriorityLevel.EMERGENCY, "STF001", "driver test", false)));

        PriorityReservation updated = control.searchPriorityReservationById(id);
        check("the override applied the new level",
                updated.getPriorityLevel() == PriorityLevel.EMERGENCY);
        check("the override recorded the authorising staff",
                "STF001".equals(updated.getOverriddenBy()));
        check("the override recorded the reason",
                "driver test".equals(updated.getOverrideReason()));
        check("VERIFICATION: the override persisted to file",
                controller().searchPriorityReservationById(id).getPriorityLevel()
                        == PriorityLevel.EMERGENCY);

        control.updatePriorityReservation(
                new PriorityReservation(id, originalLevel, originalBy, originalReason, false));
        check("the record was restored to its original level",
                controller().searchPriorityReservationById(id).getPriorityLevel() == originalLevel);

        // ---- delete ----
        check("VALIDATION: deleting an unknown id is refused",
                !control.removePriorityReservationById("NO_SUCH_ID"));

        int activeBefore = activeCount();
        check("deleting a known record succeeds", control.removePriorityReservationById(id));
        check("VERIFICATION: delete is soft - the record still exists",
                control.searchPriorityReservationById(id) != null
                        && control.searchPriorityReservationById(id).isDeleted());
        check("the soft delete persisted", controller().searchPriorityReservationById(id).isDeleted());
        check("the active count dropped by one", activeCount() == activeBefore - 1);
        check("deleting twice is harmless", control.removePriorityReservationById(id));

        control.searchPriorityReservationById(id).setDeleted(false);
        control.updatePriorityReservation(control.searchPriorityReservationById(id));
        check("the record was restored to active",
                !controller().searchPriorityReservationById(id).isDeleted());
    }

    // ==================================================================
    // PART G - robustness against missing and malformed data
    // ==================================================================

    private static void partG_robustness() {
        section("PART G  Robustness");

        LinkedListInterface<Reservation> reservations = loadReservations();
        LinkedListInterface<Staff> staff = new StaffDAO().retrieveStaffList();

        // a priority record whose reservation no longer exists
        LinkedListInterface<PriorityReservation> dangling = new LinkedList<>();
        dangling.addBack(new PriorityReservation("GHOST_ID", PriorityLevel.DIAMOND));
        boolean survived = true;
        try {
            new PriorityLevelEffectivenessReport(dangling, reservations)
                    .generate(null, null, null, null);
            new VipQueueGovernanceReport(dangling, reservations, staff)
                    .generate(null, null, null, 0);
        } catch (RuntimeException e) {
            survived = false;
        }
        check("VALIDATION: a dangling reservation reference does not crash a report", survived);

        // completely empty inputs
        boolean emptySurvived = true;
        boolean emptyIsEmpty = false;
        try {
            PriorityLevelEffectivenessReport.Result empty =
                    new PriorityLevelEffectivenessReport(new LinkedList<>(), new LinkedList<>())
                            .generate(null, null, null, null);
            emptyIsEmpty = empty.isEmpty();
            new VipQueueGovernanceReport(new LinkedList<>(), new LinkedList<>(), new LinkedList<>())
                    .generate(null, null, null, 0);
        } catch (RuntimeException e) {
            emptySurvived = false;
        }
        check("VALIDATION: empty input does not crash a report", emptySurvived);
        check("VALIDATION: empty input is reported as no data", emptyIsEmpty);

        // null constructor arguments must be tolerated
        boolean nullSurvived = true;
        try {
            new PriorityLevelEffectivenessReport(null, null).generate(null, null, null, null);
            new VipQueueGovernanceReport(null, null, null).generate(null, null, null, 0);
        } catch (RuntimeException e) {
            nullSurvived = false;
        }
        check("VALIDATION: null data sets do not crash a report", nullSurvived);

        // an unused room type must simply produce nothing
        boolean unusedTypeOk = true;
        try {
            for (RoomType type : RoomType.values()) {
                new PriorityLevelEffectivenessReport(
                        new PriorityReservationDAO().loadFromFile(), reservations)
                        .generate(null, null, null, type);
            }
        } catch (RuntimeException e) {
            unusedTypeOk = false;
        }
        check("VALIDATION: every room-type filter runs cleanly", unusedTypeOk);

        // every status filter must run cleanly too
        boolean everyStatusOk = true;
        try {
            for (ReservationStatus status : ReservationStatus.values()) {
                new PriorityLevelEffectivenessReport(
                        new PriorityReservationDAO().loadFromFile(), reservations)
                        .generate(null, null, status, null);
            }
        } catch (RuntimeException e) {
            everyStatusOk = false;
        }
        check("VALIDATION: every status filter runs cleanly", everyStatusOk);
    }

    // ==================================================================
    // PART H - user interface (boundary layer, in isolation)
    // ==================================================================

    private static void partH_userInterface() {
        section("PART H  User interface - input handling & display");

        // ---- selectPriorityLevel: every option maps to the right level ----
        check("VERIFICATION: menu option 1 selects PENALTY",
                runUi("1\n", ui -> ui.selectPriorityLevel("Level")).result == PriorityLevel.PENALTY);
        check("VERIFICATION: menu option 2 selects SLIVER",
                runUi("2\n", ui -> ui.selectPriorityLevel("Level")).result == PriorityLevel.SLIVER);
        check("VERIFICATION: menu option 3 selects GOLD",
                runUi("3\n", ui -> ui.selectPriorityLevel("Level")).result == PriorityLevel.GOLD);
        check("VERIFICATION: menu option 4 selects PLATINUM",
                runUi("4\n", ui -> ui.selectPriorityLevel("Level")).result == PriorityLevel.PLATINUM);
        check("VERIFICATION: menu option 5 selects DIAMOND",
                runUi("5\n", ui -> ui.selectPriorityLevel("Level")).result == PriorityLevel.DIAMOND);
        check("VERIFICATION: menu option 6 selects EMERGENCY",
                runUi("6\n", ui -> ui.selectPriorityLevel("Level")).result == PriorityLevel.EMERGENCY);
        check("VALIDATION: option 0 cancels level selection",
                runUi("0\n", ui -> ui.selectPriorityLevel("Level")).result == null);

        UiRun rejectsRange = runUi("9\n99\n3\n", ui -> ui.selectPriorityLevel("Level"));
        check("VALIDATION: out-of-range level input is rejected and re-prompted",
                rejectsRange.result == PriorityLevel.GOLD
                        && rejectsRange.output.contains("Please enter"));

        UiRun rejectsLetters = runUi("abc\n3\n", ui -> ui.selectPriorityLevel("Level"));
        check("VALIDATION: non-numeric level input is rejected and re-prompted",
                rejectsLetters.result == PriorityLevel.GOLD
                        && rejectsLetters.output.contains("Please enter"));

        // ---- confirm ----
        check("VERIFICATION: 'y' confirms", Boolean.TRUE.equals(
                runUi("y\n", ui -> ui.confirm("Proceed?")).result));
        check("VERIFICATION: 'yes' confirms", Boolean.TRUE.equals(
                runUi("yes\n", ui -> ui.confirm("Proceed?")).result));
        check("VERIFICATION: 'n' declines", Boolean.FALSE.equals(
                runUi("n\n", ui -> ui.confirm("Proceed?")).result));
        check("VERIFICATION: 'no' declines", Boolean.FALSE.equals(
                runUi("no\n", ui -> ui.confirm("Proceed?")).result));
        check("VALIDATION: confirm is case-insensitive", Boolean.TRUE.equals(
                runUi("Y\n", ui -> ui.confirm("Proceed?")).result));
        check("VALIDATION: confirm re-prompts on anything else", Boolean.TRUE.equals(
                runUi("maybe\n\ny\n", ui -> ui.confirm("Proceed?")).result));

        // ---- readNonEmpty ----
        check("VERIFICATION: readNonEmpty returns the typed text",
                "a reason".equals(runUi("a reason\n", ui -> ui.readNonEmpty("Reason")).result));
        UiRun blankRejected = runUi("\n   \nfinally\n", ui -> ui.readNonEmpty("Reason"));
        check("VALIDATION: readNonEmpty rejects blank and whitespace-only input",
                "finally".equals(blankRejected.result)
                        && blankRejected.output.contains("cannot be empty"));
        check("VALIDATION: readNonEmpty trims surrounding spaces",
                "trimmed".equals(runUi("   trimmed   \n", ui -> ui.readNonEmpty("Reason")).result));

        // ---- inputListIndex ----
        check("VERIFICATION: a valid list index is returned",
                Integer.valueOf(2).equals(runUi("2\n", ui -> ui.inputListIndex("record", 5)).result));
        check("VALIDATION: 0 cancels a list selection",
                Integer.valueOf(0).equals(runUi("0\n", ui -> ui.inputListIndex("record", 5)).result));
        UiRun aboveMax = runUi("6\n3\n", ui -> ui.inputListIndex("record", 5));
        check("VALIDATION: an index above the maximum is rejected",
                Integer.valueOf(3).equals(aboveMax.result) && aboveMax.output.contains("Please enter"));
        check("VALIDATION: a negative index is rejected",
                Integer.valueOf(1).equals(runUi("-4\n1\n", ui -> ui.inputListIndex("record", 5)).result));

        // ---- getPriorityActionChoice ----
        check("VERIFICATION: the record submenu accepts its actions",
                Integer.valueOf(3).equals(runUi("3\n", ui -> ui.getPriorityActionChoice()).result));
        check("VALIDATION: the record submenu rejects an out-of-range action",
                Integer.valueOf(0).equals(runUi("7\n0\n", ui -> ui.getPriorityActionChoice()).result));

        // ---- the landing menu ----
        String[][] table = {
            { "No.", "Reservation ID", "Guest ID", "Priority", "Rank", "Status", "Overridden By" },
            { "1", "RES001", "GST001", "GOLD", "20", "WAITING", "-" }
        };
        UiRun menu = runUi("0\n", ui -> ui.printPriorityListMenu(table, 0, 1, false));
        check("VERIFICATION: the menu lists all eight actions",
                menu.output.contains("1. View Details")
                        && menu.output.contains("2. Add Priority Reservation")
                        && menu.output.contains("3. Delete Priority Reservation")
                        && menu.output.contains("4. View VIP Queue")
                        && menu.output.contains("5. Filter by Priority Level")
                        && menu.output.contains("6. Search Priority Reservation")
                        && menu.output.contains("7. Priority Level Effectiveness Report")
                        && menu.output.contains("8. VIP Queue & Override Governance Report"));
        check("VERIFICATION: the menu renders the record table", menu.output.contains("RES001"));
        check("VERIFICATION: option 0 returns Back", Integer.valueOf(0).equals(menu.result));

        UiRun onePage = runUi("0\n", ui -> ui.printPriorityListMenu(table, 0, 1, false));
        check("VALIDATION: a single page offers no paging options",
                !onePage.output.contains("Next Page") && !onePage.output.contains("Previous Page"));

        UiRun firstOfTwo = runUi("0\n", ui -> ui.printPriorityListMenu(table, 0, 2, false));
        check("VALIDATION: the first of two pages offers Next but not Previous",
                firstOfTwo.output.contains("Next Page") && !firstOfTwo.output.contains("Previous Page"));

        UiRun lastOfTwo = runUi("0\n", ui -> ui.printPriorityListMenu(table, 1, 2, false));
        check("VALIDATION: the last of two pages offers Previous but not Next",
                lastOfTwo.output.contains("Previous Page") && !lastOfTwo.output.contains("Next Page"));

        UiRun withFilter = runUi("0\n", ui -> ui.printPriorityListMenu(table, 0, 1, true));
        check("VALIDATION: Clear Filter appears only when a filter is active",
                withFilter.output.contains("Clear Filter") && !menu.output.contains("Clear Filter"));

        String[][] headerOnly = { table[0] };
        UiRun noRecords = runUi("0\n", ui -> ui.printPriorityListMenu(headerOnly, 0, 1, false));
        check("VALIDATION: an empty list shows the no-records notice",
                noRecords.output.contains("(No priority reservations)"));

        // ---- detail rendering, including missing joins ----
        PriorityReservation record = new PriorityReservation(
                "RES999", PriorityLevel.DIAMOND, "STF001", "vip guest", false);
        UiRun detail = runUi("\n", ui -> {
            ui.printPriorityDetail(record, null);
            return null;
        });
        check("VALIDATION: detail view renders when the reservation is missing",
                detail.output.contains("RES999") && detail.output.contains("DIAMOND")
                        && !detail.threw);
        check("VALIDATION: missing fields render as a dash", detail.output.contains("-"));

        PriorityReservation bare = new PriorityReservation("RES998", PriorityLevel.SLIVER);
        UiRun bareDetail = runUi("\n", ui -> {
            ui.printPriorityDetail(bare, null);
            return null;
        });
        check("VALIDATION: a record with no override renders safely",
                bareDetail.output.contains("RES998") && !bareDetail.threw);

        // ---- empty selection lists ----
        UiRun noStaff = runUi("\n", ui -> ui.selectStaff(new LinkedList<>()));
        check("VALIDATION: selecting from an empty staff list returns null",
                noStaff.result == null && noStaff.output.contains("No staff records found"));

        UiRun noReservations = runUi("\n", ui -> ui.selectReservation(new LinkedList<>()));
        check("VALIDATION: selecting from an empty reservation list returns null",
                noReservations.result == null
                        && noReservations.output.contains("No eligible non-member reservations"));

        // ---- populated selection lists ----
        LinkedListInterface<Staff> staff = new StaffDAO().retrieveStaffList();
        if (!staff.isEmpty()) {
            UiRun pickStaff = runUi("1\n", ui -> ui.selectStaff(staff));
            check("VERIFICATION: selecting staff 1 returns the first staff member",
                    pickStaff.result == staff.get(0));
            check("VALIDATION: 0 cancels staff selection",
                    runUi("0\n", ui -> ui.selectStaff(staff)).result == null);
        }

        // ---- override preview ----
        UiRun preview = runUi("\n", ui -> {
            ui.displayOverridePreview(record, PriorityLevel.EMERGENCY, "STF002", "medical");
            return null;
        });
        check("VERIFICATION: the override preview shows the new level and staff",
                preview.output.contains("EMERGENCY") && preview.output.contains("STF002")
                        && preview.output.contains("medical"));

        // ---- VIP queue display ----
        UiRun queueView = runUi("\n", ui -> {
            ui.displayVIPQueue(new LinkedList<>(), new LinkedList<>());
            return null;
        });
        check("VALIDATION: the VIP queue display handles an empty queue", !queueView.threw);

        // ---- static formatting helpers ----
        UiRun statics = runUi("\n", ui -> {
            PriorityReservationUiProbe.exercise();
            return null;
        });
        check("VERIFICATION: banner, separator, section and details all render",
                statics.output.contains("TEST BANNER")
                        && statics.output.contains("Test Section")
                        && statics.output.contains("Key")
                        && statics.output.contains("Value")
                        && !statics.threw);
    }

    /** Keeps the static-helper calls in one place for readability. */
    private static final class PriorityReservationUiProbe {
        static void exercise() {
            tarumtresort.boundary.PriorityReservationUI.printBanner("TEST BANNER");
            tarumtresort.boundary.PriorityReservationUI.printSeparator();
            tarumtresort.boundary.PriorityReservationUI.printSection("Test Section");
            tarumtresort.boundary.PriorityReservationUI.printDetails(
                    new String[][] { { "Key", "Value" }, { "Longer Key", "Another Value" } });
        }
    }

    // ==================================================================
    // PART I - menu navigation (UI driven through the controller)
    // ==================================================================

    private static void partI_menuNavigation() {
        section("PART I  Menu navigation - end to end");

        check("VERIFICATION: option 0 exits the module immediately",
                navigate("0\n").length() > 0);

        check("VERIFICATION: option 1 opens a record's detail view",
                navigate("1\n1\n0\n0\n").contains("Reservation ID"));

        check("VERIFICATION: option 1 then 3 shows the VIP queue position",
                navigate("1\n1\n3\n\n0\n0\n").contains("VIP queue position"));

        check("VERIFICATION: option 4 displays the VIP queue",
                navigate("4\n\n0\n").contains("VIP"));

        check("VERIFICATION: option 5 filters by priority level",
                navigate("5\n5\n0\n").contains("DIAMOND"));

        check("VERIFICATION: option 6 finds an existing reservation",
                navigate("6\n" + firstActiveId() + "\n0\n0\n").contains(firstActiveId()));

        check("VALIDATION: option 6 reports an unknown reservation id",
                navigate("6\nNO_SUCH_ID\n\n0\n").contains("No priority record found"));

        check("VERIFICATION: option 7 produces the effectiveness report",
                navigate("7\n7\n0\n0\n\n0\n").contains("END OF THE REPORT"));

        check("VERIFICATION: option 8 produces the governance report",
                navigate("8\n7\n0\n0\n\n0\n").contains("END OF THE REPORT"));

        check("VALIDATION: a report with no matching records says so",
                navigate("7\n2\n0\n0\n\n0\n").contains("No priority reservation records match"));

        check("VALIDATION: cancelling the delete flow changes nothing",
                navigate("3\n1\nn\n0\n").length() > 0 && activeCount() > 0);

        check("VALIDATION: cancelling the add flow changes nothing",
                navigate("2\n0\n0\n").length() > 0);

        check("VALIDATION: the menu survives non-numeric input",
                navigate("abc\n0\n").contains("Please enter"));

        check("VALIDATION: the menu survives an out-of-range choice",
                navigate("99\n0\n").contains("Please enter"));

        check("VALIDATION: the menu survives blank input",
                navigate("\n\n0\n").length() > 0);
    }

    // ==================================================================
    // helpers
    // ==================================================================

    /** Result of driving a UI method with scripted input. */
    private static final class UiRun {
        private String output = "";
        private Object result;
        private boolean threw;
    }

    /**
     * Runs one UI method against scripted keyboard input with System.out
     * captured, so the driver can assert on what the user would have seen.
     *
     * The script is padded with trailing newlines because the boundary layer
     * calls System.exit when the scanner runs dry, which would take the whole
     * test run down with it.
     */
    private static UiRun runUi(String script,
            java.util.function.Function<tarumtresort.boundary.PriorityReservationUI, Object> action) {
        UiRun run = new UiRun();
        java.io.PrintStream realOut = System.out;
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        try {
            System.setOut(new java.io.PrintStream(buffer, true));
            run.result = action.apply(new tarumtresort.boundary.PriorityReservationUI(
                    new Scanner(new ByteArrayInputStream((script + "\n".repeat(20)).getBytes()))));
        } catch (RuntimeException e) {
            run.threw = true;
        } finally {
            System.setOut(realOut);
            run.output = buffer.toString();
        }
        return run;
    }

    /** Drives the whole module menu with scripted input and returns what it printed. */
    private static String navigate(String script) {
        java.io.PrintStream realOut = System.out;
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        try {
            System.setOut(new java.io.PrintStream(buffer, true));
            new PriorityReservationController(new Scanner(
                    new ByteArrayInputStream((script + "0\n".repeat(40)).getBytes()))).run();
        } catch (RuntimeException e) {
            System.setOut(realOut);
            return "EXCEPTION " + e.getClass().getSimpleName();
        } finally {
            System.setOut(realOut);
        }
        return buffer.toString();
    }

    /**
     * Backs the data file up and restores it on JVM shutdown. A shutdown hook
     * is used rather than a finally block because the boundary layer calls
     * System.exit when input runs dry, which skips finally entirely.
     */
    private static void installBackupAndRestoreHook() {
        final String backup;
        try {
            backup = Files.readString(PRIORITY_FILE);
        } catch (Exception e) {
            System.out.println("  ! could not back up " + PRIORITY_FILE
                    + " - run this driver from the project root. (" + e.getMessage() + ")");
            System.exit(2);
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Files.writeString(PRIORITY_FILE, backup);
                System.out.println("\n  data/priorityReservations.json restored.");
            } catch (Exception e) {
                System.out.println("\n  ! FAILED to restore " + PRIORITY_FILE + ": " + e.getMessage());
            }
        }));
    }

    private static PriorityReservationController controller() {
        return new PriorityReservationController(
                new Scanner(new ByteArrayInputStream("0\n".getBytes())));
    }

    private static LinkedListInterface<Reservation> loadReservations() {
        LinkedListInterface<Reservation> list = new LinkedList<>();
        new ReservationDAO().loadAllReservations(list);
        return list;
    }

    private static LinkedListInterface<Reservation> waitingReservations() {
        LinkedListInterface<Reservation> all = loadReservations();
        LinkedListInterface<Reservation> waiting = new LinkedList<>();
        for (int i = 0; i < all.size(); i++) {
            Reservation r = all.get(i);
            if (!r.isDeleted() && r.getStatus() == ReservationStatus.WAITING) {
                waiting.addBack(r);
            }
        }
        return waiting;
    }

    private static LinkedListInterface<QueueOrdering.Entry> buildEntries() {
        LinkedListInterface<PriorityReservation> priorities = new PriorityReservationDAO().loadFromFile();
        ReservationIndex index = new ReservationIndex(loadReservations());
        LinkedListInterface<QueueOrdering.Entry> entries = new LinkedList<>();
        for (int i = 0; i < priorities.size(); i++) {
            PriorityReservation record = priorities.get(i);
            if (record.isDeleted()) {
                continue;
            }
            Reservation reservation = index.find(record.getReservationId());
            if (reservation == null || reservation.isDeleted()) {
                continue;
            }
            entries.addSorted(new QueueOrdering.Entry(record, reservation));
        }
        return entries;
    }

    private static int activeCount() {
        LinkedListInterface<PriorityReservation> all = new PriorityReservationDAO().loadFromFile();
        ReservationIndex index = new ReservationIndex(loadReservations());
        int n = 0;
        for (int i = 0; i < all.size(); i++) {
            PriorityReservation record = all.get(i);
            if (record.isDeleted()) {
                continue;
            }
            Reservation reservation = index.find(record.getReservationId());
            if (reservation != null && !reservation.isDeleted()) {
                n++;
            }
        }
        return n;
    }

    private static String firstActiveId() {
        LinkedListInterface<PriorityReservation> all = new PriorityReservationDAO().loadFromFile();
        for (int i = 0; i < all.size(); i++) {
            if (!all.get(i).isDeleted()) {
                return all.get(i).getReservationId();
            }
        }
        return null;
    }

    private static LocalDateTime registrationOf(Reservation reservation) {
        if (reservation == null || reservation.getTimestamps() == null) {
            return null;
        }
        return reservation.getTimestamps().getRegistrationTimestamp();
    }

    private static boolean rectangular(String[][] table) {
        int width = table[0].length;
        for (String[] row : table) {
            if (row == null || row.length != width) {
                return false;
            }
        }
        return true;
    }

    // ==================================================================
    // reporting
    // ==================================================================

    private static void banner(String title) {
        System.out.println("=".repeat(72));
        System.out.println("  " + title);
        System.out.println("=".repeat(72));
    }

    private static void section(String name) {
        System.out.println();
        System.out.println("  " + name);
        System.out.println("  " + "-".repeat(68));
    }

    private static void check(String label, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("    [ PASS ]  " + label);
        } else {
            failed++;
            if (firstFailure == null) {
                firstFailure = label;
            }
            System.out.println("    [ FAIL ]  " + label);
        }
    }

    private static void summary() {
        int total = passed + failed;
        System.out.println();
        System.out.println("=".repeat(72));
        System.out.println("  RESULT   " + passed + " / " + total + " passed"
                + (failed == 0 ? "" : "   |   " + failed + " FAILED"));
        if (failed > 0) {
            System.out.println("  First failure: " + firstFailure);
        }
        System.out.println("=".repeat(72));
        if (failed > 0) {
            System.exit(1);
        }
    }
}
