package tarumtresort.boundary;

import java.util.Scanner;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Reward;
import tarumtresort.utility.ConsoleUtil;
import tarumtresort.utility.TablePrinter;

public class RewardManagementUI {

    private Scanner scanner = new Scanner(System.in);

    public RewardManagementUI() {
    }

    public RewardManagementUI(Scanner scanner) {
        this.scanner = scanner;
    }

    public int getMenuChoice() {
        ConsoleUtil.clearScreen();
        System.out.println();
        System.out.println("========================================");
        System.out.println("   REWARD MANAGEMENT");
        System.out.println("========================================");
        System.out.println(" 1. Add Reward");
        System.out.println(" 2. Remove Reward");
        System.out.println(" 3. Update Reward");
        System.out.println(" 4. List Rewards");
        System.out.println(" 5. Exit");
        System.out.println("----------------------------------------");
        return readInt("Enter your choice");
    }

    public Reward inputNewReward(String rewardId) {
        System.out.println("New reward id: " + rewardId);
        String name = "";
        while (name.isEmpty()) {
            System.out.print("Reward name (0 to cancel): ");
            name = scanner.nextLine().trim();
            if (name.equals("0")) {
                System.out.println("Operation cancelled.");
        ConsoleUtil.pressEnterToContinue(scanner);
                return null;
            }
            if (name.isEmpty()) {
                System.out.println("Reward name cannot be empty.");
            }
        }
        System.out.print("Description (0 to cancel): ");
        String description = scanner.nextLine().trim();
        if (description.equals("0")) {
            System.out.println("Operation cancelled.");
        ConsoleUtil.pressEnterToContinue(scanner);
            return null;
        }
        int cost = readInt("Point cost (0 to cancel)");
        if (cost == 0) {
            System.out.println("Operation cancelled.");
        ConsoleUtil.pressEnterToContinue(scanner);
            return null;
        }
        while (cost < 0) {
            System.out.println("Point cost must be positive.");
            cost = readInt("Point cost (0 to cancel)");
            if (cost == 0) {
                System.out.println("Operation cancelled.");
        ConsoleUtil.pressEnterToContinue(scanner);
                return null;
            }
        }
        return new Reward(rewardId, name, description, cost);
    }

    public String selectRewardId(LinkedListInterface<Reward> rewards, String prompt) {
        if (rewards.isEmpty()) {
            System.out.println("No rewards in the catalogue.");
            ConsoleUtil.pressEnterToContinue(scanner);
            return null;
        }
        displayRewards(rewards);
        System.out.println(" 0. Cancel");
        int index = readInt(prompt) - 1;
        if (index < 0) {
            System.out.println("Operation cancelled.");
        ConsoleUtil.pressEnterToContinue(scanner);
            return null;
        }
        if (index >= rewards.size()) {
            System.out.println("Invalid selection.");
        ConsoleUtil.pressEnterToContinue(scanner);
            return null;
        }
        return rewards.get(index).getRewardId();
    }

    public void displayRewards(LinkedListInterface<Reward> rewards) {
        if (rewards.isEmpty()) {
            System.out.println("No rewards in the catalogue.");
            return;
        }
        System.out.println();
        String[][] rows = new String[rewards.size()][4];
        for (int i = 0; i < rewards.size(); i++) {
            Reward r = rewards.get(i);
            rows[i] = new String[] {
                    r.getRewardId(),
                    truncate(r.getName(), 22),
                    String.valueOf(r.getPointCost()),
                    r.getDescription()
            };
        }
        TablePrinter.displayTable(
                new String[] { "Reward ID", "Name", "Cost", "Description" }, rows);
    }

    /** Prompts for a string, returning the current value if the input is empty. */
    public String promptWithDefault(String prompt, String current) {
        System.out.print(prompt + " (" + current + ") (0 to cancel): ");
        String input = scanner.nextLine().trim();
        if (input.equals("0")) {
            System.out.println("Operation cancelled.");
        ConsoleUtil.pressEnterToContinue(scanner);
            return null;
        }
        return input.isEmpty() ? current : input;
    }

    public Integer promptIntWithDefault(String prompt, int current) {
        System.out.print(prompt + " (" + current + ") (0 to cancel): ");
        String input = scanner.nextLine().trim();
        if (input.equals("0")) {
            System.out.println("Operation cancelled.");
        ConsoleUtil.pressEnterToContinue(scanner);
            return null;
        }
        if (input.isEmpty()) {
            return current;
        }
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid number, keeping current value.");
            return current;
        }
    }

    public void showMessage(String message) {
        System.out.println(message);
        pause();
    }

    public void show(String message) {
        System.out.println(message);
    }

    public void pause() {
        ConsoleUtil.pressEnterToContinue(scanner);
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

    private static String truncate(String text, int width) {
        if (text == null || text.length() <= width) {
            return text;
        }
        return text.substring(0, width - 3) + "...";
    }
}
