package tarumtresort.report.PriorityReservationReport;

import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Reservation;

// Author: Lee Boon Yew
/**
 * Sorted reservation index - the searching half of this module's reports.
 *
 * Reservations are inserted in reservationId order with addSorted (an
 * insertion sort), which is what makes a binary search by id possible
 * afterwards: binary search is only correct on sorted input, so the sort is
 * what buys the O(log n) comparison count.
 *
 * Honest caveat for the demo: the underlying ADT is a linked list, so get(i)
 * still walks the chain. The saving here is in the NUMBER OF COMPARISONS
 * (log n instead of n), not in the number of node hops. On an array-backed
 * list the same code would also be O(log n) in traversal.
 *
 * It replaces the linear getReservation() scan the controller uses, which is
 * O(n) comparisons per lookup and is run once per priority record.
 */
public class ReservationIndex {

    private final LinkedListInterface<Key> keys = new LinkedList<>();

    public ReservationIndex(LinkedListInterface<Reservation> reservations) {
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

    /**
     * Binary search by reservation id. Returns null when the id is absent,
     * so every caller must handle a missing join.
     */
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
