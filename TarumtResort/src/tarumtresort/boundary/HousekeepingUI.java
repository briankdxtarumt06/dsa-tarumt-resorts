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
import tarumtresort.entity.enums.TaskPriority;
import tarumtresort.entity.enums.TaskStatus;
import tarumtresort.utility.Ansi;
import tarumtresort.utility.ConsoleUtil;
import tarumtresort.utility.TablePrinter;

/**
 *
 * @author Brian
 *
 * Combined UI for the Housekeeping module (Staff + Task + Assignment).
 */
public class HousekeepingUI {

    private Scanner scanner = new Scanner(System.in);

    public static final String[] DEPARTMENTS = {"Finance", "Housekeeping", "Maintenance", "Front Office"};
    public static final String[] STAFF_ROLES = {"Manager", "Supervisor", "Cleaner", "Technician", "Receptionist", "Admin"};
    public static final String[] AVAILABILITY_STATUSES = {"Available", "Unavailable", "On Leave", "Resigned"};

    // keep in sync with HousekeepingController.SHIFT_START
    public static final LocalTime DEFAULT_SHIFT_START = LocalTime.of(8, 0);

    public HousekeepingUI() {
    }

    public HousekeepingUI(Scanner scanner) {
        this.scanner = scanner;
    }

    // MODULE MENU
    public int getMenuChoice() {
        System.out.println("\n========================================");
        System.out.println("  HOUSEKEEPING MODULE");
        System.out.println("========================================");
        System.out.println("  1. Staff Management");
        System.out.println("  2. Task Management");
        System.out.println("  3. Task Assignment Management");
        System.out.println("  4. Reports");
        System.out.println("  0. Exit");
        System.out.println("========================================");
        return inputIntChoice("Enter choice", 0, 4);
    }

    // STAFF SELECT-ENTITY LIST
    // prints the numbered staff list plus the extra options; returns the choice
    // STAFF SELECT-ENTITY LIST (paged; the Actions section is separate from
    // the rows, so a row is never chosen directly - use "View Details")
    public int printStaffListMenu(LinkedListInterface<Staff> pageList, int page, int pageCount, boolean hasFilter) {
        clearScreen();
        System.out.println("\n========================================");
        System.out.println("  STAFF MANAGEMENT (Page " + (page + 1) + " of " + pageCount + ")");
        System.out.println("========================================");
        if (pageList.isEmpty()) {
            System.out.println("  (no staff records)");
        } else {
            String[] header = new String[] { "No.", "Staff ID", "Name", "Role", "Availability" };
            String[][] rows = new String[pageList.size()][5];
            for (int i = 0; i < pageList.size(); i++) {
                Staff staff = pageList.get(i);
                rows[i] = new String[] {
                    String.valueOf(i + 1), staff.getStaffId(), staff.getStaffName(),
                    staff.getStaffRole(), staff.getAvailabilityStatus()
                };
            }
            TablePrinter.displayTable(header, rows);
        }
        System.out.println("\n--- Actions ---");
        int action = 1;
        System.out.println("  " + action++ + ". View Details");
        System.out.println("  " + action++ + ". Add New Staff");
        System.out.println("  " + action++ + ". Filter by Department");
        System.out.println("  " + action++ + ". Filter by Availability");
        if (page < pageCount - 1) {
            System.out.println("  " + action++ + ". Next Page");
        }
        if (page > 0) {
            System.out.println("  " + action++ + ". Previous Page");
        }
        if (hasFilter) {
            System.out.println("  " + action++ + ". Clear Filter");
        }
        System.out.println("  0. Back");
        System.out.println("========================================");
        return inputIntChoice("Enter choice", 0, action - 1);
    }

    public int getStaffActionChoice() {
        System.out.println("\n--- Staff Actions ---");
        System.out.println("  1. Update Staff Info");
        System.out.println("  2. Resign Staff");
        System.out.println("  3. View Task Assignments");
        System.out.println("  0. Back to List");
        return inputIntChoice("Enter choice", 0, 3);
    }

