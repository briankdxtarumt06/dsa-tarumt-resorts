package tarumtresort.control;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.boundary.HousekeepingUI;
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
import tarumtresort.entity.enums.RoomType;
import tarumtresort.entity.enums.TaskPriority;
import tarumtresort.entity.enums.TaskStatus;
import tarumtresort.report.ReportResult;
import tarumtresort.report.RoomCleaningReport;
import tarumtresort.report.StaffWorkloadReport;

/**
 *
 * @author Brian
 *
 *         Scheduling rules (schedule gap analysis):
 *         - Every room takes CLEANING_DURATION_MINUTES to clean, so every
 *         cleaning
 *         task requires a continuous free interval of that length.
 *         - A staff is "available" at an interval when they have no
 *         non-cancelled
 *         assignment covering any part of it (different task only; members of
 *         the
 *         same task may share a window as a team).
 *         - No two tasks of the same staff may share a timestamp; a task may
 *         only
 *         start after the previous one is done.
 *         - The analysis scans each eligible staff's schedule chronologically
 *         (within their SHIFT_START..SHIFT_END shift boundaries) to find the
 *         earliest continuous free gap large enough for the task. The staff who
 *         becomes available first wins; ties are broken by current workload,
 *         then
 *         lowest staff id. If no gap fits today, the task is deferred to the
 *         next
 *         shift start so the worker always has the task on their schedule.
 *         - Slot booking is done as one load -> compute -> insert -> save step
 *         against the JSON files (single-writer console), so no double-booking
 *         is
 *         possible in the current architecture.
 *
 *         FUTURE INTEGRATION:
 *         - processGuestCheckout() is the hook for the Reservation module to
 *         call
 *         on real guest checkout; validate roomId against RoomDAO and update
 *         RoomStatus (CLEANING / AVILABLE) once room persistence exists.
 *         - Shift hours (SHIFT_START / SHIFT_END) are default constants; move
 *         them
 *         into the Staff entity + Staff Management UI for per-staff shifts.
 *         - When the system gains multiple writers, replace the
 *         load-compute-save
 *         flow with a real transactional database / file lock.
 *         - Trigger Notification records for assigned staff; query change
 *         history
 *         by date range for supervisor reports.
 *         - Supervisor approval flow can be gated by staff role once auth
 *         exists.
 */
public class HousekeepingController {

    // every room takes 60 minutes to clean (default task duration)
    public static final int CLEANING_DURATION_MINUTES = 60;

    // default shift boundaries for schedule gap analysis
    public static final LocalTime SHIFT_START = LocalTime.of(8, 0);
    public static final LocalTime SHIFT_END = LocalTime.of(20, 0);

    // safety bound when deferring tasks across shifts
    private static final int MAX_DEFER_DAYS = 30;

    // ui declaration
    private HousekeepingUI ui;

    // list declaration
    private LinkedListInterface<TaskAssignment> taskAssignmentList = new LinkedList<>();
    private LinkedListInterface<TaskAssignmentChange> taskAssignmentChangeList = new LinkedList<>();
    private LinkedListInterface<Staff> staffList = new LinkedList<>();
    private LinkedListInterface<Task> taskList = new LinkedList<>();

    // dao declarations
    private static final StaffDAO staffDAO = new StaffDAO();
    private static final TaskDAO taskDAO = new TaskDAO();
    private static final TaskAssignmentDAO taskAssignmentDAO = new TaskAssignmentDAO();
    private static final TaskAssignmentChangeDAO taskAssignmentChangeDAO = new TaskAssignmentChangeDAO();
    private static final RoomDAO roomDAO = new RoomDAO();

    // constructors
    public HousekeepingController() {
        staffList = staffDAO.retrieveStaffList();
        taskList = taskDAO.retrieveTaskList();
        taskAssignmentList = taskAssignmentDAO.retrieveTaskAssignmentList();
        taskAssignmentChangeList = taskAssignmentChangeDAO.retrieveTaskAssignmentChangeList();
    }

    public HousekeepingController(HousekeepingUI ui) {
        this.ui = ui;
        staffList = staffDAO.retrieveStaffList();
        taskList = taskDAO.retrieveTaskList();
        taskAssignmentList = taskAssignmentDAO.retrieveTaskAssignmentList();
        taskAssignmentChangeList = taskAssignmentChangeDAO.retrieveTaskAssignmentChangeList();
    }

    private void refreshTaskAssignments() {
        // reduce syncing issue, always get latest data
        taskAssignmentList = taskAssignmentDAO.retrieveTaskAssignmentList();
    }

    private void refreshTaskAssignmentChanges() {
        // reduce syncing issue, always get latest data
        taskAssignmentChangeList = taskAssignmentChangeDAO.retrieveTaskAssignmentChangeList();
    }

    // entry point for housekeeping module
    public void runHousekeeping() {

        int choice;

        do {
            choice = ui.getMenuChoice();

            switch (choice) {
                case 1:
                    runStaffManagement();
                    break;
                case 2:
                    runTaskManagement();
                    break;
                case 3:
                    runAssignmentManagement();
                    break;
                case 4:
                    runReports();
                    break;
                case 0:
                    ui.printExitMessage();
                    break;
                default:
                    ui.printInvalidChoice();
            }
        } while (choice != 0);
    }

    // entry point for the reports
    public void runReports() {

        int choice;

        do {
            choice = ui.getReportMenuChoice();

            switch (choice) {
                case 1:
                    generateRoomCleaningReportMenu();
                    break;
                case 2:
                    generateStaffWorkloadReportMenu();
                    break;
                case 0:
                    break;
                default:
                    ui.printInvalidChoice();
            }

            if (choice != 0) {
                ui.pressEnterToContinue();
            }
        } while (choice != 0);
    }

    // entry point for staff management
    public void runStaffManagement() {

        int choice;

        do {
            choice = ui.getStaffMenuChoice();

            switch (choice) {
                case 1:
                    addStaffMenu();
                    break;
                case 2:
                    ui.listAllStaffs(staffListToTable(getAllStaffs()));
                    break;
                case 3:
                    searchStaffMenu();
                    break;
                case 4:
                    updateStaffMenu();
                    break;
                case 5:
                    resignStaffMenu();
                    break;
                case 6:
                    filterStaffByDepartmentMenu();
                    break;
                case 7:
                    filterStaffByAvailabilityMenu();
                    break;
                case 0:
                    break;
                default:
                    ui.printInvalidChoice();
            }

            if (choice != 0) {
                ui.pressEnterToContinue();
            }
        } while (choice != 0);
    }

    public String createStaff(String staffName, String department, String staffRole, String availabilityStatus) {

        // staff name cannot be duplicated
        for (int i = 0; i < staffList.size(); i++) { // size() = current record count of the list
            if (staffList.get(i).getStaffName().equalsIgnoreCase(staffName)) { // get(i) = record at index i
                return null;
            }
        }

        String staffId = generateStaffId();

        Staff staff = new Staff(
                staffId,
                staffName,
                department,
                staffRole,
                availabilityStatus);

        staffList.addSorted(staff); // insert the record, keeping the list sorted by staff ID
        staffDAO.saveStaffList(staffList);

        return staffId;
    }

    public boolean updateStaff(String staffId,
            String staffName,
            String department,
            String staffRole,
            String availabilityStatus) {

        for (int i = 0; i < staffList.size(); i++) { // size() = current record count of the list
            Staff staff = staffList.get(i); // get(i) = record at index i

            if (staff.getStaffId().equals(staffId)) {

                staff.setStaffName(staffName);
                staff.setDepartment(department);
                staff.setStaffRole(staffRole);
                staff.setAvailabilityStatus(availabilityStatus);

                staffDAO.saveStaffList(staffList);

                return true;
            }
        }

        return false;
    }

