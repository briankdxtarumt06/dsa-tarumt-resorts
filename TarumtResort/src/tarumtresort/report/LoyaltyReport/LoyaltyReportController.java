package tarumtresort.report.LoyaltyReport;

import java.time.LocalDateTime;
import java.util.Scanner;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.dao.GuestDAO;
import tarumtresort.dao.MemberDAO;
import tarumtresort.dao.RewardDAO;
import tarumtresort.entity.Guest;
import tarumtresort.entity.Member;
import tarumtresort.entity.Reward;
import tarumtresort.report.ReportResult;
import tarumtresort.report.ReportUI;

// Housekeeping-style facade for loyalty reports - date-range-only, Current header (ReportUI)
public class LoyaltyReportController {

    private final MemberDAO memberDAO = new MemberDAO();
    private final GuestDAO guestDAO = new GuestDAO();
    private final RewardDAO rewardDAO = new RewardDAO();

    private final Scanner scanner;
    private final ReportUI reportUI;

    public LoyaltyReportController(Scanner scanner) {
        this.scanner = scanner;
        this.reportUI = new ReportUI(scanner, "LOYALTY & REWARDS MODULE SUBSYSTEM");
    }

    public void generateMembershipPerformanceReport() {
        LocalDateTime[] range = reportUI.inputOptionalDateTimeRange("transaction");
        LinkedListInterface<Member> members = memberDAO.retrieveFromFile();
        LinkedListInterface<Guest> guests = new LinkedList<>();
        guestDAO.loadFromFile(guests);

        MembershipPerformanceReport.Result result = new MembershipPerformanceReport(members, guests).generate(range[0], range[1]);
        reportUI.printReport(new ReportResult(result.getTable(), result.getSummary(), result.getCharts(), result.getCallouts()), "MEMBERSHIP & TIER PERFORMANCE REPORT");
        reportUI.pressEnterToContinue();
    }

    public void generateRedemptionVoucherReport() {
        LocalDateTime[] range = reportUI.inputOptionalDateTimeRange("redemption");
        LinkedListInterface<Member> members = memberDAO.retrieveFromFile();
        LinkedListInterface<Reward> rewards = rewardDAO.retrieveFromFile();
        LinkedListInterface<Guest> guests = new LinkedList<>();
        guestDAO.loadFromFile(guests);

        RedemptionVoucherReport.Result result = new RedemptionVoucherReport(members, rewards, guests).generate(range[0], range[1]);
        reportUI.printReport(new ReportResult(result.getTable(), result.getSummary(), result.getCharts(), result.getCallouts()), "REDEMPTION & VOUCHER REPORT");
        reportUI.pressEnterToContinue();
    }

    public void generatePointExpiryReport() {
        LocalDateTime[] range = reportUI.inputOptionalDateTimeRange("transaction");
        LinkedListInterface<Member> members = memberDAO.retrieveFromFile();
        LinkedListInterface<Guest> guests = new LinkedList<>();
        guestDAO.loadFromFile(guests);

        PointExpiryReport.Result result = new PointExpiryReport(members, guests).generate(range[0], range[1]);
        reportUI.printReport(new ReportResult(result.getTable(), result.getSummary(), result.getCharts(), result.getCallouts()), "POINT EXPIRY & TIER PROGRESSION REPORT");
        reportUI.pressEnterToContinue();
    }
}
