package tarumtresort.entity;

import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;
import tarumtresort.entity.enums.AvailabilityStatus;
import tarumtresort.entity.enums.Department;
import tarumtresort.entity.enums.StaffRole;

// Author: Brian Kam Ding Xian
public class Staff implements Comparable<Staff> {
    private String staffId;
    private String staffName;
    private Department department;
    private StaffRole staffRole;
    private AvailabilityStatus availabilityStatus;
    private boolean isDeleted;
    private ListInterface<TaskAssignment> taskAssignments;
    
    public Staff(){ }

    public Staff(String staffId, String staffName, Department department, StaffRole staffRole, AvailabilityStatus availabilityStatus) {
        this.staffId = staffId;
        this.staffName = staffName;
        this.department = department; // Housekeeping, Front Office, Maintenance
        this.staffRole = staffRole; // Supervisor, Cleaner, Receptionist
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

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public StaffRole getStaffRole() {
        return staffRole;
    }

    public void setStaffRole(StaffRole staffRole) {
        this.staffRole = staffRole;
    }

    public AvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(AvailabilityStatus availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public ListInterface<TaskAssignment> getTaskAssignments() {
        if (taskAssignments == null) {
            taskAssignments = new DoublyLinkedList<>();
        }
        return taskAssignments;
    }

    public void setTaskAssignments(ListInterface<TaskAssignment> taskAssignments) {
        this.taskAssignments = taskAssignments;
    }

    public void addTaskAssignment(TaskAssignment taskAssignment) {
        if (taskAssignment == null || taskAssignment.getTaskAssignmentId() == null) {
            return;
        }
        if (taskAssignments == null) {
            taskAssignments = new DoublyLinkedList<>();
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