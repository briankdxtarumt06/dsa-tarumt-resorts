package tarumtresort.control;

import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;
import tarumtresort.boundary.PriorityReservationUI;
import tarumtresort.entity.*;
import tarumtresort.entity.enums.*;

import tarumtresort.dao.*;
import tarumtresort.report.PriorityReservationReport.PriorityReservationReportController;

import java.time.LocalDateTime;
import java.util.Scanner;

public class PriorityReservationController {
    private static final int PAGE_SIZE = 20;

    // ADT declaration
    private ListInterface<PriorityReservation> priorityReservations = new DoublyLinkedList<>();
    private ListInterface<Reservation> vipQueue = new DoublyLinkedList<>();

    // DAO
    private PriorityReservationDAO priorityReservationDAO = new PriorityReservationDAO();
    private ReservationDAO reservationDAO = new ReservationDAO();
    private StaffDAO staffDAO = new StaffDAO();

    // Controllers
    private LoyaltyController loyaltyController;

    // Boundary
    private PriorityReservationUI priorityReservationUI = new PriorityReservationUI();

    // Reports - all report calculation lives in the report package
    private PriorityReservationReportController reportController;

    public PriorityReservationController() {
        this(new Scanner(System.in));
    }

    public PriorityReservationController(Scanner scanner) {
        this.loyaltyController = new LoyaltyController(scanner);
        this.priorityReservationUI = new PriorityReservationUI(scanner);
        this.reportController = new PriorityReservationReportController(scanner);
        this.priorityReservations = priorityReservationDAO.loadFromFile();
    }

    public boolean addPriorityReservation(String reservationId, String guestId) {
        if (searchPriorityReservationById(reservationId) != null) {
            return true; // record already exists (e.g. created at booking, again at arrival)
        }
        Member member = loyaltyController.findMember(guestId);
        if (member == null) {
            return false;
        }
        PriorityLevel priorityLevel = PriorityLevel.convertTierToPriority(member.getTier());
        PriorityReservation priorityReservation = new PriorityReservation(reservationId, priorityLevel);
        priorityReservations.addBack(priorityReservation);
        priorityReservationDAO.saveToFile(priorityReservations);
        return true;
    }

    public boolean removePriorityReservationById(String reservationId) {
        PriorityReservation pr = searchPriorityReservationById(reservationId);
        if (pr != null) {
            pr.setDeleted(true);
            priorityReservationDAO.saveToFile(priorityReservations);
            return true;
        }
        return false;
    }

    public boolean updatePriorityReservation(PriorityReservation updatedReservation) {
        PriorityReservation pr = searchPriorityReservationById(updatedReservation.getReservationId());
        if (pr == null) {
            return false;
        }

        pr.setPriorityLevel(updatedReservation.getPriorityLevel());
        pr.setOverriddenBy(updatedReservation.getOverriddenBy());
        pr.setOverrideReason(updatedReservation.getOverrideReason());
        priorityReservationDAO.saveToFile(priorityReservations);
        return true;
    }

    public PriorityReservation searchPriorityReservationById(String reservationId) {
        for (int i = 0; i < priorityReservations.size(); i++) {
            PriorityReservation pr = priorityReservations.get(i);
            if (pr.getReservationId().equals(reservationId)) {
                return pr;
            }
        }
        return null;
    }

    public ListInterface<PriorityReservation> filterByLevel(PriorityLevel level) {
        ListInterface<PriorityReservation> result = new DoublyLinkedList<>();
        for (int i = 0; i < priorityReservations.size(); i++) {
            PriorityReservation pr = priorityReservations.get(i);
            if (!pr.isDeleted() && pr.getPriorityLevel() == level) {
                result.addBack(pr);
            }
        }
        return result;
    }

