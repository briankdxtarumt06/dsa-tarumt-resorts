package tarumtresort.dao;

import java.nio.file.Path;
import tarumtresort.adt.LinkedList;
import tarumtresort.entity.RedemptionRecord;
import tarumtresort.utility.JsonFileHandler;

public class RedemptionRecordDAO {
    private final String FILE_NAME = "data/redemptions.json";

    private final LinkedList<RedemptionRecord> redemptions = new LinkedList<>();

    public void Add(RedemptionRecord record) {
        redemptions.addSorted(record);
    }

    public void Remove(String redemptionId) {
        LinkedList<RedemptionRecord> kept = new LinkedList<>();
        for (int i = 0; i < redemptions.size(); i++) {
            RedemptionRecord r = redemptions.get(i);
            if (!r.getRedemptionId().equals(redemptionId)) {
                kept.addBack(r);
            }
        }
        redemptions.clear();
        for (int i = 0; i < kept.size(); i++) {
            redemptions.addBack(kept.get(i));
        }
    }

    public RedemptionRecord FindById(String redemptionId) {
        for (int i = 0; i < redemptions.size(); i++) {
            if (redemptions.get(i).getRedemptionId().equals(redemptionId)) {
                return redemptions.get(i);
            }
        }
        return null;
    }

    public LinkedList<RedemptionRecord> GetAll() {
        return redemptions;
    }

    public int Size() {
        return redemptions.size();
    }

    public boolean IsEmpty() {
        return redemptions.isEmpty();
    }

    public void LoadFromFile() {
        redemptions.clear();
        try {
            LinkedList<RedemptionRecord> loaded = JsonFileHandler.loadList(Path.of(FILE_NAME), RedemptionRecord.class);
            for (int i = 0; i < loaded.size(); i++) {
                redemptions.addBack(loaded.get(i));
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to load " + FILE_NAME + ": " + e.getMessage());
        }
    }

    public void SaveToFile() {
        try {
            JsonFileHandler.saveList(redemptions, Path.of(FILE_NAME));
        } catch (java.io.IOException e) {
            System.err.println("Failed to save " + FILE_NAME + ": " + e.getMessage());
        }
    }
}
