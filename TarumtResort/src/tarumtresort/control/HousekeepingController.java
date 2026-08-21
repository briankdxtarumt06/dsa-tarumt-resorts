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

// Author: Brian Kam Ding Xian
public class HousekeepingController {

    private static final int PAGE_SIZE = 10;

    // Controller declaration

    // UI declaration
    private HousekeepingUI ui;

    // ADT declaration
    private final LinkedListInterface<Task> taskList = new LinkedList<>();
    private final LinkedListInterface<Staff> staffList = new LinkedList<>();
    private final LinkedListInterface<TaskAssignment> taskAssignmentList = new LinkedList<>();
    private final LinkedListInterface<TaskAssignmentChange> taskAssignmentChangeList = new LinkedList<>();

    // DAO declaration
    private final TaskDAO taskDAO = new TaskDAO();
    private final StaffDAO staffDAO = new StaffDAO();
    private final TaskAssignmentDAO taskAssignmentDAO = new TaskAssignmentDAO();
    private final TaskAssignmentChangeDAO taskAssignmentChangeDAO = new TaskAssignmentChangeDAO();
    private final RoomDAO roomDAO = new RoomDAO();

    public HousekeepingController() {
        copyAll(taskList, taskDAO.retrieveTaskList());
        copyAll(staffList, staffDAO.retrieveStaffList());
        copyAll(taskAssignmentList, taskAssignmentDAO.retrieveTaskAssignmentList());
        copyAll(taskAssignmentChangeList, taskAssignmentChangeDAO.retrieveTaskAssignmentChangeList());
    }

    public HousekeepingController(HousekeepingUI ui) {
        this();
        this.ui = ui;
    }

    // copy source data records to list
    private static <T extends Comparable<T>> void copyAll(LinkedListInterface<T> target,
            LinkedListInterface<T> source) {
        for (int i = 0; i < source.size(); i++) {
            target.addBack(source.get(i));
        }
    }

    private void refreshTaskAssignments() {
        taskAssignmentList.clear();
        copyAll(taskAssignmentList, taskAssignmentDAO.retrieveTaskAssignmentList());
    }

    private void refreshTaskAssignmentChanges() {
        taskAssignmentChangeList.clear();
        copyAll(taskAssignmentChangeList, taskAssignmentChangeDAO.retrieveTaskAssignmentChangeList());
    }

    // module entry driver function
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

    // task management

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

            LinkedListInterface<Task> pageList = pageOfTask(display, page);
            int choice = ui.printTaskListMenu(pageList, page, pageCount, statusFilter, priorityFilter, searchTerm);

            if (choice == 0) {
                break;
            }

            int action = 1;
            int optView = action++;
            int optAdd = action++;
            int optUpdateStatus = action++;
            int optRollback = action++;
            int optAddFilter = action++;
            int optClearFilter = hasFilter ? action++ : -1;
            int optSearch = !hasSearch ? action++ : -1;
            int optClearSearch = hasSearch ? action++ : -1;
            int optNextPage = page < pageCount - 1 ? action++ : -1;
            int optPrevPage = page > 0 ? action++ : -1;

            if (choice == optView) {
                viewTask(pageList);
            } else if (choice == optAdd) {
                addTaskMenu();
            } else if (choice == optUpdateStatus) {
                int index = ui.inputListIndex("task", pageList.size());
                if (index > 0) {
                    quickUpdateTaskStatus(pageList.get(index - 1));
                }
            } else if (choice == optRollback) {
                int index = ui.inputListIndex("task", pageList.size());
                if (index > 0) {
                    quickRollbackTaskStatus(pageList.get(index - 1));
                }
            } else if (choice == optAddFilter) {
                int dimension = ui.inputTaskFilterDimension();
                if (dimension == 1) {
                    statusFilter = ui.inputTaskStatusFilter();
                    priorityFilter = null;
                } else if (dimension == 2) {
                    priorityFilter = ui.inputTaskPriorityFilter();
                    statusFilter = null;
                }
                page = 0;
            } else if (choice == optClearFilter) {
                statusFilter = null;
                priorityFilter = null;
                page = 0;
            } else if (choice == optSearch) {
                searchTerm = ui.inputSearchTerm();
                page = 0;
            } else if (choice == optClearSearch) {
                searchTerm = null;
                page = 0;
            } else if (choice == optNextPage) {
                page++;
            } else if (choice == optPrevPage) {
                page--;
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
        Staff staff = pickStaff(task, mode);
        if (staff == null) {
            return;
        }
        String result = assignTaskToStaff(task, staff);
        switch (result) {
            case "TASK_CLOSED" -> ui.printTaskClosed();
            case "TASK_ALREADY_ASSIGNED" -> ui.printTaskAlreadyAssigned();
            case "STAFF_UNAVAILABLE" -> ui.printStaffUnavailable();
            case "NO_TASK", "NO_STAFF" -> ui.printNotFound();
            default -> ui.printSuccess();
        }
    }

