package tarumtresort.dao;

import java.nio.file.Path;
import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;
import tarumtresort.entity.Reward;
import tarumtresort.utility.ConsoleUtil;
import tarumtresort.utility.JsonFileHandler;

// Author: Imam Mahdi Ali Ang Attuko
public class RewardDAO {
    private final String FILE_NAME = "data/rewards.json";

    public void saveToFile(ListInterface<Reward> list) {
        try {
            JsonFileHandler.saveList(list, Path.of(FILE_NAME));
        } catch (java.io.IOException e) {
            ConsoleUtil.printError("Failed to save " + FILE_NAME + ": " + e.getMessage());
        }
    }

    public DoublyLinkedList<Reward> retrieveFromFile() {
        DoublyLinkedList<Reward> result = new DoublyLinkedList<>();
        try {
            DoublyLinkedList<Reward> loaded = JsonFileHandler.loadList(Path.of(FILE_NAME), Reward.class);
            for (int i = 0; i < loaded.size(); i++) {
                result.addBack(loaded.get(i));
            }
        } catch (java.io.IOException e) {
            ConsoleUtil.printError("Failed to load " + FILE_NAME + ": " + e.getMessage());
        }
        return result;
    }
}
