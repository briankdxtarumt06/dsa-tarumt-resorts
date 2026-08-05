/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarumtresort.entity;

// imports
import java.time.LocalDateTime;

/**
 *
 * @author Brian
 */
public class TaskAssignment {
    private String status;
    private LocalDateTime timestamp;
    private Staff assingedStaff;
    private Task assignedTask;

    public TaskAssignment() { }

    public TaskAssignment(String status, LocalDateTime timestamp, Staff assingedStaff, Task assignedTask) {
        this.status = status;
        this.timestamp = timestamp;
        this.assingedStaff = assingedStaff;
        this.assignedTask = assignedTask;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
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
               ",\ntimestamp=" + timestamp + 
               ",\nassingedStaff=" + assingedStaff + 
               ",\nassignedTask=" + assignedTask;
    }
    
}
