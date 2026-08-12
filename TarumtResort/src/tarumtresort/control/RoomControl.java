package tarumtresort.control;

import tarumtresort.dao.RoomDAO;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.adt.LinkedList;
import tarumtresort.entity.Room;
import tarumtresort.entity.RoomStatus;
import tarumtresort.entity.RoomType;

public class RoomControl {
    private static final RoomDAO roomDAO = new RoomDAO();
    
    // list declared
    private LinkedListInterface<Room> roomList = new LinkedList<>();

    // Constructor
    public RoomControl() {
        roomList = roomDAO.retrieveRoomList();
    }

    //get room by room id
    public Room getRoomById(String roomId) {

        for (int i = 0; i < roomList.size(); i++) {

            Room room = roomList.get(i);

            if (room.getRoomId().equals(roomId)) {
                return room;
            }
        }

        return null;
    }

    // get the available room by the room type 
    public Room getAvailableRoom(RoomType roomType) {
        for (int i = 0; i < roomList.size(); i++) {

            Room room = roomList.get(i);

            if (room.getRoomType() == roomType && room.getRoomStatus() == RoomStatus.AVAILABLE) {
                return room;
            }
        }

        return null;
    }

    // update room status
    public boolean updateRoomStatus(String roomId, RoomStatus roomStatus) {

        for (int i = 0; i < roomList.size(); i++) {

            Room room = roomList.get(i);

            if (room.getRoomId().equals(roomId)) {

                room.setRoomStatus(roomStatus);

                // roomDAO.saveRoomList(roomList);

                return true;
            }
        }

        return false;
    }

    // get all available rooms
    public LinkedListInterface<Room> getAvailableRooms() {

        LinkedListInterface<Room> availableRooms = new LinkedList<>();

        for (int i = 0; i < roomList.size(); i++) {

            Room room = roomList.get(i);

            if (room.getRoomStatus() == RoomStatus.AVAILABLE) {

                availableRooms.addBack(room);
            }
        }

        return availableRooms;
    }

    // get all occupied rooms
    public LinkedListInterface<Room> getOccupiedRooms() {

        LinkedListInterface<Room> occupiedRooms = new LinkedList<>();

        for (int i = 0; i < roomList.size(); i++) {

            Room room = roomList.get(i);

            if (room.getRoomStatus() == RoomStatus.OCCUPIED) {

                occupiedRooms.addBack(room);
            }
        }

        return occupiedRooms;
    }

    // get rooms by room type
    public LinkedListInterface<Room> getRoomsByType(RoomType roomType) {

        LinkedListInterface<Room> roomTypeList = new LinkedList<>();

        for (int i = 0; i < roomList.size(); i++) {

            Room room = roomList.get(i);

            if (room.getRoomType() == roomType) {
                roomTypeList.addBack(room);
            }
        }

        return roomTypeList;
    }

    // get all the rooms
    public LinkedListInterface<Room> getAllRooms() {
        return roomList;
    }

    public boolean roomExists(String roomId) {
        return getRoomById(roomId) != null;
    }

    public boolean isRoomAvailable(String roomId) {
        Room room = getRoomById(roomId);
        return room != null && room.getRoomStatus() == RoomStatus.AVAILABLE;
    }

    public RoomStatus getRoomStatus(String roomId) {
        Room room = getRoomById(roomId);
        if (room == null) {
            return null;
        }
        return room.getRoomStatus();
    }

    public RoomType getRoomType(String roomId) {
        Room room = getRoomById(roomId);
        if (room == null) {
            return null;
        }
        return room.getRoomType();
    }

    public double getRoomPrice(String roomId) {
        Room room = getRoomById(roomId);

        if (room == null) {
            return -1;
        }

        return room.getPricePerNight();
    }
}
