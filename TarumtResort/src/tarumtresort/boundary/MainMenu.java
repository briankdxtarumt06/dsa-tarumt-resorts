package tarumtresort.boundary;

import java.util.Scanner;
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
                    new MemberManagementUI(scanner).run();
                    break;
                case 2:
                    new RewardManagementUI(scanner).run();
                    break;
                case 3:
                    new PointsManagementUI(scanner).run();
                    break;
                case 4:
                    System.out.println("Thank you for using TARUMT Resort System. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice. Please enter 1 - 4.");
            }
        } while (choice != 4);
    }

    private void printMenu() {
        ConsoleUtil.clearScreen();
        System.out.println();
        System.out.println("========================================");
        System.out.println("   TARUMT RESORT MANAGEMENT SYSTEM");
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
                System.out.println("Please enter a valid number.");
            }
        }
    }
}
