package tarumtresort.dao;

import java.nio.file.Path;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.*;
import tarumtresort.utility.JsonFileHandler;

public class PriorityReservationDAO {
    private final String FILE_NAME = "data/priorityReservations.json";
    private final LinkedListInterface<PriorityReservation> priorityReservations = new LinkedList<>();

    public LinkedListInterface<PriorityReservation> loadFromFile() {
        priorityReservations.clear();
        try {
            LinkedList<PriorityReservation> loaded = JsonFileHandler.loadList(Path.of(FILE_NAME), PriorityReservation.class);
            for (int i = 0; i < loaded.size(); i++) {
                priorityReservations.addBack(loaded.get(i));
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to load " + FILE_NAME + ": " + e.getMessage());
        }

        return priorityReservations;
    }

    public void saveToFile(LinkedListInterface<PriorityReservation> list) {
        try {
            JsonFileHandler.saveList(list, Path.of(FILE_NAME));
        } catch (java.io.IOException e) {
            System.err.println("Failed to save " + FILE_NAME + ": " + e.getMessage());
        }
    }

}