    // TASK SELECT-ENTITY LIST (paged; the Actions section is separate from
    // the rows, so a row is never chosen directly - use "View Details")
    public int printTaskListMenu(LinkedListInterface<Task> pageList, int page, int pageCount, boolean hasFilter) {
        clearScreen();
        System.out.println("\n========================================");
        System.out.println("  TASK MANAGEMENT (Page " + (page + 1) + " of " + pageCount + ")");
        System.out.println("========================================");
        if (pageList.isEmpty()) {
            System.out.println("  (no task records)");
        } else {
            String[] header = new String[] { "No.", "Task ID", "Name", "Type", "Priority", "Status" };
            String[][] rows = new String[pageList.size()][6];
            for (int i = 0; i < pageList.size(); i++) {
                Task task = pageList.get(i);
                rows[i] = new String[] {
                    String.valueOf(i + 1), task.getTaskId(), task.getTaskName(),
                    task.getTaskType(), task.getTaskPriority().name(),
                    task.getTaskStatus() == null ? "-" : task.getTaskStatus().name()
                };
            }
            TablePrinter.displayTable(header, rows);
        }
        System.out.println("\n--- Actions ---");
        int action = 1;
        System.out.println("  " + action++ + ". View Details");
        System.out.println("  " + action++ + ". Add New Task");
        System.out.println("  " + action++ + ". Filter by Priority");
        System.out.println("  " + action++ + ". Filter by Type");
        if (page < pageCount - 1) {
            System.out.println("  " + action++ + ". Next Page");
        }
        if (page > 0) {
            System.out.println("  " + action++ + ". Previous Page");
        }
        if (hasFilter) {
            System.out.println("  " + action++ + ". Clear Filter");
        }
        System.out.println("  0. Back");
        System.out.println("========================================");
        return inputIntChoice("Enter choice", 0, action - 1);
    }

    public int getTaskActionChoice() {
        System.out.println("\n--- Task Actions ---");
        System.out.println("  1. Update Task Info");
        System.out.println("  2. Update Task Status");
        System.out.println("  3. Assign Task to Room");
        System.out.println("  4. View Assignments");
        System.out.println("  5. Rollback Task Status");
        System.out.println("  6. Remove Task");
        System.out.println("  0. Back to List");
        return inputIntChoice("Enter choice", 0, 6);
    }

    // ASSIGNMENT SELECT-ENTITY LIST (paged; the Actions section is separate
    // from the rows, so a row is never chosen directly - use "View Details")
    public int printAssignmentListMenu(String[] lines, int page, int pageCount, boolean hasFilter) {
        clearScreen();
        System.out.println("\n========================================");
        System.out.println("  ASSIGNMENTS & SCHEDULING (Page " + (page + 1) + " of " + pageCount + ")");
        System.out.println("========================================");
        if (lines.length == 0) {
            System.out.println("  (no assignment records)");
        } else {
            String[] header = new String[] { "No.", "Assignment ID", "Staff → Task", "Status", "When" };
            String[][] rows = new String[lines.length][5];
            for (int i = 0; i < lines.length; i++) {
                String[] parts = lines[i].split(" \\| ", -1);
                rows[i] = new String[] {
                    String.valueOf(i + 1),
                    parts.length > 0 ? parts[0] : "",
                    parts.length > 1 ? parts[1] : "",
                    parts.length > 2 ? parts[2] : "",
                    parts.length > 3 ? parts[3] : ""
                };
            }
            TablePrinter.displayTable(header, rows);
        }
        System.out.println("\n--- Actions ---");
        int action = 1;
        System.out.println("  " + action++ + ". View Details");
        System.out.println("  " + action++ + ". + New Assignment");
        System.out.println("  " + action++ + ". Filter by Staff");
        System.out.println("  " + action++ + ". Filter by Task");
        System.out.println("  " + action++ + ". View Tasks by Room");
        System.out.println("  " + action++ + ". Simulate Guest Checkout");
        System.out.println("  " + action++ + ". Guest Cleaning Request");
        System.out.println("  " + action++ + ". View All Change History");
        if (page < pageCount - 1) {
            System.out.println("  " + action++ + ". Next Page");
        }
        if (page > 0) {
            System.out.println("  " + action++ + ". Previous Page");
        }
        if (hasFilter) {
            System.out.println("  " + action++ + ". Clear Filter");
        }
        System.out.println("  0. Back");
        System.out.println("========================================");
        return inputIntChoice("Enter choice", 0, action - 1);
    }

    public int getAssignmentActionChoice() {
        System.out.println("\n--- Assignment Actions ---");
        System.out.println("  1. Update Assignment Status (Worker)");
        System.out.println("  2. Reassign Staff / Task");
        System.out.println("  3. View Change History");
        System.out.println("  0. Back to List");
        return inputIntChoice("Enter choice", 0, 3);
    }

