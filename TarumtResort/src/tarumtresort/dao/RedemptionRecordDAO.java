package tarumtresort.dao;

import java.nio.file.Path;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.RedemptionRecord;
import tarumtresort.utility.JsonFileHandler;

public class RedemptionRecordDAO {
    private final String FILE_NAME = "data/redemptions.json";

    public void saveToFile(LinkedListInterface<RedemptionRecord> list) {
        try {
            JsonFileHandler.saveList(list, Path.of(FILE_NAME));
        } catch (java.io.IOException e) {
            System.err.println("Failed to save " + FILE_NAME + ": " + e.getMessage());
        }
    }

    public void loadFromFile(LinkedListInterface<RedemptionRecord> list) {
        list.clear();
        try {
            LinkedList<RedemptionRecord> loaded = JsonFileHandler.loadList(Path.of(FILE_NAME), RedemptionRecord.class);
            for (int i = 0; i < loaded.size(); i++) {
                list.addBack(loaded.get(i));
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to load " + FILE_NAME + ": " + e.getMessage());
        }
    }
}
