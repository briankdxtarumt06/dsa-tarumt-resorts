package tarumtresort.report;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Staff;
import tarumtresort.entity.Task;
import tarumtresort.entity.TaskAssignment;
import tarumtresort.entity.TaskAssignmentChange;
import tarumtresort.entity.enums.AvailabilityStatus;
import tarumtresort.entity.enums.TaskStatus;
import tarumtresort.utility.Ansi;

/**
 *
 * @author Brian
 *
 * Staff Productivity &amp; Reassignment Report: staff workload, completion
 * speed, and how often tasks churn between staff (task-reassignment rate -
 * the closest available proxy to turnover, since there is no hire/exit data).
 *
 * Dependencies: Staff, Task, TaskAssignment, TaskAssignmentChange (4 classes).
 * Filters: date range (date &amp; time assigned).
 *
 * Completion workflow: the cleaner marks their own assignment COMPLETED /
 * "Work Finished" (the EARLIEST COMPLETED change of the assignment is the
 * cleaner's finish time - the supervisor's later task-level sign-off also
 * references the same assignment); the supervisor then inspects the room.
 *
 * Enum mapping (spec -> model): REASSIGNED / CANCELLED assignment changes are
 * both counted as "reassigned" (the model has no REASSIGNED status; a drop or
 * decline is recorded as a CANCELLED change). ASSIGNED availability status ->
 * AvailabilityStatus.AVAILABLE / BUSY: a staff member holding at least one
 * active (non-cancelled, non-completed) assignment is counted as utilized.
 */
public class StaffProductivityReport {

    // a role with more reassignments than this share of its assignments is
    // flagged as high-churn
    private static final double HIGH_CHURN_RATE = 15.0;

    private final LinkedListInterface<Staff> staffList;
    private final LinkedListInterface<Task> taskList;
    private final LinkedListInterface<TaskAssignment> assignmentList;
    private final LinkedListInterface<TaskAssignmentChange> changeList;

    public StaffProductivityReport(LinkedListInterface<Staff> staffList, LinkedListInterface<Task> taskList,
            LinkedListInterface<TaskAssignment> assignmentList, LinkedListInterface<TaskAssignmentChange> changeList) {
        this.staffList = staffList == null ? new LinkedList<>() : staffList;
        this.taskList = taskList == null ? new LinkedList<>() : taskList;
        this.assignmentList = assignmentList == null ? new LinkedList<>() : assignmentList;
        this.changeList = changeList == null ? new LinkedList<>() : changeList;
    }

