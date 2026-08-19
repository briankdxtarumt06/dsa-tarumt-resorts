package tarumtresort.dao;

import java.io.IOException;
import java.nio.file.Path;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.TaskAssignment;
import tarumtresort.entity.TaskAssignmentChange;
import tarumtresort.utility.JsonFileHandler;

/**
 *
 * @author Brian
 */
public class TaskAssignmentDAO {

    private static final Path FILE = Path.of("data/taskassignment.json");

    private static final TaskAssignmentChangeDAO CHANGE_DAO = new TaskAssignmentChangeDAO();

    public void saveTaskAssignmentList(LinkedListInterface<TaskAssignment> taskAssignmentList) {
        try {
            JsonFileHandler.saveListWithNestedIds(
                    taskAssignmentList, FILE, "changes",
                    TaskAssignment::getChanges,
                    TaskAssignmentChange::getChangeId);
        } catch (IOException e) {
            System.err.println("  ✗ Failed to save assignment data: " + e.getMessage());
        }
    }

    public LinkedList<TaskAssignment> retrieveTaskAssignmentList() {
        try {
            return JsonFileHandler.loadListWithNestedIds(
                    FILE, TaskAssignment.class, "changes",
                    CHANGE_DAO::getTaskAssignmentChangeById,
                    TaskAssignment::setChanges);
        } catch (IOException e) {
            System.err.println("  ✗ Failed to load assignment data: " + e.getMessage());
            return new LinkedList<>();
        }
    }

    public TaskAssignment getTaskAssignmentById(String taskAssignmentId) {
        LinkedListInterface<TaskAssignment> assignmentList = retrieveTaskAssignmentList();
        for (int i = 0; i < assignmentList.size(); i++) {
            if (assignmentList.get(i).getTaskAssignmentId().equals(taskAssignmentId)) {
                return assignmentList.get(i);
            }
        }
        return null;
    }
}