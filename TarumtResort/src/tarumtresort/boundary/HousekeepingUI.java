package tarumtresort.boundary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;
import tarumtresort.entity.TaskAssignment;

/**
 *
 * @author Brian
 */
public class HousekeepingUI {

    private Scanner scanner = new Scanner(System.in);

    public HousekeepingUI() {
    }

    public HousekeepingUI(Scanner scanner) {
        this.scanner = scanner;
    }

    // MENU
    public int getMenuChoice() {
        System.out.println("\n========================================");
        System.out.println("  HOUSEKEEPING MODULE");
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
        System.out.println("  0. Exit");
        System.out.println("========================================");
        return inputIntChoice("Enter choice", 0, 10);
    }

    public int getSearchMenuChoice() {
        System.out.println("\n---- SEARCH ASSIGNMENT ----");
        System.out.println("  1. Search by Assignment ID");
        System.out.println("  2. Search by Staff ID");
        System.out.println("  3. Search by Task ID");
        System.out.println("  0. Back");
        return inputIntChoice("Enter choice", 0, 3);
    }

    // INPUT METHODS
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

    public String inputTaskStatus() {
        String input = "";
        while (input.trim().isEmpty()) {
            System.out.print("Enter task status (e.g. Pending, In Progress, Completed): ");
            input = scanner.nextLine();
            if (input.trim().isEmpty())
                System.out.println("  ✗ Task status cannot be empty!");
        }
        return input.trim();
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

    // DISPLAY / OUTPUT METHODS
    public void printAssignmentDetails(TaskAssignment assignment) {
        System.out.println("\n--- Assignment Details ---");
        System.out.println("Assignment ID      : " + assignment.getTaskAssignmentId());
        System.out.println("Status             : " + assignment.getStatus());
        System.out.println("Date & Time Assigned: " + assignment.getDateTimeAssigned());
        System.out.println("Staff ID           : " + (assignment.getAssingedStaff() == null ? "-" : assignment.getAssingedStaff().getStaffId()));
        System.out.println("Staff Name         : " + (assignment.getAssingedStaff() == null ? "-" : assignment.getAssingedStaff().getStaffName()));
        System.out.println("Task ID            : " + (assignment.getAssignedTask() == null ? "-" : assignment.getAssignedTask().getTaskId()));
        System.out.println("Task Name          : " + (assignment.getAssignedTask() == null ? "-" : assignment.getAssignedTask().getTaskName()));
        System.out.println("Room ID            : " + (assignment.getAssignedTask() == null ? "-" : assignment.getAssignedTask().getRoomId() == null ? "-" : assignment.getAssignedTask().getRoomId()));
        System.out.println("----------------------------");
    }

    public void listAllAssignments(String[][] data) {
        System.out.println("\n--- Assignment List ---");
        if (data.length <= 1) {
            System.out.println("  No assignment records found.");
            return;
        }
        printSimpleTable(data);
    }

    public void listAllChanges(String[][] data) {
        System.out.println("\n--- Change History ---");
        if (data.length <= 1) {
            System.out.println("  No change records found.");
            return;
        }
        printSimpleTable(data);
    }

    public void listAllTasks(String[][] data) {
        System.out.println("\n--- Task List ---");
        if (data.length <= 1) {
            System.out.println("  No task records found.");
            return;
        }
        printSimpleTable(data);
    }

    public void printCheckoutResult(String taskId, String roomId, String staffLabel,
                                    LocalDateTime scheduledStart, LocalDateTime scheduledEnd, boolean deferred) {
        System.out.println("\n--- Guest Checkout Processed ---");
        System.out.println("Room ID           : " + roomId);
        System.out.println("Task ID           : " + taskId);
        System.out.println("Assigned Staff    : " + (staffLabel == null ? "-" : staffLabel));
        System.out.println("Scheduled Start   : " + scheduledStart);
        System.out.println("Scheduled End     : " + scheduledEnd);
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

    // simple println-based table
    private void printSimpleTable(String[][] data) {
        if (data.length == 0) return;

        int[] widths = new int[data[0].length];
        for (String[] row : data) {
            for (int col = 0; col < row.length; col++) {
                if (row[col] != null && row[col].length() > widths[col]) {
                    widths[col] = row[col].length();
                }
            }
        }

        for (int r = 0; r < data.length; r++) {
            StringBuilder sb = new StringBuilder();
            for (int col = 0; col < data[r].length; col++) {
                String cell = data[r][col] == null ? "" : data[r][col];
                sb.append(String.format("%-" + (widths[col] + 3) + "s", cell));
            }
            System.out.println(sb.toString());

            // underline after header row
            if (r == 0) {
                int totalWidth = 0;
                for (int w : widths) totalWidth += w + 3;
                StringBuilder line = new StringBuilder();
                for (int i = 0; i < totalWidth; i++) line.append('-');
                System.out.println(line.toString());
            }
        }
    }

    public void pressEnterToContinue() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }
}