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
import tarumtresort.entity.enums.Tier;
import tarumtresort.report.ReportResult;

// Author: Imam Mahdi Ali Ang Attuko
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
        Tier tierFilter = reportUI.inputTierFilter();
        LinkedListInterface<Member> members = memberDAO.retrieveFromFile();
        LinkedListInterface<Guest> guests = new LinkedList<>();
        guestDAO.loadFromFile(guests);

        MembershipPerformanceReport.Result result = new MembershipPerformanceReport(members, guests).generate(range[0], range[1], tierFilter);
        reportUI.printReport(new ReportResult(result.getTable(), result.getSummary(), result.getCharts(), result.getCallouts(), result.getCriteria()), "MEMBERSHIP & TIER PERFORMANCE REPORT");
        reportUI.pressEnterToContinue();
    }

    public void generateRedemptionVoucherReport() {
        LocalDateTime[] range = reportUI.inputOptionalDateTimeRange("redemption");
        String statusFilter = reportUI.inputStatusFilter();
        LinkedListInterface<Member> members = memberDAO.retrieveFromFile();
        LinkedListInterface<Reward> rewards = rewardDAO.retrieveFromFile();
        LinkedListInterface<Guest> guests = new LinkedList<>();
        guestDAO.loadFromFile(guests);

        RedemptionVoucherReport.Result result = new RedemptionVoucherReport(members, rewards, guests).generate(range[0], range[1], statusFilter);
        reportUI.printReport(new ReportResult(result.getTable(), result.getSummary(), result.getCharts(), result.getCallouts(), result.getCriteria()), "REDEMPTION & VOUCHER REPORT");
        reportUI.pressEnterToContinue();
    }

    public void generatePointExpiryReport() {
        LocalDateTime[] range = reportUI.inputOptionalDateTimeRange("transaction");
        Tier tierFilter = reportUI.inputTierFilter();
        boolean expiringOnly = reportUI.inputYesNo("Show only members with points expiring within the window?");
        LinkedListInterface<Member> members = memberDAO.retrieveFromFile();
        LinkedListInterface<Guest> guests = new LinkedList<>();
        guestDAO.loadFromFile(guests);

        PointExpiryReport.Result result = new PointExpiryReport(members, guests).generate(range[0], range[1], tierFilter, expiringOnly);
        reportUI.printReport(new ReportResult(result.getTable(), result.getSummary(), result.getCharts(), result.getCallouts(), result.getCriteria()), "POINT EXPIRY & TIER PROGRESSION REPORT");
        reportUI.pressEnterToContinue();
    }
}
