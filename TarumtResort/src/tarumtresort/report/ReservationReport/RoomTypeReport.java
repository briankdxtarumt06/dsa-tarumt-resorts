package tarumtresort.report.ReservationReport;

import java.time.LocalDateTime;
import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.Room;
import tarumtresort.entity.enums.ReservationStatus;
import tarumtresort.entity.enums.RoomType;
import tarumtresort.report.ReportChart;
import tarumtresort.utility.Ansi;

// Author: Chai Chee Tong

public class RoomTypeReport {

    private final ListInterface<Room> roomList;
    private final ListInterface<Reservation> reservationList;

    public RoomTypeReport(ListInterface<Room> roomList, ListInterface<Reservation> reservationList) {
        this.roomList = roomList == null ? new DoublyLinkedList<>() : roomList;
        this.reservationList = reservationList == null ? new DoublyLinkedList<>() : reservationList;
    }

    public Result generate(LocalDateTime from, LocalDateTime to, ReservationStatus statusFilter) {
        ListInterface<RoomTypeRow> rows = new DoublyLinkedList<>();
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
            row.totalNights += r.getNumberOfNights();
            totalCounted++;
        }

        for (int i = 0; i < roomList.size(); i++) {
            Room room = roomList.get(i);
            if (room.getRoomType() == null) {
                continue;
            }
            RoomTypeRow row = findOrCreateRow(rows, room.getRoomType());
            row.totalRoomsOfType++;
            row.roomPriceSum += room.getPricePerNight();
        }

        sortByReservationCountDescending(rows);

        return new Result(toTable(rows, totalCounted), buildCharts(rows), summary(rows, totalCounted));
    }

    private RoomTypeRow findOrCreateRow(ListInterface<RoomTypeRow> rows, RoomType roomType) {
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

    private void sortByReservationCountDescending(ListInterface<RoomTypeRow> rows) {
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

    private ListInterface<ReportChart> buildCharts(ListInterface<RoomTypeRow> rows) {
        ListInterface<ReportChart> charts = new DoublyLinkedList<>();

        ReportChart reservationsChart = new ReportChart("Reservations by Room Type");
        for (int i = 0; i < rows.size(); i++) {
            RoomTypeRow row = rows.get(i);
            reservationsChart.addBar(row.roomType.name(), row.reservationCount,
                    "(" + row.reservationCount + " reservation" + (row.reservationCount == 1 ? "" : "s") + ")");
        }
        charts.addBack(reservationsChart);

        ReportChart revenueChart = new ReportChart("Estimated Revenue by Room Type (RM)");
        for (int i = 0; i < rows.size(); i++) {
            RoomTypeRow row = rows.get(i);
            revenueChart.addBar(row.roomType.name(), row.estimatedRevenue(),
                    "(" + row.totalNights + " night" + (row.totalNights == 1 ? "" : "s")
                            + " @ RM" + String.format("%.2f", row.averagePricePerNight()) + "/night)");
        }
        charts.addBack(revenueChart);

        ReportChart demandRatioChart = new ReportChart("Demand-to-Inventory Ratio by Room Type");
        for (int i = 0; i < rows.size(); i++) {
            RoomTypeRow row = rows.get(i);
            demandRatioChart.addBar(row.roomType.name(), row.demandRatio(),
                    "(" + row.reservationCount + " reservation" + (row.reservationCount == 1 ? "" : "s")
                            + " / " + row.totalRoomsOfType + " room" + (row.totalRoomsOfType == 1 ? "" : "s") + ")");
        }
        charts.addBack(demandRatioChart);

        return charts;
    }

    private String[][] toTable(ListInterface<RoomTypeRow> rows, int totalCounted) {
        String[][] table = new String[rows.size() + 1][6];
        table[0] = new String[] { "Room Type", "Total Reservations", "% of Matched Reservations",
                "Total Rooms of Type", "Est. Revenue (RM)", "Demand/Inventory Ratio" };
        for (int i = 0; i < rows.size(); i++) {
            RoomTypeRow row = rows.get(i);
            double percent = totalCounted == 0 ? 0 : (double) row.reservationCount / totalCounted * 100;
            table[i + 1] = new String[] {
                    row.roomType.name(),
                    String.valueOf(row.reservationCount),
                    String.format("%.1f%%", percent),
                    String.valueOf(row.totalRoomsOfType),
                    String.format("%.2f", row.estimatedRevenue()),
                    row.totalRoomsOfType == 0 ? "-" : String.format("%.2f", row.demandRatio())
            };
        }
        return table;
    }

    private String[] summary(ListInterface<RoomTypeRow> rows, int totalCounted) {
        if (rows.isEmpty()) {
            return new String[] { Ansi.bold("Total Reservations Matched: ") + totalCounted };
        }
        RoomTypeRow top = rows.get(0);
        RoomTypeRow bottom = rows.get(rows.size() - 1);

        RoomTypeRow highestRevenue = rows.get(0);
        RoomTypeRow highestDemandRatio = null;
        for (int i = 1; i < rows.size(); i++) {
            RoomTypeRow row = rows.get(i);
            if (row.estimatedRevenue() > highestRevenue.estimatedRevenue()) {
                highestRevenue = row;
            }
        }
        for (int i = 0; i < rows.size(); i++) {
            RoomTypeRow row = rows.get(i);
            if (row.totalRoomsOfType == 0) {
                continue;
            }
            if (highestDemandRatio == null || row.demandRatio() > highestDemandRatio.demandRatio()) {
                highestDemandRatio = row;
            }
        }

        return new String[] {
                Ansi.bold("Total Reservations Matched: ") + totalCounted,
                Ansi.bold("Room Types Represented: ") + rows.size(),
                Ansi.bold("Most Requested: ")
                        + Ansi.color(Ansi.GREEN, top.roomType.name() + " (" + top.reservationCount + ")"),
                Ansi.bold("Least Requested: ")
                        + Ansi.color(Ansi.YELLOW, bottom.roomType.name() + " (" + bottom.reservationCount + ")"),
                Ansi.bold("Highest Estimated Revenue: ")
                        + Ansi.color(Ansi.GREEN, highestRevenue.roomType.name() + " (RM "
                                + String.format("%.2f", highestRevenue.estimatedRevenue()) + ")"),
                Ansi.bold("Highest Demand-to-Inventory Ratio: ")
                        + (highestDemandRatio == null ? "-"
                                : Ansi.color(Ansi.GREEN, highestDemandRatio.roomType.name() + " ("
                                        + String.format("%.2f", highestDemandRatio.demandRatio())
                                        + " reservations per room)"))
        };
    }

    private static class RoomTypeRow implements Comparable<RoomTypeRow> {
        final RoomType roomType;
        int reservationCount;
        int totalRoomsOfType;
        int totalNights;
        double roomPriceSum;

        RoomTypeRow(RoomType roomType) {
            this.roomType = roomType;
        }

        double averagePricePerNight() {
            return totalRoomsOfType == 0 ? 0 : roomPriceSum / totalRoomsOfType;
        }

        // nights actually booked x this type's average nightly rate
        double estimatedRevenue() {
            return totalNights * averagePricePerNight();
        }

        // reservations per room of this type - how oversubscribed the type is
        double demandRatio() {
            return totalRoomsOfType == 0 ? 0 : (double) reservationCount / totalRoomsOfType;
        }

        @Override
        public int compareTo(RoomTypeRow other) {
            return this.roomType.name().compareToIgnoreCase(other.roomType.name());
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
