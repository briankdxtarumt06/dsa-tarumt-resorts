package tarumtresort.boundary;

import java.util.Scanner;

import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.Room;
import tarumtresort.utility.ConsoleUtil;
import tarumtresort.utility.SharedServices;
import tarumtresort.utility.TablePrinter;

public class ReservationUI {

    Scanner scanner = new Scanner(System.in);

    public int printReservationListMenu(String currentListName, String[][] tableData, boolean hasFilter) {
        ConsoleUtil.clearScreen();
        System.out.println("\n==============================");
        System.out.println("  RESERVATION MANAGEMENT");
        System.out.println("==============================");

        System.out.println("  Currently viewing: " + currentListName);
        if (tableData.length <= 1) {
            System.out.println("  (No reservations in this list)");
        } else {
            String[] header = tableData[0];
            String[][] rows = new String[tableData.length - 1][];
            for (int i = 1; i < tableData.length; i++) {
                rows[i - 1] = tableData[i];
            }
            TablePrinter.displayTable(header, rows);
        }

        System.out.println("==========Actions==========");
        System.out.println("  1. View Booking List");
        System.out.println("  2. View Guest Queue");
        System.out.println("  3. View Assigned List");
        System.out.println("  4. Filter by Room Type");
        System.out.println("  5. Make New Reservation");
        System.out.println("  6. Guest Arrival");
        System.out.println("  7. Assign Room to Next Guest");
        System.out.println("  8. Check In");
        System.out.println("  9. Check Out");
        System.out.println("  10. Check Queue Position");
        System.out.println("  11. Cancel Reservation");
        if (hasFilter) {
            System.out.println("  12. Clear Filter");
            return inputIntChoice("Enter choice", 0, 12);
        }
        System.out.println("  0. Back to Main Menu");

        System.out.println("===========================");
        return inputIntChoice("Enter choice", 0, 11);
    }

    // MENU
    public int getMenuChoice() {
        ConsoleUtil.clearScreen();
        System.out.println("========================================");
        System.out.println("   WALK-IN REGISTRATION & BOOKING ");
        System.out.println("========================================");
        System.out.println("  1. Guest Management");
        System.out.println("  2. Reservation Management");
        System.out.println("  3. Room Management");
        System.out.println("  4. Reports");
        System.out.println("  0. Exit");
        System.out.println("========================================");
        return inputIntChoice("Enter choice", 0, 4);
    }

    // INPUTS 
    public int inputRoomTypeChoice() {
        System.out.println();
        System.out.println("==========Room Type==========");
        System.out.println("  1. STANDARD SINGLE");
        System.out.println("  2. STANDARD DOUBLE");
        System.out.println("  3. STANDARD TRIPLE");
        System.out.println("  4. DELUXE SINGLE");
        System.out.println("  5. DELUXE DOUBLE");
        System.out.println("  6. DELUXE TRIPLE");
        System.out.println("  7. SUITE");
        System.out.println("  0. Back");
        System.out.println("=============================");

        return inputIntChoice("Enter choice", 0, 7);
    }

