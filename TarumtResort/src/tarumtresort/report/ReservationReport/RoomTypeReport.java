package tarumtresort.report.ReservationReport;

import java.time.LocalDateTime;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.Room;
import tarumtresort.entity.enums.ReservationStatus;
import tarumtresort.entity.enums.RoomType;
import tarumtresort.report.ReportChart;
import tarumtresort.utility.Ansi;

public class RoomTypeReport {

    private final LinkedListInterface<Room> roomList;
    private final LinkedListInterface<Reservation> reservationList;

    public RoomTypeReport(LinkedListInterface<Room> roomList, LinkedListInterface<Reservation> reservationList) {
        this.roomList = roomList == null ? new LinkedList<>() : roomList;
        this.reservationList = reservationList == null ? new LinkedList<>() : reservationList;
    }

    /**
     * Generates the report. from/to may be null (unbounded range);
     * statusFilter may be null (all statuses).
     */
    public Result generate(LocalDateTime from, LocalDateTime to, ReservationStatus statusFilter) {
        LinkedListInterface<RoomTypeRow> rows = new LinkedList<>();
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
            if (r.getRoomTypeRequested() == null) {
                continue;
            }

            RoomTypeRow row = findOrCreateRow(rows, r.getRoomTypeRequested());
            row.reservationCount++;
            totalCounted++;
        }

        // enrich with total room inventory per type, even for room types
        // that have zero matched reservations in this filtered period
        for (int i = 0; i < roomList.size(); i++) {
            Room room = roomList.get(i);
            if (room.getRoomType() == null) {
                continue;
            }
            RoomTypeRow row = findOrCreateRow(rows, room.getRoomType());
            row.totalRoomsOfType++;
        }

        sortByReservationCountDescending(rows);

        return new Result(toTable(rows, totalCounted), buildCharts(rows), summary(rows, totalCounted));
    }

    // -------------------- search helpers --------------------

    // linear search: find the row already tracking this room type, or append a new one
    private RoomTypeRow findOrCreateRow(LinkedListInterface<RoomTypeRow> rows, RoomType roomType) {
        for (int i = 0; i < rows.size(); i++) {
            RoomTypeRow row = rows.get(i);
            if (row.roomType == roomType) {
                return row;
            }
        }
        RoomTypeRow created = new RoomTypeRow(roomType);
        rows.addBack(created);
        return created;
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

    // hand-written selection sort: most reservations first, ties broken by room type name
    private void sortByReservationCountDescending(LinkedListInterface<RoomTypeRow> rows) {
        int n = rows.size();
        for (int i = 0; i < n - 1; i++) {
            int bestIndex = i;
            for (int j = i + 1; j < n; j++) {
                if (isHigherPriority(rows.get(j), rows.get(bestIndex))) {
                    bestIndex = j;
                }
            }
            if (bestIndex != i) {
                RoomTypeRow temp = rows.get(i);
                rows.set(i, rows.get(bestIndex));
                rows.set(bestIndex, temp);
            }
        }
    }

    private boolean isHigherPriority(RoomTypeRow a, RoomTypeRow b) {
        if (a.reservationCount != b.reservationCount) {
            return a.reservationCount > b.reservationCount;
        }
        return a.roomType.name().compareToIgnoreCase(b.roomType.name()) < 0;
    }

    // -------------------- charts --------------------

    private LinkedListInterface<ReportChart> buildCharts(LinkedListInterface<RoomTypeRow> rows) {
        LinkedListInterface<ReportChart> charts = new LinkedList<>();

        ReportChart reservationsChart = new ReportChart("Reservations by Room Type");
        for (int i = 0; i < rows.size(); i++) {
            RoomTypeRow row = rows.get(i);
            reservationsChart.addBar(row.roomType.name(), row.reservationCount,
                    "(" + row.reservationCount + " reservation" + (row.reservationCount == 1 ? "" : "s") + ")");
        }
        charts.addBack(reservationsChart);

        ReportChart inventoryChart = new ReportChart("Room Inventory by Type");
        for (int i = 0; i < rows.size(); i++) {
            RoomTypeRow row = rows.get(i);
            inventoryChart.addBar(row.roomType.name(), row.totalRoomsOfType,
                    "(" + row.totalRoomsOfType + " room" + (row.totalRoomsOfType == 1 ? "" : "s") + ")");
        }
        charts.addBack(inventoryChart);

        return charts;
    }

    // -------------------- output --------------------

    private String[][] toTable(LinkedListInterface<RoomTypeRow> rows, int totalCounted) {
        String[][] table = new String[rows.size() + 1][4];
        table[0] = new String[] { "Room Type", "Total Reservations", "% of Matched Reservations", "Total Rooms of Type" };
        for (int i = 0; i < rows.size(); i++) {
            RoomTypeRow row = rows.get(i);
            double percent = totalCounted == 0 ? 0 : (double) row.reservationCount / totalCounted * 100;
            table[i + 1] = new String[] {
                    row.roomType.name(),
                    String.valueOf(row.reservationCount),
                    String.format("%.1f%%", percent),
                    String.valueOf(row.totalRoomsOfType)
            };
        }
        return table;
    }

    private String[] summary(LinkedListInterface<RoomTypeRow> rows, int totalCounted) {
        if (rows.isEmpty()) {
            return new String[] { Ansi.bold("Total Reservations Matched: ") + totalCounted };
        }
        RoomTypeRow top = rows.get(0);
        RoomTypeRow bottom = rows.get(rows.size() - 1);
        return new String[] {
                Ansi.bold("Total Reservations Matched: ") + totalCounted,
                Ansi.bold("Room Types Represented: ") + rows.size(),
                Ansi.bold("Most Requested: ")
                        + Ansi.color(Ansi.GREEN, top.roomType.name() + " (" + top.reservationCount + ")"),
                Ansi.bold("Least Requested: ")
                        + Ansi.color(Ansi.YELLOW, bottom.roomType.name() + " (" + bottom.reservationCount + ")")
        };
    }

    // implements Comparable only to satisfy LinkedListInterface<T extends Comparable<T>>;
    // the report's own ordering is done by the hand-written selection sort above, not this
    private static class RoomTypeRow implements Comparable<RoomTypeRow> {
        final RoomType roomType;
        int reservationCount;
        int totalRoomsOfType;

        RoomTypeRow(RoomType roomType) {
            this.roomType = roomType;
        }

        @Override
        public int compareTo(RoomTypeRow other) {
            return this.roomType.name().compareToIgnoreCase(other.roomType.name());
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

        public boolean isEmpty() {
            return table == null || table.length <= 1;
        }
    }
}
