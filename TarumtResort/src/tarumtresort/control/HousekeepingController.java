package tarumtresort.control;

import java.time.LocalDateTime;
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
import tarumtresort.entity.TaskStatusChange;
import tarumtresort.entity.enums.AvailabilityStatus;
import tarumtresort.entity.enums.Department;
import tarumtresort.entity.enums.RoomStatus;
import tarumtresort.entity.enums.StaffRole;
import tarumtresort.entity.enums.TaskPriority;
import tarumtresort.entity.enums.TaskStatus;
import tarumtresort.entity.enums.TaskType;
import tarumtresort.report.HousekeepingReport.HousekeepingReportController;
import tarumtresort.utility.ConsoleUtil;
import tarumtresort.utility.DateTimeUtil;

/**
 * 
 * @author Brian
 * 
 */
public class HousekeepingController {

    private static final int PAGE_SIZE = 10;

    // UI declaration
    private HousekeepingUI ui;

    // DAO declaration
    private final TaskDAO taskDAO = new TaskDAO();
    private final StaffDAO staffDAO = new StaffDAO();
    private final TaskAssignmentDAO taskAssignmentDAO = new TaskAssignmentDAO();
    private final TaskAssignmentChangeDAO taskAssignmentChangeDAO = new TaskAssignmentChangeDAO();
    private final RoomDAO roomDAO = new RoomDAO();

    // list declaration
    private final LinkedListInterface<Task> taskList = new LinkedList<>();
    private final LinkedListInterface<Staff> staffList = new LinkedList<>();
    private final LinkedListInterface<TaskAssignment> taskAssignmentList = new LinkedList<>();
    private final LinkedListInterface<TaskAssignmentChange> taskAssignmentChangeList = new LinkedList<>();

    /**
     * Loads all persisted entities into memory.
     */
    public HousekeepingController() {
        LinkedList<Task> tasks = taskDAO.retrieveTaskList();
        for (int i = 0; i < tasks.size(); i++) {
            taskList.addBack(tasks.get(i));
        }
        LinkedList<Staff> staff = staffDAO.retrieveStaffList();
        for (int i = 0; i < staff.size(); i++) {
            staffList.addBack(staff.get(i));
        }
        LinkedList<TaskAssignment> assignments = taskAssignmentDAO.retrieveTaskAssignmentList();
        for (int i = 0; i < assignments.size(); i++) {
            taskAssignmentList.addBack(assignments.get(i));
        }
        LinkedList<TaskAssignmentChange> changes = taskAssignmentChangeDAO.retrieveTaskAssignmentChangeList();
        for (int i = 0; i < changes.size(); i++) {
            taskAssignmentChangeList.addBack(changes.get(i));
        }
    }

    /**
     * Builds the controller around the caller's shared scanner.
     *
     * @param ui the HousekeepingUI bound to the shared scanner
     */
    public HousekeepingController(HousekeepingUI ui) {
        this();
        this.ui = ui;
    }

    private void refreshTaskAssignments() {
        taskAssignmentList.clear();
        LinkedList<TaskAssignment> loaded = taskAssignmentDAO.retrieveTaskAssignmentList();
        for (int i = 0; i < loaded.size(); i++) {
            taskAssignmentList.addBack(loaded.get(i));
        }
    }

    private void refreshTaskAssignmentChanges() {
        taskAssignmentChangeList.clear();
        LinkedList<TaskAssignmentChange> loaded = taskAssignmentChangeDAO.retrieveTaskAssignmentChangeList();
        for (int i = 0; i < loaded.size(); i++) {
            taskAssignmentChangeList.addBack(loaded.get(i));
        }
    }

    public void runHousekeeping() {
        HousekeepingReportController reportController = new HousekeepingReportController(ui.getScanner());
        while (true) {
            int choice = ui.getMenuChoice();
            if (choice == 0) {
                ui.printExitMessage();
                return;
            }
            switch (choice) {
                case 1 -> runTaskManagement();
                case 2 -> runStaffManagement();
                case 3 -> reportController.generateRoomCleaningPerformanceReport();
                case 4 -> reportController.generateStaffProductivityReport();
                default -> ui.printInvalidChoice();
            }
        }
    }

    // =============== task management ===============

    private void runTaskManagement() {
        TaskStatus statusFilter = null;
        TaskPriority priorityFilter = null;
        String searchTerm = null;
        int page = 0;

        while (true) {
            LinkedListInterface<Task> display = getAllTasks();
            if (statusFilter != null) {
                display = getTasksByStatus(statusFilter);
            } else if (priorityFilter != null) {
                display = getTasksByPriority(priorityFilter);
            }
            if (searchTerm != null) {
                display = searchTasks(display, searchTerm);
            }

            boolean hasFilter = statusFilter != null || priorityFilter != null;
            boolean hasSearch = searchTerm != null;
            int pageCount = Math.max(1, (display.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            if (page >= pageCount) {
                page = pageCount - 1;
            }

            LinkedListInterface<Task> pageList = pageOf(display, page);
            int choice = ui.printTaskListMenu(pageList, page, pageCount, statusFilter, priorityFilter, searchTerm);

            if (choice == 0) {
                break;
            }

            int action = 1;
            if (choice == action++) { // 1. View Details
                viewTask(pageList);
            } else if (choice == action++) { // 2. Add New Task
                addTaskMenu();
            } else if (choice == action++) { // 3. Update Status
                int index = ui.inputListIndex("task", pageList.size());
                if (index > 0) {
                    quickUpdateTaskStatus(pageList.get(index - 1));
                }
            } else if (choice == action++) { // 4. Roll Back
                int index = ui.inputListIndex("task", pageList.size());
                if (index > 0) {
                    quickRollbackTaskStatus(pageList.get(index - 1));
                }
            } else if (choice == action++) { // 5. Add Filter
                int dimension = ui.inputTaskFilterDimension();
                if (dimension == 1) {
                    statusFilter = ui.inputTaskStatusFilter();
                    priorityFilter = null;
                } else if (dimension == 2) {
                    priorityFilter = ui.inputTaskPriorityFilter();
                    statusFilter = null;
                }
                page = 0;
            } else {
                boolean matched = false;
                if (hasFilter) {
                    matched = choice == action;
                    action++;
                    if (matched) {
                        statusFilter = null;
                        priorityFilter = null;
                        page = 0;
                    }
                }
                if (!matched && !hasSearch) {
                    matched = choice == action;
                    action++;
                    if (matched) {
                        searchTerm = ui.inputSearchTerm();
                        page = 0;
                    }
                }
                if (!matched && hasSearch) {
                    matched = choice == action;
                    action++;
                    if (matched) {
                        searchTerm = null;
                        page = 0;
                    }
                }
                if (!matched && page < pageCount - 1) {
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page++;
                    }
                }
                if (!matched && page > 0) {
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page--;
                    }
                }
            }
        }
    }

