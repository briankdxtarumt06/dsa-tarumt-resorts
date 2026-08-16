package tarumtresort.boundary;

import java.util.Scanner;
import tarumtresort.entity.Reservation;

public class ReservationUI {

    Scanner scanner = new Scanner(System.in);

    // MENU
    public int getMenuChoice() {
        System.out.println("  WALK-IN REGISTRATION & BOOKING MODULE");
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
        System.out.println("  0. Exit");
        return inputIntChoice("\nEnter choice", 0, 10);
    }

    // INPUTS 
    public int inputRoomTypeChoice() {
        System.out.println("\nSelect Room Type:");
        System.out.println("1. STANDARD SINGLE");
        System.out.println("2. STANDARD DOUBLE");
        System.out.println("3. STANDARD TRIPLE");
        System.out.println("4. DELUXE SINGLE");
        System.out.println("5. DELUXE DOUBLE");
        System.out.println("6. DELUXE TRIPLE");
        System.out.println("7. SUITE");
        System.out.println("0. Back");
        System.out.print("Enter choice: ");
        return Integer.parseInt(scanner.nextLine().trim());
    }

    public int inputNumberOfGuests() {
        System.out.print("Number of guests in room: ");
        return Integer.parseInt(scanner.nextLine().trim());
    }

    public int inputNumberOfNights() {
        System.out.print("Number of nights: ");
        return Integer.parseInt(scanner.nextLine().trim());
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
        System.out.println("\n[Reservation Details]");
        System.out.println("Reservation ID     : " + r.getReservationId());
        System.out.println("Confirmation No.   : " + r.getConfirmationNumber());
        System.out.println("Guest ID           : " + r.getGuestId());
        System.out.println("Room Type          : " + r.getRoomTypeRequested());
        System.out.println("No. of Guests      : " + r.getNumberOfGuests());
        System.out.println("No. of Nights      : " + r.getNumberOfNights());
        System.out.println("Reservation Type   : " + r.getReservationType());
        System.out.println("Status             : " + r.getStatus());
        System.out.println("Registered At      : " + r.getTimestamps().getRegistrationTimestamp());
        System.out.println("Expected Check-In  : " + r.getTimestamps().getExpectedCheckInDate());
        System.out.println("Expected Check-Out : " + r.getTimestamps().getExpectedCheckOutDate());
        if (r.getTimestamps().getAssignedTime() != null)
            System.out.println("Assigned At        : " + r.getTimestamps().getAssignedTime());
        if (r.getTimestamps().getActualCheckInTime() != null)
            System.out.println("Checked In At      : " + r.getTimestamps().getActualCheckInTime());
        if (r.getTimestamps().getActualCheckOutTime() != null)
            System.out.println("Checked Out At     : " + r.getTimestamps().getActualCheckOutTime());
    }

    public void printWaitingQueueTable(String[][] data) {
        System.out.println("\n[Waiting Queue]");
        if (data.length <= 1) {
            System.out.println("  No reservations in queue.");
            return;
        }
        
        for (String[] row : data) {
            for (String col : row) {
                System.out.printf("%-20s", col);
            }
            System.out.println();
        }
        System.out.println("Total: " + (data.length - 1) + " reservation(s)");
    }

    // MESSAGE METHODS
    public void printSuccess() {
        System.out.println("\n  Operation successful!");
    }

    public void printNotFound() {
        System.out.println("\n  Record not found!");
    }

    public void printCannotCheckIn() {
        System.out.println("\n  Cannot check in. Expected check-in date not yet reached!");
    }

    public void printRoomNotAvailable() {
        System.out.println("\n  No available room for the requested room type!");
    }

    public void printCancelled() {
        System.out.println("\n  Reservation cancelled successfully!");
    }

    public void printExitMessage() {
        System.out.println("\n  Exiting Walk-In Registration & Booking Module. Goodbye!");
    }

    public void printInvalidChoice() {
        System.out.println("\n  Invalid choice! Please try again.");
    }

    public void printError(String message) {
        System.out.println("\n  X " + message);
    }

    public void printCannotCancel(){
        System.out.print(" Your reservation cannot be canceled.");
    }
    
    //others
    public int showSubMenu(String title, String[][] options) {
        System.out.println("\n" + title);
        for (int i = 0; i < options.length; i++) {
            System.out.println(options[i][0] + ". " + options[i][1]);
        }
        System.out.print("Enter choice: ");
        while (true) {
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("  ✗ Invalid input!");
                System.out.print("Enter choice: ");
            }
        }
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
            System.out.print("Your choice (y/n)\n> ");

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