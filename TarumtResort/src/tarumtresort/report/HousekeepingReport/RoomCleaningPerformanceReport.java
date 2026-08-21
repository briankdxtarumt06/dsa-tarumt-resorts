package tarumtresort.report.HousekeepingReport;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Room;
import tarumtresort.entity.Task;
import tarumtresort.entity.TaskAssignmentChange;
import tarumtresort.entity.TaskStatusChange;
import tarumtresort.entity.enums.RoomType;
import tarumtresort.entity.enums.TaskStatus;
import tarumtresort.entity.enums.TaskType;
import tarumtresort.report.ReportChart;
import tarumtresort.utility.Ansi;

// Author: Brian Kam Ding Xian
public class RoomCleaningPerformanceReport {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final TaskType[] VOLUME_TYPES = {
        TaskType.CLEANING, TaskType.ROOM_SERVICE,
        TaskType.INSPECTION, TaskType.MAINTENANCE
    };

    private final LinkedListInterface<Room> roomList;
    private final LinkedListInterface<Task> taskList;
    private final LinkedListInterface<TaskAssignmentChange> changeList;

    public RoomCleaningPerformanceReport(LinkedListInterface<Room> roomList,
            LinkedListInterface<Task> taskList, LinkedListInterface<TaskAssignmentChange> changeList) {
        this.roomList = roomList == null ? new LinkedList<>() : roomList;
        this.taskList = taskList == null ? new LinkedList<>() : taskList;
        this.changeList = changeList == null ? new LinkedList<>() : changeList;
    }

