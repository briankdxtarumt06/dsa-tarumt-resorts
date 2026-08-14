package tarumtresort.report;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Room;
import tarumtresort.entity.Staff;
import tarumtresort.entity.Task;
import tarumtresort.entity.TaskAssignment;
import tarumtresort.entity.enums.RoomType;

/**
 *
 * @author Brian
 *
 * Room Cleaning Report: joins Room, Staff and Task (through TaskAssignment)
 * to show every cleaning task per room with its assigned worker.
 *
 * Dependencies: Room, Staff, Task, TaskAssignment (4 classes).
 * Filters: date range (task start), staff role, room type.
 */
public class RoomCleaningReport {

    private final LinkedListInterface<Room> roomList;
    private final LinkedListInterface<Staff> staffList;
    private final LinkedListInterface<Task> taskList;
    private final LinkedListInterface<TaskAssignment> assignmentList;

    public RoomCleaningReport(LinkedListInterface<Room> roomList, LinkedListInterface<Staff> staffList,
            LinkedListInterface<Task> taskList, LinkedListInterface<TaskAssignment> assignmentList) {
        this.roomList = roomList == null ? new LinkedList<>() : roomList;
        this.staffList = staffList == null ? new LinkedList<>() : staffList;
        this.taskList = taskList == null ? new LinkedList<>() : taskList;
        this.assignmentList = assignmentList == null ? new LinkedList<>() : assignmentList;
    }

    /**
     * Generates the report. from/to may be null (unbounded range),
     * staffRole may be null (all roles), roomType may be null (all types).
     */
    public ReportResult generate(LocalDateTime from, LocalDateTime to, String staffRole, RoomType roomType) {

        // rows: room id, room type, task, priority, status, staff, start time
        List<String[]> rows = new ArrayList<>();

        for (int i = 0; i < taskList.size(); i++) {
            Task task = taskList.get(i);

            // filter 1: date range on the task start date & time
            if (!inRange(task.getStartDateTime(), from, to)) {
                continue;
            }

            // resolve the room of the task
            Room room = findRoom(task.getRoomId());

            // filter 3: room type (unknown room -> only matches "all types")
            if (roomType != null && (room == null || room.getRoomType() != roomType)) {
                continue;
            }

            boolean rowAdded = false;

            // expand every task into one row per assigned worker
            LinkedListInterface<TaskAssignment> taskAssignments = task.getTaskAssignments();
            for (int j = 0; j < taskAssignments.size(); j++) {
                TaskAssignment assignment = taskAssignments.get(j);

                Staff staff = findStaff(assignment.getAssignedStaffId());

                // filter 2: staff role
                if (staffRole != null && (staff == null || !staffRole.equalsIgnoreCase(staff.getStaffRole()))) {
                    continue;
                }

                rows.add(row(room, task, staff));
                rowAdded = true;
            }

            // unassigned tasks still appear (staff column shows "-"),
            // unless a staff role filter was requested
            if (!rowAdded && staffRole == null) {
                rows.add(row(room, task, null));
            }
        }

        return new ReportResult(toTable(rows), summary(rows));
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

    private Staff findStaff(String staffId) {
        if (staffId == null) {
            return null;
        }
        for (int i = 0; i < staffList.size(); i++) {
            if (staffList.get(i).getStaffId().equals(staffId)) {
                return staffList.get(i);
            }
        }
        return null;
    }

    private String[] row(Room room, Task task, Staff staff) {
        return new String[] {
                room == null ? "-" : room.getRoomId(),
                room == null || room.getRoomType() == null ? "-" : room.getRoomType().name(),
                task.getTaskId(),
                task.getTaskName(),
                task.getTaskPriority() == null ? "-" : task.getTaskPriority().name(),
                task.getTaskStatus() == null ? "-" : task.getTaskStatus().name(),
                staff == null ? "-" : staff.getStaffId() + " (" + staff.getStaffName() + ")",
                task.getStartDateTime() == null ? "-" : task.getStartDateTime().toString()
        };
    }

    private String[][] toTable(List<String[]> rows) {
        String[][] table = new String[rows.size() + 1][8];
        table[0] = new String[] { "Room ID", "Room Type", "Task ID", "Task Name", "Priority", "Status",
                "Assigned Staff", "Start Date & Time" };
        for (int i = 0; i < rows.size(); i++) {
            table[i + 1] = rows.get(i);
        }
        return table;
    }

    private String[] summary(List<String[]> rows) {
        // count the distinct rooms actually present in the filtered rows
        List<String> rooms = new ArrayList<>();
        for (String[] row : rows) {
            String roomId = row[0];
            if ("-".equals(roomId) || roomId == null) {
                continue;
            }
            boolean seen = false;
            for (String existing : rooms) {
                if (existing.equalsIgnoreCase(roomId)) {
                    seen = true;
                    break;
                }
            }
            if (!seen) {
                rooms.add(roomId);
            }
        }
        return new String[] {
                "Total task-worker rows: " + rows.size(),
                "Rooms in report: " + rooms.size()
        };
    }
}