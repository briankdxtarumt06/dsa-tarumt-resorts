package tarumtresort.boundary;

import java.util.Scanner;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Reward;
import tarumtresort.utility.ConsoleUtil;

public class RewardManagementUI {

    private Scanner scanner = new Scanner(System.in);

    /** Standalone run creates its own scanner. */
    public RewardManagementUI() {
    }

    /** Uses a shared scanner passed from the caller (main menu). */
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

    /** Prompts for a new reward's details and returns it. */
    public Reward inputNewReward(String rewardId) {
        System.out.println("New reward id: " + rewardId);
        System.out.print("Reward name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            name = "Unnamed reward";
        }
        System.out.print("Description: ");
        String description = scanner.nextLine().trim();
        int cost = readInt("Point cost");
        while (cost <= 0) {
            System.out.println("Point cost must be positive.");
            cost = readInt("Point cost");
        }
        return new Reward(rewardId, name, description, cost);
    }

    /** Lists the rewards and returns the chosen reward id, or null. */
    public String selectRewardId(LinkedListInterface<Reward> rewards, String prompt) {
        if (rewards.isEmpty()) {
            System.out.println("No rewards in the catalogue.");
            return null;
        }
        displayRewards(rewards);
        int index = readInt(prompt) - 1;
        if (index < 0 || index >= rewards.size()) {
            System.out.println("Invalid selection.");
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
        System.out.printf("%-10s | %-22s | %10s | %s%n",
                "Reward ID", "Name", "Cost", "Description");
        System.out.println("--------------------------------------------------------------------");
        for (int i = 0; i < rewards.size(); i++) {
            Reward r = rewards.get(i);
            System.out.printf("%-10s | %-22s | %10d | %s%n",
                    r.getRewardId(), truncate(r.getName(), 22), r.getPointCost(), r.getDescription());
        }
    }

    /** Prompts for a string, returning the current value if the input is empty. */
    public String promptWithDefault(String prompt, String current) {
        System.out.print(prompt + " (" + current + "): ");
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? current : input;
    }

    /** Prompts for an int, returning the current value if the input is empty/invalid. */
    public int promptIntWithDefault(String prompt, int current) {
        System.out.print(prompt + " (" + current + "): ");
        String input = scanner.nextLine().trim();
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

    /** Prints a message and waits for the user to press Enter. */
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
