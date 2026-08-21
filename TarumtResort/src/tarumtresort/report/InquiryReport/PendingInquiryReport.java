package tarumtresort.report.InquiryReport;

import java.time.Duration;
import java.time.LocalDateTime;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
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

    private final LinkedListInterface<Inquiry> inquiryList;
    private final LinkedListInterface<Guest> guestList;

    public PendingInquiryReport(LinkedListInterface<Inquiry> inquiryList, LinkedListInterface<Guest> guestList) {
        this.inquiryList = inquiryList == null ? new LinkedList<>() : inquiryList;
        this.guestList = guestList == null ? new LinkedList<>() : guestList;
    }

    public Result generate(InquiryType filterType) {

        // collect + sort matching pending inquiries
        LinkedListInterface<Inquiry> pending = new LinkedList<>();
        for (int i = 0; i < inquiryList.size(); i++) {
            Inquiry inq = inquiryList.get(i);
            if (inq.getStatus() != InquiryStatus.PENDING) {
                continue;
            }
            if (filterType != null && inq.getInquiryType() != filterType) {
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
        LinkedListInterface<ReportChart> charts = new LinkedList<>();
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
    }
}