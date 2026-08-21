package tarumtresort.dao;

import java.nio.file.Path;
import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;
import tarumtresort.entity.Member;
import tarumtresort.utility.ConsoleUtil;
import tarumtresort.utility.JsonFileHandler;

// Author: Imam Mahdi Ali Ang Attuko
public class MemberDAO {
    private final String FILE_NAME = "data/members.json";

    public void saveToFile(ListInterface<Member> list) {
        try {
            JsonFileHandler.saveList(list, Path.of(FILE_NAME));
        } catch (java.io.IOException e) {
            ConsoleUtil.printError("Failed to save " + FILE_NAME + ": " + e.getMessage());
        }
    }

    public DoublyLinkedList<Member> retrieveFromFile() {
        DoublyLinkedList<Member> result = new DoublyLinkedList<>();
        try {
            DoublyLinkedList<Member> loaded = JsonFileHandler.loadList(Path.of(FILE_NAME), Member.class);
            for (int i = 0; i < loaded.size(); i++) {
                result.addBack(loaded.get(i));
            }
        } catch (java.io.IOException e) {
            ConsoleUtil.printError("Failed to load " + FILE_NAME + ": " + e.getMessage());
        }
        return result;
    }
}
