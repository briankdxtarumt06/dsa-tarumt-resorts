package tarumtresort.report;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Staff;
import tarumtresort.entity.Task;
import tarumtresort.entity.TaskAssignment;

/**
 *
 * @author Brian
 *
 * Staff Workload Report: joins Staff, Task and TaskAssignment to summarise
 * how many tasks each staff was assigned / completed / cancelled in a period.
 *
 * Dependencies: Staff, Task, TaskAssignment (3 classes).
 * Filters: date range (date & time assigned), staff role, department.
 */
public class StaffWorkloadReport {

    private final LinkedListInterface<Staff> staffList;
    private final LinkedListInterface<Task> taskList;
    private final LinkedListInterface<TaskAssignment> assignmentList;

    public StaffWorkloadReport(LinkedListInterface<Staff> staffList, LinkedListInterface<Task> taskList,
            LinkedListInterface<TaskAssignment> assignmentList) {
        this.staffList = staffList == null ? new LinkedList<>() : staffList;
        this.taskList = taskList == null ? new LinkedList<>() : taskList;
        this.assignmentList = assignmentList == null ? new LinkedList<>() : assignmentList;
    }

    /**
     * Generates the report. from/to may be null (unbounded range),
     * staffRole may be null (all roles), department may be null (all).
     */
    public ReportResult generate(LocalDateTime from, LocalDateTime to, String staffRole, String department) {

        // rows: staff id, name, department, role, availability, counts, workload
        List<String[]> rows = new ArrayList<>();

        int totalAssigned = 0;
        int totalCompleted = 0;

        for (int i = 0; i < staffList.size(); i++) {
            Staff staff = staffList.get(i);

            // filter 2: staff role
            if (staffRole != null && !staffRole.equalsIgnoreCase(staff.getStaffRole())) {
                continue;
            }
            // filter 3: department
            if (department != null && !department.equalsIgnoreCase(staff.getDepartment())) {
                continue;
            }

            // filter 1: date range on the date & time assigned
            int assigned = 0;
            int pending = 0;
            int inProgress = 0;
            int completed = 0;
            int cancelled = 0;

            for (int j = 0; j < assignmentList.size(); j++) {
                TaskAssignment assignment = assignmentList.get(j);

                if (assignment.getAssignedStaffId() == null
                        || !assignment.getAssignedStaffId().equals(staff.getStaffId())) {
                    continue;
                }
                if (!inRange(assignment.getDateTimeAssigned(), from, to)) {
                    continue;
                }

                assigned++;
                String status = assignment.getStatus() == null ? "" : assignment.getStatus();
                switch (status.toLowerCase()) {
                    case "pending" -> pending++;
                    case "in progress" -> inProgress++;
                    case "completed", "work finished" -> completed++;
                    case "cancelled" -> cancelled++;
                    default -> {
                    }
                }
            }

            totalAssigned += assigned;
            totalCompleted += completed;

            rows.add(new String[] {
                    staff.getStaffId(),
                    staff.getStaffName(),
                    staff.getDepartment(),
                    staff.getStaffRole(),
                    staff.getAvailabilityStatus(),
                    String.valueOf(assigned),
                    String.valueOf(pending),
                    String.valueOf(inProgress),
                    String.valueOf(completed),
                    String.valueOf(cancelled)
            });
        }

        return new ReportResult(toTable(rows),
                summary(rows.size(), totalAssigned, totalCompleted));
    }

    // -------------------- helpers --------------------

    private boolean inRange(LocalDateTime value, LocalDateTime from, LocalDateTime to) {
        if (value == null) {
            return false;
        }
        if (from != null && value.isBefore(from)) {
            return false;
        }
        if (to != null && value.isAfter(to)) {
            return false;
        }
        return true;
    }

    private String[][] toTable(List<String[]> rows) {
        String[][] table = new String[rows.size() + 1][10];
        table[0] = new String[] { "Staff ID", "Staff Name", "Department", "Role", "Availability",
                "Assigned", "Pending", "In Progress", "Completed", "Cancelled" };
        for (int i = 0; i < rows.size(); i++) {
            table[i + 1] = rows.get(i);
        }
        return table;
    }

    private String[] summary(int staffCount, int totalAssigned, int totalCompleted) {
        double avg = staffCount == 0 ? 0 : (double) totalAssigned / staffCount;
        return new String[] {
                "Staff in report: " + staffCount,
                "Total assignments: " + totalAssigned,
                "Total completed: " + totalCompleted,
                "Average assignments per staff: " + String.format("%.2f", avg)
        };
    }
}