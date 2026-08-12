package tarumtresort.entity;

//imports
import java.time.LocalDateTime;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.enums.TaskPriority;

/**
 *
 * @author Brian
 */
public class Task implements Comparable<Task> {
    private String taskId;
    private String taskName;
    private String taskType;
    private LinkedListInterface<String> taskStatus;
    private TaskPriority taskPriority;
    private LocalDateTime startDateTime;
    private String roomId;

    public Task() {
    }

    public Task(String taskId, String taskName, String taskType, LinkedListInterface<String> taskStatus, TaskPriority taskPriority, LocalDateTime startDateTime, String roomId) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.taskType = taskType;
        this.taskStatus = taskStatus;
        this.taskPriority = taskPriority;
        this.startDateTime = startDateTime;
        this.roomId = roomId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public LinkedListInterface<String> getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(LinkedListInterface<String> taskStatus) {
        this.taskStatus = taskStatus;
    }

    public void addTaskStatus(String taskStatus) {
        // add status using stack push logic
        if (this.taskStatus == null) {
            this.taskStatus = new LinkedList<>();
        }
        this.taskStatus.addBack(taskStatus);
    }

    public String rollBackTaskStatus() {
        // roll back status using stack pop logic
        if (taskStatus == null || taskStatus.isEmpty()) {
            return null;
        }

        return taskStatus.removeBack();
    }

    public String peekTaskStatus() {
        if (taskStatus == null || taskStatus.isEmpty()) {
            return null;
        }

        return taskStatus.getLast();
    }

    public TaskPriority getTaskPriority() {
        return taskPriority;
    }

    public void setTaskPriority(TaskPriority taskPriority) {
        this.taskPriority = taskPriority;
    }

    public void setTaskPriority(String taskPriority) {
        this.taskPriority = TaskPriority.fromString(taskPriority);
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    @Override
    public String toString() {
        return "Task Details:" + 
               "\ntaskName=" + taskName +
               ",\ntaskType=" + taskType + 
               ",\ntaskStatus=" + taskStatus + 
               ",\ntaskPriority=" + taskPriority +
               ",\nstartDateTime=" + startDateTime;
    }

    @Override
    public int compareTo(Task other) {
        // null checks to avoid NullPointerException
        if (other == null) {
            return 1;
        }
        // compare by task priority first, then by start date time
        int priorityCompare = Integer.compare(
                this.taskPriority == null ? TaskPriority.UNKNOWN.getRank() : this.taskPriority.getRank(),
                other.taskPriority == null ? TaskPriority.UNKNOWN.getRank() : other.taskPriority.getRank());
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        if (this.startDateTime == null && other.startDateTime == null) {
            return 0;
        }
        if (this.startDateTime == null) {
            return 1;
        }
        if (other.startDateTime == null) {
            return -1;
        }

        return this.startDateTime.compareTo(other.startDateTime);
    }
}
