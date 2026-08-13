package tarumtresort.boundary;

import java.util.Scanner;

/**
 *
 * @author Brian
 */
public class MainMenuUI {

    private Scanner scanner = new Scanner(System.in);

    // Placeholder Main Menu UI
    public MainMenuUI() {
    }

    public MainMenuUI(Scanner scanner) {
        this.scanner = scanner;
    }

    public int getModuleChoice() {
        System.out.println("\n========================================");
        System.out.println("  TARUMT RESORT MANAGEMENT SYSTEM");
        System.out.println("========================================");
        System.out.println("  1. Housekeeping Management");
        System.out.println("  0. Exit");
        System.out.println("========================================");

        int choice = -1;
        while (choice < 0 || choice > 1) {
            System.out.print("Enter choice (0-1): ");
            if (scanner.hasNextInt()) {
                choice = scanner.nextInt();
                scanner.nextLine();
                if (choice < 0 || choice > 1) {
                    System.out.println("  ✗ Please enter a number between 0 and 1!");
                }
            } else {
                System.out.println("  ✗ Invalid input! Please enter a number.");
                scanner.nextLine();
            }
        }
        System.out.println();
        return choice;
    }

    public void printExitMessage() {
        System.out.println("\n  Exiting Tarumt Resort System. Goodbye!");
    }

    public void printInvalidChoice() {
        System.out.println("\n  ✗ Invalid choice! Please try again.");
    }

    public void pressEnterToContinue() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }
}