    public Result generate(LocalDateTime from, LocalDateTime to) {
        LinkedListInterface<TaskRow> rows = new LinkedList<>();

        for (int i = 0; i < taskList.size(); i++) {
            Task task = taskList.get(i);
            if (task.isDeleted() || task.getTaskType() == TaskType.UNKNOWN) {
                continue;
            }
            if (!inRange(task.getStartDateTime(), from, to)) {
                continue;
            }

            TaskRow row = new TaskRow(task);
            row.room = findRoom(task.getRoomId());
            row.durationMinutes = getCleanDuration(task);
            row.overdue = isOverdue(row.room == null ? null : row.room.getRoomType(), row.durationMinutes);

            rows.addSorted(row);
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

    private Long getCleanDuration(Task task) {
        if (task == null || task.getTaskStatus() != TaskStatus.COMPLETED) {
            return null;
        }
        LocalDateTime start = null;
        LocalDateTime end = null;

        LinkedListInterface<TaskStatusChange> history = task.getStatusHistory();
        for (int i = 0; i < history.size(); i++) {
            TaskStatusChange change = history.get(i);
            if (change == null) {
                continue;
            }
            if (change.getTaskStatus() == TaskStatus.IN_PROGRESS && start == null) {
                start = change.getDateTime();
            }
            if (change.getTaskStatus() == TaskStatus.COMPLETED) {
                end = change.getDateTime();
            }
        }

        if (start == null) {
            start = task.getStartDateTime();
        }
        if (end == null) {
            end = task.getEndDateTime();
        }
        if (end == null) {
            end = terminalChangeTime(task.getTaskId());
        }
        if (start == null || end == null) {
            return null;
        }
        return Math.max(0, Duration.between(start, end).toMinutes());
    }

    private LocalDateTime terminalChangeTime(String taskId) {
        if (taskId == null) {
            return null;
        }
        LocalDateTime latest = null;
        for (int i = 0; i < changeList.size(); i++) {
            TaskAssignmentChange change = changeList.get(i);
            if (change.getTaskId() == null || !change.getTaskId().equals(taskId)) {
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

    private boolean isOverdue(RoomType type, Long durationMinutes) {
        if (type == null || durationMinutes == null) {
            return false;
        }
        return durationMinutes > standardMinutes(type);
    }

    private int standardMinutes(RoomType type) {
        return switch (type) {
            case STANDARD_SINGLE -> 25;
            case STANDARD_DOUBLE -> 30;
            case STANDARD_TRIPLE -> 35;
            case DELUXE_SINGLE -> 30;
            case DELUXE_DOUBLE -> 35;
            case DELUXE_TRIPLE -> 40;
            case SUITE -> 50;
        };
    }

    private String[][] toTable(LinkedListInterface<TaskRow> rows) {
        String[][] table = new String[rows.size() + 1][7];
        table[0] = new String[] { "Room ID", "Room Type", "Task Status",
                "Priority", "Started", "Completed", "Duration (min)" };
        for (int i = 0; i < rows.size(); i++) {
            TaskRow row = rows.get(i);
            Task task = row.task;
            table[i + 1] = new String[] {
                    task.getRoomId() == null ? "-" : task.getRoomId(),
                    row.room == null || row.room.getRoomType() == null ? "-" : row.room.getRoomType().toString(),
                    task.getTaskStatus() == null ? "-" : task.getTaskStatus().toString(),
                    task.getTaskPriority() == null ? "-" : task.getTaskPriority().toString(),
                    formatTime(task.getStartDateTime()),
                    formatTime(task.getEndDateTime()),
                    row.durationMinutes == null ? "In progress" : String.valueOf(row.durationMinutes)
            };
        }
        return table;
    }

    private String formatTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(TIME_FMT);
    }

    private LinkedListInterface<ReportChart> buildCharts(LinkedListInterface<TaskRow> rows) {
        LinkedListInterface<ReportChart> charts = new LinkedList<>();
        charts.addBack(buildAverageChart(rows));
        charts.addBack(buildVolumeChart(rows));
        return charts;
    }

    private ReportChart buildAverageChart(LinkedListInterface<TaskRow> rows) {
        ReportChart chart = new ReportChart("Average Cleaning Time by Room Type (min)");
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
            chart.addBar(type.toString(), avg,
                    "(" + acc[1] + " task" + (acc[1] == 1 ? "" : "s") + ")");
        }
        return chart;
    }

    private ReportChart buildVolumeChart(LinkedListInterface<TaskRow> rows) {
        ReportChart chart = new ReportChart("Task Volume by Task Type");
        int[] counts = new int[TaskType.values().length];
        for (int i = 0; i < rows.size(); i++) {
            Task task = rows.get(i).task;
            if (task.getTaskType() == null) {
                continue;
            }
            counts[task.getTaskType().ordinal()]++;
        }
        for (TaskType type : VOLUME_TYPES) {
            chart.addBar(type.toString(), counts[type.ordinal()],
                    "(" + counts[type.ordinal()] + " task" + (counts[type.ordinal()] == 1 ? "" : "s") + ")");
        }
        return chart;
    }

    private String[] buildSummary(LinkedListInterface<TaskRow> rows) {
        long[][] sums = new long[RoomType.values().length][2];
        int[] overdue = new int[RoomType.values().length];
        int[] typeCounts = new int[TaskType.values().length];
        long totalMinutes = 0;
        int measuredCount = 0;

        for (int i = 0; i < rows.size(); i++) {
            TaskRow row = rows.get(i);
            Task task = row.task;
            if (task.getTaskType() != null) {
                typeCounts[task.getTaskType().ordinal()]++;
            }
            if (row.durationMinutes == null) {
                continue;
            }
            totalMinutes += row.durationMinutes;
            measuredCount++;
            if (row.room != null && row.room.getRoomType() != null) {
                long[] acc = sums[row.room.getRoomType().ordinal()];
                acc[0] += row.durationMinutes;
                acc[1]++;
                if (row.overdue) {
                    overdue[row.room.getRoomType().ordinal()]++;
                }
            }
        }

        RoomType fastestType = null;
        RoomType slowestType = null;
        double fastestAvg = Double.MAX_VALUE;
        double slowestAvg = 0;
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

        RoomType mostOverdueType = null;
        double highestOverdueRate = 0;
        for (RoomType type : RoomType.values()) {
            long measured = sums[type.ordinal()][1];
            if (measured == 0) {
                continue;
            }
            double rate = (double) overdue[type.ordinal()] / measured;
            if (rate > highestOverdueRate) {
                highestOverdueRate = rate;
                mostOverdueType = type;
            }
        }

        TaskType mostCommonType = null;
        int mostCommonCount = 0;
        for (TaskType type : VOLUME_TYPES) {
            if (typeCounts[type.ordinal()] > mostCommonCount) {
                mostCommonCount = typeCounts[type.ordinal()];
                mostCommonType = type;
            }
        }

        double overallAvg = measuredCount == 0 ? 0 : (double) totalMinutes / measuredCount;

        return new String[] {
                Ansi.bold("Fastest Room Type: ")
                        + (fastestType == null ? "-"
                                : fastestType.toString() + " (" + Math.round(fastestAvg) + " min)"),
                Ansi.bold("Slowest Room Type: ")
                        + (slowestType == null ? "-"
                                : slowestType.toString() + " (" + Math.round(slowestAvg) + " min)"),
                Ansi.bold("Most Overdue Room Type: ")
                        + (mostOverdueType == null ? "-"
                                : mostOverdueType.toString() + " (" + Math.round(highestOverdueRate * 100)
                                        + "% overdue)"),
                Ansi.bold("Most Common Task Type: ")
                        + (mostCommonType == null ? "-" : mostCommonType.toString() + " (" + mostCommonCount
                                + " task" + (mostCommonCount == 1 ? "" : "s") + ")"),
                Ansi.bold("Overall Avg Clean Time: ") + Math.round(overallAvg) + " min"
        };
    }

    private static class TaskRow implements Comparable<TaskRow> {
        final Task task;
        Room room;
        Long durationMinutes;
        boolean overdue;

        TaskRow(Task task) {
            this.task = task;
        }

        private int typeRank() {
            RoomType type = room == null ? null : room.getRoomType();
            return type == null ? Integer.MAX_VALUE : type.ordinal();
        }

        @Override
        public int compareTo(TaskRow other) {
            int c = Integer.compare(typeRank(), other.typeRank());
            if (c != 0) {
                return c;
            }
            String thisRoom = task.getRoomId() == null ? "" : task.getRoomId();
            String otherRoom = other.task.getRoomId() == null ? "" : other.task.getRoomId();
            return thisRoom.compareToIgnoreCase(otherRoom);
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