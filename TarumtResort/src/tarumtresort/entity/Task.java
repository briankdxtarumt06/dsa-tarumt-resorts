package tarumtresort.entity;

//imports
import java.time.LocalDateTime;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.enums.TaskPriority;
import tarumtresort.entity.enums.TaskStatus;
import tarumtresort.entity.enums.TaskType;

/**
 *
 * @author Brian
 */
public class Task implements Comparable<Task> {
    private String taskId;
    private String taskName;
    private TaskType taskType;
    private TaskStatus taskStatus;
    private TaskPriority taskPriority;
    private LocalDateTime startDateTime;
    private String roomId;
    private boolean isDeleted;
    private LinkedListInterface<TaskAssignment> taskAssignments;
    private LinkedListInterface<TaskStatusChange> statusHistory;
    // update task status -> push (addFront)
    // rollback task status -> pop (removeFront)
    // get current status -> peek (getFront)

    public Task() {
    }

    public Task(String taskId, String taskName, TaskType taskType, TaskStatus taskStatus, TaskPriority taskPriority, LocalDateTime startDateTime, String roomId) {
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

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = TaskType.fromString(taskType);
    }

    public TaskStatus getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(TaskStatus taskStatus) {
        this.taskStatus = taskStatus;
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

    public LinkedListInterface<TaskAssignment> getTaskAssignments() {
        if (taskAssignments == null) {
            taskAssignments = new LinkedList<>();
        }
        return taskAssignments;
    }

    public void setTaskAssignments(LinkedListInterface<TaskAssignment> taskAssignments) {
        this.taskAssignments = taskAssignments;
    }

    public void addTaskAssignment(TaskAssignment taskAssignment) {
        if (taskAssignment == null || taskAssignment.getTaskAssignmentId() == null) {
            return;
        }
        if (taskAssignments == null) {
            taskAssignments = new LinkedList<>();
        }
        if (taskAssignments.contains(taskAssignment)) {
            return; // duplicate assignment id
        }
        taskAssignments.addBack(taskAssignment);
    }

    public boolean removeTaskAssignment(TaskAssignment taskAssignment) {
        if (taskAssignment == null || taskAssignment.getTaskAssignmentId() == null) {
            return false;
        }
        return taskAssignments != null && taskAssignments.removeElement(taskAssignment);
    }

    public LinkedListInterface<TaskStatusChange> getStatusHistory() {
        if (statusHistory == null) {
            statusHistory = new LinkedList<>();
        }
        return statusHistory;
    }

    public void setStatusHistory(LinkedListInterface<TaskStatusChange> statusHistory) {
        this.statusHistory = statusHistory;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
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