    private void viewTask(LinkedListInterface<Task> pageList) {
        int index = ui.inputListIndex("task", pageList.size());
        if (index == 0) {
            return;
        }
        Task task = pageList.get(index - 1);
        while (true) {
            ui.printTaskDetails(task, getActiveAssignment(task.getTaskId()));
            int action = ui.getTaskActionChoice();
            if (action == 0) {
                return;
            }
            switch (action) {
                case 1 -> editTaskDetailsMenu(task);
                case 2 -> quickUpdateTaskStatus(task);
                case 3 -> quickRollbackTaskStatus(task);
                case 4 -> viewStaffAssignedMenu(task);
                case 5 -> assignReassignMenu(task);
                case 6 -> {
                    deleteTaskMenu(task);
                    return;
                }
                case 7 -> viewTaskChangeHistory(task);
                default -> ui.printInvalidChoice();
            }
            task = getTaskById(task.getTaskId());
            if (task == null) {
                return;
            }
        }
    }

    private void quickUpdateTaskStatus(Task task) {
        TaskStatus[] options = getAllowedTransitions(task.getTaskStatus());
        if (options.length == 0) {
            ui.printTaskStatusDenied();
            return;
        }
        TaskStatus newStatus = ui.selectTaskStatus(options);
        if (newStatus == null) {
            return;
        }
        String reason = null;
        if (newStatus == TaskStatus.CANCELLED) {
            reason = ui.inputCancellationReason();
            if (reason == null) {
                return;
            }
        }
        ui.printStatusChangeSummary(task.getTaskId(), task.getTaskStatus(), newStatus, reason);
        if (!ui.confirm("Apply this status change?")) {
            return;
        }
        switch (updateTaskStatus(task, newStatus, reason)) {
            case "UPDATED" -> ui.printSuccess();
            case "TRANSITION_DENIED", "WORKERS_NOT_DONE" -> ui.printTaskStatusDenied();
            default -> ui.printWarning("Task status update failed.");
        }
    }

    private void quickRollbackTaskStatus(Task task) {
        if (!ui.confirm("Roll back the latest task status?")) {
            return;
        }
        switch (rollbackTaskStatus(task)) {
            case "ROLLED_BACK" -> ui.printSuccess();
            case "NO_PREVIOUS" -> ui.printNoPreviousStatus();
            default -> ui.printWarning("Rollback failed.");
        }
    }

    private void addTaskMenu() {
        String taskName = null;
        TaskType taskType = null;
        TaskPriority taskPriority = null;
        String roomId = null;
        LocalDateTime startDateTime = null;
        while (true) {
            if (taskName == null) {
                taskName = ui.inputTaskName();
            }
            if (taskType == null) {
                taskType = ui.inputTaskType();
            }
            if (taskPriority == null) {
                taskPriority = ui.inputTaskPriority();
            }
            if (roomId == null) {
                roomId = ui.inputOptionalRoomId();
            }
            if (startDateTime == null) {
                startDateTime = ui.inputStartDateTime();
            }
            ui.printTaskCreationSummary(taskName, taskType, taskPriority, roomId, startDateTime);
            if (!ui.confirm("Save this task?")) {
                int field = ui.inputTaskFieldChoice();
                if (field == 0) {
                    return;
                }
                switch (field) {
                    case 1 -> taskName = ui.inputTaskName();
                    case 2 -> taskType = ui.inputTaskType();
                    case 3 -> taskPriority = ui.inputTaskPriority();
                    case 4 -> roomId = ui.inputOptionalRoomId();
                    default -> startDateTime = ui.inputStartDateTime();
                }
                continue;
            }
            String taskId = createTask(taskName, taskType, taskPriority, startDateTime, roomId);
            if (taskId == null) {
                ui.printDuplicateNameError();
                return;
            }
            ui.printTaskId(taskId);
            ui.printSuccess();
            if (ui.confirm("Assign a staff member to this task now?")) {
                assignReassignMenu(getTaskById(taskId));
            }
            return;
        }
    }

    private void editTaskDetailsMenu(Task task) {
        if (task.getTaskStatus() == TaskStatus.COMPLETED || task.getTaskStatus() == TaskStatus.CANCELLED) {
            ui.printTaskClosed();
            return;
        }
        String name = task.getTaskName();
        TaskType type = task.getTaskType();
        TaskPriority priority = task.getTaskPriority();
        String roomId = task.getRoomId();
        LocalDateTime start = task.getStartDateTime();
        boolean changed = false;
        while (true) {
            int field = ui.inputTaskFieldChoice();
            if (field == 0) {
                break;
            }
            switch (field) {
                case 1 -> {
                    String input = ui.inputTaskName();
                    if (!task.getTaskName().equalsIgnoreCase(input) && duplicateTaskName(input, task)) {
                        ui.printDuplicateNameError();
                        continue;
                    }
                    name = input;
                }
                case 2 -> type = ui.inputTaskType();
                case 3 -> priority = ui.inputTaskPriority();
                case 4 -> roomId = ui.inputOptionalRoomId();
                default -> start = ui.inputStartDateTime();
            }
            changed = true;
        }
        if (!changed) {
            return;
        }
        ui.printTaskEditSummary(name, type, priority, roomId, start);
        if (!ui.confirm("Apply changes?")) {
            return;
        }
        task.setTaskName(name);
        task.setTaskType(type);
        task.setTaskPriority(priority);
        task.setRoomId(roomId);
        task.setStartDateTime(start);
        taskDAO.saveTaskList(taskList);
        ui.printSuccess();
    }

    private void deleteTaskMenu(Task task) {
        if (!ui.confirm("Delete this task? This cannot be undone.")) {
            return;
        }
        if (!task.getTaskName().equalsIgnoreCase(ui.inputConfirmName(task.getTaskName()))) {
            ui.printInvalidChoice();
            return;
        }
        task.setDeleted(true);
        taskDAO.saveTaskList(taskList);
        ui.printSuccess();
    }

    private void assignReassignMenu(Task task) {
        if (task.getTaskStatus() == TaskStatus.COMPLETED || task.getTaskStatus() == TaskStatus.CANCELLED) {
            ui.printTaskClosed();
            return;
        }
        TaskAssignment active = getActiveAssignment(task.getTaskId());
        if (active != null) {
            if (!ui.confirm("Task already has an active assignment. End it (Cancelled) and reassign?")) {
                return;
            }
            endAssignment(active, TaskStatus.CANCELLED);
        }
        int mode = ui.inputAssignMode();
        if (mode == 0) {
            return;
        }
        String result;
        if (mode == 1) {
            Staff staff = findEarliestAvailableStaff(task.getTaskType());
            if (staff == null) {
                ui.printNoStaffFreeForTask();
                return;
            }
            ui.printStaffDetails(staff, getActiveAssignmentOfStaff(staff.getStaffId()));
            if (!ui.confirm("Assign " + staff.getStaffName() + " to this task?")) {
                return;
            }
            result = createAssignment(task, staff);
        } else {
            LinkedListInterface<Staff> eligible = getEligibleStaffByRole(task.getTaskType());
            if (eligible.isEmpty()) {
                ui.printNoStaffFreeForTask();
                return;
            }
            int choice = ui.selectStaff(eligible);
            if (choice == 0) {
                return;
            }
            Staff staff = eligible.get(choice - 1);
            ui.printStaffDetails(staff, getActiveAssignmentOfStaff(staff.getStaffId()));
            if (!ui.confirm("Assign " + staff.getStaffName() + " to this task?")) {
                return;
            }
            result = createAssignment(task, staff);
        }
        switch (result) {
            case "TASK_CLOSED" -> ui.printTaskClosed();
            case "TASK_ALREADY_ASSIGNED" -> ui.printTaskAlreadyAssigned();
            case "STAFF_UNAVAILABLE" -> ui.printStaffUnavailable();
            case "NO_TASK", "NO_STAFF" -> ui.printNotFound();
            default -> ui.printSuccess();
        }
    }

