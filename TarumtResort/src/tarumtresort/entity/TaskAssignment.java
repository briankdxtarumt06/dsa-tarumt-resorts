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
    private Staff assingedStaff;
    private Task assignedTask;

    public TaskAssignment() { }

    public TaskAssignment(String taskAssignmentId, String status, LocalDateTime dateTimeAssigned, Staff assingedStaff, Task assignedTask) {
        this.taskAssignmentId = taskAssignmentId;
        this.status = status;
        this.dateTimeAssigned = dateTimeAssigned;
        this.assingedStaff = assingedStaff;
        this.assignedTask = assignedTask;
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

    public Staff getAssingedStaff() {
        return assingedStaff;
    }

    public void setAssingedStaff(Staff assingedStaff) {
        this.assingedStaff = assingedStaff;
    }

    public Task getAssignedTask() {
        return assignedTask;
    }

    public void setAssignedTask(Task assignedTask) {
        this.assignedTask = assignedTask;
    }

@Override
    public String toString() {
        return "Task Assignment Details:" + 
               "\nstatus=" + status +
               ",\ndateTimeAssigned=" + dateTimeAssigned + 
               ",\nassingedStaff=" + assingedStaff + 
               ",\nassignedTask=" + assignedTask;
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
