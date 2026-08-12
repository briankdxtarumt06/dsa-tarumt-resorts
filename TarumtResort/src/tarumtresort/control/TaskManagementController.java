package tarumtresort.control;

import java.time.LocalDateTime;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.boundary.TaskManagementUI;
import tarumtresort.dao.TaskDAO;
import tarumtresort.entity.Task;
import tarumtresort.entity.enums.TaskPriority;

/**
 *
 * @author Brian
 */
public class TaskManagementController {

    // controller declaration
    // ui declaration
    private TaskManagementUI ui;

    // list declaration
    private LinkedListInterface<Task> taskList = new LinkedList<>();

    // dao declaration
    private static final TaskDAO taskDAO = new TaskDAO();

    // constructor
    public TaskManagementController() {
        taskList = taskDAO.retrieveTaskList();
    }

    public TaskManagementController(TaskManagementUI ui) {
        this.ui = ui;
        taskList = taskDAO.retrieveTaskList();
    }

    // task management
    public void runTaskManagement() {

        int choice;

        do {
            choice = ui.getMenuChoice();

            switch (choice) {
                case 1:
                    addTask();
                    break;
                case 2:
                    ui.listAllTasks(taskListToTable(getAllTasks()));
                    break;
                case 3:
                    searchTask();
                    break;
                case 4:
                    updateTask();
                    break;
                case 5:
                    updateTaskStatus();
                    break;
                case 6:
                    rollBackTaskStatus();
                    break;
                case 7:
                    removeTask();
                    break;
                case 8:
                    filterTaskByPriority();
                    break;
                case 9:
                    filterTaskByType();
                    break;
                case 0:
                    ui.printExitMessage();
                    break;
                default:
                    ui.printInvalidChoice();
            }

            if (choice != 0) {
                ui.pressEnterToContinue();
            }
        } while (choice != 0);
    }

    private void addTask() {
        String[] details = ui.inputTaskDetails();
        String taskId = createTask(details[0], details[1], TaskPriority.fromString(details[2]), ui.parseDateTime(details[3]));
        if (taskId == null) {
            ui.printDuplicateName();
        } else {
            ui.printTaskId(taskId);
            ui.printSuccess();
        }
    }

    private void searchTask() {
        int searchChoice = ui.getSearchMenuChoice();
        if (searchChoice == 0) {
            return;
        }
        Task task = null;
        if (searchChoice == 1) {
            task = getTaskById(ui.inputTaskId());
        } else if (searchChoice == 2) {
            task = getTaskByName(ui.inputTaskName());
        }
        if (task == null) {
            ui.printNotFound();
        } else {
            ui.printTaskDetails(task);
        }
    }

    private void updateTask() {
        String taskId = ui.inputTaskId();
        if (!taskExists(taskId)) {
            ui.printNotFound();
            return;
        }
        String[] details = ui.inputUpdateTaskDetails();
        if (updateTask(taskId, details[0], details[1], TaskPriority.fromString(details[2]), ui.parseDateTime(details[3]))) {
            ui.printSuccess();
        } else {
            ui.printNotFound();
        }
    }

    private void updateTaskStatus() {
        String taskId = ui.inputTaskId();
        if (!taskExists(taskId)) {
            ui.printNotFound();
            return;
        }
        if (updateTaskStatus(taskId, ui.inputTaskStatus())) {
            ui.printSuccess();
        } else {
            ui.printNotFound();
        }
    }

    private void rollBackTaskStatus() {
        String taskId = ui.inputTaskId();
        if (rollBackTaskStatus(taskId) == null) {
            ui.printNoStatusToRollBack();
        } else {
            ui.printSuccess();
        }
    }

    private void removeTask() {
        String taskId = ui.inputTaskId();
        if (removeTask(taskId)) {
            ui.printSuccess();
        } else {
            ui.printNotFound();
        }
    }

    private void filterTaskByPriority() {
        TaskPriority priority = ui.inputTaskPriority();
        ui.listAllTasks(taskListToTable(getTasksByPriority(priority)));
    }

    private void filterTaskByType() {
        String taskType = ui.inputTaskType();
        ui.listAllTasks(taskListToTable(getTasksByType(taskType)));
    }

