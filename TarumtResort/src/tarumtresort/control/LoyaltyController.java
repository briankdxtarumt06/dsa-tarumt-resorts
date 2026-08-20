package tarumtresort.control;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.Scanner;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.boundary.LoyaltyRewardsUI;
import tarumtresort.dao.GuestDAO;
import tarumtresort.dao.MemberDAO;
import tarumtresort.dao.RewardDAO;
import tarumtresort.entity.Guest;
import tarumtresort.entity.Member;
import tarumtresort.entity.Notification;
import tarumtresort.entity.PointTransaction;
import tarumtresort.entity.RedemptionRecord;
import tarumtresort.entity.Reward;
import tarumtresort.entity.enums.RoomType;
import tarumtresort.entity.enums.Tier;
import tarumtresort.report.ReportChart;
import tarumtresort.report.ReportResult;
import tarumtresort.report.ReportUI;
import tarumtresort.utility.ConsoleUtil;

/**
 * Combined controller for the loyalty module: member management, reward
 * catalogue management, points earning/expiry, and redemption requests.
 * Previously split across PointsController, MemberController and
 * RewardController; merged so all loyalty data is loaded once and shared.
 */
public class LoyaltyController {

    private LinkedListInterface<Member> memberList = new LinkedList<>();
    private LinkedListInterface<Reward> rewardList = new LinkedList<>();
    private LinkedListInterface<Guest> guestList = new LinkedList<>();

    private MemberDAO memberDAO = new MemberDAO();
    private RewardDAO rewardDAO = new RewardDAO();
    private GuestDAO guestDAO = new GuestDAO();

    private ReportUI reportUI;
    private LoyaltyRewardsUI moduleUI;

    private static final int PAGE_SIZE = 20;

    private static final DateTimeFormatter NOTIF_DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public LoyaltyController() {
        this(new Scanner(System.in));
    }

    public LoyaltyController(Scanner scanner) {
        memberList = memberDAO.retrieveFromFile();
        rewardList = rewardDAO.retrieveFromFile();
        guestDAO.loadFromFile(guestList);
        reportUI = new ReportUI(scanner, "LOYALTY & REWARDS MODULE SUBSYSTEM");
        moduleUI = new LoyaltyRewardsUI(scanner);
        reconcileTiersOnLoad();
    }

    public LoyaltyController(LoyaltyRewardsUI ui) {
        this(ui.getScanner());
    }

    public void runLoyaltyRewards() {
        run();
    }

    // entry point for the loyalty module (mirrors HousekeepingController.runHousekeeping)
    public void run() {
        try {
            ConsoleUtil.clearScreen();
            int choice;

            do {
                choice = moduleUI.getMenuChoice(getUnreadNotificationCount());

                switch (choice) {
                    case 1:
                        runMemberMenu();
                        break;
                    case 2:
                        runRewardMenu();
                        break;
                    case 3:
                        runPointsMenu();
                        break;
                    case 4:
                        runNotificationCentre();
                        break;
                    case 5:
                        runReports();
                        break;
                    case 0:
                        System.out.println("\n  Returning to main menu...");
                        break;
                    default:
                        System.out.println("\n  ✗ Invalid choice! Please try again.");
                }
            } while (choice != 0);
        } catch (Exception e) {
            ConsoleUtil.printError("An unexpected error occurred in Loyalty & Rewards module: " + e.getMessage());
        }
    }

    // ======================= MENU ENTRY POINTS =======================

    /** Member management list page (mirrors HousekeepingController.runStaffManagement). */
    public void runMemberMenu() {
        String tierFilter = null;
        int page = 0;

        while (true) {
            LinkedListInterface<Member> display;
            if (tierFilter != null) {
                display = getMembersByTier(tierFilter);
            } else {
                display = getActiveMembers();
            }

            boolean hasFilter = tierFilter != null;
            boolean hasDeleted = getDeletedMembers().size() > 0;
            int pageCount = Math.max(1, (display.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            if (page >= pageCount) {
                page = pageCount - 1; // clamp after the list shrank
            }

            LinkedListInterface<Member> pageList = pageOf(display, page);
            int choice = moduleUI.printMemberListMenu(pageList, page, pageCount, hasFilter,
                    hasDeleted, this::findGuest);

            if (choice == 0) {
                break;
            }

            int action = 1;
            if (choice == action++) { // 1. View Details
                viewMember(pageList);
            } else if (choice == action++) { // 2. Filter by Tier
                String tier = moduleUI.inputTierFilter();
                if (tier != null) {
                    tierFilter = tier;
                    page = 0;
                }
            } else if (hasDeleted && choice == action++) { // 3. Restore Deleted Member
                restoreDeletedMemberFlow();
            } else {
                boolean matched = false;
                if (page < pageCount - 1) { // Next Page
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page++;
                    }
                }
                if (!matched && page > 0) { // Previous Page
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page--;
                    }
                }
                if (!matched && hasFilter) { // Clear Filter
                    matched = choice == action;
                    action++;
                    if (matched) {
                        tierFilter = null;
                        page = 0;
                    }
                }
            }
        }
    }

    /** Deleted-members list: pick one, confirm, and restore it (undo soft delete). */
    private void restoreDeletedMemberFlow() {
        int page = 0;
        while (true) {
            LinkedListInterface<Member> deleted = getDeletedMembers();
            int pageCount = Math.max(1, (deleted.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            if (page >= pageCount) {
                page = pageCount - 1;
            }
            LinkedListInterface<Member> pageList = pageOf(deleted, page);
            int choice = moduleUI.printDeletedMembersMenu(pageList, page, pageCount, this::findGuest);
            if (choice == 0) {
                return;
            }
            int action = 1;
            if (choice == action++) { // 1. Restore Selected Member
                int index = moduleUI.inputListIndex("member", pageList.size());
                if (index == 0) {
                    continue;
                }
                Member member = pageList.get(index - 1);
                if (member != null && moduleUI.confirm("Restore member " + member.getMemberId() + "?")) {
                    moduleUI.showMessage(restoreMember(member.getMemberId()));
                }
                return;
            } else {
                boolean matched = false;
                if (page < pageCount - 1) { // Next Page
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page++;
                    }
                }
                if (!matched && page > 0) { // Previous Page
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page--;
                    }
                }
            }
        }
    }

    // view flow: pick a member from the current page, then run its action menu
    private void viewMember(LinkedListInterface<Member> pageList) {
        if (pageList.isEmpty()) {
            moduleUI.showMessage("(No member records)");
            return;
        }
        int num = moduleUI.inputListIndex("member", pageList.size());
        if (num == 0) {
            return;
        }
        Member member = pageList.get(num - 1);
        if (member != null) {
            handleMemberActions(member);
        }
    }

    // select-entity action loop for one member: details -> action -> details
    private void handleMemberActions(Member member) {
        while (true) {
            moduleUI.displayProfile(member, findGuest(member.getGuestId()));

            int action = moduleUI.getMemberActionChoice();
            if (action == 0) {
                return;
            }

            switch (action) {
                case 1: // Update Member Tier
                    moduleUI.show("Current tier: " + member.getTier());
                    Tier tier = moduleUI.selectTier();
                    if (tier != null) {
                        moduleUI.showMessage(updateMember(member.getMemberId(), tier));
                    }
                    break;
                case 2: // Remove Member
                    if (moduleUI.confirm("Remove member " + member.getMemberId()
                            + "? (soft delete - can be restored later)")) {
                        moduleUI.showMessage(removeMember(member.getMemberId()));
                    }
                    return; // member is gone; back to the list
                case 3: // Set / Edit Promotion
                    setPromotionFlow(member);
                    break;
                case 4: // Clear Promotion
                    if (member.getPromotionName() != null) {
                        if (moduleUI.confirm("Clear promotion for " + member.getMemberId() + "?")) {
                            moduleUI.showMessage(clearPromotion(member.getMemberId()));
                        }
                    } else {
                        moduleUI.showMessage("No promotion set for " + member.getMemberId() + ".");
                    }
                    break;
                default:
                    break;
            }

            member = findMember(member.getMemberId()); // re-read so details stay fresh
            if (member == null) {
                return;
            }
        }
    }

    /** Prompts for and applies a personalized promotion to a member. */
    private void setPromotionFlow(Member member) {
        String name = moduleUI.inputPromotionName();
        if (name == null) {
            return; // cancelled
        }
        int percent = moduleUI.inputPromotionPercent();
        if (percent == 0) {
            return; // cancelled
        }
        String expiryInput = moduleUI.inputPromotionExpiryString();
        if (expiryInput == null) {
            return; // cancelled
        }
        LocalDateTime expiry = expiryInput.isBlank()
                ? null
                : java.time.LocalDate.parse(expiryInput).atStartOfDay();
        moduleUI.showMessage(setPromotion(member.getMemberId(), name, percent, expiry, LocalDateTime.now()));
    }

    /** Assigns a personalized promotion (percent off stays) to a member and notifies them. */
    public String setPromotion(String memberId, String name, int percent, LocalDateTime expiry, LocalDateTime now) {
        Member member = findMember(memberId);
        if (member == null) {
            return "Member not found: " + memberId;
        }
        if (name == null || name.isBlank() || percent <= 0 || percent > 100) {
            return "Invalid promotion: a name and a discount percent between 1 and 100 are required.";
        }
        if (expiry != null && !expiry.isAfter(now)) {
            return "Promotion expiry must be in the future.";
        }
        member.setPromotionName(name.trim());
        member.setPromotionDiscountPercent(percent);
        member.setPromotionExpiry(expiry);
        persistMembers();
        notifyMember(member, "PROMOTION_ASSIGNED",
                "You have a special offer: " + name.trim() + " (" + percent + "% off stays)"
                        + (expiry == null ? "." : " - expires " + expiry.toLocalDate() + "."), now);
        return "Promotion set for " + memberId + ": " + name.trim() + " (" + percent + "% off stays)"
                + (expiry == null ? "" : ", expires " + expiry.toLocalDate()) + ".";
    }

    /** Removes a member's personalized promotion. */
    public String clearPromotion(String memberId) {
        Member member = findMember(memberId);
        if (member == null) {
            return "Member not found: " + memberId;
        }
        member.setPromotionName(null);
        member.setPromotionDiscountPercent(0);
        member.setPromotionExpiry(null);
        persistMembers();
        return "Promotion cleared for " + memberId + ".";
    }

    public LinkedListInterface<Member> getMembersByTier(String tier) {
        LinkedListInterface<Member> filteredList = new LinkedList<>();
        for (int i = 0; i < memberList.size(); i++) {
            Member member = memberList.get(i);
            if (!member.isDeleted() && member.getTier() != null && member.getTier().name().equalsIgnoreCase(tier)) {
                filteredList.addBack(member);
            }
        }
        return filteredList;
    }

    // the rows of one page (PAGE_SIZE at most), starting at page * PAGE_SIZE
    private <T extends Comparable<T>> LinkedList<T> pageOf(LinkedListInterface<T> list, int page) {
        LinkedList<T> result = new LinkedList<>();
        int start = page * PAGE_SIZE;
        int end = Math.min(list.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            result.addBack(list.get(i));
        }
        return result;
    }

