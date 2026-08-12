package tarumtresort.dao;

import java.io.IOException;
import java.nio.file.Path;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Task;
import tarumtresort.utility.JsonFileHandler;

/**
 *
 * @author Brian
 */
public class TaskDAO {

    private static final Path FILE = Path.of("src", "tarumtresort", "data", "task.json");

    public void saveTaskList(LinkedListInterface<Task> taskList) {
        try {
            JsonFileHandler.saveList(taskList, FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public LinkedList<Task> retrieveTaskList() {
        try {
            return JsonFileHandler.loadList(FILE, Task.class);
        } catch (IOException e) {
            e.printStackTrace();
            return new LinkedList<>();
        }
    }
}