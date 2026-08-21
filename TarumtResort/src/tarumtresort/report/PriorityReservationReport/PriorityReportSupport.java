package tarumtresort.report.PriorityReservationReport;

import java.time.Duration;
import java.time.LocalDateTime;
import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;
import tarumtresort.entity.PriorityReservation;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.Staff;
import tarumtresort.entity.enums.PriorityLevel;
import tarumtresort.entity.enums.ReservationStatus;

// Author: Lee Boon Yew

public final class PriorityReportSupport {

    private PriorityReportSupport() {
    }

    // ==================== QUEUE MECHANICS ====================
    // Kept inside this report so the file is self-contained: the ordering
    // rule, the sorted index it searches, and the displacement measurements
    // all belong to this report's calculation.

    /**
     * Service-level promise per tier, in minutes. Management sets these: the
     * higher the tier, the shorter the wait the resort has committed to.
     */
    public static int slaTargetMinutes(PriorityLevel level) {
        if (level == null) {
            return Integer.MAX_VALUE;
        }
        return switch (level) {
            case EMERGENCY -> 15;
            case DIAMOND -> 30;
            case PLATINUM -> 60;
            case GOLD -> 120;
            case SLIVER -> 240;
            case PENALTY -> 480;
        };
    }

    /** A reservation that has already been given a room or checked in/out. */
    public static boolean isServed(ReservationStatus status) {
        return status == ReservationStatus.ASSIGNED
                || status == ReservationStatus.CHECKED_IN
                || status == ReservationStatus.CHECKED_OUT;
    }

    /**
     * Assigns 1-based queue positions, then computes guestsDisplaced and
     * timesOvertaken in a single pass over every ordered pair.
     *
     * For each pair (i, j) with i ahead of j, if j registered EARLIER than i
     * then i has displaced j: i jumped the queue past a guest who was already
     * waiting. That increments i's displacement and j's overtaken count.
     */
    public static void assignPositionsAndDisplacement(ListInterface<QueueEntry> queue) {
        if (queue == null) {
            return;
        }
        int total = queue.size();
        for (int i = 0; i < total; i++) {
            queue.get(i).position = i + 1;
        }
        for (int i = 0; i < total; i++) {
            QueueEntry ahead = queue.get(i);
            if (ahead.registeredAt == null) {
                continue;
            }
            for (int j = i + 1; j < total; j++) {
                QueueEntry behind = queue.get(j);
                if (behind.registeredAt == null) {
                    continue;
                }
                if (behind.registeredAt.isBefore(ahead.registeredAt)) {
                    ahead.guestsDisplaced++;
                    behind.timesOvertaken++;
                }
            }
        }
    }

    /**
     * Self-check on the ordering: counts places where a LOWER rank sits ahead
     * of a HIGHER rank. On a correctly ordered queue this is always 0, so any
     * other number printed in a report means compareTo is wrong.
     */
    public static int countPriorityInversions(ListInterface<QueueEntry> queue) {
        if (queue == null || queue.size() < 2) {
            return 0;
        }
        int inversions = 0;
        for (int i = 0; i < queue.size() - 1; i++) {
            if (queue.get(i).rank() < queue.get(i + 1).rank()) {
                inversions++;
            }
        }
        return inversions;
    }

    /**
     * One queued priority reservation. Sorting these into a DoublyLinkedList via
     * addSorted produces the VIP queue order directly - highest rank first,
     * ties broken by earliest registration (FIFO within a tier).
     */
    public static class QueueEntry implements Comparable<QueueEntry> {

        private final PriorityReservation priority;
        private final Reservation reservation;
        private final LocalDateTime registeredAt;

        private int position;
        private int guestsDisplaced;
        private int timesOvertaken;

        public QueueEntry(PriorityReservation priority, Reservation reservation) {
            this.priority = priority;
            this.reservation = reservation;
            this.registeredAt = (reservation == null || reservation.getTimestamps() == null)
                    ? null
                    : reservation.getTimestamps().getRegistrationTimestamp();
        }

        public PriorityReservation getPriority() {
            return priority;
        }

        public Reservation getReservation() {
            return reservation;
        }

        public LocalDateTime getRegisteredAt() {
            return registeredAt;
        }

        public int getPosition() {
            return position;
        }

        public int getGuestsDisplaced() {
            return guestsDisplaced;
        }

        public int getTimesOvertaken() {
            return timesOvertaken;
        }

