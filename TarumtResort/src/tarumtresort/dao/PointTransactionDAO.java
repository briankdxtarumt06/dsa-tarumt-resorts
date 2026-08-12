package tarumtresort.dao;

import java.nio.file.Path;
import tarumtresort.adt.LinkedList;
import tarumtresort.entity.PointTransaction;
import tarumtresort.utility.JsonFileHandler;

public class PointTransactionDAO {
    private final String FILE_NAME = "data/pointtransactions.json";
    private final LinkedList<PointTransaction> transactions = new LinkedList<>();

    public LinkedList<PointTransaction> LoadFromFile() {
        transactions.clear();
        try {
            LinkedList<PointTransaction> loaded = JsonFileHandler.loadList(Path.of(FILE_NAME), PointTransaction.class);
            for (int i = 0; i < loaded.size(); i++) {
                transactions.addBack(loaded.get(i));
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to load " + FILE_NAME + ": " + e.getMessage());
        }
        return transactions;
    }

    public void SaveToFile() {
        try {
            JsonFileHandler.saveList(transactions, Path.of(FILE_NAME));
        } catch (java.io.IOException e) {
            System.err.println("Failed to save " + FILE_NAME + ": " + e.getMessage());
        }
    }
}
