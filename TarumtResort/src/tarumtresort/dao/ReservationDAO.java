package tarumtresort.dao;

import java.nio.file.Path;
import tarumtresort.adt.LinkedList;
import tarumtresort.entity.Reservation;
import tarumtresort.utility.JsonFileHandler;

public class ReservationDAO {

    private final String BOOKING_LIST_FILE = "data/bookingList.json";
    private final String GUEST_QUEUE_FILE = "data/guestQueue.json";
    private final String ASSIGNED_LIST_FILE = "data/assignedList.json";
    private final LinkedList<Reservation> bookingList = new LinkedList<>();
    private final LinkedList<Reservation> guestQueue = new LinkedList<>();
    private final LinkedList<Reservation> assignedList = new LinkedList<>();

    public void loadBookingListFromFile() {
        guestQueue.clear();
        try {
            LinkedList<Reservation> loaded = JsonFileHandler.loadList(Path.of(BOOKING_LIST_FILE), Reservation.class);
            for (int i = 0; i < loaded.size(); i++) {
                bookingList.addSorted(loaded.get(i));
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to load " + BOOKING_LIST_FILE + ": " + e.getMessage());
        }
    }

    public void saveBookingListToFile() {
        try {
            JsonFileHandler.saveList(bookingList, Path.of(BOOKING_LIST_FILE));
        } catch (java.io.IOException e) {
            System.err.println("Failed to save " + BOOKING_LIST_FILE + ": " + e.getMessage());
        }
    }
    
    public void loadGuestQueueFromFile() {
        guestQueue.clear();
        try {
            LinkedList<Reservation> loaded = JsonFileHandler.loadList(Path.of(GUEST_QUEUE_FILE), Reservation.class);
            for (int i = 0; i < loaded.size(); i++) {
                guestQueue.addSorted(loaded.get(i));
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to load " + GUEST_QUEUE_FILE + ": " + e.getMessage());
        }
    }

    public void saveGuestQueueToFile() {
        try {
            JsonFileHandler.saveList(guestQueue, Path.of(GUEST_QUEUE_FILE));
        } catch (java.io.IOException e) {
            System.err.println("Failed to save " + GUEST_QUEUE_FILE + ": " + e.getMessage());
        }
    }

    public void loadAssignedListFromFile() {
        assignedList.clear();
        try {
            LinkedList<Reservation> loaded = JsonFileHandler.loadList(Path.of(ASSIGNED_LIST_FILE), Reservation.class);
            for (int i = 0; i < loaded.size(); i++) {
                assignedList.addBack(loaded.get(i));
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to load " + ASSIGNED_LIST_FILE + ": " + e.getMessage());
        }
    }

    public void saveAssignedListToFile() {
        try {
            JsonFileHandler.saveList(assignedList, Path.of(ASSIGNED_LIST_FILE));
        } catch (java.io.IOException e) {
            System.err.println("Failed to save " + ASSIGNED_LIST_FILE + ": " + e.getMessage());
        }
    }

    public LinkedList<Reservation> getBookList(){
        return bookingList;
    }

    public LinkedList<Reservation> getGuestQueue() {
        return guestQueue;
    }

    public LinkedList<Reservation> getAssignedList() {
        return assignedList;
    }
}
