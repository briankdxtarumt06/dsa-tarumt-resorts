package tarumtresort.entity;

// imports
import java.time.LocalDateTime;

/**
 *
 * @author Brian
 *
 * Append-only history record for task / assignment changes.
 * Every task status change and every worker assignment status change is
 * recorded here (status + staff involved + date & time), separate from the
 * TaskAssignment records which only hold the current assignment state.
 *
 * Staff and task are linked by id only (no duplicate data); the calling
 * module resolves the ids to full entities when it needs the details.
 */
public class TaskAssignmentChange implements Comparable<TaskAssignmentChange> {
    private String changeId;
    private String taskAssignmentId;
    private String status;
    private String staffId;
    private String taskId;
    private LocalDateTime changedAt;

    public TaskAssignmentChange() { }

    public TaskAssignmentChange(String changeId, String taskAssignmentId, String status, LocalDateTime changedAt, String staffId, String taskId) {
        this.changeId = changeId;
        this.taskAssignmentId = taskAssignmentId;
        this.status = status;
        this.changedAt = changedAt;
        this.staffId = staffId;
        this.taskId = taskId;
    }

    public String getChangeId() {
        return changeId;
    }

    public void setChangeId(String changeId) {
        this.changeId = changeId;
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

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    @Override
    public String toString() {
        return "Task Assignment Change Details:" +
               "\nchangeId=" + changeId +
               ",\ntaskAssignmentId=" + taskAssignmentId +
               ",\nstatus=" + status +
               ",\nchangedAt=" + changedAt +
               ",\nstaffId=" + staffId +
               ",\ntaskId=" + taskId;
    }

    @Override
    public int compareTo(TaskAssignmentChange other) {
        // null checks to avoid NullPointerException
        if (other == null) {
            return 1;
        }
        if (this.changedAt == null && other.changedAt == null) {
            return 0;
        }
        if (this.changedAt == null) {
            return 1;
        }
        if (other.changedAt == null) {
            return -1;
        }

        return this.changedAt.compareTo(other.changedAt);
    }
}