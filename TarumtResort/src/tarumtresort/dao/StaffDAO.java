package tarumtresort.dao;

import java.io.IOException;
import java.nio.file.Path;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Staff;
import tarumtresort.utility.JsonFileHandler;

/**
 *
 * @author Brian
 */
public class StaffDAO {

    private static final Path FILE = Path.of("data/staff.json");

    public void saveStaffList(LinkedListInterface<Staff> staffList) {
        try {
            JsonFileHandler.saveList(staffList, FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public LinkedList<Staff> retrieveStaffList() {
        try {
            return JsonFileHandler.loadList(FILE, Staff.class);
        } catch (IOException e) {
            e.printStackTrace();
            return new LinkedList<>();
        }
    }
}