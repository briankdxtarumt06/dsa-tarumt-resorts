package tarumtresort.boundary;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Staff;
import tarumtresort.entity.Task;
import tarumtresort.entity.TaskAssignment;
import tarumtresort.entity.enums.Department;
import tarumtresort.entity.enums.StaffRole;
import tarumtresort.entity.enums.TaskPriority;
import tarumtresort.entity.enums.TaskStatus;
import tarumtresort.entity.enums.TaskType;
import tarumtresort.utility.Ansi;
import tarumtresort.utility.ConsoleUtil;
import tarumtresort.utility.DateTimeUtil;
import tarumtresort.utility.TablePrinter;

/**
 *
 * @author Brian Kam Ding Xian
 *
 */
public class HousekeepingUI {

    private Scanner scanner = new Scanner(System.in);

    public static final LocalTime DEFAULT_SHIFT_START = LocalTime.of(8, 0);

    public HousekeepingUI() {
    }

    public HousekeepingUI(Scanner scanner) {
        this.scanner = scanner;
    }

    public Scanner getScanner() {
        return scanner;
    }

    private static final int BANNER_WIDTH = 32;
    private static final String BANNER_LINE = "=".repeat(BANNER_WIDTH);

    public static void printBanner(String title) {
        System.out.println("\n" + BANNER_LINE);
        System.out.println(center(title, BANNER_WIDTH));
        System.out.println(BANNER_LINE);
    }

    public static void printSeparator() {
        System.out.println(BANNER_LINE);
    }

    public static void printSection(String text) {
        if (text == null) {
            text = "";
        }
        if (text.isEmpty()) {
            System.out.println("\n" + BANNER_LINE);
            return;
        }
        String core = " " + text + " ";
        int available = BANNER_WIDTH - core.length();
        int left = available / 2;
        System.out.println("\n" + "=".repeat(left) + core + "=".repeat(available - left));
    }

    private static String center(String text, int width) {
        if (text == null) {
            text = "";
        }
        if (text.length() >= width) {
            return text;
        }
        int pad = width - text.length();
        int left = pad / 2;
        return " ".repeat(left) + text + " ".repeat(pad - left);
    }

    // MODULE MENU
    public int getMenuChoice() {
        printBanner("HOUSEKEEPING MODULE");
        System.out.println("  1. Task Management");
        System.out.println("  2. Staff Management");
        System.out.println("  3. Room Turnover Report");
        System.out.println("  4. Staff Productivity Report");
        System.out.println("  0. Exit");
        printSeparator();
        return inputIntChoice("Enter choice", 0, 4);
    }

    public int printTaskListMenu(LinkedListInterface<Task> pageList, int page, int pageCount, TaskStatus statusFilter,
            TaskPriority priorityFilter, String searchTerm) {
        clearScreen();
        printBanner("TASK MANAGEMENT (Page " + (page + 1) + " of " + pageCount + ")");
        if (statusFilter != null) {
            System.out.println("  Filter: Status = " + statusFilter.name());
        }
        if (priorityFilter != null) {
            System.out.println("  Filter: Priority = " + priorityFilter.name());
        }
        if (searchTerm != null) {
            System.out.println("  Search: \"" + searchTerm + "\"");
        }
        if (pageList.isEmpty()) {
            System.out.println("  (no task records)");
        } else {
            String[] header = new String[] { "No.", "Task ID", "Name", "Type", "Priority", "Status" };
            String[][] rows = new String[pageList.size()][6];
            for (int i = 0; i < pageList.size(); i++) {
                Task task = pageList.get(i);
                rows[i] = new String[] {
                    String.valueOf(i + 1), task.getTaskId(), task.getTaskName(),
                    task.getTaskType() == null ? "-" : task.getTaskType().name(),
                    task.getTaskPriority() == null ? "-" : task.getTaskPriority().name(),
                    task.getTaskStatus() == null ? "-" : task.getTaskStatus().name()
                };
            }
            TablePrinter.displayTable(header, rows);
        }
        printSection("Actions");
        int action = 1;
        System.out.println("  " + action++ + ". View Details");
        System.out.println("  " + action++ + ". Add New Task");
        System.out.println("  " + action++ + ". Update Task Status");
        System.out.println("  " + action++ + ". Roll Back Task Status");
        System.out.println("  " + action++ + ". Add Filter");
        boolean hasFilter = statusFilter != null || priorityFilter != null;
        boolean hasSearch = searchTerm != null;
        if (hasFilter) {
            System.out.println("  " + action++ + ". Clear Filter");
        }
        if (!hasSearch) {
            System.out.println("  " + action++ + ". Search Tasks");
        }
        if (hasSearch) {
            System.out.println("  " + action++ + ". Clear Search");
        }
        if (page < pageCount - 1) {
            System.out.println("  " + action++ + ". Next Page");
        }
        if (page > 0) {
            System.out.println("  " + action++ + ". Previous Page");
        }
        System.out.println("  0. Back");
        printSeparator();
        return inputIntChoice("Enter choice", 0, action - 1);
    }

