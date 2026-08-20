package tarumtresort.entity;

import java.time.LocalDateTime;
import tarumtresort.entity.enums.TaskStatus;

// Author: Brian Kam Ding Xian
public class TaskStatusChange implements Comparable<TaskStatusChange> {
    private TaskStatus taskStatus;
    private String reason;
    private LocalDateTime dateTime;

    public TaskStatusChange() {
    }

    public TaskStatusChange(TaskStatus taskStatus, String reason, LocalDateTime dateTime) {
        this.taskStatus = taskStatus;
        this.reason = reason;
        this.dateTime = dateTime;
    }

    public TaskStatus getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(TaskStatus taskStatus) {
        this.taskStatus = taskStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    @Override
    public int compareTo(TaskStatusChange other) {
        if (other == null) {
            return 1;
        }
        if (dateTime == null) {
            return other.getDateTime() == null ? 0 : -1;
        }
        if (other.getDateTime() == null) {
            return 1;
        }
        return dateTime.compareTo(other.getDateTime());
    }
}