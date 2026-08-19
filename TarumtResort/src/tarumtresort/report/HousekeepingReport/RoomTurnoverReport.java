package tarumtresort.report.HousekeepingReport;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.Room;
import tarumtresort.entity.Task;
import tarumtresort.entity.TaskAssignment;
import tarumtresort.entity.TaskAssignmentChange;
import tarumtresort.entity.enums.RoomType;
import tarumtresort.entity.enums.TaskStatus;
import tarumtresort.entity.enums.TaskType;
import tarumtresort.report.ReportChart;
import tarumtresort.utility.Ansi;

public class RoomTurnoverReport {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final LinkedListInterface<Room> roomList;
    private final LinkedListInterface<Task> taskList;
    private final LinkedListInterface<TaskAssignment> assignmentList;
    private final LinkedListInterface<TaskAssignmentChange> changeList;
    private final LinkedListInterface<Reservation> reservationList;

    public RoomTurnoverReport(LinkedListInterface<Room> roomList,
            LinkedListInterface<Task> taskList, LinkedListInterface<TaskAssignment> assignmentList,
            LinkedListInterface<TaskAssignmentChange> changeList,
            LinkedListInterface<Reservation> reservationList) {
        this.roomList = roomList == null ? new LinkedList<>() : roomList;
        this.taskList = taskList == null ? new LinkedList<>() : taskList;
        this.assignmentList = assignmentList == null ? new LinkedList<>() : assignmentList;
        this.changeList = changeList == null ? new LinkedList<>() : changeList;
        this.reservationList = reservationList == null ? new LinkedList<>() : reservationList;
    }

