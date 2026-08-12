package tarumtresort.entity;

/**
 *
 * @author Brian
 */
public class Staff {
    private String staffId;
    private String staffName;
    private String department;
    private String staffRole;
    private String availabilityStatus;
    
    public Staff(){ }

    public Staff(String staffName, String department, String staffRole, String availabilityStatus) {
        this.staffName = staffName;
        this.department = department;
        this.staffRole = staffRole;
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

    @Override
    public String toString() {
        return "Staff Details:" + 
               "\nstaffName=" + staffName +
               ",\ndepartment=" + department + 
               ",\nstaffRole=" + staffRole + 
               ",\navailabilityStatus=" + availabilityStatus;
    }
    
}