    // INPUT METHODS (staff)
    public String inputStaffId() {
        String input = "";
        while (input.trim().isEmpty()) {
            System.out.print("Enter Staff ID: ");
            input = scanner.nextLine();
            if (input.trim().isEmpty())
                ConsoleUtil.printError("Staff ID cannot be empty!");
        }
        return input.trim();
    }

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

    public String inputDepartment() {
        System.out.println("\nSelect Department:");
        for (int i = 0; i < DEPARTMENTS.length; i++) {
            System.out.println("  " + (i + 1) + ". " + DEPARTMENTS[i]);
        }
        return DEPARTMENTS[inputIntChoice("Enter department", 1, DEPARTMENTS.length) - 1];
    }

    public String inputStaffRole() {
        System.out.println("\nSelect Staff Role:");
        for (int i = 0; i < STAFF_ROLES.length; i++) {
            System.out.println("  " + (i + 1) + ". " + STAFF_ROLES[i]);
        }
        return STAFF_ROLES[inputIntChoice("Enter staff role", 1, STAFF_ROLES.length) - 1];
    }

    public String inputAvailabilityStatus() {
        System.out.println("\nSelect Availability Status:");
        for (int i = 0; i < AVAILABILITY_STATUSES.length; i++) {
            System.out.println("  " + (i + 1) + ". " + AVAILABILITY_STATUSES[i]);
        }
        return AVAILABILITY_STATUSES[inputIntChoice("Enter availability status", 1, AVAILABILITY_STATUSES.length) - 1];
    }

    public String[] inputStaffDetails() {
        System.out.println("\n--- Add New Staff ---");
        String staffName = inputStaffName();
        String department = inputDepartment();
        String staffRole = inputStaffRole();
        String availabilityStatus = inputAvailabilityStatus();
        System.out.println();
        return new String[]{staffName, department, staffRole, availabilityStatus};
    }

    public String[] inputUpdateStaffDetails() {
        System.out.println("\n--- Update Staff Information ---");
        String staffName = inputStaffName();
        String department = inputDepartment();
        String staffRole = inputStaffRole();
        String availabilityStatus = inputAvailabilityStatus();
        System.out.println();
        return new String[]{staffName, department, staffRole, availabilityStatus};
    }

    // INPUT METHODS (task)
    public String inputTaskId() {
        String input = "";
        while (input.trim().isEmpty()) {
            System.out.print("Enter Task ID: ");
            input = scanner.nextLine();
            if (input.trim().isEmpty())
                ConsoleUtil.printError("Task ID cannot be empty!");
        }
        return input.trim();
    }

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

    public String inputTaskType() {
        String input = "";
        while (input.trim().isEmpty()) {
            System.out.print("Enter Task Type (e.g. Housekeeping, Maintenance): ");
            input = scanner.nextLine();
            if (input.trim().isEmpty())
                ConsoleUtil.printError("Task Type cannot be empty!");
        }
        return input.trim();
    }

    public TaskPriority inputTaskPriority() {
        System.out.println("\nSelect Task Priority:");
        System.out.println("  1. HIGH");
        System.out.println("  2. MEDIUM");
        System.out.println("  3. LOW");
        int choice = inputIntChoice("Enter task priority", 1, 3);
        return choice == 1 ? TaskPriority.HIGH : choice == 2 ? TaskPriority.MEDIUM : TaskPriority.LOW;
    }

    public LocalDateTime inputStartDateTime() {
        return inputDateTimeWithQuickOptions("Task Start Date & Time");
    }

