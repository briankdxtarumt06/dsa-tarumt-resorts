package tarumtresort.boundary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;
import tarumtresort.entity.Staff;
import tarumtresort.entity.Task;
import tarumtresort.entity.TaskAssignment;
import tarumtresort.entity.enums.RoomType;
import tarumtresort.entity.enums.TaskPriority;
import tarumtresort.entity.enums.TaskStatus;
import tarumtresort.report.ReportResult;

/**
 *
 * @author Brian
 *
 * Combined UI for the Housekeeping module (Staff + Task + Assignment).
 * All table / detail output is delegated to the shared TablePrinter.
 */
public class HousekeepingUI {

    private Scanner scanner = new Scanner(System.in);

    public static final String[] DEPARTMENTS = {"Finance", "Housekeeping", "Maintenance", "Front Office"};
    public static final String[] STAFF_ROLES = {"Manager", "Supervisor", "Cleaner", "Technician", "Receptionist", "Admin"};
    public static final String[] AVAILABILITY_STATUSES = {"Available", "Unavailable", "On Leave", "Resigned"};
    public static final RoomType[] ROOM_TYPES = {RoomType.STANDARD, RoomType.DELUXE, RoomType.SUITE};

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

    // STAFF SUB-MENU
    public int getStaffMenuChoice() {
        System.out.println("\n========================================");
        System.out.println("  STAFF MANAGEMENT");
        System.out.println("========================================");
        System.out.println("  1. Add Staff");
        System.out.println("  2. View All Staff");
        System.out.println("  3. Search Staff");
        System.out.println("  4. Update Staff");
        System.out.println("  5. Remove Staff (Resign)");
        System.out.println("  6. Filter Staff by Department");
        System.out.println("  7. Filter Staff by Availability");
        System.out.println("  0. Back");
        System.out.println("========================================");
        return inputIntChoice("Enter choice", 0, 7);
    }

    public int getStaffSearchMenuChoice() {
        System.out.println("\n---- SEARCH STAFF ----");
        System.out.println("  1. Search by Staff ID");
        System.out.println("  2. Search by Staff Name");
        System.out.println("  0. Back");
        return inputIntChoice("Enter choice", 0, 2);
    }

    // TASK SUB-MENU
    public int getTaskMenuChoice() {
        System.out.println("\n========================================");
        System.out.println("  TASK MANAGEMENT");
        System.out.println("========================================");
        System.out.println("  1. Add Task");
        System.out.println("  2. View All Tasks");
        System.out.println("  3. Search Task");
        System.out.println("  4. Update Task");
        System.out.println("  5. Update Task Status");
        System.out.println("  6. Remove Task");
        System.out.println("  7. Filter Tasks by Priority");
        System.out.println("  8. Filter Tasks by Type");
        System.out.println("  0. Back");
        System.out.println("========================================");
        return inputIntChoice("Enter choice", 0, 8);
    }

    public int getTaskSearchMenuChoice() {
        System.out.println("\n---- SEARCH TASK ----");
        System.out.println("  1. Search by Task ID");
        System.out.println("  2. Search by Task Name");
        System.out.println("  0. Back");
        return inputIntChoice("Enter choice", 0, 2);
    }

    // ASSIGNMENT SUB-MENU
    public int getAssignmentMenuChoice() {
        System.out.println("\n========================================");
        System.out.println("  ASSIGNMENTS & SCHEDULING");
        System.out.println("========================================");
        System.out.println("  1. Assign Staff to Task");
        System.out.println("  2. View All Assignments");
        System.out.println("  3. Search Assignment");
        System.out.println("  4. Update Assignment");
        System.out.println("  5. Assign Task to Room");
        System.out.println("  6. View Tasks by Room");
        System.out.println("  7. Simulate Guest Checkout");
        System.out.println("  8. Update Assignment Status (Worker)");
        System.out.println("  9. Update Task Status (Tracked)");
        System.out.println("  10. View Change History");
        System.out.println("  11. Guest Cleaning Request");
        System.out.println("  0. Back");
        System.out.println("========================================");
        return inputIntChoice("Enter choice", 0, 11);
    }

    public int getAssignmentSearchMenuChoice() {
        System.out.println("\n---- SEARCH ASSIGNMENT ----");
        System.out.println("  1. Search by Assignment ID");
        System.out.println("  2. Search by Staff ID");
        System.out.println("  3. Search by Task ID");
        System.out.println("  0. Back");
        return inputIntChoice("Enter choice", 0, 3);
    }