    public boolean resignStaff(String staffId) {

        for (int i = 0; i < staffList.size(); i++) { // size() = current record count of the list
            Staff staff = staffList.get(i); // get(i) = record at index i

            if (staff.getStaffId().equals(staffId)) {

                staff.setAvailabilityStatus("Resigned"); // soft delete

                staffDAO.saveStaffList(staffList);

                return true;
            }
        }

        return false;
    }

    public Staff getStaffById(String staffId) {

        for (int i = 0; i < staffList.size(); i++) { // size() = current record count of the list
            Staff staff = staffList.get(i); // get(i) = record at index i

            if (staff.getStaffId().equals(staffId)) {
                return staff;
            }
        }

        return null;
    }

    public Staff getStaffByName(String staffName) {

        for (int i = 0; i < staffList.size(); i++) { // size() = current record count of the list
            Staff staff = staffList.get(i); // get(i) = record at index i

            if (staff.getStaffName().equalsIgnoreCase(staffName)) {
                return staff;
            }
        }

        return null;
    }

    public LinkedListInterface<Staff> getStaffsByDepartment(String department) {

        LinkedListInterface<Staff> filteredList = new LinkedList<>();

        for (int i = 0; i < staffList.size(); i++) { // size() = current record count of the list
            Staff staff = staffList.get(i); // get(i) = record at index i

            if (staff.getDepartment().equalsIgnoreCase(department)) {
                filteredList.addBack(staff); // append the matching record to the end of the result list
            }
        }

        return filteredList;
    }

    public LinkedListInterface<Staff> getStaffsByAvailability(String availabilityStatus) {

        LinkedListInterface<Staff> filteredList = new LinkedList<>();

        for (int i = 0; i < staffList.size(); i++) { // size() = current record count of the list
            Staff staff = staffList.get(i); // get(i) = record at index i

            if (staff.getAvailabilityStatus().equalsIgnoreCase(availabilityStatus)) {
                filteredList.addBack(staff); // append the matching record to the end of the result list
            }
        }

        return filteredList;
    }

    public LinkedListInterface<Staff> getAllStaffs() {
        return staffList;
    }

    // -------------------- private menu handlers --------------------

    private void addStaffMenu() {
        String[] details = ui.inputStaffDetails();
        String staffId = createStaff(details[0], details[1], details[2], details[3]);
        if (staffId == null) {
            ui.printDuplicateName();
        } else {
            ui.printStaffId(staffId);
            ui.printSuccess();
        }
    }

    private void searchStaffMenu() {
        int searchChoice = ui.getStaffSearchMenuChoice();
        if (searchChoice == 0) {
            return;
        }
        Staff staff = null;
        if (searchChoice == 1) {
            staff = getStaffById(ui.inputStaffId());
        } else if (searchChoice == 2) {
            staff = getStaffByName(ui.inputStaffName());
        }
        if (staff == null) {
            ui.printNotFound();
        } else {
            ui.printStaffDetails(staff);
        }
    }

    private void updateStaffMenu() {
        String staffId = ui.inputStaffId();
        if (!staffExists(staffId)) {
            ui.printNotFound();
            return;
        }
        String[] details = ui.inputUpdateStaffDetails();
        if (updateStaff(staffId, details[0], details[1], details[2], details[3])) {
            ui.printSuccess();
        } else {
            ui.printNotFound();
        }
    }

    private void resignStaffMenu() {
        String staffId = ui.inputStaffId();
        if (resignStaff(staffId)) {
            ui.printSuccess();
        } else {
            ui.printNotFound();
        }
    }

    private void filterStaffByDepartmentMenu() {
        String department = ui.inputDepartment();
        ui.listAllStaffs(staffListToTable(getStaffsByDepartment(department)));
    }

    private void filterStaffByAvailabilityMenu() {
        String availabilityStatus = ui.inputAvailabilityStatus();
        ui.listAllStaffs(staffListToTable(getStaffsByAvailability(availabilityStatus)));
    }

    // -------------------- private helpers --------------------

    private String generateStaffId() {

        int max = 0;

        for (int i = 0; i < staffList.size(); i++) { // size() = current record count of the list
            String staffId = staffList.get(i).getStaffId(); // get(i) = record at index i

            int number = Integer.parseInt(staffId.substring(3));

            if (number > max) {
                max = number;
            }
        }

        return String.format("STF%012d", max + 1);
    }

    private boolean staffExists(String staffId) {
        return getStaffById(staffId) != null;
    }

    // convert staff list to 2D table
    private String[][] staffListToTable(LinkedListInterface<Staff> staffList) {
        String[][] data = new String[staffList.size() + 1][5]; // +1 row for the header; size() = record count
        data[0] = new String[] { "Staff ID", "Staff Name", "Department", "Staff Role", "Availability" };
        for (int i = 0; i < staffList.size(); i++) { // size() = current record count of the list
            Staff staff = staffList.get(i); // get(i) = record at index i
            data[i + 1] = new String[] {
                    staff.getStaffId(),
                    staff.getStaffName(),
                    staff.getDepartment(),
                    staff.getStaffRole(),
                    staff.getAvailabilityStatus()
            };
        }
        return data;
    }

    // entry point for task management
    public void runTaskManagement() {

        int choice;

        do {
            choice = ui.getTaskMenuChoice();

            switch (choice) {
                case 1:
                    addTaskMenu();
                    break;
                case 2:
                    ui.listAllTasks(taskListToTable(getAllTasks()));
                    break;
                case 3:
                    searchTaskMenu();
                    break;
                case 4:
                    updateTaskMenu();
                    break;
                case 5:
                    updateTaskStatusMenu();
                    break;
                case 6:
                    removeTaskMenu();
                    break;
                case 7:
                    filterTaskByPriorityMenu();
                    break;
                case 8:
                    filterTaskByTypeMenu();
                    break;
                case 0:
                    break;
                default:
                    ui.printInvalidChoice();
            }

            if (choice != 0) {
                ui.pressEnterToContinue();
            }
        } while (choice != 0);
    }

    public String createTask(String taskName, String taskType, TaskPriority taskPriority, LocalDateTime startDateTime,
            String roomId) {

        for (int i = 0; i < taskList.size(); i++) { // size() = current record count of the list
            if (taskList.get(i).getTaskName().equalsIgnoreCase(taskName)) { // get(i) = record at index i
                return null;
            }
        }

        String taskId = generateTaskId();

        Task task = new Task(
                taskId,
                taskName,
                taskType,
                null,
                taskPriority,
                startDateTime,
                roomId);

        task.setTaskStatus(TaskStatus.PENDING);
        taskList.addSorted(task); // insert the record, keeping the list sorted by priority then start time
        taskDAO.saveTaskList(taskList);

        return taskId;
    }

    // create cleaning task for guest request
    public Task createCleaningTask(String roomId, String taskType, TaskPriority priority, LocalDateTime requestedStart) {
        if (roomId == null || roomId.isBlank() || requestedStart == null) {
            return null;
        }

        String taskId = createTask("Clean " + roomId, taskType, priority, requestedStart, roomId);

        return taskId == null ? null : getTaskById(taskId);
    }

    public boolean updateTask(String taskId,
            String taskName,
            String taskType,
            TaskPriority taskPriority,
            LocalDateTime startDateTime) {

        for (int i = 0; i < taskList.size(); i++) { // size() = current record count of the list
            Task task = taskList.get(i); // get(i) = record at index i

            if (task.getTaskId().equals(taskId)) {

                task.setTaskName(taskName);
                task.setTaskType(taskType);
                task.setTaskPriority(taskPriority);
                task.setStartDateTime(startDateTime);

                taskDAO.saveTaskList(taskList);

                return true;
            }
        }

        return false;
    }

