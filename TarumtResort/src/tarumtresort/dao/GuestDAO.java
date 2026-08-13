package tarumtresort.dao;

import java.nio.file.Path;
import tarumtresort.adt.LinkedList;
import tarumtresort.entity.Guest;
import tarumtresort.utility.JsonFileHandler;

public class GuestDAO {

    private final String FILE_NAME = "data/guestList.json";
    private final LinkedList<Guest> guests = new LinkedList<>();

    public void LoadFromFile() {
        guests.clear();

        try {
            LinkedList<Guest> loaded =
                    JsonFileHandler.loadList(
                            Path.of(FILE_NAME),
                            Guest.class
                    );

            for (int i = 0; i < loaded.size(); i++) {
                guests.addBack(loaded.get(i));
            }

        } catch (java.io.IOException e) {
            System.err.println(
                    "Failed to load " + FILE_NAME + ": " + e.getMessage()
            );
        }
    }

    public void SaveToFile() {
        try {
            JsonFileHandler.saveList(
                    guests,
                    Path.of(FILE_NAME)
            );

        } catch (java.io.IOException e) {
            System.err.println(
                    "Failed to save " + FILE_NAME + ": " + e.getMessage()
            );
        }
    }
}