package tarumtresort.boundary;

import java.util.Scanner;
import tarumtresort.utility.Ansi;

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

        while (true) {
            System.out.print("Enter choice (0-1): ");
            String line = scanner.nextLine();
            if (line.trim().isEmpty()) {
                // ignore a bare Enter: re-prompt in place, no error, no advance
                if (Ansi.ENABLED) {
                    System.out.print("\u001B[1A\u001B[2K");
                }
                continue;
            }
            try {
                int value = Integer.parseInt(line.trim());
                if (value >= 0 && value <= 1) {
                    System.out.println();
                    return value;
                }
            } catch (NumberFormatException e) {
                // fall through to the range error below
            }
            System.out.println("  ✗ Please enter a number between 0 and 1!");
        }
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