    public boolean updateTaskStatus(String taskId, String status) {

        TaskStatus taskStatus = TaskStatus.fromString(status);
        if (taskStatus == null) {
            return false;
        }

        for (int i = 0; i < taskList.size(); i++) { // size() = current record count of the list
            Task task = taskList.get(i); // get(i) = record at index i

            if (task.getTaskId().equals(taskId)) {

                task.setTaskStatus(taskStatus);

                // every task status change is recorded as a TaskAssignmentChange
                // history record (separate entity)
                appendTaskStatusChange(task, status, LocalDateTime.now());

                // FUTURE INTEGRATION: trigger a Notification for any staff
                // currently assigned to this task.

                taskDAO.saveTaskList(taskList);

                return true;
            }
        }

        return false;
    }

    public boolean updateTaskStartDateTime(String taskId, LocalDateTime startDateTime) {

        for (int i = 0; i < taskList.size(); i++) { // size() = current record count of the list
            Task task = taskList.get(i); // get(i) = record at index i

            if (task.getTaskId().equals(taskId)) {

                task.setStartDateTime(startDateTime);

                taskDAO.saveTaskList(taskList);

                return true;
            }
        }

        return false;
    }

    public boolean updateTaskRoomId(String taskId, String roomId) {

        for (int i = 0; i < taskList.size(); i++) { // size() = current record count of the list
            Task task = taskList.get(i); // get(i) = record at index i

            if (task.getTaskId().equals(taskId)) {

                // FUTURE INTEGRATION: validate roomId against RoomDAO /
                // Reservation module once room persistence is available.
                task.setRoomId(roomId);

                taskDAO.saveTaskList(taskList);

                return true;
            }
        }

        return false;
    }

    public boolean removeTask(String taskId) {

        for (int i = 0; i < taskList.size(); i++) { // size() = current record count of the list
            Task task = taskList.get(i); // get(i) = record at index i

            if (task.getTaskId().equals(taskId)) {

                task.setTaskStatus(TaskStatus.CANCELLED); // soft delete

                // soft delete is also a status change, keep the change history trail
                appendTaskStatusChange(task, "Cancelled", LocalDateTime.now());

                taskDAO.saveTaskList(taskList);

                return true;
            }
        }

        return false;
    }

    public Task getTaskById(String taskId) {

        for (int i = 0; i < taskList.size(); i++) { // size() = current record count of the list
            Task task = taskList.get(i); // get(i) = record at index i

            if (task.getTaskId().equals(taskId)) {
                return task;
            }
        }

        return null;
    }

    public Task getTaskByName(String taskName) {

        for (int i = 0; i < taskList.size(); i++) { // size() = current record count of the list
            Task task = taskList.get(i); // get(i) = record at index i

            if (task.getTaskName().equalsIgnoreCase(taskName)) {
                return task;
            }
        }

        return null;
    }

    public LinkedListInterface<Task> getTasksByPriority(TaskPriority taskPriority) {

        LinkedListInterface<Task> filteredList = new LinkedList<>();

        for (int i = 0; i < taskList.size(); i++) { // size() = current record count of the list
            Task task = taskList.get(i); // get(i) = record at index i

            if (task.getTaskPriority() == taskPriority) {
                filteredList.addBack(task); // append the matching record to the end of the result list
            }
        }

        return filteredList;
    }

    public LinkedListInterface<Task> getTasksByType(String taskType) {

        LinkedListInterface<Task> filteredList = new LinkedList<>();

        for (int i = 0; i < taskList.size(); i++) { // size() = current record count of the list
            Task task = taskList.get(i); // get(i) = record at index i

            if (task.getTaskType().equalsIgnoreCase(taskType)) {
                filteredList.addBack(task); // append the matching record to the end of the result list
            }
        }

        return filteredList;
    }

    public LinkedListInterface<Task> getAllTasks() {
        return taskList;
    }

    public boolean taskExists(String taskId) {
        return getTaskById(taskId) != null;
    }

    // -------------------- private menu handlers --------------------

    private void addTaskMenu() {
        String[] details = ui.inputTaskDetails();
        // FUTURE INTEGRATION: accept room ID input here once RoomDAO exists;
        // rooms are currently linked to tasks via the Assignments menu.
        String taskId = createTask(details[0], details[1], TaskPriority.fromString(details[2]),
                ui.parseDateTime(details[3]), null);
        if (taskId == null) {
            ui.printDuplicateName();
        } else {
            ui.printTaskId(taskId);
            ui.printSuccess();
        }
    }

    private void searchTaskMenu() {
        int searchChoice = ui.getTaskSearchMenuChoice();
        if (searchChoice == 0) {
            return;
        }
        Task task = null;
        if (searchChoice == 1) {
            task = getTaskById(ui.inputTaskId());
        } else if (searchChoice == 2) {
            task = getTaskByName(ui.inputTaskName());
        }
        if (task == null) {
            ui.printNotFound();
        } else {
            ui.printTaskDetails(task);
        }
    }

    private void updateTaskMenu() {
        String taskId = ui.inputTaskId();
        if (!taskExists(taskId)) {
            ui.printNotFound();
            return;
        }
        String[] details = ui.inputUpdateTaskDetails();
        if (updateTask(taskId, details[0], details[1], TaskPriority.fromString(details[2]),
                ui.parseDateTime(details[3]))) {
            ui.printSuccess();
        } else {
            ui.printNotFound();
        }
    }

    private void updateTaskStatusMenu() {
        String taskId = ui.inputTaskId();
        if (!taskExists(taskId)) {
            ui.printNotFound();
            return;
        }
        if (updateTaskStatus(taskId, ui.inputTaskStatus())) {
            ui.printSuccess();
        } else {
            ui.printNotFound();
        }
    }

    private void removeTaskMenu() {
        String taskId = ui.inputTaskId();
        if (removeTask(taskId)) {
            ui.printSuccess();
        } else {
            ui.printNotFound();
        }
    }

    private void filterTaskByPriorityMenu() {
        TaskPriority priority = ui.inputTaskPriority();
        ui.listAllTasks(taskListToTable(getTasksByPriority(priority)));
    }

    private void filterTaskByTypeMenu() {
        String taskType = ui.inputTaskType();
        ui.listAllTasks(taskListToTable(getTasksByType(taskType)));
    }

    // -------------------- private helpers --------------------

    private String generateTaskId() {

        int max = 0;

        for (int i = 0; i < taskList.size(); i++) { // size() = current record count of the list
            String taskId = taskList.get(i).getTaskId(); // get(i) = record at index i

            int number = Integer.parseInt(taskId.substring(3));

            if (number > max) {
                max = number;
            }
        }

        return String.format("TSK%012d", max + 1);
    }

    // convert task list to 2D table
    private String[][] taskListToTable(LinkedListInterface<Task> taskList) {
        String[][] data = new String[taskList.size() + 1][6]; // +1 row for the header; size() = record count
        data[0] = new String[] { "Task ID", "Task Name", "Task Type", "Priority", "Current Status",
                "Start Date & Time" };
        for (int i = 0; i < taskList.size(); i++) { // size() = current record count of the list
            Task task = taskList.get(i); // get(i) = record at index i
            data[i + 1] = new String[] {
                    task.getTaskId(),
                    task.getTaskName(),
                    task.getTaskType(),
                    task.getTaskPriority() == null ? "-" : task.getTaskPriority().name(),
                    task.getTaskStatus() == null ? "-" : task.getTaskStatus().name(),
                    task.getStartDateTime() == null ? "-" : task.getStartDateTime().toString()
            };
        }
        return data;
    }