    public LocalDateTime parseDateTime(String value) {
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            ConsoleUtil.printError("Invalid date & time format! Please use yyyy-MM-dd HH:mm.");
            return null;
        }
    }

    public String inputTaskStatus(TaskStatus currentStatus) {
        System.out.println("\nSelect Task Status:");
        TaskStatus[] options = getValidTransitions(currentStatus);
        if (options.length == 0) {
            ConsoleUtil.printWarning("No valid status transitions available — use Rollback instead.");
            return null;
        }
        for (int i = 0; i < options.length; i++) {
            System.out.println("  " + (i + 1) + ". " + options[i].name());
        }
        int choice = inputIntChoice("Enter task status", 1, options.length);
        return options[choice - 1].name();
    }

    private TaskStatus[] getValidTransitions(TaskStatus current) {
        if (current == null) {
            return new TaskStatus[]{TaskStatus.PENDING, TaskStatus.CANCELLED};
        }
        return switch (current) {
            case PENDING -> new TaskStatus[]{TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED};
            case IN_PROGRESS -> new TaskStatus[]{TaskStatus.CANCELLED, TaskStatus.COMPLETED};
            case COMPLETED -> new TaskStatus[]{};
            case CANCELLED -> new TaskStatus[]{TaskStatus.IN_PROGRESS};
        };
    }

    public String[] inputTaskDetails() {
        System.out.println("\n--- Add New Task ---");
        String taskName = inputTaskName();
        String taskType = inputTaskType();
        TaskPriority taskPriority = inputTaskPriority();
        LocalDateTime startDateTime = inputStartDateTime();
        System.out.println();
        return new String[]{taskName, taskType, taskPriority.name(), startDateTime.toString()};
    }

    public String[] inputUpdateTaskDetails() {
        System.out.println("\n--- Update Task Information ---");
        String taskName = inputTaskName();
        String taskType = inputTaskType();
        TaskPriority taskPriority = inputTaskPriority();
        LocalDateTime startDateTime = inputStartDateTime();
        System.out.println();
        return new String[]{taskName, taskType, taskPriority.name(), startDateTime.toString()};
    }

    // INPUT METHODS (assignment)
    public String inputAssignmentId() {
        String input = "";
        while (input.trim().isEmpty()) {
            System.out.print("Enter Assignment ID: ");
            input = scanner.nextLine();
            if (input.trim().isEmpty())
                ConsoleUtil.printError("Assignment ID cannot be empty!");
        }
        return input.trim();
    }

    // optional task id filter for the change history (blank = all changes)
    public String inputOptionalTaskId() {
        System.out.print("Enter Task ID to filter (blank = all): ");
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? null : input;
    }

    public String inputRoomId() {
        String input = "";
        while (input.trim().isEmpty()) {
            System.out.print("Enter Room ID: ");
            input = scanner.nextLine();
            if (input.trim().isEmpty())
                ConsoleUtil.printError("Room ID cannot be empty!");
        }
        return input.trim();
    }

    public String inputAssignmentStatus() {
        String input = "";
        while (input.trim().isEmpty()) {
            System.out.print("Enter assignment status (e.g. Pending, In Progress, Completed, Cancelled): ");
            input = scanner.nextLine();
            if (input.trim().isEmpty())
                ConsoleUtil.printError("Assignment status cannot be empty!");
        }
        return input.trim();
    }

    public String inputWorkerAssignmentStatus() {
        System.out.println("\nSelect worker assignment status:");
        System.out.println("  1. In Progress");
        System.out.println("  2. Completed");
        System.out.println("  3. Cancelled (drop / decline)");
        System.out.println("  4. Handed Off (shift change)");
        System.out.println("  5. Paused");
        System.out.println("  6. Work Finished (awaiting inspection)");
        System.out.println("  7. Inspected (passed inspection)");
        int choice = inputIntChoice("Enter status", 1, 7);
        switch (choice) {
            case 1: return "In Progress";
            case 2: return "Completed";
            case 3: return "Cancelled";
            case 4: return "Handed Off";
            case 5: return "Paused";
            case 6: return "Work Finished";
            default: return "Inspected";
        }
    }

    public LocalDateTime inputDateAssigned() {
        return inputDateTimeWithQuickOptions("Date & Time Assigned");
    }

    public LocalDateTime inputCheckoutDateTime() {
        return inputDateTimeWithQuickOptions("Guest Checkout Date & Time");
    }

    // -------------------- QUICK DATE-TIME INPUT --------------------

    private LocalDateTime inputDateTimeWithQuickOptions(String prompt) {
        while (true) {
            System.out.println("\n========================================");
            System.out.println("  " + prompt.toUpperCase());
            System.out.println("========================================");
            System.out.println("  1. Now");
            System.out.println("  2. 30 Minutes Later");
            System.out.println("  3. 1 Hour Later");
            System.out.println("  4. 2 Hours Later");
            System.out.println("  5. Next Shift Start (08:00)");
            System.out.println("  6. Custom (type manually)");
            System.out.println("========================================");
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
        return shift.isAfter(LocalDateTime.now()) ? shift : shift.plusDays(1);
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

    // DISPLAY / OUTPUT METHODS (staff)
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

        System.out.println("\n--- Details ---");
        for (String[] pair : details) {
            System.out.println(String.format("%-" + (keyWidth + 3) + "s: %s",
                    pair[0] == null ? "" : pair[0],
                    pair.length > 1 && pair[1] != null ? pair[1] : "-"));
        }
        System.out.println("----------------------------");
    }

    public void printStaffDetails(Staff staff) {
        printDetails(new String[][] {
            {"Staff ID", staff.getStaffId()},
            {"Staff Name", staff.getStaffName()},
            {"Department", staff.getDepartment()},
            {"Staff Role", staff.getStaffRole()},
            {"Availability", staff.getAvailabilityStatus()}
        });
    }

    public void listAllStaffs(String[][] data) {
        System.out.println("\n--- Staff List ---");
        if (data.length <= 1) {
            System.out.println("  No staff records found.");
            return;
        }
        String[] header = data[0];
        String[][] rows = new String[data.length - 1][];
        System.arraycopy(data, 1, rows, 0, rows.length);
        TablePrinter.displayTable(header, rows);
    }

    public void printStaffId(String staffId) {
        ConsoleUtil.printSuccess("New Staff ID: " + staffId);
    }

    // DISPLAY / OUTPUT METHODS (task)
    public void printTaskDetails(Task task) {
        printDetails(new String[][] {
            {"Task ID", task.getTaskId()},
            {"Task Name", task.getTaskName()},
            {"Task Type", task.getTaskType()},
            {"Current Status", task.getTaskStatus() == null ? "-" : task.getTaskStatus().name()},
            {"Priority", task.getTaskPriority().name()},
            {"Start Date & Time", String.valueOf(task.getStartDateTime())},
            {"Room ID", task.getRoomId() == null ? "-" : task.getRoomId()}
        });
    }

    public void listAllTasks(String[][] data) {
        System.out.println("\n--- Task List ---");
        if (data.length <= 1) {
            System.out.println("  No task records found.");
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

    // DISPLAY / OUTPUT METHODS (assignment)
    public void printAssignmentDetails(TaskAssignment assignment, Staff staff, Task task) {
        printDetails(new String[][] {
            {"Assignment ID", assignment.getTaskAssignmentId()},
            {"Status", assignment.getStatus()},
            {"Date & Time Assigned", String.valueOf(assignment.getDateTimeAssigned())},
            {"Staff ID", staff == null ? "-" : staff.getStaffId()},
            {"Staff Name", staff == null ? "-" : staff.getStaffName()},
            {"Task ID", task == null ? "-" : task.getTaskId()},
            {"Task Name", task == null ? "-" : task.getTaskName()},
            {"Room ID", task == null || task.getRoomId() == null ? "-" : task.getRoomId()}
        });
    }

    public void listAllAssignments(String[][] data) {
        System.out.println("\n--- Assignment List ---");
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
        System.out.println("\n--- Change History ---");
        if (data.length <= 1) {
            System.out.println("  No change records found.");
            return;
        }
        String[] header = data[0];
        String[][] rows = new String[data.length - 1][];
        System.arraycopy(data, 1, rows, 0, rows.length);
        TablePrinter.displayTable(header, rows);
    }

    public void printGuestCheckoutTask(Task task, Staff staff, TaskAssignment assignment, boolean deferred) {
        printDetails(new String[][] {
            {"Room ID", task.getRoomId()},
            {"Task ID", task.getTaskId()},
            {"Task Name", task.getTaskName()},
            {"Current Status", String.valueOf(task.getTaskStatus())},
            {"Priority", task.getTaskPriority().name()},
            {"Assigned Staff", staff == null ? "-" : staff.getStaffId() + " (" + staff.getStaffName() + ")"},
            {"Assignment ID", assignment == null ? "-" : assignment.getTaskAssignmentId()},
            {"Assignment Status", assignment == null ? "-" : assignment.getStatus()},
            {"Date Assigned", assignment == null || assignment.getDateTimeAssigned() == null ? "-"
                    : String.valueOf(assignment.getDateTimeAssigned())},
            {"Scheduled Start", String.valueOf(task.getStartDateTime())},
            {"Scheduled End", String.valueOf(task.getStartDateTime().plusMinutes(60))}
        });
        if (deferred) {
            ConsoleUtil.printWarning("Note: No staff was free at checkout time.");
            System.out.println("  Task was scheduled into the first free");
            System.out.println("  60-minute slot on the staff timetable.");
        }
    }

    public void printAssignmentId(String assignmentId) {
        ConsoleUtil.printSuccess("New Assignment ID: " + assignmentId);
    }

    // MESSAGE METHODS
    public void printSuccess() {
        ConsoleUtil.printSuccess("Operation successful!");
    }

    public void printNotFound() {
        ConsoleUtil.printError("Record not found!");
    }

    public void printDuplicateName() {
        ConsoleUtil.printError("Name already exists!");
    }

    public void printStaffNotFound() {
        ConsoleUtil.printError("Staff not found!");
    }

    public void printTaskNotFound() {
        ConsoleUtil.printError("Task not found!");
    }

    public void printStaffUnavailable() {
        ConsoleUtil.printError("Staff is not available (Resigned)!");
    }

    public void printWindowOverlap() {
        ConsoleUtil.printError("Staff already has another task in this 60-minute window!");
        ConsoleUtil.printError("A staff can only take the next task after the current one is done.");
    }

    public void printTaskAlreadyExists() {
        ConsoleUtil.printError("A cleaning task for this room already exists!");
    }

    public void printAllWorkersDoneHint(String assignmentId) {
        ConsoleUtil.printSuccess("All workers of this task are done.");
        System.out.println("    The task stays active under inspection until a supervisor");
        System.out.println("    gives final approval (use 'Update Task Status' to complete it).");
    }

    public void printReassigned() {
        ConsoleUtil.printSuccess("Assignment closed and task auto-reassigned.");
        System.out.println("    The task is scheduled into the first free 60-minute slot");
        System.out.println("    on the next available staff's timetable.");
    }

    public void printNoStaffFreeForTask() {
        ConsoleUtil.printError("No Housekeeping staff is free for this task's 60-minute window!");
        ConsoleUtil.printError("Try another task, or cancel / complete an overlapping task first.");
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

    public void printNoRecords() {
        ConsoleUtil.printError("No records to view!");
    }

    // view flow prompt: which row of the current page to open (0 = cancel)
    public int inputListIndex(String entityLabel, int max) {
        return inputIntChoice("Enter " + entityLabel + " number (0 = cancel)", 0, max);
    }

    // STAFF PICKER (manual assignment / reassign): lists the eligible staff
    // (built by the controller) as numbered options; returns the choice,
    // 0 = cancel
    public int printEligibleStaffMenu(LinkedListInterface<Staff> staffList) {
        System.out.println("\n--- Select Staff to Assign ---");
        if (staffList.isEmpty()) {
            System.out.println("  (no eligible staff)");
        } else {
            String[] header = new String[] { "No.", "Staff ID", "Name", "Department", "Availability" };
            String[][] rows = new String[staffList.size()][5];
            for (int i = 0; i < staffList.size(); i++) {
                Staff staff = staffList.get(i);
                rows[i] = new String[] {
                    String.valueOf(i + 1), staff.getStaffId(), staff.getStaffName(),
                    staff.getDepartment(), staff.getAvailabilityStatus()
                };
            }
            TablePrinter.displayTable(header, rows);
        }
        System.out.println("  0. Cancel");
        return inputIntChoice("Enter choice", 0, staffList.size());
    }

    public void printExitMessage() {
        System.out.println("\n  Exiting Housekeeping Module. Goodbye!");
    }

    public void printInvalidChoice() {
        ConsoleUtil.printError("Invalid choice! Please try again.");
    }

    // HELPER METHODS
    private int inputIntChoice(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt + " (" + min + "-" + max + "): ");
            String line = scanner.nextLine();
            if (line.trim().isEmpty()) {
                // ignore a bare Enter: re-prompt in place, no error, no advance
                if (Ansi.ENABLED) {
                    System.out.print("\u001B[1A\u001B[2K");
                }
                continue;
            }
            try {
                int value = Integer.parseInt(line.trim());
                if (value >= min && value <= max) {
                    System.out.println();
                    return value;
                }
            } catch (NumberFormatException e) {
                // fall through to the range error below
            }
            ConsoleUtil.printError("Please enter a number between " + min + " and " + max + "!");
        }
    }

    // clears the console screen (delegates to ConsoleUtil)
    private void clearScreen() {
        ConsoleUtil.clearScreen();
    }

    public void pressEnterToContinue() {
        ConsoleUtil.pressEnterToContinue(scanner);
    }
}