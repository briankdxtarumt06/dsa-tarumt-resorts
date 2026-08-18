package tarumtresort.boundary;

import java.util.Scanner;

import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Guest;
import tarumtresort.entity.Reservation;
import tarumtresort.utility.*;

public class GuestUI {

    Scanner scanner = new Scanner(System.in);

    public int printGuestListMenu(LinkedListInterface<Guest> pageList, int page, int pageCount, boolean hasFilter) {
        ConsoleUtil.clearScreen();
        System.out.println("\n==============================");
        System.out.println("  GUEST MANAGEMENT (Page " + (page + 1) + " of " + pageCount + ")");
        System.out.println("==============================");
        if (pageList.isEmpty()) {
            System.out.println("  (No guest records)");
        } else {
            String[] header = {"No.", "Guest ID", "Name", "Nationality", "Contact"};
            String[][] rows = new String[pageList.size()][5];
            for (int i = 0; i < pageList.size(); i++) {
                Guest g = pageList.get(i);
                rows[i] = new String[]{
                    String.valueOf(i + 1), g.getGuestId(), g.getName(),
                    g.getNationality(), g.getContactNumber()
                };
            }
            TablePrinter.displayTable(header, rows);
        }

        System.out.println("==========Actions==========");
        int action = 1;
        System.out.println("  " + action++ + ". View Details");
        System.out.println("  " + action++ + ". Register New Guest");
        System.out.println("  " + action++ + ". Filter by Nationality");
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

        System.out.println("===========================");
        return inputIntChoice("Enter choice", 0, action - 1);
    }

    public int getGuestActionChoice() {
        System.err.println();
        System.out.print("==========Actions==========");
        System.out.println("\n  1. View Reservation History");
        System.out.println("  0. Back to List");
        System.out.println("===========================");
        return inputIntChoice("Enter choice", 0, 1);
    }

    public void printNoRecords() {
        ConsoleUtil.printError("No records to view!");
    }

    public int inputListIndex(String entityLabel, int max) {
        return inputIntChoice("Enter " + entityLabel + " number to view (0 = cancel)", 0, max);
    }

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

    public void printGuestReservationHistory(LinkedListInterface<Reservation> reservations) {
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

    public void pressEnterToContinue() {
        ConsoleUtil.pressEnterToContinue(scanner);
    }

    // INPUTS
    public String inputName() {
        return SharedServices.askNonEmptyInput(scanner, "Name (0 = cancel)");
    }

    public String inputNationality(String[] nationalityOptions) {
        int otherChoice = nationalityOptions.length + 1;

        System.out.println();
        System.out.println("==========Nationality==========");

        for (int i = 0; i < nationalityOptions.length; i++) {
            System.out.println("  " + (i + 1) + ". " + nationalityOptions[i]);
        }

        System.out.println("  "+ otherChoice + ". Other");

        System.out.println("==============================");

        int choice = inputIntChoice("Enter Choice", 1, otherChoice);

        if (choice == otherChoice) {
            String typed = SharedServices.askNonEmptyInput(
                scanner, 
                "Please specify nationality"
            );

            // Check if it matches one of the fixed options
            for (int i = 0; i < nationalityOptions.length; i++) {
                if (nationalityOptions[i].equalsIgnoreCase(typed.trim())) {
                    return nationalityOptions[i];
                }
            }

            return typed;
        }

        return nationalityOptions[choice - 1];
    }

    public String inputIc() {
        return SharedServices.askNonEmptyInput(scanner, "IC Number (XXXXXXXXXXXX)");
    }

    public String inputPassport() {
        return SharedServices.askNonEmptyInput(scanner, "Passport Number");
    }

    public String inputContactNumber() {
        return SharedServices.askNonEmptyInput(scanner, "Contact Number");
    }

    public String inputAddress() {
        return SharedServices.askNonEmptyInput(scanner, "Address");
    }

    public void printInvalidInput(String message) {
        ConsoleUtil.printError(message + "\n");
    }

    public void printSuccess() {
        ConsoleUtil.printSuccess("\nOperation successful!\n");
    }

    // OUTPUTS 


    // PRINT TABLE
    public void printGuestDetails(Guest guest) {
        String[] header = {"Field", "Value"};
        String[][] rows = {
            {"Guest ID", guest.getGuestId()},
            {"Name", guest.getName()},
            {"IC/Passport", guest.getIcOrPassport()},
            {"Contact", guest.getContactNumber()},
            {"Nationality", guest.getNationality()},
            {"Address", guest.getAddress()}
        };
        TablePrinter.displayTable(header, rows);
    }
}