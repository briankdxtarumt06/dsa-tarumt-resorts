package tarumtresort.dao;

import java.nio.file.Path;
import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;
import tarumtresort.entity.RedemptionRecord;
import tarumtresort.utility.ConsoleUtil;
import tarumtresort.utility.JsonFileHandler;

public class RedemptionRecordDAO {
    private final String FILE_NAME = "data/redemptions.json";

    public void saveToFile(ListInterface<RedemptionRecord> list) {
        try {
            JsonFileHandler.saveList(list, Path.of(FILE_NAME));
        } catch (java.io.IOException e) {
            ConsoleUtil.printError("Failed to save " + FILE_NAME + ": " + e.getMessage());
        }
    }

    public DoublyLinkedList<RedemptionRecord> retrieveFromFile() {
        DoublyLinkedList<RedemptionRecord> result = new DoublyLinkedList<>();
        try {
            DoublyLinkedList<RedemptionRecord> loaded = JsonFileHandler.loadList(Path.of(FILE_NAME), RedemptionRecord.class);
            for (int i = 0; i < loaded.size(); i++) {
                result.addBack(loaded.get(i));
            }
        } catch (java.io.IOException e) {
            ConsoleUtil.printError("Failed to load " + FILE_NAME + ": " + e.getMessage());
        }
        return result;
    }
}