    public ListInterface<Reservation> generateVIPQueue(ListInterface<Reservation> reservations) {
        vipQueue = new DoublyLinkedList<>();
        int n = priorityReservations.size();
        boolean[] used = new boolean[n];

        for (int count = 0; count < n; count++) {
            int bestIndex = -1;
            PriorityReservation best = null;
            LocalDateTime bestTime = null;

            for (int i = 0; i < n; i++) {
                if (used[i]) {
                    continue;
                }
                PriorityReservation tmp = priorityReservations.get(i);

                if (tmp.isDeleted()) {
                    continue;
                }

                if (best == null) {
                    bestIndex = i;
                    best = tmp;
                    bestTime = getTimestamp(reservations, tmp);
                    continue;
                }

                int rankCompare = Integer.compare(
                        tmp.getPriorityLevel().getRank(),
                        best.getPriorityLevel().getRank());

                if (rankCompare > 0) { // tmp > best
                    bestIndex = i;
                    best = tmp;
                    bestTime = getTimestamp(reservations, tmp);
                } else if (rankCompare == 0) { // tmp == best
                    LocalDateTime tmpTime = getTimestamp(reservations, tmp);
                    if (tmpTime != null && (bestTime == null || tmpTime.isBefore(bestTime))) {
                        bestIndex = i;
                        best = tmp;
                        bestTime = tmpTime;
                    }
                }
            }

            if (bestIndex == -1) {
                break;
            }

            used[bestIndex] = true;

            Reservation reservation = getReservation(reservations, best.getReservationId());
            if (reservation != null) {
                vipQueue.addBack(reservation);
            }
        }

        return vipQueue;
    }

    private Reservation getReservation(ListInterface<Reservation> source, String reservationId) {
        for (int i = 0; i < source.size(); i++) {
            if (source.get(i).getReservationId().equals(reservationId)) {
                return source.get(i);
            }
        }
        return null;
    }

    private LocalDateTime getTimestamp(ListInterface<Reservation> source, PriorityReservation pr) {
        Reservation reservation = getReservation(source, pr.getReservationId());
        if (reservation == null || reservation.getTimestamps() == null) {
            return null;
        }
        return reservation.getTimestamps().getRegistrationTimestamp();
    }

    public boolean isEmpty() {
        return priorityReservations.isEmpty();
    }

    public int size() {
        return priorityReservations.size();
    }

