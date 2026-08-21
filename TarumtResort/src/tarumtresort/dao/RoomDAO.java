package tarumtresort.dao;

import java.nio.file.Path;
import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;
import tarumtresort.entity.Room;
import tarumtresort.utility.JsonFileHandler;

public class RoomDAO {
    private final String FILE_NAME = "data/roomList.json";

    public void saveToFile(ListInterface<Room> list) {
        try {
            JsonFileHandler.saveList(list, Path.of(FILE_NAME));
        } catch (java.io.IOException e) {
            System.err.println("Failed to save " + FILE_NAME + ": " + e.getMessage());
        }
    }

    public void loadFromFile(ListInterface<Room> list) {
        list.clear();
        try {
            DoublyLinkedList<Room> loaded = JsonFileHandler.loadList(Path.of(FILE_NAME), Room.class);
            for (int i = 0; i < loaded.size(); i++) {
                list.addBack(loaded.get(i));
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to load " + FILE_NAME + ": " + e.getMessage());
        }
    }
}