    public int printStaffListMenu(LinkedListInterface<Staff> pageList, int page, int pageCount,
            Department departmentFilter, StaffRole roleFilter, String searchTerm) {
        clearScreen();
        printBanner("STAFF MANAGEMENT (Page " + (page + 1) + " of " + pageCount + ")");
        if (departmentFilter != null) {
            System.out.println("  Filter: Department = " + departmentFilter);
        }
        if (roleFilter != null) {
            System.out.println("  Filter: Role = " + roleFilter);
        }
        if (searchTerm != null) {
            System.out.println("  Search: \"" + searchTerm + "\"");
        }
        if (pageList.isEmpty()) {
            System.out.println("  (no staff records)");
        } else {
            String[] header = new String[] { "No.", "Staff ID", "Name", "Department", "Role", "Availability" };
            String[][] rows = new String[pageList.size()][6];
            for (int i = 0; i < pageList.size(); i++) {
                Staff staff = pageList.get(i);
                rows[i] = new String[] {
                    String.valueOf(i + 1), staff.getStaffId(), staff.getStaffName(),
                    staff.getDepartment() == null ? "-" : staff.getDepartment().toString(),
                    staff.getStaffRole() == null ? "-" : staff.getStaffRole().toString(),
                    staff.getAvailabilityStatus() == null ? "-" : staff.getAvailabilityStatus().name()
                };
            }
            TablePrinter.displayTable(header, rows);
        }
        printSection("Actions");
        int action = 1;
        System.out.println("  " + action++ + ". View Details");
        System.out.println("  " + action++ + ". Add New Staff");
        System.out.println("  " + action++ + ". Add Filter");
        boolean hasFilter = departmentFilter != null || roleFilter != null;
        boolean hasSearch = searchTerm != null;
        if (hasFilter) {
            System.out.println("  " + action++ + ". Clear Filter");
        }
        if (!hasSearch) {
            System.out.println("  " + action++ + ". Search Staff");
        }
        if (hasSearch) {
            System.out.println("  " + action++ + ". Clear Search");
        }
        if (page < pageCount - 1) {
            System.out.println("  " + action++ + ". Next Page");
        }
        if (page > 0) {
            System.out.println("  " + action++ + ". Previous Page");
        }
        System.out.println("  0. Back");
        printSeparator();
        return inputIntChoice("Enter choice", 0, action - 1);
    }

    public int getTaskActionChoice() {
        printSection("Task Actions");
        System.out.println("  1. Edit Task Details");
        System.out.println("  2. Update Task Status");
        System.out.println("  3. Roll Back Latest Status");
        System.out.println("  4. View Staff Assigned");
        System.out.println("  5. Assign / Reassign Staff");
        System.out.println("  6. Delete Task");
        System.out.println("  7. View Change History");
        System.out.println("  0. Back to List");
        printSeparator();
        return inputIntChoice("Enter choice", 0, 7);
    }

public int getStaffActionChoice() {
        printSection("Staff Actions");
        System.out.println("  1. Edit Staff Details");
        System.out.println("  2. Start First Task in Queue");
        System.out.println("  3. View Assignment History");
        System.out.println("  4. Resign Staff");
        System.out.println("  5. View Change History");
        System.out.println("  0. Back");
        printSeparator();
        return inputIntChoice("Enter choice", 0, 5);
    }

    public int getAssignmentActionChoice(TaskAssignment assignment) {
        printSection("Assignment Actions");
        System.out.println("  Assignment: "
                + (assignment == null || assignment.getTaskAssignmentId() == null ? "-" : assignment.getTaskAssignmentId()));
        System.out.println("  1. View Change History");
        System.out.println("  2. Update Status");
        System.out.println("  0. Back");
        printSeparator();
        return inputIntChoice("Enter choice", 0, 2);
    }

