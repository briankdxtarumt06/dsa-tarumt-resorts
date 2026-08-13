package tarumtresort.dao;

import java.nio.file.Path;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Promotion;
import tarumtresort.utility.JsonFileHandler;

/**
 * Stateless data access for Promotion records. Methods take or return the
 * LinkedListInterface of entities - the DAO holds no list of its own.
 */
public class PromotionDAO {
    private final String FILE_NAME = "data/promotions.json";

    /** Saves the given promotion list to file. */
    public void saveToFile(LinkedListInterface<Promotion> list) {
        try {
            JsonFileHandler.saveList(list, Path.of(FILE_NAME));
        } catch (java.io.IOException e) {
            System.err.println("Failed to save " + FILE_NAME + ": " + e.getMessage());
        }
    }

    /** Loads promotions from file into the given list, preserving file order. */
    public void loadFromFile(LinkedListInterface<Promotion> list) {
        list.clear();
        try {
            LinkedList<Promotion> loaded = JsonFileHandler.loadList(Path.of(FILE_NAME), Promotion.class);
            for (int i = 0; i < loaded.size(); i++) {
                list.addBack(loaded.get(i));
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to load " + FILE_NAME + ": " + e.getMessage());
        }
    }
}
