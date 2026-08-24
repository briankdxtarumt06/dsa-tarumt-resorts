package tarumtresort.entity;

// imports
import java.time.LocalDateTime;
import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;
import tarumtresort.entity.enums.TaskStatus;

// Author: Brian Kam Ding Xian
public class TaskAssignment implements Comparable<TaskAssignment> {
    private String taskAssignmentId;
    private TaskStatus status;
    private LocalDateTime dateTimeAssigned;
    private LocalDateTime dateTimeEnded;
    private boolean isDeleted;
    private String assignedStaffId;
    private String assignedTaskId;
    private ListInterface<TaskAssignmentChange> changes;

    public TaskAssignment() { }

    public TaskAssignment(String taskAssignmentId, TaskStatus status, LocalDateTime dateTimeAssigned, String assignedStaffId, String assignedTaskId) {
        this.taskAssignmentId = taskAssignmentId;
        this.status = status;
        this.dateTimeAssigned = dateTimeAssigned;
        this.assignedStaffId = assignedStaffId;
        this.assignedTaskId = assignedTaskId;
    }

    public String getTaskAssignmentId() {
        return taskAssignmentId;
    }

    public void setTaskAssignmentId(String taskAssignmentId) {
        this.taskAssignmentId = taskAssignmentId;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public LocalDateTime getDateTimeAssigned() {
        return dateTimeAssigned;
    }

    public void setDateTimeAssigned(LocalDateTime dateTimeAssigned) {
        this.dateTimeAssigned = dateTimeAssigned;
    }

    public LocalDateTime getDateTimeEnded() {
        return dateTimeEnded;
    }

    public void setDateTimeEnded(LocalDateTime dateTimeEnded) {
        this.dateTimeEnded = dateTimeEnded;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public String getAssignedStaffId() {
        return assignedStaffId;
    }

    public void setAssignedStaffId(String assignedStaffId) {
        this.assignedStaffId = assignedStaffId;
    }

    public String getAssignedTaskId() {
        return assignedTaskId;
    }

    public void setAssignedTaskId(String assignedTaskId) {
        this.assignedTaskId = assignedTaskId;
    }

    public boolean isActive() {
        return status != TaskStatus.COMPLETED && status != TaskStatus.CANCELLED;
    }

    public ListInterface<TaskAssignmentChange> getChanges() {
        if (changes == null) {
            changes = new DoublyLinkedList<>();
        }
        return changes;
    }

    public void setChanges(ListInterface<TaskAssignmentChange> changes) {
        this.changes = changes;
    }

    public void addChange(TaskAssignmentChange change) {
        if (change == null || change.getChangeId() == null) {
            return;
        }
        if (changes == null) {
            changes = new DoublyLinkedList<>();
        }
        if (changes.contains(change)) {
            return; // duplicate change id
        }
        changes.addBack(change);
    }

@Override
    public String toString() {
        return "Task Assignment Details:" + 
               "\nstatus=" + status +
               ",\ndateTimeAssigned=" + dateTimeAssigned + 
               ",\ndateTimeEnded=" + dateTimeEnded +
               ",\nassignedStaffId=" + assignedStaffId + 
               ",\nassignedTaskId=" + assignedTaskId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TaskAssignment)) {
            return false;
        }
        TaskAssignment other = (TaskAssignment) obj;
        if (this.taskAssignmentId == null && other.taskAssignmentId == null) {
            return true;
        }
        if (this.taskAssignmentId == null || other.taskAssignmentId == null) {
            return false;
        }
        return this.taskAssignmentId.equals(other.taskAssignmentId);
    }

    @Override
    public int hashCode() {
        return taskAssignmentId == null ? 0 : taskAssignmentId.hashCode();
    }

    @Override
    public int compareTo(TaskAssignment other) {
        // null checks to avoid NullPointerException
        if (other == null) {
            return 1;
        }
        if (this.dateTimeAssigned == null && other.dateTimeAssigned == null) {
            return 0;
        }
        if (this.dateTimeAssigned == null) {
            return 1;
        }
        if (other.dateTimeAssigned == null) {
            return -1;
        }
        
        return this.dateTimeAssigned.compareTo(other.dateTimeAssigned);
    }
}