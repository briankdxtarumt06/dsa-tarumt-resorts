package tarumtresort.dao;

import java.nio.file.Path;
import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;
import tarumtresort.entity.*;
import tarumtresort.utility.JsonFileHandler;

// Author: Lee Boon Yew
public class PriorityReservationDAO {
    private final String FILE_NAME = "data/priorityReservations.json";
    private final ListInterface<PriorityReservation> priorityReservations = new DoublyLinkedList<>();

    public ListInterface<PriorityReservation> loadFromFile() {
        priorityReservations.clear();
        try {
            DoublyLinkedList<PriorityReservation> loaded = JsonFileHandler.loadList(Path.of(FILE_NAME), PriorityReservation.class);
            for (int i = 0; i < loaded.size(); i++) {
                priorityReservations.addBack(loaded.get(i));
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to load " + FILE_NAME + ": " + e.getMessage());
        }

        return priorityReservations;
    }

    public void saveToFile(ListInterface<PriorityReservation> list) {
        try {
            JsonFileHandler.saveList(list, Path.of(FILE_NAME));
        } catch (java.io.IOException e) {
            System.err.println("Failed to save " + FILE_NAME + ": " + e.getMessage());
        }
    }

}