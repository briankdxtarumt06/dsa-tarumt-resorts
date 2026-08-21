package tarumtresort.report;

import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;
import tarumtresort.entity.Inquiry;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.enums.InquiryStatus;
import tarumtresort.entity.enums.RoomType;

/**
 *
 * @author Wen Ling
 *
 */
public class RoomTypeInquiryDistributionReport {

    private final ListInterface<Inquiry> inquiryList;
    private final ListInterface<Reservation> reservationList;

    public RoomTypeInquiryDistributionReport(ListInterface<Inquiry> inquiryList,
            ListInterface<Reservation> reservationList) {
        this.inquiryList = inquiryList == null ? new DoublyLinkedList<>() : inquiryList;
        this.reservationList = reservationList == null ? new DoublyLinkedList<>() : reservationList;
    }

    public ReportResult generate(RoomType filterType) {

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
            Reservation reservation = findReservation(inq.getConfirmationNumber());
            if (reservation == null) {
                continue;
            }
            RoomType type = reservation.getRoomTypeRequested();
            if (filterType != null && type != filterType) {
                continue;
            }
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

        Integer[] order = new Integer[types.length];
        for (int i = 0; i < types.length; i++) {
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
        }

        String[] summary = {"Total resolved inquiries counted: " + grandTotal};

        ListInterface<ReportChart> charts = new DoublyLinkedList<>();
        charts.addBack(chart);

        return new ReportResult(table, summary, charts, null);
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
}