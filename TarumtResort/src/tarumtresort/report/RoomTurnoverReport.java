package tarumtresort.report;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Room;
import tarumtresort.entity.Task;
import tarumtresort.entity.TaskAssignment;
import tarumtresort.entity.TaskAssignmentChange;
import tarumtresort.entity.enums.RoomStatus;
import tarumtresort.entity.enums.RoomType;
import tarumtresort.entity.enums.TaskStatus;
import tarumtresort.utility.Ansi;

/**
 *
 * @author Brian
 *
 * Room Turnover &amp; Readiness Report: how fast rooms go from dirty to
 * ready, and which rooms are lagging.
 *
 * Dependencies: Room, Task, TaskAssignment, TaskAssignmentChange (4 classes).
 * Filters: date range (task start), task type = Housekeeping.
 *
 * Turnover workflow: the cleaner is given a 60-minute window per task but may
 * finish early ("Completed" / "Work Finished" assignment change); the
 * supervisor then inspects the room and signs the TASK off as COMPLETED.
 * A room counts as ready only at the supervisor sign-off, i.e. the LATEST
 * COMPLETED change of the task. Tasks without a task-level COMPLETED change
 * are still listed with "In progress" as their turnover time.
 *
 * Enum mapping (spec -> model): TURNOVER_CLEAN / CHECKOUT_CLEAN -> "Housekeeping"
 * task type; OUT_OF_ORDER room status -> MAINTENANCE.
 */
public class RoomTurnoverReport {

    // spec's TURNOVER_CLEAN / CHECKOUT_CLEAN task types; the model stores
    // the free-form type "Housekeeping"
    private static final String TURNOVER_TASK_TYPE = "Housekeeping";

    // rooms are flagged for attention when their last turnover exceeds this
    private static final int ATTENTION_MINUTES = 45;

    private final LinkedListInterface<Room> roomList;
    private final LinkedListInterface<Task> taskList;
    private final LinkedListInterface<TaskAssignment> assignmentList;
    private final LinkedListInterface<TaskAssignmentChange> changeList;

    public RoomTurnoverReport(LinkedListInterface<Room> roomList,
            LinkedListInterface<Task> taskList, LinkedListInterface<TaskAssignment> assignmentList,
            LinkedListInterface<TaskAssignmentChange> changeList) {
        this.roomList = roomList == null ? new LinkedList<>() : roomList;
        this.taskList = taskList == null ? new LinkedList<>() : taskList;
        this.assignmentList = assignmentList == null ? new LinkedList<>() : assignmentList;
        this.changeList = changeList == null ? new LinkedList<>() : changeList;
    }