    public int inputTaskFieldChoice() {
        printSection("Select Field to Edit");
        System.out.println("  1. Task Name");
        System.out.println("  2. Task Type");
        System.out.println("  3. Priority");
        System.out.println("  4. Room ID");
        System.out.println("  5. Start Date & Time");
        System.out.println("  0. Back");
        return inputIntChoice("Enter choice", 0, 5);
    }

    public int inputStaffFieldChoice() {
        printSection("Select Field to Edit");
        System.out.println("  1. Staff Name");
        System.out.println("  2. Department");
        System.out.println("  3. Staff Role");
        System.out.println("  4. Toggle On Leave");
        System.out.println("  0. Back");
        return inputIntChoice("Enter choice", 0, 4);
    }

    public int inputTaskFilterDimension() {
        printSection("Filter Tasks By");
        System.out.println("  1. Status");
        System.out.println("  2. Priority");
        System.out.println("  0. Back");
        return inputIntChoice("Enter choice", 0, 2);
    }

    public int inputStaffFilterDimension() {
        printSection("Filter Staff By");
        System.out.println("  1. Department");
        System.out.println("  2. Role");
        System.out.println("  0. Back");
        return inputIntChoice("Enter choice", 0, 2);
    }

    // INPUT METHODS (staff)
    public String inputStaffName() {
        String input = "";
        while (input.trim().isEmpty()) {
            System.out.print("Enter Staff Name: ");
            input = scanner.nextLine();
            if (input.trim().isEmpty())
                ConsoleUtil.printError("Staff Name cannot be empty!");
        }
        return input.trim();
    }

    public Department inputDepartment() {
        System.out.println("\nSelect Department:");
        Department[] departments = Department.values();
        int count = 0;
        for (Department department : departments) {
            if (department == Department.UNKNOWN) {
                continue;
            }
            count++;
            System.out.println("  " + count + ". " + department);
        }
        return departments[inputIntChoice("Enter department", 1, count) - 1];
    }

    public StaffRole inputStaffRole() {
        System.out.println("Select Staff Role:");
        StaffRole[] roles = StaffRole.values();
        int count = 0;
        for (StaffRole role : roles) {
            if (role == StaffRole.UNKNOWN) {
                continue;
            }
            count++;
            System.out.println("  " + count + ". " + role);
        }
        return roles[inputIntChoice("Enter staff role", 1, count) - 1];
    }

    // INPUT METHODS (task)
    public String inputTaskName() {
        String input = "";
        while (input.trim().isEmpty()) {
            System.out.print("Enter Task Name: ");
            input = scanner.nextLine();
            if (input.trim().isEmpty())
                ConsoleUtil.printError("Task Name cannot be empty!");
        }
        return input.trim();
    }

    public TaskType inputTaskType() {
        System.out.println("\nSelect Task Type:");
        System.out.println("  1. CHECKOUT_CLEAN");
        System.out.println("  2. MAINTENANCE");
        System.out.println("  3. INSPECTION");
        System.out.println("  4. ROOM_SERVICE");
        int choice = inputIntChoice("Enter task type", 1, 4);
        return switch (choice) {
            case 1 -> TaskType.CHECKOUT_CLEAN;
            case 2 -> TaskType.MAINTENANCE;
            case 3 -> TaskType.INSPECTION;
            default -> TaskType.ROOM_SERVICE;
        };
    }

    public TaskPriority inputTaskPriority() {
        System.out.println("\nSelect Task Priority:");
        System.out.println("  1. HIGH");
        System.out.println("  2. MEDIUM");
        System.out.println("  3. LOW");
        int choice = inputIntChoice("Enter task priority", 1, 3);
        return choice == 1 ? TaskPriority.HIGH : choice == 2 ? TaskPriority.MEDIUM : TaskPriority.LOW;
    }

    public TaskStatus inputTaskStatusFilter() {
        System.out.println("\nSelect Task Status:");
        System.out.println("  1. PENDING");
        System.out.println("  2. IN_PROGRESS");
        System.out.println("  3. COMPLETED");
        System.out.println("  4. CANCELLED");
        int choice = inputIntChoice("Enter task status", 1, 4);
        return switch (choice) {
            case 1 -> TaskStatus.PENDING;
            case 2 -> TaskStatus.IN_PROGRESS;
            case 3 -> TaskStatus.COMPLETED;
            default -> TaskStatus.CANCELLED;
        };
    }

    public TaskPriority inputTaskPriorityFilter() {
        return inputTaskPriority();
    }

