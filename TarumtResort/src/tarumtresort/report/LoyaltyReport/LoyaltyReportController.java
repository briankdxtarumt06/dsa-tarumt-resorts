package tarumtresort.report.LoyaltyReport;

import java.time.LocalDateTime;
import java.util.Scanner;
import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;
import tarumtresort.dao.GuestDAO;
import tarumtresort.dao.MemberDAO;
import tarumtresort.dao.RewardDAO;
import tarumtresort.entity.Guest;
import tarumtresort.entity.Member;
import tarumtresort.entity.Reward;
import tarumtresort.entity.enums.Tier;

// Loyalty report facade - mirrors HousekeepingReport/HousekeepingReportController:
// per-report flow = date range (0 = Back aborts) -> secondary filters -> generate
// -> dedicated per-report UI render -> press Enter.
public class LoyaltyReportController {

    private final MemberDAO memberDAO = new MemberDAO();
    private final GuestDAO guestDAO = new GuestDAO();
    private final RewardDAO rewardDAO = new RewardDAO();

    private final Scanner scanner;

    public LoyaltyReportController(Scanner scanner) {
        this.scanner = scanner;
    }

    public void generateMembershipPerformanceReport() {
        LocalDateTime[] range = ui().inputOptionalDateTimeRange("transaction");
        if (range == null) {
            return;
        }
        Tier tierFilter = ui().inputTierFilter();

        ListInterface<Member> members = memberDAO.retrieveFromFile();
        ListInterface<Guest> guests = new DoublyLinkedList<>();
        guestDAO.loadFromFile(guests);

        MembershipPerformanceReport.Result result = new MembershipPerformanceReport(members, guests)
                .generate(range[0], range[1], tierFilter);
        new MembershipPerformanceUI(ui()).render(result);
        ui().pressEnterToContinue();
    }

    public void generateRedemptionVoucherReport() {
        LocalDateTime[] range = ui().inputOptionalDateTimeRange("redemption");
        if (range == null) {
            return;
        }
        String statusFilter = ui().inputStatusFilter();

        ListInterface<Member> members = memberDAO.retrieveFromFile();
        ListInterface<Reward> rewards = rewardDAO.retrieveFromFile();
        ListInterface<Guest> guests = new DoublyLinkedList<>();
        guestDAO.loadFromFile(guests);

        RedemptionVoucherReport.Result result = new RedemptionVoucherReport(members, rewards, guests)
                .generate(range[0], range[1], statusFilter);
        new RedemptionVoucherUI(ui()).render(result);
        ui().pressEnterToContinue();
    }

    private ReportUI ui() {
        return new ReportUI(scanner);
    }
}
