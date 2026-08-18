package tarumtresort.entity;

import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;

/**
 *
 * @author Brian
 */
public class Staff implements Comparable<Staff> {
    private String staffId;
    private String staffName;
    private String department;
    private String staffRole;
    private String availabilityStatus;
    private LinkedListInterface<TaskAssignment> taskAssignments;
    
    public Staff(){ }

    public Staff(String staffId, String staffName, String department, String staffRole, String availabilityStatus) {
        this.staffId = staffId;
        this.staffName = staffName;
        this.department = department; // Housekeeping
        this.staffRole = staffRole; // Supervisor, Cleaner
        this.availabilityStatus = availabilityStatus;
    }

    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getStaffRole() {
        return staffRole;
    }

    public void setStaffRole(String staffRole) {
        this.staffRole = staffRole;
    }

    public String getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(String availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
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

    @Override
    public String toString() {
        return "Staff Details:" + 
               "\nstaffName=" + staffName +
               ",\ndepartment=" + department + 
               ",\nstaffRole=" + staffRole + 
               ",\navailabilityStatus=" + availabilityStatus;
    }

    @Override
    public int compareTo(Staff other) {
        // null checks to avoid NullPointerException
        if (other == null) {
            return 1;
        }
        if (this.staffId == null && other.staffId == null) {
            return 0;
        }
        if (this.staffId == null) {
            return 1;
        }
        if (other.staffId == null) {
            return -1;
        }
        
        return this.staffId.compareToIgnoreCase(other.staffId);
    }
}