    // entry point for task assignment management
    public void runAssignmentManagement() {

        int choice;

        do {
            choice = ui.getAssignmentMenuChoice();

            switch (choice) {
                case 1:
                    assignStaffToTaskMenu();
                    break;
                case 2:
                    refreshTaskAssignments(); // always retrieve the latest records
                    ui.listAllAssignments(assignmentListToTable(taskAssignmentList));
                    break;
                case 3:
                    searchAssignmentMenu();
                    break;
                case 4:
                    updateAssignmentMenu();
                    break;
                case 5:
                    assignTaskToRoomMenu();
                    break;
                case 6:
                    viewTasksByRoomMenu();
                    break;
                case 7:
                    simulateGuestCheckoutMenu();
                    break;
                case 8:
                    updateAssignmentStatusMenu();
                    break;
                case 9:
                    updateTaskStatusMenu();
                    break;
                case 10:
                    viewChangeHistoryMenu();
                    break;
                case 11:
                    guestCleaningRequestMenu();
                    break;
                case 0:
                    break;
                default:
                    ui.printInvalidChoice();
            }

            if (choice != 0) {
                ui.pressEnterToContinue();
            }
        } while (choice != 0);
    }

    /**
     * Public API for other modules: call this after a guest confirms checkout.
     * Creates the cleaning task for the room and assigns it to the
     * housekeeping staff whose earliest free 60-minute slot (>= checkout
     * time) comes first. If nobody is free at checkout time, the task start
     * is deferred to the earliest slot found, i.e. the staff already has the
     * task on their schedule right after their current cleaning.
     * <p>
     * Returns the full new Task entity (for display) or null when a cleaning
     * task for this room already exists.
     * <p>
     * FUTURE INTEGRATION: validate roomId against RoomDAO and flip the room
     * to CLEANING then AVAILABLE once room persistence exists.
     */
    public Task processGuestCheckout(String roomId, LocalDateTime checkoutTime) {

        if (roomId == null || checkoutTime == null) {
            return null;
        }

        // auto create the cleaning task for the room
        String taskId = createTask("Clean " + roomId, "Housekeeping", TaskPriority.MEDIUM, checkoutTime, roomId);

        if (taskId == null) {
            return null; // a cleaning task for this room already exists
        }

        return autoAssignTask(taskId, checkoutTime, roomId);
    }

    /**
     * Assigns an existing task to the best available housekeeping staff using
     * the schedule gap analysis (see findEarliestFreeSlot). The slot is
     * booked atomically: assignments are loaded once, the earliest gap is
     * computed against that snapshot, the assignment is inserted and the
     * list is saved in the same step, so no double-booking can happen.
     */
    private Task autoAssignTask(String taskId, LocalDateTime requestedStart, String roomId) {

        StaffAndSlot best = findEarliestFreeSlot(requestedStart, CLEANING_DURATION_MINUTES, null);

        // set task start to the scheduled slot (deferred if staff are busy)
        LocalDateTime scheduledStart = best == null ? requestedStart : best.slotStart;
        updateTaskStartDateTime(taskId, scheduledStart);

        insertAssignment(generateAssignmentId(),
                "Pending", scheduledStart,
                best == null ? null : best.staff,
                getTaskById(taskId));

        return getTaskById(taskId);
    }

    /**
     * Creates an assignment linking staff <-> task. Multiple staff may share
     * the SAME task window (team), but a staff can never have two different
     * tasks overlapping. Returns the new assignment id, or an error code.
     */
    public String createAssignment(String staffId, String taskId, String status, LocalDateTime dateTimeAssigned) {

        Staff staff = getStaffById(staffId);
        if (staff == null) {
            return null; // staff record does not exist
        }
        if ("Resigned".equalsIgnoreCase(staff.getAvailabilityStatus())) {
            return "STAFF_UNAVAILABLE";
        }

        Task task = getTaskById(taskId);
        if (task == null) {
            return "TASK_NOT_FOUND";
        }

        // the task window is fixed by its start date & time
        if (task.getStartDateTime() == null) {
            return "TASK_NOT_FOUND";
        }

        LocalDateTime windowStart = task.getStartDateTime();
        LocalDateTime windowEnd = windowStart.plusMinutes(CLEANING_DURATION_MINUTES);

        // load the assignment snapshot once; the overlap check reuses it
        refreshTaskAssignments(); // always retrieve the latest records from disk

        if (!isStaffFreeForTask(staff, windowStart, windowEnd, taskId, taskAssignmentList)) {
            return "WINDOW_OVERLAP"; // staff already has another task in this window
        }

        // FUTURE INTEGRATION: pick workload / shift aware staff automatically
        // and trigger a Notification for the assigned staff.

        String assignmentId = generateAssignmentId();

        insertAssignment(assignmentId, status, dateTimeAssigned, staff, task);

        return assignmentId;
    }

    /**
     * Updates an individual worker's assignment status without necessarily
     * changing the parent task. Statuses such as Completed, Cancelled (drop /
     * decline), Handed Off, Paused or Work Finished belong to the worker;
     * the parent task follows the aggregated rules below.
     * <p>
     * Rules:
     * - Handed Off / Paused: worker status only; parent task unchanged.
     * - Completed / Work Finished: parent task stays active; once ALL
     * non-cancelled workers are done the caller is told to ask a supervisor
     * for final approval (parent task is then completed via task status
     * update).
     * - Cancelled (drop / decline): if it was the only active worker, the
     * parent task goes back to Pending and is auto-reassigned via the
     * earliest-free-slot timetable.
     *
     * Returns: "NOT_FOUND", "UPDATED", "ALL_DONE" or "REASSIGNED".
     */
    public String updateAssignmentStatus(String assignmentId, String status) {

        refreshTaskAssignments(); // always retrieve the latest records

        TaskAssignment target = null;

        for (int i = 0; i < taskAssignmentList.size(); i++) { // size() = current record count of the list
            if (taskAssignmentList.get(i).getTaskAssignmentId().equals(assignmentId)) { // get(i) = record at index i
                target = taskAssignmentList.get(i); // get(i) = record at index i
                break;
            }
        }

        if (target == null) {
            return "NOT_FOUND";
        }

        target.setStatus(status);
        taskAssignmentDAO.saveTaskAssignmentList(taskAssignmentList);

        // record the worker's status change in the separate change history
        appendAssignmentChange(target, status, LocalDateTime.now());

        // FUTURE INTEGRATION: propagate worker status updates to the parent
        // task via the task status stack and update RoomStatus once rooms are
        // persisted.

        String taskId = target.getAssignedTaskId();
        if (taskId == null) {
            return "UPDATED";
        }

        if ("Cancelled".equalsIgnoreCase(status) && countActiveWorkers(taskId, taskAssignmentList) == 0) {

            // the only worker dropped/declined: parent task back to Pending,
            // then auto-reassign to the next free slot (not to the same worker)
            Task parentTask = getTaskById(taskId);
            if (parentTask != null && parentTask.getTaskStatus() != TaskStatus.PENDING) {
                updateTaskStatus(taskId, "Pending");
            }

            String droppedStaffId = target.getAssignedStaffId();

            return reassignTask(taskId, droppedStaffId);
        }

        if (isTaskFullyFinished(taskId, taskAssignmentList)) {
            // all workers done; parent task remains for supervisor approval
            return "ALL_DONE";
        }

        return "UPDATED";
    }

    public boolean assignTaskToRoom(String taskId, String roomId) {
        // FUTURE INTEGRATION: validate roomId against RoomDAO / Reservation
        // module and update RoomStatus to CLEANING once room persistence exists.
        return updateTaskRoomId(taskId, roomId);
    }

    public TaskAssignment getAssignmentById(String assignmentId) {

        refreshTaskAssignments(); // always retrieve the latest records

        for (int i = 0; i < taskAssignmentList.size(); i++) { // size() = current record count of the list
            if (taskAssignmentList.get(i).getTaskAssignmentId().equals(assignmentId)) { // get(i) = record at index i
                return taskAssignmentList.get(i); // get(i) = record at index i
            }
        }

        return null;
    }

