package tarumtresort.dao;

import java.nio.file.Path;
import tarumtresort.adt.LinkedList;
import tarumtresort.entity.PointTransaction;
import tarumtresort.utility.JsonFileHandler;

public class PointTransactionDAO {
    private final String FILE_NAME = "data/pointtransactions.json";

    private final LinkedList<PointTransaction> transactions = new LinkedList<>();

    public void Add(PointTransaction transaction) {
        transactions.addSorted(transaction);
    }

    public void Remove(String transactionId) {
        LinkedList<PointTransaction> kept = new LinkedList<>();
        for (int i = 0; i < transactions.size(); i++) {
            PointTransaction t = transactions.get(i);
            if (!t.getTransactionId().equals(transactionId)) {
                kept.addBack(t);
            }
        }
        transactions.clear();
        for (int i = 0; i < kept.size(); i++) {
            transactions.addBack(kept.get(i));
        }
    }

    public PointTransaction FindById(String transactionId) {
        for (int i = 0; i < transactions.size(); i++) {
            if (transactions.get(i).getTransactionId().equals(transactionId)) {
                return transactions.get(i);
            }
        }
        return null;
    }

    public LinkedList<PointTransaction> GetAll() {
        return transactions;
    }

    public int Size() {
        return transactions.size();
    }

    public boolean IsEmpty() {
        return transactions.isEmpty();
    }

    public void LoadFromFile() {
        transactions.clear();
        try {
            LinkedList<PointTransaction> loaded = JsonFileHandler.loadList(Path.of(FILE_NAME), PointTransaction.class);
            for (int i = 0; i < loaded.size(); i++) {
                transactions.addBack(loaded.get(i));
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to load " + FILE_NAME + ": " + e.getMessage());
        }
    }

    public void SaveToFile() {
        try {
            JsonFileHandler.saveList(transactions, Path.of(FILE_NAME));
        } catch (java.io.IOException e) {
            System.err.println("Failed to save " + FILE_NAME + ": " + e.getMessage());
        }
    }
}
