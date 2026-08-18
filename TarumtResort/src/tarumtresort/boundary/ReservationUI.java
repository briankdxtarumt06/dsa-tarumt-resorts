package tarumtresort.boundary;

import java.util.Scanner;

import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Reservation;
import tarumtresort.utility.ConsoleUtil;
import tarumtresort.utility.TablePrinter;

public class ReservationUI {

    Scanner scanner = new Scanner(System.in);

    // MENU
    public int getMenuChoice() {
        ConsoleUtil.clearScreen();
        System.out.println("========================================");
        System.out.println("   WALK-IN REGISTRATION & BOOKING ");
        System.out.println("========================================");
        System.out.println("  1. Register Guest");
        System.out.println("  2. Book Room");
        System.out.println("  3. Guest Arrival");
        System.out.println("  4. Assign Room to Next Guest");
        System.out.println("  5. Check In");
        System.out.println("  6. Check Out");
        System.out.println("  7. View Queue");
        System.out.println("  8. Check Queue Position");
        System.out.println("  9. Cancel Reservation");
        System.out.println("  10. Reports");
        System.out.println("  11. View Payment / Refund Records");
        System.out.println("  0. Exit");
        return inputIntChoice("\nEnter choice", 0, 11);
    }

    // INPUTS 
    public int inputRoomTypeChoice() {
        String[] header = {"No.", "Room Type"};
        String[][] rows = {
            {"1", "STANDARD SINGLE"},
            {"2", "STANDARD DOUBLE"},
            {"3", "STANDARD TRIPLE"},
            {"4", "DELUXE SINGLE"},
            {"5", "DELUXE DOUBLE"},
            {"6", "DELUXE TRIPLE"},
            {"7", "SUITE"},
            {"0", "Back"}
        };

        System.out.println("\nSelect Room Type:");
        TablePrinter.displayTable(header, rows);

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
                System.out.println("Must be at least 1!");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter a number.");
            }
        }
    }

    public int inputNumberOfNights() {
        while (true) {
            ConsoleUtil.clearScreen();
            System.out.print("Number of nights: ");
            try {
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value >= 1) {
                    return value;
                }
                System.out.println("Must be at least 1!");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter a number.");
            }
        }
    }

    public String inputDate(String prompt) {
        System.out.print(prompt + " (YYYY-MM-DD): ");
        return scanner.nextLine();
    }

    public String inputIcOrPassport(){
        System.out.print("Enter guest's IC or Passport: ");
        return scanner.nextLine();
    }

    public String inputConfirmationNumber(){
        System.out.print("Enter confirmation number: ");
        return scanner.nextLine();
    }

    public void printQueuePosition(String confirmationNumber, int position) {
        System.out.println("  Confirmation No. " + confirmationNumber + " → Position #" + position);
    }
    // OUTPUTS

    // PRINT TABLE
    // print reservation details aftre finish reservation 
    public void printReservationDetails(Reservation r) {
        String[] header = {
            "Reservation ID", "Conf. No.", "Guest ID", "Room Type",
            "Guests", "Nights", "Type", "Status",
            "Registered At", "Expected Check-In", "Expected Check-Out",
            "Assigned At", "Checked In At", "Checked Out At"
        };

        String[][] rows = {
            {
                r.getReservationId(),
                r.getConfirmationNumber(),
                r.getGuestId(),
                r.getRoomTypeRequested().toString(),
                String.valueOf(r.getNumberOfGuests()),
                String.valueOf(r.getNumberOfNights()),
                r.getReservationType().toString(),
                r.getStatus().toString(),
                String.valueOf(r.getTimestamps().getRegistrationTimestamp()),
                String.valueOf(r.getTimestamps().getExpectedCheckInDate()),
                String.valueOf(r.getTimestamps().getExpectedCheckOutDate()),
                r.getTimestamps().getAssignedTime() != null ? r.getTimestamps().getAssignedTime().toString() : "-",
                r.getTimestamps().getActualCheckInTime() != null ? r.getTimestamps().getActualCheckInTime().toString() : "-",
                r.getTimestamps().getActualCheckOutTime() != null ? r.getTimestamps().getActualCheckOutTime().toString() : "-"
            }
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
        System.out.println("\nWaiting Queue");
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

    // MESSAGE METHODS
    public void printSuccess() {
        ConsoleUtil.printSuccess("\n  Operation successful!");
    }

    public void printSuccess(String message) {
        ConsoleUtil.printSuccess("\n  " + message);
    }

    public void printNotFound() {
        ConsoleUtil.printWarning("\n  Record not found!");
    }

    public void printCannotCheckIn() {
        ConsoleUtil.printError("\n  Cannot check in. Expected check-in date not yet reached!");
    }

    public void printRoomNotAvailable() {
        ConsoleUtil.printWarning("\n  No available room for the requested room type!");
    }

    public void printCancelled() {
        ConsoleUtil.printSuccess("\n  Reservation cancelled successfully!");
    }

    public void printExitMessage() {
        ConsoleUtil.printSuccess("\n  Exiting Walk-In Registration & Booking Module. Goodbye!");
    }

    public void printInvalidChoice() {
        ConsoleUtil.printError("\n  Invalid choice! Please try again.");
    }

    public void printError(String message) {
        ConsoleUtil.printError("\n  " + message);
    }

    public void printCannotCancel(){
        ConsoleUtil.printError(" Your reservation cannot be canceled.");
    }
    
    //others
    public int showSubMenu(String title, String[][] options) {
        String[] header = {"No.", "Option"};

        System.out.println("\n" + title);
        TablePrinter.displayTable(header, options);

        int maxChoice = options.length - 1;
        return inputIntChoice("Enter choice", 0, maxChoice);
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
                    System.out.println("Please enter a number between " + min + " and " + max + "!");
            } else {
                System.out.println("Invalid input! Please enter a number.");
                scanner.nextLine();
            }
        }
        System.out.println();
        return choice;
    }

    public boolean askConfirmation(String message, String yesMessage, String noMessage) {
        char userInput;
        boolean validInput;
        boolean result;

        do {
            System.out.println();
            System.out.println("# " + message);
            System.out.println("[Y] YES - " + yesMessage);
            System.out.println("[N] NO - " + noMessage);
            System.out.println();
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
                System.out.println("Error: Invalid input! Please enter 'y' or 'n'.");
            }
        } while (!validInput);

        return result;
    }

    public String askNonEmptyInput(String prompt) {
        String userInput;
        boolean validInput;

        do {
            System.out.print(prompt + "\n> ");
            userInput = scanner.nextLine().trim();

            if (userInput.isEmpty()) {
                validInput = false;
                System.out.println("Error: Input cannot be empty! Please try again.");
            } else {
                validInput = true;
            }
        } while (!validInput);

        return userInput;
    }

    public void pressEnterToContinue() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }
}