    public LinkedListInterface<TaskAssignment> getAssignmentsByStaff(String staffId) {

        LinkedListInterface<TaskAssignment> filteredList = new LinkedList<>();
        refreshTaskAssignments(); // always retrieve the latest records

        for (int i = 0; i < taskAssignmentList.size(); i++) { // size() = current record count of the list
            TaskAssignment assignment = taskAssignmentList.get(i); // get(i) = record at index i

            if (assignment.getAssignedStaffId() != null
                    && assignment.getAssignedStaffId().equals(staffId)) {
                filteredList.addBack(assignment); // append the matching record to the end of the result list
            }
        }

        return filteredList;
    }

    public LinkedListInterface<TaskAssignment> getAssignmentsByTask(String taskId) {

        LinkedListInterface<TaskAssignment> filteredList = new LinkedList<>();
        refreshTaskAssignments(); // always retrieve the latest records

        for (int i = 0; i < taskAssignmentList.size(); i++) { // size() = current record count of the list
            TaskAssignment assignment = taskAssignmentList.get(i); // get(i) = record at index i

            if (assignment.getAssignedTaskId() != null
                    && assignment.getAssignedTaskId().equals(taskId)) {
                filteredList.addBack(assignment); // append the matching record to the end of the result list
            }
        }

        return filteredList;
    }

    public LinkedListInterface<TaskAssignment> getAllAssignments() {
        refreshTaskAssignments(); // always retrieve the latest records
        return taskAssignmentList;
    }

    public Room getRoomById(String roomId) {
        return roomDAO.getRoomById(roomId);
    }

    // -------------------- reports --------------------

    /**
     * Room Cleaning Report (Room + Staff + Task + TaskAssignment).
     * Filters: date range (task start), staff role, room type.
     */
    public ReportResult generateRoomCleaningReport(LocalDateTime from, LocalDateTime to,
            String staffRole, RoomType roomType) {

        LinkedListInterface<Room> rooms = roomDAO.retrieveRoomList();
        LinkedListInterface<Staff> staffs = staffDAO.retrieveStaffList();
        LinkedListInterface<Task> tasks = taskDAO.retrieveTaskList();
        LinkedListInterface<TaskAssignment> assignments = taskAssignmentDAO.retrieveTaskAssignmentList();

        return new RoomCleaningReport(rooms, staffs, tasks, assignments)
                .generate(from, to, staffRole, roomType);
    }

    /**
     * Staff Workload Report (Staff + Task + TaskAssignment).
     * Filters: date range (date & time assigned), staff role, department.
     */
    public ReportResult generateStaffWorkloadReport(LocalDateTime from, LocalDateTime to,
            String staffRole, String department) {

        LinkedListInterface<Staff> staffs = staffDAO.retrieveStaffList();
        LinkedListInterface<Task> tasks = taskDAO.retrieveTaskList();
        LinkedListInterface<TaskAssignment> assignments = taskAssignmentDAO.retrieveTaskAssignmentList();

        return new StaffWorkloadReport(staffs, tasks, assignments)
                .generate(from, to, staffRole, department);
    }

    private void generateRoomCleaningReportMenu() {
        LocalDateTime[] range = ui.inputOptionalDateTimeRange("task start");
        String staffRole = ui.inputOptionalStaffRole();
        RoomType roomType = ui.inputOptionalRoomType();
        ui.printReport(generateRoomCleaningReport(range[0], range[1], staffRole, roomType));
    }

    private void generateStaffWorkloadReportMenu() {
        LocalDateTime[] range = ui.inputOptionalDateTimeRange("date & time assigned");
        String staffRole = ui.inputOptionalStaffRole();
        String department = ui.inputOptionalDepartment();
        ui.printReport(generateStaffWorkloadReport(range[0], range[1], staffRole, department));
    }

    private void guestCleaningRequestMenu() {
        String roomId = ui.inputRoomId();
        TaskPriority priority = ui.inputTaskPriority();
        LocalDateTime requestedStart = ui.inputCheckoutDateTime();

        Task task = createCleaningTask(roomId, "Housekeeping", priority, requestedStart);

        if (task == null) {
            ui.printTaskAlreadyExists();
            return;
        }

        ui.printTaskDetails(task);
        ui.printSuccess();
    }

    // rooms: currently free-form text; see FUTURE INTEGRATION note above
    public LinkedListInterface<Task> getTasksByRoom(String roomId) {

        LinkedListInterface<Task> filteredList = new LinkedList<>();
        LinkedListInterface<Task> allTasks = getAllTasks();

        for (int i = 0; i < allTasks.size(); i++) { // size() = current record count of the list
            Task task = allTasks.get(i); // get(i) = record at index i

            if (task.getRoomId() != null && task.getRoomId().equalsIgnoreCase(roomId)) {
                filteredList.addBack(task); // append the matching record to the end of the result list
            }
        }

        return filteredList;
    }

    /**
     * Reassign: change the staff and/or task of an existing assignment.
     */
    public boolean reassignAssignment(String assignmentId, String staffId, String taskId) {

        refreshTaskAssignments(); // always retrieve the latest records

        for (int i = 0; i < taskAssignmentList.size(); i++) { // size() = current record count of the list
            TaskAssignment assignment = taskAssignmentList.get(i); // get(i) = record at index i

            if (assignment.getTaskAssignmentId().equals(assignmentId)) {

                Staff staff = getStaffById(staffId);
                Task task = getTaskById(taskId);

                if (staff == null
                        || "Resigned".equalsIgnoreCase(staff.getAvailabilityStatus())
                        || task == null) {
                    return false;
                }

                // detach from the previous staff / task entity lists
                Staff oldStaff = assignment.getAssignedStaffId() == null ? null : getStaffById(assignment.getAssignedStaffId());
                Task oldTask = assignment.getAssignedTaskId() == null ? null : getTaskById(assignment.getAssignedTaskId());
                if (oldStaff != null) {
                    oldStaff.removeTaskAssignment(assignment);
                }
                if (oldTask != null) {
                    oldTask.removeTaskAssignment(assignment);
                }

                assignment.setAssignedStaffId(staff.getStaffId());
                assignment.setAssignedTaskId(task.getTaskId());

                // attach to the new staff / task entity lists (ids only persisted)
                staff.addTaskAssignment(assignment);
                task.addTaskAssignment(assignment);

                taskAssignmentDAO.saveTaskAssignmentList(taskAssignmentList);
                staffDAO.saveStaffList(staffList);
                taskDAO.saveTaskList(taskList);

                return true;
            }
        }

        return false;
    }

    // -------------------- private scheduling helpers --------------------

    /**
     * Schedule gap analysis over all eligible housekeeping staff.
     * The assignment list is loaded ONCE from disk into a snapshot; every
     * eligible worker is then scanned chronologically for the earliest
     * continuous free interval >= requestedStart that fits a task of
     * durationMinutes inside the shift boundaries. The worker who becomes
     * available first wins; ties are broken by the lowest current workload,
     * then by the lowest staff id. The staff who dropped / declined a task
     * may be excluded via excludeStaffId.
     */
    private StaffAndSlot findEarliestFreeSlot(LocalDateTime requestedStart, int durationMinutes,
            String excludeStaffId) {

        refreshTaskAssignments(); // always retrieve the latest records
        LinkedListInterface<TaskAssignment> snapshot = taskAssignmentList;

        StaffAndSlot best = null;

        for (int i = 0; i < getAllStaffs().size(); i++) { // size() = current record count of the list
            Staff staff = getAllStaffs().get(i); // get(i) = record at index i

            if (!"Housekeeping".equalsIgnoreCase(staff.getDepartment())) {
                continue;
            }
            if (!"Available".equalsIgnoreCase(staff.getAvailabilityStatus())) {
                continue;
            }
            if (excludeStaffId != null && excludeStaffId.equals(staff.getStaffId())) {
                continue; // do not immediately reassign to the staff who declined
            }

            LocalDateTime gapStart = earliestGapStart(staff, requestedStart, durationMinutes, null, snapshot);

            if (gapStart == null) {
                continue;
            }
            if (best == null || gapStart.isBefore(best.slotStart)) {
                best = new StaffAndSlot(staff, gapStart);
            } else if (gapStart.equals(best.slotStart)) {
                // tie: prefer the worker with the smaller current workload
                if (countWorkload(staff, snapshot) < countWorkload(best.staff, snapshot)) {
                    best = new StaffAndSlot(staff, gapStart);
                }
            }
        }

        return best;
    }

