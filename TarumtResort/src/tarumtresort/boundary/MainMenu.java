package tarumtresort.boundary;

import java.util.Scanner;

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
                    new RewardManagementUI(scanner).run();
                    break;
                case 2:
                    new PointsManagementUI(scanner).run();
                    break;
                case 3:
                    System.out.println("Thank you for using TARUMT Resort System. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice. Please enter 1 - 3.");
            }
        } while (choice != 3);
    }

    private void printMenu() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("   TARUMT RESORT MANAGEMENT SYSTEM");
        System.out.println("========================================");
        System.out.println(" 1. Reward Management");
        System.out.println(" 2. Points & Redemption Management");
        System.out.println(" 3. Exit");
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
