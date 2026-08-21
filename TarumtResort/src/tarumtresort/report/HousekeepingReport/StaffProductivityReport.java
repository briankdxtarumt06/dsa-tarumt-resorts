package tarumtresort.report.HousekeepingReport;

import java.time.Duration;
import java.time.LocalDateTime;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Staff;
import tarumtresort.entity.TaskAssignment;
import tarumtresort.entity.TaskAssignmentChange;
import tarumtresort.entity.enums.StaffRole;
import tarumtresort.entity.enums.TaskStatus;
import tarumtresort.report.ReportChart;
import tarumtresort.utility.Ansi;

// Author: Brian Kam Ding Xian
public class StaffProductivityReport {

    private final LinkedListInterface<Staff> staffList;
    private final LinkedListInterface<TaskAssignment> assignmentList;
    private final LinkedListInterface<TaskAssignmentChange> changeList;

    public StaffProductivityReport(
        LinkedListInterface<Staff> staffList,
        LinkedListInterface<TaskAssignment> assignmentList,
        LinkedListInterface<TaskAssignmentChange> changeList
    ) {
        this.staffList = staffList == null ? new LinkedList<>() : staffList;
        this.assignmentList = assignmentList == null ? new LinkedList<>() : assignmentList;
        this.changeList = changeList == null ? new LinkedList<>() : changeList;
    }

    public Result generate(LocalDateTime from, LocalDateTime to) {
        ListInterface<StaffRow> rows = new DoublyLinkedList<>();

        for (int i = 0; i < staffList.size(); i++) {
            Staff staff = staffList.get(i);
            if (staff.isDeleted() || !isTrackedRole(staff.getStaffRole())) {
                continue;
            }
            StaffRow row = new StaffRow(staff);
            for (int j = 0; j < assignmentList.size(); j++) {
                TaskAssignment assignment = assignmentList.get(j);
                if (assignment.isDeleted()
                        || assignment.getStatus() != TaskStatus.COMPLETED
                        || assignment.getAssignedStaffId() == null
                        || !assignment.getAssignedStaffId().equals(staff.getStaffId())) {
                    continue;
                }
                if (!inRange(assignment.getDateTimeAssigned(), from, to)) {
                    continue;
                }
                Long minutes = assignmentDuration(assignment);
                if (minutes == null) {
                    continue;
                }
                row.completed++;
                row.totalMinutes += minutes;
            }
            if (row.completed > 0) {
                rows.addSorted(row);
            }
        }

        return new Result(toTable(rows), buildCharts(rows), buildSummary(rows));
    }

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

    private boolean isTrackedRole(StaffRole role) {
        return role == StaffRole.CLEANER || role == StaffRole.SUPERVISOR;
    }

    private Long assignmentDuration(TaskAssignment assignment) {
        LocalDateTime start = assignment.getDateTimeAssigned();
        if (start == null) {
            return null;
        }
        LocalDateTime end = assignment.getDateTimeEnded();
        if (end == null) {
            end = terminalChangeTime(assignment.getTaskAssignmentId());
        }
        if (end == null) {
            return null;
        }
        return Math.max(0, Duration.between(start, end).toMinutes());
    }

    private LocalDateTime terminalChangeTime(String taskAssignmentId) {
        if (taskAssignmentId == null) {
            return null;
        }
        LocalDateTime latest = null;
        for (int i = 0; i < changeList.size(); i++) {
            TaskAssignmentChange change = changeList.get(i);
            if (change.getTaskAssignmentId() == null
                    || !change.getTaskAssignmentId().equals(taskAssignmentId)) {
                continue;
            }
            if (!isTerminal(change.getStatus())) {
                continue;
            }
            if (change.getChangedAt() != null && (latest == null || change.getChangedAt().isAfter(latest))) {
                latest = change.getChangedAt();
            }
        }
        return latest;
    }

    private boolean isTerminal(String status) {
        if (status == null) {
            return false;
        }
        return status.equalsIgnoreCase("Completed")
                || status.equalsIgnoreCase("Work Finished")
                || status.equalsIgnoreCase("Inspected");
    }

    private String[][] toTable(ListInterface<StaffRow> rows) {
        String[][] table = new String[rows.size() + 1][7];
        table[0] = new String[] { "Staff ID", "Name", "Role", "Department",
                "Tasks Completed", "Avg (min)", "Per Hr" };
        for (int i = 0; i < rows.size(); i++) {
            StaffRow row = rows.get(i);
            Staff staff = row.staff;
            table[i + 1] = new String[] {
                    staff.getStaffId(),
                    staff.getStaffName(),
                    staff.getStaffRole() == null ? "-" : staff.getStaffRole().name(),
                    staff.getDepartment() == null ? "-" : staff.getDepartment().name(),
                    String.valueOf(row.completed),
                    String.valueOf(Math.round(row.average())),
                    String.format("%.2f", row.perHour())
            };
        }
        return table;
    }

    private ListInterface<ReportChart> buildCharts(ListInterface<StaffRow> rows) {
        ListInterface<ReportChart> charts = new DoublyLinkedList<>();
        charts.addBack(buildCompletedChart(rows));
        charts.addBack(buildAverageChart(rows));
        return charts;
    }

