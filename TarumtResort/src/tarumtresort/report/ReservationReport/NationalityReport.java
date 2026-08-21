package tarumtresort.report.ReservationReport;

import java.time.LocalDateTime;

import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;
import tarumtresort.entity.Guest;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.enums.ReservationStatus;
import tarumtresort.report.ReportChart;
import tarumtresort.utility.Ansi;

// Author: Chai Chee Tong

public class NationalityReport {

    private final ListInterface<Guest> guestList;
    private final ListInterface<Reservation> reservationList;

    public NationalityReport(ListInterface<Guest> guestList, ListInterface<Reservation> reservationList) {
        this.guestList = guestList == null ? new DoublyLinkedList<>() : guestList;
        this.reservationList = reservationList == null ? new DoublyLinkedList<>() : reservationList;
    }

    /**
     * Generates the report. from/to may be null (unbounded range);
     * statusFilter may be null (all statuses).
     */
    public Result generate(LocalDateTime from, LocalDateTime to, ReservationStatus statusFilter) {
        ListInterface<NationalityRow> rows = new DoublyLinkedList<>();
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
            row.totalNights += r.getNumberOfNights();
            addGuestIdIfNew(row, r.getGuestId());
            totalCounted++;
        }

        sortByReservationCountDescending(rows);

        return new Result(toTable(rows, totalCounted), buildCharts(rows), summary(rows, totalCounted));
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
    private NationalityRow findOrCreateRow(ListInterface<NationalityRow> rows, String nationality) {
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
    private void sortByReservationCountDescending(ListInterface<NationalityRow> rows) {
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

    private ListInterface<ReportChart> buildCharts(ListInterface<NationalityRow> rows) {
        ListInterface<ReportChart> charts = new DoublyLinkedList<>();

        ReportChart reservationsChart = new ReportChart("Reservations by Nationality");
        for (int i = 0; i < rows.size(); i++) {
            NationalityRow row = rows.get(i);
            reservationsChart.addBar(row.nationality, row.reservationCount,
                    "(" + row.reservationCount + " reservation" + (row.reservationCount == 1 ? "" : "s") + ")");
        }
        charts.addBack(reservationsChart);

        ReportChart avgNightsChart = new ReportChart("Average Length of Stay by Nationality (nights)");
        for (int i = 0; i < rows.size(); i++) {
            NationalityRow row = rows.get(i);
            avgNightsChart.addBar(row.nationality, row.averageNights(),
                    "(" + row.totalNights + " night" + (row.totalNights == 1 ? "" : "s")
                            + " / " + row.reservationCount + " reservation" + (row.reservationCount == 1 ? "" : "s") + ")");
        }
        charts.addBack(avgNightsChart);

        ReportChart repeatRateChart = new ReportChart("Repeat Guest Rate by Nationality");
        for (int i = 0; i < rows.size(); i++) {
            NationalityRow row = rows.get(i);
            int guestCount = row.distinctGuestIds.size();
            repeatRateChart.addBar(row.nationality, row.repeatRate(),
                    "(" + row.reservationCount + " reservation" + (row.reservationCount == 1 ? "" : "s")
                            + " / " + guestCount + " guest" + (guestCount == 1 ? "" : "s") + ")");
        }
        charts.addBack(repeatRateChart);

        return charts;
    }

    // -------------------- output --------------------

    private String[][] toTable(ListInterface<NationalityRow> rows, int totalCounted) {
        String[][] table = new String[rows.size() + 1][6];
        table[0] = new String[] { "Nationality", "Total Reservations", "% of Matched Reservations",
                "Distinct Guests", "Avg Nights", "Repeat Rate" };
        for (int i = 0; i < rows.size(); i++) {
            NationalityRow row = rows.get(i);
            double percent = totalCounted == 0 ? 0 : (double) row.reservationCount / totalCounted * 100;
            table[i + 1] = new String[] {
                    row.nationality,
                    String.valueOf(row.reservationCount),
                    String.format("%.1f%%", percent),
                    String.valueOf(row.distinctGuestIds.size()),
                    String.format("%.1f", row.averageNights()),
                    String.format("%.2f", row.repeatRate())
            };
        }
        return table;
    }

    private String[] summary(ListInterface<NationalityRow> rows, int totalCounted) {
        if (rows.isEmpty()) {
            return new String[] { Ansi.bold("Total Reservations Matched: ") + totalCounted };
        }
        NationalityRow top = rows.get(0);
        NationalityRow bottom = rows.get(rows.size() - 1);

        NationalityRow longestStay = rows.get(0);
        NationalityRow mostRepeat = rows.get(0);
        for (int i = 1; i < rows.size(); i++) {
            NationalityRow row = rows.get(i);
            if (row.averageNights() > longestStay.averageNights()) {
                longestStay = row;
            }
            if (row.repeatRate() > mostRepeat.repeatRate()) {
                mostRepeat = row;
            }
        }

        return new String[] {
                Ansi.bold("Total Reservations Matched: ") + totalCounted,
                Ansi.bold("Nationalities Represented: ") + rows.size(),
                Ansi.bold("Most Reservations: ")
                        + Ansi.color(Ansi.GREEN, top.nationality + " (" + top.reservationCount + ")"),
                Ansi.bold("Fewest Reservations: ")
                        + Ansi.color(Ansi.YELLOW, bottom.nationality + " (" + bottom.reservationCount + ")"),
                Ansi.bold("Longest Average Stay: ")
                        + Ansi.color(Ansi.GREEN, longestStay.nationality + " ("
                                + String.format("%.1f", longestStay.averageNights()) + " nights)"),
                Ansi.bold("Highest Repeat Guest Rate: ")
                        + Ansi.color(Ansi.GREEN, mostRepeat.nationality + " ("
                                + String.format("%.2f", mostRepeat.repeatRate()) + " reservations per guest)")
        };
    }

    // implements Comparable only to satisfy ListInterface<T extends Comparable<T>>;
    // the report's own ordering is done by the hand-written selection sort above, not this
    private static class NationalityRow implements Comparable<NationalityRow> {
        final String nationality;
        int reservationCount;
        int totalNights;
        final ListInterface<String> distinctGuestIds = new DoublyLinkedList<>();

        NationalityRow(String nationality) {
            this.nationality = nationality;
        }

        double averageNights() {
            return reservationCount == 0 ? 0 : (double) totalNights / reservationCount;
        }

        // reservations per distinct guest - close to 1 means mostly one-time
        // visitors, higher means guests from this nationality come back
        double repeatRate() {
            int guestCount = distinctGuestIds.size();
            return guestCount == 0 ? 0 : (double) reservationCount / guestCount;
        }

        @Override
        public int compareTo(NationalityRow other) {
            return this.nationality.compareToIgnoreCase(other.nationality);
        }
    }

    public static class Result {
        private final String[][] table;
        private final ListInterface<ReportChart> charts;
        private final String[] summary;

        Result(String[][] table, ListInterface<ReportChart> charts, String[] summary) {
            this.table = table;
            this.charts = charts == null ? new DoublyLinkedList<>() : charts;
            this.summary = summary;
        }

        public String[][] getTable() {
            return table;
        }

        public ListInterface<ReportChart> getCharts() {
            return charts;
        }

        public String[] getSummary() {
            return summary;
        }

        public boolean isEmpty() {
            return table == null || table.length <= 1;
        }
    }
}
