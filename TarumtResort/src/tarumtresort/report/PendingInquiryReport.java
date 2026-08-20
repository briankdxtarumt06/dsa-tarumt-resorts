package tarumtresort.report;

import java.time.Duration;
import java.time.LocalDateTime;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Guest;
import tarumtresort.entity.Inquiry;
import tarumtresort.entity.enums.InquiryPriority;
import tarumtresort.entity.enums.InquiryStatus;
import tarumtresort.entity.enums.InquiryType;

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

    public ReportResult generate(InquiryType filterType) {

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

        String[][] table = new String[pending.size() + 1][6];
        table[0] = new String[]{"Inquiry ID", "Confirm No.", "Guest Name", "Type", "Priority", "Waiting"};

        for (int i = 0; i < pending.size(); i++) {
            Inquiry inq = pending.get(i);
            Guest guest = findGuest(inq.getGuestId());
            String guestName = guest == null ? "-" : guest.getName();

            table[i + 1] = new String[]{
                    inq.getInquiryId(),
                    inq.getConfirmationNumber(),
                    guestName,
                    inq.getInquiryType().toString(),
                    inq.getInquiryType().getPriority().toString(),
                    formatDuration(calculateWaitingTime(inq))
            };

            countPerPriority[inq.getInquiryType().getPriority().ordinal()]++;
        }

        String[] summary = {"Total pending inquiries shown: " + pending.size()};

        ReportChart chart = new ReportChart("Pending Inquiries by Priority");
        for (InquiryPriority p : InquiryPriority.values()) {
            chart.addBar(p.name(), countPerPriority[p.ordinal()], countPerPriority[p.ordinal()] + " inquiries");
        }
        LinkedListInterface<ReportChart> charts = new LinkedList<>();
        charts.addBack(chart);

        return new ReportResult(table, summary, charts, null);
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
}