    private ReportChart buildCompletedChart(ListInterface<StaffRow> rows) {
        ReportChart chart = new ReportChart("Completed Tasks per Staff");
        LinkedListInterface<StaffRow> sorted = new LinkedList<>();
        for (int i = 0; i < rows.size(); i++) {
            sorted.addSorted(rows.get(i));
        }
        for (int i = 0; i < sorted.size(); i++) {
            StaffRow row = sorted.get(i);
            chart.addBar(row.staff.getStaffName() + "\n" + row.staff.getStaffId(),
                    row.completed,
                    "(" + row.completed + " task" + (row.completed == 1 ? "" : "s") + ")");
        }
        return chart;
    }

    private ReportChart buildAverageChart(ListInterface<StaffRow> rows) {
        ReportChart chart = new ReportChart("Average Completion Time per Staff (min)");
        LinkedListInterface<AverageSortRow> sorted = new LinkedList<>();
        for (int i = 0; i < rows.size(); i++) {
            sorted.addSorted(new AverageSortRow(rows.get(i)));
        }
        for (int i = 0; i < sorted.size(); i++) {
            StaffRow row = sorted.get(i).row;
            chart.addBar(row.staff.getStaffName() + "\n" + row.staff.getStaffId(),
                    row.average(),
                    "(" + row.completed + " task" + (row.completed == 1 ? "" : "s")
                            + ", ~" + Math.round(row.average()) + " min avg)");
        }
        return chart;
    }

    private String[] buildSummary(ListInterface<StaffRow> rows) {
        StaffRow topPerformer = null;
        StaffRow fastest = null;
        StaffRow slowest = null;
        StaffRow mostProductive = null;
        int totalCompleted = 0;

        for (int i = 0; i < rows.size(); i++) {
            StaffRow row = rows.get(i);
            totalCompleted += row.completed;
            if (topPerformer == null || row.completed > topPerformer.completed) {
                topPerformer = row;
            }
            if (fastest == null || row.average() < fastest.average()) {
                fastest = row;
            }
            if (slowest == null || row.average() > slowest.average()) {
                slowest = row;
            }
            if (mostProductive == null || row.perHour() > mostProductive.perHour()) {
                mostProductive = row;
            }
        }

        return new String[] {
                Ansi.bold("Top Performer: ")
                        + (topPerformer == null ? "-"
                                : topPerformer.staff.getStaffName() + " (" + topPerformer.completed
                                        + " task" + (topPerformer.completed == 1 ? "" : "s") + ")"),
                Ansi.bold("Fastest Staff: ")
                        + (fastest == null ? "-"
                                : fastest.staff.getStaffName() + " (" + Math.round(fastest.average())
                                        + " min avg)"),
                Ansi.bold("Slowest Staff: ")
                        + (slowest == null ? "-"
                                : slowest.staff.getStaffName() + " (" + Math.round(slowest.average())
                                        + " min avg)"),
                Ansi.bold("Most Productive Staff: ")
                        + (mostProductive == null ? "-"
                                : mostProductive.staff.getStaffName() + " ("
                                        + String.format("%.2f", mostProductive.perHour()) + " tasks/hr)"),
                Ansi.bold("Total Completed Tasks: ") + totalCompleted
        };
    }

    private static class StaffRow implements Comparable<StaffRow> {
        final Staff staff;
        int completed;
        long totalMinutes;

        StaffRow(Staff staff) {
            this.staff = staff;
        }

        double average() {
            return completed == 0 ? 0 : (double) totalMinutes / completed;
        }

        double perHour() {
            if (totalMinutes == 0) {
                return 0;
            }
            return completed / (totalMinutes / 60.0);
        }

        @Override
        public int compareTo(StaffRow other) {
            int c = Integer.compare(other.completed, completed);
            if (c != 0) {
                return c;
            }
            c = Double.compare(average(), other.average());
            if (c != 0) {
                return c;
            }
            return staff.getStaffId().compareToIgnoreCase(other.staff.getStaffId());
        }
    }

    // sorts by average ascending, then completed ascending for the average chart
    private static class AverageSortRow implements Comparable<AverageSortRow> {
        final StaffRow row;

        AverageSortRow(StaffRow row) {
            this.row = row;
        }

        @Override
        public int compareTo(AverageSortRow other) {
            int c = Double.compare(row.average(), other.row.average());
            if (c != 0) {
                return c;
            }
            c = Integer.compare(row.completed, other.row.completed);
            if (c != 0) {
                return c;
            }
            return row.staff.getStaffId().compareToIgnoreCase(other.row.staff.getStaffId());
        }
    }

    public static class Result {
        private final String[][] table;
        private final ListInterface<ReportChart> charts;
        private final String[] summary;

        Result(String[][] table, ListInterface<ReportChart> charts, String[] summary) {
            this.table = table;
            this.charts = charts == null ? new DoublyLinkedList<>() : charts;
            this.summary = summary;
        }

        public String[][] getTable() {
            return table;
        }

        public ListInterface<ReportChart> getCharts() {
            return charts;
        }

        public String[] getSummary() {
            return summary;
        }
    }
}