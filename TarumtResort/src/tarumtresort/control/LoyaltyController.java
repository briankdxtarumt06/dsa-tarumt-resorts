package tarumtresort.control;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.Random;
import java.util.Scanner;
import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;
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
import tarumtresort.entity.enums.NotificationType;
import tarumtresort.entity.enums.RoomType;
import tarumtresort.entity.enums.Tier;
import tarumtresort.report.LoyaltyReport.LoyaltyReportController;
import tarumtresort.utility.ConsoleUtil;

// Author: Imam Mahdi Ali Ang Attuko
public class LoyaltyController {

    private static final int PAGE_SIZE = 20;

    // ADT declaration
    private ListInterface<Member> memberList = new DoublyLinkedList<>();
    private ListInterface<Reward> rewardList = new DoublyLinkedList<>();
    private ListInterface<Guest> guestList = new DoublyLinkedList<>();

    // DAO declaration
    private MemberDAO memberDAO = new MemberDAO();
    private RewardDAO rewardDAO = new RewardDAO();
    private GuestDAO guestDAO = new GuestDAO();

    // UI declaration
    private LoyaltyRewardsUI moduleUI;

    private static final DateTimeFormatter NOTIF_DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public LoyaltyController() {
        this(new Scanner(System.in));
    }

    public LoyaltyController(Scanner scanner) {
        memberList = memberDAO.retrieveFromFile();
        rewardList = rewardDAO.retrieveFromFile();
        guestDAO.loadFromFile(guestList);
        moduleUI = new LoyaltyRewardsUI(scanner);
        reconcileTiersOnLoad();
    }

    public LoyaltyController(LoyaltyRewardsUI ui) {
        this(ui.getScanner());
    }

    // =============== module entry ===============