    // auto mode picks the earliest available staff, manual mode asks the user
    private Staff pickStaff(Task task, int mode) {
        Staff staff;
        if (mode == 1) {
            staff = findEarliestAvailableStaff(task.getTaskType());
            if (staff == null) {
                ui.printNoStaffFreeForTask();
                return null;
            }
        } else {
            LinkedListInterface<Staff> eligible = getEligibleStaffByRole(task.getTaskType());
            if (eligible.isEmpty()) {
                ui.printNoStaffFreeForTask();
                return null;
            }
            int choice = ui.selectStaff(eligible);
            if (choice == 0) {
                return null;
            }
            staff = eligible.get(choice - 1);
        }
        ui.printStaffDetails(staff, getActiveAssignmentOfStaff(staff.getStaffId()));
        if (!ui.confirm("Assign " + staff.getStaffName() + " to this task?")) {
            return null;
        }
        return staff;
    }

    private void viewStaffAssignedMenu(Task task) {
        LinkedListInterface<TaskAssignment> filtered = nonDeletedAssignments(task.getTaskAssignments());
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

    // staff management

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
            int optView = action++;
            int optAdd = action++;
            int optAddFilter = action++;
            int optClearFilter = hasFilter ? action++ : -1;
            int optSearch = !hasSearch ? action++ : -1;
            int optClearSearch = hasSearch ? action++ : -1;
            int optNextPage = page < pageCount - 1 ? action++ : -1;
            int optPrevPage = page > 0 ? action++ : -1;

            if (choice == optView) {
                viewStaff(pageList);
            } else if (choice == optAdd) {
                addStaffMenu();
            } else if (choice == optAddFilter) {
                int dimension = ui.inputStaffFilterDimension();
                if (dimension == 1) {
                    departmentFilter = ui.inputDepartment();
                    roleFilter = null;
                } else if (dimension == 2) {
                    roleFilter = ui.inputStaffRole();
                    departmentFilter = null;
                }
                page = 0;
            } else if (choice == optClearFilter) {
                departmentFilter = null;
                roleFilter = null;
                page = 0;
            } else if (choice == optSearch) {
                searchTerm = ui.inputSearchTerm();
                page = 0;
            } else if (choice == optClearSearch) {
                searchTerm = null;
                page = 0;
            } else if (choice == optNextPage) {
                page++;
            } else if (choice == optPrevPage) {
                page--;
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
            if (!ui.confirm("Staff has an active assignment. Cancel it?")) {
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
        LinkedListInterface<TaskAssignment> filtered = nonDeletedAssignments(staff.getTaskAssignments());
        ui.listAllAssignments(assignmentListToTable(filtered));
        ui.pressEnterToContinue();
    }

    private void viewStaffChangeHistory(Staff staff) {
        ui.listAllChanges(changeListToTable(getChangesByStaff(staff.getStaffId())));
        ui.pressEnterToContinue();
    }

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

    private static LinkedListInterface<TaskAssignment> nonDeletedAssignments(
            LinkedListInterface<TaskAssignment> source) {
        LinkedListInterface<TaskAssignment> filtered = new LinkedList<>();
        for (int i = 0; i < source.size(); i++) {
            TaskAssignment assignment = source.get(i);
            if (!assignment.isDeleted()) {
                filtered.addBack(assignment);
            }
        }
        return filtered;
    }

    private void startFirstTaskMenu(Staff staff) {
        LinkedListInterface<TaskAssignment> queue = getStaffTaskQueue(staff);
        if (queue.isEmpty()) {
            ui.printNoTasksInQueue();
            return;
        }
        TaskAssignment first = queue.getFront();
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

    private String startFirstTask(Staff staff) {
        if (staff == null || staff.getStaffId() == null) {
            return "NO_STAFF";
        }
        LinkedListInterface<TaskAssignment> queue = getStaffTaskQueue(staff);
        if (queue.isEmpty()) {
            return "EMPTY_QUEUE";
        }
        TaskAssignment first = queue.getFront();
        if (first.getStatus() == TaskStatus.IN_PROGRESS) {
            return "ALREADY_STARTED";
        }
        return startAssignment(first);
    }

    private String startAssignment(TaskAssignment first) {
        TaskAssignment target = getLatestAssignment(first.getTaskAssignmentId());
        if (target == null) {
            return "UPDATE_FAILED";
        }
        target.setStatus(TaskStatus.IN_PROGRESS);
        taskAssignmentDAO.saveTaskAssignmentList(taskAssignmentList);
        appendAssignmentChange(target, "In Progress", LocalDateTime.now());
        return "STARTED";
    }

    private TaskAssignment getLatestAssignment(String assignmentId) {
        if (assignmentId == null) {
            return null;
        }
        refreshTaskAssignments();
        for (int i = 0; i < taskAssignmentList.size(); i++) {
            TaskAssignment candidate = taskAssignmentList.get(i);
            if (assignmentId.equals(candidate.getTaskAssignmentId())) {
                return candidate;
            }
        }
        return null;
    }

    // task query

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
        String term = searchTerm.toLowerCase();
        for (int i = 0; i < source.size(); i++) {
            Task task = source.get(i);
            if (matchesSearch(task.getTaskId(), term) || matchesSearch(task.getTaskName(), term)) {
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

    private boolean matchesSearch(String value, String term) {
        return value != null && value.toLowerCase().contains(term);
    }

    // staff query

    private LinkedListInterface<Staff> getStaffsByDepartment(Department department) {
        LinkedListInterface<Staff> filtered = new LinkedList<>();
        for (int i = 0; i < staffList.size(); i++) {
            Staff staff = staffList.get(i);
            if (!staff.isDeleted() && staff.getDepartment() != null && staff.getDepartment() == department) {
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
        String term = searchTerm.toLowerCase();
        for (int i = 0; i < source.size(); i++) {
            Staff staff = source.get(i);
            if (matchesSearch(staff.getStaffId(), term) || matchesSearch(staff.getStaffName(), term)) {
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

    // create entity functions

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
        appendTaskStatusChange(task, TaskStatus.PENDING.toString(), LocalDateTime.now());
        return taskId;
    }

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

    // status functions

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

    private static boolean isAllowed(TaskStatus[] options, TaskStatus newStatus) {
        for (TaskStatus option : options) {
            if (option == newStatus) {
                return true;
            }
        }
        return false;
    }

    public String updateTaskStatus(Task task, TaskStatus newStatus, String reason) {
        if (task == null || newStatus == null) {
            return "TRANSITION_DENIED";
        }
        TaskStatus current = task.getTaskStatus();
        if (!isAllowed(getAllowedTransitions(current), newStatus)) {
            return "TRANSITION_DENIED";
        }
        if (newStatus == TaskStatus.COMPLETED) {
            refreshTaskAssignments();
            for (int i = 0; i < taskAssignmentList.size(); i++) {
                TaskAssignment assignment = taskAssignmentList.get(i);
                if (assignment.isDeleted() || !task.getTaskId().equals(assignment.getAssignedTaskId()) || !assignment.isActive()) {
                    continue;
                }
                return "WORKERS_NOT_DONE";
            }
        }
        if (current != null) {
            task.getStatusHistory().addFront(new TaskStatusChange(current, reason, LocalDateTime.now()));
        }
        LocalDateTime previousEnd = task.getEndDateTime();
        task.setTaskStatus(newStatus);
        try {
            if (newStatus == TaskStatus.CANCELLED) {
                refreshTaskAssignments();
                for (int i = 0; i < taskAssignmentList.size(); i++) {
                    TaskAssignment assignment = taskAssignmentList.get(i);
                    if (assignment.isDeleted() || !task.getTaskId().equals(assignment.getAssignedTaskId()) || !assignment.isActive()) {
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
            appendTaskStatusChange(task, newStatus.toString(), LocalDateTime.now());
            taskDAO.saveTaskList(taskList);
        } catch (Exception e) {
            task.setTaskStatus(current);
            task.setEndDateTime(previousEnd);
            if (current != null) {
                task.getStatusHistory().removeFront();
            }
            ConsoleUtil.printError("Status update failed during room/worker update: " + e.getMessage());
            return "UPDATE_FAILED";
        }
        return "UPDATED";
    }

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

    public String updateAssignmentStatus(TaskAssignment assignment, TaskStatus newStatus, String reason) {
        if (assignment == null || newStatus == null) {
            return "TRANSITION_DENIED";
        }
        if (!isAllowed(getAllowedAssignmentTransitions(assignment.getStatus()), newStatus)) {
            return "TRANSITION_DENIED";
        }
        TaskAssignment target = getLatestAssignment(assignment.getTaskAssignmentId());
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
                    appendTaskStatusChange(task, TaskStatus.COMPLETED.toString(), LocalDateTime.now());
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

    public String rollbackTaskStatus(Task task) {
        if (task == null) {
            return "NO_PREVIOUS";
        }
        LinkedListInterface<TaskStatusChange> statusHistoryStack = task.getStatusHistory();
        if (statusHistoryStack.isEmpty()) {
            return "NO_PREVIOUS";
        }
        TaskStatusChange previousStatusChange = statusHistoryStack.removeFront();
        if (previousStatusChange == null || previousStatusChange.getTaskStatus() == null) {
            if (previousStatusChange != null) {
                statusHistoryStack.addFront(previousStatusChange);
            }
            return "NO_PREVIOUS";
        }
        TaskStatus previous = previousStatusChange.getTaskStatus();
        TaskStatus current = task.getTaskStatus();
        LocalDateTime previousEnd = task.getEndDateTime();
        task.setTaskStatus(previous);
        try {
            if (current == TaskStatus.COMPLETED) {
                task.setEndDateTime(null);
                setRoomStatus(task.getRoomId(), RoomStatus.CLEANING);
            }
            appendTaskStatusChange(task, previous.toString(), LocalDateTime.now());
            taskDAO.saveTaskList(taskList);
        } catch (Exception e) {
            task.setTaskStatus(current);
            task.setEndDateTime(previousEnd);
            statusHistoryStack.addFront(previousStatusChange);
            ConsoleUtil.printError("Rollback failed during room status update: " + e.getMessage());
            return "UPDATE_FAILED";
        }
        return "ROLLED_BACK";
    }

    // task and staff assignment functions

    public Staff findEarliestAvailableStaff(TaskType taskType) {
        LinkedListInterface<Staff> eligible = getEligibleStaffByRole(taskType);
        Staff best = null;
        LocalDateTime bestEnd = null;
        for (int i = 0; i < eligible.size(); i++) {
            Staff staff = eligible.get(i);
            LocalDateTime end = latestEndTime(staff.getStaffId());
            if (best == null || isBetterCandidate(end, staff.getStaffId(), bestEnd, best.getStaffId())) {
                best = staff;
                bestEnd = end;
            }
        }
        return best;
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

    private static boolean isBetterCandidate(LocalDateTime end, String staffId,
            LocalDateTime bestEnd, String bestId) {
        if (end == null) {
            return bestEnd != null || staffId.compareToIgnoreCase(bestId) < 0;
        }
        if (bestEnd == null) {
            return false;
        }
        int compared = end.compareTo(bestEnd);
        return compared < 0 || (compared == 0 && staffId.compareToIgnoreCase(bestId) < 0);
    }

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

    public String assignTaskToStaff(Task task, Staff staff) {
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
        appendAssignmentChange(assignment, endStatus.toString(), LocalDateTime.now());
    }

    // update staff status based on assigned tasks
    public void recomputeStaffAvailability(Staff staff) {
        if (staff == null) {
            return;
        }
        AvailabilityStatus staffStatus = staff.getAvailabilityStatus();
        if ( staffStatus == AvailabilityStatus.ON_LEAVE || staffStatus == AvailabilityStatus.RESIGNED) {
            return;
        }
        refreshTaskAssignments();
        for (int i = 0; i < taskAssignmentList.size(); i++) {
            TaskAssignment assignment = taskAssignmentList.get(i);
            if (assignment.isDeleted() || !staff.getStaffId().equals(assignment.getAssignedStaffId()) || !assignment.isActive()) {
                continue;
            }
            staff.setAvailabilityStatus(AvailabilityStatus.BUSY);
            staffDAO.saveStaffList(staffList);
            return;
        }
        staff.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        staffDAO.saveStaffList(staffList);
    }

    // update room status function
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

    // called from inquiry module
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
            assignTaskToStaff(getTaskById(taskId), staff);
        }
        return taskId;
    }

    // called from reservation module
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
            assignTaskToStaff(getTaskById(taskId), cleaner);
        }
        // assign earliest available supervisor to inspection task
        Staff supervisor = findEarliestAvailableStaff(TaskType.INSPECTION);
        if (supervisor != null) {
            assignTaskToStaff(getTaskById(taskId), supervisor);
        }
        return taskId;
    }

    // get entity by id functions

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

    // generate id

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

    // table pagination functions

    private LinkedListInterface<Task> pageOfTask(LinkedListInterface<Task> source, int page) {
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

    // history changes functions

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

    // table functions

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
                    assignment.getStatus() == null ? "-" : assignment.getStatus().toString(),
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