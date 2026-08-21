package tarumtresort.report;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Guest;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.enums.ReservationStatus;
import tarumtresort.utility.Ansi;

/**
 *
 * Nationality Demand Report: which guest nationality books the most / fewest
 * reservations.
 *
 * Dependencies: Guest (nationality), Reservation (guestId, status, registration
 * timestamp). Filters: date range (registration timestamp), reservation status.
 *
 * Report data (aggregation, search, sort) uses only LinkedListInterface - no
 * java.util collection framework. Row lookup is a hand-written linear search
 * (find-or-create), final ordering is a hand-written selection sort - no
 * addSorted()/Collections.sort(). java.util.List is only used at the very end
 * to satisfy ReportChart/ReportResult's existing constructor signatures.
 */
public class NationalityReport {

    private final LinkedListInterface<Guest> guestList;
    private final LinkedListInterface<Reservation> reservationList;

    public NationalityReport(LinkedListInterface<Guest> guestList, LinkedListInterface<Reservation> reservationList) {
        this.guestList = guestList == null ? new LinkedList<>() : guestList;
        this.reservationList = reservationList == null ? new LinkedList<>() : reservationList;
    }

    /**
     * Generates the report. from/to may be null (unbounded range);
     * statusFilter may be null (all statuses).
     */
    public ReportResult generate(LocalDateTime from, LocalDateTime to, ReservationStatus statusFilter) {
        LinkedListInterface<NationalityRow> rows = new LinkedList<>();
        int totalCounted = 0;

        for (int i = 0; i < reservationList.size(); i++) {
            Reservation r = reservationList.get(i);
            if (r.isDeleted()) {
                continue;
            }
            LocalDateTime registeredAt = r.getTimestamps() == null ? null : r.getTimestamps().getRegistrationTimestamp();
            if (!inRange(registeredAt, from, to)) {
                continue;
            }
            if (statusFilter != null && r.getStatus() != statusFilter) {
                continue;
            }

            Guest guest = findGuestById(r.getGuestId());
            String nationality = guest == null || guest.getNationality() == null ? "Unknown" : guest.getNationality();

            NationalityRow row = findOrCreateRow(rows, nationality);
            row.reservationCount++;
            addGuestIdIfNew(row, r.getGuestId());
            totalCounted++;
        }

        sortByReservationCountDescending(rows);

        return new ReportResult(toTable(rows, totalCounted), summary(rows, totalCounted), buildCharts(rows), null);
    }

    // -------------------- search helpers --------------------

    private Guest findGuestById(String guestId) {
        if (guestId == null) {
            return null;
        }
        for (int i = 0; i < guestList.size(); i++) {
            Guest guest = guestList.get(i);
            if (guest.getGuestId() != null && guest.getGuestId().equals(guestId)) {
                return guest;
            }
        }
        return null;
    }

    // linear search: find the row already tracking this nationality, or append a new one
    private NationalityRow findOrCreateRow(LinkedListInterface<NationalityRow> rows, String nationality) {
        for (int i = 0; i < rows.size(); i++) {
            NationalityRow row = rows.get(i);
            if (row.nationality.equalsIgnoreCase(nationality)) {
                return row;
            }
        }
        NationalityRow created = new NationalityRow(nationality);
        rows.addBack(created);
        return created;
    }

