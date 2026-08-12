package tarumtresort.dao;

import java.io.IOException;
import java.nio.file.Path;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.TaskAssignment;
import tarumtresort.utility.JsonFileHandler;

/**
 *
 * @author Brian
 */
public class TaskAssignmentDAO {

    private static final Path FILE = Path.of("src", "tarumtresort", "data", "taskassignment.json");

    public void saveTaskAssignmentList(LinkedListInterface<TaskAssignment> taskAssignmentList) {
        try {
            JsonFileHandler.saveList(taskAssignmentList, FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public LinkedList<TaskAssignment> retrieveTaskAssignmentList() {
        try {
            return JsonFileHandler.loadList(FILE, TaskAssignment.class);
        } catch (IOException e) {
            e.printStackTrace();
            return new LinkedList<>();
        }
    }
}