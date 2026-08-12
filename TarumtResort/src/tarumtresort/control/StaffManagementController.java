package tarumtresort.control;

import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.boundary.StaffManagementUI;
import tarumtresort.dao.StaffDAO;
import tarumtresort.entity.Staff;

/**
 *
 * @author Brian
 */
public class StaffManagementController {

    // controller declaration
    // ui declaration
    private StaffManagementUI ui;

    // list declaration
    private LinkedListInterface<Staff> staffList = new LinkedList<>();

    // dao declaration
    private static final StaffDAO staffDAO = new StaffDAO();

    // constructor
    public StaffManagementController() {
        staffList = staffDAO.retrieveStaffList();
    }

    public StaffManagementController(StaffManagementUI ui) {
        this.ui = ui;
        staffList = staffDAO.retrieveStaffList();
    }

    public String createStaff(String staffName, String department, String staffRole, String availabilityStatus) {

        // staff name cannot be duplicated
        for (int i = 0; i < staffList.size(); i++) {
            if (staffList.get(i).getStaffName().equalsIgnoreCase(staffName)) {
                return null;
            }
        }

        String staffId = generateStaffId();

        Staff staff = new Staff(
                staffId,
                staffName,
                department,
                staffRole,
                availabilityStatus
        );

        staffList.addSorted(staff);
        staffDAO.saveStaffList(staffList);

        return staffId;
    }

    public boolean updateStaff(String staffId,
                            String staffName,
                            String department,
                            String staffRole,
                            String availabilityStatus) {

        for (int i = 0; i < staffList.size(); i++) {

            Staff staff = staffList.get(i);

            if (staff.getStaffId().equals(staffId)) {

                staff.setStaffName(staffName);
                staff.setDepartment(department);
                staff.setStaffRole(staffRole);
                staff.setAvailabilityStatus(availabilityStatus);

                staffDAO.saveStaffList(staffList);

                return true;
            }
        }

        return false;
    }

    public boolean resignStaff(String staffId) {

        for (int i = 0; i < staffList.size(); i++) {

            Staff staff = staffList.get(i);

            if (staff.getStaffId().equals(staffId)) {

                staff.setAvailabilityStatus("Resigned"); // soft delete

                staffDAO.saveStaffList(staffList);

                return true;
            }
        }

        return false;
    }

    public Staff getStaffById(String staffId) {

        for (int i = 0; i < staffList.size(); i++) {

            Staff staff = staffList.get(i);

            if (staff.getStaffId().equals(staffId)) {
                return staff;
            }
        }

        return null;
    }

    public Staff getStaffByName(String staffName) {

        for (int i = 0; i < staffList.size(); i++) {

            Staff staff = staffList.get(i);

            if (staff.getStaffName().equalsIgnoreCase(staffName)) {
                return staff;
            }
        }

        return null;
    }

    public LinkedListInterface<Staff> getStaffsByDepartment(String department) {

        LinkedListInterface<Staff> filteredList = new LinkedList<>();

        for (int i = 0; i < staffList.size(); i++) {

            Staff staff = staffList.get(i);

            if (staff.getDepartment().equalsIgnoreCase(department)) {
                filteredList.addBack(staff);
            }
        }

        return filteredList;
    }

    public LinkedListInterface<Staff> getStaffsByAvailability(String availabilityStatus) {

        LinkedListInterface<Staff> filteredList = new LinkedList<>();

        for (int i = 0; i < staffList.size(); i++) {

            Staff staff = staffList.get(i);

            if (staff.getAvailabilityStatus().equalsIgnoreCase(availabilityStatus)) {
                filteredList.addBack(staff);
            }
        }

        return filteredList;
    }

    public LinkedListInterface<Staff> getAllStaffs() {
        return staffList;
    }

    private String generateStaffId() {

        int max = 0;

        for (int i = 0; i < staffList.size(); i++) {

            String staffId = staffList.get(i).getStaffId();

            int number = Integer.parseInt(staffId.substring(3));

            if (number > max) {
                max = number;
            }
        }

        return String.format("STF%012d", max + 1);
    }

    private boolean staffExists(String staffId) {
        return getStaffById(staffId) != null;
    }

    private boolean staffExistsByName(String staffName) {
        return getStaffByName(staffName) != null;
    }

    // staff management
    public void runStaffManagement() {

        int choice;

        do {
            choice = ui.getMenuChoice();

            switch (choice) {
                case 1:
                    addStaff();
                    break;
                case 2:
                    ui.listAllStaffs(staffListToTable(getAllStaffs()));
                    break;
                case 3:
                    searchStaff();
                    break;
                case 4:
                    updateStaff();
                    break;
                case 5:
                    resignStaff();
                    break;
                case 6:
                    filterStaffByDepartment();
                    break;
                case 7:
                    filterStaffByAvailability();
                    break;
                case 0:
                    ui.printExitMessage();
                    break;
                default:
                    ui.printInvalidChoice();
            }

            if (choice != 0) {
                ui.pressEnterToContinue();
            }
        } while (choice != 0);
    }

    private void addStaff() {
        String[] details = ui.inputStaffDetails();
        String staffId = createStaff(details[0], details[1], details[2], details[3]);
        if (staffId == null) {
            ui.printDuplicateName();
        } else {
            ui.printStaffId(staffId);
            ui.printSuccess();
        }
    }

    private void searchStaff() {
        int searchChoice = ui.getSearchMenuChoice();
        if (searchChoice == 0) {
            return;
        }
        Staff staff = null;
        if (searchChoice == 1) {
            staff = getStaffById(ui.inputStaffId());
        } else if (searchChoice == 2) {
            staff = getStaffByName(ui.inputStaffName());
        }
        if (staff == null) {
            ui.printNotFound();
        } else {
            ui.printStaffDetails(staff);
        }
    }

    private void updateStaff() {
        String staffId = ui.inputStaffId();
        if (!staffExists(staffId)) {
            ui.printNotFound();
            return;
        }
        String[] details = ui.inputUpdateStaffDetails();
        if (updateStaff(staffId, details[0], details[1], details[2], details[3])) {
            ui.printSuccess();
        } else {
            ui.printNotFound();
        }
    }

    private void resignStaff() {
        String staffId = ui.inputStaffId();
        if (resignStaff(staffId)) {
            ui.printSuccess();
        } else {
            ui.printNotFound();
        }
    }

    private void filterStaffByDepartment() {
        String department = ui.inputDepartment();
        ui.listAllStaffs(staffListToTable(getStaffsByDepartment(department)));
    }

    private void filterStaffByAvailability() {
        String availabilityStatus = ui.inputAvailabilityStatus();
        ui.listAllStaffs(staffListToTable(getStaffsByAvailability(availabilityStatus)));
    }

    // convert to table 2D array
    private String[][] staffListToTable(LinkedListInterface<Staff> staffList) {
        String[][] data = new String[staffList.size() + 1][5];
        data[0] = new String[]{"Staff ID", "Staff Name", "Department", "Staff Role", "Availability"};
        for (int i = 0; i < staffList.size(); i++) {
            Staff staff = staffList.get(i);
            data[i + 1] = new String[]{
                staff.getStaffId(),
                staff.getStaffName(),
                staff.getDepartment(),
                staff.getStaffRole(),
                staff.getAvailabilityStatus()
            };
        }
        return data;
    }
}