    // UI
    public void run() {
        PriorityLevel levelFilter = null;
        int page = 0;

        while (true) {
            ListInterface<Reservation> history = loadHistory();

            ListInterface<PriorityReservation> display = (levelFilter == null) ? activePriorityReservations()
                    : filterByLevel(levelFilter);
            boolean hasFilter = levelFilter != null;

            int pageCount = Math.max(1, (display.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            if (page >= pageCount) {
                page = pageCount - 1;
            }
            ListInterface<PriorityReservation> pageList = pageOf(display, page);

            int choice = priorityReservationUI.printPriorityListMenu(
                    buildPriorityRows(pageList, history), page, pageCount, hasFilter);
            if (choice == 0) {
                return;
            }

            int action = 1;
            if (choice == action++) { // View Details
                viewPriority(pageList);
            } else if (choice == action++) { // Add Priority Reservation
                addPriorityFlow();
            } else if (choice == action++) { // Delete Priority Reservation
                deletePriorityFlow(pageList);
            } else if (choice == action++) { // View VIP Queue
                viewVIPQueueFlow();
            } else if (choice == action++) { // Filter by Priority Level
                PriorityLevel level = priorityReservationUI.selectPriorityLevel("Select a level to filter by");
                if (level != null) {
                    levelFilter = level;
                    page = 0;
                }
            } else if (choice == action++) { // Search Priority Reservation
                searchFlow();
            } else if (choice == action++) { // Priority Level Effectiveness Report
                reportController.generatePriorityLevelEffectivenessReport();
            } else if (choice == action++) { // VIP Queue & Override Governance Report
                reportController.generateVipQueueGovernanceReport();
            } else {
                boolean matched = false;
                if (page < pageCount - 1) {
                    matched = choice == action;
                    action++;
                    if (matched)
                        page++;
                }
                if (!matched && page > 0) {
                    matched = choice == action;
                    action++;
                    if (matched)
                        page--;
                }
                if (!matched && hasFilter) {
                    matched = choice == action;
                    action++;
                    if (matched) {
                        levelFilter = null;
                        page = 0;
                    }
                }
            }
        }
    }

    private void viewPriority(ListInterface<PriorityReservation> pageList) {
        if (pageList.isEmpty()) {
            priorityReservationUI.showMessage("No priority reservations to view.");
            return;
        }
        int num = priorityReservationUI.inputListIndex("record", pageList.size());
        if (num == 0) {
            return;
        }
        handlePriorityActions(pageList.get(num - 1).getReservationId());
    }

    private void handlePriorityActions(String reservationId) {
        while (true) {
            PriorityReservation pr = searchPriorityReservationById(reservationId);
            if (pr == null) {
                priorityReservationUI.showError("Record no longer exists.");
                return;
            }
            Reservation reservation = getReservation(loadHistory(), reservationId);
            priorityReservationUI.printPriorityDetail(pr, reservation);

            int action = priorityReservationUI.getPriorityActionChoice();
            if (action == 0) {
                return;
            }
            switch (action) {
                case 1: // Update Priority Level
                    overrideFlow(pr);
                    break;
                case 2: // Delete Priority Record
                    if (priorityReservationUI.confirm("Delete this priority record?")) {
                        removePriorityReservationById(reservationId);
                        priorityReservationUI.showMessage("Priority record deleted.");
                        return; // record removed - go back to the list
                    }
                    break;
                case 3: // View VIP Queue Position
                    viewQueuePosition(reservationId);
                    break;
                default:
                    break;
            }
        }
    }

    // Staff-authorized EMERGENCY priority for a non-member waiting reservation.
    private void addPriorityFlow() {
        Reservation reservation = priorityReservationUI.selectReservation(nonMemberWaiting());
        if (reservation == null) {
            return; // cancelled or none eligible
        }
        Staff staff = priorityReservationUI.selectStaff(staffDAO.retrieveStaffList());
        if (staff == null) {
            return;
        }
        String reason = priorityReservationUI.readNonEmpty("Enter the reason for emergency priority");

        if (!priorityReservationUI.confirm("Grant EMERGENCY priority to " + reservation.getReservationId() + "?")) {
            priorityReservationUI.showMessage("Cancelled. Nothing was changed.");
            return;
        }

        PriorityReservation pr = new PriorityReservation(
                reservation.getReservationId(), PriorityLevel.EMERGENCY, staff.getStaffId(), reason, false);
        priorityReservations.addBack(pr);
        priorityReservationDAO.saveToFile(priorityReservations);
        priorityReservationUI.showMessage(
                "EMERGENCY priority granted to " + reservation.getReservationId() + " by " + staff.getStaffId() + ".");
    }

    // non-member waiting reservations = WAITING, not deleted, and no priority record yet
    private ListInterface<Reservation> nonMemberWaiting() {
        ListInterface<Reservation> history = loadHistory();
        ListInterface<Reservation> result = new DoublyLinkedList<>();
        for (int i = 0; i < history.size(); i++) {
            Reservation r = history.get(i);
            if (!r.isDeleted() && r.getStatus() == ReservationStatus.WAITING
                    && searchPriorityReservationById(r.getReservationId()) == null) {
                result.addBack(r);
            }
        }
        return result;
    }

    private void deletePriorityFlow(ListInterface<PriorityReservation> pageList) {
        if (pageList.isEmpty()) {
            priorityReservationUI.showMessage("No priority reservations to delete.");
            return;
        }
        int num = priorityReservationUI.inputListIndex("record", pageList.size());
        if (num == 0) {
            return;
        }
        PriorityReservation pr = pageList.get(num - 1);
        if (!priorityReservationUI.confirm("Delete priority record for " + pr.getReservationId() + "?")) {
            return;
        }
        if (removePriorityReservationById(pr.getReservationId())) {
            priorityReservationUI.showMessage("Priority record deleted.");
        } else {
            priorityReservationUI.showError("Delete failed - record not found.");
        }
    }

    private void searchFlow() {
        String reservationId = priorityReservationUI.readNonEmpty("Enter reservation ID to search");
        if (searchPriorityReservationById(reservationId) == null) {
            priorityReservationUI.showError("No priority record found for " + reservationId + ".");
            return;
        }
        handlePriorityActions(reservationId);
    }

    private void viewQueuePosition(String reservationId) {
        ListInterface<Reservation> waiting = waitingReservations();
        ListInterface<Reservation> queue = generateVIPQueue(waiting);
        int position = -1;
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).getReservationId().equals(reservationId)) {
                position = i + 1;
                break;
            }
        }
        if (position == -1) {
            priorityReservationUI.showMessage(
                    "This reservation is not in the VIP queue (only WAITING members are queued).");
        } else {
            priorityReservationUI.showMessage(
                    "VIP queue position for " + reservationId + ": #" + position + " of " + queue.size() + ".");
        }
    }

    private void overrideFlow(PriorityReservation pr) {
        PriorityLevel newLevel = priorityReservationUI.selectPriorityLevel("Select the new priority level");
        if (newLevel == null) {
            return;
        }
        Staff staff = priorityReservationUI.selectStaff(staffDAO.retrieveStaffList());
        if (staff == null) {
            return; // cancelled or no staff on record
        }
        String staffId = staff.getStaffId();
        String reason = priorityReservationUI.readNonEmpty("Enter the reason for this override");

        priorityReservationUI.displayOverridePreview(pr, newLevel, staffId, reason);

        if (!priorityReservationUI.confirm("Apply this override?")) {
            priorityReservationUI.showMessage("Override cancelled. Nothing was changed.");
            return;
        }

        boolean updated = updatePriorityReservation(
                new PriorityReservation(pr.getReservationId(), newLevel, staffId, reason, false));

        if (updated) {
            priorityReservationUI
                    .showMessage("Priority for " + pr.getReservationId() + " updated to " + newLevel + ".");
        } else {
            priorityReservationUI.showError("Update failed - the record no longer exists.");
        }
    }

    private void viewVIPQueueFlow() {
        priorityReservationUI.displayVIPQueue(generateVIPQueue(waitingReservations()), priorityReservations);
        priorityReservationUI.pause();
    }

    // ===== UI HELPERS =====
    private ListInterface<Reservation> loadHistory() {
        ListInterface<Reservation> history = new DoublyLinkedList<>();
        reservationDAO.loadAllReservations(history);
        return history;
    }

    // only WAITING (and not soft-deleted) reservations are eligible for the VIP queue
    private ListInterface<Reservation> waitingReservations() {
        ListInterface<Reservation> history = loadHistory();
        ListInterface<Reservation> waiting = new DoublyLinkedList<>();
        for (int i = 0; i < history.size(); i++) {
            Reservation r = history.get(i);
            if (!r.isDeleted() && r.getStatus() == ReservationStatus.WAITING) {
                waiting.addBack(r);
            }
        }
        return waiting;
    }

    // active (not soft-deleted) priority records - the landing list hides deleted ones
    private ListInterface<PriorityReservation> activePriorityReservations() {
        ListInterface<PriorityReservation> result = new DoublyLinkedList<>();
        for (int i = 0; i < priorityReservations.size(); i++) {
            PriorityReservation pr = priorityReservations.get(i);
            if (!pr.isDeleted()) {
                result.addBack(pr);
            }
        }
        return result;
    }

    private DoublyLinkedList<PriorityReservation> pageOf(ListInterface<PriorityReservation> list, int page) {
        DoublyLinkedList<PriorityReservation> result = new DoublyLinkedList<>();
        int start = page * PAGE_SIZE;
        int end = Math.min(list.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            result.addBack(list.get(i));
        }
        return result;
    }

    private String[][] buildPriorityRows(ListInterface<PriorityReservation> list,
            ListInterface<Reservation> history) {
        String[][] data = new String[list.size() + 1][7];
        data[0] = new String[] { "No.", "Reservation ID", "Guest ID", "Priority", "Rank", "Status", "Overridden By" };
        for (int i = 0; i < list.size(); i++) {
            PriorityReservation pr = list.get(i);
            Reservation r = getReservation(history, pr.getReservationId());
            data[i + 1] = new String[] {
                    String.valueOf(i + 1),
                    pr.getReservationId(),
                    r == null ? "-" : r.getGuestId(),
                    pr.getPriorityLevel().name(),
                    String.valueOf(pr.getPriorityLevel().getRank()),
                    (r == null || r.getStatus() == null) ? "-" : r.getStatus().name(),
                    orDash(pr.getOverriddenBy())
            };
        }
        return data;
    }

    private String orDash(String value) {
        return (value == null || value.isEmpty()) ? "-" : value;
    }
}

// todo validation