    /** Reward catalogue list page (mirrors HousekeepingController.runTaskManagement). */
    public void runRewardMenu() {
        Tier tierFilter = null;
        int sortMode = 0; // 0 = default, 1 = points asc, 2 = points desc
        int page = 0;

        while (true) {
            LinkedListInterface<Reward> display;
            if (tierFilter != null) {
                display = getRewardsByMinTier(tierFilter);
            } else {
                display = getActiveRewards();
            }
            display = sortedRewardView(display, sortMode);

            boolean hasFilter = tierFilter != null;
            boolean hasDeleted = getDeletedRewards().size() > 0;
            int pageCount = Math.max(1, (display.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            if (page >= pageCount) {
                page = pageCount - 1; // clamp after the list shrank
            }

            LinkedListInterface<Reward> pageList = pageOf(display, page);
            int choice = moduleUI.printRewardListMenu(pageList, page, pageCount, hasFilter,
                    hasDeleted, sortLabelOf(sortMode));

            if (choice == 0) {
                break;
            }

            int action = 1;
            if (choice == action++) { // 1. View Details
                viewReward(pageList);
            } else if (choice == action++) { // 2. Add New Reward
                addRewardFlow();
            } else if (choice == action++) { // 3. Filter by Min Tier
                Tier tier = moduleUI.inputMinTierFilter();
                if (tier != null) {
                    tierFilter = tier;
                    page = 0;
                }
            } else if (choice == action++) { // 4. Sort by Points
                sortMode = (sortMode + 1) % 3;
                page = 0;
            } else if (hasDeleted && choice == action++) { // 5. Restore Deleted Reward
                restoreDeletedRewardFlow();
            } else {
                boolean matched = false;
                if (page < pageCount - 1) { // Next Page
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page++;
                    }
                }
                if (!matched && page > 0) { // Previous Page
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page--;
                    }
                }
                if (!matched && hasFilter) { // Clear Filter
                    matched = choice == action;
                    action++;
                    if (matched) {
                        tierFilter = null;
                        page = 0;
                    }
                }
            }
        }
    }

    /** Deleted-rewards list: pick one, confirm, and restore it (undo soft delete). */
    private void restoreDeletedRewardFlow() {
        int page = 0;
        while (true) {
            LinkedListInterface<Reward> deleted = getDeletedRewards();
            int pageCount = Math.max(1, (deleted.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            if (page >= pageCount) {
                page = pageCount - 1;
            }
            LinkedListInterface<Reward> pageList = pageOf(deleted, page);
            int choice = moduleUI.printDeletedRewardsMenu(pageList, page, pageCount);
            if (choice == 0) {
                return;
            }
            int action = 1;
            if (choice == action++) { // 1. Restore Selected Reward
                int index = moduleUI.inputListIndex("reward", pageList.size());
                if (index == 0) {
                    continue;
                }
                Reward reward = pageList.get(index - 1);
                if (reward != null && moduleUI.confirm("Restore reward " + reward.getRewardId()
                        + " (" + reward.getName() + ")?")) {
                    moduleUI.showMessage(restoreReward(reward.getRewardId()));
                }
                return;
            } else {
                boolean matched = false;
                if (page < pageCount - 1) { // Next Page
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page++;
                    }
                }
                if (!matched && page > 0) { // Previous Page
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page--;
                    }
                }
            }
        }
    }

    /** Rewards in the requested point-cost order. Mode 0 keeps the catalogue
     *  order (already points-ascending), mode 1 = low→high, mode 2 = high→low. */
    private LinkedListInterface<Reward> sortedRewardView(LinkedListInterface<Reward> source, int sortMode) {
        if (sortMode == 2) {
            LinkedListInterface<Reward> result = new LinkedList<>();
            for (int i = source.size() - 1; i >= 0; i--) {
                result.addBack(source.get(i));
            }
            return result;
        }
        return source;
    }

    private String sortLabelOf(int sortMode) {
        switch (sortMode) {
            case 1:
                return "Points (Low -> High)";
            case 2:
                return "Points (High -> Low)";
            default:
                return "";
        }
    }

    // view flow: pick a reward from the current page, then run its action menu
    private void viewReward(LinkedListInterface<Reward> pageList) {
        if (pageList.isEmpty()) {
            moduleUI.showMessage("(No rewards in the catalogue)");
            return;
        }
        int num = moduleUI.inputListIndex("reward", pageList.size());
        if (num == 0) {
            return;
        }
        Reward reward = pageList.get(num - 1);
        if (reward != null) {
            handleRewardActions(reward);
        }
    }

    // select-entity action loop for one reward: details -> action -> details
    private void handleRewardActions(Reward reward) {
        while (true) {
            moduleUI.displayRewardDetails(reward);

            int action = moduleUI.getRewardActionChoice();
            if (action == 0) {
                return;
            }

            switch (action) {
                case 1: // Update Reward
                    updateRewardPrompt(reward);
                    break;
                case 2: // Remove Reward
                    if (moduleUI.confirm("Remove reward " + reward.getRewardId()
                            + " (" + reward.getName() + ")? (soft delete - can be restored later)")) {
                        moduleUI.showMessage(removeReward(reward.getRewardId()));
                    }
                    return; // reward is gone; back to the list
                default:
                    break;
            }

            reward = findReward(reward.getRewardId()); // re-read so details stay fresh
            if (reward == null) {
                return;
            }
        }
    }

    private void updateRewardPrompt(Reward reward) {
        String name = moduleUI.promptWithDefault("New name", reward.getName());
        if (name == null) {
            moduleUI.showMessage("Operation cancelled.");
            return;
        }
        String description = moduleUI.promptWithDefault("New description", reward.getDescription());
        if (description == null) {
            moduleUI.showMessage("Operation cancelled.");
            return;
        }
        Integer cost = moduleUI.promptIntWithDefault("New point cost", reward.getPointCost());
        if (cost == null) {
            moduleUI.showMessage("Operation cancelled.");
            return;
        }
        Tier minTier = moduleUI.promptTierWithDefault("New min tier",
                reward.getMinTier() == null ? Tier.SILVER : reward.getMinTier());
        if (minTier == null) {
            moduleUI.showMessage("Operation cancelled.");
            return;
        }
        RoomType roomType = moduleUI.promptRoomTypeWithDefault("New room type", reward.getRoomType());
        Integer voucherType = moduleUI.promptVoucherTypeWithDefault(reward);
        if (voucherType == null) {
            moduleUI.showMessage("Operation cancelled.");
            return;
        }
        Double voucherValue = reward.getVoucherValue();
        Integer discountPercent = reward.getDiscountPercent();
        if (voucherType == 1) {
            voucherValue = moduleUI.promptDoubleWithDefault("New voucher value (RM)", reward.getVoucherValue());
            if (voucherValue == null && reward.getVoucherValue() != null) {
                moduleUI.showMessage("Operation cancelled.");
                return;
            }
            discountPercent = null;
        } else if (voucherType == 2) {
            discountPercent = moduleUI.promptPercentWithDefault("New discount percent (%)", reward.getDiscountPercent());
            if (discountPercent == null && reward.getDiscountPercent() != null) {
                moduleUI.showMessage("Operation cancelled.");
                return;
            }
            voucherValue = null;
        }
        moduleUI.showMessage(updateReward(reward.getRewardId(), name, description, cost,
                minTier, roomType, voucherValue, discountPercent));
    }

    public LinkedListInterface<Reward> getRewardsByName(String keyword) {
        LinkedListInterface<Reward> filteredList = new LinkedList<>();
        for (int i = 0; i < rewardList.size(); i++) {
            Reward reward = rewardList.get(i);
            if (!reward.isDeleted() && reward.getName() != null
                    && reward.getName().toLowerCase().contains(keyword.toLowerCase())) {
                filteredList.addBack(reward);
            }
        }
        return filteredList;
    }

    /** Rewards whose minimum redeemable tier is exactly the given tier. */
    public LinkedListInterface<Reward> getRewardsByMinTier(Tier tier) {
        LinkedListInterface<Reward> filteredList = new LinkedList<>();
        for (int i = 0; i < rewardList.size(); i++) {
            Reward reward = rewardList.get(i);
            if (reward.isDeleted()) {
                continue;
            }
            Tier minTier = reward.getMinTier() == null ? Tier.SILVER : reward.getMinTier();
            if (minTier == tier) {
                filteredList.addBack(reward);
            }
        }
        return filteredList;
    }

    /** Rewards the given tier (and above) is allowed to redeem. */
    public LinkedListInterface<Reward> getRewardsEligibleFor(Tier tier) {
        LinkedListInterface<Reward> eligible = new LinkedList<>();
        for (int i = 0; i < rewardList.size(); i++) {
            Reward reward = rewardList.get(i);
            if (reward.isDeleted()) {
                continue;
            }
            Tier minTier = reward.getMinTier() == null ? Tier.SILVER : reward.getMinTier();
            if (tier != null && tier.ordinal() >= minTier.ordinal()) {
                eligible.addBack(reward);
            }
        }
        return eligible;
    }

    /** Points, redemption and notification list page (mirrors HousekeepingController.runAssignmentManagement). */
    public void runPointsMenu() {
        String alert = generateExpiryAlerts(LocalDateTime.now());
        if (!alert.startsWith("No new")) {
            moduleUI.show(alert);
        }
        String tierFilter = null;
        int page = 0;

        while (true) {
            LinkedListInterface<Member> display;
            if (tierFilter != null) {
                display = getMembersByTier(tierFilter);
            } else {
                display = getActiveMembers();
            }

            boolean hasFilter = tierFilter != null;
            int pageCount = Math.max(1, (display.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            if (page >= pageCount) {
                page = pageCount - 1; // clamp after the list shrank
            }

            LinkedListInterface<Member> pageList = pageOf(display, page);
            int choice = moduleUI.printPointsListMenu(pageList, page, pageCount, hasFilter,
                    this::findGuest, m -> getAvailableBalance(m.getMemberId(), LocalDateTime.now()));

            if (choice == 0) {
                break;
            }

            int action = 1;
            if (choice == action++) { // 1. View Details
                viewMemberPoints(pageList);
            } else if (choice == action++) { // 2. Earn Points
                earnPointsFlow(pageList);
            } else if (choice == action++) { // 3. Request Redemption
                requestRedemptionFlow(pageList);
            } else if (choice == action++) { // 4. Process Redemption Requests
                processRedemptionRequestsFlow();
            } else if (choice == action++) { // 5. Generate Expiry Alerts
                moduleUI.showMessage(generateExpiryAlerts(LocalDateTime.now()));
            } else if (choice == action++) { // 6. Filter by Tier
                String tier = moduleUI.inputTierFilter();
                if (tier != null) {
                    tierFilter = tier;
                    page = 0;
                }
            } else {
                boolean matched = false;
                if (page < pageCount - 1) { // Next Page
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page++;
                    }
                }
                if (!matched && page > 0) { // Previous Page
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page--;
                    }
                }
                if (!matched && hasFilter) { // Clear Filter
                    matched = choice == action;
                    action++;
                    if (matched) {
                        tierFilter = null;
                        page = 0;
                    }
                }
            }
        }
    }

