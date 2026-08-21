package tarumtresort.report.PriorityReservationReport;

import java.time.Duration;
import java.time.LocalDateTime;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.PriorityReservation;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.enums.PriorityLevel;
import tarumtresort.entity.enums.ReservationStatus;

// Author: Lee Boon Yew
/**
 * Shared queue mechanics for both Priority Reservation reports.
 *
 * Holds the one thing this module exists to decide - the order guests are
 * served in - plus the two measurements that only mean something inside a
 * priority queue:
 *
 *   GUESTS DISPLACED  how many reservations registered EARLIER than this one
 *                     but sit BEHIND it in the queue. Plain meaning: how many
 *                     people this guest jumped ahead of.
 *
 *   TIMES OVERTAKEN   the mirror image - how often this record was passed by
 *                     someone who registered LATER. A tier that is overtaken
 *                     often is a tier the queue is failing.
 *
 * Ordering rule matches PriorityReservationController.generateVIPQueue:
 * highest rank first, ties broken by earliest registration (FIFO within a
 * tier). It is expressed here as compareTo + addSorted (insertion sort)
 * rather than the controller's selection sort, and the report classes own it
 * so that no report ever calls back into the controller.
 */
public class QueueOrdering {

    private QueueOrdering() {
    }

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
    public static void assignPositionsAndDisplacement(LinkedListInterface<Entry> queue) {
        if (queue == null) {
            return;
        }
        int total = queue.size();
        for (int i = 0; i < total; i++) {
            queue.get(i).position = i + 1;
        }
        for (int i = 0; i < total; i++) {
            Entry ahead = queue.get(i);
            if (ahead.registeredAt == null) {
                continue;
            }
            for (int j = i + 1; j < total; j++) {
                Entry behind = queue.get(j);
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
    public static int countPriorityInversions(LinkedListInterface<Entry> queue) {
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
     * One queued priority reservation. Sorting into a LinkedList via
     * addSorted produces the VIP queue order directly.
     */
    public static class Entry implements Comparable<Entry> {

        private final PriorityReservation priority;
        private final Reservation reservation;
        private final LocalDateTime registeredAt;

        private int position;
        private int guestsDisplaced;
        private int timesOvertaken;

        public Entry(PriorityReservation priority, Reservation reservation) {
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
        public int compareTo(Entry other) {
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
}
