package tarumtresort.dao;

import java.nio.file.Path;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Reward;
import tarumtresort.utility.ConsoleUtil;
import tarumtresort.utility.JsonFileHandler;

public class RewardDAO {
    private final String FILE_NAME = "data/rewards.json";

    public void saveToFile(LinkedListInterface<Reward> list) {
        try {
            JsonFileHandler.saveList(list, Path.of(FILE_NAME));
        } catch (java.io.IOException e) {
            ConsoleUtil.printError("Failed to save " + FILE_NAME + ": " + e.getMessage());
        }
    }

    public LinkedList<Reward> retrieveFromFile() {
        LinkedList<Reward> result = new LinkedList<>();
        try {
            LinkedList<Reward> loaded = JsonFileHandler.loadList(Path.of(FILE_NAME), Reward.class);
            for (int i = 0; i < loaded.size(); i++) {
                result.addBack(loaded.get(i));
            }
        } catch (java.io.IOException e) {
            ConsoleUtil.printError("Failed to load " + FILE_NAME + ": " + e.getMessage());
        }
        return result;
    }
}