    // view flow: pick a member from the current page, then run its points action menu
    private void viewMemberPoints(LinkedListInterface<Member> pageList) {
        String memberId = pickMemberFromPage(pageList);
        if (memberId == null) {
            return;
        }
        Member member = findMember(memberId);
        if (member != null) {
            handleMemberPointsActions(member);
        }
    }

    // select-entity action loop for one member's points: balance -> action -> balance
    private void handleMemberPointsActions(Member member) {
        while (true) {
            moduleUI.displayBalance(member, getAvailableBalance(member.getMemberId(), LocalDateTime.now()));

            int action = moduleUI.getMemberPointsActionChoice();
            if (action == 0) {
                return;
            }

            switch (action) {
                case 1: // Run Expiry Check
                    moduleUI.showMessage(expirePoints(member.getMemberId(), LocalDateTime.now()));
                    break;
                case 2: // View Transaction History
                    moduleUI.displayTransactions(getTransactions(member.getMemberId()));
                    moduleUI.pause();
                    break;
                case 3: // View Tier Progression
                    moduleUI.showMessage(getTierProgress(member.getMemberId()));
                    break;
                case 4: // View Notifications
                    viewMemberNotifications(member);
                    break;
                default:
                    break;
            }

            member = findMember(member.getMemberId()); // re-read so balance stays fresh
            if (member == null) {
                return;
            }
        }
    }

    private void viewMemberNotifications(Member member) {
        if (member.getGuestId() == null) {
            moduleUI.showMessage("Member has no guest account linked.");
            return;
        }
        LinkedListInterface<Notification> list = getNotifications(member.getGuestId());
        moduleUI.displayNotifications(list);
        if (moduleUI.confirmMarkAllRead()) {
            for (int i = 0; i < list.size(); i++) {
                markNotificationRead(list.get(i).getNotificationId());
            }
            moduleUI.show("All notifications marked as read.");
        }
        moduleUI.pause();
    }

    // ======================= REPORTS =======================

    /** Management report submenu: 3 analytical reports with search + sort + filters. */
    private void runReports() {
        while (true) {
            int choice = moduleUI.getReportMenuChoice();
            if (choice == 0) {
                return;
            }
            switch (choice) {
                case 1:
                    generateMembershipReport();
                    break;
                case 2:
                    generateRedemptionReport();
                    break;
                case 3:
                    generateExpiryReport();
                    break;
                default:
                    break;
            }
            moduleUI.pause();
        }
    }

    // ---- Report 1: Membership & Tier Performance ----

    private void generateMembershipReport() {
        int tierChoice = moduleUI.inputReportTierFilter();
        Tier tier = tierChoice == 0 ? null : Tier.values()[tierChoice - 1];
        int minPoints = moduleUI.inputMinPoints();
        int status = moduleUI.inputMemberStatus();
        int promo = moduleUI.inputPromotionFilter();
        LocalDateTime[] range = reportUI.inputOptionalDateTimeRange("transaction");
        String keyword = moduleUI.inputSearchKeyword();
        int sortField = moduleUI.inputSortField(
                new String[] { "Name", "Tier", "Balance", "Cumulative Earned", "Transactions" });
        boolean asc = moduleUI.inputSortOrder();

        LocalDateTime now = LocalDateTime.now();
        LinkedListInterface<Member> rows = new LinkedList<>();
        for (int i = 0; i < memberList.size(); i++) {
            Member m = memberList.get(i);
            if (!statusMatches(m, status)) {
                continue;
            }
            if (tier != null && m.getTier() != tier) {
                continue;
            }
            if (m.getPoints() < minPoints) {
                continue;
            }
            if (promo == 2 && !m.hasActivePromotion(now)) {
                continue;
            }
            if (promo == 3 && m.hasActivePromotion(now)) {
                continue;
            }
            if (keyword != null && !keyword.isEmpty()
                    && !(m.getMemberId().toLowerCase().contains(keyword.toLowerCase())
                            || guestName(m).toLowerCase().contains(keyword.toLowerCase()))) {
                continue;
            }
            if (range != null && range[0] != null && !hasTxInRange(m, range)) {
                continue;
            }
            rows.addBack(m);
        }
        insertionSortMembers(rows, sortField, asc);

        String[] header = { "No.", "Member ID", "Name", "Tier", "Balance", "Cum Earned", "Txns", "Redemptions", "Promotion", "Status" };
        String[][] table = new String[rows.size() + 1][10];
        table[0] = header;
        int[] perTier = new int[Tier.values().length];
        double balanceSum = 0;
        long cumSum = 0;
        int withPromo = 0;
        for (int i = 0; i < rows.size(); i++) {
            Member m = rows.get(i);
            int cumulative = getCumulativeEarned(m.getMemberId());
            perTier[m.getTier() == null ? 0 : m.getTier().ordinal()]++;
            balanceSum += m.getPoints();
            cumSum += cumulative;
            if (m.hasActivePromotion(now)) {
                withPromo++;
            }
            table[i + 1] = new String[] {
                String.valueOf(i + 1), m.getMemberId(), guestName(m),
                m.getTier() == null ? "-" : m.getTier().name(),
                String.valueOf(m.getPoints()),
                String.valueOf(cumulative),
                String.valueOf(m.getPointTransactionList().size()),
                String.valueOf(m.getRedemptionRecordList().size()),
                promoText(m, now),
                m.isDeleted() ? "DELETED" : "ACTIVE"
            };
        }
        if (rows.isEmpty()) {
            ConsoleUtil.printError("No records match the given filters.");
            return;
        }

        String[] summary = {
            "TOTAL MEMBERS: " + rows.size(),
            "AVG BALANCE: " + Math.round(balanceSum / rows.size()) + " pts",
            "TOTAL PTS IN CIRCULATION: " + Math.round(balanceSum),
            "MEMBERS WITH ACTIVE PROMOTION: " + withPromo
        };

        LinkedListInterface<ReportChart> charts = new LinkedList<>();
        ReportChart byTier = new ReportChart("Members per Tier");
        ReportChart avgByTier = new ReportChart("Avg Balance per Tier");
        for (Tier t : Tier.values()) {
            byTier.addBar(t.name(), perTier[t.ordinal()], perTier[t.ordinal()] + " member(s)");
            int count = perTier[t.ordinal()];
            avgByTier.addBar(t.name(), count == 0 ? 0 : Math.round(balanceSumOfTier(t) / count),
                    count == 0 ? "0" : String.valueOf(count));
        }
        charts.addBack(byTier);
        charts.addBack(avgByTier);

        LinkedListInterface<String> callouts = new LinkedList<>();
        if (!rows.isEmpty()) {
            Member top = rows.get(0);
            for (int i = 1; i < rows.size(); i++) {
                if (getCumulativeEarned(rows.get(i).getMemberId()) > getCumulativeEarned(top.getMemberId())) {
                    top = rows.get(i);
                }
            }
            callouts.addBack("Top member by cumulative earnings: " + top.getMemberId()
                    + " " + guestName(top) + " (" + getCumulativeEarned(top.getMemberId()) + " pts)");
        }

        String criteria = "Tier=" + (tier == null ? "All" : tier)
                + ", Min pts=" + minPoints + ", Status=" + statusLabel(status)
                + ", Promotion=" + promoLabel(promo) + ", Sort=" + sortLabel(
                        new String[] { "Name", "Tier", "Balance", "Cum Earned", "Txns" }, sortField, asc);
        moduleUI.showCriteria(criteria);
        reportUI.printReport(new ReportResult(table, summary, charts, callouts),
                "MEMBERSHIP & TIER PERFORMANCE REPORT");
    }

    private double balanceSumOfTier(Tier t) {
        double sum = 0;
        for (int i = 0; i < memberList.size(); i++) {
            Member m = memberList.get(i);
            if (m.getTier() == t && !m.isDeleted()) {
                sum += m.getPoints();
            }
        }
        return sum;
    }

    // ---- Report 2: Redemption & Voucher ----

