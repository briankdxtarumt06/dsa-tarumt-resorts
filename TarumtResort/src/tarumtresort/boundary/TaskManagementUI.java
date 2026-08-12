package tarumtresort.boundary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;
import tarumtresort.entity.Task;
import tarumtresort.entity.enums.TaskPriority;

/**
 *
 * @author Brian
 */
public class TaskManagementUI {

    private Scanner scanner = new Scanner(System.in);

    public TaskManagementUI() {
    }

    public TaskManagementUI(Scanner scanner) {
        this.scanner = scanner;
    }

    // MENU
    public int getMenuChoice() {
        System.out.println("\n========================================");
        System.out.println("  TASK MANAGEMENT MODULE");
        System.out.println("========================================");
        System.out.println("  1. Add Task");
        System.out.println("  2. View All Tasks");
        System.out.println("  3. Search Task");
        System.out.println("  4. Update Task");
        System.out.println("  5. Update Task Status");
        System.out.println("  6. Roll Back Task Status");
        System.out.println("  7. Remove Task");
        System.out.println("  8. Filter Tasks by Priority");
        System.out.println("  9. Filter Tasks by Type");
        System.out.println("  0. Exit");
        System.out.println("========================================");
        return inputIntChoice("Enter choice", 0, 9);
    }

    public int getSearchMenuChoice() {
        System.out.println("\n---- SEARCH TASK ----");
        System.out.println("  1. Search by Task ID");
        System.out.println("  2. Search by Task Name");
        System.out.println("  0. Back");
        return inputIntChoice("Enter choice", 0, 2);
    }

    // INPUT METHODS
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

    public LocalDateTime parseDateTime(String value) {
        return LocalDateTime.parse(value);
    }

    // DISPLAY / OUTPUT METHODS
    public void printTaskDetails(Task task) {
        System.out.println("\n--- Task Details ---");
        System.out.println("Task ID          : " + task.getTaskId());
        System.out.println("Task Name        : " + task.getTaskName());
        System.out.println("Task Type        : " + task.getTaskType());
        System.out.println("Current Status   : " + (task.peekTaskStatus() == null ? "-" : task.peekTaskStatus()));
        System.out.println("Priority         : " + task.getTaskPriority());
        System.out.println("Start Date & Time: " + task.getStartDateTime());
        System.out.println("----------------------------");
    }

    public void listAllTasks(String[][] data) {
        System.out.println("\n--- Task List ---");
        if (data.length <= 1) {
            System.out.println("  No task records found.");
            return;
        }
        printSimpleTable(data);
    }

    public void printTaskId(String taskId) {
        System.out.println("\n  New Task ID: " + taskId);
    }

    // MESSAGE METHODS
    public void printSuccess() {
        System.out.println("\n  ✓ Operation successful!");
    }

    public void printNotFound() {
        System.out.println("\n  ✗ Record not found!");
    }

    public void printDuplicateName() {
        System.out.println("\n  ✗ Task name already exists!");
    }

    public void printNoStatusToRollBack() {
        System.out.println("\n  ✗ No task status to roll back!");
    }

    public void printExitMessage() {
        System.out.println("\n  Exiting Task Management Module. Goodbye!");
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