    // REPORTS SUB-MENU
    public int getReportMenuChoice() {
        System.out.println("\n========================================");
        System.out.println("  REPORTS");
        System.out.println("========================================");
        System.out.println("  1. Room Cleaning Report (Room + Staff + Task)");
        System.out.println("  2. Staff Workload Report (Staff + Task + Assignment)");
        System.out.println("  0. Back");
        System.out.println("========================================");
        return inputIntChoice("Enter choice", 0, 2);
    }

    // INPUT METHODS (staff)
    public String inputStaffId() {
        String input = "";
        while (input.trim().isEmpty()) {
            System.out.print("Enter Staff ID: ");
            input = scanner.nextLine();
            if (input.trim().isEmpty())
                System.out.println("  ✗ Staff ID cannot be empty!");
        }
        return input.trim();
    }

    public String inputStaffName() {
        String input = "";
        while (input.trim().isEmpty()) {
            System.out.print("Enter Staff Name: ");
            input = scanner.nextLine();
            if (input.trim().isEmpty())
                System.out.println("  ✗ Staff Name cannot be empty!");
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
                System.out.println("  ✗ Task ID cannot be empty!");
        }
        return input.trim();
    }

    public String inputTaskName() {
        String input = "";
        while (input.trim().isEmpty()) {
            System.out.print("Enter Task Name: ");
            input = scanner.nextLine();
            if (input.trim().isEmpty())
                System.out.println("  ✗ Task Name cannot be empty!");
        }
        return input.trim();
    }