    private void viewStaffAssignedMenu(Task task) {
        LinkedListInterface<TaskAssignment> filtered = new LinkedList<>();
        for (int i = 0; i < task.getTaskAssignments().size(); i++) {
            TaskAssignment assignment = task.getTaskAssignments().get(i);
            if (!assignment.isDeleted()) {
                filtered.addBack(assignment);
            }
        }
        ui.listAllAssignments(assignmentListToTable(filtered));
        if (filtered.isEmpty()) {
            ui.pressEnterToContinue();
            return;
        }
        int index = ui.inputListIndex("assignment", filtered.size());
        if (index == 0) {
            return;
        }
        updateAssignmentStatusMenu(filtered.get(index - 1));
    }

    private void updateAssignmentStatusMenu(TaskAssignment assignment) {
        while (true) {
            int choice = ui.getAssignmentActionChoice(assignment);
            if (choice == 0) {
                return;
            }
            if (choice == 1) {
                ui.listAllChanges(changeListToTable(getChangesByAssignment(assignment.getTaskAssignmentId())));
                ui.pressEnterToContinue();
                continue;
            }
            break;
        }
        TaskStatus[] options = getAllowedAssignmentTransitions(assignment.getStatus());
        if (options.length == 0) {
            ui.printTaskStatusDenied();
            return;
        }
        TaskStatus newStatus = ui.selectTaskStatus(options);
        if (newStatus == null) {
            return;
        }
        String reason = null;
        if (newStatus == TaskStatus.CANCELLED) {
            reason = ui.inputCancellationReason();
            if (reason == null) {
                return;
            }
        }
        ui.printAssignmentStatusSummary(assignment.getTaskAssignmentId(), assignment.getStatus(), newStatus, reason);
        if (!ui.confirm("Apply this status change?")) {
            return;
        }
        Task task = assignment.getAssignedTaskId() == null ? null : getTaskById(assignment.getAssignedTaskId());
        String result = updateAssignmentStatus(assignment, newStatus, reason);
        switch (result) {
            case "UPDATED" -> ui.printSuccess();
            case "TASK_COMPLETED" -> {
                ui.printSuccess();
                ui.printTaskAutoCompleted(task);
            }
            case "TRANSITION_DENIED" -> ui.printTaskStatusDenied();
            default -> ui.printWarning("Assignment status update failed.");
        }
        if (newStatus == TaskStatus.CANCELLED && task != null
                && task.getTaskStatus() != TaskStatus.COMPLETED && task.getTaskStatus() != TaskStatus.CANCELLED) {
            if (ui.confirm("Reassign this task to another staff member?")) {
                assignReassignMenu(task);
            }
        }
    }

    private void viewTaskChangeHistory(Task task) {
        ui.listAllChanges(changeListToTable(getChangesByTask(task.getTaskId())));
        ui.pressEnterToContinue();
    }

    // =============== staff management ===============

    private void runStaffManagement() {
        Department departmentFilter = null;
        StaffRole roleFilter = null;
        String searchTerm = null;
        int page = 0;

        while (true) {
            LinkedListInterface<Staff> display = getAllStaffs();
            if (departmentFilter != null) {
                display = getStaffsByDepartment(departmentFilter);
            } else if (roleFilter != null) {
                display = getStaffsByRole(roleFilter);
            }
            if (searchTerm != null) {
                display = searchStaffs(display, searchTerm);
            }

            boolean hasFilter = departmentFilter != null || roleFilter != null;
            boolean hasSearch = searchTerm != null;
            int pageCount = Math.max(1, (display.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            if (page >= pageCount) {
                page = pageCount - 1;
            }

            LinkedListInterface<Staff> pageList = pageOfStaff(display, page);
            int choice = ui.printStaffListMenu(pageList, page, pageCount, departmentFilter, roleFilter, searchTerm);

            if (choice == 0) {
                break;
            }

            int action = 1;
            if (choice == action++) { // 1. View Details
                viewStaff(pageList);
            } else if (choice == action++) { // 2. Add New Staff
                addStaffMenu();
            } else if (choice == action++) { // 3. Add Filter
                int dimension = ui.inputStaffFilterDimension();
                if (dimension == 1) {
                    departmentFilter = ui.inputDepartment();
                    roleFilter = null;
                } else if (dimension == 2) {
                    roleFilter = ui.inputStaffRole();
                    departmentFilter = null;
                }
                page = 0;
            } else {
                boolean matched = false;
                if (hasFilter) {
                    matched = choice == action;
                    action++;
                    if (matched) {
                        departmentFilter = null;
                        roleFilter = null;
                        page = 0;
                    }
                }
                if (!matched && !hasSearch) {
                    matched = choice == action;
                    action++;
                    if (matched) {
                        searchTerm = ui.inputSearchTerm();
                        page = 0;
                    }
                }
                if (!matched && hasSearch) {
                    matched = choice == action;
                    action++;
                    if (matched) {
                        searchTerm = null;
                        page = 0;
                    }
                }
                if (!matched && page < pageCount - 1) {
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page++;
                    }
                }
                if (!matched && page > 0) {
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page--;
                    }
                }
            }
        }
    }

    private void viewStaff(LinkedListInterface<Staff> pageList) {
        int index = ui.inputListIndex("staff", pageList.size());
        if (index == 0) {
            return;
        }
        Staff staff = pageList.get(index - 1);
        while (true) {
            ui.printStaffDetails(staff, getActiveAssignmentOfStaff(staff.getStaffId()));
            int action = ui.getStaffActionChoice();
            if (action == 0) {
                return;
            }
            switch (action) {
                case 1 -> editStaffDetailsMenu(staff);
                case 2 -> startFirstTaskMenu(staff);
                case 3 -> viewStaffAssignmentHistory(staff);
                case 4 -> resignStaffMenu(staff);
                case 5 -> viewStaffChangeHistory(staff);
                default -> ui.printInvalidChoice();
            }
            staff = getStaffById(staff.getStaffId());
            if (staff == null) {
                return;
            }
        }
    }

    private void addStaffMenu() {
        String staffName = null;
        Department department = null;
        StaffRole staffRole = null;
        while (true) {
            if (staffName == null) {
                staffName = ui.inputStaffName();
            }
            if (department == null) {
                department = ui.inputDepartment();
            }
            if (staffRole == null) {
                staffRole = ui.inputStaffRole();
            }
            ui.printStaffCreationSummary(staffName, department, staffRole);
            if (!ui.confirm("Save this staff member?")) {
                int field = ui.inputStaffFieldChoice();
                if (field == 0) {
                    return;
                }
                switch (field) {
                    case 1 -> staffName = ui.inputStaffName();
                    case 2 -> department = ui.inputDepartment();
                    case 3 -> staffRole = ui.inputStaffRole();
                    default -> { }
                }
                continue;
            }
            String staffId = createStaff(staffName, department, staffRole);
            if (staffId == null) {
                ui.printDuplicateNameError();
                return;
            }
            ui.printStaffId(staffId);
            ui.printSuccess();
            return;
        }
    }

