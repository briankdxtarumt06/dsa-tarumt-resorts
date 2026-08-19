package tarumtresort.report;

import java.time.LocalDateTime;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.dao.ReservationDAO;
import tarumtresort.dao.RoomDAO;
import tarumtresort.dao.StaffDAO;
import tarumtresort.dao.TaskAssignmentChangeDAO;
import tarumtresort.dao.TaskAssignmentDAO;
import tarumtresort.dao.TaskDAO;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.Room;
import tarumtresort.entity.Staff;
import tarumtresort.entity.Task;
import tarumtresort.entity.TaskAssignment;
import tarumtresort.entity.TaskAssignmentChange;

/**
 *
 * @author Brian
 *
 * Shared report driver: runs the reports submenu, loads DAO data, and
 * delegates to the two remaining report classes (Room Turnover, Staff Productivity).
 */
public class ReportMenu {

    private final ReportUI ui;

    // DAOs
    private final RoomDAO roomDAO = new RoomDAO();
    private final StaffDAO staffDAO = new StaffDAO();
    private final TaskDAO taskDAO = new TaskDAO();
    private final TaskAssignmentDAO taskAssignmentDAO = new TaskAssignmentDAO();
    private final TaskAssignmentChangeDAO taskAssignmentChangeDAO = new TaskAssignmentChangeDAO();
    private final ReservationDAO reservationDAO = new ReservationDAO();

    public ReportMenu(ReportUI ui) {
        this.ui = ui;
    }

    public void run() {
        int choice;

        do {
            choice = ui.getReportMenuChoice();

            switch (choice) {
                case 1:
                    generateRoomTurnoverReportMenu();
                    break;
                case 2:
                    generateStaffProductivityReportMenu();
                    break;
                case 0:
                    break;
            }

            if (choice != 0) {
                ui.pressEnterToContinue();
            }
        } while (choice != 0);
    }

    /**
     * Room Turnover &amp; Readiness Report (Room + Task + Assignment + Change + Reservation).
     * Filters: date range (task start).
     */
    private void generateRoomTurnoverReportMenu() {
        LocalDateTime[] range = ui.inputOptionalDateTimeRange("task start");

        LinkedListInterface<Room> rooms = new LinkedList<>();
        roomDAO.loadFromFile(rooms);
        LinkedListInterface<Task> tasks = taskDAO.retrieveTaskList();
        LinkedListInterface<TaskAssignment> assignments = taskAssignmentDAO.retrieveTaskAssignmentList();
        LinkedListInterface<TaskAssignmentChange> changes = taskAssignmentChangeDAO.retrieveTaskAssignmentChangeList();
        LinkedListInterface<Reservation> reservations = new LinkedList<>();
        reservationDAO.loadAllReservations(reservations);

        ReportResult result = new RoomTurnoverReport(rooms, tasks, assignments, changes, reservations)
                .generate(range[0], range[1]);
        ui.printReport(result, "ROOM TURNOVER & READINESS REPORT");
    }

    /**
     * Staff Productivity &amp; Reassignment Report (Staff + Task + Assignment + Change).
     * Filters: date range (date &amp; time assigned).
     */
    private void generateStaffProductivityReportMenu() {
        LocalDateTime[] range = ui.inputOptionalDateTimeRange("date & time assigned");

        LinkedListInterface<Staff> staffs = staffDAO.retrieveStaffList();
        LinkedListInterface<Task> tasks = taskDAO.retrieveTaskList();
        LinkedListInterface<TaskAssignment> assignments = taskAssignmentDAO.retrieveTaskAssignmentList();
        LinkedListInterface<TaskAssignmentChange> changes = taskAssignmentChangeDAO.retrieveTaskAssignmentChangeList();

        ReportResult result = new StaffProductivityReport(staffs, tasks, assignments, changes)
                .generate(range[0], range[1]);
        ui.printReport(result, "STAFF PRODUCTIVITY & REASSIGNMENT REPORT");
    }
}
