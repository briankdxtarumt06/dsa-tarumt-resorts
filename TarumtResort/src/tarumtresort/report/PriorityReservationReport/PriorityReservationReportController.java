package tarumtresort.report.PriorityReservationReport;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.dao.PriorityReservationDAO;
import tarumtresort.dao.ReservationDAO;
import tarumtresort.dao.StaffDAO;
import tarumtresort.entity.PriorityReservation;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.Staff;
import tarumtresort.entity.enums.PriorityLevel;
import tarumtresort.entity.enums.ReservationStatus;
import tarumtresort.entity.enums.RoomType;

// Author: Lee Boon Yew
/**
 * Report driver for the Priority Reservation module.
 *
 * Collects the filters, loads the DAO data, hands both to the report class,
 * and passes the finished Result to the render UI. No metric is calculated
 * here and none is calculated in PriorityReservationController - the module
 * controller only calls one of the two generate methods below.
 */
public class PriorityReservationReportController {

    private static final DateTimeFormatter FILTER_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final PriorityReservationDAO priorityReservationDAO = new PriorityReservationDAO();
    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final StaffDAO staffDAO = new StaffDAO();

    private final Scanner scanner;

    public PriorityReservationReportController(Scanner scanner) {
        this.scanner = scanner;
    }

    /**
     * Report 1 - is the priority level actually buying faster service?
     * Filters: registration date range + reservation status + room type.
     */
    public void generatePriorityLevelEffectivenessReport() {
        PriorityReservationReportUI ui = ui();

        LocalDateTime[] range = ui.inputOptionalDateTimeRange("reservation registration");
        ReservationStatus statusFilter = ui.selectStatusFilter();
        RoomType roomTypeFilter = ui.selectRoomTypeFilter();

        PriorityLevelEffectivenessReport.Result result = new PriorityLevelEffectivenessReport(
                loadPriorityReservations(), loadReservations())
                .generate(range[0], range[1], statusFilter, roomTypeFilter);

        new PriorityLevelEffectivenessUI(ui).render(result, new String[] {
            "Registration period : " + describeRange(range),
            "Reservation status  : " + (statusFilter == null ? "All statuses" : statusFilter.name()),
            "Room type requested : " + (roomTypeFilter == null ? "All room types" : roomTypeFilter.name()),
            "Queue positions     : measured against the full active queue, not the filtered subset"
        });
        ui.pressEnterToContinue();
    }

    /**
     * Report 2 - who is bypassing the loyalty tier rules, and at whose expense?
     * Filters: registration date range + minimum priority level + override scope.
     */
    public void generateVipQueueGovernanceReport() {
        PriorityReservationReportUI ui = ui();

        LocalDateTime[] range = ui.inputOptionalDateTimeRange("reservation registration");
        PriorityLevel minLevel = ui.selectMinimumPriorityLevel();
        int overrideScope = ui.selectOverrideScope();

        VipQueueGovernanceReport.Result result = new VipQueueGovernanceReport(
                loadPriorityReservations(), loadReservations(), staffDAO.retrieveStaffList())
                .generate(range[0], range[1], minLevel, overrideScope);

        new VipQueueGovernanceUI(ui).render(result, new String[] {
            "Registration period : " + describeRange(range),
            "Minimum priority    : " + (minLevel == null
                    ? "All levels" : minLevel.name() + " and above (rank " + minLevel.getRank() + "+)"),
            "Override scope      : " + describeScope(overrideScope),
            "Queue positions     : measured against the full active queue, not the filtered subset"
        });
        ui.pressEnterToContinue();
    }

    // -------------------- data loading --------------------

    private LinkedListInterface<PriorityReservation> loadPriorityReservations() {
        return priorityReservationDAO.loadFromFile();
    }

    private LinkedListInterface<Reservation> loadReservations() {
        LinkedListInterface<Reservation> reservations = new LinkedList<>();
        reservationDAO.loadAllReservations(reservations);
        return reservations;
    }

    // -------------------- filter descriptions --------------------

    private String describeRange(LocalDateTime[] range) {
        if (range == null || (range[0] == null && range[1] == null)) {
            return "All time (no limit)";
        }
        String from = range[0] == null ? "earliest" : range[0].format(FILTER_FMT);
        String to = range[1] == null ? "now" : range[1].format(FILTER_FMT);
        return from + "  to  " + to;
    }

    private String describeScope(int overrideScope) {
        return switch (overrideScope) {
            case 1 -> "Staff-overridden records only";
            case 2 -> "Loyalty-tier records only (not overridden)";
            default -> "All records";
        };
    }

    private PriorityReservationReportUI ui() {
        return new PriorityReservationReportUI(scanner);
    }
}