    public String inputOptionalRoomId() {
        System.out.print("Enter Room ID (blank = none): ");
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? null : input;
    }

    public LocalDateTime inputStartDateTime() {
        return inputDateTimeWithQuickOptions("Task Start Date & Time");
    }

    public String inputSearchTerm() {
        System.out.print("Enter search term (blank = cancel): ");
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? null : input;
    }

    public TaskStatus selectTaskStatus(TaskStatus[] options) {
        System.out.println("\nSelect Task Status:");
        for (int i = 0; i < options.length; i++) {
            System.out.println("  " + (i + 1) + ". " + options[i].name());
        }
        System.out.println("  0. Cancel");
        int choice = inputIntChoice("Enter task status", 0, options.length);
        return choice == 0 ? null : options[choice - 1];
    }

    public String inputCancellationReason() {
        String input = "";
        while (input.trim().isEmpty()) {
            System.out.print("Enter cancellation reason (0 = cancel): ");
            input = scanner.nextLine();
            if (input.trim().isEmpty())
                ConsoleUtil.printError("Cancellation reason cannot be empty!");
        }
        if (input.trim().equals("0")) {
            return null;
        }
        return input.trim();
    }

    public int inputAssignMode() {
        printSection("Assign Staff");
        System.out.println("  1. Auto-assign (earliest available staff)");
        System.out.println("  2. Choose staff manually");
        System.out.println("  0. Cancel");
        return inputIntChoice("Enter choice", 0, 2);
    }

    public int selectStaff(LinkedListInterface<Staff> staffList) {
        printSection("Select Staff to Assign");
        if (staffList.isEmpty()) {
            System.out.println("  (no eligible staff)");
        } else {
            String[] header = new String[] { "No.", "Staff ID", "Name", "Department", "Role", "Availability" };
            String[][] rows = new String[staffList.size()][6];
            for (int i = 0; i < staffList.size(); i++) {
                Staff staff = staffList.get(i);
                rows[i] = new String[] {
                    String.valueOf(i + 1), staff.getStaffId(), staff.getStaffName(),
                    staff.getDepartment() == null ? "-" : staff.getDepartment().toString(),
                    staff.getStaffRole() == null ? "-" : staff.getStaffRole().toString(),
                    staff.getAvailabilityStatus() == null ? "-" : staff.getAvailabilityStatus().name()
                };
            }
            TablePrinter.displayTable(header, rows);
        }
        System.out.println("  0. Cancel");
        return inputIntChoice("Enter choice", 0, staffList.size());
    }

    public String inputConfirmName(String expectedName) {
        String input = "";
        while (input.trim().isEmpty()) {
            System.out.print("Type the name \"" + expectedName + "\" to confirm: ");
            input = scanner.nextLine();
            if (input.trim().isEmpty())
                ConsoleUtil.printError("Name cannot be empty!");
        }
        return input.trim();
    }