    /**
     * Gap analysis for a single staff member against one assignment snapshot:
     * 1. Collect the occupied intervals from their non-cancelled assignment
     * records (other tasks only; the candidate task itself is excluded so
     * team members can share its window).
     * 2. Sort the intervals chronologically and MERGE overlapping (or
     * touching) neighbours, so the scan walks a minimal non-overlapping
     * timetable instead of every raw record.
     * 3. Scan the merged intervals against the staff's shift window. The
     * cursor always points at the next possible start: a gap
     * [cursor, nextIntervalStart) that is large enough - and still ends
     * before the shift ends - is the earliest valid start; otherwise the
     * cursor jumps past the occupied interval.
     * 4. If no gap fits inside today's shift, the search defers to the next
     * day's SHIFT_START so the worker always has the task on schedule.
     */
    private LocalDateTime earliestGapStart(Staff staff, LocalDateTime requestedStart, int durationMinutes,
            String excludeTaskId, LinkedListInterface<TaskAssignment> snapshot) {

        // step 1: occupied intervals of this staff
        List<Interval> intervals = new ArrayList<>();

        for (int i = 0; i < snapshot.size(); i++) { // size() = current record count of the list
            TaskAssignment assignment = snapshot.get(i); // get(i) = record at index i

            if (assignment.getAssignedStaffId() == null
                    || !assignment.getAssignedStaffId().equals(staff.getStaffId())) {
                continue;
            }
            if ("Cancelled".equalsIgnoreCase(assignment.getStatus())) {
                continue;
            }

            Task task = getTaskById(assignment.getAssignedTaskId());
            if (task == null || task.getStartDateTime() == null) {
                continue;
            }
            // same task = team members, may share the window
            if (task.getTaskId().equals(excludeTaskId)) {
                continue;
            }

            LocalDateTime taskStart = task.getStartDateTime();

            intervals.add(new Interval(taskStart, taskStart.plusMinutes(CLEANING_DURATION_MINUTES)));
        }

        // step 2: chronological sort, then merge overlapping/touching intervals
        Collections.sort(intervals, (a, b) -> a.start.compareTo(b.start));

        List<Interval> merged = new ArrayList<>();
        for (Interval interval : intervals) {
            if (merged.isEmpty() || interval.start.isAfter(merged.get(merged.size() - 1).end)) {
                merged.add(interval);
            } else if (interval.end.isAfter(merged.get(merged.size() - 1).end)) {
                // extend the last merged interval to cover the overlap
                Interval last = merged.get(merged.size() - 1);
                merged.set(merged.size() - 1, new Interval(last.start, interval.end));
            }
        }

        // step 3: chronological scan within each day's shift
        LocalDateTime start = requestedStart;

        for (int day = 0; day < MAX_DEFER_DAYS; day++) {

            LocalDate date = start.toLocalDate();
            LocalDateTime shiftStart = LocalDateTime.of(date, SHIFT_START);
            LocalDateTime shiftEnd = LocalDateTime.of(date, SHIFT_END);

            LocalDateTime cursor = start.isBefore(shiftStart) ? shiftStart : start;

            if (!cursor.isBefore(shiftEnd)) {
                // requested start is already past today's shift end
                start = LocalDateTime.of(date.plusDays(1), SHIFT_START);
                continue;
            }

            for (Interval interval : merged) {

                if (!interval.end.isAfter(cursor)) {
                    continue; // interval already finished before the cursor
                }

                if (!interval.start.isBefore(cursor)) {
                    // continuous gap [cursor, interval.start)
                    if (gapFitsWithinShift(cursor, interval.start, durationMinutes, shiftEnd)) {
                        return cursor;
                    }
                }

                // gap too small (or overlapping) -> jump past the interval
                if (interval.end.isAfter(cursor)) {
                    cursor = interval.end;
                }
                if (!cursor.isBefore(shiftEnd)) {
                    break; // the rest of today's shift is unusable
                }
            }

            // gap after the last occupied interval
            if (gapFitsWithinShift(cursor, shiftEnd, durationMinutes, shiftEnd)) {
                return cursor;
            }

            // step 4: no fit today -> defer to the next shift
            start = LocalDateTime.of(date.plusDays(1), SHIFT_START);
        }

        return null;
    }

    /**
     * True when [cursor, limit) is long enough for the task and the task
     * still ends before or at the shift end.
     */
    private boolean gapFitsWithinShift(LocalDateTime cursor, LocalDateTime limit, int durationMinutes,
            LocalDateTime shiftEnd) {
        return !cursor.plusMinutes(durationMinutes).isAfter(limit)
                && !cursor.plusMinutes(durationMinutes).isAfter(shiftEnd);
    }

    // occupied windows are sorted and merged before the gap scan
    private static class Interval {
        final LocalDateTime start;
        final LocalDateTime end;

        Interval(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
        }
    }

    // current workload of a staff: non-cancelled, non-completed assignments
    private int countWorkload(Staff staff, LinkedListInterface<TaskAssignment> snapshot) {

        int count = 0;

        for (int i = 0; i < snapshot.size(); i++) { // size() = current record count of the list
            TaskAssignment assignment = snapshot.get(i); // get(i) = record at index i

            if (assignment.getAssignedStaffId() == null
                    || !assignment.getAssignedStaffId().equals(staff.getStaffId())) {
                continue;
            }
            if ("Cancelled".equalsIgnoreCase(assignment.getStatus())
                    || "Completed".equalsIgnoreCase(assignment.getStatus())) {
                continue;
            }

            count++;
        }

        return count;
    }

    private boolean windowsOverlap(LocalDateTime s1, LocalDateTime e1, LocalDateTime s2, LocalDateTime e2) {
        return s1.isBefore(e2) && s2.isBefore(e1);
    }

    /**
     * True when the staff has no non-cancelled record with an overlapping
     * window belonging to a DIFFERENT task than the candidate task.
     * The caller passes one assignment snapshot so the check needs no
     * additional disk reads.
     */
    private boolean isStaffFreeForTask(Staff staff, LocalDateTime windowStart, LocalDateTime windowEnd,
            String taskId, LinkedListInterface<TaskAssignment> snapshot) {

        for (int i = 0; i < snapshot.size(); i++) { // size() = current record count of the list
            TaskAssignment assignment = snapshot.get(i); // get(i) = record at index i

            if (assignment.getAssignedStaffId() == null
                    || !assignment.getAssignedStaffId().equals(staff.getStaffId())) {
                continue;
            }
            if ("Cancelled".equalsIgnoreCase(assignment.getStatus())) {
                continue;
            }

            Task task = getTaskById(assignment.getAssignedTaskId());
            if (task == null || task.getStartDateTime() == null) {
                continue;
            }
            if (task.getTaskId().equals(taskId)) {
                continue; // team member on the same task is allowed
            }

            LocalDateTime taskStart = task.getStartDateTime();
            LocalDateTime taskEnd = taskStart.plusMinutes(CLEANING_DURATION_MINUTES);

            if (windowsOverlap(windowStart, windowEnd, taskStart, taskEnd)) {
                return false;
            }
        }

        return true;
    }

