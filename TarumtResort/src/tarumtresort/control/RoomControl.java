package tarumtresort.control;

import tarumtresort.dao.RoomDAO;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.boundary.GuestUI;
import tarumtresort.boundary.RoomUI;
import tarumtresort.adt.LinkedList;
import tarumtresort.entity.Room;
import tarumtresort.entity.enums.RoomStatus;
import tarumtresort.entity.enums.RoomType;
import java.time.LocalDate;

public class RoomControl {
    private static final RoomDAO roomDAO = new RoomDAO();
    
    // list declared
    private LinkedListInterface<Room> roomList = new LinkedList<>();

    // UI
    private RoomUI roomUI = new RoomUI();

    // Constructor
    public RoomControl() {
        roomDAO.loadFromFile(roomList);
    }

    private static final int PAGE_SIZE = 20;

    public void runRoomManagement() {
        RoomType typeFilter = null;
        RoomStatus statusFilter = null;
        int page = 0;

        while (true) {
            LinkedListInterface<Room> display;
            if (typeFilter != null) {
                display = getRoomsByType(typeFilter);
            } else if (statusFilter != null) {
                display = getRoomsByStatus(statusFilter);
            } else {
                display = roomList;
            }

            boolean hasFilter = typeFilter != null || statusFilter != null;
            int pageCount = Math.max(1, (display.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            if (page >= pageCount) {
                page = pageCount - 1;
            }

            LinkedListInterface<Room> pageList = pageOf(display, page);
            int choice = roomUI.printRoomListMenu(pageList, page, pageCount, hasFilter);

            if (choice == 0) break;

            int action = 1;
            if (choice == action++) { // View Details
                viewRoom(pageList);
            } else if (choice == action++) { // Filter by Room Type
                int typeChoice = roomUI.inputRoomTypeChoice();
                if (typeChoice != 0) {
                    typeFilter = intToRoomType(typeChoice);
                    statusFilter = null;
                    page = 0;
                }
            } else if (choice == action++) { // Filter by Room Status
                int statusChoice = roomUI.inputRoomStatusChoice();
                if (statusChoice != 0) {
                    statusFilter = intToRoomStatus(statusChoice);
                    typeFilter = null;
                    page = 0;
                }
            } else {
                boolean matched = false;
                if (page < pageCount - 1) {
                    matched = choice == action;
                    action++;
                    if (matched) page++;
                }
                if (!matched && page > 0) {
                    matched = choice == action;
                    action++;
                    if (matched) page--;
                }
                if (!matched && hasFilter) {
                    matched = choice == action;
                    action++;
                    if (matched) {
                        typeFilter = null;
                        statusFilter = null;
                        page = 0;
                    }
                }
            }
        }
    }

    private LinkedListInterface<Room> pageOf(LinkedListInterface<Room> source, int page) {
        LinkedListInterface<Room> result = new LinkedList<>();
        int startIndex = page * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, source.size());
        for (int i = startIndex; i < endIndex; i++) {
            result.addBack(source.get(i));
        }
        return result;
    }

    private void viewRoom(LinkedListInterface<Room> pageList) {
        if (pageList.isEmpty()) {
            roomUI.printNoRecords();
            roomUI.pressEnterToContinue();
            return;
        }
        int num = roomUI.inputListIndex("room", pageList.size());
        if (num == 0) return;
        Room room = pageList.get(num - 1);
        if (room != null) {
            roomUI.printRoomDetails(room);
            roomUI.pressEnterToContinue();
        }
    }

    public LinkedListInterface<Room> getRoomsByStatus(RoomStatus status) {
        LinkedListInterface<Room> result = new LinkedList<>();
        for (int i = 0; i < roomList.size(); i++) {
            if (roomList.get(i).getRoomStatus() == status) {
                result.addBack(roomList.get(i));
            }
        }
        return result;
    }

    public RoomType intToRoomType(int choice) {
        switch (choice) {
            case 1: return RoomType.STANDARD_SINGLE;
            case 2: return RoomType.STANDARD_DOUBLE;
            case 3: return RoomType.STANDARD_TRIPLE;
            case 4: return RoomType.DELUXE_SINGLE;
            case 5: return RoomType.DELUXE_DOUBLE;
            case 6: return RoomType.DELUXE_TRIPLE;
            case 7: return RoomType.SUITE;
            default: return null;
        }
    }

    private RoomStatus intToRoomStatus(int choice) {
        switch (choice) {
            case 1: return RoomStatus.AVAILABLE;
            case 2: return RoomStatus.OCCUPIED;
            case 3: return RoomStatus.CLEANING;
            default: return null;
        }
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
        LinkedListInterface<Room> roomsOfType = getRoomsByType(roomType);
        for (int i = 0; i < roomsOfType.size(); i++) {
            Room room = roomsOfType.get(i);
            if (room.getRoomStatus() == RoomStatus.AVAILABLE) {
                return room;
            }
        }
        return null;
    }

    // get all AVAILABLE rooms of a given room type
    public LinkedListInterface<Room> getAvailableRoomsByType(RoomType roomType) {
        LinkedListInterface<Room> result = new LinkedList<>();
        LinkedListInterface<Room> roomsOfType = getRoomsByType(roomType);
        for (int i = 0; i < roomsOfType.size(); i++) {
            Room room = roomsOfType.get(i);
            if (room.getRoomStatus() == RoomStatus.AVAILABLE) {
                result.addBack(room);
            }
        }
        return result;
    }

    // count how many physical rooms exist for a given room type
    public int countRoomsByType(RoomType roomType) {
        return getRoomsByType(roomType).size();
    }

    // update room status
    public boolean updateRoomStatus(String roomId, RoomStatus roomStatus) {
        Room room = getRoomById(roomId);
        if (room == null) {
            return false;
        }

        room.setRoomStatus(roomStatus);
        roomDAO.saveToFile(roomList);

        return true;
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

    public double getPriceByRoomType(RoomType roomType) {
        LinkedListInterface<Room> roomsOfType = getRoomsByType(roomType);
        if (roomsOfType.isEmpty()) {
            return -1;
        }
        return roomsOfType.get(0).getPricePerNight();
    }

    public void saveRoomList() {
        roomDAO.saveToFile(roomList);
    }
}