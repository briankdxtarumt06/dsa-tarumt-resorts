package tarumtresort.dao;

import java.nio.file.Path;
import tarumtresort.adt.LinkedList;
import tarumtresort.entity.Reward;
import tarumtresort.utility.JsonFileHandler;

public class RewardDAO {
    private final String FILE_NAME = "data/rewards.json";

    private final LinkedList<Reward> rewards = new LinkedList<>();

    public void Add(Reward reward) {
        rewards.addSorted(reward);
    }

    public void Remove(String rewardId) {
        LinkedList<Reward> kept = new LinkedList<>();
        for (int i = 0; i < rewards.size(); i++) {
            Reward r = rewards.get(i);
            if (!r.getRewardId().equals(rewardId)) {
                kept.addBack(r);
            }
        }
        rewards.clear();
        for (int i = 0; i < kept.size(); i++) {
            rewards.addBack(kept.get(i));
        }
    }

    public Reward FindById(String rewardId) {
        for (int i = 0; i < rewards.size(); i++) {
            if (rewards.get(i).getRewardId().equals(rewardId)) {
                return rewards.get(i);
            }
        }
        return null;
    }

    public LinkedList<Reward> GetAll() {
        return rewards;
    }

    public int Size() {
        return rewards.size();
    }

    public boolean IsEmpty() {
        return rewards.isEmpty();
    }

    public void LoadFromFile() {
        rewards.clear();
        try {
            LinkedList<Reward> loaded = JsonFileHandler.loadList(Path.of(FILE_NAME), Reward.class);
            for (int i = 0; i < loaded.size(); i++) {
                rewards.addBack(loaded.get(i));
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to load " + FILE_NAME + ": " + e.getMessage());
        }
    }

    public void SaveToFile() {
        try {
            JsonFileHandler.saveList(rewards, Path.of(FILE_NAME));
        } catch (java.io.IOException e) {
            System.err.println("Failed to save " + FILE_NAME + ": " + e.getMessage());
        }
    }
}
