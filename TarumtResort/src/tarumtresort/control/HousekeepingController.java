package tarumtresort.control;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
import tarumtresort.entity.enums.RoomStatus;
import tarumtresort.entity.enums.TaskPriority;
import tarumtresort.entity.enums.TaskStatus;
import tarumtresort.report.ReportMenu;
import tarumtresort.report.ReportUI;
import tarumtresort.utility.ConsoleUtil;

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

    // paging: how many entity rows fit on one list page
    private static final int PAGE_SIZE = 20;

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
        try {
            ConsoleUtil.clearScreen();
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
                        new ReportMenu(new ReportUI(ui.getScanner())).run();
                        break;
                    case 0:
                        ui.printExitMessage();
                        break;
                    default:
                        ui.printInvalidChoice();
                }
            } while (choice != 0);
        } catch (Exception e) {
            ConsoleUtil.printError("An unexpected error occurred in Housekeeping module: " + e.getMessage());
        }
    }

    // entry point for staff management
    public void runStaffManagement() {

        String departmentFilter = null;
        String availabilityFilter = null;
        int page = 0;

        while (true) {
            LinkedListInterface<Staff> display;
            if (departmentFilter != null) {
                display = getStaffsByDepartment(departmentFilter);
            } else if (availabilityFilter != null) {
                display = getStaffsByAvailability(availabilityFilter);
            } else {
                display = getAllStaffs();
            }

            boolean hasFilter = departmentFilter != null || availabilityFilter != null;
            int pageCount = Math.max(1, (display.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            if (page >= pageCount) {
                page = pageCount - 1; // clamp after the list shrank
            }

            LinkedListInterface<Staff> pageList = pageOf(display, page);
            int choice = ui.printStaffListMenu(pageList, page, pageCount, hasFilter);

            if (choice == 0) {
                break;
            }

            int action = 1;
            if (choice == action++) { // 1. View Details
                viewStaff(pageList);
            } else if (choice == action++) { // 2. Add New Staff
                addStaffMenu();
            } else if (choice == action++) { // 3. Filter by Department
                departmentFilter = ui.inputDepartment();
                availabilityFilter = null;
                page = 0;
            } else if (choice == action++) { // 4. Filter by Availability
                availabilityFilter = ui.inputAvailabilityStatus();
                departmentFilter = null;
                page = 0;
            } else {
                boolean matched = false;
                if (page < pageCount - 1) { // Next Page
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page++;
                    }
                }
                if (!matched && page > 0) { // Previous Page
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page--;
                    }
                }
                if (!matched && hasFilter) { // Clear Filter
                    matched = choice == action;
                    action++;
                    if (matched) {
                        departmentFilter = null;
                        availabilityFilter = null;
                        page = 0;
                    }
                }
            }
        }
    }

    // view flow: pick a record from the current page, then run its action menu
    private void viewStaff(LinkedListInterface<Staff> pageList) {
        if (pageList.isEmpty()) {
            ui.printNoRecords();
            ui.pressEnterToContinue();
            return;
        }
        int num = ui.inputListIndex("staff", pageList.size());
        if (num == 0) {
            return;
        }
        Staff staff = pageList.get(num - 1);
        if (staff != null) {
            handleStaffActions(staff);
        }
    }

    // select-entity action loop for one staff: details -> action -> details
    private void handleStaffActions(Staff staff) {
        while (true) {
            ui.printStaffDetails(staff);

            int action = ui.getStaffActionChoice();
            if (action == 0) {
                return;
            }

            switch (action) {
                case 1:
                    updateStaffFieldMenu(staff);
                    break;
                case 2:
                    if (resignStaff(staff.getStaffId())) {
                        ui.printSuccess();
                    } else {
                        ui.printNotFound();
                    }
                    break;
                case 3:
                    ui.listAllAssignments(assignmentListToTable(getAssignmentsByStaff(staff.getStaffId())));
                    break;
                default:
                    break;
            }

            staff = getStaffById(staff.getStaffId()); // re-read so details stay fresh
        }
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

    // loop: pick a field, edit it, show updated details, until Back
    private void updateStaffFieldMenu(Staff staff) {
        String[] fields = {"Staff Name", "Department", "Staff Role", "Availability Status"};
        while (true) {
            int field = ui.inputFieldChoice(fields);
            if (field == 0) {
                return;
            }
            String value = switch (field) {
                case 1 -> ui.inputStaffName();
                case 2 -> ui.inputDepartment();
                case 3 -> ui.inputStaffRole();
                default -> ui.inputAvailabilityStatus();
            };
            if (updateStaffField(staff.getStaffId(), field, value)) {
                ui.printSuccess();
            } else {
                ui.printNotFound();
                return;
            }
            staff = getStaffById(staff.getStaffId()); // re-read so details stay fresh
            ui.printStaffDetails(staff);
        }
    }

    // update only one chosen field of a staff record
    public boolean updateStaffField(String staffId, int field, String value) {
        for (int i = 0; i < staffList.size(); i++) {
            Staff staff = staffList.get(i);
            if (staff.getStaffId().equals(staffId)) {
                switch (field) {
                    case 1 -> staff.setStaffName(value);
                    case 2 -> staff.setDepartment(value);
                    case 3 -> staff.setStaffRole(value);
                    default -> staff.setAvailabilityStatus(value);
                }
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

                // open assignments of the resigned staff are cancelled; each
                // affected task is auto-reassigned when it has no workers left
                refreshTaskAssignments(); // always retrieve the latest records

                for (int j = 0; j < taskAssignmentList.size(); j++) { // size() = current record count of the list
                    TaskAssignment assignment = taskAssignmentList.get(j); // get(j) = record at index j

                    if (!staffId.equals(assignment.getAssignedStaffId())
                            || "Cancelled".equalsIgnoreCase(assignment.getStatus())
                            || "Completed".equalsIgnoreCase(assignment.getStatus())
                            || "Work Finished".equalsIgnoreCase(assignment.getStatus())
                            || "Inspected".equalsIgnoreCase(assignment.getStatus())) {
                        continue;
                    }

                    assignment.setStatus("Cancelled");
                    taskAssignmentDAO.saveTaskAssignmentList(taskAssignmentList);
                    appendAssignmentChange(assignment, "Cancelled", LocalDateTime.now());

                    String taskId = assignment.getAssignedTaskId();
                    if (taskId != null && countActiveWorkers(taskId, taskAssignmentList) == 0) {
                        reassignTask(taskId, staffId);
                    }
                }

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

    // the rows of one page (PAGE_SIZE at most), starting at page * PAGE_SIZE
    private <T extends Comparable<T>> LinkedList<T> pageOf(LinkedListInterface<T> list, int page) {
        LinkedList<T> result = new LinkedList<>();
        int start = page * PAGE_SIZE;
        int end = Math.min(list.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            result.addBack(list.get(i)); // append the record to the page list
        }
        return result;
    }

    private void addStaffMenu() {
        String[] details = ui.inputStaffDetails();
        String staffId = createStaff(details[0], details[1], details[2], details[3]);
        if (staffId == null) {
            ui.printDuplicateNameError();
        } else {
            ui.printStaffId(staffId);
            ui.printSuccess();
        }
    }

    // -------------------- private helpers --------------------

    private int parseIdSuffix(String id) {
        if (id == null || id.length() <= 3) {
            return 0;
        }
        try {
            return Integer.parseInt(id.substring(3));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String generateStaffId() {

        int max = 0;

        for (int i = 0; i < staffList.size(); i++) { // size() = current record count of the list
            String staffId = staffList.get(i).getStaffId(); // get(i) = record at index i

            int number = parseIdSuffix(staffId);

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

        TaskPriority priorityFilter = null;
        String typeFilter = null;
        int page = 0;

        while (true) {
            LinkedListInterface<Task> display;
            if (priorityFilter != null) {
                display = getTasksByPriority(priorityFilter);
            } else if (typeFilter != null) {
                display = getTasksByType(typeFilter);
            } else {
                display = getAllTasks();
            }

            boolean hasFilter = priorityFilter != null || typeFilter != null;
            int pageCount = Math.max(1, (display.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            if (page >= pageCount) {
                page = pageCount - 1; // clamp after the list shrank
            }

            LinkedListInterface<Task> pageList = pageOf(display, page);
            int choice = ui.printTaskListMenu(pageList, page, pageCount, hasFilter);

            if (choice == 0) {
                break;
            }

            // the numbering mirrors the Actions section in the UI: Next /
            // Previous / Clear only occupy a number when they are shown
            int action = 1;
            if (choice == action++) { // 1. View Details
                viewTask(pageList);
            } else if (choice == action++) { // 2. Add New Task
                addTaskMenu();
            } else if (choice == action++) { // 3. Filter by Priority
                priorityFilter = ui.inputTaskPriority();
                typeFilter = null;
                page = 0;
            } else if (choice == action++) { // 4. Filter by Type
                typeFilter = ui.inputTaskType();
                priorityFilter = null;
                page = 0;
            } else {
                boolean matched = false;
                if (page < pageCount - 1) { // Next Page
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page++;
                    }
                }
                if (!matched && page > 0) { // Previous Page
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page--;
                    }
                }
                if (!matched && hasFilter) { // Clear Filter
                    matched = choice == action;
                    action++;
                    if (matched) {
                        priorityFilter = null;
                        typeFilter = null;
                        page = 0;
                    }
                }
            }
        }
    }

    // view flow: pick a record from the current page, then run its action menu
    private void viewTask(LinkedListInterface<Task> pageList) {
        if (pageList.isEmpty()) {
            ui.printNoRecords();
            return;
        }
        int num = ui.inputListIndex("task", pageList.size());
        if (num == 0) {
            return;
        }
        Task task = pageList.get(num - 1);
        if (task != null) {
            handleTaskActions(task);
        }
    }

    // select-entity action loop for one task: details -> action -> details
    private void handleTaskActions(Task task) {
        while (true) {
            ui.printTaskDetails(task);

            int action = ui.getTaskActionChoice();
            if (action == 0) {
                return;
            }

            switch (action) {
                case 1:
                    updateTaskFieldMenu(task);
                    break;
                case 2: {
                    String newStatus = ui.inputTaskStatus(task.getTaskStatus());
                    if (newStatus == null) {
                        ui.printTaskStatusDenied();
                    } else if (updateTaskStatus(task.getTaskId(), newStatus)) {
                        ui.printSuccess();
                    } else {
                        ui.printTaskStatusDenied();
                    }
                    break;
                }
                case 3:
                    if (assignTaskToRoom(task.getTaskId(), ui.inputRoomId())) {
                        ui.printSuccess();
                    } else {
                        ui.printNotFound();
                    }
                    break;
                case 4:
                    ui.listAllAssignments(assignmentListToTable(getAssignmentsByTask(task.getTaskId())));
                    ui.pressEnterToContinue();
                    break;
                case 5:
                    if (rollbackTaskStatus(task.getTaskId())) {
                        ui.printSuccess();
                    } else {
                        ui.printNoPreviousStatus();
                    }
                    break;
                case 6:
                    if (removeTask(task.getTaskId())) {
                        ui.printSuccess();
                        return; // task no longer exists; back to the list
                    }
                    ui.printNotFound();
                    break;
                default:
                    break;
            }

            task = getTaskById(task.getTaskId()); // re-read so details stay fresh
        }
    }

    public String createTask(String taskName, String taskType, TaskPriority taskPriority, LocalDateTime startDateTime,
            String roomId) {

        for (int i = 0; i < taskList.size(); i++) { // size() = current record count of the list
            if (taskList.get(i).getTaskName().equalsIgnoreCase(taskName)) { // get(i) = record at index i
                return null;
            }
        }

        // TODO: FUTURE INTEGRATION — validate roomId against RoomDAO.
        //   When RoomDAO is shared, reject tasks with an unknown roomId here
        //   and in guestCleaningRequestMenu / simulateGuestCheckoutMenu.
        //   For now, roomId is accepted as-is (may be null or blank).

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

    // loop: pick a field, edit it, show updated details, until Back
    private void updateTaskFieldMenu(Task task) {
        String[] fields = {"Task Name", "Task Type", "Task Priority", "Start Date & Time"};
        while (true) {
            int field = ui.inputFieldChoice(fields);
            if (field == 0) {
                return;
            }
            String value = switch (field) {
                case 1 -> ui.inputTaskName();
                case 2 -> ui.inputTaskType();
                case 3 -> ui.inputTaskPriority().name();
                default -> {
                    LocalDateTime parsedDateTime = ui.inputStartDateTime();
                    if (!canRescheduleTask(task, parsedDateTime)) {
                        ui.printScheduleConflict();
                        yield null;
                    }
                    yield parsedDateTime.toString();
                }
            };
            if (value == null) {
                continue;
            }
            if (updateTaskField(task.getTaskId(), field, value)) {
                ui.printSuccess();
            } else {
                ui.printNotFound();
                return;
            }
            task = getTaskById(task.getTaskId()); // re-read so details stay fresh
            ui.printTaskDetails(task);
        }
    }

    // update only one chosen field of a task record
    public boolean updateTaskField(String taskId, int field, String value) {
        for (int i = 0; i < taskList.size(); i++) {
            Task task = taskList.get(i);
            if (task.getTaskId().equals(taskId)) {
                switch (field) {
                    case 1 -> task.setTaskName(value);
                    case 2 -> task.setTaskType(value);
                    case 3 -> task.setTaskPriority(TaskPriority.fromString(value));
                    default -> task.setStartDateTime(LocalDateTime.parse(value));
                }
                taskDAO.saveTaskList(taskList);
                return true;
            }
        }
        return false;
    }

    /**
     * True when moving the task to newStart keeps every non-cancelled
     * assignment free of overlaps with the staff's other tasks (and inside
     * the shift boundaries). A task without assignments is always movable.
     */
    private boolean canRescheduleTask(Task task, LocalDateTime newStart) {
        if (task == null || newStart == null || task.getStartDateTime() == null
                || newStart.equals(task.getStartDateTime())) {
            return true;
        }

        refreshTaskAssignments(); // always retrieve the latest records

        LocalDateTime windowEnd = newStart.plusMinutes(CLEANING_DURATION_MINUTES);

        for (int i = 0; i < taskAssignmentList.size(); i++) { // size() = current record count of the list
            TaskAssignment assignment = taskAssignmentList.get(i); // get(i) = record at index i

            if (!task.getTaskId().equals(assignment.getAssignedTaskId())
                    || assignment.getAssignedStaffId() == null
                    || "Cancelled".equalsIgnoreCase(assignment.getStatus())) {
                continue;
            }

            Staff staff = getStaffById(assignment.getAssignedStaffId());
            if (staff == null) {
                continue;
            }

            if (!isStaffFreeForTask(staff, newStart, windowEnd, task.getTaskId(), taskAssignmentList)) {
                return false;
            }
        }

        return true;
    }

    public boolean updateTaskStatus(String taskId, String status) {

        TaskStatus taskStatus = TaskStatus.fromString(status);
        if (taskStatus == null) {
            return false;
        }

        for (int i = 0; i < taskList.size(); i++) { // size() = current record count of the list
            Task task = taskList.get(i); // get(i) = record at index i

            if (task.getTaskId().equals(taskId)) {

                TaskStatus current = task.getTaskStatus();

                // guardrail: the transition must be allowed by the status matrix
                if (!isAllowedTaskTransition(current, taskStatus)) {
                    return false;
                }

                // COMPLETED means the cleaning task was inspected and approved;
                // it may only be set when every active worker is done/inspected
                // (a task without workers may be completed directly)
                if (taskStatus == TaskStatus.COMPLETED) {
                    refreshTaskAssignments(); // always retrieve the latest records
                    if (!isTaskFullyFinished(taskId, taskAssignmentList)
                            && countActiveWorkers(taskId, taskAssignmentList) > 0) {
                        return false;
                    }
                }

                // push the previous status onto the persisted rollback stack
                // before applying (a linked list used as a stack)
                if (current != null) {
                    task.getStatusHistory().addFront(current); // push
                }

                task.setTaskStatus(taskStatus);

                try {
                    // cancelling a task frees its workers: every non-cancelled
                    // worker assignment is cancelled so the schedule gaps reopen
                    if (taskStatus == TaskStatus.CANCELLED) {
                        cancelTaskWorkers(taskId);
                    }

                    // a Housekeeping task that passes inspection makes the room
                    // available again for the next guest
                    if (taskStatus == TaskStatus.COMPLETED) {
                        setRoomStatus(task.getRoomId(), RoomStatus.AVAILABLE);
                    }
                } catch (Exception e) {
                    // room / worker update failed — undo the task status change
                    task.setTaskStatus(current);
                    if (current != null) {
                        task.getStatusHistory().removeFront(); // undo push
                    }
                    ConsoleUtil.printError("Status update failed during room/worker update: " + e.getMessage());
                    return false;
                }

                // every task status change is recorded as a TaskAssignmentChange
                // history record (separate entity)
                appendTaskStatusChange(task, status, LocalDateTime.now());

                // FUTURE INTEGRATION: trigger a Notification for any staff
                // currently assigned to this task.
                // TODO future integration - Staff Notification hook:
                //   params needed: staffId(s) of the task's workers, notification type
                //   (e.g. "TASK_APPROVED" / "TASK_CANCELLED"), message text, timestamp
                //   output: a Notification record appended to notifications.json

                taskDAO.saveTaskList(taskList);

                return true;
            }
        }

        return false;
    }

    /**
     * Guardrail matrix for task status transitions. From a finished /
     * inspected task (COMPLETED) there is no forward move; reverting is
     * done through the rollback stack. A cancelled task may be re-opened.
     */
    private boolean isAllowedTaskTransition(TaskStatus current, TaskStatus next) {
        if (current == null) {
            return next == TaskStatus.PENDING || next == TaskStatus.CANCELLED;
        }
        return switch (current) {
            case PENDING -> next == TaskStatus.IN_PROGRESS || next == TaskStatus.CANCELLED;
            case IN_PROGRESS -> next == TaskStatus.CANCELLED || next == TaskStatus.COMPLETED;
            case COMPLETED -> false; // revert only via rollback
            case CANCELLED -> next == TaskStatus.IN_PROGRESS; // reopening goes directly to IN_PROGRESS
        };
    }

    /**
     * Rolls back the last task status change using the per-task status stack.
     * A cancelled task that gets rolled back is re-opened, its workers stay
     * freed, and the task is reassigned to a new free worker when possible.
     * Returns true when a previous status existed and was restored.
     */
    public boolean rollbackTaskStatus(String taskId) {

        for (int i = 0; i < taskList.size(); i++) { // size() = current record count of the list
            Task task = taskList.get(i); // get(i) = record at index i

            if (task.getTaskId().equals(taskId)) {

                LinkedListInterface<TaskStatus> stack = task.getStatusHistory();
                if (stack.isEmpty()) {
                    return false; // nothing to roll back
                }

                TaskStatus previous = stack.removeFront(); // pop
                TaskStatus current = task.getTaskStatus();

                task.setTaskStatus(previous);

                try {
                    // leaving the inspected/finished state puts the room back into cleaning
                    if (current == TaskStatus.COMPLETED) {
                        setRoomStatus(task.getRoomId(), RoomStatus.CLEANING);
                    }
                } catch (Exception e) {
                    // room status update failed — undo the task status rollback
                    task.setTaskStatus(current);
                    stack.addFront(previous); // push back
                    ConsoleUtil.printError("Rollback failed during room status update: " + e.getMessage());
                    return false;
                }

                // audit entry is written even if later steps fail
                appendTaskStatusChange(task, previous.name(), LocalDateTime.now());
                taskDAO.saveTaskList(taskList);

                // re-opening a cancelled task: workers were freed when it was
                // cancelled, so find a fresh worker if one is available now
                if (current == TaskStatus.CANCELLED) {
                    reassignTask(taskId, null);
                }

                return true;
            }
        }

        return false;
    }

    /**
     * Cancels every non-cancelled worker assignment of a task so the
     * workers' schedule gaps reopen. Each cancellation is logged.
     */
    private void cancelTaskWorkers(String taskId) {

        refreshTaskAssignments(); // always retrieve the latest records

        for (int i = 0; i < taskAssignmentList.size(); i++) { // size() = current record count of the list
            TaskAssignment assignment = taskAssignmentList.get(i); // get(i) = record at index i

            if (!taskId.equals(assignment.getAssignedTaskId())
                    || "Cancelled".equalsIgnoreCase(assignment.getStatus())) {
                continue;
            }

            assignment.setStatus("Cancelled");
            taskAssignmentDAO.saveTaskAssignmentList(taskAssignmentList);
            appendAssignmentChange(assignment, "Cancelled", LocalDateTime.now());
        }
    }

    private void setRoomStatus(String roomId, RoomStatus status) {
        if (roomId == null || roomId.isBlank()) {
            return;
        }
        LinkedListInterface<Room> rooms = new LinkedList<>();
        roomDAO.loadFromFile(rooms);
        for (int i = 0; i < rooms.size(); i++) { // size() = current record count of the list
            Room room = rooms.get(i); // get(i) = record at index i
            if (room.getRoomId().equalsIgnoreCase(roomId)) {
                room.setRoomStatus(status);
                roomDAO.saveToFile(rooms);
                return;
            }
        }
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

                // cancelling the task frees its workers' schedules
                cancelTaskWorkers(taskId);

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
            ui.printDuplicateNameError();
        } else {
            ui.printTaskId(taskId);
            ui.printSuccess();
        }
    }

    // -------------------- private helpers --------------------

    private String generateTaskId() {

        int max = 0;

        for (int i = 0; i < taskList.size(); i++) { // size() = current record count of the list
            String taskId = taskList.get(i).getTaskId(); // get(i) = record at index i

            int number = parseIdSuffix(taskId);

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

        String staffFilter = null;
        String taskFilter = null;
        int page = 0;

        while (true) {
            LinkedListInterface<TaskAssignment> display;
            if (staffFilter != null) {
                display = getAssignmentsByStaff(staffFilter);
            } else if (taskFilter != null) {
                display = getAssignmentsByTask(taskFilter);
            } else {
                refreshTaskAssignments(); // always retrieve the latest records
                display = getAllAssignments();
            }

            boolean hasFilter = staffFilter != null || taskFilter != null;
            int pageCount = Math.max(1, (display.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            if (page >= pageCount) {
                page = pageCount - 1; // clamp after the list shrank
            }

            LinkedListInterface<TaskAssignment> pageList = pageOf(display, page);
            int choice = ui.printAssignmentListMenu(assignmentListToLines(pageList), page, pageCount, hasFilter);

            if (choice == 0) {
                break;
            }

            // the numbering mirrors the Actions section in the UI: Next /
            // Previous / Clear only occupy a number when they are shown
            int action = 1;
            if (choice == action++) { // 1. View Details
                viewAssignment(pageList);
            } else if (choice == action++) { // 2. + New Assignment
                assignStaffToTaskMenu();
                ui.pressEnterToContinue();
            } else if (choice == action++) { // 3. Filter by Staff
                staffFilter = ui.inputStaffId();
                taskFilter = null;
                page = 0;
            } else if (choice == action++) { // 4. Filter by Task
                taskFilter = ui.inputTaskId();
                staffFilter = null;
                page = 0;
            } else if (choice == action++) { // 5. View Tasks by Room
                viewTasksByRoomMenu();
                ui.pressEnterToContinue();
            } else if (choice == action++) { // 6. Simulate Guest Checkout
                simulateGuestCheckoutMenu();
                ui.pressEnterToContinue();
            } else if (choice == action++) { // 7. Guest Cleaning Request
                guestCleaningRequestMenu();
                ui.pressEnterToContinue();
            } else if (choice == action++) { // 8. View All Change History
                viewChangeHistoryMenu();
                ui.pressEnterToContinue();
            } else {
                boolean matched = false;
                if (page < pageCount - 1) { // Next Page
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page++;
                    }
                }
                if (!matched && page > 0) { // Previous Page
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page--;
                    }
                }
                if (!matched && hasFilter) { // Clear Filter
                    matched = choice == action;
                    action++;
                    if (matched) {
                        staffFilter = null;
                        taskFilter = null;
                        page = 0;
                    }
                }
            }
        }
    }

    // view flow: pick a record from the current page, then run its action menu
    private void viewAssignment(LinkedListInterface<TaskAssignment> pageList) {
        if (pageList.isEmpty()) {
            ui.printNoRecords();
            return;
        }
        int num = ui.inputListIndex("assignment", pageList.size());
        if (num == 0) {
            return;
        }
        TaskAssignment assignment = pageList.get(num - 1);
        if (assignment != null) {
            handleAssignmentActions(assignment);
        }
    }

    // select-entity action loop for one assignment: details -> action -> details
    private void handleAssignmentActions(TaskAssignment assignment) {
        while (true) {
            ui.printAssignmentDetails(assignment,
                    assignment.getAssignedStaffId() == null ? null : getStaffById(assignment.getAssignedStaffId()),
                    assignment.getAssignedTaskId() == null ? null : getTaskById(assignment.getAssignedTaskId()));

            int action = ui.getAssignmentActionChoice();
            if (action == 0) {
                return;
            }

            switch (action) {
                case 1: {
                    String result = updateAssignmentStatus(assignment.getTaskAssignmentId(),
                            ui.inputWorkerAssignmentStatus());
                    if ("NOT_FOUND".equals(result)) {
                        ui.printNotFound();
                    } else if ("ALL_DONE".equals(result)) {
                        ui.printAllWorkersDoneHint(assignment.getTaskAssignmentId());
                    } else if ("REASSIGNED".equals(result)) {
                        ui.printReassigned();
                    } else {
                        ui.printSuccess();
                    }
                    break;
                }
                case 2: {
                    String taskId = ui.inputTaskId();
                    Task targetTask = getTaskById(taskId);
                    if (targetTask == null) {
                        ui.printNotFound();
                        break;
                    }
                    LinkedListInterface<Staff> eligible = getEligibleStaffForTask(targetTask);
                    if (eligible.isEmpty()) {
                        ui.printNoStaffFreeForTask();
                        break;
                    }
                    int choice = ui.printEligibleStaffMenu(eligible);
                    if (choice == 0) {
                        break;
                    }
                    if (reassignAssignment(assignment.getTaskAssignmentId(),
                            eligible.get(choice - 1).getStaffId(), taskId)) {
                        ui.printSuccess();
                    } else {
                        ui.printNotFound();
                    }
                    break;
                }
                case 3:
                    ui.listAllChanges(changeListToTable(getChangesByAssignment(assignment.getTaskAssignmentId())));
                    break;
                default:
                    break;
            }

            refreshTaskAssignments(); // always retrieve the latest records
            assignment = getAssignmentById(assignment.getTaskAssignmentId());
            if (assignment == null) {
                return; // assignment is gone; back to the list
            }
        }
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

        if (best == null) {
            // no housekeeping staff is available: keep the task unassigned
            // (a null-staff "Pending" assignment could never be resolved)
            updateTaskStartDateTime(taskId, requestedStart);
            return getTaskById(taskId);
        }

        // set task start to the scheduled slot (deferred if staff are busy)
        LocalDateTime scheduledStart = best.slotStart;
        updateTaskStartDateTime(taskId, scheduledStart);

        insertAssignment(generateAssignmentId(),
                "Pending", scheduledStart,
                best.staff,
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
     * parent task is set back to Pending (bypassing the normal transition
     * matrix — this is an internal system action) and auto-reassigned via
     * the earliest-free-slot timetable.
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
            // NOTE: this bypasses the transition matrix — it is an internal
            // system action, not a user-driven status change.
            Task parentTask = getTaskById(taskId);
            if (parentTask != null && parentTask.getTaskStatus() != TaskStatus.PENDING) {
                TaskStatus prevStatus = parentTask.getTaskStatus();
                parentTask.getStatusHistory().addFront(prevStatus); // push for rollback
                parentTask.setTaskStatus(TaskStatus.PENDING);
                appendTaskStatusChange(parentTask, "Pending", LocalDateTime.now());
                taskDAO.saveTaskList(taskList);
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
        LinkedListInterface<Room> rooms = new LinkedList<>();
        roomDAO.loadFromFile(rooms);
        for (int i = 0; i < rooms.size(); i++) {
            if (rooms.get(i).getRoomId().equals(roomId)) {
                return rooms.get(i);
            }
        }
        return null;
    }

    private void guestCleaningRequestMenu() {
        String roomId = ui.inputRoomId();

        // TODO future integration - room id validation:
        //   params needed: roomId (String)
        //   output: boolean (true when roomId exists in RoomDAO)
        //   once RoomDAO is shared, reject requests for unknown rooms here
        //   and in assignTaskToRoomMenu.

        TaskPriority priority = ui.inputTaskPriority();
        LocalDateTime requestedStart = ui.inputCheckoutDateTime();

        Task task = createCleaningTask(roomId, "Housekeeping", priority, requestedStart);

        if (task == null) {
            ui.printTaskAlreadyExists();
            return;
        }

        // auto-assign the new cleaning task to the earliest free housekeeping
        // staff, exactly like a guest checkout (same timetable logic)
        Task assigned = autoAssignTask(task.getTaskId(), requestedStart, roomId);

        ui.printTaskDetails(assigned == null ? task : assigned);

        // no staff was available: the task stays Pending for manual assignment
        if (assigned != null && assigned.getTaskAssignments().isEmpty()) {
            ui.printNoStaffFreeForTask();
        } else {
            ui.printSuccess();
        }
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
                    || "Work Finished".equalsIgnoreCase(assignment.getStatus())
                    || "Inspected".equalsIgnoreCase(assignment.getStatus()))) {
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

        // every assignment creation (manual, checkout, auto-reassign) is
        // recorded in the change history so the trail is complete
        appendAssignmentChange(assignment, "Assigned", LocalDateTime.now());

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

            int number = parseIdSuffix(assignmentId);

            if (number > max) {
                max = number;
            }
        }

        return String.format("ASG%012d", max + 1);
    }

    // -------------------- private menu handlers --------------------

    private void assignStaffToTaskMenu() {
        while (true) {
            String taskId = ui.inputTaskId();
            Task task = getTaskById(taskId);
            if (task == null) {
                ui.printTaskNotFound();
                return;
            }

            // staff picker: only Housekeeping, Available, non-resigned staff
            // who have a free window for this task may be chosen
            LinkedListInterface<Staff> eligible = getEligibleStaffForTask(task);
            if (eligible.isEmpty()) {
                ui.printNoStaffFreeForTask();
                continue; // retry with another task
            }

            int choice = ui.printEligibleStaffMenu(eligible);
            if (choice == 0) {
                return;
            }

            String status = ui.inputAssignmentStatus();
            LocalDateTime dateTimeAssigned = ui.inputDateAssigned();

            String result = createAssignment(eligible.get(choice - 1).getStaffId(), taskId, status, dateTimeAssigned);

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
            return;
        }
    }

    // Housekeeping + Available + not Resigned + free for the task's 60-min window
    private LinkedListInterface<Staff> getEligibleStaffForTask(Task task) {
        LinkedListInterface<Staff> eligible = new LinkedList<>();
        if (task == null || task.getStartDateTime() == null) {
            return eligible;
        }

        refreshTaskAssignments(); // always retrieve the latest records
        LinkedListInterface<TaskAssignment> snapshot = taskAssignmentList;

        LocalDateTime windowStart = task.getStartDateTime();
        LocalDateTime windowEnd = windowStart.plusMinutes(CLEANING_DURATION_MINUTES);

        for (int i = 0; i < getAllStaffs().size(); i++) { // size() = current record count of the list
            Staff staff = getAllStaffs().get(i); // get(i) = record at index i

            if (!"Housekeeping".equalsIgnoreCase(staff.getDepartment())) {
                continue;
            }
            if (!"Available".equalsIgnoreCase(staff.getAvailabilityStatus())) {
                continue;
            }
            if (isStaffFreeForTask(staff, windowStart, windowEnd, task.getTaskId(), snapshot)) {
                eligible.addBack(staff); // append to the end of the eligible list
            }
        }

        return eligible;
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
        TaskAssignment assignment = null;
        if (!task.getTaskAssignments().isEmpty()) {
            assignment = task.getTaskAssignments().getFront();
            staff = assignment.getAssignedStaffId() == null ? null
                    : getStaffById(assignment.getAssignedStaffId());
        }

        boolean deferred = task.getStartDateTime() != null && task.getStartDateTime().isAfter(checkoutTime);

        ui.printGuestCheckoutTask(task, staff, assignment, deferred);

        // no staff was available: the task stays Pending for manual assignment
        if (task.getTaskAssignments().isEmpty()) {
            ui.printNoStaffFreeForTask();
        }
    }

    private void viewChangeHistoryMenu() {
        String taskId = ui.inputOptionalTaskId();
        LinkedListInterface<TaskAssignmentChange> changes = taskId == null ? getAllChanges() : getChangesByTask(taskId);
        ui.listAllChanges(changeListToTable(changes));
    }

    // convert assignment list to one-line summaries for the selection menu
    private String[] assignmentListToLines(LinkedListInterface<TaskAssignment> assignmentList) {
        String[] lines = new String[assignmentList.size()];
        for (int i = 0; i < assignmentList.size(); i++) { // size() = current record count of the list
            TaskAssignment assignment = assignmentList.get(i); // get(i) = record at index i
            Staff staff = assignment.getAssignedStaffId() == null ? null
                    : getStaffById(assignment.getAssignedStaffId());
            Task task = assignment.getAssignedTaskId() == null ? null : getTaskById(assignment.getAssignedTaskId());

            String staffName = staff == null ? "-" : staff.getStaffName();
            int space = staffName.indexOf(' ');
            if (space > 0) {
                staffName = staffName.substring(0, space); // first name keeps the line short
            }
            String taskName = task == null ? "-" : task.getTaskName();
            String when = assignment.getDateTimeAssigned() == null ? "-"
                    : assignment.getDateTimeAssigned().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));

            lines[i] = assignment.getTaskAssignmentId() + " | " + staffName + " → " + taskName
                    + " | " + assignment.getStatus() + " | " + when;
        }
        return lines;
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

    public LinkedListInterface<TaskAssignmentChange> getChangesByAssignment(String assignmentId) {
        LinkedListInterface<TaskAssignmentChange> filteredList = new LinkedList<>();
        refreshTaskAssignmentChanges(); // always retrieve the latest records

        for (int i = 0; i < taskAssignmentChangeList.size(); i++) { // size() = current record count of the list
            TaskAssignmentChange change = taskAssignmentChangeList.get(i); // get(i) = record at index i

            if (change.getTaskAssignmentId() != null && change.getTaskAssignmentId().equals(assignmentId)) {
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

            int number = parseIdSuffix(changeId);

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