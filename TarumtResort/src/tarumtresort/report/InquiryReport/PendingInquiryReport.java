package tarumtresort.report.InquiryReport;

import java.time.Duration;
import java.time.LocalDateTime;
import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;
import tarumtresort.entity.Guest;
import tarumtresort.entity.Inquiry;
import tarumtresort.entity.enums.InquiryPriority;
import tarumtresort.entity.enums.InquiryStatus;
import tarumtresort.entity.enums.InquiryType;
import tarumtresort.report.ReportChart;
import tarumtresort.utility.Ansi;

/**
 *
 * @author Wen Ling
 *
 */
public class PendingInquiryReport {

    private final ListInterface<Inquiry> inquiryList;
    private final ListInterface<Guest> guestList;

    public PendingInquiryReport(ListInterface<Inquiry> inquiryList, ListInterface<Guest> guestList) {
        this.inquiryList = inquiryList == null ? new DoublyLinkedList<>() : inquiryList;
        this.guestList = guestList == null ? new DoublyLinkedList<>() : guestList;
    }


    public Result generate(LocalDateTime from, LocalDateTime to) {

        // collect + sort matching pending inquiries
        ListInterface<Inquiry> pending = new DoublyLinkedList<>();
        for (int i = 0; i < inquiryList.size(); i++) {
            Inquiry inq = inquiryList.get(i);
            if (inq.getStatus() != InquiryStatus.PENDING) {
                continue;
            }
            if (!inRange(inq.getCreatedTime(), from, to)) {
                continue;
            }
            pending.addSorted(inq);
        }

        int[] countPerPriority = new int[InquiryPriority.values().length];
        int[] countPerType = new int[InquiryType.values().length];

        String[][] table = new String[pending.size() + 1][6];
        table[0] = new String[]{"Inquiry ID", "Confirm No.", "Guest Name", "Type", "Priority", "Waiting"};

        Inquiry longestWaiting = null;
        Duration longestWait = null;

        for (int i = 0; i < pending.size(); i++) {
            Inquiry inq = pending.get(i);
            Guest guest = findGuest(inq.getGuestId());
            String guestName = guest == null ? "-" : guest.getName();
            Duration waited = calculateWaitingTime(inq);

            table[i + 1] = new String[]{
                    inq.getInquiryId(),
                    inq.getConfirmationNumber(),
                    guestName,
                    inq.getInquiryType().toString(),
                    inq.getInquiryType().getPriority().toString(),
                    formatDuration(waited)
            };

            countPerPriority[inq.getInquiryType().getPriority().ordinal()]++;
            countPerType[inq.getInquiryType().ordinal()]++;

            if (longestWait == null || waited.compareTo(longestWait) > 0) {
                longestWait = waited;
                longestWaiting = inq;
            }
        }

        ReportChart chart = new ReportChart("Pending Inquiries by Priority");
        for (InquiryPriority p : InquiryPriority.values()) {
            chart.addBar(p.name(), countPerPriority[p.ordinal()], countPerPriority[p.ordinal()] + " inquiries");
        }
        ListInterface<ReportChart> charts = new DoublyLinkedList<>();
        charts.addBack(chart);

        String[] summary = buildSummary(pending.size(), countPerPriority, countPerType, longestWaiting, longestWait);

        return new Result(table, charts, summary);
    }

    private String[] buildSummary(int totalPending, int[] countPerPriority, int[] countPerType,
            Inquiry longestWaiting, Duration longestWait) {

        InquiryType mostCommonType = null;
        int mostCommonCount = 0;
        for (InquiryType type : InquiryType.values()) {
            if (countPerType[type.ordinal()] > mostCommonCount) {
                mostCommonCount = countPerType[type.ordinal()];
                mostCommonType = type;
            }
        }

        int urgentCount = countPerPriority[InquiryPriority.URGENT.ordinal()];

        return new String[]{
                Ansi.bold("Total Pending Inquiries: ") + totalPending,
                Ansi.bold("Urgent Inquiries Waiting: ") + urgentCount,
                Ansi.bold("Longest Waiting Inquiry: ")
                        + (longestWaiting == null ? "-"
                                : longestWaiting.getInquiryId() + " (" + formatDuration(longestWait) + ")"),
                Ansi.bold("Most Common Query Type: ")
                        + (mostCommonType == null ? "-" : mostCommonType + " (" + mostCommonCount
                                + " inquir" + (mostCommonCount == 1 ? "y" : "ies") + ")")
        };
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

    private Guest findGuest(String guestId) {
        if (guestId == null) {
            return null;
        }
        for (int i = 0; i < guestList.size(); i++) {
            Guest g = guestList.get(i);
            if (g.getGuestId().equals(guestId)) {
                return g;
            }
        }
        return null;
    }

    private Duration calculateWaitingTime(Inquiry inquiry) {
        return Duration.between(inquiry.getCreatedTime(), LocalDateTime.now());
    }

    private String formatDuration(Duration d) {
        long minutes = d.toMinutes();
        long seconds = d.minusMinutes(minutes).getSeconds();
        return minutes + "m " + seconds + "s";
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