    private int countActiveWorkers(String taskId, LinkedListInterface<TaskAssignment> snapshot) {

        int count = 0;

        for (int i = 0; i < snapshot.size(); i++) { // size() = current record count of the list
            TaskAssignment assignment = snapshot.get(i); // get(i) = record at index i
            if (taskId.equals(assignment.getAssignedTaskId())
                    && !"Cancelled".equalsIgnoreCase(assignment.getStatus())) {
                count++;
            }
        }

        return count;
    }

    private boolean isTaskFullyFinished(String taskId, LinkedListInterface<TaskAssignment> snapshot) {

        boolean any = false;

        for (int i = 0; i < snapshot.size(); i++) { // size() = current record count of the list
            TaskAssignment assignment = snapshot.get(i); // get(i) = record at index i
            if (!taskId.equals(assignment.getAssignedTaskId())
                    || "Cancelled".equalsIgnoreCase(assignment.getStatus())) {
                continue;
            }
            any = true;
            if (!("Completed".equalsIgnoreCase(assignment.getStatus())
                    || "Work Finished".equalsIgnoreCase(assignment.getStatus()))) {
                return false;
            }
        }

        return any;
    }

    /**
     * Reassigns a task to the earliest free slot on the timetable after a
     * worker dropped/declined it (the declining worker is excluded from the
     * re-pool). Returns "REASSIGNED" or "UPDATED" when no slot could be planned.
     */
    private String reassignTask(String taskId, String excludeStaffId) {

        Task task = getTaskById(taskId);
        if (task == null || task.getStartDateTime() == null) {
            return "UPDATED";
        }

        StaffAndSlot best = findEarliestFreeSlot(task.getStartDateTime(), CLEANING_DURATION_MINUTES, excludeStaffId);

        if (best == null) {
            // no slot could be planned; parent task stays Pending for manual assignment
            return "UPDATED";
        }

        updateTaskStartDateTime(taskId, best.slotStart);

        insertAssignment(generateAssignmentId(), "Pending", best.slotStart, best.staff, getTaskById(taskId));

        return "REASSIGNED";
    }

    private String insertAssignment(String assignmentId, String status, LocalDateTime dateTimeAssigned,
            Staff staff, Task task) {

        refreshTaskAssignments(); // always retrieve the latest records

        TaskAssignment assignment = new TaskAssignment(assignmentId, status, dateTimeAssigned,
                staff == null ? null : staff.getStaffId(),
                task == null ? null : task.getTaskId());

        taskAssignmentList.addSorted(assignment); // insert the record, keeping the list sorted by date & time assigned
        taskAssignmentDAO.saveTaskAssignmentList(taskAssignmentList);

        // save in staff task assignment list
        if (staff != null) {
            staff.addTaskAssignment(assignment);
            staffDAO.saveStaffList(staffList);
        }
        // save in task task assignment list
        if (task != null) {
            task.addTaskAssignment(assignment);
            taskDAO.saveTaskList(taskList);
        }

        return assignmentId;
    }

    private String generateAssignmentId() {

        int max = 0;
        refreshTaskAssignments(); // always retrieve the latest records

        for (int i = 0; i < taskAssignmentList.size(); i++) { // size() = current record count of the list
            String assignmentId = taskAssignmentList.get(i).getTaskAssignmentId(); // get(i) = record at index i

            if (assignmentId == null) {
                continue;
            }

            int number = Integer.parseInt(assignmentId.substring(3));

            if (number > max) {
                max = number;
            }
        }

        return String.format("ASG%012d", max + 1);
    }

    // -------------------- private menu handlers --------------------

    private void assignStaffToTaskMenu() {
        String staffId = ui.inputStaffId();
        String taskId = ui.inputTaskId();
        String status = ui.inputAssignmentStatus();
        LocalDateTime dateTimeAssigned = ui.inputDateAssigned();

        String result = createAssignment(staffId, taskId, status, dateTimeAssigned);

        if (result == null) {
            ui.printStaffNotFound();
        } else if ("STAFF_UNAVAILABLE".equals(result)) {
            ui.printStaffUnavailable();
        } else if ("TASK_NOT_FOUND".equals(result)) {
            ui.printTaskNotFound();
        } else if ("WINDOW_OVERLAP".equals(result)) {
            ui.printWindowOverlap();
        } else {
            ui.printAssignmentId(result);
            ui.printSuccess();
        }
    }

    private void searchAssignmentMenu() {
        int searchChoice = ui.getAssignmentSearchMenuChoice();
        if (searchChoice == 0) {
            return;
        }
        LinkedListInterface<TaskAssignment> result = new LinkedList<>();
        if (searchChoice == 1) {
            TaskAssignment assignment = getAssignmentById(ui.inputAssignmentId());
            if (assignment != null) {
                result.addBack(assignment); // append the matching record to the end of the result list
            }
        } else if (searchChoice == 2) {
            result = getAssignmentsByStaff(ui.inputStaffId());
        } else if (searchChoice == 3) {
            result = getAssignmentsByTask(ui.inputTaskId());
        }
        if (result.isEmpty()) { // true when the list holds no records
            ui.printNotFound();
        } else if (result.size() == 1) { // exactly one record matched
            ui.printAssignmentDetails(result.getFirst(), // getFirst() = head record of the list
                    result.getFirst().getAssignedStaffId() == null ? null
                            : getStaffById(result.getFirst().getAssignedStaffId()),
                    result.getFirst().getAssignedTaskId() == null ? null
                            : getTaskById(result.getFirst().getAssignedTaskId()));
        } else {
            ui.listAllAssignments(assignmentListToTable(result));
        }
    }

    private void updateAssignmentMenu() {
        String assignmentId = ui.inputAssignmentId();
        if (getAssignmentById(assignmentId) == null) {
            ui.printNotFound();
            return;
        }
        String staffId = ui.inputStaffId();
        String taskId = ui.inputTaskId();
        if (reassignAssignment(assignmentId, staffId, taskId)) {
            ui.printSuccess();
        } else {
            ui.printNotFound();
        }
    }

    private void assignTaskToRoomMenu() {
        String taskId = ui.inputTaskId();
        if (!taskExists(taskId)) {
            ui.printTaskNotFound();
            return;
        }
        String roomId = ui.inputRoomId();
        if (assignTaskToRoom(taskId, roomId)) {
            ui.printSuccess();
        } else {
            ui.printTaskNotFound();
        }
    }

    private void viewTasksByRoomMenu() {
        String roomId = ui.inputRoomId();
        // FUTURE INTEGRATION: switch to RoomDAO-based display once rooms are
        // persisted (show room details + linked tasks in one view).
        ui.listAllTasks(taskListToTable(getTasksByRoom(roomId)));
    }

    private void simulateGuestCheckoutMenu() {
        String roomId = ui.inputRoomId();
        LocalDateTime checkoutTime = ui.inputCheckoutDateTime();

        Task task = processGuestCheckout(roomId, checkoutTime);

        if (task == null) {
            ui.printTaskAlreadyExists();
            return;
        }

        // resolve the assigned staff (first active assignment) for display
        Staff staff = null;
        if (!task.getTaskAssignments().isEmpty()) {
            staff = getStaffById(task.getTaskAssignments().getFirst().getAssignedStaffId());
        }

        boolean deferred = task.getStartDateTime() != null && task.getStartDateTime().isAfter(checkoutTime);

        ui.printGuestCheckoutTask(task, staff, deferred);
    }

