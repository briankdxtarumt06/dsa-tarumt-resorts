package tarumtresort.report.InquiryReport;

import java.time.LocalDateTime;
import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;
import tarumtresort.entity.Inquiry;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.enums.InquiryStatus;
import tarumtresort.entity.enums.InquiryType;
import tarumtresort.entity.enums.RoomType;
import tarumtresort.report.ReportChart;
import tarumtresort.utility.Ansi;

// Author: Fong Wen Ling
public class RoomTypeInquiryDistributionReport {

    private final ListInterface<Inquiry> inquiryList;
    private final ListInterface<Reservation> reservationList;

    public RoomTypeInquiryDistributionReport(ListInterface<Inquiry> inquiryList,
            ListInterface<Reservation> reservationList) {
        this.inquiryList = inquiryList == null ? new DoublyLinkedList<>() : inquiryList;
        this.reservationList = reservationList == null ? new DoublyLinkedList<>() : reservationList;
    }

    public Result generate(LocalDateTime from, LocalDateTime to) {

        RoomType[] types = RoomType.values();
        int[] totalCount = new int[types.length];
        int[] guestIdCount = new int[types.length];
        int[] roomAvailCount = new int[types.length];
        int[] billingCount = new int[types.length];
        int[] roomServiceCount = new int[types.length];

        for (int i = 0; i < inquiryList.size(); i++) {
            Inquiry inq = inquiryList.get(i);
            if (inq.getStatus() != InquiryStatus.RESOLVED) {
                continue; // exclude PENDING / IN_PROGRESS / CANCELLED
            }
            if (!inRange(inq.getCreatedTime(), from, to)) {
                continue;
            }
            Reservation reservation = findReservation(inq.getConfirmationNumber());
            if (reservation == null) {
                continue;
            }
            RoomType type = reservation.getRoomTypeRequested();
            int idx = indexOfRoomType(types, type);
            if (idx < 0) {
                continue;
            }
            totalCount[idx]++;
            switch (inq.getInquiryType()) {
                case GUESTIDENTIFICATION: guestIdCount[idx]++; break;
                case ROOMAVAILABILITY: roomAvailCount[idx]++; break;
                case BILLINGDETAILS: billingCount[idx]++; break;
                case ROOMSERVICE: roomServiceCount[idx]++; break;
            }
        }

        // sort room types by total inquiries descending
        Integer[] order = sortDescending(totalCount, types.length);

        // count non-zero rows first so the table array can be sized exactly
        int rowCount = 0;
        for (int idx : order) {
            if (totalCount[idx] > 0) {
                rowCount++;
            }
        }

        String[][] table = new String[rowCount + 1][6];
        table[0] = new String[]{"Room Type", "Total", "GuestID", "RoomAvail", "Billing", "RoomServ"};

        ReportChart chart = new ReportChart("Total Inquiries by Room Type");
        int grandTotal = 0;
        int row = 1;
        RoomType busiestType = null;
        int busiestCount = 0;

        for (int idx : order) {
            if (totalCount[idx] == 0) {
                continue;
            }
            table[row++] = new String[]{
                    types[idx].toString(),
                    String.valueOf(totalCount[idx]),
                    String.valueOf(guestIdCount[idx]),
                    String.valueOf(roomAvailCount[idx]),
                    String.valueOf(billingCount[idx]),
                    String.valueOf(roomServiceCount[idx])
            };
            chart.addBar(types[idx].toString(), totalCount[idx], totalCount[idx] + " inquiries");
            grandTotal += totalCount[idx];
            if (totalCount[idx] > busiestCount) {
                busiestCount = totalCount[idx];
                busiestType = types[idx];
            }
        }

        ListInterface<ReportChart> charts = new DoublyLinkedList<>();
        charts.addBack(chart);

        String[] summary = buildSummary(grandTotal, busiestType, busiestCount,
                sum(guestIdCount), sum(roomAvailCount), sum(billingCount), sum(roomServiceCount));

        return new Result(table, charts, summary);
    }

    private String[] buildSummary(int grandTotal, RoomType busiestType, int busiestCount,
            int guestIdTotal, int roomAvailTotal, int billingTotal, int roomServiceTotal) {

        String[] typeNames = {
            InquiryType.GUESTIDENTIFICATION.toString(),
            InquiryType.ROOMAVAILABILITY.toString(),
            InquiryType.BILLINGDETAILS.toString(),
            InquiryType.ROOMSERVICE.toString()
        };
        int[] typeCounts = { guestIdTotal, roomAvailTotal, billingTotal, roomServiceTotal };

        String mostCommonType = "-";
        int mostCommonCount = 0;
        for (int i = 0; i < typeNames.length; i++) {
            if (typeCounts[i] > mostCommonCount) {
                mostCommonCount = typeCounts[i];
                mostCommonType = typeNames[i];
            }
        }

        return new String[]{
                Ansi.bold("Total Resolved Inquiries: ") + grandTotal,
                Ansi.bold("Busiest Room Type: ")
                        + (busiestType == null ? "-" : busiestType + " (" + busiestCount + " inquiries)"),
                Ansi.bold("Most Common Query Type: ")
                        + (mostCommonCount == 0 ? "-" : mostCommonType + " (" + mostCommonCount + ")")
        };
    }

    private int sum(int[] counts) {
        int total = 0;
        for (int c : counts) {
            total += c;
        }
        return total;
    }

    private Integer[] sortDescending(int[] totalCount, int length) {
        Integer[] order = new Integer[length];
        for (int i = 0; i < length; i++) {
            order[i] = i;
        }
        for (int i = 0; i < order.length - 1; i++) {
            int maxIdx = i;
            for (int j = i + 1; j < order.length; j++) {
                if (totalCount[order[j]] > totalCount[order[maxIdx]]) {
                    maxIdx = j;
                }
            }
            int temp = order[i];
            order[i] = order[maxIdx];
            order[maxIdx] = temp;
        }
        return order;
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

    private Reservation findReservation(String confirmationNumber) {
        if (confirmationNumber == null) {
            return null;
        }
        for (int i = 0; i < reservationList.size(); i++) {
            Reservation r = reservationList.get(i);
            if (r.getConfirmationNumber().equals(confirmationNumber)) {
                return r;
            }
        }
        return null;
    }

    private int indexOfRoomType(RoomType[] types, RoomType target) {
        for (int i = 0; i < types.length; i++) {
            if (types[i] == target) {
                return i;
            }
        }
        return -1;
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
    }
}