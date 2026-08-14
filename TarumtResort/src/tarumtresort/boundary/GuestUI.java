package tarumtresort.boundary;

import java.util.Scanner;

import tarumtresort.entity.Guest;

public class GuestUI {

    Scanner scanner = new Scanner(System.in);

    // INPUTS
    public String inputName() {
        System.out.print("Name: ");
        return scanner.nextLine();
    }

    public String inputNationality() {
        System.out.print("Nationality: ");
        return scanner.nextLine();
    }

    public String inputIc() {
        System.out.print("IC Number (######-##-####): ");
        return scanner.nextLine();
    }

    public String inputPassport() {
        System.out.print("Passport Number: ");
        return scanner.nextLine();
    }

    public String inputContactNumber() {
        System.out.print("Contact Number: ");
        return scanner.nextLine();
    }

    public String inputAddress() {
        System.out.print("Address: ");
        return scanner.nextLine();
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