    public Result generate(LocalDateTime from, LocalDateTime to) {
        LinkedListInterface<TaskRow> rows = new LinkedList<>();
        int trackedCount = 0;
        int completedCount = 0;

        for (int i = 0; i < taskList.size(); i++) {
            Task task = taskList.get(i);
            if (task.isDeleted() || task.getTaskType() != TaskType.CHECKOUT_CLEAN) {
                continue;
            }
            if (!inRange(task.getStartDateTime(), from, to)) {
                continue;
            }

            TaskRow row = new TaskRow(task);
            row.room = findRoom(task.getRoomId());
            row.completedAt = taskEndTime(task);
            row.durationMinutes = row.completedAt == null || task.getStartDateTime() == null ? null
                    : Duration.between(task.getStartDateTime(), row.completedAt).toMinutes();
            row.checkoutAt = linkedCheckout(row.room);

            trackedCount++;
            if (row.completedAt != null) {
                completedCount++;
            }
            rows.addBack(row);
        }

        return new Result(toTable(rows), buildChart(rows), buildSummary(rows, trackedCount, completedCount));
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

    private Room findRoom(String roomId) {
        if (roomId == null) {
            return null;
        }
        for (int i = 0; i < roomList.size(); i++) {
            if (roomId.equalsIgnoreCase(roomList.get(i).getRoomId())) {
                return roomList.get(i);
            }
        }
        return null;
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

    private LocalDateTime taskEndTime(Task task) {
        if (task.getTaskStatus() != TaskStatus.COMPLETED) {
            return null;
        }
        LocalDateTime end = task.getEndDateTime();
        if (end != null) {
            return end;
        }
        LocalDateTime latest = null;
        for (int i = 0; i < changeList.size(); i++) {
            TaskAssignmentChange change = changeList.get(i);
            if (change.getTaskId() == null || !change.getTaskId().equals(task.getTaskId())) {
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

    private LocalDateTime linkedCheckout(Room room) {
        if (room == null || room.getRoomId() == null) {
            return null;
        }
        LocalDateTime latest = null;
        for (int i = 0; i < reservationList.size(); i++) {
            Reservation reservation = reservationList.get(i);
            if (reservation.getRoomId() == null || !reservation.getRoomId().equalsIgnoreCase(room.getRoomId())) {
                continue;
            }
            if (reservation.getTimestamps() == null) {
                continue;
            }
            LocalDateTime checkout = reservation.getTimestamps().getActualCheckOutTime();
            if (checkout != null && (latest == null || checkout.isAfter(latest))) {
                latest = checkout;
            }
        }
        return latest;
    }

    private String[][] toTable(LinkedListInterface<TaskRow> rows) {
        String[][] table = new String[rows.size() + 1][7];
        table[0] = new String[] { "Room ID", "Room Type", "Task Status",
                "Started", "Completed", "Duration (min)", "Checked Out" };
        for (int i = 0; i < rows.size(); i++) {
            TaskRow row = rows.get(i);
            Task task = row.task;
            table[i + 1] = new String[] {
                    task.getRoomId() == null ? "-" : task.getRoomId(),
                    row.room == null || row.room.getRoomType() == null ? "-" : row.room.getRoomType().name(),
                    task.getTaskStatus() == null ? "-" : task.getTaskStatus().name(),
                    formatTime(task.getStartDateTime()),
                    formatTime(row.completedAt),
                    row.durationMinutes == null ? "In progress" : String.valueOf(row.durationMinutes),
                    formatTime(row.checkoutAt)
            };
        }
        return table;
    }

    private String formatTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(TIME_FMT);
    }

    private ReportChart buildChart(LinkedListInterface<TaskRow> rows) {
        ReportChart chart = new ReportChart("Average Turnover Time by Room Type (min)");
        long[][] sums = new long[RoomType.values().length][2];
        for (int i = 0; i < rows.size(); i++) {
            TaskRow row = rows.get(i);
            if (row.durationMinutes == null || row.room == null || row.room.getRoomType() == null) {
                continue;
            }
            long[] acc = sums[row.room.getRoomType().ordinal()];
            acc[0] += row.durationMinutes;
            acc[1]++;
        }
        for (RoomType type : RoomType.values()) {
            long[] acc = sums[type.ordinal()];
            double avg = acc[1] == 0 ? 0 : (double) acc[0] / acc[1];
            chart.addBar(type.name(), avg,
                    "(" + acc[1] + " task" + (acc[1] == 1 ? "" : "s") + ")");
        }
        return chart;
    }

    private String[] buildSummary(LinkedListInterface<TaskRow> rows, int trackedCount, int completedCount) {
        double completionRate = trackedCount == 0 ? 0 : (double) completedCount / trackedCount * 100;
        long totalMinutes = 0;
        RoomType fastestType = null;
        RoomType slowestType = null;
        double fastestAvg = Double.MAX_VALUE;
        double slowestAvg = 0;

        long[][] sums = new long[RoomType.values().length][2];
        for (int i = 0; i < rows.size(); i++) {
            TaskRow row = rows.get(i);
            if (row.durationMinutes == null || row.room == null || row.room.getRoomType() == null) {
                continue;
            }
            totalMinutes += row.durationMinutes;
            long[] acc = sums[row.room.getRoomType().ordinal()];
            acc[0] += row.durationMinutes;
            acc[1]++;
        }
        for (RoomType type : RoomType.values()) {
            long[] acc = sums[type.ordinal()];
            if (acc[1] == 0) {
                continue;
            }
            double avg = (double) acc[0] / acc[1];
            if (avg < fastestAvg) {
                fastestAvg = avg;
                fastestType = type;
            }
            if (avg > slowestAvg) {
                slowestAvg = avg;
                slowestType = type;
            }
        }

        double overallAvg = completedCount == 0 ? 0 : (double) totalMinutes / completedCount;
        String rateText = String.format("%.1f%%", completionRate);
        String rateColor = completionRate >= 80 ? Ansi.GREEN : Ansi.RED;

        return new String[] {
                Ansi.bold("Total Turnover Tasks: ") + trackedCount,
                Ansi.bold("Completed Turnover Tasks: ") + completedCount,
                Ansi.bold("Turnover Completion Rate: ") + Ansi.color(rateColor, rateText),
                Ansi.bold("Overall Average Turnover: ") + Math.round(overallAvg) + " min",
                Ansi.bold("Fastest Turning Room Type: ")
                        + (fastestType == null ? "-" : fastestType.name() + " (" + Math.round(fastestAvg) + " min)"),
                Ansi.bold("Slowest Turning Room Type: ")
                        + (slowestType == null ? "-" : slowestType.name() + " (" + Math.round(slowestAvg) + " min)")
        };
    }

    private static class TaskRow implements Comparable<TaskRow> {
        final Task task;
        Room room;
        LocalDateTime completedAt;
        Long durationMinutes;
        LocalDateTime checkoutAt;

        TaskRow(Task task) {
            this.task = task;
        }

        @Override
        public int compareTo(TaskRow other) {
            if (durationMinutes == null && other.durationMinutes == null) {
                return task.getTaskId().compareToIgnoreCase(other.task.getTaskId());
            }
            if (durationMinutes == null) {
                return 1;
            }
            if (other.durationMinutes == null) {
                return -1;
            }
            int c = Long.compare(durationMinutes, other.durationMinutes);
            return c != 0 ? c : task.getTaskId().compareToIgnoreCase(other.task.getTaskId());
        }
    }

    public static class Result {
        private final String[][] table;
        private final LinkedListInterface<ReportChart> charts;
        private final String[] summary;

        Result(String[][] table, ReportChart chart, String[] summary) {
            this.table = table;
            LinkedListInterface<ReportChart> chartList = new LinkedList<>();
            chartList.addBack(chart);
            this.charts = chartList;
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