    /**
     * Generates the report. from/to may be null (unbounded range).
     */
    public ReportResult generate(LocalDateTime from, LocalDateTime to) {

        List<TaskRow> rows = new ArrayList<>();
        Set<String> trackedRooms = new LinkedHashSet<>();
        int completedCount = 0;

        for (int i = 0; i < taskList.size(); i++) {
            Task task = taskList.get(i);

            // filter: only turnover/cleaning tasks started within the period
            if (task.getTaskType() == null || !TURNOVER_TASK_TYPE.equalsIgnoreCase(task.getTaskType())) {
                continue;
            }
            if (!inRange(task.getStartDateTime(), from, to)) {
                continue;
            }

            TaskRow row = new TaskRow(task);
            row.room = findRoom(task.getRoomId());
            row.completedAt = latestCompletedChange(task.getTaskId());
            row.turnoverMinutes = row.completedAt == null ? null
                    : Duration.between(task.getStartDateTime(), row.completedAt).toMinutes();
            row.assignedStaffId = assignedStaffId(task);

            if (row.completedAt != null) {
                completedCount++;
            }
            if (task.getRoomId() != null) {
                trackedRooms.add(task.getRoomId());
            }
            rows.add(row);
        }

        return new ReportResult(
                toTable(rows),
                summary(rows.size(), trackedRooms.size(), completedCount),
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

    private Room findRoom(String roomId) {
        if (roomId == null) {
            return null;
        }
        for (int i = 0; i < roomList.size(); i++) {
            if (roomList.get(i).getRoomId().equalsIgnoreCase(roomId)) {
                return roomList.get(i);
            }
        }
        return null;
    }

    /**
     * Supervisor sign-off: the latest COMPLETED change belonging to the task.
     * Both the worker's "Completed" and the supervisor's task-level
     * "Completed" changes carry the task id; the sign-off is always last.
     * A room counts as ready only when the TASK itself has been signed off
     * (task status COMPLETED): a worker's own "Completed" mark without
     * supervisor approval leaves the room "In progress".
     */
    private LocalDateTime latestCompletedChange(String taskId) {
        Task task = findTask(taskId);
        if (task == null || task.getTaskStatus() != TaskStatus.COMPLETED) {
            return null;
        }
        LocalDateTime latest = null;
        for (int i = 0; i < changeList.size(); i++) {
            TaskAssignmentChange change = changeList.get(i);
            if (change.getTaskId() == null || !change.getTaskId().equals(taskId)) {
                continue;
            }
            if (change.getStatus() == null || !"Completed".equalsIgnoreCase(change.getStatus())) {
                continue;
            }
            if (change.getChangedAt() != null && (latest == null || change.getChangedAt().isAfter(latest))) {
                latest = change.getChangedAt();
            }
        }
        return latest;
    }

    private Task findTask(String taskId) {
        if (taskId == null) {
            return null;
        }
        for (int i = 0; i < taskList.size(); i++) {
            if (taskList.get(i).getTaskId().equalsIgnoreCase(taskId)) {
                return taskList.get(i);
            }
        }
        return null;
    }

    // most recent non-cancelled worker of the task (skips dropped/declined
    // assignments and records without a staff id)
    private String assignedStaffId(Task task) {
        TaskAssignment latest = null;
        for (int i = 0; i < assignmentList.size(); i++) {
            TaskAssignment assignment = assignmentList.get(i);
            if (assignment.getAssignedTaskId() == null
                    || !assignment.getAssignedTaskId().equals(task.getTaskId())) {
                continue;
            }
            if (assignment.getAssignedStaffId() == null
                    || "Cancelled".equalsIgnoreCase(assignment.getStatus())) {
                continue;
            }
            if (latest == null || isAfter(assignment, latest)) {
                latest = assignment;
            }
        }
        return latest == null ? null : latest.getAssignedStaffId();
    }

    private boolean isAfter(TaskAssignment candidate, TaskAssignment current) {
        return candidate.getDateTimeAssigned() != null
                && (current.getDateTimeAssigned() == null
                        || candidate.getDateTimeAssigned().isAfter(current.getDateTimeAssigned()));
    }

    private String[][] toTable(List<TaskRow> rows) {
        String[][] table = new String[rows.size() + 1][7];
        table[0] = new String[] { "Room ID", "Room No.", "Room Type", "Current Status",
                "Last Task Status", "Turnover Time (min)", "Assigned Staff ID" };
        for (int i = 0; i < rows.size(); i++) {
            TaskRow row = rows.get(i);
            Task task = row.task;
            table[i + 1] = new String[] {
                    task.getRoomId() == null ? "-" : task.getRoomId(),
                    row.room == null || row.room.getRoomNumber() == null ? "-" : row.room.getRoomNumber(),
                    row.room == null || row.room.getRoomType() == null ? "-" : row.room.getRoomType().name(),
                    row.room == null || row.room.getRoomStatus() == null ? "-" : row.room.getRoomStatus().name(),
                    task.getTaskStatus() == null ? "-" : task.getTaskStatus().name(),
                    row.turnoverMinutes == null ? "In progress" : String.valueOf(row.turnoverMinutes),
                    row.assignedStaffId == null ? "-" : row.assignedStaffId
            };
        }
        return table;
    }

    private String[] summary(int taskCount, int roomCount, int completedCount) {
        double rate = taskCount == 0 ? 0 : (double) completedCount / taskCount * 100;
        String rateText = String.format("%.1f%%", rate);
        String rateColor = rate >= 80 ? Ansi.GREEN : Ansi.RED;
        return new String[] {
                Ansi.bold("Total Rooms Tracked: ") + roomCount,
                Ansi.bold("Total Turnover Tasks: ") + taskCount,
                Ansi.bold("Turnover Completion Rate: ")
                        + Ansi.color(rateColor, rateText)
        };
    }

    private List<ReportChart> buildCharts(List<TaskRow> rows) {
        List<ReportChart> charts = new ArrayList<>();

        // chart 1: average turnover time by room type (completed tasks only)
        ReportChart chart1 = new ReportChart("Average Turnover Time by Room Type (min)");
        Map<RoomType, long[]> sums = new LinkedHashMap<>();
        for (TaskRow row : rows) {
            if (row.turnoverMinutes == null || row.room == null || row.room.getRoomType() == null) {
                continue;
            }
            long[] acc = sums.computeIfAbsent(row.room.getRoomType(), k -> new long[2]);
            acc[0] += row.turnoverMinutes;
            acc[1]++;
        }
        for (RoomType type : RoomType.values()) {
            long[] acc = sums.get(type);
            if (acc != null) {
                double avg = (double) acc[0] / acc[1];
                chart1.addBar(type.name(), avg, "(" + acc[1] + " task" + (acc[1] == 1 ? "" : "s") + ")");
            }
        }
        charts.add(chart1);

        // chart 2: room status distribution across all tracked rooms
        ReportChart chart2 = new ReportChart("Room Status Distribution");
        Map<RoomStatus, Integer> counts = new LinkedHashMap<>();
        for (int i = 0; i < roomList.size(); i++) {
            Room room = roomList.get(i);
            if (room.getRoomStatus() == null) {
                continue;
            }
            counts.merge(room.getRoomStatus(), 1, Integer::sum);
        }
        for (RoomStatus status : RoomStatus.values()) {
            Integer count = counts.get(status);
            if (count != null) {
                chart2.addBar(status.name(), count, "(room" + (count == 1 ? "" : "s") + ")");
            }
        }
        charts.add(chart2);

        return charts;
    }

    private List<String> buildCallouts(List<TaskRow> rows) {
        List<String> callouts = new ArrayList<>();

        // callout 1: fastest turnovers (top 3 by turnover time ascending)
        List<TaskRow> completed = new ArrayList<>();
        for (TaskRow row : rows) {
            if (row.turnoverMinutes != null) {
                completed.add(row);
            }
        }
        Collections.sort(completed, (a, b) -> Long.compare(a.turnoverMinutes, b.turnoverMinutes));

        callouts.add(Ansi.green(Ansi.bold("◆ Fastest Turnovers (top 3)")));
        if (completed.isEmpty()) {
            callouts.add(Ansi.green("  (no completed turnovers in range)"));
        } else {
            for (int i = 0; i < Math.min(3, completed.size()); i++) {
                TaskRow row = completed.get(i);
                callouts.add(Ansi.green("  " + (i + 1) + ". " + roomLabel(row) + " - "
                        + row.turnoverMinutes + " min (" + row.task.getTaskId() + ")"));
            }
        }

        // callout 2: rooms requiring attention (out of order or > 45 min)
        callouts.add(Ansi.red(Ansi.bold("⚠ Rooms Requiring Attention")));
        boolean any = false;
        for (TaskRow row : rows) {
            boolean outOfOrder = row.room != null && row.room.getRoomStatus() == RoomStatus.MAINTENANCE;
            boolean slow = row.turnoverMinutes != null && row.turnoverMinutes > ATTENTION_MINUTES;
            if (!outOfOrder && !slow) {
                continue;
            }
            any = true;
            String reason = outOfOrder ? "OUT OF ORDER"
                    : row.turnoverMinutes + " min turnover (> " + ATTENTION_MINUTES + ")";
            callouts.add(Ansi.red("  ⚠ " + roomLabel(row) + " - " + reason));
        }
        if (!any) {
            callouts.add(Ansi.red("  (none)"));
        }

        return callouts;
    }

    // "R002 (102 STANDARD)" or "-" when the room is unknown
    private String roomLabel(TaskRow row) {
        if (row.room == null) {
            return row.task.getRoomId() == null ? "-" : row.task.getRoomId();
        }
        String roomNumber = row.room.getRoomNumber() == null ? "-" : row.room.getRoomNumber();
        String roomType = row.room.getRoomType() == null ? "-" : row.room.getRoomType().name();
        return row.room.getRoomId() + " (" + roomNumber + " " + roomType + ")";
    }

    private static class TaskRow {
        final Task task;
        Room room;
        LocalDateTime completedAt;
        Long turnoverMinutes;
        String assignedStaffId;

        TaskRow(Task task) {
            this.task = task;
        }
    }
}