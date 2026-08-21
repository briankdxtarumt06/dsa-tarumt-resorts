package tarumtresort.dao;

import java.nio.file.Path;
import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;
import tarumtresort.entity.Reservation;
import tarumtresort.utility.JsonFileHandler;

// Author: Chai Chee Tong

public class ReservationDAO {
    private final String ALL_RESERVATIONS_FILE = "data/allReservationList.json";

    public void saveAllReservations(ListInterface<Reservation> all) {
        try {
            JsonFileHandler.saveList(all, Path.of(ALL_RESERVATIONS_FILE));
        } catch (java.io.IOException e) {
            System.err.println("Failed to save: " + e.getMessage());
        }
    }

    public void loadAllReservations(ListInterface<Reservation> list) {
        list.clear();
        try {
            DoublyLinkedList<Reservation> loaded = JsonFileHandler.loadList(
                    Path.of(ALL_RESERVATIONS_FILE), Reservation.class);
            for (int i = 0; i < loaded.size(); i++) {
                list.addBack(loaded.get(i));
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to load: " + e.getMessage());
        }
    }

}