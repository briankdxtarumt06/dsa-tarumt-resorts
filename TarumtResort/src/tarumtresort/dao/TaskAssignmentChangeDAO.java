package tarumtresort.dao;

import java.io.IOException;
import java.nio.file.Path;
import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;
import tarumtresort.entity.TaskAssignmentChange;
import tarumtresort.utility.JsonFileHandler;

// Author: Brian Kam Ding Xian
public class TaskAssignmentChangeDAO {

    private static final Path FILE = Path.of("data/taskassignmentchange.json");

    public void saveTaskAssignmentChangeList(ListInterface<TaskAssignmentChange> changeList) {
        try {
            JsonFileHandler.saveList(changeList, FILE);
        } catch (IOException e) {
            System.err.println("  ✗ Failed to save change history data: " + e.getMessage());
        }
    }

    public DoublyLinkedList<TaskAssignmentChange> retrieveTaskAssignmentChangeList() {
        try {
            return JsonFileHandler.loadList(FILE, TaskAssignmentChange.class);
        } catch (IOException e) {
            System.err.println("  ✗ Failed to load change history data: " + e.getMessage());
            return new DoublyLinkedList<>();
        }
    }

    public TaskAssignmentChange getTaskAssignmentChangeById(String changeId) {
        ListInterface<TaskAssignmentChange> changeList = retrieveTaskAssignmentChangeList();
        for (int i = 0; i < changeList.size(); i++) {
            if (changeId != null && changeId.equals(changeList.get(i).getChangeId())) {
                return changeList.get(i);
            }
        }
        return null;
    }
}