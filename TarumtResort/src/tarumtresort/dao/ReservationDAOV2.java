package tarumtresort.dao;

import java.nio.file.Path;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Reservation;
import tarumtresort.utility.JsonFileHandler;

public class ReservationDAOV2 {
    private final String ALL_RESERVATIONS_FILE = "data/allReservationList.json";

    public void saveAllReservations(LinkedListInterface<Reservation> all) {
        try {
            JsonFileHandler.saveList(all, Path.of(ALL_RESERVATIONS_FILE));
        } catch (java.io.IOException e) {
            System.err.println("Failed to save: " + e.getMessage());
        }
    }

    public void loadAllReservations(LinkedListInterface<Reservation> list) {
        list.clear();
        try {
            LinkedList<Reservation> loaded = JsonFileHandler.loadList(
                    Path.of(ALL_RESERVATIONS_FILE), Reservation.class);
            for (int i = 0; i < loaded.size(); i++) {
                list.addBack(loaded.get(i));
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to load: " + e.getMessage());
        }
    }

}