    public void runLoyaltyRewards() {
        run();
    }

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
                        System.out.println("\n ?! Invalid choice! Please try again.");
                }
            } while (choice != 0);
        } catch (Exception e) {
            ConsoleUtil.printError("An unexpected error occurred in Loyalty & Rewards module: " + e.getMessage());
        }
    }

    // =============== member menu flows ===============

    public void runMemberMenu() {
        String tierFilter = null;
        int page = 0;

        while (true) {
            ListInterface<Member> display;
            if (tierFilter != null) {
                display = getMembersByTier(tierFilter);
            } else {
                display = getActiveMembers();
            }

            boolean hasFilter = tierFilter != null;
            boolean hasDeleted = getDeletedMembers().size() > 0;  // adt method
            int pageCount = Math.max(1, (display.size() + PAGE_SIZE - 1) / PAGE_SIZE);  // adt method
            if (page >= pageCount) {
                page = pageCount - 1;
            }

            ListInterface<Member> pageList = pageOf(display, page);
            int choice = moduleUI.printMemberListMenu(pageList, page, pageCount, hasFilter,
                    hasDeleted, this::findGuest);

            if (choice == 0) {
                break;
            }

            int action = 1;
            if (choice == action++) {
                viewMember(pageList);
            } else if (choice == action++) { 
                String tier = moduleUI.inputTierFilter();
                if (tier != null) {
                    tierFilter = tier;
                    page = 0;
                }
            } else if (hasDeleted && choice == action++) {
                restoreDeletedMemberFlow();
            } else {
                boolean matched = false;
                if (page < pageCount - 1) {
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page++;
                    }
                }
                if (!matched && page > 0) { 
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page--;
                    }
                }
                if (!matched && hasFilter) { 
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

    private void restoreDeletedMemberFlow() {
        int page = 0;
        while (true) {
            ListInterface<Member> deleted = getDeletedMembers();
            int pageCount = Math.max(1, (deleted.size() + PAGE_SIZE - 1) / PAGE_SIZE);  // adt method
            if (page >= pageCount) {
                page = pageCount - 1;
            }
            ListInterface<Member> pageList = pageOf(deleted, page);
            int choice = moduleUI.printDeletedMembersMenu(pageList, page, pageCount, this::findGuest);
            if (choice == 0) {
                return;
            }
            int action = 1;
            if (choice == action++) {
                int index = moduleUI.inputListIndex("member", pageList.size());  // adt method
                if (index == 0) {
                    continue;
                }
                Member member = pageList.get(index - 1);  // adt method
                if (member != null && moduleUI.confirm("Restore member " + member.getMemberId() + "?")) {
                    moduleUI.showMessage(restoreMember(member.getMemberId()));
                }
                return;
            } else {
                boolean matched = false;
                if (page < pageCount - 1) {
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page++;
                    }
                }
                if (!matched && page > 0) {
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page--;
                    }
                }
            }
        }
    }

    private void viewMember(ListInterface<Member> pageList) {
        if (pageList.isEmpty()) {  // adt method
            moduleUI.showMessage("(No member records)");
            return;
        }
        int num = moduleUI.inputListIndex("member", pageList.size());  // adt method
        if (num == 0) {
            return;
        }
        Member member = pageList.get(num - 1);  // adt method
        if (member != null) {
            handleMemberActions(member);
        }
    }

    private void handleMemberActions(Member member) {
        while (true) {
            moduleUI.displayProfile(member, findGuest(member.getGuestId()));

            int action = moduleUI.getMemberActionChoice();
            if (action == 0) {
                return;
            }

            switch (action) {
                case 1: 
                    moduleUI.show("Current tier: " + member.getTier());
                    Tier tier = moduleUI.selectTier();
                    if (tier != null) {
                        moduleUI.showMessage(updateMember(member.getMemberId(), tier));
                    }
                    break;
                case 2:
                    if (moduleUI.confirm("Remove member " + member.getMemberId()
                            + "? (soft delete - can be restored later)")) {
                        moduleUI.showMessage(removeMember(member.getMemberId()));
                    }
                    return;
                default:
                    break;
            }

            member = findMember(member.getMemberId());
            if (member == null) {
                return;
            }
        }
    }

    // =============== member queries ===============

    public ListInterface<Member> getMembersByTier(String tier) {
        ListInterface<Member> filteredList = new DoublyLinkedList<>();
        for (int i = 0; i < memberList.size(); i++) {  // adt method
            Member member = memberList.get(i);  // adt method 
            if (!member.isDeleted() && member.getTier() != null && member.getTier().name().equalsIgnoreCase(tier)) {
                filteredList.addBack(member); // adt method
            }
        }
        return filteredList;
    }

    // the rows of one page (PAGE_SIZE at most), starting at page * PAGE_SIZE
    private <T extends Comparable<T>> DoublyLinkedList<T> pageOf(ListInterface<T> list, int page) {
        DoublyLinkedList<T> result = new DoublyLinkedList<>();
        int start = page * PAGE_SIZE;
        int end = Math.min(list.size(), start + PAGE_SIZE);  // adt method
        for (int i = start; i < end; i++) {
            result.addBack(list.get(i)); // adt method
        }
        return result;
    }

    // =============== reward menu flows ===============

    public void runRewardMenu() {
        Tier tierFilter = null;
        int sortMode = 0;
        int page = 0;

        while (true) {
            ListInterface<Reward> display;
            if (tierFilter != null) {
                display = getRewardsByMinTier(tierFilter);
            } else {
                display = getActiveRewards();
            }
            display = sortedRewardView(display, sortMode);

            boolean hasFilter = tierFilter != null;
            boolean hasDeleted = getDeletedRewards().size() > 0;  // adt method
            int pageCount = Math.max(1, (display.size() + PAGE_SIZE - 1) / PAGE_SIZE);  // adt method
            if (page >= pageCount) {
                page = pageCount - 1;
            }

            ListInterface<Reward> pageList = pageOf(display, page);
            int choice = moduleUI.printRewardListMenu(pageList, page, pageCount, hasFilter,
                    hasDeleted, sortLabelOf(sortMode));

            if (choice == 0) {
                break;
            }

            int action = 1;
            if (choice == action++) { 
                viewReward(pageList);
            } else if (choice == action++) { 
                addRewardFlow();
            } else if (choice == action++) { 
                Tier tier = moduleUI.inputMinTierFilter();
                if (tier != null) {
                    tierFilter = tier;
                    page = 0;
                }
            } else if (choice == action++) {
                sortMode = (sortMode + 1) % 3;
                page = 0;
            } else if (hasDeleted && choice == action++) {
                restoreDeletedRewardFlow();
            } else {
                boolean matched = false;
                if (page < pageCount - 1) {
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page++;
                    }
                }
                if (!matched && page > 0) {
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page--;
                    }
                }
                if (!matched && hasFilter) {
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

    private void restoreDeletedRewardFlow() {
        int page = 0;
        while (true) {
            ListInterface<Reward> deleted = getDeletedRewards();
            int pageCount = Math.max(1, (deleted.size() + PAGE_SIZE - 1) / PAGE_SIZE);  // adt method
            if (page >= pageCount) {
                page = pageCount - 1;
            }
            ListInterface<Reward> pageList = pageOf(deleted, page);
            int choice = moduleUI.printDeletedRewardsMenu(pageList, page, pageCount);
            if (choice == 0) {
                return;
            }
            int action = 1;
            if (choice == action++) {
                int index = moduleUI.inputListIndex("reward", pageList.size());  // adt method
                if (index == 0) {
                    continue;
                }
                Reward reward = pageList.get(index - 1); // adt method
                if (reward != null && moduleUI.confirm("Restore reward " + reward.getRewardId()
                        + " (" + reward.getName() + ")?")) {
                    moduleUI.showMessage(restoreReward(reward.getRewardId()));
                }
                return;
            } else {
                boolean matched = false;
                if (page < pageCount - 1) {
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page++;
                    }
                }
                if (!matched && page > 0) {
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page--;
                    }
                }
            }
        }
    }

    private ListInterface<Reward> sortedRewardView(ListInterface<Reward> source, int sortMode) {
        if (sortMode == 2) {
            ListInterface<Reward> result = new DoublyLinkedList<>();
            for (int i = source.size() - 1; i >= 0; i--) {  // adt method
                result.addBack(source.get(i)); // adt method
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

    private void viewReward(ListInterface<Reward> pageList) {
        if (pageList.isEmpty()) { // adt method
            moduleUI.showMessage("(No rewards in the catalogue)");
            return;
        }
        int num = moduleUI.inputListIndex("reward", pageList.size());  // adt method
        if (num == 0) {
            return;
        }
        Reward reward = pageList.get(num - 1); // adt method
        if (reward != null) {
            handleRewardActions(reward);
        }
    }

    private void handleRewardActions(Reward reward) {
        while (true) {
            moduleUI.displayRewardDetails(reward);

            int action = moduleUI.getRewardActionChoice();
            if (action == 0) {
                return;
            }

            switch (action) {
                case 1:
                    updateRewardPrompt(reward);
                    break;
                case 2:
                    if (moduleUI.confirm("Remove reward " + reward.getRewardId()
                            + " (" + reward.getName() + ")? (soft delete - can be restored later)")) {
                        moduleUI.showMessage(removeReward(reward.getRewardId()));
                    }
                    return;
                default:
                    break;
            }

            reward = findReward(reward.getRewardId());
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

    public ListInterface<Reward> getRewardsByName(String keyword) {
        ListInterface<Reward> filteredList = new DoublyLinkedList<>();
        for (int i = 0; i < rewardList.size(); i++) {  // adt method
            Reward reward = rewardList.get(i); // adt method
            if (!reward.isDeleted() && reward.getName() != null
                    && reward.getName().toLowerCase().contains(keyword.toLowerCase())) {
                filteredList.addBack(reward); // adt method
            }
        }
        return filteredList;
    }

    public ListInterface<Reward> getRewardsByMinTier(Tier tier) {
        ListInterface<Reward> filteredList = new DoublyLinkedList<>();
        for (int i = 0; i < rewardList.size(); i++) {  // adt method
            Reward reward = rewardList.get(i); // adt method
            if (reward.isDeleted()) {
                continue;
            }
            Tier minTier = reward.getMinTier() == null ? Tier.SILVER : reward.getMinTier();
            if (minTier == tier) {
                filteredList.addBack(reward); // adt method
            }
        }
        return filteredList;
    }

    public ListInterface<Reward> getRewardsEligibleFor(Tier tier) {
        ListInterface<Reward> eligible = new DoublyLinkedList<>();
        for (int i = 0; i < rewardList.size(); i++) {  // adt method
            Reward reward = rewardList.get(i); // adt method
            if (reward.isDeleted()) {
                continue;
            }
            Tier minTier = reward.getMinTier() == null ? Tier.SILVER : reward.getMinTier();
            if (tier != null && tier.ordinal() >= minTier.ordinal()) {
                eligible.addBack(reward); // adt method
            }
        }
        return eligible;
    }

    // =============== points menu flows ===============

    public void runPointsMenu() {
        String alert = generateExpiryAlerts(LocalDateTime.now());
        if (!alert.startsWith("No new")) {
            moduleUI.show(alert);
        }
        String tierFilter = null;
        int page = 0;

        while (true) {
            ListInterface<Member> display;
            if (tierFilter != null) {
                display = getMembersByTier(tierFilter);
            } else {
                display = getActiveMembers();
            }

            boolean hasFilter = tierFilter != null;
            int pageCount = Math.max(1, (display.size() + PAGE_SIZE - 1) / PAGE_SIZE); // adt method
            if (page >= pageCount) {
                page = pageCount - 1;
            }

            ListInterface<Member> pageList = pageOf(display, page);
            int choice = moduleUI.printPointsListMenu(pageList, page, pageCount, hasFilter,
                    this::findGuest, m -> getAvailableBalance(m.getMemberId(), LocalDateTime.now()));

            if (choice == 0) {
                break;
            }

            int action = 1;
            if (choice == action++) { 
                viewMemberPoints(pageList);
            } else if (choice == action++) {
                earnPointsFlow(pageList);
            } else if (choice == action++) {
                requestRedemptionFlow(pageList);
            } else if (choice == action++) {
                processRedemptionRequestsFlow();
            } else if (choice == action++) {
                moduleUI.showMessage(generateExpiryAlerts(LocalDateTime.now()));
            } else if (choice == action++) {
                String tier = moduleUI.inputTierFilter();
                if (tier != null) {
                    tierFilter = tier;
                    page = 0;
                }
            } else {
                boolean matched = false;
                if (page < pageCount - 1) {
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page++;
                    }
                }
                if (!matched && page > 0) {
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page--;
                    }
                }
                if (!matched && hasFilter) {
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
    private void viewMemberPoints(ListInterface<Member> pageList) {
        String memberId = pickMemberFromPage(pageList);
        if (memberId == null) {
            return;
        }
        Member member = findMember(memberId);
        if (member != null) {
            handleMemberPointsActions(member);
        }
    }

    private void handleMemberPointsActions(Member member) {
        while (true) {
            moduleUI.displayBalance(member, getAvailableBalance(member.getMemberId(), LocalDateTime.now()));

            int action = moduleUI.getMemberPointsActionChoice();
            if (action == 0) {
                return;
            }

            switch (action) {
                case 1:
                    moduleUI.showMessage(expirePoints(member.getMemberId(), LocalDateTime.now()));
                    break;
                case 2: 
                    moduleUI.displayTransactions(getTransactions(member.getMemberId()));
                    moduleUI.pause();
                    break;
                case 3:     
                    moduleUI.showMessage(getTierProgress(member.getMemberId()));
                    break;
                case 4:
                    viewMemberNotifications(member);
                    break;
                default:
                    break;
            }

            member = findMember(member.getMemberId());
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
        ListInterface<Notification> list = getNotifications(member.getGuestId());
        moduleUI.displayNotifications(list);
        if (moduleUI.confirmMarkAllRead()) {
            for (int i = 0; i < list.size(); i++) { // adt method
                markNotificationRead(list.get(i).getNotificationId()); // adt method
            }
            moduleUI.show("All notifications marked as read.");
        }
        moduleUI.pause();
    }

    // =============== reports ===============

    private void runReports() {
        LoyaltyReportController reportController = new LoyaltyReportController(moduleUI.getScanner());
        while (true) {
            int choice = moduleUI.getReportMenuChoice();
            if (choice == 0) {
                return;
            }
            switch (choice) {
                case 1:
                    reportController.generateMembershipPerformanceReport();
                    break;
                case 2:
                    reportController.generateRedemptionVoucherReport();
                    break;
                default:
                    break;
            }
        }
    }

    // =============== notifications - counts & marking ===============

    public int getUnreadNotificationCount() {
        int count = 0;
        for (int i = 0; i < guestList.size(); i++) { // adt method
            ListInterface<Notification> list = guestList.get(i).getNotificationList();  // adt method
            for (int j = 0; j < list.size(); j++) { // adt method
                Notification n = list.get(j); // adt method
                if (!n.isRead() && !n.isDeleted()) {
                    count++;
                }
            }
        }
        return count;
    }

    public int markAllNotificationsRead() {
        int count = 0;
        for (int i = 0; i < guestList.size(); i++) { // adt method
            ListInterface<Notification> list = guestList.get(i).getNotificationList();  // adt method
            for (int j = 0; j < list.size(); j++) { // adt method
                Notification n = list.get(j); // adt method
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

    public int getUnreadNotificationCount(String memberId) {
        Member member = findMember(memberId);
        if (member == null || member.getGuestId() == null) {
            return 0;
        }
        int count = 0;
        ListInterface<Notification> list = getNotifications(member.getGuestId());
        for (int i = 0; i < list.size(); i++) { // adt method
            if (!list.get(i).isRead()) { // adt method
                count++;
            }
        }
        return count;
    }

    public int getUnreadNotificationCountByGuest(String guestId) {
        if (guestId == null) {
            return 0;
        }
        Guest guest = findGuest(guestId);
        if (guest == null) {
            return 0;
        }
        int count = 0;
        ListInterface<Notification> list = guest.getNotificationList();
        for (int i = 0; i < list.size(); i++) { // adt method
            Notification n = list.get(i); // adt method
            if (!n.isRead() && !n.isDeleted()) {
                count++;
            }
        }
        return count;
    }

    public int markGuestNotificationsRead(String guestId) {
        Guest guest = findGuest(guestId);
        if (guest == null) {
            return 0;
        }
        int count = 0;
        ListInterface<Notification> list = guest.getNotificationList();
        for (int i = 0; i < list.size(); i++) { // adt method
            Notification n = list.get(i); // adt method
            if (!n.isRead() && !n.isDeleted()) {
                n.setRead(true);
                count++;
            }
        }
        if (count > 0) {
            guestDAO.saveToFile(guestList);
        }
        return count;
    }

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
        ListInterface<Notification> list = guest.getNotificationList();
        for (int j = 0; j < list.size(); j++) { // adt method
            Notification n = list.get(j); // adt method
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

    // =============== notification centre screens ===============

    private void runNotificationCentre() {
        int memberPage = 0;
        while (true) {
            ListInterface<Member> display = getActiveMembers();
            int pageCount = Math.max(1, (display.size() + PAGE_SIZE - 1) / PAGE_SIZE); // adt method
            if (memberPage >= pageCount) {
                memberPage = pageCount - 1;
            }
            ListInterface<Member> pageList = pageOf(display, memberPage);

            String[][] memberRows = new String[pageList.size()][]; // adt method
            for (int i = 0; i < pageList.size(); i++) { // adt method
                Member m = pageList.get(i); // adt method
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
            if (choice == action++) {
                int index = moduleUI.inputListIndex("member number", pageList.size()); // adt method
                if (index == 0) {
                    continue;
                }
                viewMemberNotificationsCentre(pageList.get(index - 1)); // adt method
            } else if (choice == action++) {
                int marked = markAllNotificationsRead();
                moduleUI.showMessage(marked + " notification(s) marked as read.");
                moduleUI.pause();
                memberPage = 0;
            } else {
                boolean matched = false;
                if (memberPage < pageCount - 1) {
                    matched = choice == action;
                    action++;
                    if (matched) {
                        memberPage++;
                    }
                }
                if (!matched && memberPage > 0) { 
                    matched = choice == action;
                    action++;
                    if (matched) {
                        memberPage--;
                    }
                }
            }
        }
    }

    private void viewMemberNotificationsCentre(Member member) {
        if (member.getGuestId() == null) {
            moduleUI.showMessage("Member has no guest account linked.");
            moduleUI.pause();
            return;
        }
        int page = 0;
        while (true) {
            ListInterface<Notification> list = getNotifications(member.getGuestId());
            int pageCount = Math.max(1, (list.size() + PAGE_SIZE - 1) / PAGE_SIZE); // adt method
            if (page >= pageCount) {
                page = pageCount - 1;
            }
            int from = page * PAGE_SIZE;
            String[][] rows = new String[Math.min(PAGE_SIZE, list.size() - from)][]; // adt method
            for (int k = 0; k < rows.length; k++) {
                Notification n = list.get(from + k); // adt method
                rows[k] = new String[] {
                    String.valueOf(k + 1), n.getType() == null ? "-" : n.getType().name(),
                    truncate(n.getMessage(), 48),
                    n.getDate() == null ? "-" : n.getDate().format(NOTIF_DATE_FMT),
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
            if (choice == action++) {
                int marked = markMemberNotificationsRead(member.getMemberId());
                moduleUI.showMessage(marked + " notification(s) marked as read.");
                moduleUI.pause();
                page = 0;
            } else if (choice == action++) {
                if (list.isEmpty()) { // adt method
                    moduleUI.showMessage("(No notifications to delete)");
                    moduleUI.pause();
                } else {
                    int num = moduleUI.inputListIndex("notification", "delete", list.size()); // adt method
                    if (num != 0) {
                        Notification target = list.get(num - 1); // adt method
                        if (moduleUI.confirm("Delete notification "
                                + target.getNotificationId() + "? (y/n): ")) {
                            moduleUI.showMessage(deleteNotification(target.getNotificationId()));
                            int newPageCount = Math.max(1,
                                    (getNotifications(member.getGuestId()).size() + PAGE_SIZE - 1) / PAGE_SIZE); // adt method
                            if (page >= newPageCount) {
                                page = newPageCount - 1;
                            }
                        }
                        moduleUI.pause();
                    }
                }
            } else {
                boolean matched = false;
                if (page < pageCount - 1) { 
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page++;
                    }
                }
                if (!matched && page > 0) {
                    matched = choice == action;
                    action++;
                    if (matched) {
                        page--;
                    }
                }
            }
        }
    }

    // =============== shared ui helpers ===============

    private static String truncate(String text, int max) {
        if (text == null || text.length() <= max) {
            return text == null ? "" : text;
        }
        return text.substring(0, max - 3) + "...";
    }

    private String pickMemberFromPage(ListInterface<Member> pageList) {
        if (pageList.isEmpty()) { // adt method
            moduleUI.showMessage("(No member records)");
            return null;
        }
        int num = moduleUI.inputListIndex("member", pageList.size()); // adt method
        if (num == 0) {
            return null;
        }
        Member member = pageList.get(num - 1); // adt method
        return member == null ? null : member.getMemberId();
    }

    // =============== member management ===============

    public String registerMember(Guest guest) {
        if (guest == null || guest.getGuestId() == null) {
            return "Guest cannot be null and must have an id.";
        }
        Member existing = findMemberByGuestId(guest.getGuestId());
        if (existing != null) {
            return "This guest is already registered as member: " + existing.getMemberId() + ".";
        }
        Member member = new Member(nextMemberId(), 0, Tier.SILVER, LocalDateTime.now(), guest.getGuestId());
        memberList.addSorted(member);  // adt method
        persistMembers();
        return "Guest " + guest.getName() + " registered as member " + member.getMemberId()
                + " (Tier: " + member.getTier() + ").";
    }

    public Member findMemberByGuestId(String guestId) {
        for (int i = 0; i < memberList.size(); i++) { // adt method
            Member m = memberList.get(i);  // adt method
            if (m.getGuestId() != null && m.getGuestId().equals(guestId) && !m.isDeleted()) {
                return m;
            }
        }
        return null;
    }

    public ListInterface<Member> getMembers() {
        return memberList;
    }

    public Member findMember(String memberId) {
        for (int i = 0; i < memberList.size(); i++) { // adt method
            if (memberList.get(i).getMemberId().equals(memberId)) {  // adt method
                return memberList.get(i);  // adt method
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

        int rejected = 0;
        ListInterface<RedemptionRecord> records = member.getRedemptionRecordList();
        for (int i = 0; i < records.size(); i++) { // adt method
            RedemptionRecord r = records.get(i); // adt method
            if (r != null && "PENDING".equals(r.getStatus())) {
                r.setStatus("REJECTED");
                rejected++;
            }
        }
        persistMembers();
        return "Member " + memberId + " removed (soft delete). " + rejected
                + " pending redemption request(s) rejected.";
    }

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

    public ListInterface<Member> getActiveMembers() {
        ListInterface<Member> active = new DoublyLinkedList<>();
        for (int i = 0; i < memberList.size(); i++) { // adt method
            Member m = memberList.get(i);  // adt method
            if (!m.isDeleted()) { // adt method
                active.addBack(m); // adt method
            }
        }
        return active;
    }

    public ListInterface<Member> getDeletedMembers() {
        ListInterface<Member> deleted = new DoublyLinkedList<>();
        for (int i = 0; i < memberList.size(); i++) { // adt method
            Member m = memberList.get(i);  // adt method
            if (m.isDeleted()) {
                deleted.addBack(m); // adt method
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
            for (int i = 0; i < memberList.size(); i++) { // adt method
                String mid = memberList.get(i).getMemberId();  // adt method
                if (mid != null && mid.matches("M\\d+")) {
                    int n = Integer.parseInt(mid.substring(1));
                    if (n > max) {
                        max = n;
                    }
                }
            }
            return String.format("M%03d", max + 1);
        } catch (RuntimeException e) {
            return String.format("M%03d", memberList.size() + 1); // adt method
        }
    }

    // =============== reward management ===============

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

    public ListInterface<Reward> getRewards() {
        return rewardList;
    }

    public Reward findReward(String rewardId) {
        for (int i = 0; i < rewardList.size(); i++) { // adt method
            if (rewardList.get(i).getRewardId().equals(rewardId)) { // adt method
                return rewardList.get(i); // adt method
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
        rewardList.addSorted(reward); // adt method
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

    public ListInterface<Reward> getActiveRewards() {
        ListInterface<Reward> active = new DoublyLinkedList<>();
        for (int i = 0; i < rewardList.size(); i++) { // adt method
            Reward r = rewardList.get(i); // adt method
            if (!r.isDeleted()) {
                active.addBack(r); // adt method
            }
        }
        return active;
    }

    public ListInterface<Reward> getDeletedRewards() {
        ListInterface<Reward> deleted = new DoublyLinkedList<>();
        for (int i = 0; i < rewardList.size(); i++) { // adt method
            Reward r = rewardList.get(i); // adt method
            if (r.isDeleted()) {
                deleted.addBack(r); // adt method
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
        ListInterface<Reward> reordered = new DoublyLinkedList<>();
        for (int i = 0; i < rewardList.size(); i++) { // adt method
            reordered.addSorted(rewardList.get(i)); // adt method
        }
        rewardList.clear(); // adt method
        for (int i = 0; i < reordered.size(); i++) { // adt method
            rewardList.addBack(reordered.get(i)); // adt method
        }
        persistRewards();
        return "Reward updated: " + reward.getName() + " (" + reward.getPointCost() + " pts).";
    }

    public String nextRewardId() {
        try {
            int max = 0;
            for (int i = 0; i < rewardList.size(); i++) { // adt method
                String rid = rewardList.get(i).getRewardId(); // adt method
                if (rid != null && rid.matches("R\\d+")) {
                    int n = Integer.parseInt(rid.substring(1));
                    if (n > max) {
                        max = n;
                    }
                }
            }
            return String.format("R%03d", max + 1);
        } catch (RuntimeException e) {
            return String.format("R%03d", rewardList.size() + 1); // adt method
        }
    }

    // =============== tier reconciliation ===============

    private void reconcileTiersOnLoad() {
        boolean changed = false;
        for (int i = 0; i < memberList.size(); i++) { // adt method
            Member m = memberList.get(i);  // adt method
            if (m.isDeleted()) {
                continue; 
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

    private void earnPointsFlow(ListInterface<Member> pageList) {
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

    private void requestRedemptionFlow(ListInterface<Member> pageList) {
        String memberId = pickMemberFromPage(pageList);
        if (memberId == null) {
            return;
        }
        Member member = findMember(memberId);
        if (member == null) {
            return;
        }
        ListInterface<Reward> eligible = getRewardsEligibleFor(member.getTier());
        String rewardId = moduleUI.selectReward(eligible);
        if (rewardId == null) {
            return;
        }
        moduleUI.showMessage(requestRedemption(memberId, rewardId, LocalDateTime.now()));
    }

    private void processRedemptionRequestsFlow() {
        ListInterface<RedemptionRecord> pending = getPendingRedemptions();
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

    // =============== transaction & redemption queries ===============

    public ListInterface<PointTransaction> getPointTransactions() {
        ListInterface<PointTransaction> result = new DoublyLinkedList<>();
        for (int i = 0; i < memberList.size(); i++) { // adt method
            ListInterface<PointTransaction> list = memberList.get(i).getPointTransactionList();  // adt method
            for (int j = 0; j < list.size(); j++) { // adt method
                result.addBack(list.get(j)); // adt method
            }
        }
        return result;
    }

    public ListInterface<RedemptionRecord> getRedemptions() {
        ListInterface<RedemptionRecord> result = new DoublyLinkedList<>();
        for (int i = 0; i < memberList.size(); i++) { // adt method
            ListInterface<RedemptionRecord> list = memberList.get(i).getRedemptionRecordList();  // adt method
            for (int j = 0; j < list.size(); j++) { // adt method
                result.addBack(list.get(j)); // adt method
            }
        }
        return result;
    }

    public ListInterface<PointTransaction> getTransactions(String memberId) {
        ListInterface<PointTransaction> result = new DoublyLinkedList<>();
        Member member = findMember(memberId);
        if (member == null) {
            return result;
        }
        ListInterface<PointTransaction> list = member.getPointTransactionList();
        for (int i = 0; i < list.size(); i++) { // adt method
            result.addBack(list.get(i)); // adt method
        }
        return result;
    }

    // =============== points & expiry ===============

    public String expirePoints(String memberId, LocalDateTime now) {
        Member member = findMember(memberId);
        if (member == null) {
            return "Member not found: " + memberId;
        }

        int totalExpired = 0;
        StringBuilder report = new StringBuilder();
        ListInterface<PointTransaction> txs = getTransactions(memberId);
        for (int i = 0; i < txs.size(); i++) { // adt method
            PointTransaction t = txs.get(i); // adt method
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

    // called from ReservationControl payment - 1 pt per RM1 of the bill paid
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
        member.getPointTransactionList().addSorted(t); // adt method
        recomputeBalance(member);
        recomputeTier(member, date);
        persistMembers();
        return amount + " pts earned by " + memberId + " (expires " + expiry.toLocalDate() + "). New balance: "
                + member.getPoints();
    }

    // refund claw-back: deduct previously earned points when a paid booking is refunded
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
        member.getPointTransactionList().addSorted(t); // adt method
        recomputeBalance(member);
        recomputeTier(member, date);
        persistMembers();
        return deduction + " pts deducted from " + memberId + " (refund claw-back). New balance: "
                + member.getPoints();
    }

    // =============== redemption & vouchers ===============

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
        member.getRedemptionRecordList().addSorted(new RedemptionRecord(nextRedemptionId(), now, memberId, rewardId)); // adt method
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

        // FIFO: consume earliest-expiring lots first (list is sorted by expiry date)
        int remainingCost = cost;
        StringBuilder breakdown = new StringBuilder();
        ListInterface<PointTransaction> txs = getTransactions(member.getMemberId());
        for (int i = 0; i < txs.size() && remainingCost > 0; i++) { // adt method
            PointTransaction t = txs.get(i); // adt method
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
        notifyMember(member, NotificationType.REDEMPTION_APPROVED,
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

    public ListInterface<RedemptionRecord> getAvailableVouchers(String memberId) {
        ListInterface<RedemptionRecord> result = new DoublyLinkedList<>();
        Member member = findMember(memberId);
        if (member == null) {
            return result;
        }
        ListInterface<RedemptionRecord> list = member.getRedemptionRecordList();
        for (int i = 0; i < list.size(); i++) { // adt method
            RedemptionRecord r = list.get(i); // adt method
            if ("APPROVED".equals(r.getStatus())
                    && !r.isUsed()
                    && r.getVoucherCode() != null
                    && ((r.getVoucherValue() != null && r.getVoucherValue() > 0)
                        || (r.getDiscountPercent() != null && r.getDiscountPercent() > 0))) {
                result.addBack(r); // adt method
            }
        }
        return result;
    }

    public String useVoucher(String memberId, String redemptionId) {
        Member member = findMember(memberId);
        if (member == null) {
            return "Member not found: " + memberId;
        }
        ListInterface<RedemptionRecord> list = member.getRedemptionRecordList();
        for (int i = 0; i < list.size(); i++) { // adt method
            RedemptionRecord r = list.get(i); // adt method
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
            notifyMember(member, NotificationType.REDEMPTION_REJECTED,
                    "Your redemption request " + redemptionId + " (" + rewardName + ") has been rejected.", now);
        }
        persistMembers();
        return "Rejected redemption request " + redemptionId + ".";
    }

    public ListInterface<RedemptionRecord> getPendingRedemptions() {
        ListInterface<RedemptionRecord> result = new DoublyLinkedList<>();
        for (int i = 0; i < memberList.size(); i++) { // adt method
            if (memberList.get(i).isDeleted()) {  // adt method
                continue;
            }
            ListInterface<RedemptionRecord> list = memberList.get(i).getRedemptionRecordList();  // adt method
            for (int j = 0; j < list.size(); j++) { // adt method
                if ("PENDING".equals(list.get(j).getStatus())) { // adt method
                    result.addBack(list.get(j)); // adt method
                }
            }
        }
        return result;
    }

    private RedemptionRecord findRedemption(String redemptionId) {
        for (int i = 0; i < memberList.size(); i++) { // adt method
            ListInterface<RedemptionRecord> list = memberList.get(i).getRedemptionRecordList(); // adt method
            for (int j = 0; j < list.size(); j++) { // adt method
                if (list.get(j).getRedemptionId().equals(redemptionId)) { // adt method
                    return list.get(j); // adt method
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
        ListInterface<PointTransaction> txs = getTransactions(member.getMemberId());
        for (int i = 0; i < txs.size(); i++) { // adt method
            sum += txs.get(i).getRemainingPoints(); // adt method
        }
        member.setPoints(sum);
    }

    // =============== persistence ===============

    private void persistMembers() {
        memberDAO.saveToFile(memberList);
    }

    private void persistRewards() {
        rewardDAO.saveToFile(rewardList);
    }

    private String nextTransactionId() {
        try {
            int max = 0;
            for (int i = 0; i < memberList.size(); i++) { // adt method
                ListInterface<PointTransaction> tlist = memberList.get(i).getPointTransactionList(); // adt method
                for (int j = 0; j < tlist.size(); j++) { // adt method
                    String tid = tlist.get(j).getTransactionId(); // adt method
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
        for (int i = 0; i < memberList.size(); i++) { // adt method
            ListInterface<PointTransaction> tlist = memberList.get(i).getPointTransactionList(); // adt method
            for (int j = 0; j < tlist.size(); j++) { // adt method
                if (tlist.get(j).getTransactionId().equals(transactionId)) { // adt method
                    return tlist.get(j); // adt method
                }
            }
        }
        return null;
    }

    private String nextRedemptionId() {
        try {
            int max = 0;
            for (int i = 0; i < memberList.size(); i++) { // adt method
                ListInterface<RedemptionRecord> rlist = memberList.get(i).getRedemptionRecordList(); // adt method
                for (int j = 0; j < rlist.size(); j++) { // adt method
                    String rid = rlist.get(j).getRedemptionId(); // adt method
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

    // =============== expiry alerts ===============

    public String generateExpiryAlerts(LocalDateTime now) {
        int created = 0;
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < memberList.size(); i++) { // adt method
            Member owner = memberList.get(i); // adt method
            if (owner.isDeleted()) {
                continue;
            }
            ListInterface<PointTransaction> tlist = owner.getPointTransactionList();
            for (int j = 0; j < tlist.size(); j++) { // adt method
            PointTransaction t = tlist.get(j); // adt method
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
                continue;
            }
            notifyMember(member, NotificationType.POINT_EXPIRY, message, now);
            created++;
            summary.append("  - ").append(message).append("\n");
            }
        }
        if (created > 0) {
            return created + " expiry alert(s) generated:\n" + summary;
        }
        return "No new expiry alerts to generate.";
    }

    // =============== notification operations ===============

    private boolean hasNotification(String guestId, String message) {
        Guest guest = findGuest(guestId);
        if (guest == null) {
            return false;
        }
        ListInterface<Notification> list = guest.getNotificationList();
        for (int i = 0; i < list.size(); i++) { // adt method
            Notification n = list.get(i); // adt method
            if (!n.isDeleted() && message != null && message.equals(n.getMessage())) {
                return true;
            }
        }
        return false;
    }

    public ListInterface<Notification> getNotifications(String guestId) {
        Guest guest = findGuest(guestId);
        if (guest == null) {
            return new DoublyLinkedList<>();
        }
        ListInterface<Notification> result = new DoublyLinkedList<>();
        ListInterface<Notification> list = guest.getNotificationList();
        for (int i = 0; i < list.size(); i++) { // adt method
            Notification n = list.get(i); // adt method
            if (!n.isDeleted()) {
                result.addBack(n); // adt method
            }
        }
        return result;
    }

    public String markNotificationRead(String notificationId) {
        for (int i = 0; i < guestList.size(); i++) { // adt method
            ListInterface<Notification> list = guestList.get(i).getNotificationList();  // adt method
            for (int j = 0; j < list.size(); j++) { // adt method
                Notification n = list.get(j); // adt method
                if (n.getNotificationId().equals(notificationId)) {
                    n.setRead(true);
                    guestDAO.saveToFile(guestList);
                    return "Notification " + notificationId + " marked as read.";
                }
            }
        }
        return "Notification not found: " + notificationId;
    }

    public String deleteNotification(String notificationId) {
        if (notificationId == null) {
            return "Notification id cannot be null.";
        }
        for (int i = 0; i < guestList.size(); i++) { // adt method
            ListInterface<Notification> list = guestList.get(i).getNotificationList();  // adt method
            for (int j = 0; j < list.size(); j++) { // adt method
                Notification n = list.get(j); // adt method
                if (notificationId.equals(n.getNotificationId())) {
                    if (n.isDeleted()) {
                        return "Notification " + notificationId + " is already deleted.";
                    }
                    n.setDeleted(true);
                    guestDAO.saveToFile(guestList);
                    return "Notification " + notificationId + " deleted.";
                }
            }
        }
        return "Notification not found: " + notificationId;
    }

    private String nextNotificationId() {
        try {
            int max = 0;
            for (int i = 0; i < guestList.size(); i++) { // adt method
                ListInterface<Notification> list = guestList.get(i).getNotificationList();  // adt method
                for (int j = 0; j < list.size(); j++) { // adt method
                    String nid = list.get(j).getNotificationId(); // adt method
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
        for (int i = 0; i < guestList.size(); i++) { // adt method
            if (guestList.get(i).getGuestId().equals(guestId)) { // adt method
                return guestList.get(i);  // adt method
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
        ListInterface<PointTransaction> txs = getTransactions(memberId);
        for (int i = 0; i < txs.size(); i++) { // adt method
            int change = txs.get(i).getPointChange(); // adt method
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
                notifyMember(member, NotificationType.TIER_UPGRADE, message, now);
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

    private void notifyMember(Member member, NotificationType type, String message, LocalDateTime now) {
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
