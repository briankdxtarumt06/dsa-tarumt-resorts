package tarumtresort.report.HousekeepingReport;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Room;
import tarumtresort.entity.Staff;
import tarumtresort.entity.Task;
import tarumtresort.entity.TaskAssignment;
import tarumtresort.entity.TaskAssignmentChange;
import tarumtresort.entity.enums.StaffRole;
import tarumtresort.entity.enums.TaskStatus;
import tarumtresort.entity.enums.TaskType;
import tarumtresort.report.ReportChart;
import tarumtresort.utility.Ansi;

public class StaffProductivityReport {

    private final LinkedListInterface<Staff> staffList;
    private final LinkedListInterface<Task> taskList;
    private final LinkedListInterface<TaskAssignment> assignmentList;
    private final LinkedListInterface<TaskAssignmentChange> changeList;
    private final LinkedListInterface<Room> roomList;

    public StaffProductivityReport(LinkedListInterface<Staff> staffList,
            LinkedListInterface<Task> taskList, LinkedListInterface<TaskAssignment> assignmentList,
            LinkedListInterface<TaskAssignmentChange> changeList, LinkedListInterface<Room> roomList) {
        this.staffList = staffList == null ? new LinkedList<>() : staffList;
        this.taskList = taskList == null ? new LinkedList<>() : taskList;
        this.assignmentList = assignmentList == null ? new LinkedList<>() : assignmentList;
        this.changeList = changeList == null ? new LinkedList<>() : changeList;
        this.roomList = roomList == null ? new LinkedList<>() : roomList;
    }

    public Result generate(LocalDateTime from, LocalDateTime to) {
        LinkedListInterface<StaffRow> rows = new LinkedList<>();

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
                Task task = findTask(assignment.getAssignedTaskId());
                if (!relevantTask(task == null ? null : task.getTaskType(), staff.getStaffRole())) {
                    continue;
                }
                Long minutes = assignmentDuration(assignment);
                if (minutes == null) {
                    continue;
                }
                row.completed++;
                row.totalMinutes += minutes;
                if (row.fastest == 0 || minutes < row.fastest) {
                    row.fastest = minutes;
                }
                if (minutes > row.slowest) {
                    row.slowest = minutes;
                }
                row.servedRooms.add(roomLabel(task));
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

    private boolean relevantTask(TaskType type, StaffRole role) {
        if (role == StaffRole.CLEANER) {
            return type == TaskType.CHECKOUT_CLEAN || type == TaskType.ROOM_SERVICE;
        }
        if (role == StaffRole.SUPERVISOR) {
            return type == TaskType.INSPECTION;
        }
        return false;
    }

    private Task findTask(String taskId) {
        if (taskId == null) {
            return null;
        }
        for (int i = 0; i < taskList.size(); i++) {
            if (taskId.equalsIgnoreCase(taskList.get(i).getTaskId())) {
                return taskList.get(i);
            }
        }
        return null;
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

    private String roomLabel(Task task) {
        if (task == null || task.getRoomId() == null) {
            return "-";
        }
        for (int i = 0; i < roomList.size(); i++) {
            Room room = roomList.get(i);
            if (task.getRoomId().equalsIgnoreCase(room.getRoomId())) {
                return room.getRoomId() + " (" + room.getRoomNumber() + " "
                        + (room.getRoomType() == null ? "-" : room.getRoomType().name()) + ")";
            }
        }
        return task.getRoomId();
    }

    private String[][] toTable(LinkedListInterface<StaffRow> rows) {
        String[][] table = new String[rows.size() + 1][7];
        table[0] = new String[] { "Staff ID", "Name", "Role", "Department",
                "Tasks Completed", "Avg (min)", "Rooms" };
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
                    String.valueOf(row.servedRooms.size())
            };
        }
        return table;
    }

    private LinkedListInterface<ReportChart> buildCharts(LinkedListInterface<StaffRow> rows) {
        LinkedListInterface<ReportChart> charts = new LinkedList<>();
        charts.addBack(buildRoleChart(rows, StaffRole.CLEANER, "Cleaner Completion Speed (min)"));
        charts.addBack(buildRoleChart(rows, StaffRole.SUPERVISOR, "Supervisor Completion Speed (min)"));
        return charts;
    }

