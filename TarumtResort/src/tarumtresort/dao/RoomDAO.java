package tarumtresort.dao;

import java.nio.file.Path;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Room;
import tarumtresort.utility.JsonFileHandler;

/**
 *
 * @author Brian
 */
public class RoomDAO {

    private static final Path FILE = Path.of("data/rooms.json");

    public void saveRoomList(LinkedListInterface<Room> roomList) {
        try {
            JsonFileHandler.saveList(roomList, FILE);
        } catch (java.io.IOException e) {
            System.err.println("  ✗ Failed to save room data: " + e.getMessage());
        }
    }

    public LinkedList<Room> retrieveRoomList() {
        try {
            return JsonFileHandler.loadList(FILE, Room.class);
        } catch (java.io.IOException e) {
            System.err.println("  ✗ Failed to load room data: " + e.getMessage());
            return new LinkedList<>();
        }
    }

    public Room getRoomById(String roomId) {
        LinkedListInterface<Room> roomList = retrieveRoomList();
        for (int i = 0; i < roomList.size(); i++) {
            if (roomList.get(i).getRoomId().equals(roomId)) {
                return roomList.get(i);
            }
        }
        return null;
    }
}