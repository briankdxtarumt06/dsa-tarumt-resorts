package tarumtresort.boundary;

import java.util.Scanner;

// Author: Brian Kam Ding Xian
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
        System.out.println("  1. Reservation");
        System.out.println("  2. VIP Reservation");
        System.out.println("  3. Housekeeping");
        System.out.println("  4. Front-Desk Service");
        System.out.println("  5. Loyalty & Rewards");
        System.out.println("  0. Exit");
        System.out.println("========================================");

        return getIntInput("Enter choice (0-5): ", 0, 5);
    }

    // TODO: turn into shared function
    private int getIntInput(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);

            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            try {
                int value = Integer.parseInt(input);

                if (value >= min && value <= max) {
                    return value;
                }

                System.out.printf(
                    " ?! Please enter a number between %d and %d!%n !?",
                    min, max
                );

            } catch (NumberFormatException e) {
                System.out.println(
                    " ?! Invalid input !? Please enter a valid number."
                );
            }
        }
    }

    public void printExitMessage() {
        System.out.println("\n  Exiting Tarumt Resort System. Goodbye!");
    }

    public void printInvalidChoice() {
        System.out.println("\n  ?! Invalid choice !? Please try again.");
    }

    public void pressEnterToContinue() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }
}