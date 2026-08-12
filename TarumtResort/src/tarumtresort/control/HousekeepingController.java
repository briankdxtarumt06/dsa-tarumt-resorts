package tarumtresort.control;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.boundary.HousekeepingUI;
import tarumtresort.dao.TaskAssignmentChangeDAO;
import tarumtresort.dao.TaskAssignmentDAO;
import tarumtresort.entity.Staff;
import tarumtresort.entity.Task;
import tarumtresort.entity.TaskAssignment;
import tarumtresort.entity.TaskAssignmentChange;
import tarumtresort.entity.enums.TaskPriority;

/**
 *
 * @author Brian
 *
 * Housekeeping module: links Staff <-> Task via TaskAssignment records,
 * links Task <-> Room via Task.roomId, and keeps an append-only
 * TaskAssignmentChange history (status + staff + date & time) separate from
 * the assignment records themselves.
 *
 * Scheduling rules (schedule gap analysis):
 * - Every room takes CLEANING_DURATION_MINUTES to clean, so every cleaning
 *   task requires a continuous free interval of that length.
 * - A staff is "available" at an interval when they have no non-cancelled
 *   assignment covering any part of it (different task only; members of the
 *   same task may share a window as a team).
 * - No two tasks of the same staff may share a timestamp; a task may only
 *   start after the previous one is done.
 * - The analysis scans each eligible staff's schedule chronologically
 *   (within their SHIFT_START..SHIFT_END shift boundaries) to find the
 *   earliest continuous free gap large enough for the task. The staff who
 *   becomes available first wins; ties are broken by current workload, then
 *   lowest staff id. If no gap fits today, the task is deferred to the next
 *   shift start so the worker always has the task on their schedule.
 * - Slot booking is done as one load -> compute -> insert -> save step
 *   against the JSON files (single-writer console), so no double-booking is
 *   possible in the current architecture.
 *
 * FUTURE INTEGRATION:
 * - processGuestCheckout() is the hook for the Reservation module to call
 *   on real guest checkout; validate roomId against RoomDAO and update
 *   RoomStatus (CLEANING / AVILABLE) once room persistence exists.
 * - Shift hours (SHIFT_START / SHIFT_END) are default constants; move them
 *   into the Staff entity + Staff Management UI for per-staff shifts.
 * - When the system gains multiple writers, replace the load-compute-save
 *   flow with a real transactional database / file lock.
 * - Trigger Notification records for assigned staff; query change history
 *   by date range for supervisor reports.
 * - Supervisor approval flow can be gated by staff role once auth exists.
 */
public class HousekeepingController {

    // every room takes 60 minutes to clean (default task duration)
    public static final int CLEANING_DURATION_MINUTES = 60;

    // default shift boundaries for schedule gap analysis
    public static final LocalTime SHIFT_START = LocalTime.of(8, 0);
    public static final LocalTime SHIFT_END = LocalTime.of(20, 0);

    // safety bound when deferring tasks across shifts
    private static final int MAX_DEFER_DAYS = 30;

    // ui declaration
    private HousekeepingUI ui;

    // dao declarations
    private static final TaskAssignmentDAO taskAssignmentDAO = new TaskAssignmentDAO();
    private static final TaskAssignmentChangeDAO taskAssignmentChangeDAO = new TaskAssignmentChangeDAO();

    // shared controllers for lookups
    private final TaskManagementController taskController = new TaskManagementController();
    private final StaffManagementController staffController = new StaffManagementController();

    // checkout result holder (returned to calling modules)
    public static class CheckoutResult {
        public final String taskId;
        public final String roomId;
        public final String staffId;
        public final String staffName;
        public final LocalDateTime scheduledStart;
        public final LocalDateTime scheduledEnd;
        public final boolean deferred;

        public CheckoutResult(String taskId, String roomId, String staffId, String staffName,
                              LocalDateTime scheduledStart, LocalDateTime scheduledEnd, boolean deferred) {
            this.taskId = taskId;
            this.roomId = roomId;
            this.staffId = staffId;
            this.staffName = staffName;
            this.scheduledStart = scheduledStart;
            this.scheduledEnd = scheduledEnd;
            this.deferred = deferred;
        }
    }

    // constructors
    public HousekeepingController() {
    }

    public HousekeepingController(HousekeepingUI ui) {
        this.ui = ui;
    }

