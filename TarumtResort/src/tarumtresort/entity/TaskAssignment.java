package tarumtresort.entity;

// imports
import java.time.LocalDateTime;

/**
 *
 * @author Brian
 */
public class TaskAssignment implements Comparable<TaskAssignment> {
    private String taskAssignmentId;
    private String status;
    private LocalDateTime dateTimeAssigned;
    private String assignedStaffId;
    private String assignedTaskId;

    public TaskAssignment() { }

    public TaskAssignment(String taskAssignmentId, String status, LocalDateTime dateTimeAssigned, String assignedStaffId, String assignedTaskId) {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getDateTimeAssigned() {
        return dateTimeAssigned;
    }

    public void setDateTimeAssigned(LocalDateTime dateTimeAssigned) {
        this.dateTimeAssigned = dateTimeAssigned;
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

@Override
    public String toString() {
        return "Task Assignment Details:" + 
               "\nstatus=" + status +
               ",\ndateTimeAssigned=" + dateTimeAssigned + 
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