    private void editStaffDetailsMenu(Staff staff) {
        String name = staff.getStaffName();
        Department department = staff.getDepartment();
        StaffRole staffRole = staff.getStaffRole();
        boolean changed = false;
        while (true) {
            int field = ui.inputStaffFieldChoice();
            if (field == 0) {
                break;
            }
            switch (field) {
                case 1 -> {
                    String input = ui.inputStaffName();
                    if (!staff.getStaffName().equalsIgnoreCase(input) && duplicateStaffName(input, staff)) {
                        ui.printDuplicateNameError();
                        continue;
                    }
                    name = input;
                }
                case 2 -> department = ui.inputDepartment();
                case 3 -> staffRole = ui.inputStaffRole();
                default -> {
                    toggleOnLeave(staff);
                    continue;
                }
            }
            changed = true;
        }
        if (!changed) {
            return;
        }
        ui.printStaffEditSummary(name, department, staffRole);
        if (!ui.confirm("Apply changes?")) {
            return;
        }
        staff.setStaffName(name);
        staff.setDepartment(department);
        staff.setStaffRole(staffRole);
        staffDAO.saveStaffList(staffList);
        ui.printSuccess();
    }

    private void toggleOnLeave(Staff staff) {
        if (staff.getAvailabilityStatus() == AvailabilityStatus.ON_LEAVE) {
            recomputeStaffAvailability(staff);
            ui.printSuccess();
            return;
        }
        if (getActiveAssignmentOfStaff(staff.getStaffId()) != null) {
            ui.printStaffHasActiveAssignment();
            return;
        }
        staff.setAvailabilityStatus(AvailabilityStatus.ON_LEAVE);
        staffDAO.saveStaffList(staffList);
        ui.printSuccess();
    }

    private void resignStaffMenu(Staff staff) {
        if (!ui.confirm("Resign this staff member?")) {
            return;
        }
        TaskAssignment active = getActiveAssignmentOfStaff(staff.getStaffId());
        if (active != null) {
            if (!ui.confirm("Staff has an active assignment. End it (Cancelled)?")) {
                return;
            }
            endAssignment(active, TaskStatus.CANCELLED);
        }
        staff.setAvailabilityStatus(AvailabilityStatus.RESIGNED);
        staff.setDeleted(true);
        staffDAO.saveStaffList(staffList);
        ui.printSuccess();
    }

    private void viewStaffAssignmentHistory(Staff staff) {
        LinkedListInterface<TaskAssignment> filtered = new LinkedList<>();
        for (int i = 0; i < staff.getTaskAssignments().size(); i++) {
            TaskAssignment assignment = staff.getTaskAssignments().get(i);
            if (!assignment.isDeleted()) {
                filtered.addBack(assignment);
            }
        }
        ui.listAllAssignments(assignmentListToTable(filtered));
        ui.pressEnterToContinue();
    }

    private void viewStaffChangeHistory(Staff staff) {
        ui.listAllChanges(changeListToTable(getChangesByStaff(staff.getStaffId())));
        ui.pressEnterToContinue();
    }

    /**
     * Queue = the staff member's active (non-terminal) assignments ordered by
     * dateTimeAssigned, i.e. the order they were assigned.
     *
     * @param staff staff member to inspect
     * @return queue list (may be empty)
     */
    private LinkedListInterface<TaskAssignment> getStaffTaskQueue(Staff staff) {
        LinkedListInterface<TaskAssignment> queue = new LinkedList<>();
        if (staff == null || staff.getStaffId() == null) {
            return queue;
        }
        refreshTaskAssignments();
        for (int i = 0; i < taskAssignmentList.size(); i++) {
            TaskAssignment assignment = taskAssignmentList.get(i);
            if (assignment.isDeleted() || !assignment.isActive()
                    || !staff.getStaffId().equals(assignment.getAssignedStaffId())) {
                continue;
            }
            queue.addSorted(assignment);
        }
        return queue;
    }

    /**
     * Shows the first task in the staff member's queue and asks for
     * confirmation before starting it.
     *
     * @param staff staff member to start a task for
     */
    private void startFirstTaskMenu(Staff staff) {
        LinkedListInterface<TaskAssignment> queue = getStaffTaskQueue(staff);
        if (queue.isEmpty()) {
            ui.printNoTasksInQueue();
            return;
        }
        TaskAssignment first = queue.get(0);
        if (first.getStatus() == TaskStatus.IN_PROGRESS) {
            ui.printTaskAlreadyStarted();
            return;
        }
        Task task = first.getAssignedTaskId() == null ? null : getTaskById(first.getAssignedTaskId());
        if (task == null) {
            ui.printNotFound();
            return;
        }
        ui.printTaskDetails(task, first);
        if (!ui.confirm("Start this task?")) {
            return;
        }
        switch (startFirstTask(staff)) {
            case "STARTED" -> ui.printTaskStarted(task.getTaskName());
            case "ALREADY_STARTED" -> ui.printTaskAlreadyStarted();
            default -> ui.printWarning("Failed to start the task.");
        }
    }

    /**
     * Starts the first task in the staff member's queue: the earliest
     * assigned active assignment is moved to IN_PROGRESS. Only the
     * assignment status changes; the task-level status remains under the
     * supervisor's control.
     *
     * @param staff staff member to start a task for
     * @return outcome code
     */
    public String startFirstTask(Staff staff) {
        if (staff == null || staff.getStaffId() == null) {
            return "NO_STAFF";
        }
        LinkedListInterface<TaskAssignment> queue = getStaffTaskQueue(staff);
        if (queue.isEmpty()) {
            return "EMPTY_QUEUE";
        }
        TaskAssignment first = queue.get(0);
        if (first.getStatus() == TaskStatus.IN_PROGRESS) {
            return "ALREADY_STARTED";
        }
        refreshTaskAssignments();
        TaskAssignment target = null;
        for (int i = 0; i < taskAssignmentList.size(); i++) {
            TaskAssignment candidate = taskAssignmentList.get(i);
            if (first.getTaskAssignmentId().equals(candidate.getTaskAssignmentId())) {
                target = candidate;
                break;
            }
        }
        if (target == null) {
            return "UPDATE_FAILED";
        }
        target.setStatus(TaskStatus.IN_PROGRESS);
        taskAssignmentDAO.saveTaskAssignmentList(taskAssignmentList);
        appendAssignmentChange(target, "In Progress", LocalDateTime.now());
        return "STARTED";
    }

    // =============== task queries ===============

    private LinkedListInterface<Task> getTasksByStatus(TaskStatus status) {
        LinkedListInterface<Task> filtered = new LinkedList<>();
        for (int i = 0; i < taskList.size(); i++) {
            Task task = taskList.get(i);
            if (!task.isDeleted() && task.getTaskStatus() == status) {
                filtered.addBack(task);
            }
        }
        return filtered;
    }

    private LinkedListInterface<Task> getTasksByPriority(TaskPriority priority) {
        LinkedListInterface<Task> filtered = new LinkedList<>();
        for (int i = 0; i < taskList.size(); i++) {
            Task task = taskList.get(i);
            if (!task.isDeleted() && task.getTaskPriority() == priority) {
                filtered.addBack(task);
            }
        }
        return filtered;
    }

