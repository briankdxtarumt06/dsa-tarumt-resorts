package tarumtresort.boundary;

import java.util.Scanner;
import tarumtresort.entity.Staff;

/**
 *
 * @author Brian
 */
public class StaffManagementUI {

    private Scanner scanner = new Scanner(System.in);

    public StaffManagementUI() {
    }

    public StaffManagementUI(Scanner scanner) {
        this.scanner = scanner;
    }

    public static final String[] DEPARTMENTS = {"Finance", "Housekeeping", "Maintenance", "Front Office"};
    public static final String[] STAFF_ROLES = {"Manager", "Supervisor", "Cleaner", "Technician", "Receptionist", "Admin"};
    public static final String[] AVAILABILITY_STATUSES = {"Available", "Unavailable", "On Leave", "Resigned"};

    // MENU
    public int getMenuChoice() {
        System.out.println("\n========================================");
        System.out.println("  STAFF MANAGEMENT MODULE");
        System.out.println("========================================");
        System.out.println("  1. Add Staff");
        System.out.println("  2. View All Staff");
        System.out.println("  3. Search Staff");
        System.out.println("  4. Update Staff");
        System.out.println("  5. Remove Staff");
        System.out.println("  6. Filter Staff by Department");
        System.out.println("  7. Filter Staff by Availability");
        System.out.println("  0. Exit");
        System.out.println("========================================");
        return inputIntChoice("Enter choice", 0, 7);
    }

    public int getSearchMenuChoice() {
        System.out.println("\n---- SEARCH STAFF ----");
        System.out.println("  1. Search by Staff ID");
        System.out.println("  2. Search by Staff Name");
        System.out.println("  0. Back");
        return inputIntChoice("Enter choice", 0, 2);
    }

    // INPUT METHODS
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

    // DISPLAY / OUTPUT METHODS
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
        printSimpleTable(data);
    }

    public void printStaffId(String staffId) {
        System.out.println("\n  New Staff ID: " + staffId);
    }

    // MESSAGE METHODS
    public void printSuccess() {
        System.out.println("\n  ✓ Operation successful!");
    }

    public void printNotFound() {
        System.out.println("\n  ✗ Record not found!");
    }

    public void printDuplicateName() {
        System.out.println("\n  ✗ Staff name already exists!");
    }

    public void printExitMessage() {
        System.out.println("\n  Exiting Staff Management Module. Goodbye!");
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