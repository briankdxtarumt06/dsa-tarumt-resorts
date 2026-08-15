package tarumtresort.boundary;

import java.util.Scanner;

import tarumtresort.entity.Guest;
import tarumtresort.utility.SharedServices;

public class GuestUI {

    Scanner scanner = new Scanner(System.in);

    // INPUTS
    public String inputName() {
        return SharedServices.askNonEmptyInput(scanner, "Name");
    }

    public String inputNationality() {
        return SharedServices.askNonEmptyInput(scanner, "Nationality");
    }

    public String inputIc() {
        return SharedServices.askNonEmptyInput(scanner, "IC Number (######-##-####)");
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
        System.out.println("  ✗ " + message);
    }

    public void printSuccess() {
        System.out.println("\n  ✓ Operation successful!");
    }
    // OUTPUTS 


    // PRINT TABLE
    public void printGuestDetails(Guest guest) {
        System.out.println("\n[Guest Details]");
        System.out.println("Guest ID    : " + guest.getGuestId());
        System.out.println("Name        : " + guest.getName());
        System.out.println("IC/Passport : " + guest.getIcOrPassport());
        System.out.println("Contact     : " + guest.getContactNumber());
        System.out.println("Nationality : " + guest.getNationality());
        System.out.println("Address     : " + guest.getAddress());
    }
}