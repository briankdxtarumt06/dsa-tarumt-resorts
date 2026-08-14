package tarumtresort.control;

import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.boundary.RewardManagementUI;
import tarumtresort.dao.RewardDAO;
import tarumtresort.entity.Reward;
import java.util.Scanner;
import tarumtresort.utility.ConsoleUtil;

/**
 * Business logic for the reward catalogue. The controller owns its UI and
 * drives the menu loop; the UI only handles input and output.
 */
public class RewardController {
    private LinkedListInterface<Reward> rewardList = new LinkedList<>();
    private RewardDAO rewardDAO = new RewardDAO();
    private RewardManagementUI rewardUI;

    public RewardController() {
        this(new Scanner(System.in));
    }

    /** Shares a scanner with the caller (main menu) to avoid input conflicts. */
    public RewardController(Scanner scanner) {
        rewardList = rewardDAO.retrieveFromFile();
        rewardUI = new RewardManagementUI(scanner);
    }

    public static void main(String[] args) {
        // ConsoleUtil.enableUtf8Console();
        new RewardController().run();
    }

    /** Drives the reward management menu until the user exits. */
    public void run() {
        int choice;
        do {
            choice = rewardUI.getMenuChoice();
            switch (choice) {
                case 1:
                    addRewardFlow();
                    break;
                case 2:
                    removeRewardFlow();
                    break;
                case 3:
                    updateRewardFlow();
                    break;
                case 4:
                    rewardUI.displayRewards(rewardList);
                    rewardUI.pause();
                    break;
                case 5:
                    rewardUI.showMessage("Returning to main menu...");
                    break;
                default:
                    rewardUI.showMessage("Invalid choice. Please enter 1 - 5.");
            }
        } while (choice != 5);
    }

    private void addRewardFlow() {
        Reward reward = rewardUI.inputNewReward(nextRewardId());
        if (reward == null) {
            rewardUI.showMessage("Operation cancelled.");
            return;
        }
        rewardUI.showMessage(addReward(reward));
    }

    private void removeRewardFlow() {
        String rewardId = rewardUI.selectRewardId(rewardList, "Select a reward to remove");
        if (rewardId == null) {
            return;
        }
        rewardUI.showMessage(removeReward(rewardId));
    }

    private void updateRewardFlow() {
        String rewardId = rewardUI.selectRewardId(rewardList, "Select a reward to update");
        if (rewardId == null) {
            return;
        }
        Reward reward = findReward(rewardId);
        String name = rewardUI.promptWithDefault("New name", reward.getName());
        if (name == null) {
            rewardUI.showMessage("Operation cancelled.");
            return;
        }
        String description = rewardUI.promptWithDefault("New description", reward.getDescription());
        if (description == null) {
            rewardUI.showMessage("Operation cancelled.");
            return;
        }
        Integer cost = rewardUI.promptIntWithDefault("New point cost", reward.getPointCost());
        if (cost == null) {
            rewardUI.showMessage("Operation cancelled.");
            return;
        }
        rewardUI.showMessage(updateReward(rewardId, name, description, cost));
    }

    // ---------------------------------------------------------------
    // Business logic
    // ---------------------------------------------------------------

    public LinkedListInterface<Reward> getRewards() {
        return rewardList;
    }

    public Reward findReward(String rewardId) {
        for (int i = 0; i < rewardList.size(); i++) {
            if (rewardList.get(i).getRewardId().equals(rewardId)) {
                return rewardList.get(i);
            }
        }
        return null;
    }

    /** Adds a reward to the catalogue and persists it. */
    public String addReward(Reward reward) {
        if (reward == null || reward.getRewardId() == null) {
            return "Reward cannot be null and must have an id.";
        }
        if (findReward(reward.getRewardId()) != null) {
            return "Reward id already exists: " + reward.getRewardId();
        }
        rewardList.addSorted(reward);
        persist();
        return "Reward added: " + reward.getName() + " (" + reward.getPointCost() + " pts).";
    }

    /** Removes a reward from the catalogue by id and persists. */
    public String removeReward(String rewardId) {
        Reward reward = findReward(rewardId);
        if (reward == null) {
            return "Reward not found: " + rewardId;
        }
        LinkedListInterface<Reward> kept = new LinkedList<>();
        for (int i = 0; i < rewardList.size(); i++) {
            if (!rewardList.get(i).getRewardId().equals(rewardId)) {
                kept.addBack(rewardList.get(i));
            }
        }
        rewardList.clear();
        for (int i = 0; i < kept.size(); i++) {
            rewardList.addBack(kept.get(i));
        }
        persist();
        return "Reward removed: " + reward.getName() + " (" + rewardId + ").";
    }

    /** Updates the name, description and cost of a reward, then persists. */
    public String updateReward(String rewardId, String name, String description, int pointCost) {
        Reward reward = findReward(rewardId);
        if (reward == null) {
            return "Reward not found: " + rewardId;
        }
        reward.setName(name);
        reward.setDescription(description);
        reward.setPointCost(pointCost);
        // re-sort so the list stays ordered by cost
        LinkedListInterface<Reward> reordered = new LinkedList<>();
        for (int i = 0; i < rewardList.size(); i++) {
            reordered.addSorted(rewardList.get(i));
        }
        rewardList.clear();
        for (int i = 0; i < reordered.size(); i++) {
            rewardList.addBack(reordered.get(i));
        }
        persist();
        return "Reward updated: " + reward.getName() + " (" + reward.getPointCost() + " pts).";
    }

    /** Generates the next available reward id, e.g. R005. */
    public String nextRewardId() {
        try {
            int max = 0;
            for (int i = 0; i < rewardList.size(); i++) {
                String rid = rewardList.get(i).getRewardId();
                if (rid != null && rid.matches("R\\d+")) {
                    int n = Integer.parseInt(rid.substring(1));
                    if (n > max) {
                        max = n;
                    }
                }
            }
            return String.format("R%03d", max + 1);
        } catch (RuntimeException e) {
            return String.format("R%03d", rewardList.size() + 1);
        }
    }

    private void persist() {
        rewardDAO.saveToFile(rewardList);
    }
}