    // housekeeping management
    public void runHousekeeping() {

        int choice;

        do {
            choice = ui.getMenuChoice();

            switch (choice) {
                case 1:
                    assignStaffToTask();
                    break;
                case 2:
                    ui.listAllAssignments(assignmentListToTable(getAllAssignments()));
                    break;
                case 3:
                    searchAssignment();
                    break;
                case 4:
                    updateAssignment();
                    break;
                case 5:
                    assignTaskToRoom();
                    break;
                case 6:
                    viewTasksByRoom();
                    break;
                case 7:
                    simulateGuestCheckout();
                    break;
                case 8:
                    updateAssignmentStatus();
                    break;
                case 9:
                    updateTaskStatus();
                    break;
                case 10:
                    viewChangeHistory();
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

    // ------------------------------------------------------------------
    // GUEST CHECKOUT: auto task creation + auto assignment (timetable)
    // ------------------------------------------------------------------

    /**
     * Creates a cleaning task for the room and assigns it to the housekeeping
     * staff whose earliest free 60-minute slot (>= checkout time) comes first.
     * If nobody is free at checkout time, the task start is deferred to the
     * earliest slot found, i.e. the staff already has the task on their
     * schedule right after their current cleaning.
     * <p>
     * FUTURE INTEGRATION: call this method from the Reservation module when a
     * guest checks out; validate roomId against RoomDAO and flip the room to
     * CLEANING then AVILABLE once room persistence exists.
     */
    public CheckoutResult processGuestCheckout(String roomId, LocalDateTime checkoutTime) {

        if (roomId == null || checkoutTime == null) {
            return null;
        }

        // auto create the cleaning task for the room
        String taskName = "Clean " + roomId;
        String taskId = taskController.createTask(taskName, "Housekeeping", TaskPriority.MEDIUM, checkoutTime, roomId);

        if (taskId == null) {
            return null; // a cleaning task for this room already exists
        }

        return autoAssignTask(taskId, checkoutTime, roomId);
    }

    /**
     * Assigns an existing task to the best available housekeeping staff using
     * the schedule gap analysis (see findEarliestFreeSlot). The slot is
     * booked atomically: assignments are loaded once, the earliest gap is
     * computed against that snapshot, the assignment is inserted and the
     * list is saved in the same step, so no double-booking can happen.
     */
    private CheckoutResult autoAssignTask(String taskId, LocalDateTime requestedStart, String roomId) {

        StaffAndSlot best = findEarliestFreeSlot(requestedStart, CLEANING_DURATION_MINUTES, null);

        // set task start to the scheduled slot (deferred if staff are busy)
        LocalDateTime scheduledStart = best == null ? requestedStart : best.slotStart;
        taskController.updateTaskStartDateTime(taskId, scheduledStart);

        String assignmentId = insertAssignment(generateAssignmentId(),
                "Pending", scheduledStart,
                best == null ? null : best.staff,
                taskController.getTaskById(taskId));

        boolean deferred = best != null && best.slotStart.isAfter(requestedStart);

        return new CheckoutResult(
                taskId,
                roomId,
                best == null ? null : best.staff.getStaffId(),
                best == null ? null : best.staff.getStaffName(),
                scheduledStart,
                scheduledStart.plusMinutes(CLEANING_DURATION_MINUTES),
                deferred
        );
    }

    /**
     * Schedule gap analysis over all eligible housekeeping staff.
     * Every eligible worker is scanned chronologically for the earliest
     * continuous free interval >= requestedStart that fits a task of
     * durationMinutes inside the shift boundaries. The worker who becomes
     * available first wins; ties are broken by the lowest current workload,
     * then by the lowest staff id. The staff who dropped / declined a task
     * may be excluded via excludeStaffId.
     */
    private StaffAndSlot findEarliestFreeSlot(LocalDateTime requestedStart, int durationMinutes, String excludeStaffId) {

        StaffAndSlot best = null;

        for (int i = 0; i < staffController.getAllStaffs().size(); i++) {

            Staff staff = staffController.getAllStaffs().get(i);

            if (!"Housekeeping".equalsIgnoreCase(staff.getDepartment())) {
                continue;
            }
            if (!"Available".equalsIgnoreCase(staff.getAvailabilityStatus())) {
                continue;
            }
            if (excludeStaffId != null && excludeStaffId.equals(staff.getStaffId())) {
                continue; // do not immediately reassign to the staff who declined
            }

            LocalDateTime gapStart = earliestGapStart(staff, requestedStart, durationMinutes, null);

            if (gapStart == null) {
                continue;
            }
            if (best == null || gapStart.isBefore(best.slotStart)) {
                best = new StaffAndSlot(staff, gapStart);
            } else if (gapStart.equals(best.slotStart)) {
                // tie: prefer the worker with the smaller current workload
                if (countWorkload(staff) < countWorkload(best.staff)) {
                    best = new StaffAndSlot(staff, gapStart);
                }
            }
        }

        return best;
    }

    /**
     * Gap analysis for a single staff member:
     * 1. Collect the occupied intervals from their non-cancelled assignment
     *    records (other tasks only; the candidate task itself is excluded so
     *    team members can share its window).
     * 2. Sort the intervals chronologically and scan them against the staff's
     *    shift window. The cursor always points at the next possible start:
     *    a gap [cursor, nextIntervalStart) that is large enough - and still
     *    ends before the shift ends - is the earliest valid start; otherwise
     *    the cursor jumps past the occupied interval.
     * 3. If no gap fits inside today's shift, the search defers to the next
     *    day's SHIFT_START so the worker always has the task on schedule.
     */
    private LocalDateTime earliestGapStart(Staff staff, LocalDateTime requestedStart, int durationMinutes, String excludeTaskId) {

        // step 1: occupied intervals of this staff (transient java.util list
        // used only for the gap computation; not part of the ADT store)
        List<Interval> intervals = new ArrayList<>();

        LinkedListInterface<TaskAssignment> assignments = getAllAssignments();

        for (int i = 0; i < assignments.size(); i++) {

            TaskAssignment assignment = assignments.get(i);

            if (assignment.getAssingedStaff() == null
                    || assignment.getAssingedStaff().getStaffId() == null
                    || !assignment.getAssingedStaff().getStaffId().equals(staff.getStaffId())) {
                continue;
            }
            if ("Cancelled".equalsIgnoreCase(assignment.getStatus())) {
                continue;
            }

            Task task = assignment.getAssignedTask();
            if (task == null || task.getStartDateTime() == null) {
                continue;
            }
            // same task = team members, may share the window
            if (task.getTaskId().equals(excludeTaskId)) {
                continue;
            }

            LocalDateTime taskStart = task.getStartDateTime();

            intervals.add(new Interval(taskStart, taskStart.plusMinutes(CLEANING_DURATION_MINUTES)));
        }

        // step 2: chronological scan within each day's shift
        Collections.sort(intervals, (a, b) -> a.start.compareTo(b.start));
        LocalDateTime start = requestedStart;

        for (int day = 0; day < MAX_DEFER_DAYS; day++) {

            LocalDate date = start.toLocalDate();
            LocalDateTime shiftStart = LocalDateTime.of(date, SHIFT_START);
            LocalDateTime shiftEnd = LocalDateTime.of(date, SHIFT_END);

            LocalDateTime cursor = start.isBefore(shiftStart) ? shiftStart : start;

            if (!cursor.isBefore(shiftEnd)) {
                // requested start is already past today's shift end
                start = LocalDateTime.of(date.plusDays(1), SHIFT_START);
                continue;
            }

            for (Interval interval : intervals) {

                if (!interval.end.isAfter(cursor)) {
                    continue; // interval already finished before the cursor
                }

                if (!interval.start.isBefore(cursor)) {
                    // continuous gap [cursor, interval.start)
                    if (gapFitsWithinShift(cursor, interval.start, durationMinutes, shiftEnd)) {
                        return cursor;
                    }
                }

                // gap too small (or overlapping) -> jump past the interval
                if (interval.end.isAfter(cursor)) {
                    cursor = interval.end;
                }
                if (!cursor.isBefore(shiftEnd)) {
                    break; // the rest of today's shift is unusable
                }
            }

            // gap after the last occupied interval
            if (gapFitsWithinShift(cursor, shiftEnd, durationMinutes, shiftEnd)) {
                return cursor;
            }

            // step 3: no fit today -> defer to the next shift
            start = LocalDateTime.of(date.plusDays(1), SHIFT_START);
        }

        return null;
    }

    /**
     * True when [cursor, limit) is long enough for the task and the task
     * still ends before or at the shift end.
     */
    private boolean gapFitsWithinShift(LocalDateTime cursor, LocalDateTime limit, int durationMinutes, LocalDateTime shiftEnd) {
        return !cursor.plusMinutes(durationMinutes).isAfter(limit)
                && !cursor.plusMinutes(durationMinutes).isAfter(shiftEnd);
    }

    // occupied windows are sorted chronologically before the gap scan
    private static class Interval {
        final LocalDateTime start;
        final LocalDateTime end;

        Interval(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
        }
    }

    // current workload of a staff: non-cancelled, non-completed assignments
    private int countWorkload(Staff staff) {

        int count = 0;
        LinkedListInterface<TaskAssignment> assignments = getAllAssignments();

        for (int i = 0; i < assignments.size(); i++) {

            TaskAssignment assignment = assignments.get(i);

            if (assignment.getAssingedStaff() == null
                    || !assignment.getAssingedStaff().getStaffId().equals(staff.getStaffId())) {
                continue;
            }
            if ("Cancelled".equalsIgnoreCase(assignment.getStatus())
                    || "Completed".equalsIgnoreCase(assignment.getStatus())) {
                continue;
            }

            count++;
        }

        return count;
    }

    private boolean windowsOverlap(LocalDateTime s1, LocalDateTime e1, LocalDateTime s2, LocalDateTime e2) {
        return s1.isBefore(e2) && s2.isBefore(e1);
    }

    // ------------------------------------------------------------------
    // ASSIGNMENT CREATION (manual, multi-staff aware)
    // ------------------------------------------------------------------

    /**
     * Creates an assignment linking staff <-> task. Multiple staff may share
     * the SAME task window (team), but a staff can never have two different
     * tasks overlapping. Returns the new assignment id, or an error code.
     */
    public String createAssignment(String staffId, String taskId, String status, LocalDateTime dateTimeAssigned) {

        Staff staff = staffController.getStaffById(staffId);
        if (staff == null) {
            return null; // staff record does not exist
        }
        if ("Resigned".equalsIgnoreCase(staff.getAvailabilityStatus())) {
            return "STAFF_UNAVAILABLE";
        }

        Task task = taskController.getTaskById(taskId);
        if (task == null) {
            return "TASK_NOT_FOUND";
        }

        // the task window is fixed by its start date & time
        if (task.getStartDateTime() == null) {
            return "TASK_NOT_FOUND";
        }

        LocalDateTime windowStart = task.getStartDateTime();
        LocalDateTime windowEnd = windowStart.plusMinutes(CLEANING_DURATION_MINUTES);

        if (!isStaffFreeForTask(staff, windowStart, windowEnd, taskId)) {
            return "WINDOW_OVERLAP"; // staff already has another task in this window
        }

        // FUTURE INTEGRATION: pick workload / shift aware staff automatically
        // and trigger a Notification for the assigned staff.

        String assignmentId = generateAssignmentId();

        insertAssignment(assignmentId, status, dateTimeAssigned, staff, task);

        return assignmentId;
    }

    /**
     * True when the staff has no non-cancelled record with an overlapping
     * window belonging to a DIFFERENT task than the candidate task.
     */
    private boolean isStaffFreeForTask(Staff staff, LocalDateTime windowStart, LocalDateTime windowEnd, String taskId) {

        LinkedListInterface<TaskAssignment> assignments = getAllAssignments();

        for (int i = 0; i < assignments.size(); i++) {

            TaskAssignment assignment = assignments.get(i);

            if (assignment.getAssingedStaff() == null
                    || !assignment.getAssingedStaff().getStaffId().equals(staff.getStaffId())) {
                continue;
            }
            if ("Cancelled".equalsIgnoreCase(assignment.getStatus())) {
                continue;
            }

            Task task = assignment.getAssignedTask();
            if (task == null || task.getStartDateTime() == null) {
                continue;
            }
            if (task.getTaskId().equals(taskId)) {
                continue; // team member on the same task is allowed
            }

            LocalDateTime taskStart = task.getStartDateTime();
            LocalDateTime taskEnd = taskStart.plusMinutes(CLEANING_DURATION_MINUTES);

            if (windowsOverlap(windowStart, windowEnd, taskStart, taskEnd)) {
                return false;
            }
        }

        return true;
    }

    // ------------------------------------------------------------------
    // PER-WORKER ASSIGNMENT STATUS (multi-worker rules)
    // ------------------------------------------------------------------

    /**
     * Updates an individual worker's assignment status without necessarily
     * changing the parent task. Statuses such as Completed, Cancelled (drop /
     * decline), Handed Off, Paused or Work Finished belong to the worker;
     * the parent task follows the aggregated rules below.
     * <p>
     * Rules:
     * - Handed Off / Paused: worker status only; parent task unchanged.
     * - Completed / Work Finished: parent task stays active; once ALL
     *   non-cancelled workers are done the caller is told to ask a supervisor
     *   for final approval (parent task is then completed via task status
     *   update).
     * - Cancelled (drop / decline): if it was the only active worker, the
     *   parent task goes back to Pending and is auto-reassigned via the
     *   earliest-free-slot timetable.
     *
     * Returns: "NOT_FOUND", "UPDATED", "ALL_DONE" or "REASSIGNED".
     */
    public String updateAssignmentStatus(String assignmentId, String status) {

        LinkedListInterface<TaskAssignment> assignments = getAllAssignments();

        TaskAssignment target = null;

        for (int i = 0; i < assignments.size(); i++) {
            if (assignments.get(i).getTaskAssignmentId().equals(assignmentId)) {
                target = assignments.get(i);
                break;
            }
        }

        if (target == null) {
            return "NOT_FOUND";
        }

        target.setStatus(status);
        taskAssignmentDAO.saveTaskAssignmentList(assignments);

        // record the worker's status change in the separate change history
        appendAssignmentChange(target, status, LocalDateTime.now());

        // FUTURE INTEGRATION: propagate worker status updates to the parent
        // task via the task status stack and update RoomStatus once rooms are
        // persisted.

        String taskId = target.getAssignedTask() == null ? null : target.getAssignedTask().getTaskId();
        if (taskId == null) {
            return "UPDATED";
        }

        if ("Cancelled".equalsIgnoreCase(status) && countActiveWorkers(taskId) == 0) {

            // the only worker dropped/declined: parent task back to Pending,
            // then auto-reassign to the next free slot (not to the same worker)
            if (!"Pending".equalsIgnoreCase(taskController.getTaskById(taskId).peekTaskStatus())) {
                taskController.updateTaskStatus(taskId, "Pending");
            }

            String droppedStaffId = target.getAssingedStaff() == null ? null : target.getAssingedStaff().getStaffId();

            return reassignTask(taskId, droppedStaffId);
        }

        if (isTaskFullyFinished(taskId)) {
            // all workers done; parent task remains for supervisor approval
            return "ALL_DONE";
        }

        return "UPDATED";
    }

    private int countActiveWorkers(String taskId) {

        int count = 0;
        LinkedListInterface<TaskAssignment> assignments = getAllAssignments();

        for (int i = 0; i < assignments.size(); i++) {
            TaskAssignment assignment = assignments.get(i);
            Task task = assignment.getAssignedTask();
            if (task != null && task.getTaskId().equals(taskId)
                    && !"Cancelled".equalsIgnoreCase(assignment.getStatus())) {
                count++;
            }
        }

        return count;
    }

    private boolean isTaskFullyFinished(String taskId) {

        LinkedListInterface<TaskAssignment> assignments = getAllAssignments();
        boolean any = false;

        for (int i = 0; i < assignments.size(); i++) {
            TaskAssignment assignment = assignments.get(i);
            Task task = assignment.getAssignedTask();
            if (task == null || !task.getTaskId().equals(taskId)
                    || "Cancelled".equalsIgnoreCase(assignment.getStatus())) {
                continue;
            }
            any = true;
            if (!("Completed".equalsIgnoreCase(assignment.getStatus())
                    || "Work Finished".equalsIgnoreCase(assignment.getStatus()))) {
                return false;
            }
        }

        return any;
    }

    /**
     * Reassigns a task to the earliest free slot on the timetable after a
     * worker dropped/declined it (the declining worker is excluded from the
     * re-pool). Returns "REASSIGNED" or "UPDATED" when no slot could be planned.
     */
    private String reassignTask(String taskId, String excludeStaffId) {

        Task task = taskController.getTaskById(taskId);
        if (task == null || task.getStartDateTime() == null) {
            return "UPDATED";
        }

        StaffAndSlot best = findEarliestFreeSlot(task.getStartDateTime(), CLEANING_DURATION_MINUTES, excludeStaffId);

        if (best == null) {
            // no slot could be planned; parent task stays Pending for manual assignment
            return "UPDATED";
        }

        taskController.updateTaskStartDateTime(taskId, best.slotStart);

        insertAssignment(generateAssignmentId(), "Pending", best.slotStart, best.staff, taskController.getTaskById(taskId));

        return "REASSIGNED";
    }

    // ------------------------------------------------------------------
    // TASK STATUS UPDATE (records a TaskAssignmentChange via the controller)
    // ------------------------------------------------------------------

    public boolean updateTaskStatus(String taskId, String status) {
        // TaskManagementController records every change as a TaskAssignmentChange
        return taskController.updateTaskStatus(taskId, status);
    }

    // ------------------------------------------------------------------
    // ROOM LINK
    // ------------------------------------------------------------------

    // link a task to a room (Task.roomId)
    public boolean assignTaskToRoom(String taskId, String roomId) {
        // FUTURE INTEGRATION: validate roomId against RoomDAO / Reservation
        // module and update RoomStatus to CLEANING once room persistence exists.
        return taskController.updateTaskRoomId(taskId, roomId);
    }

    // ------------------------------------------------------------------
    // QUERIES
    // ------------------------------------------------------------------

    public TaskAssignment getAssignmentById(String assignmentId) {

        LinkedListInterface<TaskAssignment> assignments = getAllAssignments();

        for (int i = 0; i < assignments.size(); i++) {
            if (assignments.get(i).getTaskAssignmentId().equals(assignmentId)) {
                return assignments.get(i);
            }
        }

        return null;
    }

    public LinkedListInterface<TaskAssignment> getAssignmentsByStaff(String staffId) {

        LinkedListInterface<TaskAssignment> filteredList = new LinkedList<>();

        for (int i = 0; i < getAllAssignments().size(); i++) {

            TaskAssignment assignment = getAllAssignments().get(i);

            if (assignment.getAssingedStaff() != null
                    && assignment.getAssingedStaff().getStaffId().equals(staffId)) {
                filteredList.addBack(assignment);
            }
        }

        return filteredList;
    }

    public LinkedListInterface<TaskAssignment> getAssignmentsByTask(String taskId) {

        LinkedListInterface<TaskAssignment> filteredList = new LinkedList<>();

        for (int i = 0; i < getAllAssignments().size(); i++) {

            TaskAssignment assignment = getAllAssignments().get(i);

            if (assignment.getAssignedTask() != null
                    && assignment.getAssignedTask().getTaskId().equals(taskId)) {
                filteredList.addBack(assignment);
            }
        }

        return filteredList;
    }

    public static LinkedListInterface<TaskAssignment> getAllAssignments() {
        // always read the latest records from disk (the DAO is the single
        // source of truth, so no controller keeps a stale copy)
        return taskAssignmentDAO.retrieveTaskAssignmentList();
    }

    // rooms: currently free-form text; see FUTURE INTEGRATION note above
    public LinkedListInterface<Task> getTasksByRoom(String roomId) {

        LinkedListInterface<Task> filteredList = new LinkedList<>();
        LinkedListInterface<Task> allTasks = taskController.getAllTasks();

        for (int i = 0; i < allTasks.size(); i++) {

            Task task = allTasks.get(i);

            if (task.getRoomId() != null && task.getRoomId().equalsIgnoreCase(roomId)) {
                filteredList.addBack(task);
            }
        }

        return filteredList;
    }

    // ------------------------------------------------------------------
    // HELPERS
    // ------------------------------------------------------------------

    /**
     * Reassign: change the staff and/or task of an existing assignment.
     */
    public boolean reassignAssignment(String assignmentId, String staffId, String taskId) {

        LinkedListInterface<TaskAssignment> assignments = getAllAssignments();

        for (int i = 0; i < assignments.size(); i++) {

            TaskAssignment assignment = assignments.get(i);

            if (assignment.getTaskAssignmentId().equals(assignmentId)) {

                Staff staff = staffController.getStaffById(staffId);
                Task task = taskController.getTaskById(taskId);

                if (staff == null
                        || "Resigned".equalsIgnoreCase(staff.getAvailabilityStatus())
                        || task == null) {
                    return false;
                }

                assignment.setAssingedStaff(staff);
                assignment.setAssignedTask(task);

                taskAssignmentDAO.saveTaskAssignmentList(assignments);

                return true;
            }
        }

        return false;
    }

    private String insertAssignment(String assignmentId, String status, LocalDateTime dateTimeAssigned,
                                    Staff staff, Task task) {

        LinkedListInterface<TaskAssignment> assignments = getAllAssignments();

        TaskAssignment assignment = new TaskAssignment(assignmentId, status, dateTimeAssigned, staff, task);

        assignments.addSorted(assignment);
        taskAssignmentDAO.saveTaskAssignmentList(assignments);

        return assignmentId;
    }

    static String generateAssignmentId() {

        int max = 0;
        LinkedListInterface<TaskAssignment> assignments = getAllAssignments();

        for (int i = 0; i < assignments.size(); i++) {

            String assignmentId = assignments.get(i).getTaskAssignmentId();

            if (assignmentId == null) {
                continue;
            }

            int number = Integer.parseInt(assignmentId.substring(3));

            if (number > max) {
                max = number;
            }
        }

        return String.format("ASG%012d", max + 1);
    }

    // ------------------------------------------------------------------
    // CHANGE HISTORY (separate entity, tracked from any module)
    // ------------------------------------------------------------------

    /**
     * Records a task status change as a TaskAssignmentChange. Called by
     * TaskManagementController (status update / soft delete) and by this
     * module's auto-reassignment flow. The record keeps the status, the date
     * & time of the change, the staff currently active on the task and a
     * snapshot of the task object (plus the active assignment it belongs to).
     * <p>
     * FUTURE INTEGRATION: trigger a Notification for the staff involved in
     * each new change; query history by date range for supervisor reports.
     */
    public static TaskAssignmentChange appendTaskStatusChange(Task task, String status, LocalDateTime dateTime) {
        if (task == null || status == null) {
            return null;
        }

        TaskAssignment active = getActiveAssignment(getAllAssignments(), task.getTaskId());

        return insertChange(new TaskAssignmentChange(
                generateChangeId(),
                active == null ? null : active.getTaskAssignmentId(),
                status,
                dateTime,
                active == null ? null : active.getAssingedStaff(),
                task
        ));
    }

    /**
     * Records a worker's assignment status change as a TaskAssignmentChange.
     * The TaskAssignment keeps its current status in place; the change is
     * appended to the separate change history.
     */
    public static TaskAssignmentChange appendAssignmentChange(TaskAssignment assignment, String status, LocalDateTime dateTime) {
        if (assignment == null || status == null) {
            return null;
        }

        return insertChange(new TaskAssignmentChange(
                generateChangeId(),
                assignment.getTaskAssignmentId(),
                status,
                dateTime,
                assignment.getAssingedStaff(),
                assignment.getAssignedTask()
        ));
    }

    private static TaskAssignmentChange insertChange(TaskAssignmentChange change) {

        LinkedListInterface<TaskAssignmentChange> changeList = getAllChanges();

        changeList.addSorted(change);
        taskAssignmentChangeDAO.saveTaskAssignmentChangeList(changeList);

        return change;
    }

    public static LinkedListInterface<TaskAssignmentChange> getAllChanges() {
        // always read the latest records from disk (single source of truth)
        return taskAssignmentChangeDAO.retrieveTaskAssignmentChangeList();
    }

    public LinkedListInterface<TaskAssignmentChange> getChangesByTask(String taskId) {

        LinkedListInterface<TaskAssignmentChange> filteredList = new LinkedList<>();

        for (int i = 0; i < getAllChanges().size(); i++) {

            TaskAssignmentChange change = getAllChanges().get(i);

            if (change.getTask() != null && change.getTask().getTaskId().equals(taskId)) {
                filteredList.addBack(change);
            }
        }

        return filteredList;
    }

    private static String generateChangeId() {

        int max = 0;
        LinkedListInterface<TaskAssignmentChange> changeList = getAllChanges();

        for (int i = 0; i < changeList.size(); i++) {

            String changeId = changeList.get(i).getChangeId();

            if (changeId == null) {
                continue;
            }

            int number = Integer.parseInt(changeId.substring(3));

            if (number > max) {
                max = number;
            }
        }

        return String.format("CHG%012d", max + 1);
    }

    private static TaskAssignment getActiveAssignment(LinkedListInterface<TaskAssignment> assignmentList, String taskId) {
        // most recent non-cancelled record for the task carries the active worker
        TaskAssignment active = null;
        LocalDateTime latest = null;

        for (int i = 0; i < assignmentList.size(); i++) {
            TaskAssignment assignment = assignmentList.get(i);
            Task task = assignment.getAssignedTask();
            if (task == null || !task.getTaskId().equals(taskId)) {
                continue;
            }
            if ("Cancelled".equalsIgnoreCase(assignment.getStatus())) {
                continue;
            }
            if (latest == null || (assignment.getDateTimeAssigned() != null
                    && assignment.getDateTimeAssigned().isAfter(latest))) {
                latest = assignment.getDateTimeAssigned();
                active = assignment;
            }
        }
        return active;
    }

    private static class StaffAndSlot {
        final Staff staff;
        final LocalDateTime slotStart;

        StaffAndSlot(Staff staff, LocalDateTime slotStart) {
            this.staff = staff;
            this.slotStart = slotStart;
        }
    }

    // ------------------------------------------------------------------
    // private menu handlers
    // ------------------------------------------------------------------

    private void assignStaffToTask() {
        String staffId = ui.inputStaffId();
        String taskId = ui.inputTaskId();
        String status = ui.inputAssignmentStatus();
        LocalDateTime dateTimeAssigned = ui.inputDateAssigned();

        String result = createAssignment(staffId, taskId, status, dateTimeAssigned);

        if (result == null) {
            ui.printStaffNotFound();
        } else if ("STAFF_UNAVAILABLE".equals(result)) {
            ui.printStaffUnavailable();
        } else if ("TASK_NOT_FOUND".equals(result)) {
            ui.printTaskNotFound();
        } else if ("WINDOW_OVERLAP".equals(result)) {
            ui.printWindowOverlap();
        } else {
            ui.printAssignmentId(result);
            ui.printSuccess();
        }
    }

    private void searchAssignment() {
        int searchChoice = ui.getSearchMenuChoice();
        if (searchChoice == 0) {
            return;
        }
        LinkedListInterface<TaskAssignment> result = new LinkedList<>();
        if (searchChoice == 1) {
            TaskAssignment assignment = getAssignmentById(ui.inputAssignmentId());
            if (assignment != null) {
                result.addBack(assignment);
            }
        } else if (searchChoice == 2) {
            result = getAssignmentsByStaff(ui.inputStaffId());
        } else if (searchChoice == 3) {
            result = getAssignmentsByTask(ui.inputTaskId());
        }
        if (result.isEmpty()) {
            ui.printNotFound();
        } else if (result.size() == 1) {
            ui.printAssignmentDetails(result.getFirst());
        } else {
            ui.listAllAssignments(assignmentListToTable(result));
        }
    }

    private void updateAssignment() {
        String assignmentId = ui.inputAssignmentId();
        if (getAssignmentById(assignmentId) == null) {
            ui.printNotFound();
            return;
        }
        String staffId = ui.inputStaffId();
        String taskId = ui.inputTaskId();
        if (reassignAssignment(assignmentId, staffId, taskId)) {
            ui.printSuccess();
        } else {
            ui.printNotFound();
        }
    }

    private void assignTaskToRoom() {
        String taskId = ui.inputTaskId();
        if (!taskController.taskExists(taskId)) {
            ui.printTaskNotFound();
            return;
        }
        String roomId = ui.inputRoomId();
        if (assignTaskToRoom(taskId, roomId)) {
            ui.printSuccess();
        } else {
            ui.printTaskNotFound();
        }
    }

    private void viewTasksByRoom() {
        String roomId = ui.inputRoomId();
        // FUTURE INTEGRATION: switch to RoomDAO-based display once rooms are
        // persisted (show room details + linked tasks in one view).
        ui.listAllTasks(taskListToTable(getTasksByRoom(roomId)));
    }

    private void simulateGuestCheckout() {
        String roomId = ui.inputRoomId();
        LocalDateTime checkoutTime = ui.inputCheckoutDateTime();

        CheckoutResult result = processGuestCheckout(roomId, checkoutTime);

        if (result == null) {
            ui.printTaskAlreadyExists();
            return;
        }

        ui.printCheckoutResult(
                result.taskId,
                result.roomId,
                result.staffId == null ? null : result.staffId + " (" + result.staffName + ")",
                result.scheduledStart,
                result.scheduledEnd,
                result.deferred
        );
    }

    private void updateAssignmentStatus() {
        String assignmentId = ui.inputAssignmentId();
        if (getAssignmentById(assignmentId) == null) {
            ui.printNotFound();
            return;
        }
        String status = ui.inputWorkerAssignmentStatus();

        String result = updateAssignmentStatus(assignmentId, status);

        if ("NOT_FOUND".equals(result)) {
            ui.printNotFound();
        } else if ("ALL_DONE".equals(result)) {
            ui.printAllWorkersDoneHint(assignmentId);
        } else if ("REASSIGNED".equals(result)) {
            ui.printReassigned();
        } else {
            ui.printSuccess();
        }
    }

    private void updateTaskStatus() {
        String taskId = ui.inputTaskId();
        if (!taskController.taskExists(taskId)) {
            ui.printTaskNotFound();
            return;
        }
        String status = ui.inputTaskStatus();
        if (updateTaskStatus(taskId, status)) {
            ui.printSuccess();
        } else {
            ui.printTaskNotFound();
        }
    }

    private void viewChangeHistory() {
        String taskId = ui.inputOptionalTaskId();
        LinkedListInterface<TaskAssignmentChange> changes = taskId == null ? getAllChanges() : getChangesByTask(taskId);
        ui.listAllChanges(changeListToTable(changes));
    }

    // convert change history list to 2D table
    private String[][] changeListToTable(LinkedListInterface<TaskAssignmentChange> changeList) {
        String[][] data = new String[changeList.size() + 1][6];
        data[0] = new String[]{"Change ID", "Task", "Assignment", "Status", "Staff", "Changed At"};
        for (int i = 0; i < changeList.size(); i++) {
            TaskAssignmentChange change = changeList.get(i);
            Staff staff = change.getStaff();
            Task task = change.getTask();
            data[i + 1] = new String[]{
                change.getChangeId(),
                task == null ? "-" : task.getTaskId() + " (" + task.getTaskName() + ")",
                change.getTaskAssignmentId() == null ? "-" : change.getTaskAssignmentId(),
                change.getStatus(),
                staff == null ? "-" : staff.getStaffId() + " (" + staff.getStaffName() + ")",
                change.getChangedAt() == null ? "-" : change.getChangedAt().toString()
            };
        }
        return data;
    }

    // convert assignment list to 2D table
    private String[][] assignmentListToTable(LinkedListInterface<TaskAssignment> assignmentList) {
        String[][] data = new String[assignmentList.size() + 1][6];
        data[0] = new String[]{"Assignment ID", "Staff", "Task", "Room ID", "Status", "Date & Time Assigned"};
        for (int i = 0; i < assignmentList.size(); i++) {
            TaskAssignment assignment = assignmentList.get(i);
            Staff staff = assignment.getAssingedStaff();
            Task task = assignment.getAssignedTask();
            data[i + 1] = new String[]{
                assignment.getTaskAssignmentId(),
                staff == null ? "-" : staff.getStaffId() + " (" + staff.getStaffName() + ")",
                task == null ? "-" : task.getTaskId() + " (" + task.getTaskName() + ")",
                task == null || task.getRoomId() == null ? "-" : task.getRoomId(),
                assignment.getStatus(),
                assignment.getDateTimeAssigned() == null ? "-" : assignment.getDateTimeAssigned().toString()
            };
        }
        return data;
    }

    // convert task list to 2D table (for viewing tasks by room)
    private String[][] taskListToTable(LinkedListInterface<Task> taskList) {
        // FUTURE INTEGRATION: reuse a shared table converter once extracted into
        // a common utility (e.g. SharedServices) to avoid duplication.
        String[][] data = new String[taskList.size() + 1][6];
        data[0] = new String[]{"Task ID", "Task Name", "Task Type", "Priority", "Current Status", "Start Date & Time"};
        for (int i = 0; i < taskList.size(); i++) {
            Task task = taskList.get(i);
            data[i + 1] = new String[]{
                task.getTaskId(),
                task.getTaskName(),
                task.getTaskType(),
                task.getTaskPriority() == null ? "-" : task.getTaskPriority().name(),
                task.peekTaskStatus() == null ? "-" : task.peekTaskStatus(),
                task.getStartDateTime() == null ? "-" : task.getStartDateTime().toString()
            };
        }
        return data;
    }
}