    public String inputTaskType() {
        String input = "";
        while (input.trim().isEmpty()) {
            System.out.print("Enter Task Type (e.g. Housekeeping, Maintenance): ");
            input = scanner.nextLine();
            if (input.trim().isEmpty())
                System.out.println("  ✗ Task Type cannot be empty!");
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
        LocalDateTime dateTime = null;
        while (dateTime == null) {
            System.out.print("Enter start date & time (yyyy-MM-dd HH:mm): ");
            String input = scanner.nextLine();
            try {
                dateTime = LocalDateTime.parse(input, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            } catch (DateTimeParseException e) {
                System.out.println("  ✗ Invalid date & time format! Please use yyyy-MM-dd HH:mm.");
            }
        }
        return dateTime;
    }

    public LocalDateTime parseDateTime(String value) {
        return LocalDateTime.parse(value);
    }

    public String inputTaskStatus() {
        System.out.println("\nSelect Task Status:");
        TaskStatus[] statuses = TaskStatus.values();
        for (int i = 0; i < statuses.length; i++) {
            System.out.println("  " + (i + 1) + ". " + statuses[i].name());
        }
        return statuses[inputIntChoice("Enter task status", 1, statuses.length) - 1].name();
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
                System.out.println("  ✗ Assignment ID cannot be empty!");
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
                System.out.println("  ✗ Room ID cannot be empty!");
        }
        return input.trim();
    }

    public String inputAssignmentStatus() {
        String input = "";
        while (input.trim().isEmpty()) {
            System.out.print("Enter assignment status (e.g. Pending, In Progress, Completed, Cancelled): ");
            input = scanner.nextLine();
            if (input.trim().isEmpty())
                System.out.println("  ✗ Assignment status cannot be empty!");
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
        int choice = inputIntChoice("Enter status", 1, 6);
        switch (choice) {
            case 1: return "In Progress";
            case 2: return "Completed";
            case 3: return "Cancelled";
            case 4: return "Handed Off";
            case 5: return "Paused";
            default: return "Work Finished";
        }
    }

    public LocalDateTime inputDateAssigned() {
        LocalDateTime dateTime = null;
        while (dateTime == null) {
            System.out.print("Enter date & time assigned (yyyy-MM-dd HH:mm): ");
            String input = scanner.nextLine();
            try {
                dateTime = LocalDateTime.parse(input, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            } catch (DateTimeParseException e) {
                System.out.println("  ✗ Invalid date & time format! Please use yyyy-MM-dd HH:mm.");
            }
        }
        return dateTime;
    }

    public LocalDateTime inputCheckoutDateTime() {
        LocalDateTime dateTime = null;
        while (dateTime == null) {
            System.out.print("Enter guest checkout date & time (yyyy-MM-dd HH:mm): ");
            String input = scanner.nextLine();
            try {
                dateTime = LocalDateTime.parse(input, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            } catch (DateTimeParseException e) {
                System.out.println("  ✗ Invalid date & time format! Please use yyyy-MM-dd HH:mm.");
            }
        }
        return dateTime;
    }

    // REPORT INPUT METHODS
    // returns [from, to]; either side may be null when left blank (no bound)
    public LocalDateTime[] inputOptionalDateTimeRange(String fieldLabel) {
        System.out.println("\n--- Date & Time Range (blank = no limit) ---");
        LocalDateTime from = inputOptionalDateTime("Enter " + fieldLabel + " FROM (yyyy-MM-dd HH:mm)");
        LocalDateTime to = inputOptionalDateTime("Enter " + fieldLabel + " TO (yyyy-MM-dd HH:mm)");
        return new LocalDateTime[] { from, to };
    }

    public LocalDateTime inputOptionalDateTime(String prompt) {
        System.out.print(prompt + ": ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(input, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (DateTimeParseException e) {
            System.out.println("  ✗ Invalid format! Please use yyyy-MM-dd HH:mm (blank = no limit).");
            return inputOptionalDateTime(prompt);
        }
    }

    // null = all roles
    public String inputOptionalStaffRole() {
        System.out.println("\nSelect Staff Role (0 = All Roles):");
        for (int i = 0; i < STAFF_ROLES.length; i++) {
            System.out.println("  " + (i + 1) + ". " + STAFF_ROLES[i]);
        }
        int choice = inputIntChoice("Enter staff role", 0, STAFF_ROLES.length);
        return choice == 0 ? null : STAFF_ROLES[choice - 1];
    }

    // null = all room types
    public RoomType inputOptionalRoomType() {
        System.out.println("\nSelect Room Type (0 = All Types):");
        for (int i = 0; i < ROOM_TYPES.length; i++) {
            System.out.println("  " + (i + 1) + ". " + ROOM_TYPES[i].name());
        }
        int choice = inputIntChoice("Enter room type", 0, ROOM_TYPES.length);
        return choice == 0 ? null : ROOM_TYPES[choice - 1];
    }

    // null = all departments
    public String inputOptionalDepartment() {
        System.out.println("\nSelect Department (0 = All Departments):");
        for (int i = 0; i < DEPARTMENTS.length; i++) {
            System.out.println("  " + (i + 1) + ". " + DEPARTMENTS[i]);
        }
        int choice = inputIntChoice("Enter department", 0, DEPARTMENTS.length);
        return choice == 0 ? null : DEPARTMENTS[choice - 1];
    }

    // DISPLAY / OUTPUT METHODS (staff)
    public void printStaffDetails(Staff staff) {
        System.out.println("\n--- Staff Details ---");
        System.out.println("Staff ID          : " + staff.getStaffId());
        System.out.println("Staff Name        : " + staff.getStaffName());
        System.out.println("Department        : " + staff.getDepartment());
        System.out.println("Staff Role        : " + staff.getStaffRole());
        System.out.println("Availability      : " + staff.getAvailabilityStatus());
        System.out.println("----------------------------");
    }

    public void listAllStaffs(String[][] data) {
        System.out.println("\n--- Staff List ---");
        if (data.length <= 1) {
            System.out.println("  No staff records found.");
            return;
        }
        TablePrinter.printTable(data);
    }

    public void printStaffId(String staffId) {
        System.out.println("\n  New Staff ID: " + staffId);
    }

    // DISPLAY / OUTPUT METHODS (task)
    public void printTaskDetails(Task task) {
        System.out.println("\n--- Task Details ---");
        System.out.println("Task ID          : " + task.getTaskId());
        System.out.println("Task Name        : " + task.getTaskName());
        System.out.println("Task Type        : " + task.getTaskType());
        System.out.println("Current Status   : " + (task.getTaskStatus() == null ? "-" : task.getTaskStatus()));
        System.out.println("Priority         : " + task.getTaskPriority());
        System.out.println("Start Date & Time: " + task.getStartDateTime());
        System.out.println("Room ID          : " + (task.getRoomId() == null ? "-" : task.getRoomId()));
        System.out.println("----------------------------");
    }

    public void listAllTasks(String[][] data) {
        System.out.println("\n--- Task List ---");
        if (data.length <= 1) {
            System.out.println("  No task records found.");
            return;
        }
        TablePrinter.printTable(data);
    }

    public void printTaskId(String taskId) {
        System.out.println("\n  New Task ID: " + taskId);
    }

    // DISPLAY / OUTPUT METHODS (assignment)
    public void printAssignmentDetails(TaskAssignment assignment, Staff staff, Task task) {
        System.out.println("\n--- Assignment Details ---");
        System.out.println("Assignment ID      : " + assignment.getTaskAssignmentId());
        System.out.println("Status             : " + assignment.getStatus());
        System.out.println("Date & Time Assigned: " + assignment.getDateTimeAssigned());
        System.out.println("Staff ID           : " + (staff == null ? "-" : staff.getStaffId()));
        System.out.println("Staff Name         : " + (staff == null ? "-" : staff.getStaffName()));
        System.out.println("Task ID            : " + (task == null ? "-" : task.getTaskId()));
        System.out.println("Task Name          : " + (task == null ? "-" : task.getTaskName()));
        System.out.println("Room ID            : " + (task == null || task.getRoomId() == null ? "-" : task.getRoomId()));
        System.out.println("----------------------------");
    }

    public void listAllAssignments(String[][] data) {
        System.out.println("\n--- Assignment List ---");
        if (data.length <= 1) {
            System.out.println("  No assignment records found.");
            return;
        }
        TablePrinter.printTable(data);
    }

    public void listAllChanges(String[][] data) {
        System.out.println("\n--- Change History ---");
        if (data.length <= 1) {
            System.out.println("  No change records found.");
            return;
        }
        TablePrinter.printTable(data);
    }

    public void printReport(ReportResult result) {
        System.out.println("\n--- Report ---");
        if (result.isEmpty()) {
            System.out.println("  No records match the given filters.");
            return;
        }
        TablePrinter.printTable(result.getTable());
        TablePrinter.printSummary(result.getSummary());
    }

    public void printGuestCheckoutTask(Task task, Staff staff, boolean deferred) {
        System.out.println("\n--- Guest Checkout Processed ---");
        System.out.println("Room ID           : " + task.getRoomId());
        System.out.println("Task ID           : " + task.getTaskId());
        System.out.println("Task Name         : " + task.getTaskName());
        System.out.println("Current Status    : " + task.getTaskStatus());
        System.out.println("Priority          : " + task.getTaskPriority());
        System.out.println("Assigned Staff    : " + (staff == null ? "-" : staff.getStaffId() + " (" + staff.getStaffName() + ")"));
        System.out.println("Scheduled Start   : " + task.getStartDateTime());
        System.out.println("Scheduled End     : " + task.getStartDateTime().plusMinutes(60));
        if (deferred) {
            System.out.println("Note              : No staff was free at checkout time.");
            System.out.println("                    Task was scheduled into the first free");
            System.out.println("                    60-minute slot on the staff timetable.");
        }
        System.out.println("----------------------------");
    }

    public void printAssignmentId(String assignmentId) {
        System.out.println("\n  New Assignment ID: " + assignmentId);
    }

    // MESSAGE METHODS
    public void printSuccess() {
        System.out.println("\n  ✓ Operation successful!");
    }

    public void printNotFound() {
        System.out.println("\n  ✗ Record not found!");
    }

    public void printDuplicateName() {
        System.out.println("\n  ✗ Name already exists!");
    }

    public void printStaffNotFound() {
        System.out.println("\n  ✗ Staff not found!");
    }

    public void printTaskNotFound() {
        System.out.println("\n  ✗ Task not found!");
    }

    public void printStaffUnavailable() {
        System.out.println("\n  ✗ Staff is not available (Resigned)!");
    }

    public void printWindowOverlap() {
        System.out.println("\n  ✗ Staff already has another task in this 60-minute window!");
        System.out.println("    A staff can only take the next task after the current one is done.");
    }

    public void printTaskAlreadyExists() {
        System.out.println("\n  ✗ A cleaning task for this room already exists!");
    }

    public void printAllWorkersDoneHint(String assignmentId) {
        System.out.println("\n  ✓ All workers of this task are done.");
        System.out.println("    The task stays active under inspection until a supervisor");
        System.out.println("    gives final approval (use 'Update Task Status' to complete it).");
    }

    public void printReassigned() {
        System.out.println("\n  ✓ Assignment closed and task auto-reassigned.");
        System.out.println("    The task is scheduled into the first free 60-minute slot");
        System.out.println("    on the next available staff's timetable.");
    }

    public void printExitMessage() {
        System.out.println("\n  Exiting Housekeeping Module. Goodbye!");
    }

    public void printInvalidChoice() {
        System.out.println("\n  ✗ Invalid choice! Please try again.");
    }

    // HELPER METHODS
    private int inputIntChoice(String prompt, int min, int max) {
        int choice = -1;
        while (choice < min || choice > max) {
            System.out.print(prompt + " (" + min + "-" + max + "): ");
            if (scanner.hasNextInt()) {
                choice = scanner.nextInt();
                scanner.nextLine();
                if (choice < min || choice > max)
                    System.out.println("  ✗ Please enter a number between " + min + " and " + max + "!");
            } else {
                System.out.println("  ✗ Invalid input! Please enter a number.");
                scanner.nextLine();
            }
        }
        System.out.println();
        return choice;
    }

    public void pressEnterToContinue() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }
}