package tarumtresort.boundary;

import java.util.Scanner;
import tarumtresort.control.MemberController;
import tarumtresort.control.PointsController;
import tarumtresort.control.RewardController;

/**
 *
 * @author Brian
 *
 * Module driver for the Loyalty & Rewards subsystem.
 * Launched from TarumtResort via MainMenuUI case 5.
 */
public class LoyaltyRewardsUI {

    private final Scanner scanner;

    public LoyaltyRewardsUI(Scanner scanner) {
        this.scanner = scanner;
    }

    public void run() {
        int choice;
        do {
            printMenu();
            choice = getIntInput("Enter choice (0-3): ", 0, 3);

            switch (choice) {
                case 1:
                    new MemberController(scanner).run();
                    break;
                case 2:
                    new RewardController(scanner).run();
                    break;
                case 3:
                    new PointsController(scanner).run();
                    break;
                case 0:
                    System.out.println("\n  Returning to main menu...");
                    break;
                default:
                    System.out.println("\n  ✗ Invalid choice! Please try again.");
            }
        } while (choice != 0);
    }

    private void printMenu() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("  LOYALTY & REWARDS MODULE");
        System.out.println("========================================");
        System.out.println("  1. Member Management");
        System.out.println("  2. Reward Management");
        System.out.println("  3. Points & Redemption Management");
        System.out.println("  0. Back to Main Menu");
        System.out.println("========================================");
    }

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
                    "  ✗ Please enter a number between %d and %d!%n",
                    min, max
                );

            } catch (NumberFormatException e) {
                System.out.println(
                    "  ✗ Invalid input! Please enter a valid number."
                );
            }
        }
    }
}
