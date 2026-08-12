package tarumtresort.dao;

import java.nio.file.Path;
import tarumtresort.adt.LinkedList;
import tarumtresort.entity.RedemptionRecord;
import tarumtresort.utility.JsonFileHandler;

public class RedemptionRecordDAO {
    private final String FILE_NAME = "data/redemptions.json";
    private final LinkedList<RedemptionRecord> redemptions = new LinkedList<>();

    public LinkedList<RedemptionRecord> LoadFromFile() {
        redemptions.clear();
        try {
            LinkedList<RedemptionRecord> loaded = JsonFileHandler.loadList(Path.of(FILE_NAME), RedemptionRecord.class);
            for (int i = 0; i < loaded.size(); i++) {
                redemptions.addBack(loaded.get(i));
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to load " + FILE_NAME + ": " + e.getMessage());
        }
        return redemptions;
    }

    public void SaveToFile() {
        try {
            JsonFileHandler.saveList(redemptions, Path.of(FILE_NAME));
        } catch (java.io.IOException e) {
            System.err.println("Failed to save " + FILE_NAME + ": " + e.getMessage());
        }
    }
}
