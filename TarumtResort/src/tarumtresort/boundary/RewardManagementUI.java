package tarumtresort.boundary;

import java.util.Scanner;
import tarumtresort.control.RewardController;
import tarumtresort.dao.RewardDAO;
import tarumtresort.entity.Reward;

public class RewardManagementUI {
    private final Scanner scanner;
    private final RewardController controller;

    public RewardManagementUI() {
        this(new Scanner(System.in));
    }

    public RewardManagementUI(Scanner scanner) {
        this.scanner = scanner;
        RewardDAO rewardDAO = new RewardDAO();
        this.controller = new RewardController(rewardDAO);
    }


    public void run() {
        int choice;
        do {
            printMenu();
            choice = readInt("Enter your choice");
            switch (choice) {
                case 1:
                    addReward();
                    break;
                case 2:
                    removeReward();
                    break;
                case 3:
                    updateReward();
                    break;
                case 4:
                    listRewards();
                    break;
                case 5:
                    System.out.println("Returning to main menu...");
                    break;
                default:
                    System.out.println("Invalid choice. Please enter 1 - 5.");
            }
        } while (choice != 5);
    }

    private void printMenu() {
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
    }

    private void addReward() {
        System.out.println();
        String rewardId = controller.nextRewardId();
        System.out.println("New reward id: " + rewardId);
        System.out.print("Reward name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("Name cannot be empty.");
            return;
        }
        System.out.print("Description: ");
        String description = scanner.nextLine().trim();
        int cost = readInt("Point cost");
        if (cost <= 0) {
            System.out.println("Point cost must be positive.");
            return;
        }
        System.out.println(controller.addReward(new Reward(rewardId, name, description, cost)));
    }

    private void removeReward() {
        String rewardId = selectReward("Select a reward to remove");
        if (rewardId == null) {
            return;
        }
        System.out.println(controller.removeReward(rewardId));
    }

    private void updateReward() {
        String rewardId = selectReward("Select a reward to update");
        if (rewardId == null) {
            return;
        }
        Reward reward = controller.findReward(rewardId);
        System.out.print("New name (" + reward.getName() + "): ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            name = reward.getName();
        }
        System.out.print("New description (" + reward.getDescription() + "): ");
        String description = scanner.nextLine().trim();
        if (description.isEmpty()) {
            description = reward.getDescription();
        }
        System.out.print("New point cost (" + reward.getPointCost() + "): ");
        String costInput = scanner.nextLine().trim();
        int cost;
        if (costInput.isEmpty()) {
            cost = reward.getPointCost();
        } else {
            try {
                cost = Integer.parseInt(costInput);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number, keeping current cost.");
                cost = reward.getPointCost();
            }
        }
        System.out.println(controller.updateReward(rewardId, name, description, cost));
    }

    private void listRewards() {
        if (controller.getRewards().isEmpty()) {
            System.out.println("No rewards in the catalogue.");
            return;
        }
        System.out.println();
        System.out.printf("%-10s | %-22s | %10s | %s%n",
                "Reward ID", "Name", "Cost", "Description");
        System.out.println("--------------------------------------------------------------------");
        for (int i = 0; i < controller.getRewards().size(); i++) {
            Reward r = controller.getRewards().get(i);
            System.out.printf("%-10s | %-22s | %10d | %s%n",
                    r.getRewardId(), truncate(r.getName(), 22), r.getPointCost(), r.getDescription());
        }
    }

    private String selectReward(String prompt) {
        if (controller.getRewards().isEmpty()) {
            System.out.println("No rewards in the catalogue.");
            return null;
        }
        listRewards();
        int index = readInt(prompt) - 1;
        if (index < 0 || index >= controller.getRewards().size()) {
            System.out.println("Invalid selection.");
            return null;
        }
        return controller.getRewards().get(index).getRewardId();
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