    private LinkedListInterface<Task> searchTasks(LinkedListInterface<Task> source, String searchTerm) {
        LinkedListInterface<Task> filtered = new LinkedList<>();
        for (int i = 0; i < source.size(); i++) {
            Task task = source.get(i);
            if (task.getTaskName() != null
                    && task.getTaskName().toLowerCase().contains(searchTerm.toLowerCase())) {
                filtered.addBack(task);
            }
        }
        return filtered;
    }

    private LinkedListInterface<Task> getAllTasks() {
        LinkedListInterface<Task> all = new LinkedList<>();
        for (int i = 0; i < taskList.size(); i++) {
            Task task = taskList.get(i);
            if (!task.isDeleted()) {
                all.addBack(task);
            }
        }
        return all;
    }

    // =============== staff queries ===============

    private LinkedListInterface<Staff> getStaffsByDepartment(Department department) {
        LinkedListInterface<Staff> filtered = new LinkedList<>();
        for (int i = 0; i < staffList.size(); i++) {
            Staff staff = staffList.get(i);
            if (!staff.isDeleted()
                    && staff.getDepartment() != null
                    && staff.getDepartment() == department) {
                filtered.addBack(staff);
            }
        }
        return filtered;
    }

    private LinkedListInterface<Staff> getStaffsByRole(StaffRole role) {
        LinkedListInterface<Staff> filtered = new LinkedList<>();
        for (int i = 0; i < staffList.size(); i++) {
            Staff staff = staffList.get(i);
            if (!staff.isDeleted() && staff.getStaffRole() == role) {
                filtered.addBack(staff);
            }
        }
        return filtered;
    }

    private LinkedListInterface<Staff> searchStaffs(LinkedListInterface<Staff> source, String searchTerm) {
        LinkedListInterface<Staff> filtered = new LinkedList<>();
        for (int i = 0; i < source.size(); i++) {
            Staff staff = source.get(i);
            boolean matches = (staff.getStaffName() != null
                    && staff.getStaffName().toLowerCase().contains(searchTerm.toLowerCase()))
                    || (staff.getStaffId() != null
                            && staff.getStaffId().toLowerCase().contains(searchTerm.toLowerCase()));
            if (matches) {
                filtered.addBack(staff);
            }
        }
        return filtered;
    }

    private LinkedListInterface<Staff> getAllStaffs() {
        LinkedListInterface<Staff> all = new LinkedList<>();
        for (int i = 0; i < staffList.size(); i++) {
            Staff staff = staffList.get(i);
            if (!staff.isDeleted()) {
                all.addBack(staff);
            }
        }
        return all;
    }

    // =============== creation ===============

    /**
     *
     * @param taskName       display name of the task
     * @param taskType       task category (CHECKOUT_CLEAN / MAINTENANCE / ...)
     * @param taskPriority   urgency level
     * @param startDateTime  scheduled start
     * @param roomId         optional room the task is for (may be null)
     * @return new task ID, or null on failure
     */
    public String createTask(String taskName, TaskType taskType, TaskPriority taskPriority,
            LocalDateTime startDateTime, String roomId) {
        if (taskName == null || taskType == null || taskPriority == null || startDateTime == null) {
            return null;
        }
        if (duplicateTaskName(taskName, null)) {
            return null;
        }
        String taskId = generateTaskId();
        Task task = new Task(taskId, taskName, taskType, TaskStatus.PENDING, taskPriority, startDateTime, null, roomId);
        task.getStatusHistory().addFront(new TaskStatusChange(TaskStatus.PENDING, null, LocalDateTime.now()));
        taskList.addSorted(task);
        taskDAO.saveTaskList(taskList);
        appendTaskStatusChange(task, TaskStatus.PENDING.name(), LocalDateTime.now());
        return taskId;
    }

    /**
     * 
     * @param staffName  full name of the staff member
     * @param department department the staff member belongs to
     * @param staffRole  role within the department (e.g. Cleaner)
     * @return new staff ID, or null on failure
     */
    public String createStaff(String staffName, Department department, StaffRole staffRole) {
        if (staffName == null || department == null || staffRole == null) {
            return null;
        }
        if (duplicateStaffName(staffName, null)) {
            return null;
        }
        String staffId = generateStaffId();
        Staff staff = new Staff(staffId, staffName, department, staffRole, AvailabilityStatus.AVAILABLE);
        staffList.addSorted(staff);
        staffDAO.saveStaffList(staffList);
        return staffId;
    }