    private void generateRedemptionReport() {
        System.out.println("\nRedemption Status:");
        System.out.println("  0. All");
        System.out.println("  1. PENDING");
        System.out.println("  2. APPROVED");
        System.out.println("  3. REJECTED");
        int statusFilter = moduleUI.inputChoice("Enter status", 0, 3);
        System.out.println("\nVoucher Type:");
        System.out.println("  1. All");
        System.out.println("  2. Fixed RM");
        System.out.println("  3. Percentage (%)");
        System.out.println("  4. Not a voucher");
        int typeFilter = moduleUI.inputChoice("Enter type", 1, 4);
        int minCost = moduleUI.inputMinPoints();
        LocalDateTime[] range = reportUI.inputOptionalDateTimeRange("redemption");
        String keyword = moduleUI.inputSearchKeyword();
        int sortField = moduleUI.inputSortField(
                new String[] { "Member", "Reward", "Date", "Status", "Points Cost" });
        boolean asc = moduleUI.inputSortOrder();

        LinkedListInterface<RedemptionRecord> rows = new LinkedList<>();
        for (int i = 0; i < memberList.size(); i++) {
            Member m = memberList.get(i);
            LinkedListInterface<RedemptionRecord> recs = m.getRedemptionRecordList();
            for (int j = 0; j < recs.size(); j++) {
                RedemptionRecord r = recs.get(j);
                if (statusFilter == 1 && !"PENDING".equals(r.getStatus())) {
                    continue;
                }
                if (statusFilter == 2 && !"APPROVED".equals(r.getStatus())) {
                    continue;
                }
                if (statusFilter == 3 && !"REJECTED".equals(r.getStatus())) {
                    continue;
                }
                Reward reward = findReward(r.getRewardId());
                boolean isPercent = r.getDiscountPercent() != null;
                boolean isRM = r.getVoucherValue() != null;
                int type = isPercent ? 3 : (isRM ? 2 : 1);
                if (typeFilter == 2 && type != 2) {
                    continue;
                }
                if (typeFilter == 3 && type != 3) {
                    continue;
                }
                if (typeFilter == 4 && type != 1) {
                    continue;
                }
                int cost = reward == null ? 0 : reward.getPointCost();
                if (cost < minCost) {
                    continue;
                }
                if (keyword != null && !keyword.isEmpty()
                        && !(m.getMemberId().toLowerCase().contains(keyword.toLowerCase())
                                || guestName(m).toLowerCase().contains(keyword.toLowerCase())
                                || (reward != null && reward.getName().toLowerCase().contains(keyword.toLowerCase())))) {
                    continue;
                }
                if (range != null && range[0] != null && r.getRedeemedDate() != null
                        && (r.getRedeemedDate().isBefore(range[0]) || r.getRedeemedDate().isAfter(range[1]))) {
                    continue;
                }
                rows.addBack(r);
            }
        }
        insertionSortRedemptions(rows, sortField, asc, memberList);

        String[] header = { "No.", "Redemption ID", "Member", "Reward", "Type", "Status", "Pts Cost", "Voucher", "Used", "Date" };
        String[][] table = new String[rows.size() + 1][10];
        table[0] = header;
        int pending = 0, approved = 0, rejected = 0;
        long totalCost = 0;
        int vouchersIssued = 0, vouchersUsed = 0;
        for (int i = 0; i < rows.size(); i++) {
            RedemptionRecord r = rows.get(i);
            Member m = findMember(r.getMemberId());
            Reward reward = findReward(r.getRewardId());
            boolean isPercent = r.getDiscountPercent() != null;
            boolean isRM = r.getVoucherValue() != null;
            String typeText = isPercent ? r.getDiscountPercent() + "%" : (isRM ? "RM" : "Other");
            int cost = reward == null ? 0 : reward.getPointCost();
            if ("PENDING".equals(r.getStatus())) {
                pending++;
            } else if ("APPROVED".equals(r.getStatus())) {
                approved++;
            } else {
                rejected++;
            }
            totalCost += cost;
            boolean voucher = isPercent || isRM || r.getVoucherCode() != null;
            if (voucher && "APPROVED".equals(r.getStatus())) {
                vouchersIssued++;
            }
            if (voucher && r.isUsed()) {
                vouchersUsed++;
            }
            table[i + 1] = new String[] {
                String.valueOf(i + 1), r.getRedemptionId(),
                m == null ? r.getMemberId() : r.getMemberId() + " " + guestName(m),
                reward == null ? r.getRewardId() : reward.getName(),
                typeText, r.getStatus(), String.valueOf(cost),
                r.getVoucherCode() == null ? "-" : r.getVoucherCode(),
                r.isUsed() ? "USED" : "-",
                r.getRedeemedDate() == null ? "-" : r.getRedeemedDate().toLocalDate().toString()
            };
        }
        if (rows.isEmpty()) {
            ConsoleUtil.printError("No records match the given filters.");
            return;
        }

        double approvalRate = (pending + approved) == 0 ? 0 : approved * 100.0 / (pending + approved);
        String[] summary = {
            "TOTAL REDEMPTIONS: " + rows.size(),
            "PENDING: " + pending + " | APPROVED: " + approved + " | REJECTED: " + rejected,
            "TOTAL POINTS SPENT: " + totalCost,
            "VOUCHERS ISSUED: " + vouchersIssued + " | VOUCHERS USED: " + vouchersUsed,
            "APPROVAL RATE: " + Math.round(approvalRate) + "%"
        };

        LinkedListInterface<ReportChart> charts = new LinkedList<>();
        ReportChart byStatus = new ReportChart("Redemptions by Status");
        byStatus.addBar("PENDING", pending, pending + " req(s)");
        byStatus.addBar("APPROVED", approved, approved + " req(s)");
        byStatus.addBar("REJECTED", rejected, rejected + " req(s)");
        charts.addBack(byStatus);

        ReportChart byReward = new ReportChart("Redemptions per Reward");
        for (int i = 0; i < rewardList.size(); i++) {
            Reward reward = rewardList.get(i);
            int count = redemptionCountFor(reward.getRewardId());
            if (count > 0) {
                byReward.addBar(truncateName(reward.getName(), 8), count, count + " time(s)");
            }
        }
        charts.addBack(byReward);

        LinkedListInterface<String> callouts = new LinkedList<>();
        String most = mostRedeemedReward();
        if (most != null) {
            callouts.addBack("Most redeemed reward: " + most);
        }

        String criteria = "Status=" + statusLabel2(statusFilter) + ", Type=" + typeLabel(typeFilter)
                + ", Min cost=" + minCost + ", Sort=" + sortLabel(
                        new String[] { "Member", "Reward", "Date", "Status", "Pts Cost" }, sortField, asc);
        moduleUI.showCriteria(criteria);
        reportUI.printReport(new ReportResult(table, summary, charts, callouts),
                "REDEMPTION & VOUCHER REPORT");
    }

    // ---- Report 3: Point Expiry & Tier Progression ----

    private void generateExpiryReport() {
        int tierChoice = moduleUI.inputReportTierFilter();
        Tier tier = tierChoice == 0 ? null : Tier.values()[tierChoice - 1];
        int minCumulative = moduleUI.inputMinPoints();
        int window = moduleUI.inputExpiryWindow();
        String keyword = moduleUI.inputSearchKeyword();
        int sortField = moduleUI.inputSortField(
                new String[] { "Name", "Tier", "Balance", "Cumulative Earned", "Nearest Expiry" });
        boolean asc = moduleUI.inputSortOrder();

        LocalDateTime now = LocalDateTime.now();
        LinkedListInterface<Member> rows = new LinkedList<>();
        for (int i = 0; i < memberList.size(); i++) {
            Member m = memberList.get(i);
            if (m.isDeleted()) {
                continue;
            }
            if (tier != null && m.getTier() != tier) {
                continue;
            }
            if (getCumulativeEarned(m.getMemberId()) < minCumulative) {
                continue;
            }
            if (keyword != null && !keyword.isEmpty()
                    && !(m.getMemberId().toLowerCase().contains(keyword.toLowerCase())
                            || guestName(m).toLowerCase().contains(keyword.toLowerCase()))) {
                continue;
            }
            rows.addBack(m);
        }
        insertionSortMembers(rows, sortField, asc);

        String[] header = { "No.", "Member ID", "Name", "Tier", "Balance", "Cum Earned", "Next Tier", "Pts to Next", "Expiring <= " + window + "d" };
        String[][] table = new String[rows.size() + 1][9];
        table[0] = header;
        long cumSum = 0;
        long expiringSum = 0;
        int nearExpiry = 0;
        for (int i = 0; i < rows.size(); i++) {
            Member m = rows.get(i);
            int cumulative = getCumulativeEarned(m.getMemberId());
            cumSum += cumulative;
            int expiring = window > 0 ? expiringWithin(m, window, now) : 0;
            expiringSum += expiring;
            if (expiring > 0) {
                nearExpiry++;
            }
            Tier current = m.getTier() == null ? Tier.SILVER : m.getTier();
            String nextTier = "-";
            String ptsToNext = "-";
            if (current.ordinal() < Tier.values().length - 1) {
                Tier next = Tier.values()[current.ordinal() + 1];
                nextTier = next.name();
                ptsToNext = String.valueOf(TIER_THRESHOLDS[current.ordinal() + 1] - cumulative);
            }
            table[i + 1] = new String[] {
                String.valueOf(i + 1), m.getMemberId(), guestName(m),
                m.getTier() == null ? "-" : m.getTier().name(),
                String.valueOf(m.getPoints()),
                String.valueOf(cumulative),
                nextTier, ptsToNext,
                String.valueOf(expiring)
            };
        }
        if (rows.isEmpty()) {
            ConsoleUtil.printError("No records match the given filters.");
            return;
        }

        String[] summary = {
            "TOTAL MEMBERS: " + rows.size(),
            "AVG CUMULATIVE EARNED: " + Math.round(cumSum / rows.size()) + " pts",
            "MEMBERS WITH POINTS EXPIRING <= " + window + "d: " + nearExpiry,
            "TOTAL EXPIRING POINTS: " + expiringSum
        };

        LinkedListInterface<ReportChart> charts = new LinkedList<>();
        ReportChart byTier = new ReportChart("Members per Tier");
        ReportChart expTier = new ReportChart("Expiring Pts per Tier");
        for (Tier t : Tier.values()) {
            byTier.addBar(t.name(), memberCountOfTier(t), memberCountOfTier(t) + " member(s)");
            expTier.addBar(t.name(), expiringOfTier(t, window, now), "pts");
        }
        charts.addBack(byTier);
        charts.addBack(expTier);

        LinkedListInterface<String> callouts = new LinkedList<>();
        if (window > 0) {
            for (int i = 0; i < rows.size(); i++) {
                Member m = rows.get(i);
                int expiring = expiringWithin(m, window, now);
                if (expiring > 0) {
                    callouts.addBack(m.getMemberId() + " " + guestName(m)
                            + " has " + expiring + " pts expiring within " + window + " day(s).");
                }
            }
        }

        String criteria = "Tier=" + (tier == null ? "All" : tier)
                + ", Min cum=" + minCumulative + ", Expiry window=" + (window == 0 ? "None" : window + "d")
                + ", Sort=" + sortLabel(
                        new String[] { "Name", "Tier", "Balance", "Cum Earned", "Nearest Expiry" }, sortField, asc);
        moduleUI.showCriteria(criteria);
        reportUI.printReport(new ReportResult(table, summary, charts, callouts),
                "POINT EXPIRY & TIER PROGRESSION REPORT");
    }

    // ---- report helpers ----

    private boolean statusMatches(Member m, int status) {
        if (status == 1) {
            return !m.isDeleted();
        }
        if (status == 2) {
            return m.isDeleted();
        }
        return true;
    }

    private boolean hasTxInRange(Member m, LocalDateTime[] range) {
        LinkedListInterface<PointTransaction> txs = m.getPointTransactionList();
        for (int i = 0; i < txs.size(); i++) {
            LocalDateTime d = txs.get(i).getDate();
            if (d != null && !d.isBefore(range[0]) && !d.isAfter(range[1])) {
                return true;
            }
        }
        return false;
    }

    private int expiringWithin(Member m, int windowDays, LocalDateTime now) {
        int sum = 0;
        LinkedListInterface<PointTransaction> txs = m.getPointTransactionList();
        for (int i = 0; i < txs.size(); i++) {
            PointTransaction t = txs.get(i);
            if (t.getRemainingPoints() > 0 && t.getExpiryDate() != null
                    && !t.isExpired(now)
                    && t.getExpiryDate().isBefore(now.plusDays(windowDays))) {
                sum += t.getRemainingPoints();
            }
        }
        return sum;
    }

    private int memberCountOfTier(Tier t) {
        int count = 0;
        for (int i = 0; i < memberList.size(); i++) {
            Member m = memberList.get(i);
            if (!m.isDeleted() && m.getTier() == t) {
                count++;
            }
        }
        return count;
    }

    private int expiringOfTier(Tier t, int window, LocalDateTime now) {
        int sum = 0;
        for (int i = 0; i < memberList.size(); i++) {
            Member m = memberList.get(i);
            if (!m.isDeleted() && m.getTier() == t) {
                sum += expiringWithin(m, window, now);
            }
        }
        return sum;
    }

    private int redemptionCountFor(String rewardId) {
        int count = 0;
        for (int i = 0; i < memberList.size(); i++) {
            LinkedListInterface<RedemptionRecord> recs = memberList.get(i).getRedemptionRecordList();
            for (int j = 0; j < recs.size(); j++) {
                if (rewardId.equals(recs.get(j).getRewardId())) {
                    count++;
                }
            }
        }
        return count;
    }

