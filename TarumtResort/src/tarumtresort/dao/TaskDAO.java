package tarumtresort.dao;

import java.io.IOException;
import java.nio.file.Path;
import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;
import tarumtresort.entity.Task;
import tarumtresort.entity.TaskAssignment;
import tarumtresort.utility.JsonFileHandler;

// Author: Brian Kam Ding Xian
public class TaskDAO {

    private static final Path FILE = Path.of("data/task.json");

    private static final TaskAssignmentDAO TASK_ASSIGNMENT_DAO = new TaskAssignmentDAO();

    public void saveTaskList(ListInterface<Task> taskList) {
        try {
            JsonFileHandler.saveListWithNestedIds(
                    taskList, FILE, "taskAssignments",
                    Task::getTaskAssignments,
                    TaskAssignment::getTaskAssignmentId);
        } catch (IOException e) {
            System.err.println(" ?! Failed to save task data !? : " + e.getMessage());
        }
    }

    public DoublyLinkedList<Task> retrieveTaskList() {
        try {
            return JsonFileHandler.loadListWithNestedIds(
                    FILE, Task.class, "taskAssignments",
                    TASK_ASSIGNMENT_DAO::getTaskAssignmentById,
                    Task::setTaskAssignments);
        } catch (IOException e) {
            System.err.println(" ?! Failed to load task data !? : " + e.getMessage());
            return new LinkedList<>();
        }
    }
}