package tarumtresort.dao;

import java.nio.file.Path;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.PointTransaction;
import tarumtresort.utility.JsonFileHandler;

public class PointTransactionDAO {
    private final String FILE_NAME = "data/pointtransactions.json";

    public void saveToFile(LinkedListInterface<PointTransaction> list) {
        try {
            JsonFileHandler.saveList(list, Path.of(FILE_NAME));
        } catch (java.io.IOException e) {
            System.err.println("Failed to save " + FILE_NAME + ": " + e.getMessage());
        }
    }

    public LinkedList<PointTransaction> retrieveFromFile() {
        LinkedList<PointTransaction> result = new LinkedList<>();
        try {
            LinkedList<PointTransaction> loaded = JsonFileHandler.loadList(Path.of(FILE_NAME), PointTransaction.class);
            for (int i = 0; i < loaded.size(); i++) {
                result.addBack(loaded.get(i));
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to load " + FILE_NAME + ": " + e.getMessage());
        }
        return result;
    }
}