    private String mostRedeemedReward() {
        String best = null;
        int bestCount = 0;
        for (int i = 0; i < rewardList.size(); i++) {
            Reward r = rewardList.get(i);
            int count = redemptionCountFor(r.getRewardId());
            if (count > bestCount) {
                bestCount = count;
                best = r.getName();
            }
        }
        return bestCount == 0 ? null : best;
    }

    private String guestName(Member m) {
        Guest g = m == null || m.getGuestId() == null ? null : findGuest(m.getGuestId());
        return g == null || g.getName() == null ? "-" : g.getName();
    }

    private String promoText(Member m, LocalDateTime now) {
        if (m.hasActivePromotion(now)) {
            return m.promotionLabel(now);
        }
        if (m.getPromotionName() != null) {
            return m.getPromotionName() + " (expired)";
        }
        return "-";
    }

    private String statusLabel(int status) {
        return status == 1 ? "Active" : (status == 2 ? "Deleted" : "All");
    }

    private String promoLabel(int promo) {
        return promo == 1 ? "All" : (promo == 2 ? "Active" : "No active");
    }

    private String statusLabel2(int status) {
        return status == 0 ? "All" : (status == 1 ? "PENDING" : (status == 2 ? "APPROVED" : "REJECTED"));
    }

    private String typeLabel(int type) {
        return type == 1 ? "All" : (type == 2 ? "RM" : (type == 3 ? "%" : "Not voucher"));
    }

    private String sortLabel(String[] labels, int sortField, boolean asc) {
        String field = sortField >= 1 && sortField <= labels.length ? labels[sortField - 1] : "?";
        return field + (asc ? " (asc)" : " (desc)");
    }

    private String truncateName(String text, int width) {
        if (text == null || text.length() <= width) {
            return text == null ? "-" : text;
        }
        return text.substring(0, width - 3) + "...";
    }

    /** Insertion sort on members by the chosen field (asc/desc). */
    private void insertionSortMembers(LinkedListInterface<Member> rows, int sortField, boolean asc) {
        for (int i = 1; i < rows.size(); i++) {
            Member key = rows.get(i);
            int j = i - 1;
            while (j >= 0 && compareMembers(rows.get(j), key, sortField, asc) > 0) {
                rows.set(j + 1, rows.get(j));
                j--;
            }
            rows.set(j + 1, key);
        }
    }

    private int compareMembers(Member a, Member b, int sortField, boolean asc) {
        int cmp;
        switch (sortField) {
            case 1: // Tier
                cmp = Integer.compare(a.getTier() == null ? 0 : a.getTier().ordinal(),
                        b.getTier() == null ? 0 : b.getTier().ordinal());
                break;
            case 2: // Balance
                cmp = Integer.compare(a.getPoints(), b.getPoints());
                break;
            case 3: // Cumulative earned
                cmp = Integer.compare(getCumulativeEarned(a.getMemberId()), getCumulativeEarned(b.getMemberId()));
                break;
            case 4: // Transactions
                cmp = Integer.compare(a.getPointTransactionList().size(), b.getPointTransactionList().size());
                break;
            default: // Name
                cmp = guestName(a).compareToIgnoreCase(guestName(b));
                break;
        }
        return asc ? cmp : -cmp;
    }

    /** Insertion sort on redemption records by the chosen field (asc/desc). */
    private void insertionSortRedemptions(LinkedListInterface<RedemptionRecord> rows, int sortField, boolean asc,
            LinkedListInterface<Member> members) {
        for (int i = 1; i < rows.size(); i++) {
            RedemptionRecord key = rows.get(i);
            int j = i - 1;
            while (j >= 0 && compareRedemptions(rows.get(j), key, sortField, asc, members) > 0) {
                rows.set(j + 1, rows.get(j));
                j--;
            }
            rows.set(j + 1, key);
        }
    }

    private int compareRedemptions(RedemptionRecord a, RedemptionRecord b, int sortField, boolean asc,
            LinkedListInterface<Member> members) {
        int cmp;
        switch (sortField) {
            case 1: // Reward name
                Reward ra = findReward(a.getRewardId());
                Reward rb = findReward(b.getRewardId());
                cmp = (ra == null ? a.getRewardId() : ra.getName())
                        .compareToIgnoreCase(rb == null ? b.getRewardId() : rb.getName());
                break;
            case 2: // Date
                LocalDateTime da = a.getRedeemedDate();
                LocalDateTime db = b.getRedeemedDate();
                cmp = da == null ? (db == null ? 0 : -1) : (db == null ? 1 : da.compareTo(db));
                break;
            case 3: // Status
                cmp = String.valueOf(a.getStatus()).compareTo(String.valueOf(b.getStatus()));
                break;
            case 4: // Points cost
                Reward ca = findReward(a.getRewardId());
                Reward cb = findReward(b.getRewardId());
                cmp = Integer.compare(ca == null ? 0 : ca.getPointCost(), cb == null ? 0 : cb.getPointCost());
                break;
            default: // Member id
                cmp = String.valueOf(a.getMemberId()).compareTo(String.valueOf(b.getMemberId()));
                break;
        }
        return asc ? cmp : -cmp;
    }

    /** Unread notification count across ALL members (module menu badge). */
    public int getUnreadNotificationCount() {
        int count = 0;
        for (int i = 0; i < guestList.size(); i++) {
            LinkedListInterface<Notification> list = guestList.get(i).getNotificationList();
            for (int j = 0; j < list.size(); j++) {
                if (!list.get(j).isRead()) {
                    count++;
                }
            }
        }
        return count;
    }

    /** Marks every notification across all members as read; returns how many were flipped. */
    public int markAllNotificationsRead() {
        int count = 0;
        for (int i = 0; i < guestList.size(); i++) {
            LinkedListInterface<Notification> list = guestList.get(i).getNotificationList();
            for (int j = 0; j < list.size(); j++) {
                Notification n = list.get(j);
                if (!n.isRead()) {
                    n.setRead(true);
                    count++;
                }
            }
        }
        if (count > 0) {
            guestDAO.saveToFile(guestList);
        }
        return count;
    }

    /** Unread notification count for ONE member (member list column). */
    public int getUnreadNotificationCount(String memberId) {
        Member member = findMember(memberId);
        if (member == null || member.getGuestId() == null) {
            return 0;
        }
        int count = 0;
        LinkedListInterface<Notification> list = getNotifications(member.getGuestId());
        for (int i = 0; i < list.size(); i++) {
            if (!list.get(i).isRead()) {
                count++;
            }
        }
        return count;
    }

    /** Marks one member's notifications as read; returns how many were flipped. */
    public int markMemberNotificationsRead(String memberId) {
        Member member = findMember(memberId);
        if (member == null || member.getGuestId() == null) {
            return 0;
        }
        int count = 0;
        Guest guest = findGuest(member.getGuestId());
        if (guest == null) {
            return 0;
        }
        LinkedListInterface<Notification> list = guest.getNotificationList();
        for (int j = 0; j < list.size(); j++) {
            Notification n = list.get(j);
            if (!n.isRead()) {
                n.setRead(true);
                count++;
            }
        }
        if (count > 0) {
            guestDAO.saveToFile(guestList);
        }
        return count;
    }

