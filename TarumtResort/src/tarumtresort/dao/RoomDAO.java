package tarumtresort.dao;

import java.nio.file.Path;
import tarumtresort.adt.LinkedList;
import tarumtresort.entity.Room;
import tarumtresort.utility.JsonFileHandler;

public class RoomDAO {
    
    private final String FILE_NAME = "data/roomList.json";
    private final LinkedList<Room> roomList = new LinkedList<>();

    public void loadFromFile() {
        roomList.clear();
        try {
            LinkedList<Room> loaded = JsonFileHandler.loadList(Path.of(FILE_NAME), Room.class);
            for (int i = 0; i < loaded.size(); i++) {
                roomList.addBack(loaded.get(i));
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to load " + FILE_NAME + ": " + e.getMessage());
        }
    }

    public void saveToFile() {
        try {
            JsonFileHandler.saveList(roomList, Path.of(FILE_NAME));
        } catch (java.io.IOException e) {
            System.err.println("Failed to save " + FILE_NAME + ": " + e.getMessage());
        }
    }

    public LinkedList<Room> getRoomList() {
        return roomList;
    }
}
