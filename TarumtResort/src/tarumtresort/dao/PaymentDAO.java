package tarumtresort.dao;

import tarumtresort.adt.*;
import tarumtresort.entity.*;
import tarumtresort.utility.JsonFileHandler;
import java.nio.file.Path;

public class PaymentDAO {
    private final String FILE_NAME = "data/payments.json";

    public void saveToFile(ListInterface<Payment> list) {
        try {
            JsonFileHandler.saveList(list, Path.of(FILE_NAME));
        } catch (java.io.IOException e) {
            System.err.println("Failed to save: " + e.getMessage());
        }
    }

    public void loadFromFile(ListInterface<Payment> list) {
        list.clear();
        try {
            DoublyLinkedList<Payment> loaded = JsonFileHandler.loadList(Path.of(FILE_NAME), Payment.class);
            for (int i = 0; i < loaded.size(); i++) {
                list.addBack(loaded.get(i));
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to load: " + e.getMessage());
        }
    }
}
