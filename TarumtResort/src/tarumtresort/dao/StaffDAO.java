package tarumtresort.dao;

import java.io.IOException;
import java.nio.file.Path;
import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;
import tarumtresort.entity.Staff;
import tarumtresort.entity.TaskAssignment;
import tarumtresort.utility.JsonFileHandler;

// Author: Brian Kam Ding Xian
public class StaffDAO {

    private static final Path FILE = Path.of("data/staff.json");

    private static final TaskAssignmentDAO TASK_ASSIGNMENT_DAO = new TaskAssignmentDAO();

    public void saveStaffList(ListInterface<Staff> staffList) {
        try {
            JsonFileHandler.saveListWithNestedIds(
                    staffList, FILE, "taskAssignments",
                    Staff::getTaskAssignments,
                    TaskAssignment::getTaskAssignmentId);
        } catch (IOException e) {
            System.err.println("  ✗ Failed to save staff data: " + e.getMessage());
        }
    }

    public DoublyLinkedList<Staff> retrieveStaffList() {
        try {
            return JsonFileHandler.loadListWithNestedIds(
                    FILE, Staff.class, "taskAssignments",
                    TASK_ASSIGNMENT_DAO::getTaskAssignmentById,
                    Staff::setTaskAssignments);
        } catch (IOException e) {
            System.err.println("  ✗ Failed to load staff data: " + e.getMessage());
            return new DoublyLinkedList<>();
        }
    }
}