    /** Notification centre: pick a member, then manage that member's notifications. */
    private void runNotificationCentre() {
        int memberPage = 0;
        while (true) {
            // level 1: all active members with their unread counts
            LinkedListInterface<Member> display = getActiveMembers();
            int pageCount = Math.max(1, (display.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            if (memberPage >= pageCount) {
                memberPage = pageCount - 1;
            }
            LinkedListInterface<Member> pageList = pageOf(display, memberPage);

            String[][] memberRows = new String[pageList.size()][];
            for (int i = 0; i < pageList.size(); i++) {
                Member m = pageList.get(i);
                Guest g = m.getGuestId() == null ? null : findGuest(m.getGuestId());
                memberRows[i] = new String[] {
                    String.valueOf(i + 1), m.getMemberId(),
                    g == null ? "-" : g.getName(),
                    String.valueOf(getUnreadNotificationCount(m.getMemberId()))
                };
            }

            int choice = moduleUI.printMemberListMenu(
                    memberRows, memberPage, pageCount, getUnreadNotificationCount());
            if (choice == 0) {
                return;
            }
            int action = 1;
            if (choice == action++) { // 1. View Notifications - pick a member
                int index = moduleUI.inputListIndex("member number", pageList.size());
                if (index == 0) {
                    continue;
                }
                viewMemberNotificationsCentre(pageList.get(index - 1));
            } else if (choice == action++) { // 2. Mark All Read (all members)
                int marked = markAllNotificationsRead();
                moduleUI.showMessage(marked + " notification(s) marked as read.");
                moduleUI.pause();
                memberPage = 0;
            } else {
                boolean matched = false;
                if (memberPage < pageCount - 1) { // Next Page
                    matched = choice == action;
                    action++;
                    if (matched) {
                        memberPage++;
                    }
                }
                if (!matched && memberPage > 0) { // Previous Page
                    matched = choice == action;
                    action++;
                    if (matched) {
                        memberPage--;
                    }
                }
            }
        }
    }

    /** Level 2: one member's notifications, newest first, with per-member mark-all-read. */
    private void viewMemberNotificationsCentre(Member member) {
        if (member.getGuestId() == null) {
            moduleUI.showMessage("Member has no guest account linked.");
            moduleUI.pause();
            return;
        }
        int page = 0;
        while (true) {
            LinkedListInterface<Notification> list = getNotifications(member.getGuestId());
            int pageCount = Math.max(1, (list.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            if (page >= pageCount) {
                page = pageCount - 1;
            }
            int from = page * PAGE_SIZE;
            String[][] rows = new String[Math.min(PAGE_SIZE, list.size() - from)][];
            for (int k = 0; k < rows.length; k++) {
                Notification n = list.get(from + k);
                rows[k] = new String[] {
                    String.valueOf(k + 1), n.getType(),
                    truncate(n.getMessage(), 48),
                    n.getDate().format(NOTIF_DATE_FMT),
                    n.isRead() ? "READ" : "UNREAD"
                };
            }
            Guest g = findGuest(member.getGuestId());
            int choice = moduleUI.printMemberNotificationsMenu(
                    member.getMemberId(), g == null ? null : g.getName(), rows, page, pageCount);
            if (choice == 0) {
                return;
            }
            int action = 1;
            if (choice == action++) { // 1. Mark All Read (this member)
                int marked = markMemberNotificationsRead(member.getMemberId());
                moduleUI.showMessage(marked + " notification(s) marked as read.");
                moduleUI.pause();
                page = 0;
            } else {
                boolean matched = false;
                if (page < pageCount - 1) { // Next Page
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page++;
                    }
                }
                if (!matched && page > 0) { // Previous Page
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page--;
                    }
                }
            }
        }
    }

    private static String truncate(String text, int max) {
        if (text == null || text.length() <= max) {
            return text == null ? "" : text;
        }
        return text.substring(0, max - 3) + "...";
    }

    // pick a member by its on-screen number from the current page (0 = cancel)
    private String pickMemberFromPage(LinkedListInterface<Member> pageList) {
        if (pageList.isEmpty()) {
            moduleUI.showMessage("(No member records)");
            return null;
        }
        int num = moduleUI.inputListIndex("member", pageList.size());
        if (num == 0) {
            return null;
        }
        Member member = pageList.get(num - 1);
        return member == null ? null : member.getMemberId();
    }

    // ======================= MEMBER MANAGEMENT =======================

    /** Registers an existing guest as a member (called from Guest Management). */
    public String registerMember(Guest guest) {
        if (guest == null || guest.getGuestId() == null) {
            return "Guest cannot be null and must have an id.";
        }
        Member existing = findMemberByGuestId(guest.getGuestId());
        if (existing != null) {
            return "This guest is already registered as member: " + existing.getMemberId() + ".";
        }
        Member member = new Member(nextMemberId(), 0, Tier.SILVER, LocalDateTime.now(), guest.getGuestId());
        memberList.addSorted(member);
        persistMembers();
        return "Guest " + guest.getName() + " registered as member " + member.getMemberId()
                + " (Tier: " + member.getTier() + ").";
    }

    public Member findMemberByGuestId(String guestId) {
        for (int i = 0; i < memberList.size(); i++) {
            Member m = memberList.get(i);
            if (m.getGuestId() != null && m.getGuestId().equals(guestId) && !m.isDeleted()) {
                return m;
            }
        }
        return null;
    }

    public LinkedListInterface<Member> getMembers() {
        return memberList;
    }

    public Member findMember(String memberId) {
        for (int i = 0; i < memberList.size(); i++) {
            if (memberList.get(i).getMemberId().equals(memberId)) {
                return memberList.get(i);
            }
        }
        return null;
    }

    public String removeMember(String memberId) {
        Member member = findMember(memberId);
        if (member == null) {
            return "Member not found: " + memberId;
        }
        if (member.isDeleted()) {
            return "Member " + memberId + " is already removed.";
        }
        member.setDeleted(true);

        // auto-reject any still-pending redemption requests
        int rejected = 0;
        LinkedListInterface<RedemptionRecord> records = member.getRedemptionRecordList();
        for (int i = 0; i < records.size(); i++) {
            RedemptionRecord r = records.get(i);
            if (r != null && "PENDING".equals(r.getStatus())) {
                r.setStatus("REJECTED");
                rejected++;
            }
        }
        persistMembers();
        return "Member " + memberId + " removed (soft delete). " + rejected
                + " pending redemption request(s) rejected.";
    }

    /** Undoes a soft delete so the member appears in active views again. */
    public String restoreMember(String memberId) {
        Member member = findMember(memberId);
        if (member == null) {
            return "Member not found: " + memberId;
        }
        if (!member.isDeleted()) {
            return "Member " + memberId + " is not deleted.";
        }
        member.setDeleted(false);
        persistMembers();
        return "Member " + memberId + " restored.";
    }

    /** Members that are active (not soft-deleted) - used for all list views. */
    public LinkedListInterface<Member> getActiveMembers() {
        LinkedListInterface<Member> active = new LinkedList<>();
        for (int i = 0; i < memberList.size(); i++) {
            Member m = memberList.get(i);
            if (!m.isDeleted()) {
                active.addBack(m);
            }
        }
        return active;
    }

    /** Soft-deleted members (history kept, hidden from active views). */
    public LinkedListInterface<Member> getDeletedMembers() {
        LinkedListInterface<Member> deleted = new LinkedList<>();
        for (int i = 0; i < memberList.size(); i++) {
            Member m = memberList.get(i);
            if (m.isDeleted()) {
                deleted.addBack(m);
            }
        }
        return deleted;
    }

    public String updateMember(String memberId, Tier tier) {
        Member member = findMember(memberId);
        if (member == null) {
            return "Member not found: " + memberId;
        }
        member.setTier(tier);
        persistMembers();
        return "Member " + memberId + " updated to tier " + tier + ".";
    }

    public String nextMemberId() {
        try {
            int max = 0;
            for (int i = 0; i < memberList.size(); i++) {
                String mid = memberList.get(i).getMemberId();
                if (mid != null && mid.matches("M\\d+")) {
                    int n = Integer.parseInt(mid.substring(1));
                    if (n > max) {
                        max = n;
                    }
                }
            }
            return String.format("M%03d", max + 1);
        } catch (RuntimeException e) {
            return String.format("M%03d", memberList.size() + 1);
        }
    }

    // ======================= REWARD MANAGEMENT =======================

    private void addRewardFlow() {
        Reward reward = moduleUI.inputNewReward(nextRewardId());
        if (reward == null) {
            moduleUI.showMessage("Operation cancelled.");
            return;
        }
        moduleUI.printRewardCreationSummary(reward);
        if (!moduleUI.confirm("Add this reward?")) {
            moduleUI.showMessage("Reward creation cancelled.");
            return;
        }
        moduleUI.showMessage(addReward(reward));
    }

    public LinkedListInterface<Reward> getRewards() {
        return rewardList;
    }

    public Reward findReward(String rewardId) {
        for (int i = 0; i < rewardList.size(); i++) {
            if (rewardList.get(i).getRewardId().equals(rewardId)) {
                return rewardList.get(i);
            }
        }
        return null;
    }

    public String addReward(Reward reward) {
        if (reward == null || reward.getRewardId() == null) {
            return "Reward cannot be null and must have an id.";
        }
        if (findReward(reward.getRewardId()) != null) {
            return "Reward id already exists: " + reward.getRewardId();
        }
        rewardList.addSorted(reward);
        persistRewards();
        return "Reward added: " + reward.getName() + " (" + reward.getPointCost() + " pts).";
    }

    public String removeReward(String rewardId) {
        Reward reward = findReward(rewardId);
        if (reward == null) {
            return "Reward not found: " + rewardId;
        }
        if (reward.isDeleted()) {
            return "Reward " + rewardId + " is already removed.";
        }
        reward.setDeleted(true);
        persistRewards();
        return "Reward removed (soft delete): " + reward.getName() + " (" + rewardId + ").";
    }

    /** Undoes a soft delete so the reward appears in active views again. */
    public String restoreReward(String rewardId) {
        Reward reward = findReward(rewardId);
        if (reward == null) {
            return "Reward not found: " + rewardId;
        }
        if (!reward.isDeleted()) {
            return "Reward " + rewardId + " is not deleted.";
        }
        reward.setDeleted(false);
        persistRewards();
        return "Reward " + rewardId + " restored.";
    }

    /** Rewards that are active (not soft-deleted) - used for all list views. */
    public LinkedListInterface<Reward> getActiveRewards() {
        LinkedListInterface<Reward> active = new LinkedList<>();
        for (int i = 0; i < rewardList.size(); i++) {
            Reward r = rewardList.get(i);
            if (!r.isDeleted()) {
                active.addBack(r);
            }
        }
        return active;
    }

    /** Soft-deleted rewards (history kept, hidden from active views). */
    public LinkedListInterface<Reward> getDeletedRewards() {
        LinkedListInterface<Reward> deleted = new LinkedList<>();
        for (int i = 0; i < rewardList.size(); i++) {
            Reward r = rewardList.get(i);
            if (r.isDeleted()) {
                deleted.addBack(r);
            }
        }
        return deleted;
    }

    public String updateReward(String rewardId, String name, String description, int pointCost,
            Tier minTier, RoomType roomType, Double voucherValue, Integer discountPercent) {
        Reward reward = findReward(rewardId);
        if (reward == null) {
            return "Reward not found: " + rewardId;
        }
        reward.setName(name);
        reward.setDescription(description);
        reward.setPointCost(pointCost);
        reward.setMinTier(minTier);
        reward.setRoomType(roomType);
        reward.setVoucherValue(voucherValue);
        reward.setDiscountPercent(discountPercent);
        LinkedListInterface<Reward> reordered = new LinkedList<>();
        for (int i = 0; i < rewardList.size(); i++) {
            reordered.addSorted(rewardList.get(i));
        }
        rewardList.clear();
        for (int i = 0; i < reordered.size(); i++) {
            rewardList.addBack(reordered.get(i));
        }
        persistRewards();
        return "Reward updated: " + reward.getName() + " (" + reward.getPointCost() + " pts).";
    }

    public String nextRewardId() {
        try {
            int max = 0;
            for (int i = 0; i < rewardList.size(); i++) {
                String rid = rewardList.get(i).getRewardId();
                if (rid != null && rid.matches("R\\d+")) {
                    int n = Integer.parseInt(rid.substring(1));
                    if (n > max) {
                        max = n;
                    }
                }
            }
            return String.format("R%03d", max + 1);
        } catch (RuntimeException e) {
            return String.format("R%03d", rewardList.size() + 1);
        }
    }

    // ======================= POINTS & REDEMPTION =======================

    private void reconcileTiersOnLoad() {
        boolean changed = false;
        for (int i = 0; i < memberList.size(); i++) {
            Member m = memberList.get(i);
            if (m.isDeleted()) {
                continue; // soft-deleted members keep their stored tier
            }
            Tier correct = tierFor(getCumulativeEarned(m.getMemberId()));
            if (m.getTier() != correct) {
                m.setTier(correct);
                changed = true;
            }
        }
        if (changed) {
            memberDAO.saveToFile(memberList);
        }
    }

    private void earnPointsFlow(LinkedListInterface<Member> pageList) {
        String memberId = pickMemberFromPage(pageList);
        if (memberId == null) {
            return;
        }
        int amount = moduleUI.inputAmount();
        if (amount == 0) {
            moduleUI.showMessage("Operation cancelled.");
            return;
        }
        String description = moduleUI.inputDescription();
        if (description == null) {
            moduleUI.showMessage("Operation cancelled.");
            return;
        }
        moduleUI.showMessage(earnPoints(memberId, amount, description, LocalDateTime.now()));
    }

    private void requestRedemptionFlow(LinkedListInterface<Member> pageList) {
        String memberId = pickMemberFromPage(pageList);
        if (memberId == null) {
            return;
        }
        Member member = findMember(memberId);
        if (member == null) {
            return;
        }
        LinkedListInterface<Reward> eligible = getRewardsEligibleFor(member.getTier());
        String rewardId = moduleUI.selectReward(eligible);
        if (rewardId == null) {
            return;
        }
        moduleUI.showMessage(requestRedemption(memberId, rewardId, LocalDateTime.now()));
    }

    private void processRedemptionRequestsFlow() {
        LinkedListInterface<RedemptionRecord> pending = getPendingRedemptions();
        String redemptionId = moduleUI.selectPendingRequest(pending);
        if (redemptionId == null) {
            return;
        }
        String answer = moduleUI.approveOrReject();
        if (answer == null) {
            moduleUI.showMessage("Operation cancelled.");
            return;
        }
        if ("a".equals(answer)) {
            moduleUI.showMessage(approveRedemption(redemptionId, LocalDateTime.now()));
        } else {
            moduleUI.showMessage(rejectRedemption(redemptionId, LocalDateTime.now()));
        }
    }

    public LinkedListInterface<PointTransaction> getPointTransactions() {
        LinkedListInterface<PointTransaction> result = new LinkedList<>();
        for (int i = 0; i < memberList.size(); i++) {
            LinkedListInterface<PointTransaction> list = memberList.get(i).getPointTransactionList();
            for (int j = 0; j < list.size(); j++) {
                result.addBack(list.get(j));
            }
        }
        return result;
    }

    public LinkedListInterface<RedemptionRecord> getRedemptions() {
        LinkedListInterface<RedemptionRecord> result = new LinkedList<>();
        for (int i = 0; i < memberList.size(); i++) {
            LinkedListInterface<RedemptionRecord> list = memberList.get(i).getRedemptionRecordList();
            for (int j = 0; j < list.size(); j++) {
                result.addBack(list.get(j));
            }
        }
        return result;
    }

    public LinkedListInterface<PointTransaction> getTransactions(String memberId) {
        LinkedListInterface<PointTransaction> result = new LinkedList<>();
        Member member = findMember(memberId);
        if (member == null) {
            return result;
        }
        LinkedListInterface<PointTransaction> list = member.getPointTransactionList();
        for (int i = 0; i < list.size(); i++) {
            result.addBack(list.get(i));
        }
        return result;
    }

    public String expirePoints(String memberId, LocalDateTime now) {
        Member member = findMember(memberId);
        if (member == null) {
            return "Member not found: " + memberId;
        }

        int totalExpired = 0;
        StringBuilder report = new StringBuilder();
        LinkedListInterface<PointTransaction> txs = getTransactions(memberId);
        for (int i = 0; i < txs.size(); i++) {
            PointTransaction t = txs.get(i);
            if (t.isExpired(now)) {
                totalExpired += t.getRemainingPoints();
                report.append("  - ").append(t.getRemainingPoints())
                        .append(" pts expired (tx ").append(t.getTransactionId())
                        .append(", earned ").append(t.getDate()).append("\n");
                t.setRemainingPoints(0);
            }
        }

        recomputeBalance(member);
        if (totalExpired > 0) {
            persistMembers();
            return totalExpired + " point(s) expired and removed from " + memberId + "'s balance:\n" + report;
        }
        return "No points expired for " + memberId + ".";
    }

    public String earnPoints(String memberId, int amount, String description, LocalDateTime date) {
        Member member = findMember(memberId);
        if (member == null) {
            return "Member not found: " + memberId;
        }
        if (amount <= 0) {
            return "Amount must be positive.";
        }
        expirePoints(memberId, date);

        LocalDateTime expiry = date.plusDays(365);
        PointTransaction t = new PointTransaction(nextTransactionId(), date,
                description == null || description.isBlank() ? "Points earned" : description,
                amount, expiry, amount, memberId);
        member.getPointTransactionList().addSorted(t);
        recomputeBalance(member);
        recomputeTier(member, date);
        persistMembers();
        return amount + " pts earned by " + memberId + " (expires " + expiry.toLocalDate() + "). New balance: "
                + member.getPoints();
    }

    /**
     * Deduct points - used when a booking refund claws back the points that
     * were earned at payment. Writes a negative PointTransaction (never
     * expires) clamped so the member's balance cannot go below zero.
     */
    public String deductPoints(String memberId, int amount, String description, LocalDateTime date) {
        Member member = findMember(memberId);
        if (member == null) {
            return "Member not found: " + memberId;
        }
        if (amount <= 0) {
            return "Amount must be positive.";
        }
        expirePoints(memberId, date);

        int deduction = Math.min(amount, member.getPoints());
        if (deduction <= 0) {
            return "No points to deduct from " + memberId + ".";
        }
        PointTransaction t = new PointTransaction(nextTransactionId(), date,
                description == null || description.isBlank() ? "Points deducted" : description,
                -deduction, date, -deduction, memberId);
        member.getPointTransactionList().addSorted(t);
        recomputeBalance(member);
        recomputeTier(member, date);
        persistMembers();
        return deduction + " pts deducted from " + memberId + " (refund claw-back). New balance: "
                + member.getPoints();
    }

    public String requestRedemption(String memberId, String rewardId, LocalDateTime now) {
        Member member = findMember(memberId);
        if (member == null) {
            return "Member not found: " + memberId;
        }
        Reward reward = findReward(rewardId);
        if (reward == null) {
            return "Reward not found: " + rewardId;
        }
        if (reward.isDeleted()) {
            return "Reward not available: " + rewardId + " has been removed.";
        }
        Tier minTier = reward.getMinTier() == null ? Tier.SILVER : reward.getMinTier();
        if (member.getTier() == null || member.getTier().ordinal() < minTier.ordinal()) {
            return "Tier not high enough: " + reward.getName() + " requires " + minTier
                    + " but " + memberId + " is " + (member.getTier() == null ? "SILVER" : member.getTier()) + ".";
        }
        expirePoints(memberId, now);
        int cost = reward.getPointCost();
        if (member.getPoints() < cost) {
            return "Insufficient points: " + memberId + " needs " + cost + " pts but only has "
                    + member.getPoints() + ".";
        }
        member.getRedemptionRecordList().addSorted(new RedemptionRecord(nextRedemptionId(), now, memberId, rewardId));
        persistMembers();
        return "Redemption requested: " + reward.getName() + " (" + cost + " pts) for " + memberId
                + " - pending approval.";
    }

    public String approveRedemption(String redemptionId, LocalDateTime now) {
        RedemptionRecord record = findRedemption(redemptionId);
        if (record == null) {
            return "Redemption request not found: " + redemptionId;
        }
        if (!"PENDING".equals(record.getStatus())) {
            return "Request " + redemptionId + " is already " + record.getStatus() + ".";
        }
        Member member = findMember(record.getMemberId());
        if (member == null) {
            return "Member not found: " + record.getMemberId();
        }
        Reward reward = findReward(record.getRewardId());
        if (reward == null) {
            return "Reward not found: " + record.getRewardId();
        }

        expirePoints(member.getMemberId(), now);
        int cost = reward.getPointCost();
        if (member.getPoints() < cost) {
            return "Cannot approve " + redemptionId + ": " + member.getMemberId()
                    + " no longer has enough points (" + member.getPoints() + "/" + cost + ").";
        }

        int remainingCost = cost;
        StringBuilder breakdown = new StringBuilder();
        LinkedListInterface<PointTransaction> txs = getTransactions(member.getMemberId());
        for (int i = 0; i < txs.size() && remainingCost > 0; i++) {
            PointTransaction t = txs.get(i);
            int used = Math.min(t.getRemainingPoints(), remainingCost);
            if (used > 0) {
                t.setRemainingPoints(t.getRemainingPoints() - used);
                remainingCost -= used;
                breakdown.append("  - ").append(used).append(" pts from tx ")
                        .append(t.getTransactionId()).append(" (earned ").append(t.getDate()).append("\n");
            }
        }

        recomputeBalance(member);
        record.setStatus("APPROVED");

        // voucher-type rewards: issue a redeemable code and lock in the value
        // (either a fixed RM amount or a percentage discount)
        String voucherNote = "";
        if (reward.getVoucherValue() != null && reward.getVoucherValue() > 0) {
            record.setVoucherCode(generateVoucherCode(redemptionId));
            record.setVoucherValue(reward.getVoucherValue());
            record.setRoomType(reward.getRoomType());
            voucherNote = " Voucher code: " + record.getVoucherCode()
                    + " (worth RM" + String.format("%.2f", reward.getVoucherValue()) + ").";
        } else if (reward.getDiscountPercent() != null && reward.getDiscountPercent() > 0) {
            record.setVoucherCode(generateVoucherCode(redemptionId));
            record.setDiscountPercent(reward.getDiscountPercent());
            record.setRoomType(reward.getRoomType());
            voucherNote = " Voucher code: " + record.getVoucherCode()
                    + " (worth " + record.getDiscountPercent() + "% off "
                    + (record.getRoomType() == null ? "any room" : record.getRoomType().name()) + ").";
        }
        notifyMember(member, "REDEMPTION_APPROVED",
                "Your redemption request " + redemptionId + " (" + reward.getName() + ") has been approved."
                        + voucherNote, now);
        persistMembers();
        return "Approved " + redemptionId + " (" + reward.getName() + "):\n" + breakdown
                + (voucherNote.isEmpty() ? "" : "  - voucher " + record.getVoucherCode()
                        + (record.getDiscountPercent() != null
                            ? " worth " + record.getDiscountPercent() + "% off "
                                    + (record.getRoomType() == null ? "any room" : record.getRoomType().name())
                            : " worth RM" + String.format("%.2f", record.getVoucherValue())) + "\n")
                + "New balance for " + member.getMemberId() + ": " + member.getPoints();
    }

    /**
     * Returns the member's unused, approved voucher redemptions - the vouchers
     * that can be offered to the customer at payment time.
     *
     * @param memberId the member to look up
     * @return approved, unused vouchers (empty list if none)
     */
    public LinkedListInterface<RedemptionRecord> getAvailableVouchers(String memberId) {
        LinkedListInterface<RedemptionRecord> result = new LinkedList<>();
        Member member = findMember(memberId);
        if (member == null) {
            return result;
        }
        LinkedListInterface<RedemptionRecord> list = member.getRedemptionRecordList();
        for (int i = 0; i < list.size(); i++) {
            RedemptionRecord r = list.get(i);
            if ("APPROVED".equals(r.getStatus())
                    && !r.isUsed()
                    && r.getVoucherCode() != null
                    && ((r.getVoucherValue() != null && r.getVoucherValue() > 0)
                        || (r.getDiscountPercent() != null && r.getDiscountPercent() > 0))) {
                result.addBack(r);
            }
        }
        return result;
    }

    /**
     * Marks an approved, unused voucher as used. Called by the payment flow
     * once the voucher value has been applied to the customer's bill.
     *
     * @param memberId     the member who owns the voucher
     * @param redemptionId the redemption (voucher) to mark used
     * @return a result message for the UI
     */
    public String useVoucher(String memberId, String redemptionId) {
        Member member = findMember(memberId);
        if (member == null) {
            return "Member not found: " + memberId;
        }
        LinkedListInterface<RedemptionRecord> list = member.getRedemptionRecordList();
        for (int i = 0; i < list.size(); i++) {
            RedemptionRecord r = list.get(i);
            if (r.getRedemptionId().equals(redemptionId)) {
                if (!"APPROVED".equals(r.getStatus())) {
                    return "Voucher " + redemptionId + " is not approved ("
                            + r.getStatus() + ").";
                }
                if (r.isUsed()) {
                    return "Voucher " + redemptionId + " has already been used.";
                }
                if (r.getVoucherCode() == null) {
                    return "Redemption " + redemptionId + " is not a voucher.";
                }
                r.setUsed(true);
                persistMembers();
                String worth = r.getDiscountPercent() != null
                        ? r.getDiscountPercent() + "% off " + (r.getRoomType() == null ? "any room" : r.getRoomType().name())
                        : "RM" + String.format("%.2f", r.getVoucherValue());
                return "Voucher " + r.getVoucherCode() + " (" + worth + ") marked as used.";
            }
        }
        return "Voucher not found: " + redemptionId;
    }

    /** Generates a unique-looking voucher code, e.g. VCH-RR0001-8F3K. */
    private String generateVoucherCode(String redemptionId) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder("VCH-").append(redemptionId).append('-');
        for (int i = 0; i < 4; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public String rejectRedemption(String redemptionId, LocalDateTime now) {
        RedemptionRecord record = findRedemption(redemptionId);
        if (record == null) {
            return "Redemption request not found: " + redemptionId;
        }
        if (!"PENDING".equals(record.getStatus())) {
            return "Request " + redemptionId + " is already " + record.getStatus() + ".";
        }
        record.setStatus("REJECTED");
        Member member = findMember(record.getMemberId());
        if (member != null) {
            Reward reward = findReward(record.getRewardId());
            String rewardName = reward == null ? record.getRewardId() : reward.getName();
            notifyMember(member, "REDEMPTION_REJECTED",
                    "Your redemption request " + redemptionId + " (" + rewardName + ") has been rejected.", now);
        }
        persistMembers();
        return "Rejected redemption request " + redemptionId + ".";
    }

    public LinkedListInterface<RedemptionRecord> getPendingRedemptions() {
        LinkedListInterface<RedemptionRecord> result = new LinkedList<>();
        for (int i = 0; i < memberList.size(); i++) {
            if (memberList.get(i).isDeleted()) {
                continue; // deleted members' requests were auto-rejected
            }
            LinkedListInterface<RedemptionRecord> list = memberList.get(i).getRedemptionRecordList();
            for (int j = 0; j < list.size(); j++) {
                if ("PENDING".equals(list.get(j).getStatus())) {
                    result.addBack(list.get(j));
                }
            }
        }
        return result;
    }

    private RedemptionRecord findRedemption(String redemptionId) {
        for (int i = 0; i < memberList.size(); i++) {
            LinkedListInterface<RedemptionRecord> list = memberList.get(i).getRedemptionRecordList();
            for (int j = 0; j < list.size(); j++) {
                if (list.get(j).getRedemptionId().equals(redemptionId)) {
                    return list.get(j);
                }
            }
        }
        return null;
    }

    public int getAvailableBalance(String memberId, LocalDateTime now) {
        Member member = findMember(memberId);
        if (member == null) {
            return 0;
        }
        expirePoints(memberId, now);
        return member.getPoints();
    }

    private void recomputeBalance(Member member) {
        int sum = 0;
        LinkedListInterface<PointTransaction> txs = getTransactions(member.getMemberId());
        for (int i = 0; i < txs.size(); i++) {
            sum += txs.get(i).getRemainingPoints();
        }
        member.setPoints(sum);
    }

    private void persistMembers() {
        memberDAO.saveToFile(memberList);
    }

    private void persistRewards() {
        rewardDAO.saveToFile(rewardList);
    }

    private String nextTransactionId() {
        try {
            int max = 0;
            for (int i = 0; i < memberList.size(); i++) {
                LinkedListInterface<PointTransaction> tlist = memberList.get(i).getPointTransactionList();
                for (int j = 0; j < tlist.size(); j++) {
                    String tid = tlist.get(j).getTransactionId();
                    if (tid != null && tid.matches("PT\\d+")) {
                        int n = Integer.parseInt(tid.substring(2));
                        if (n > max) {
                            max = n;
                        }
                    }
                }
            }
            String id = String.format("PT%04d", max + 1);
            while (findTransaction(id) != null) {
                max++;
                id = String.format("PT%04d", max + 1);
            }
            return id;
        } catch (RuntimeException e) {
            return String.format("PT%04d", (int) (Math.random() * 9000) + 1000);
        }
    }

    private PointTransaction findTransaction(String transactionId) {
        for (int i = 0; i < memberList.size(); i++) {
            LinkedListInterface<PointTransaction> tlist = memberList.get(i).getPointTransactionList();
            for (int j = 0; j < tlist.size(); j++) {
                if (tlist.get(j).getTransactionId().equals(transactionId)) {
                    return tlist.get(j);
                }
            }
        }
        return null;
    }

    private String nextRedemptionId() {
        try {
            int max = 0;
            for (int i = 0; i < memberList.size(); i++) {
                LinkedListInterface<RedemptionRecord> rlist = memberList.get(i).getRedemptionRecordList();
                for (int j = 0; j < rlist.size(); j++) {
                    String rid = rlist.get(j).getRedemptionId();
                    if (rid != null && rid.matches("RR\\d+")) {
                        int n = Integer.parseInt(rid.substring(2));
                        if (n > max) {
                            max = n;
                        }
                    }
                }
            }
            return String.format("RR%04d", max + 1);
        } catch (RuntimeException e) {
            return String.format("RR%04d", (int) (Math.random() * 9000) + 1000);
        }
    }

    private static final int ALERT_DAYS_BEFORE_EXPIRY = 7;

    public String generateExpiryAlerts(LocalDateTime now) {
        int created = 0;
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < memberList.size(); i++) {
            Member owner = memberList.get(i);
            if (owner.isDeleted()) {
                continue; // no expiry alerts for soft-deleted members
            }
            LinkedListInterface<PointTransaction> tlist = owner.getPointTransactionList();
            for (int j = 0; j < tlist.size(); j++) {
            PointTransaction t = tlist.get(j);
            if (t.isExpired(now) || t.getRemainingPoints() <= 0) {
                continue;
            }
            boolean expiringSoon = !now.isBefore(t.getExpiryDate().minusDays(ALERT_DAYS_BEFORE_EXPIRY))
                    && now.isBefore(t.getExpiryDate());
            if (!expiringSoon) {
                continue;
            }
            Member member = findMember(t.getMemberId());
            if (member == null || member.getGuestId() == null) {
                continue;
            }
            String message = "Your " + t.getRemainingPoints() + " pts (tx " + t.getTransactionId()
                    + ") will expire on " + t.getExpiryDate().toLocalDate() + ".";
            if (hasNotification(member.getGuestId(), message)) {
                continue; // already alerted
            }
            notifyMember(member, "POINT_EXPIRY", message, now);
            created++;
            summary.append("  - ").append(message).append("\n");
            }
        }
        if (created > 0) {
            return created + " expiry alert(s) generated:\n" + summary;
        }
        return "No new expiry alerts to generate.";
    }

    private boolean hasNotification(String guestId, String message) {
        Guest guest = findGuest(guestId);
        if (guest == null) {
            return false;
        }
        LinkedListInterface<Notification> list = guest.getNotificationList();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getMessage().equals(message)) {
                return true;
            }
        }
        return false;
    }