    /**
     * Generates the report. from/to may be null (unbounded range).
     */
    public ReportResult generate(LocalDateTime from, LocalDateTime to) {

        LinkedListInterface<StaffRow> rows = new LinkedList<>();
        int totalAssignments = 0;
        int totalReassigned = 0;

        for (int i = 0; i < staffList.size(); i++) {
            Staff staff = staffList.get(i);
            if (staff.isDeleted()) {
                continue;
            }
            StaffRow row = new StaffRow(staff);

            for (int j = 0; j < assignmentList.size(); j++) {
                TaskAssignment assignment = assignmentList.get(j);

                if (assignment.isDeleted() || assignment.getAssignedStaffId() == null
                        || !assignment.getAssignedStaffId().equals(staff.getStaffId())) {
                    continue;
                }
                if (!inRange(assignment.getDateTimeAssigned(), from, to)) {
                    continue;
                }

                row.assignments++;

                // cleaner's own finish time: earliest COMPLETED change
                LocalDateTime completedAt = earliestCompletedChange(assignment.getTaskAssignmentId());
                if (completedAt != null && assignment.getDateTimeAssigned() != null) {
                    row.completed++;
                    long minutes = Duration.between(assignment.getDateTimeAssigned(), completedAt).toMinutes();
                    row.completionSum += minutes;
                }

                // every status transition of this assignment (drop / decline /
                // reassignment counts as churn); only changes inside the
                // report period are counted
                for (int k = 0; k < changeList.size(); k++) {
                    TaskAssignmentChange change = changeList.get(k);
                    if (change.getTaskAssignmentId() == null
                            || !change.getTaskAssignmentId().equals(assignment.getTaskAssignmentId())) {
                        continue;
                    }
                    if (!inRange(change.getChangedAt(), from, to)) {
                        continue;
                    }
                    if (change.getStatus() != null && isReassignedStatus(change.getStatus())) {
                        row.reassigned++;
                    }
                }
            }

            // summary totals only count active staff so the denominator
            // matches the "Total Active Staff" figure
            if (staff.getAvailabilityStatus() != AvailabilityStatus.RESIGNED) {
                totalAssignments += row.assignments;
                totalReassigned += row.reassigned;
            }
            rows.addSorted(row);
        }

        return new ReportResult(
                toTable(rows),
                summary(rows, totalAssignments, totalReassigned),
                buildCharts(rows),
                buildCallouts(rows));
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

    /**
     * Cleaner's own finish time: the EARLIEST COMPLETED change of the
     * assignment. The supervisor's task-level sign-off happens later and also
     * references the same assignment, so the earliest one is the worker's.
     * The cleaner may finish via "Completed", "Work Finished" or "Inspected"
     * (see HousekeepingController.isTaskFullyFinished).
     */
    private LocalDateTime earliestCompletedChange(String taskAssignmentId) {
        LocalDateTime earliest = null;
        for (int i = 0; i < changeList.size(); i++) {
            TaskAssignmentChange change = changeList.get(i);
            if (change.getTaskAssignmentId() == null
                    || !change.getTaskAssignmentId().equals(taskAssignmentId)) {
                continue;
            }
            if (change.getStatus() == null || !isCompletedStatus(change.getStatus())) {
                continue;
            }
            if (change.getChangedAt() != null && (earliest == null || change.getChangedAt().isBefore(earliest))) {
                earliest = change.getChangedAt();
            }
        }
        return earliest;
    }

    private boolean isCompletedStatus(String status) {
        return "Completed".equalsIgnoreCase(status)
                || "Work Finished".equalsIgnoreCase(status)
                || "Inspected".equalsIgnoreCase(status);
    }

    private boolean isCompletedStatus(TaskStatus status) {
        return status == TaskStatus.COMPLETED;
    }

    private boolean isReassignedStatus(String status) {
        return "Reassigned".equalsIgnoreCase(status) || "Cancelled".equalsIgnoreCase(status);
    }

    private String[][] toTable(LinkedListInterface<StaffRow> rows) {
        String[][] table = new String[rows.size() + 1][8];
        table[0] = new String[] { "Staff ID", "Name", "Department", "Role", "Tasks Completed",
                "Tasks Reassigned", "Avg Completion Time (min)", "Availability" };
        for (int i = 0; i < rows.size(); i++) {
            StaffRow row = rows.get(i);
            Staff staff = row.staff;
            table[i + 1] = new String[] {
                    staff.getStaffId(),
                    staff.getStaffName(),
                    staff.getDepartment() == null ? "-" : staff.getDepartment(),
                    staff.getStaffRole() == null ? "-" : staff.getStaffRole(),
                    String.valueOf(row.completed),
                    String.valueOf(row.reassigned),
                    row.completed == 0 ? "-" : String.valueOf(Math.round(row.averageCompletion())),
                    staff.getAvailabilityStatus() == null ? "-" : staff.getAvailabilityStatus().name()
            };
        }
        return table;
    }

    private String[] summary(LinkedListInterface<StaffRow> rows, int totalAssignments, int totalReassigned) {
        int activeStaff = 0;
        int utilizedStaff = 0;
        for (int i = 0; i < staffList.size(); i++) {
            Staff staff = staffList.get(i);
            if (staff.isDeleted() || staff.getAvailabilityStatus() == AvailabilityStatus.RESIGNED) {
                continue;
            }
            activeStaff++;
            if (isCurrentlyUtilized(staff)) {
                utilizedStaff++;
            }
        }

        double overallRate = totalAssignments == 0 ? 0 : (double) totalReassigned / totalAssignments * 100;
        double utilization = activeStaff == 0 ? 0 : (double) utilizedStaff / activeStaff * 100;

        String overallText = String.format("%.1f%%", overallRate);
        String utilizationText = String.format("%.1f%%", utilization);

        return new String[] {
                Ansi.bold("Total Active Staff: ") + activeStaff,
                Ansi.bold("Overall Reassignment Rate: ")
                        + Ansi.color(overallRate > HIGH_CHURN_RATE ? Ansi.RED : Ansi.GREEN, overallText),
                Ansi.bold("Staff Utilization Rate: ")
                        + Ansi.color(utilization >= 50 ? Ansi.GREEN : Ansi.YELLOW, utilizationText)
        };
    }

    // a staff is "utilized" when holding at least one active assignment
    // (assignments finished via COMPLETED / CANCELLED are terminal and no
    // longer count as active work)
    private boolean isCurrentlyUtilized(Staff staff) {
        for (int i = 0; i < assignmentList.size(); i++) {
            TaskAssignment assignment = assignmentList.get(i);
            if (assignment.isDeleted() || assignment.getAssignedStaffId() == null
                    || !assignment.getAssignedStaffId().equals(staff.getStaffId())) {
                continue;
            }
            if (assignment.getStatus() != null && isCompletedStatus(assignment.getStatus())) {
                continue;
            }
            if (assignment.getStatus() == TaskStatus.CANCELLED) {
                continue;
            }
            return true;
        }
        return false;
    }

    private LinkedListInterface<ReportChart> buildCharts(LinkedListInterface<StaffRow> rows) {
        LinkedListInterface<ReportChart> charts = new LinkedList<>();

        // chart 1: top staff by completed tasks (best 8)
        ReportChart chart1 = new ReportChart("Top Staff by Completed Tasks");
        int shown = 0;
        for (StaffRow row : rows) {
            if (row.completed == 0 || shown >= 8) {
                continue;
            }
            shown++;
            // first name only: full names are too long for the vertical chart slots
            String firstName = row.staff.getStaffName();
            int space = firstName.indexOf(' ');
            if (space > 0) {
                firstName = firstName.substring(0, space);
            }
            chart1.addBar(firstName,
                    row.completed,
                    "(" + row.completed + " task" + (row.completed == 1 ? "" : "s")
                            + ", ~" + Math.round(row.averageCompletion()) + " min avg)");
        }
        charts.addBack(chart1);

        // chart 2: reassignment rate by role
        ReportChart chart2 = new ReportChart("Reassignment Rate by Role (%)");
        Map<String, long[]> roleStats = new LinkedHashMap<>();
        for (StaffRow row : rows) {
            String role = row.staff.getStaffRole();
            if (role == null) {
                continue;
            }
            long[] acc = roleStats.computeIfAbsent(role, k -> new long[2]);
            acc[0] += row.reassigned;
            acc[1] += row.assignments;
        }
        LinkedListInterface<String> roles = new LinkedList<>();
        for (String role : roleStats.keySet()) {
            roles.addBack(role);
        }
        sortRolesByRate(roles, roleStats);
        for (String role : roles) {
            long[] acc = roleStats.get(role);
            double rate = rateOf(acc);
            chart2.addBar(role, rate,
                    "(" + acc[0] + "/" + acc[1] + " reassigned)");
        }
        charts.addBack(chart2);

        return charts;
    }

    private double rateOf(long[] acc) {
        return acc[1] == 0 ? 0 : (double) acc[0] / acc[1] * 100;
    }

    // insertion sort: roles by reassignment rate descending (stable)
    private void sortRolesByRate(LinkedListInterface<String> roles, Map<String, long[]> roleStats) {
        for (int i = 1; i < roles.size(); i++) {
            String key = roles.get(i);
            long[] stats = roleStats.get(key);
            int j = i - 1;
            while (j >= 0 && rateOf(roleStats.get(roles.get(j))) < rateOf(stats)) {
                roles.set(j + 1, roles.get(j));
                j--;
            }
            roles.set(j + 1, key);
        }
    }

    private LinkedListInterface<String> buildCallouts(LinkedListInterface<StaffRow> rows) {
        LinkedListInterface<String> callouts = new LinkedList<>();

        // callout 1: top performers (top 3 by completed desc, fastest first)
        callouts.addBack(Ansi.green(Ansi.bold("★ Top Performers (top 3)")));
        boolean any = false;
        for (int i = 0; i < Math.min(3, rows.size()); i++) {
            StaffRow row = rows.get(i);
            if (row.completed == 0) {
                break;
            }
            any = true;
            callouts.addBack(Ansi.green("  " + (i + 1) + ". " + row.staff.getStaffName() + " ("
                    + row.staff.getStaffId() + ") - " + row.completed + " task"
                    + (row.completed == 1 ? "" : "s")
                    + ", ~" + Math.round(row.averageCompletion()) + " min avg"));
        }
        if (!any) {
            callouts.addBack(Ansi.green("  (no completed tasks in range)"));
        }

        // callout 2: high-churn roles (reassignment rate > 15%)
        callouts.addBack(Ansi.red(Ansi.bold("⚠ Highest Reassignment Roles (> " + HIGH_CHURN_RATE + "%)")));
        Map<String, long[]> roleStats = new LinkedHashMap<>();
        for (StaffRow row : rows) {
            String role = row.staff.getStaffRole();
            if (role == null) {
                continue;
            }
            long[] acc = roleStats.computeIfAbsent(role, k -> new long[2]);
            acc[0] += row.reassigned;
            acc[1] += row.assignments;
        }
        boolean anyRole = false;
        for (Map.Entry<String, long[]> entry : roleStats.entrySet()) {
            double rate = rateOf(entry.getValue());
            if (rate > HIGH_CHURN_RATE) {
                anyRole = true;
                callouts.addBack(Ansi.red("  ⚠ " + entry.getKey() + " - " + String.format("%.1f%%", rate)
                        + " (" + entry.getValue()[0] + "/" + entry.getValue()[1] + " reassigned)"));
            }
        }
        if (!anyRole) {
            callouts.addBack(Ansi.red("  (no role exceeds the threshold)"));
        }

        return callouts;
    }

    private static class StaffRow implements Comparable<StaffRow> {
        final Staff staff;
        int assignments;
        int completed;
        long completionSum;
        int reassigned;

        StaffRow(Staff staff) {
            this.staff = staff;
        }

        double averageCompletion() {
            return completed == 0 ? 0 : (double) completionSum / completed;
        }

        // most completed first, then fastest, then by id (matches addSorted order)
        @Override
        public int compareTo(StaffRow other) {
            int c = Integer.compare(other.completed, completed);
            if (c != 0) {
                return c;
            }
            double avgA = averageCompletion();
            double avgB = other.averageCompletion();
            if (avgA != avgB) {
                return Double.compare(avgA, avgB);
            }
            return staff.getStaffId().compareToIgnoreCase(other.staff.getStaffId());
        }
    }
}