    public int inputNumberOfGuests() {
        while (true) {
            System.out.print("Number of guests in room: ");
            try {
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value >= 1) {
                    return value;
                }
                ConsoleUtil.printWarning("Must be at least 1!");
            } catch (NumberFormatException e) {
                ConsoleUtil.printError("Invalid input! Please enter a number.");
            }
        }
    }

    public int inputNumberOfNights() {
        while (true) {
            System.out.print("Number of nights: ");
            try {
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value >= 1) {
                    return value;
                }
                ConsoleUtil.printWarning("Must be at least 1!");
            } catch (NumberFormatException e) {
                ConsoleUtil.printError("Invalid input! Please enter a number.");
            }
        }
    }

    public String inputDate(String prompt) {
        return SharedServices.askNonEmptyInput(scanner, prompt + " (YYYY-MM-DD)");
    }

    public String inputIcOrPassport(){
        return askNonEmptyInput("Enter guest's IC or Passport");
    }

    public String inputConfirmationNumber(){
        return SharedServices.askNonEmptyInput(scanner, "Enter confirmation number (0 = cancel)");
    }

    public int inputListIndex(String entityLabel, int max) {
        return inputIntChoice("Enter " + entityLabel + " number to select (0 = cancel)", 0, max);
    }

    public void printQueuePosition(String confirmationNumber, int position) {
        System.out.println("  Confirmation No. " + confirmationNumber + " → Position #" + position);
    }
    // OUTPUTS

    // PRINT TABLE
    // print reservation details aftre finish reservation 
    public void printReservationDetails(Reservation r) {
        String[] header = {"Field", "Value"};
        String[][] rows = {
            {"Conf. No.", r.getConfirmationNumber()},
            {"Room Type", r.getRoomTypeRequested().toString()},
            {"Guests", String.valueOf(r.getNumberOfGuests())},
            {"Nights", String.valueOf(r.getNumberOfNights())},
            {"Type", r.getReservationType().toString()},
            {"Status", r.getStatus().toString()},
            {"Expected Check-In", String.valueOf(r.getTimestamps().getExpectedCheckInDate())},
            {"Expected Check-Out", String.valueOf(r.getTimestamps().getExpectedCheckOutDate())},
            {"Checked In At", r.getTimestamps().getActualCheckInTime() != null ? r.getTimestamps().getActualCheckInTime().toString() : "-"},
            {"Checked Out At", r.getTimestamps().getActualCheckOutTime() != null ? r.getTimestamps().getActualCheckOutTime().toString() : "-"}
        };

        TablePrinter.displayTable(header, rows);
    }

    // single-record summary of a room assignment result
    public void printAssignmentSummary(Reservation r, Room room) {
        String[] header = {"Field", "Value"};
        String[][] rows = {
            {"Conf. No.", r.getConfirmationNumber()},
            {"Guest ID", r.getGuestId()},
            {"Room Number", room.getRoomNumber()},
            {"Room Type", r.getRoomTypeRequested().toString()},
            {"Status", r.getStatus().toString()},
            {"Assigned Time", r.getTimestamps().getAssignedTime() != null ? r.getTimestamps().getAssignedTime().toString() : "-"}
        };
        TablePrinter.displayTable(header, rows);
    }

    public void printReservationDetails(LinkedListInterface<Reservation> reservations) {
        if (reservations.size() == 0) {
            System.out.println("\nNo reservations found.");
            return;
        }

        String[] header = {
            "No.", "Conf. No.", "Room Type", "Guests", "Nights",
            "Type", "Status", "Expected Check-In", "Expected Check-Out"
        };

        String[][] rows = new String[reservations.size()][9];
        for (int i = 0; i < reservations.size(); i++) {
            Reservation r = reservations.get(i);
            rows[i] = new String[]{
                String.valueOf(i + 1),
                r.getConfirmationNumber(),
                r.getRoomTypeRequested().toString(),
                String.valueOf(r.getNumberOfGuests()),
                String.valueOf(r.getNumberOfNights()),
                r.getReservationType().toString(),
                r.getStatus().toString(),
                String.valueOf(r.getTimestamps().getExpectedCheckInDate()),
                String.valueOf(r.getTimestamps().getExpectedCheckOutDate())
            };
        }

        TablePrinter.displayTable(header, rows);
    }

    public void printWaitingQueueTable(String[][] data) {
        if (data.length <= 1) {
            System.out.println("  No reservations in queue.");
            return;
        }

        String[] header = data[0];
        String[][] rows = new String[data.length - 1][];
        for (int i = 1; i < data.length; i++) {
            rows[i - 1] = data[i];
        }

        TablePrinter.displayTable(header, rows);
        System.out.println("Total: " + (data.length - 1) + " reservation(s)");
    }

    public void printBookingListForArrival(String[][] data) {
        if (data.length <= 1) {
            System.out.println("\nNo advance bookings found.");
            return;
        }

        String[] header = data[0];
        String[][] rows = new String[data.length - 1][];
        for (int i = 1; i < data.length; i++) {
            rows[i - 1] = data[i];
        }

        System.out.println("\nAdvance Bookings (soonest check-in first):");
        TablePrinter.displayTable(header, rows);
    }

    // MESSAGE METHODS
    public void printSuccess() {
        ConsoleUtil.printSuccess("  Operation successful!");
    }

    public void printNotFound() {
        ConsoleUtil.printWarning("  Record not found!");
    }

    public void printCannotCheckIn() {
        ConsoleUtil.printError("  Cannot check in. Expected check-in date not yet reached!");
    }

    public void printRoomNotAvailable() {
        ConsoleUtil.printWarning("  No available room for the requested room type!");
    }

    public void printCancelled() {
        ConsoleUtil.printSuccess("  Reservation cancelled successfully!");
    }

    public void printExitMessage() {
        ConsoleUtil.printSuccess("  Exiting Walk-In Registration & Booking Module. Goodbye!");
    }

    public void printInvalidChoice() {
        ConsoleUtil.printError("  Invalid choice! Please try again.");
    }

    public void printError(String message) {
        ConsoleUtil.printError("  " + message);
    }

    public void printCannotCancel(){
        ConsoleUtil.printError(" Your reservation cannot be canceled.");
    }
    
    //others
    public int showSubMenu(String title, String[][] options) {

        System.out.println("\n==========" + title + "==========");

        for (int i = 0; i < options.length; i++) {
            System.out.println("  " + options[i][0] + ". " + options[i][1]);
        }

        System.out.println("===============================");

        int maxChoice = options.length - 1;
        return inputIntChoice("Enter choice", 0, maxChoice);
    }

    // HELPER METHODS
    private int inputIntChoice(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt + " (" + min + "-" + max + "): ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                ConsoleUtil.printError("Input cannot be empty! Please enter a number.");
                continue;
            }
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    System.out.println();
                    return value;
                }
            } catch (NumberFormatException e) {
                // retry
            }
            ConsoleUtil.printError("Please enter a number between " + min + " and " + max + "!");
        }
    }

    public boolean askConfirmation(String message, String yesMessage, String noMessage) {
        char userInput;
        boolean validInput;
        boolean result;

        do {
            System.out.println("==========Confirmation==========");
            System.out.println("  " + message);
            System.out.println("  [Y] YES - " + yesMessage);
            System.out.println("  [N] NO  - " + noMessage);
            System.out.println("=================================");

            System.out.print("Your choice (y/n): ");

            String line = scanner.nextLine().trim();
            userInput = line.isEmpty() ? ' ' : Character.toLowerCase(line.charAt(0));

            if (userInput == 'y') {
                validInput = true;
                result = true;
            } else if (userInput == 'n') {
                validInput = true;
                result = false;
            } else {
                validInput = false;
                result = false;
                ConsoleUtil.printError("Error: Invalid input! Please enter 'y' or 'n'.");
            }
        } while (!validInput);

        return result;
    }

    public String askNonEmptyInput(String prompt) {
        return SharedServices.askNonEmptyInput(scanner, prompt);
    }


    
    public void pressEnterToContinue() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }
}