package tarumtresort.dao;

import java.nio.file.Path;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Guest;
import tarumtresort.utility.JsonFileHandler;

/**
 * Stateless data access for Guest records, persisted to data/guests.json.
 * Notifications are embedded inside each guest's notificationList, so there
 * is no separate notifications file.
 */
public class GuestDAO {
    private final String FILE_NAME = "data/guestList.json";

    public void saveToFile(LinkedListInterface<Guest> list) {
        try {
            JsonFileHandler.saveList(list, Path.of(FILE_NAME));
        } catch (java.io.IOException e) {
            System.err.println("Failed to save " + FILE_NAME + ": " + e.getMessage());
        }
    }

    public void loadFromFile(LinkedListInterface<Guest> list) {
        list.clear();
        try {
            LinkedList<Guest> loaded = JsonFileHandler.loadList(Path.of(FILE_NAME), Guest.class);
            for (int i = 0; i < loaded.size(); i++) {
                list.addBack(loaded.get(i));
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to load " + FILE_NAME + ": " + e.getMessage());
        }
        return result;
    }
}