    private ReportChart buildRoleChart(LinkedListInterface<StaffRow> rows, StaffRole role, String title) {
        ReportChart chart = new ReportChart(title);
        for (int i = 0; i < rows.size(); i++) {
            StaffRow row = rows.get(i);
            if (row.staff.getStaffRole() != role) {
                continue;
            }
            chart.addBar(row.staff.getStaffName() + "\n" + row.staff.getStaffId(),
                    row.average(),
                    "(" + row.completed + " task" + (row.completed == 1 ? "" : "s")
                            + ", ~" + Math.round(row.average()) + " min avg)");
        }
        return chart;
    }

    private String[] buildSummary(LinkedListInterface<StaffRow> rows) {
        int cleanerTasks = 0;
        long cleanerMinutes = 0;
        int supervisorTasks = 0;
        long supervisorMinutes = 0;
        StaffRow topCleaner = null;
        StaffRow topSupervisor = null;
        StaffRow mostTasks = null;

        for (int i = 0; i < rows.size(); i++) {
            StaffRow row = rows.get(i);
            if (row.staff.getStaffRole() == StaffRole.CLEANER) {
                cleanerTasks += row.completed;
                cleanerMinutes += row.totalMinutes;
                if (topCleaner == null || row.average() < topCleaner.average()) {
                    topCleaner = row;
                }
            } else if (row.staff.getStaffRole() == StaffRole.SUPERVISOR) {
                supervisorTasks += row.completed;
                supervisorMinutes += row.totalMinutes;
                if (topSupervisor == null || row.average() < topSupervisor.average()) {
                    topSupervisor = row;
                }
            }
            if (mostTasks == null || row.completed > mostTasks.completed) {
                mostTasks = row;
            }
        }

        double cleanerAvg = cleanerTasks == 0 ? 0 : (double) cleanerMinutes / cleanerTasks;
        double supervisorAvg = supervisorTasks == 0 ? 0 : (double) supervisorMinutes / supervisorTasks;

        return new String[] {
                Ansi.bold("Staff Tracked: ") + rows.size(),
                Ansi.bold("Cleaner Average Completion: ") + Math.round(cleanerAvg) + " min",
                Ansi.bold("Supervisor Average Completion: ") + Math.round(supervisorAvg) + " min",
                Ansi.bold("Top Cleaner: ")
                        + (topCleaner == null ? "-"
                                : topCleaner.staff.getStaffName() + " (" + Math.round(topCleaner.average())
                                        + " min, " + topCleaner.completed + " task"
                                        + (topCleaner.completed == 1 ? "" : "s") + ")"),
                Ansi.bold("Top Supervisor: ")
                        + (topSupervisor == null ? "-"
                                : topSupervisor.staff.getStaffName() + " (" + Math.round(topSupervisor.average())
                                        + " min, " + topSupervisor.completed + " task"
                                        + (topSupervisor.completed == 1 ? "" : "s") + ")"),
                Ansi.bold("Most Tasks Completed: ")
                        + (mostTasks == null ? "-"
                                : mostTasks.staff.getStaffName() + " (" + mostTasks.completed + " tasks)")
        };
    }

    private static class StaffRow implements Comparable<StaffRow> {
        final Staff staff;
        int completed;
        long totalMinutes;
        long fastest;
        long slowest;
        final Set<String> servedRooms = new LinkedHashSet<>();

        StaffRow(Staff staff) {
            this.staff = staff;
        }

        double average() {
            return completed == 0 ? 0 : (double) totalMinutes / completed;
        }

        @Override
        public int compareTo(StaffRow other) {
            int c = Double.compare(average(), other.average());
            if (c != 0) {
                return c;
            }
            c = Integer.compare(other.completed, completed);
            if (c != 0) {
                return c;
            }
            return staff.getStaffId().compareToIgnoreCase(other.staff.getStaffId());
        }
    }

    public static class Result {
        private final String[][] table;
        private final LinkedListInterface<ReportChart> charts;
        private final String[] summary;

        Result(String[][] table, LinkedListInterface<ReportChart> charts, String[] summary) {
            this.table = table;
            this.charts = charts == null ? new LinkedList<>() : charts;
            this.summary = summary;
        }

        public String[][] getTable() {
            return table;
        }

        public LinkedListInterface<ReportChart> getCharts() {
            return charts;
        }

        public String[] getSummary() {
            return summary;
        }
    }
}