    public boolean confirm(String prompt) {
        while (true) {
            System.out.print(prompt + " (y/n): ");
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("y") || input.equalsIgnoreCase("yes")) {
                return true;
            }
            if (input.equalsIgnoreCase("n") || input.equalsIgnoreCase("no")) {
                return false;
            }
            ConsoleUtil.printError("Please answer y or n!");
        }
    }

    // -------------------- QUICK DATE-TIME INPUT --------------------

    private LocalDateTime inputDateTimeWithQuickOptions(String prompt) {
        while (true) {
            printBanner(prompt.toUpperCase());
            System.out.println("  1. Now");
            System.out.println("  2. 30 Minutes Later");
            System.out.println("  3. 1 Hour Later");
            System.out.println("  4. 2 Hours Later");
            System.out.println("  5. Next Shift Start (08:00)");
            System.out.println("  6. Custom (type manually)");
            printSeparator();
            int choice = inputIntChoice("Enter option", 1, 6);

            switch (choice) {
                case 1: return LocalDateTime.now();
                case 2: return LocalDateTime.now().plusMinutes(30);
                case 3: return LocalDateTime.now().plusHours(1);
                case 4: return LocalDateTime.now().plusHours(2);
                case 5: return nextShiftStart();
                case 6: return inputCustomDateTime(prompt);
            }
        }
    }

    private LocalDateTime nextShiftStart() {
        LocalDateTime shift = LocalDateTime.of(LocalDate.now(), DEFAULT_SHIFT_START);
        return shift.isBefore(LocalDateTime.now()) ? shift.plusDays(1) : shift;
    }

    private LocalDateTime inputCustomDateTime(String prompt) {
        LocalDateTime dateTime = null;
        while (dateTime == null) {
            System.out.print("Enter " + prompt.toLowerCase() + " (yyyy-MM-dd HH:mm): ");
            String input = scanner.nextLine();
            try {
                dateTime = LocalDateTime.parse(input, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            } catch (DateTimeParseException e) {
                ConsoleUtil.printError("Invalid date & time format! Please use yyyy-MM-dd HH:mm.");
            }
        }
        return dateTime;
    }

    // DISPLAY / OUTPUT METHODS
    public static void printDetails(String[][] details) {
        if (details == null || details.length == 0) {
            return;
        }

        int keyWidth = 0;
        for (String[] pair : details) {
            if (pair[0] != null && pair[0].length() > keyWidth) {
                keyWidth = pair[0].length();
            }
        }

        printSection("Details");
        for (String[] pair : details) {
            System.out.println(String.format("%-" + (keyWidth + 3) + "s: %s",
                    pair[0] == null ? "" : pair[0],
                    pair.length > 1 && pair[1] != null ? pair[1] : "-"));
        }
        printSeparator();
    }

    public void printTaskDetails(Task task, TaskAssignment active) {
        printDetails(new String[][] {
            {"Task ID", task.getTaskId()},
            {"Task Name", task.getTaskName()},
            {"Task Type", task.getTaskType() == null ? "-" : task.getTaskType().name()},
            {"Current Status", task.getTaskStatus() == null ? "-" : task.getTaskStatus().name()},
            {"Priority", task.getTaskPriority() == null ? "-" : task.getTaskPriority().name()},
            {"Start Date & Time", DateTimeUtil.readable(task.getStartDateTime())},
            {"End Date & Time", DateTimeUtil.readable(task.getEndDateTime())},
            {"Room ID", task.getRoomId() == null ? "-" : task.getRoomId()},
            {"Active Assignment", active == null ? "-"
                    : active.getTaskAssignmentId() + " (" + (active.getStatus() == null ? "-" : active.getStatus().name()) + ")"}
        });
    }

    public void printStaffDetails(Staff staff, TaskAssignment active) {
        printDetails(new String[][] {
            {"Staff ID", staff.getStaffId()},
            {"Staff Name", staff.getStaffName()},
            {"Department", staff.getDepartment() == null ? "-" : staff.getDepartment().toString()},
            {"Staff Role", staff.getStaffRole() == null ? "-" : staff.getStaffRole().toString()},
            {"Availability", staff.getAvailabilityStatus() == null ? "-" : staff.getAvailabilityStatus().name()},
            {"Active Assignment", active == null ? "-"
                    : active.getTaskAssignmentId() + " (" + (active.getStatus() == null ? "-" : active.getStatus().name()) + ")"}
        });
    }

    public void printTaskCreationSummary(String taskName, TaskType taskType, TaskPriority taskPriority,
            String roomId, LocalDateTime startDateTime) {
        printDetails(new String[][] {
            {"Task Name", taskName},
            {"Task Type", taskType == null ? "-" : taskType.name()},
            {"Priority", taskPriority == null ? "-" : taskPriority.name()},
            {"Room ID", roomId == null ? "-" : roomId},
            {"Start Date & Time", DateTimeUtil.readable(startDateTime)}
        });
    }

    public void printTaskEditSummary(String taskName, TaskType taskType, TaskPriority taskPriority, String roomId,
            LocalDateTime startDateTime) {
        printTaskCreationSummary(taskName, taskType, taskPriority, roomId, startDateTime);
    }

    public void printStaffCreationSummary(String staffName, Department department, StaffRole staffRole) {
        printDetails(new String[][] {
            {"Staff Name", staffName},
            {"Department", department == null ? "-" : department.toString()},
            {"Staff Role", staffRole == null ? "-" : staffRole.toString()}
        });
    }

    public void printStaffEditSummary(String staffName, Department department, StaffRole staffRole) {
        printStaffCreationSummary(staffName, department, staffRole);
    }

    public void printStatusChangeSummary(String taskId, TaskStatus current, TaskStatus next, String reason) {
        printDetails(new String[][] {
            {"Task ID", taskId},
            {"Status Change", (current == null ? "-" : current.name()) + "  to  " + next.name()},
            {"Reason", reason == null ? "-" : reason}
        });
    }

    public void printAssignmentStatusSummary(String assignmentId, TaskStatus current, TaskStatus next, String reason) {
        printDetails(new String[][] {
            {"Assignment ID", assignmentId},
            {"Status Change", (current == null ? "-" : current.name()) + "  to  " + next.name()},
            {"Reason", reason == null ? "-" : reason}
        });
    }

    public void printTaskAutoCompleted(Task task) {
        if (task == null) {
            return;
        }
        ConsoleUtil.printSuccess("Task " + task.getTaskId()
                + " auto-completed: all assigned workers finished.");
    }

    public void listAllAssignments(String[][] data) {
        printSection("Assignment List");
        if (data.length <= 1) {
            System.out.println("  No assignment records found.");
            return;
        }
        String[] header = data[0];
        String[][] rows = new String[data.length - 1][];
        System.arraycopy(data, 1, rows, 0, rows.length);
        TablePrinter.displayTable(header, rows);
    }

    public void listAllChanges(String[][] data) {
        printSection("Change History");
        if (data.length <= 1) {
            System.out.println("  No change records found.");
            return;
        }
        String[] header = data[0];
        String[][] rows = new String[data.length - 1][];
        System.arraycopy(data, 1, rows, 0, rows.length);
        TablePrinter.displayTable(header, rows);
    }

    public void printTaskId(String taskId) {
        ConsoleUtil.printSuccess("New Task ID: " + taskId);
    }

    public void printStaffId(String staffId) {
        ConsoleUtil.printSuccess("New Staff ID: " + staffId);
    }

    public void printSuccess() {
        ConsoleUtil.printSuccess("Operation successful!");
    }

    public void printNotFound() {
        ConsoleUtil.printError("Record not found!");
    }

    public void printDuplicateNameError() {
        ConsoleUtil.printError("Name already exists!");
    }

    public void printStaffUnavailable() {
        ConsoleUtil.printError("Staff is not available right now!");
    }

    public void printNoStaffFreeForTask() {
        ConsoleUtil.printError("No eligible staff is free for this task right now!");
    }

    public void printNoPreviousStatus() {
        ConsoleUtil.printError("No previous status to roll back to!");
        System.out.println("    (The rollback stack for this task is empty.)");
    }

    public void printTaskStatusDenied() {
        ConsoleUtil.printError("Task status change not allowed!");
        System.out.println("    The transition is outside the allowed status matrix");
        System.out.println("    (or the task still has unfinished workers).");
    }

    public void printTaskClosed() {
        ConsoleUtil.printError("Task is already closed (completed or cancelled)!");
    }

    public void printTaskAlreadyAssigned() {
        ConsoleUtil.printError("Task already has an active assignment!");
    }

    public void printStaffHasActiveAssignment() {
        ConsoleUtil.printError("Staff has an active assignment and cannot go on leave!");
    }

    public void printNoRecords() {
        ConsoleUtil.printError("No records to view!");
    }

    public void printTaskStarted(String taskName) {
        ConsoleUtil.printSuccess("Task started: " + (taskName == null ? "-" : taskName));
    }

    public void printNoTasksInQueue() {
        ConsoleUtil.printError("No pending task in this staff member's queue!");
    }

    public void printTaskAlreadyStarted() {
        ConsoleUtil.printWarning("The first task in the queue is already in progress!");
    }

    public void printWarning(String message) {
        ConsoleUtil.printWarning(message);
    }

    public void printExitMessage() {
        System.out.println("\n  Exiting Housekeeping Module. Goodbye!");
    }

    public void printInvalidChoice() {
        ConsoleUtil.printError("Invalid choice! Please try again.");
    }

    public int inputListIndex(String entityLabel, int max) {
        return inputIntChoice("Enter " + entityLabel + " number to view (0 = cancel)", 0, max);
    }

    private int inputIntChoice(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt + " (" + min + "-" + max + "): ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                if (Ansi.ENABLED) {
                    System.out.print("\u001B[1A\u001B[2K");
                }
                continue;
            }
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    System.out.println();
                    return value;
                }
            } catch (NumberFormatException e) {
                // continue retry until integer input
            }
            ConsoleUtil.printError("Please enter a number between " + min + " and " + max + "!");
        }
    }

    private void clearScreen() {
        ConsoleUtil.clearScreen();
    }

    public void pressEnterToContinue() {
        ConsoleUtil.pressEnterToContinue(scanner);
    }
}