    private String[][] taskListToTable(LinkedListInterface<Task> taskList) {
        String[][] data = new String[taskList.size() + 1][6];
        data[0] = new String[]{"Task ID", "Task Name", "Task Type", "Priority", "Current Status", "Start Date & Time"};
        for (int i = 0; i < taskList.size(); i++) {
            Task task = taskList.get(i);
            data[i + 1] = new String[]{
                task.getTaskId(),
                task.getTaskName(),
                task.getTaskType(),
                task.getTaskPriority() == null ? "-" : task.getTaskPriority().name(),
                task.peekTaskStatus() == null ? "-" : task.peekTaskStatus(),
                task.getStartDateTime() == null ? "-" : task.getStartDateTime().toString()
            };
        }
        return data;
    }

    public String createTask(String taskName, String taskType, TaskPriority taskPriority, LocalDateTime startDateTime) {

        for (int i = 0; i < taskList.size(); i++) {
            if (taskList.get(i).getTaskName().equalsIgnoreCase(taskName)) {
                return null;
            }
        }

        String taskId = generateTaskId();

        Task task = new Task(
                taskId,
                taskName,
                taskType,
                null,
                taskPriority,
                startDateTime
        );

        task.addTaskStatus("Pending");
        taskList.addSorted(task);
        taskDAO.saveTaskList(taskList);

        return taskId;
    }

    // update task
    public boolean updateTask(String taskId,
                            String taskName,
                            String taskType,
                            TaskPriority taskPriority,
                            LocalDateTime startDateTime) {

        for (int i = 0; i < taskList.size(); i++) {

            Task task = taskList.get(i);

            if (task.getTaskId().equals(taskId)) {

                task.setTaskName(taskName);
                task.setTaskType(taskType);
                task.setTaskPriority(taskPriority);
                task.setStartDateTime(startDateTime);

                taskDAO.saveTaskList(taskList);

                return true;
            }
        }

        return false;
    }

    public boolean updateTaskStatus(String taskId, String status) {

        for (int i = 0; i < taskList.size(); i++) {

            Task task = taskList.get(i);

            if (task.getTaskId().equals(taskId)) {

                task.addTaskStatus(status);
                taskDAO.saveTaskList(taskList);

                return true;
            }
        }

        return false;
    }

    public String rollBackTaskStatus(String taskId) {

        for (int i = 0; i < taskList.size(); i++) {

            Task task = taskList.get(i);

            if (task.getTaskId().equals(taskId)) {

                String rolledBack = task.rollBackTaskStatus();
                taskDAO.saveTaskList(taskList);

                return rolledBack;
            }
        }

        return null;
    }

    public boolean removeTask(String taskId) {

        for (int i = 0; i < taskList.size(); i++) {

            Task task = taskList.get(i);

            if (task.getTaskId().equals(taskId)) {

                task.addTaskStatus("Cancelled"); // soft delete
                taskDAO.saveTaskList(taskList);

                return true;
            }
        }

        return false;
    }

    public Task getTaskById(String taskId) {

        for (int i = 0; i < taskList.size(); i++) {

            Task task = taskList.get(i);

            if (task.getTaskId().equals(taskId)) {
                return task;
            }
        }

        return null;
    }

    public Task getTaskByName(String taskName) {

        for (int i = 0; i < taskList.size(); i++) {

            Task task = taskList.get(i);

            if (task.getTaskName().equalsIgnoreCase(taskName)) {
                return task;
            }
        }

        return null;
    }

    public LinkedListInterface<Task> getTasksByPriority(TaskPriority taskPriority) {

        LinkedListInterface<Task> filteredList = new LinkedList<>();

        for (int i = 0; i < taskList.size(); i++) {

            Task task = taskList.get(i);

            if (task.getTaskPriority() == taskPriority) {
                filteredList.addBack(task);
            }
        }

        return filteredList;
    }

    public LinkedListInterface<Task> getTasksByType(String taskType) {

        LinkedListInterface<Task> filteredList = new LinkedList<>();

        for (int i = 0; i < taskList.size(); i++) {

            Task task = taskList.get(i);

            if (task.getTaskType().equalsIgnoreCase(taskType)) {
                filteredList.addBack(task);
            }
        }

        return filteredList;
    }

    public LinkedListInterface<Task> getAllTasks() {
        return taskList;
    }

    private String generateTaskId() {

        int max = 0;

        for (int i = 0; i < taskList.size(); i++) {

            String taskId = taskList.get(i).getTaskId();

            int number = Integer.parseInt(taskId.substring(3));

            if (number > max) {
                max = number;
            }
        }

        return String.format("TSK%03d", max + 1);
    }

    public boolean taskExists(String taskId) {
        return getTaskById(taskId) != null;
    }
}