    public LinkedListInterface<Notification> getNotifications(String guestId) {
        Guest guest = findGuest(guestId);
        if (guest == null) {
            return new LinkedList<>();
        }
        LinkedListInterface<Notification> result = new LinkedList<>();
        LinkedListInterface<Notification> list = guest.getNotificationList();
        for (int i = 0; i < list.size(); i++) {
            result.addBack(list.get(i));
        }
        return result;
    }

    public String markNotificationRead(String notificationId) {
        for (int i = 0; i < guestList.size(); i++) {
            LinkedListInterface<Notification> list = guestList.get(i).getNotificationList();
            for (int j = 0; j < list.size(); j++) {
                Notification n = list.get(j);
                if (n.getNotificationId().equals(notificationId)) {
                    n.setRead(true);
                    guestDAO.saveToFile(guestList);
                    return "Notification " + notificationId + " marked as read.";
                }
            }
        }
        return "Notification not found: " + notificationId;
    }

    private String nextNotificationId() {
        try {
            int max = 0;
            for (int i = 0; i < guestList.size(); i++) {
                LinkedListInterface<Notification> list = guestList.get(i).getNotificationList();
                for (int j = 0; j < list.size(); j++) {
                    String nid = list.get(j).getNotificationId();
                    if (nid != null && nid.matches("NT\\d+")) {
                        int n = Integer.parseInt(nid.substring(2));
                        if (n > max) {
                            max = n;
                        }
                    }
                }
            }
            return String.format("NT%04d", max + 1);
        } catch (RuntimeException e) {
            return String.format("NT%04d", 1);
        }
    }

