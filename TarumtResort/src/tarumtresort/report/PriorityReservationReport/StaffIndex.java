package tarumtresort.report.PriorityReservationReport;

import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Staff;

// Author: Lee Boon Yew
/**
 * Sorted staff index - same sort-then-binary-search pairing as
 * ReservationIndex, used by the governance report to resolve the staff id
 * stored in PriorityReservation.getOverriddenBy() into a real name and role.
 */
public class StaffIndex {

    private final LinkedListInterface<Key> keys = new LinkedList<>();

    public StaffIndex(LinkedListInterface<Staff> staffList) {
        if (staffList == null) {
            return;
        }
        for (int i = 0; i < staffList.size(); i++) {
            Staff staff = staffList.get(i);
            if (staff == null || staff.getStaffId() == null) {
                continue;
            }
            keys.addSorted(new Key(staff));
        }
    }

    public int size() {
        return keys.size();
    }

    /** Binary search by staff id. Returns null when the id is absent. */
    public Staff find(String staffId) {
        if (staffId == null) {
            return null;
        }
        int low = 0;
        int high = keys.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            Key key = keys.get(mid);
            if (key == null) {
                return null;
            }
            int comparison = key.id().compareToIgnoreCase(staffId);
            if (comparison == 0) {
                return key.staff;
            }
            if (comparison < 0) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return null;
    }

    /** Staff name for a staff id, falling back to the raw id then a dash. */
    public String nameOf(String staffId) {
        Staff staff = find(staffId);
        if (staff != null && staff.getStaffName() != null && !staff.getStaffName().isEmpty()) {
            return staff.getStaffName();
        }
        return (staffId == null || staffId.isEmpty()) ? "-" : staffId;
    }

    private static class Key implements Comparable<Key> {

        private final Staff staff;

        Key(Staff staff) {
            this.staff = staff;
        }

        String id() {
            return staff.getStaffId();
        }

        @Override
        public int compareTo(Key other) {
            return id().compareToIgnoreCase(other.id());
        }
    }
}
