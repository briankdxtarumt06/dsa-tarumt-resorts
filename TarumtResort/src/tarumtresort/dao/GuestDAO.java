package tarumtresort.dao;

import java.nio.file.Path;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Guest;
import tarumtresort.utility.ConsoleUtil;
import tarumtresort.utility.JsonFileHandler;

public class GuestDAO {
    private final String FILE_NAME = "data/guests.json";

    public void saveToFile(LinkedListInterface<Guest> list) {
        try {
            JsonFileHandler.saveList(list, Path.of(FILE_NAME));
        } catch (java.io.IOException e) {
            ConsoleUtil.printError("Failed to save " + FILE_NAME + ": " + e.getMessage());
        }
    }

    /** Loads guests into the given list, replacing its current contents. */
    public void loadFromFile(LinkedListInterface<Guest> list) {
        list.clear();
        try {
            LinkedList<Guest> loaded = JsonFileHandler.loadList(Path.of(FILE_NAME), Guest.class);
            for (int i = 0; i < loaded.size(); i++) {
                list.addBack(loaded.get(i));
            }
        } catch (java.io.IOException e) {
            ConsoleUtil.printError("Failed to load " + FILE_NAME + ": " + e.getMessage());
        }
    }

    /** Loads and returns a fresh guest list (empty list if the file is missing). */
    public LinkedList<Guest> retrieveFromFile() {
        LinkedList<Guest> result = new LinkedList<>();
        loadFromFile(result);
        return result;
    }
}
