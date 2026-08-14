package tarumtresort.dao;

import java.nio.file.Path;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Reservation;
import tarumtresort.utility.JsonFileHandler;

public class ReservationDAO {
    private final String BOOKING_LIST_FILE = "data/bookingList.json";
    private final String GUEST_QUEUE_FILE = "data/guestQueue.json";
    private final String ASSIGNED_LIST_FILE = "data/assignedList.json";

    public void saveBookingList(LinkedListInterface<Reservation> list) {
        try {
            JsonFileHandler.saveList(list, Path.of(BOOKING_LIST_FILE));
        } catch (java.io.IOException e) {
            System.err.println("Failed to save " + BOOKING_LIST_FILE + ": " + e.getMessage());
        }
    }

    public void loadBookingList(LinkedListInterface<Reservation> list) {
        list.clear();
        try {
            LinkedList<Reservation> loaded = JsonFileHandler.loadList(Path.of(BOOKING_LIST_FILE), Reservation.class);
            for (int i = 0; i < loaded.size(); i++) {
                list.addSorted(loaded.get(i));
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to load " + BOOKING_LIST_FILE + ": " + e.getMessage());
        }
    }

    public void saveGuestQueue(LinkedListInterface<Reservation> list) {
        try {
            JsonFileHandler.saveList(list, Path.of(GUEST_QUEUE_FILE));
        } catch (java.io.IOException e) {
            System.err.println("Failed to save " + GUEST_QUEUE_FILE + ": " + e.getMessage());
        }
    }

    public void loadGuestQueue(LinkedListInterface<Reservation> list) {
        list.clear();
        try {
            LinkedList<Reservation> loaded = JsonFileHandler.loadList(Path.of(GUEST_QUEUE_FILE), Reservation.class);
            for (int i = 0; i < loaded.size(); i++) {
                list.addSorted(loaded.get(i));
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to load " + GUEST_QUEUE_FILE + ": " + e.getMessage());
        }
    }

    public void saveAssignedList(LinkedListInterface<Reservation> list) {
        try {
            JsonFileHandler.saveList(list, Path.of(ASSIGNED_LIST_FILE));
        } catch (java.io.IOException e) {
            System.err.println("Failed to save " + ASSIGNED_LIST_FILE + ": " + e.getMessage());
        }
    }

    public void loadAssignedList(LinkedListInterface<Reservation> list) {
        list.clear();
        try {
            LinkedList<Reservation> loaded = JsonFileHandler.loadList(Path.of(ASSIGNED_LIST_FILE), Reservation.class);
            for (int i = 0; i < loaded.size(); i++) {
                list.addBack(loaded.get(i));
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to load " + ASSIGNED_LIST_FILE + ": " + e.getMessage());
        }
    }
}