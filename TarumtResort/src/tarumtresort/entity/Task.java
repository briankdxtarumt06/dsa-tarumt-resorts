/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarumtresort.entity;

//imports
import java.time.LocalDateTime;
import tarumtresort.adt.LinkedListInterface;

/**
 *
 * @author Brian
 */
public class Task {

    private String taskName;
    private String taskType;
    private LinkedListInterface<String> taskStatus;
    private String taskPriority; //TODO: change to enum 1 - High, 2 - Medium, 3 - Low, etc
    private LocalDateTime startDateTime;

    public Task() {
    }

    public Task(String taskName, String taskType, LinkedListInterface<String> taskStatus, String taskPriority, LocalDateTime startDateTime) {
        this.taskName = taskName;
        this.taskType = taskType;
        this.taskStatus = taskStatus;
        this.taskPriority = taskPriority;
        this.startDateTime = startDateTime;
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
        //TODO: add status using stack push logic
    }

    public void rollBackTaskStatus() {
        //TODO: roll back status using stack pop logic
    }

    public String getTaskPriority() {
        return taskPriority;
    }

    public void setTaskPriority(String taskPriority) {
        this.taskPriority = taskPriority;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
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
    
}