    private void updateAssignmentStatusMenu() {
        String assignmentId = ui.inputAssignmentId();
        if (getAssignmentById(assignmentId) == null) {
            ui.printNotFound();
            return;
        }
        String status = ui.inputWorkerAssignmentStatus();

        String result = updateAssignmentStatus(assignmentId, status);

        if ("NOT_FOUND".equals(result)) {
            ui.printNotFound();
        } else if ("ALL_DONE".equals(result)) {
            ui.printAllWorkersDoneHint(assignmentId);
        } else if ("REASSIGNED".equals(result)) {
            ui.printReassigned();
        } else {
            ui.printSuccess();
        }
    }

    private void viewChangeHistoryMenu() {
        String taskId = ui.inputOptionalTaskId();
        LinkedListInterface<TaskAssignmentChange> changes = taskId == null ? getAllChanges() : getChangesByTask(taskId);
        ui.listAllChanges(changeListToTable(changes));
    }

    // -------------------- table converters --------------------

    // convert assignment list to 2D table
    private String[][] assignmentListToTable(LinkedListInterface<TaskAssignment> assignmentList) {
        String[][] data = new String[assignmentList.size() + 1][6]; // +1 row for the header; size() = record count
        data[0] = new String[] { "Assignment ID", "Staff", "Task", "Room ID", "Status", "Date & Time Assigned" };
        for (int i = 0; i < assignmentList.size(); i++) { // size() = current record count of the list
            TaskAssignment assignment = assignmentList.get(i); // get(i) = record at index i
            Staff staff = assignment.getAssignedStaffId() == null ? null
                    : getStaffById(assignment.getAssignedStaffId());
            Task task = assignment.getAssignedTaskId() == null ? null : getTaskById(assignment.getAssignedTaskId());
            data[i + 1] = new String[] {
                    assignment.getTaskAssignmentId(),
                    staff == null ? "-" : staff.getStaffId() + " (" + staff.getStaffName() + ")",
                    task == null ? "-" : task.getTaskId() + " (" + task.getTaskName() + ")",
                    task == null || task.getRoomId() == null ? "-" : task.getRoomId(),
                    assignment.getStatus(),
                    assignment.getDateTimeAssigned() == null ? "-" : assignment.getDateTimeAssigned().toString()
            };
        }
        return data;
    }

    /**
     * Records a task status change as a TaskAssignmentChange. Called on
     * task status update / soft delete and by the auto-reassignment flow.
     * The record keeps the status, the date & time of the change, the staff
     * currently active on the task and a snapshot of the task object (plus
     * the active assignment it belongs to).
     * <p>
     * FUTURE INTEGRATION: trigger a Notification for the staff involved in
     * each new change; query history by date range for supervisor reports.
     */
    public TaskAssignmentChange appendTaskStatusChange(Task task, String status, LocalDateTime dateTime) {
        if (task == null || status == null) {
            return null;
        }

        refreshTaskAssignments(); // always retrieve the latest records
        TaskAssignment active = getActiveAssignment(taskAssignmentList, task.getTaskId());

        return insertChange(new TaskAssignmentChange(
                generateChangeId(),
                active == null ? null : active.getTaskAssignmentId(),
                status,
                dateTime,
                active == null ? null : active.getAssignedStaffId(),
                task.getTaskId()));
    }

    /**
     * Records a worker's assignment status change as a TaskAssignmentChange.
     * The TaskAssignment keeps its current status in place; the change is
     * appended to the separate change history.
     */
    public TaskAssignmentChange appendAssignmentChange(TaskAssignment assignment, String status,
            LocalDateTime dateTime) {
        if (assignment == null || status == null) {
            return null;
        }

        return insertChange(new TaskAssignmentChange(
                generateChangeId(),
                assignment.getTaskAssignmentId(),
                status,
                dateTime,
                assignment.getAssignedStaffId(),
                assignment.getAssignedTaskId()));
    }

    private TaskAssignmentChange insertChange(TaskAssignmentChange change) {

        refreshTaskAssignmentChanges(); // always retrieve the latest records

        taskAssignmentChangeList.addSorted(change); // insert the record, keeping the list sorted by change time
        taskAssignmentChangeDAO.saveTaskAssignmentChangeList(taskAssignmentChangeList);

        return change;
    }

    public LinkedListInterface<TaskAssignmentChange> getAllChanges() {
        refreshTaskAssignmentChanges(); // always retrieve the latest records
        return taskAssignmentChangeList;
    }

    public LinkedListInterface<TaskAssignmentChange> getChangesByTask(String taskId) {

        LinkedListInterface<TaskAssignmentChange> filteredList = new LinkedList<>();
        refreshTaskAssignmentChanges(); // always retrieve the latest records

        for (int i = 0; i < taskAssignmentChangeList.size(); i++) { // size() = current record count of the list
            TaskAssignmentChange change = taskAssignmentChangeList.get(i); // get(i) = record at index i

            if (change.getTaskId() != null && change.getTaskId().equals(taskId)) {
                filteredList.addBack(change); // append the matching record to the end of the result list
            }
        }

        return filteredList;
    }

    private String generateChangeId() {

        int max = 0;
        refreshTaskAssignmentChanges(); // always retrieve the latest records

        for (int i = 0; i < taskAssignmentChangeList.size(); i++) { // size() = current record count of the list
            String changeId = taskAssignmentChangeList.get(i).getChangeId(); // get(i) = record at index i

            if (changeId == null) {
                continue;
            }

            int number = Integer.parseInt(changeId.substring(3));

            if (number > max) {
                max = number;
            }
        }

        return String.format("CHG%012d", max + 1);
    }

    private TaskAssignment getActiveAssignment(LinkedListInterface<TaskAssignment> assignmentList, String taskId) {
        // most recent non-cancelled record for the task carries the active worker
        TaskAssignment active = null;
        LocalDateTime latest = null;

        for (int i = 0; i < assignmentList.size(); i++) { // size() = current record count of the list
            TaskAssignment assignment = assignmentList.get(i); // get(i) = record at index i
            if (!taskId.equals(assignment.getAssignedTaskId())) {
                continue;
            }
            if ("Cancelled".equalsIgnoreCase(assignment.getStatus())) {
                continue;
            }
            if (latest == null || (assignment.getDateTimeAssigned() != null
                    && assignment.getDateTimeAssigned().isAfter(latest))) {
                latest = assignment.getDateTimeAssigned();
                active = assignment;
            }
        }
        return active;
    }

    // convert change history list to 2D table (ids resolved to entities for display)
    private String[][] changeListToTable(LinkedListInterface<TaskAssignmentChange> changeList) {
        String[][] data = new String[changeList.size() + 1][6]; // +1 row for the header; size() = record count
        data[0] = new String[] { "Change ID", "Task", "Assignment", "Status", "Staff", "Changed At" };
        for (int i = 0; i < changeList.size(); i++) { // size() = current record count of the list
            TaskAssignmentChange change = changeList.get(i); // get(i) = record at index i
            Staff staff = change.getStaffId() == null ? null : getStaffById(change.getStaffId());
            Task task = change.getTaskId() == null ? null : getTaskById(change.getTaskId());
            data[i + 1] = new String[] {
                    change.getChangeId(),
                    task == null ? "-" : task.getTaskId() + " (" + task.getTaskName() + ")",
                    change.getTaskAssignmentId() == null ? "-" : change.getTaskAssignmentId(),
                    change.getStatus(),
                    staff == null ? "-" : staff.getStaffId() + " (" + staff.getStaffName() + ")",
                    change.getChangedAt() == null ? "-" : change.getChangedAt().toString()
            };
        }
        return data;
    }

    private static class StaffAndSlot {
        final Staff staff;
        final LocalDateTime slotStart;

        StaffAndSlot(Staff staff, LocalDateTime slotStart) {
            this.staff = staff;
            this.slotStart = slotStart;
        }
    }
}