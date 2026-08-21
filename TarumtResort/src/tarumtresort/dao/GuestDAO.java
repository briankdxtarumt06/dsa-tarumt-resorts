package tarumtresort.dao;

// Author: Chai Chee Tong

import java.nio.file.Path;
import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;
import tarumtresort.entity.Guest;
import tarumtresort.utility.ConsoleUtil;
import tarumtresort.utility.JsonFileHandler;

public class GuestDAO {
    private final String FILE_NAME = "data/guestList.json";

    public void saveToFile(ListInterface<Guest> list) {
        try {
            JsonFileHandler.saveList(list, Path.of(FILE_NAME));
        } catch (java.io.IOException e) {
            ConsoleUtil.printError("Failed to save " + FILE_NAME + ": " + e.getMessage());
        }
    }

    /** Loads guests into the given list, replacing its current contents. */
    public void loadFromFile(ListInterface<Guest> list) {
        list.clear();
        try {
            DoublyLinkedList<Guest> loaded = JsonFileHandler.loadList(Path.of(FILE_NAME), Guest.class);
            for (int i = 0; i < loaded.size(); i++) {
                list.addBack(loaded.get(i));
            }
        } catch (java.io.IOException e) {
            ConsoleUtil.printError("Failed to load " + FILE_NAME + ": " + e.getMessage());
        }
    }
}