    public Guest findGuest(String guestId) {
        for (int i = 0; i < guestList.size(); i++) {
            if (guestList.get(i).getGuestId().equals(guestId)) {
                return guestList.get(i);
            }
        }
        return null;
    }

    private static final int[] TIER_THRESHOLDS = {0, 1000, 3000, 6000};

    private Tier tierFor(int cumulativeEarned) {
        if (cumulativeEarned >= TIER_THRESHOLDS[3]) {
            return Tier.DIAMOND;
        }
        if (cumulativeEarned >= TIER_THRESHOLDS[2]) {
            return Tier.PLATINUM;
        }
        if (cumulativeEarned >= TIER_THRESHOLDS[1]) {
            return Tier.GOLD;
        }
        return Tier.SILVER;
    }

    public int getCumulativeEarned(String memberId) {
        int total = 0;
        LinkedListInterface<PointTransaction> txs = getTransactions(memberId);
        for (int i = 0; i < txs.size(); i++) {
            int change = txs.get(i).getPointChange();
            if (change > 0) {
                total += change;
            }
        }
        return total;
    }

    public boolean recomputeTier(Member member, LocalDateTime now) {
        Tier current = tierFor(getCumulativeEarned(member.getMemberId()));
        if (member.getTier() != current) {
            member.setTier(current);
            if (member.getGuestId() != null) {
                String message = "Congratulations! You have been upgraded to " + current
                        + "! You now enjoy " + current.getDiscountPercent() + "% off stays & dining.";
                notifyMember(member, "TIER_UPGRADE", message, now);
            }
            return true;
        }
        return false;
    }

    public String getTierProgress(String memberId) {
        Member member = findMember(memberId);
        if (member == null) {
            return "Member not found: " + memberId;
        }
        int cumulative = getCumulativeEarned(memberId);
        Tier current = tierFor(cumulative);
        int currentIdx = current.ordinal();
        String line = "Member " + memberId + " - Tier: " + current
                + " (cumulative earned " + cumulative + " pts)";
        if (currentIdx < TIER_THRESHOLDS.length - 1) {
            int nextThreshold = TIER_THRESHOLDS[currentIdx + 1];
            int needed = nextThreshold - cumulative;
            line += "\n  Earn " + needed + " more pts to reach " + Tier.values()[currentIdx + 1]
                    + " (needs " + nextThreshold + " cumulative pts).";
        } else {
            line += "\n  You have reached the highest tier!";
        }
        return line;
    }

    private void notifyMember(Member member, String type, String message, LocalDateTime now) {
        if (member == null || member.getGuestId() == null) {
            return;
        }
        Guest guest = findGuest(member.getGuestId());
        if (guest == null) {
            return;
        }
        guest.addNotification(new Notification(nextNotificationId(), type, message, now, false));
        guestDAO.saveToFile(guestList);
    }
}