    private void addGuestIdIfNew(NationalityRow row, String guestId) {
        if (guestId == null) {
            return;
        }
        for (int i = 0; i < row.distinctGuestIds.size(); i++) {
            if (row.distinctGuestIds.get(i).equals(guestId)) {
                return;
            }
        }
        row.distinctGuestIds.addBack(guestId);
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

    // -------------------- sort --------------------

    // hand-written selection sort: most reservations first, ties broken alphabetically
    private void sortByReservationCountDescending(LinkedListInterface<NationalityRow> rows) {
        int n = rows.size();
        for (int i = 0; i < n - 1; i++) {
            int bestIndex = i;
            for (int j = i + 1; j < n; j++) {
                if (isHigherPriority(rows.get(j), rows.get(bestIndex))) {
                    bestIndex = j;
                }
            }
            if (bestIndex != i) {
                NationalityRow temp = rows.get(i);
                rows.set(i, rows.get(bestIndex));
                rows.set(bestIndex, temp);
            }
        }
    }

    private boolean isHigherPriority(NationalityRow a, NationalityRow b) {
        if (a.reservationCount != b.reservationCount) {
            return a.reservationCount > b.reservationCount;
        }
        return a.nationality.compareToIgnoreCase(b.nationality) < 0;
    }

    // -------------------- charts --------------------

    private List<ReportChart> buildCharts(LinkedListInterface<NationalityRow> rows) {
        List<ReportChart> charts = new ArrayList<>();

        ReportChart reservationsChart = new ReportChart("Reservations by Nationality");
        for (int i = 0; i < rows.size(); i++) {
            NationalityRow row = rows.get(i);
            reservationsChart.addBar(row.nationality, row.reservationCount,
                    "(" + row.reservationCount + " reservation" + (row.reservationCount == 1 ? "" : "s") + ")");
        }
        charts.add(reservationsChart);

        ReportChart guestsChart = new ReportChart("Distinct Guests by Nationality");
        for (int i = 0; i < rows.size(); i++) {
            NationalityRow row = rows.get(i);
            int guestCount = row.distinctGuestIds.size();
            guestsChart.addBar(row.nationality, guestCount,
                    "(" + guestCount + " guest" + (guestCount == 1 ? "" : "s") + ")");
        }
        charts.add(guestsChart);

        return charts;
    }

    // -------------------- output --------------------

    private String[][] toTable(LinkedListInterface<NationalityRow> rows, int totalCounted) {
        String[][] table = new String[rows.size() + 1][4];
        table[0] = new String[] { "Nationality", "Total Reservations", "% of Matched Reservations", "Distinct Guests" };
        for (int i = 0; i < rows.size(); i++) {
            NationalityRow row = rows.get(i);
            double percent = totalCounted == 0 ? 0 : (double) row.reservationCount / totalCounted * 100;
            table[i + 1] = new String[] {
                    row.nationality,
                    String.valueOf(row.reservationCount),
                    String.format("%.1f%%", percent),
                    String.valueOf(row.distinctGuestIds.size())
            };
        }
        return table;
    }

    private String[] summary(LinkedListInterface<NationalityRow> rows, int totalCounted) {
        if (rows.isEmpty()) {
            return new String[] { Ansi.bold("Total Reservations Matched: ") + totalCounted };
        }
        NationalityRow top = rows.get(0);
        NationalityRow bottom = rows.get(rows.size() - 1);
        return new String[] {
                Ansi.bold("Total Reservations Matched: ") + totalCounted,
                Ansi.bold("Nationalities Represented: ") + rows.size(),
                Ansi.bold("Most Reservations: ")
                        + Ansi.color(Ansi.GREEN, top.nationality + " (" + top.reservationCount + ")"),
                Ansi.bold("Fewest Reservations: ")
                        + Ansi.color(Ansi.YELLOW, bottom.nationality + " (" + bottom.reservationCount + ")")
        };
    }

    // implements Comparable only to satisfy LinkedListInterface<T extends Comparable<T>>;
    // the report's own ordering is done by the hand-written selection sort above, not this
    private static class NationalityRow implements Comparable<NationalityRow> {
        final String nationality;
        int reservationCount;
        final LinkedListInterface<String> distinctGuestIds = new LinkedList<>();

        NationalityRow(String nationality) {
            this.nationality = nationality;
        }

        @Override
        public int compareTo(NationalityRow other) {
            return this.nationality.compareToIgnoreCase(other.nationality);
        }
    }
}
