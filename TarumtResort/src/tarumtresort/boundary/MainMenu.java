package tarumtresort.boundary;

import java.util.Scanner;
import tarumtresort.control.MemberController;
import tarumtresort.control.PointsController;
import tarumtresort.control.RewardController;
import tarumtresort.utility.ConsoleUtil;

public class MainMenu {
    private final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        new MainMenu().run();
    }

    public void run() {
        int choice;
        do {
            printMenu();
            choice = readInt("Enter your choice");
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
                case 4:
                    System.out.println("Thank you for using TARUMT Resort Loyalty System. Goodbye!");
                    break;
                default:
                    ConsoleUtil.printError("Invalid choice. Please enter 1 - 4.");
                    ConsoleUtil.pressEnterToContinue(scanner);
            }
        } while (choice != 4);
    }

    private void printMenu() {
        ConsoleUtil.clearScreen();
        System.out.println();
        System.out.println("========================================");
        System.out.println("   TARUMT RESORT LOYALTY SYSTEM");
        System.out.println("========================================");
        System.out.println(" 1. Member Management");
        System.out.println(" 2. Reward Management");
        System.out.println(" 3. Points & Redemption Management");
        System.out.println(" 4. Exit");
        System.out.println("----------------------------------------");
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt + ": ");
            if (!scanner.hasNextLine()) {
                System.out.println("No more input. Exiting.");
                System.exit(0);
            }
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                ConsoleUtil.printError("Please enter a valid number.");
            }
        }
    }
}