    private boolean duplicateTaskName(String name, Task self) {
        for (int i = 0; i < taskList.size(); i++) {
            Task existing = taskList.get(i);
            if (existing.isDeleted() || existing == self) {
                continue;
            }
            if (existing.getTaskName() != null && existing.getTaskName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private boolean duplicateStaffName(String name, Staff self) {
        for (int i = 0; i < staffList.size(); i++) {
            Staff existing = staffList.get(i);
            if (existing.isDeleted() || existing == self) {
                continue;
            }
            if (existing.getStaffName() != null && existing.getStaffName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    // =============== status transitions ===============

    /**
     *
     * @param current the task's current status
     * @return legal next statuses
     */
    public TaskStatus[] getAllowedTransitions(TaskStatus current) {
        if (current == null) {
            return new TaskStatus[] { TaskStatus.PENDING, TaskStatus.CANCELLED };
        }
        return switch (current) {
            case PENDING -> new TaskStatus[] { TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED };
            case IN_PROGRESS -> new TaskStatus[] { TaskStatus.CANCELLED, TaskStatus.COMPLETED };
            case COMPLETED -> new TaskStatus[] {};
            case CANCELLED -> new TaskStatus[] { TaskStatus.IN_PROGRESS };
        };
    }

    /**
     *
     * @param task     task to update
     * @param newStatus target status
     * @param reason   cancellation reason (only meaningful for CANCELLED)
     * @return outcome code
     */
    public String updateTaskStatus(Task task, TaskStatus newStatus, String reason) {
        if (task == null || newStatus == null) {
            return "TRANSITION_DENIED";
        }
        TaskStatus current = task.getTaskStatus();
        boolean allowed = false;
        for (TaskStatus option : getAllowedTransitions(current)) {
            if (option == newStatus) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            return "TRANSITION_DENIED";
        }
        if (newStatus == TaskStatus.COMPLETED) {
            refreshTaskAssignments();
            for (int i = 0; i < taskAssignmentList.size(); i++) {
                TaskAssignment assignment = taskAssignmentList.get(i);
                if (assignment.isDeleted() || !task.getTaskId().equals(assignment.getAssignedTaskId())
                        || !assignment.isActive()) {
                    continue;
                }
                return "WORKERS_NOT_DONE";
            }
        }
        if (current != null) {
            task.getStatusHistory().addFront(new TaskStatusChange(current, reason, LocalDateTime.now()));
        }
        task.setTaskStatus(newStatus);
        try {
            if (newStatus == TaskStatus.CANCELLED) {
                refreshTaskAssignments();
                for (int i = 0; i < taskAssignmentList.size(); i++) {
                    TaskAssignment assignment = taskAssignmentList.get(i);
                    if (assignment.isDeleted() || !task.getTaskId().equals(assignment.getAssignedTaskId())
                            || !assignment.isActive()) {
                        continue;
                    }
                    endAssignment(assignment, TaskStatus.CANCELLED);
                }
            } else if (newStatus == TaskStatus.COMPLETED) {
                task.setEndDateTime(LocalDateTime.now());
                if (task.getTaskType() == TaskType.INSPECTION) {
                    setRoomStatus(task.getRoomId(), RoomStatus.AVAILABLE);
                }
            }
        } catch (Exception e) {
            task.setTaskStatus(current);
            if (current != null) {
                task.getStatusHistory().removeFront();
            }
            ConsoleUtil.printError("Status update failed during room/worker update: " + e.getMessage());
            return "UPDATE_FAILED";
        }
        appendTaskStatusChange(task, newStatus.name(), LocalDateTime.now());
        taskDAO.saveTaskList(taskList);
        return "UPDATED";
    }

    /**
     *
     * @param current the assignment's current status
     * @return legal next statuses
     */
    public TaskStatus[] getAllowedAssignmentTransitions(TaskStatus current) {
        if (current == null) {
            return new TaskStatus[] { TaskStatus.PENDING, TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED,
                    TaskStatus.CANCELLED };
        }
        return switch (current) {
            case PENDING -> new TaskStatus[] { TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED, TaskStatus.CANCELLED };
            case IN_PROGRESS -> new TaskStatus[] { TaskStatus.COMPLETED, TaskStatus.CANCELLED };
            case COMPLETED -> new TaskStatus[] {};
            case CANCELLED -> new TaskStatus[] {};
        };
    }

    /**
     *
     * @param assignment assignment to update
     * @param newStatus  target status
     * @param reason     cancellation reason (only meaningful for CANCELLED)
     * @return outcome code
     */
    public String updateAssignmentStatus(TaskAssignment assignment, TaskStatus newStatus, String reason) {
        if (assignment == null || newStatus == null) {
            return "TRANSITION_DENIED";
        }
        boolean allowed = false;
        for (TaskStatus option : getAllowedAssignmentTransitions(assignment.getStatus())) {
            if (option == newStatus) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            return "TRANSITION_DENIED";
        }
        refreshTaskAssignments();
        TaskAssignment target = null;
        for (int i = 0; i < taskAssignmentList.size(); i++) {
            TaskAssignment candidate = taskAssignmentList.get(i);
            if (assignment.getTaskAssignmentId().equals(candidate.getTaskAssignmentId())) {
                target = candidate;
                break;
            }
        }
        if (target == null) {
            return "UPDATE_FAILED";
        }
        if (newStatus == TaskStatus.COMPLETED || newStatus == TaskStatus.CANCELLED) {
            endAssignment(target, newStatus);
            if (newStatus == TaskStatus.COMPLETED) {
                Task task = target.getAssignedTaskId() == null ? null : getTaskById(target.getAssignedTaskId());
                if (task != null && task.getTaskStatus() != TaskStatus.COMPLETED && allAssignmentsTerminal(task)) {
                    TaskStatus current = task.getTaskStatus();
                    if (current != null) {
                        task.getStatusHistory().addFront(new TaskStatusChange(current, null, LocalDateTime.now()));
                    }
                    task.setTaskStatus(TaskStatus.COMPLETED);
                    task.setEndDateTime(LocalDateTime.now());
                    if (task.getTaskType() == TaskType.INSPECTION) {
                        setRoomStatus(task.getRoomId(), RoomStatus.AVAILABLE);
                    }
                    appendTaskStatusChange(task, TaskStatus.COMPLETED.name(), LocalDateTime.now());
                    taskDAO.saveTaskList(taskList);
                    return "TASK_COMPLETED";
                }
            }
        }
        return "UPDATED";
    }

    private boolean allAssignmentsTerminal(Task task) {
        refreshTaskAssignments();
        for (int i = 0; i < taskAssignmentList.size(); i++) {
            TaskAssignment assignment = taskAssignmentList.get(i);
            if (assignment.isDeleted() || !task.getTaskId().equals(assignment.getAssignedTaskId())) {
                continue;
            }
            if (assignment.isActive()) {
                return false;
            }
        }
        return true;
    }

    /**
     *
     * @param task task to roll back
     * @return outcome code
     */
    public String rollbackTaskStatus(Task task) {
        if (task == null) {
            return "NO_PREVIOUS";
        }
        LinkedListInterface<TaskStatusChange> stack = task.getStatusHistory();
        if (stack.isEmpty()) {
            return "NO_PREVIOUS";
        }
        TaskStatusChange entry = stack.removeFront();
        TaskStatus previous = entry.getTaskStatus();
        TaskStatus current = task.getTaskStatus();
        task.setTaskStatus(previous);
        try {
            if (current == TaskStatus.COMPLETED) {
                task.setEndDateTime(null);
                setRoomStatus(task.getRoomId(), RoomStatus.CLEANING);
            }
        } catch (Exception e) {
            task.setTaskStatus(current);
            stack.addFront(entry);
            ConsoleUtil.printError("Rollback failed during room status update: " + e.getMessage());
            return "UPDATE_FAILED";
        }
        appendTaskStatusChange(task, previous.name(), LocalDateTime.now());
        taskDAO.saveTaskList(taskList);
        return "ROLLED_BACK";
    }

    // =============== assignment / rotation ===============

    /**
     *
     * @param taskType task category determining the required role
     * @return earliest-available eligible staff, or null
     */
    public Staff findEarliestAvailableStaff(TaskType taskType) {
        LinkedListInterface<Staff> eligible = getEligibleStaffByRole(taskType);
        Staff best = null;
        LocalDateTime bestEnd = null;
        for (int i = 0; i < eligible.size(); i++) {
            Staff staff = eligible.get(i);
            LocalDateTime end = latestEndTime(staff.getStaffId());
            if (best == null) {
                best = staff;
                bestEnd = end;
            } else if (end == null) {
                if (bestEnd != null || staff.getStaffId().compareToIgnoreCase(best.getStaffId()) < 0) {
                    best = staff;
                    bestEnd = end;
                }
            } else if (bestEnd == null || end.isBefore(bestEnd)
                    || (end.equals(bestEnd) && staff.getStaffId().compareToIgnoreCase(best.getStaffId()) < 0)) {
                best = staff;
                bestEnd = end;
            }
        }
        return best;
    }

    /**
     *
     * @param taskType task category determining the required role
     * @return eligible staff list (may be empty)
     */
    public LinkedListInterface<Staff> getEligibleStaffByRole(TaskType taskType) {
        LinkedListInterface<Staff> eligible = new LinkedList<>();
        StaffRole requiredRole = switch (taskType) {
            case CHECKOUT_CLEAN, ROOM_SERVICE, MAINTENANCE -> StaffRole.CLEANER;
            case INSPECTION -> StaffRole.SUPERVISOR;
            default -> StaffRole.UNKNOWN;
        };
        for (int i = 0; i < staffList.size(); i++) {
            Staff staff = staffList.get(i);
            if (staff.isDeleted() || staff.getAvailabilityStatus() != AvailabilityStatus.AVAILABLE) {
                continue;
            }
            if (staff.getDepartment() != Department.HOUSEKEEPING) {
                continue;
            }
            if (staff.getStaffRole() == requiredRole) {
                eligible.addBack(staff);
            }
        }
        return eligible;
    }

    /**
     *
     * @param task  task to assign
     * @param staff staff member to assign
     * @return assignment ID or failure code
     */
    public String createAssignment(Task task, Staff staff) {
        if (task == null) {
            return "NO_TASK";
        }
        if (staff == null) {
            return "NO_STAFF";
        }
        if (task.getTaskStatus() == TaskStatus.COMPLETED || task.getTaskStatus() == TaskStatus.CANCELLED) {
            return "TASK_CLOSED";
        }
        if (staff.getAvailabilityStatus() != AvailabilityStatus.AVAILABLE) {
            return "STAFF_UNAVAILABLE";
        }
        refreshTaskAssignments();
        if (getActiveAssignment(task.getTaskId()) != null) {
            return "TASK_ALREADY_ASSIGNED";
        }
        String assignmentId = generateAssignmentId();
        TaskAssignment assignment = new TaskAssignment(assignmentId, TaskStatus.PENDING, LocalDateTime.now(),
                staff.getStaffId(), task.getTaskId());
        taskAssignmentList.addSorted(assignment);
        taskAssignmentDAO.saveTaskAssignmentList(taskAssignmentList);
        staff.addTaskAssignment(assignment);
        staff.setAvailabilityStatus(AvailabilityStatus.BUSY);
        staffDAO.saveStaffList(staffList);
        task.addTaskAssignment(assignment);
        taskDAO.saveTaskList(taskList);
        appendAssignmentChange(assignment, "Assigned", LocalDateTime.now());
        return assignmentId;
    }

    /**
     *
     * @param assignment assignment to close
     * @param endStatus  terminal status (COMPLETED or CANCELLED)
     */
    public void endAssignment(TaskAssignment assignment, TaskStatus endStatus) {
        if (assignment == null) {
            return;
        }
        assignment.setStatus(endStatus);
        assignment.setDateTimeEnded(LocalDateTime.now());
        taskAssignmentDAO.saveTaskAssignmentList(taskAssignmentList);
        Staff staff = assignment.getAssignedStaffId() == null ? null : getStaffById(assignment.getAssignedStaffId());
        if (staff != null) {
            recomputeStaffAvailability(staff);
        }
        appendAssignmentChange(assignment, endStatus.name(), LocalDateTime.now());
    }

    /**
     *
     * @param staff staff member to recompute
     */
    public void recomputeStaffAvailability(Staff staff) {
        if (staff == null) {
            return;
        }
        if (staff.getAvailabilityStatus() == AvailabilityStatus.ON_LEAVE
                || staff.getAvailabilityStatus() == AvailabilityStatus.RESIGNED) {
            return;
        }
        refreshTaskAssignments();
        for (int i = 0; i < taskAssignmentList.size(); i++) {
            TaskAssignment assignment = taskAssignmentList.get(i);
            if (assignment.isDeleted() || !staff.getStaffId().equals(assignment.getAssignedStaffId())
                    || !assignment.isActive()) {
                continue;
            }
            staff.setAvailabilityStatus(AvailabilityStatus.BUSY);
            staffDAO.saveStaffList(staffList);
            return;
        }
        staff.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        staffDAO.saveStaffList(staffList);
    }

    private LocalDateTime latestEndTime(String staffId) {
        LocalDateTime latest = null;
        refreshTaskAssignments();
        for (int i = 0; i < taskAssignmentList.size(); i++) {
            TaskAssignment assignment = taskAssignmentList.get(i);
            if (assignment.isDeleted() || !staffId.equals(assignment.getAssignedStaffId())
                    || assignment.getDateTimeEnded() == null || assignment.isActive()) {
                continue;
            }
            if (latest == null || assignment.getDateTimeEnded().isAfter(latest)) {
                latest = assignment.getDateTimeEnded();
            }
        }
        return latest;
    }

    // =============== room lifecycle ===============

    private void setRoomStatus(String roomId, RoomStatus status) {
        if (roomId == null || roomId.isBlank()) {
            return;
        }
        LinkedListInterface<Room> rooms = new LinkedList<>();
        roomDAO.loadFromFile(rooms);
        for (int i = 0; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            if (room.getRoomId() != null && room.getRoomId().equalsIgnoreCase(roomId)) {
                room.setRoomStatus(status);
                roomDAO.saveToFile(rooms);
                return;
            }
        }
    }

    // =============== public API for other modules ===============

    /**
     *
     * @param roomId room requiring service
     * @return new task ID, or null on failure
     */
    public String createRoomServiceTask(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            return null;
        }
        String taskId = createTask("Room Service " + roomId, TaskType.ROOM_SERVICE, TaskPriority.HIGH,
                LocalDateTime.now(), roomId);
        if (taskId == null) {
            return null;
        }
        Staff staff = findEarliestAvailableStaff(TaskType.ROOM_SERVICE);
        if (staff != null) {
            createAssignment(getTaskById(taskId), staff);
        }
        return taskId;
    }

    /**
     *
     * @param roomId room left by a guest
     * @return the cleaning task ID, or null on failure
     */
    public String createCheckoutTask(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            return null;
        }
        String taskId = createTask("Clean " + roomId, TaskType.CHECKOUT_CLEAN, TaskPriority.MEDIUM,
                LocalDateTime.now(), roomId);
        if (taskId == null) {
            return null;
        }
        setRoomStatus(roomId, RoomStatus.CLEANING);
        // assign earliest available cleaner to cleaning task
        Staff cleaner = findEarliestAvailableStaff(TaskType.CHECKOUT_CLEAN);
        if (cleaner != null) {
            createAssignment(getTaskById(taskId), cleaner);
        }
        // assign earliest available supervisor to inspection task
        Staff supervisor = findEarliestAvailableStaff(TaskType.INSPECTION);
        if (supervisor != null) {
            createAssignment(getTaskById(taskId), supervisor);
        }
        return taskId;
    }

    /**
     *
     * @param taskId task ID to look up
     * @return the task, or null when not found
     */
    public Task getTaskById(String taskId) {
        if (taskId == null) {
            return null;
        }
        for (int i = 0; i < taskList.size(); i++) {
            Task task = taskList.get(i);
            if (taskId.equals(task.getTaskId())) {
                return task;
            }
        }
        return null;
    }

    /**
     *
     * @param staffId staff ID to look up
     * @return the staff member, or null when not found
     */
    public Staff getStaffById(String staffId) {
        if (staffId == null) {
            return null;
        }
        for (int i = 0; i < staffList.size(); i++) {
            Staff staff = staffList.get(i);
            if (staffId.equals(staff.getStaffId())) {
                return staff;
            }
        }
        return null;
    }

    private TaskAssignment getActiveAssignment(String taskId) {
        refreshTaskAssignments();
        for (int i = 0; i < taskAssignmentList.size(); i++) {
            TaskAssignment assignment = taskAssignmentList.get(i);
            if (!assignment.isDeleted() && taskId.equals(assignment.getAssignedTaskId()) && assignment.isActive()) {
                return assignment;
            }
        }
        return null;
    }

    private TaskAssignment getActiveAssignmentOfStaff(String staffId) {
        refreshTaskAssignments();
        for (int i = 0; i < taskAssignmentList.size(); i++) {
            TaskAssignment assignment = taskAssignmentList.get(i);
            if (!assignment.isDeleted() && staffId.equals(assignment.getAssignedStaffId()) && assignment.isActive()) {
                return assignment;
            }
        }
        return null;
    }

    // =============== ID generation ===============

    private String generateTaskId() {
        int max = 0;
        for (int i = 0; i < taskList.size(); i++) {
            int number = parseIdSuffix(taskList.get(i).getTaskId());
            if (number > max) {
                max = number;
            }
        }
        return String.format("TSK%012d", max + 1);
    }

    private String generateStaffId() {
        int max = 0;
        for (int i = 0; i < staffList.size(); i++) {
            int number = parseIdSuffix(staffList.get(i).getStaffId());
            if (number > max) {
                max = number;
            }
        }
        return String.format("STF%012d", max + 1);
    }

    private String generateAssignmentId() {
        int max = 0;
        for (int i = 0; i < taskAssignmentList.size(); i++) {
            int number = parseIdSuffix(taskAssignmentList.get(i).getTaskAssignmentId());
            if (number > max) {
                max = number;
            }
        }
        return String.format("ASG%012d", max + 1);
    }

    private String generateChangeId() {
        int max = 0;
        for (int i = 0; i < taskAssignmentChangeList.size(); i++) {
            int number = parseIdSuffix(taskAssignmentChangeList.get(i).getChangeId());
            if (number > max) {
                max = number;
            }
        }
        return String.format("CHG%012d", max + 1);
    }

    private int parseIdSuffix(String id) {
        if (id == null || id.length() < 4) {
            return 0;
        }
        try {
            return Integer.parseInt(id.substring(3));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // =============== pagination ===============

    private LinkedListInterface<Task> pageOf(LinkedListInterface<Task> source, int page) {
        LinkedListInterface<Task> pageList = new LinkedList<>();
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, source.size());
        for (int i = start; i < end; i++) {
            pageList.addBack(source.get(i));
        }
        return pageList;
    }

    private LinkedListInterface<Staff> pageOfStaff(LinkedListInterface<Staff> source, int page) {
        LinkedListInterface<Staff> pageList = new LinkedList<>();
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, source.size());
        for (int i = start; i < end; i++) {
            pageList.addBack(source.get(i));
        }
        return pageList;
    }

    // =============== audit trail ===============

    private TaskAssignmentChange appendTaskStatusChange(Task task, String status, LocalDateTime dateTime) {
        if (task == null || status == null) {
            return null;
        }
        TaskAssignment active = getActiveAssignment(task.getTaskId());
        return insertChange(new TaskAssignmentChange(generateChangeId(),
                active == null ? null : active.getTaskAssignmentId(), status, dateTime,
                active == null ? null : active.getAssignedStaffId(), task.getTaskId()));
    }

    private TaskAssignmentChange appendAssignmentChange(TaskAssignment assignment, String status,
            LocalDateTime dateTime) {
        if (assignment == null || status == null) {
            return null;
        }
        TaskAssignmentChange change = insertChange(new TaskAssignmentChange(generateChangeId(),
                assignment.getTaskAssignmentId(), status, dateTime,
                assignment.getAssignedStaffId(), assignment.getAssignedTaskId()));
        assignment.addChange(change);
        taskAssignmentDAO.saveTaskAssignmentList(taskAssignmentList);
        return change;
    }

    private TaskAssignmentChange insertChange(TaskAssignmentChange change) {
        refreshTaskAssignmentChanges();
        taskAssignmentChangeList.addSorted(change);
        taskAssignmentChangeDAO.saveTaskAssignmentChangeList(taskAssignmentChangeList);
        return change;
    }

    private LinkedListInterface<TaskAssignmentChange> getChangesByTask(String taskId) {
        LinkedListInterface<TaskAssignmentChange> filtered = new LinkedList<>();
        for (int i = 0; i < taskAssignmentChangeList.size(); i++) {
            TaskAssignmentChange change = taskAssignmentChangeList.get(i);
            if (change.getTaskId() != null && change.getTaskId().equals(taskId)) {
                filtered.addBack(change);
            }
        }
        return filtered;
    }

    private LinkedListInterface<TaskAssignmentChange> getChangesByStaff(String staffId) {
        LinkedListInterface<TaskAssignmentChange> filtered = new LinkedList<>();
        for (int i = 0; i < taskAssignmentChangeList.size(); i++) {
            TaskAssignmentChange change = taskAssignmentChangeList.get(i);
            if (change.getStaffId() != null && change.getStaffId().equals(staffId)) {
                filtered.addBack(change);
            }
        }
        return filtered;
    }

    private LinkedListInterface<TaskAssignmentChange> getChangesByAssignment(String taskAssignmentId) {
        LinkedListInterface<TaskAssignmentChange> filtered = new LinkedList<>();
        for (int i = 0; i < taskAssignmentChangeList.size(); i++) {
            TaskAssignmentChange change = taskAssignmentChangeList.get(i);
            if (change.getTaskAssignmentId() != null && change.getTaskAssignmentId().equals(taskAssignmentId)) {
                filtered.addBack(change);
            }
        }
        return filtered;
    }

    // =============== table rendering ===============

    private String[][] assignmentListToTable(LinkedListInterface<TaskAssignment> assignmentList) {
        String[][] data = new String[assignmentList.size() + 1][6];
        data[0] = new String[] { "Assignment ID", "Staff", "Task", "Status", "Started At", "Ended At" };
        for (int i = 0; i < assignmentList.size(); i++) {
            TaskAssignment assignment = assignmentList.get(i);
            Staff staff = assignment.getAssignedStaffId() == null ? null
                    : getStaffById(assignment.getAssignedStaffId());
            Task task = assignment.getAssignedTaskId() == null ? null : getTaskById(assignment.getAssignedTaskId());
            data[i + 1] = new String[] {
                    assignment.getTaskAssignmentId(),
                    staff == null ? "-" : staff.getStaffId() + " (" + staff.getStaffName() + ")",
                    task == null ? "-" : task.getTaskId() + " (" + task.getTaskName() + ")",
                    assignment.getStatus() == null ? "-" : assignment.getStatus().name(),
                    DateTimeUtil.readable(assignment.getDateTimeAssigned()),
                    DateTimeUtil.readable(assignment.getDateTimeEnded())
            };
        }
        return data;
    }

    private String[][] changeListToTable(LinkedListInterface<TaskAssignmentChange> changeList) {
        String[][] data = new String[changeList.size() + 1][6];
        data[0] = new String[] { "Change ID", "Task", "Assignment", "Status", "Staff", "Changed At" };
        for (int i = 0; i < changeList.size(); i++) {
            TaskAssignmentChange change = changeList.get(i);
            Staff staff = change.getStaffId() == null ? null : getStaffById(change.getStaffId());
            Task task = change.getTaskId() == null ? null : getTaskById(change.getTaskId());
            data[i + 1] = new String[] {
                    change.getChangeId(),
                    task == null ? "-" : task.getTaskId() + " (" + task.getTaskName() + ")",
                    change.getTaskAssignmentId() == null ? "-" : change.getTaskAssignmentId(),
                    change.getStatus() == null ? "-" : change.getStatus(),
                    staff == null ? "-" : staff.getStaffId() + " (" + staff.getStaffName() + ")",
                    DateTimeUtil.readable(change.getChangedAt())
            };
        }
        return data;
    }
}