package tarumtresort.report.HousekeepingReport;

import java.time.LocalDateTime;
import java.util.Scanner;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.dao.RoomDAO;
import tarumtresort.dao.StaffDAO;
import tarumtresort.dao.TaskAssignmentChangeDAO;
import tarumtresort.dao.TaskAssignmentDAO;
import tarumtresort.dao.TaskDAO;
import tarumtresort.entity.Room;
import tarumtresort.entity.Staff;
import tarumtresort.entity.Task;
import tarumtresort.entity.TaskAssignment;
import tarumtresort.entity.TaskAssignmentChange;

public class HousekeepingReportController {

    private final RoomDAO roomDAO = new RoomDAO();
    private final StaffDAO staffDAO = new StaffDAO();
    private final TaskDAO taskDAO = new TaskDAO();
    private final TaskAssignmentDAO taskAssignmentDAO = new TaskAssignmentDAO();
    private final TaskAssignmentChangeDAO taskAssignmentChangeDAO = new TaskAssignmentChangeDAO();

    private final Scanner scanner;

    public HousekeepingReportController(Scanner scanner) {
        this.scanner = scanner;
    }

    public void generateRoomCleaningPerformanceReport() {
        LocalDateTime[] range = ui().inputOptionalDateTimeRange("task start");

        LinkedListInterface<Room> rooms = new LinkedList<>();
        roomDAO.loadFromFile(rooms);
        LinkedListInterface<Task> tasks = taskDAO.retrieveTaskList();
        LinkedListInterface<TaskAssignmentChange> changes = taskAssignmentChangeDAO.retrieveTaskAssignmentChangeList();

        RoomCleaningPerformanceReport.Result result = new RoomCleaningPerformanceReport(
                rooms, tasks, changes).generate(range[0], range[1]);
        new RoomCleaningPerformanceUI(ui()).render(result);
        ui().pressEnterToContinue();
    }

    public void generateStaffProductivityReport() {
        LocalDateTime[] range = ui().inputOptionalDateTimeRange("date & time assigned");

        LinkedListInterface<Staff> staffs = staffDAO.retrieveStaffList();
        LinkedListInterface<Task> tasks = taskDAO.retrieveTaskList();
        LinkedListInterface<TaskAssignment> assignments = taskAssignmentDAO.retrieveTaskAssignmentList();
        LinkedListInterface<TaskAssignmentChange> changes = taskAssignmentChangeDAO.retrieveTaskAssignmentChangeList();

        StaffProductivityReport.Result result = new StaffProductivityReport(
                staffs, tasks, assignments, changes).generate(range[0], range[1]);
        new StaffProductivityUI(ui()).render(result);
        ui().pressEnterToContinue();
    }

    private HousekeepingReportUI ui() {
        return new HousekeepingReportUI(scanner);
    }
}