        public PriorityLevel getLevel() {
            return priority == null ? null : priority.getPriorityLevel();
        }

        public ReservationStatus getStatus() {
            return reservation == null ? null : reservation.getStatus();
        }

        public String getReservationId() {
            return priority == null ? "-" : priority.getReservationId();
        }

        /** A level set by a staff member rather than derived from loyalty tier. */
        public boolean isOverridden() {
            return priority != null
                    && priority.getOverriddenBy() != null
                    && !priority.getOverriddenBy().isEmpty();
        }

        public int rank() {
            PriorityLevel level = getLevel();
            return level == null ? Integer.MIN_VALUE : level.getRank();
        }

        /**
         * Minutes the guest has waited.
         *   served   registration to assignedTime, falling back to check-in
         *   waiting  registration to now (still accruing)
         *   other    null, so cancelled records never distort an average
         */
        public Long waitingMinutes() {
            if (registeredAt == null) {
                return null;
            }
            ReservationStatus status = getStatus();
            LocalDateTime end;
            if (isServed(status)) {
                end = reservation.getTimestamps().getAssignedTime();
                if (end == null) {
                    end = reservation.getTimestamps().getActualCheckInTime();
                }
            } else if (status == ReservationStatus.WAITING) {
                end = LocalDateTime.now();
            } else {
                return null;
            }
            if (end == null) {
                return null;
            }
            return Math.max(0, Duration.between(registeredAt, end).toMinutes());
        }

        public boolean breachedSla() {
            Long waited = waitingMinutes();
            return waited != null && waited > slaTargetMinutes(getLevel());
        }

        // highest rank first, then earliest registration (FIFO inside a tier),
        // then reservation id so the order is fully deterministic
        @Override
        public int compareTo(QueueEntry other) {
            int comparison = Integer.compare(other.rank(), rank());
            if (comparison != 0) {
                return comparison;
            }
            if (registeredAt != null && other.registeredAt != null) {
                comparison = registeredAt.compareTo(other.registeredAt);
                if (comparison != 0) {
                    return comparison;
                }
            } else if (registeredAt == null && other.registeredAt != null) {
                return 1;
            } else if (registeredAt != null && other.registeredAt == null) {
                return -1;
            }
            return getReservationId().compareToIgnoreCase(other.getReservationId());
        }
    }

    /**
     * Sorted reservation index - the searching half of this report.
     *
     * Reservations are inserted in reservationId order with addSorted (an
     * insertion sort), which is what makes a binary search by id possible
     * afterwards: binary search is only correct on sorted input, so the sort
     * is what buys the O(log n) comparison count.
     *
     * Honest caveat: the underlying ADT is a linked list, so get(i) still
     * walks the chain. The saving is in the NUMBER OF COMPARISONS (log n
     * instead of n), not in the number of node hops.
     */
    public static class ReservationIndex {

        private final ListInterface<Key> keys = new DoublyLinkedList<>();

        public ReservationIndex(ListInterface<Reservation> reservations) {
            if (reservations == null) {
                return;
            }
            for (int i = 0; i < reservations.size(); i++) {
                Reservation reservation = reservations.get(i);
                if (reservation == null || reservation.getReservationId() == null) {
                    continue;
                }
                keys.addSorted(new Key(reservation));
            }
        }

        public int size() {
            return keys.size();
        }

        /** Binary search by reservation id. Returns null when the id is absent. */
        public Reservation find(String reservationId) {
            if (reservationId == null) {
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
                int comparison = key.id().compareToIgnoreCase(reservationId);
                if (comparison == 0) {
                    return key.reservation;
                }
                if (comparison < 0) {
                    low = mid + 1;
                } else {
                    high = mid - 1;
                }
            }
            return null;
        }

        private static class Key implements Comparable<Key> {

            private final Reservation reservation;

            Key(Reservation reservation) {
                this.reservation = reservation;
            }

            String id() {
                return reservation.getReservationId();
            }

            @Override
            public int compareTo(Key other) {
                return id().compareToIgnoreCase(other.id());
            }
        }
    }

    /**
     * Sorted staff index - same sort-then-binary-search pairing as
     * ReservationIndex, used to resolve the staff id stored in
     * PriorityReservation.getOverriddenBy() into a real name.
     */
    public static class StaffIndex {

        private final ListInterface<Key> keys = new DoublyLinkedList<>();

        public StaffIndex(ListInterface<Staff> staffList) {
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
}
