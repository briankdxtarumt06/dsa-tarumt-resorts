package tarumtresort.dao;

import java.io.IOException;
import java.nio.file.Path;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Staff;
import tarumtresort.entity.TaskAssignment;
import tarumtresort.utility.JsonFileHandler;

/**
 *
 * @author Brian
 */
public class StaffDAO {

    private static final Path FILE = Path.of("data/staff.json");

    private static final TaskAssignmentDAO TASK_ASSIGNMENT_DAO = new TaskAssignmentDAO();

    public void saveStaffList(LinkedListInterface<Staff> staffList) {
        try {
            JsonFileHandler.saveListWithNestedIds(
                    staffList, FILE, "taskAssignments",
                    Staff::getTaskAssignments,
                    TaskAssignment::getTaskAssignmentId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public LinkedList<Staff> retrieveStaffList() {
        try {
            return JsonFileHandler.loadListWithNestedIds(
                    FILE, Staff.class, "taskAssignments",
                    TASK_ASSIGNMENT_DAO::getTaskAssignmentById,
                    Staff::setTaskAssignments);
        } catch (IOException e) {
            e.printStackTrace();
            return new LinkedList<>();
        }
    }

    public Staff getStaffById(String staffId) {
        LinkedListInterface<Staff> staffList = retrieveStaffList();
        for (int i = 0; i < staffList.size(); i++) {
            if (staffList.get(i).getStaffId().equals(staffId)) {
                return staffList.get(i);
            }
        }
        return null;
    }
}