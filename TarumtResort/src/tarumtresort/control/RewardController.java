package tarumtresort.control;

import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.dao.RewardDAO;
import tarumtresort.entity.Reward;

public class RewardController {
    private final RewardDAO rewardDAO;
    private final LinkedListInterface<Reward> rewards = new LinkedList<>();

    public RewardController(RewardDAO rewardDAO) {
        this.rewardDAO = rewardDAO;
        rewardDAO.LoadFromFile(rewards);
    }

    public LinkedListInterface<Reward> getRewards() {
        return rewards;
    }

    public Reward findReward(String rewardId) {
        for (int i = 0; i < rewards.size(); i++) {
            if (rewards.get(i).getRewardId().equals(rewardId)) {
                return rewards.get(i);
            }
        }
        return null;
    }

    public String addReward(Reward reward) {
        if (reward == null || reward.getRewardId() == null) {
            return "Reward cannot be null and must have an id.";
        }
        if (findReward(reward.getRewardId()) != null) {
            return "Reward id already exists: " + reward.getRewardId();
        }
        rewards.addSorted(reward);
        persist();
        return "Reward added: " + reward.getName() + " (" + reward.getPointCost() + " pts).";
    }

    public String removeReward(String rewardId) {
        Reward reward = findReward(rewardId);
        if (reward == null) {
            return "Reward not found: " + rewardId;
        }
        LinkedListInterface<Reward> kept = new LinkedList<>();
        for (int i = 0; i < rewards.size(); i++) {
            if (!rewards.get(i).getRewardId().equals(rewardId)) {
                kept.addBack(rewards.get(i));
            }
        }
        rewards.clear();
        for (int i = 0; i < kept.size(); i++) {
            rewards.addBack(kept.get(i));
        }
        persist();
        return "Reward removed: " + reward.getName() + " (" + rewardId + ").";
    }

    public String updateReward(String rewardId, String name, String description, int pointCost) {
        Reward reward = findReward(rewardId);
        if (reward == null) {
            return "Reward not found: " + rewardId;
        }
        reward.setName(name);
        reward.setDescription(description);
        reward.setPointCost(pointCost);
        LinkedListInterface<Reward> reordered = new LinkedList<>();
        for (int i = 0; i < rewards.size(); i++) {
            reordered.addSorted(rewards.get(i));
        }
        rewards.clear();
        for (int i = 0; i < reordered.size(); i++) {
            rewards.addBack(reordered.get(i));
        }
        persist();
        return "Reward updated: " + reward.getName() + " (" + reward.getPointCost() + " pts).";
    }

    public String nextRewardId() {
        try {
            int max = 0;
            for (int i = 0; i < rewards.size(); i++) {
                String rid = rewards.get(i).getRewardId();
                if (rid != null && rid.matches("R\\d+")) {
                    int n = Integer.parseInt(rid.substring(1));
                    if (n > max) {
                        max = n;
                    }
                }
            }
            return String.format("R%03d", max + 1);
        } catch (RuntimeException e) {
            return String.format("R%03d", rewards.size() + 1);
        }
    }

    private void persist() {
        rewardDAO.SaveToFile(rewards);
    }
}
