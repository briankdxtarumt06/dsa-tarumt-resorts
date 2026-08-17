package tarumtresort.boundary;

import java.util.Scanner;

import tarumtresort.entity.Guest;
import tarumtresort.utility.*;

public class GuestUI {

    Scanner scanner = new Scanner(System.in);

    // INPUTS
    public String inputName() {
        return SharedServices.askNonEmptyInput(scanner, "Name (Enter '0' to go back)");
    }

    public String inputNationality(String[] nationalityOptions) {
        int otherChoice = nationalityOptions.length + 1;

        String[] header = {"No.", "Nationality"};
        String[][] rows = new String[otherChoice][2];
        for (int i = 0; i < nationalityOptions.length; i++) {
            rows[i] = new String[]{String.valueOf(i + 1), nationalityOptions[i]};
        }
        rows[nationalityOptions.length] = new String[]{String.valueOf(otherChoice), "Other"};

        System.out.println("\nNationality:");
        TablePrinter.displayTable(header, rows);

        int choice;
        while (true) {
            System.out.print("Enter choice: ");
            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice >= 1 && choice <= otherChoice) {
                    break;
                }
                System.out.println("Error: Invalid choice! Please try again.");
            } catch (NumberFormatException e) {
                System.out.println("Error: Please enter a number.");
            }
        }

        if (choice == otherChoice) {
            String typed = SharedServices.askNonEmptyInput(scanner, "Please specify nationality");

            // check if it actually matches one of the fixed options  
            for (int i = 0; i < nationalityOptions.length; i++) {
                if (nationalityOptions[i].equalsIgnoreCase(typed.trim())) {
                    return nationalityOptions[i]; // reuse the exact spelling already in the list
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
        String[] header = {" ", "Guest Information"};
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