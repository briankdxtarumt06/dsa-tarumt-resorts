package tarumtresort.control;

import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.boundary.PriorityReservationUI;
import tarumtresort.entity.*;
import tarumtresort.entity.enums.*;

import tarumtresort.dao.*;

import java.time.LocalDateTime;
import java.util.Scanner;

public class PriorityReservationController {
    private static final int PAGE_SIZE = 20;

    // ADT declaration
    private LinkedListInterface<PriorityReservation> priorityReservations = new LinkedList<>();
    private LinkedListInterface<Reservation> vipQueue = new LinkedList<>();

    // DAO
    private PriorityReservationDAO priorityReservationDAO = new PriorityReservationDAO();
    private ReservationDAOV2 reservationDAO = new ReservationDAOV2();

    // Controllers
    private LoyaltyController loyaltyController;

    // Boundary
    private PriorityReservationUI priorityReservationUI = new PriorityReservationUI();

    public PriorityReservationController() {
        this(new Scanner(System.in));
    }

    public PriorityReservationController(Scanner scanner) {
        this.loyaltyController = new LoyaltyController(scanner);
        this.priorityReservationUI = new PriorityReservationUI(scanner);
        this.priorityReservations = priorityReservationDAO.loadFromFile();
    }

    public boolean addPriorityReservation(String reservationId, String guestId) {
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

    public LinkedListInterface<PriorityReservation> filterByLevel(PriorityLevel level) {
        LinkedListInterface<PriorityReservation> result = new LinkedList<>();
        for (int i = 0; i < priorityReservations.size(); i++) {
            PriorityReservation pr = priorityReservations.get(i);
            if (!pr.isDeleted() && pr.getPriorityLevel() == level) {
                result.addBack(pr);
            }
        }
        return result;
    }

    public LinkedListInterface<Reservation> generateVIPQueue(LinkedListInterface<Reservation> reservations) {
        vipQueue = new LinkedList<>();
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

    private Reservation getReservation(LinkedListInterface<Reservation> source, String reservationId) {
        for (int i = 0; i < source.size(); i++) {
            if (source.get(i).getReservationId().equals(reservationId)) {
                return source.get(i);
            }
        }
        return null;
    }

    private LocalDateTime getTimestamp(LinkedListInterface<Reservation> source, PriorityReservation pr) {
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
            LinkedListInterface<Reservation> history = loadHistory();

            LinkedListInterface<PriorityReservation> display = (levelFilter == null) ? priorityReservations
                    : allByLevel(levelFilter);
            boolean hasFilter = levelFilter != null;

            int pageCount = Math.max(1, (display.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            if (page >= pageCount) {
                page = pageCount - 1;
            }
            LinkedListInterface<PriorityReservation> pageList = pageOf(display, page);

            int choice = priorityReservationUI.printPriorityListMenu(
                    buildPriorityRows(pageList, history), page, pageCount, hasFilter);
            if (choice == 0) {
                return;
            }

            int action = 1;
            if (choice == action++) { // View Details
                viewPriority(pageList);
            } else if (choice == action++) { // View VIP Queue
                viewVIPQueueFlow();
            } else if (choice == action++) { // Filter by Priority Level
                PriorityLevel level = priorityReservationUI.selectPriorityLevel("Select a level to filter by");
                if (level != null) {
                    levelFilter = level;
                    page = 0;
                }
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

    private void viewPriority(LinkedListInterface<PriorityReservation> pageList) {
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
            priorityReservationUI.printPriorityDetail(buildDetailRows(pr, reservation));

            int action = priorityReservationUI.getPriorityActionChoice();
            if (action == 0) {
                return;
            }
            switch (action) {
                case 1:
                    overrideFlow(pr);
                    break;
                default:
                    break;
            }
        }
    }

    private void overrideFlow(PriorityReservation pr) {
        PriorityLevel newLevel = priorityReservationUI.selectPriorityLevel("Select the new priority level");
        if (newLevel == null) {
            return;
        }
        String staffId = priorityReservationUI.readNonEmpty("Enter your staff ID");
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
        LinkedListInterface<Reservation> history = loadHistory();
        LinkedListInterface<Reservation> waiting = new LinkedList<>();
        for (int i = 0; i < history.size(); i++) {
            Reservation r = history.get(i);
            if (!r.isDeleted() && r.getStatus() == ReservationStatus.WAITING) {
                waiting.addBack(r);
            }
        }
        priorityReservationUI.displayVIPQueue(generateVIPQueue(waiting), priorityReservations);
        priorityReservationUI.pause();
    }

    // ===== UI HELPERS =====
    private LinkedListInterface<Reservation> loadHistory() {
        LinkedListInterface<Reservation> history = new LinkedList<>();
        reservationDAO.loadAllReservations(history);
        return history;
    }

    // all records of a level (incl. served/cancelled) - the landing list is a
    // history view
    private LinkedListInterface<PriorityReservation> allByLevel(PriorityLevel level) {
        LinkedListInterface<PriorityReservation> result = new LinkedList<>();
        for (int i = 0; i < priorityReservations.size(); i++) {
            PriorityReservation pr = priorityReservations.get(i);
            if (pr.getPriorityLevel() == level) {
                result.addBack(pr);
            }
        }
        return result;
    }

    private LinkedList<PriorityReservation> pageOf(LinkedListInterface<PriorityReservation> list, int page) {
        LinkedList<PriorityReservation> result = new LinkedList<>();
        int start = page * PAGE_SIZE;
        int end = Math.min(list.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            result.addBack(list.get(i));
        }
        return result;
    }

    private String[][] buildPriorityRows(LinkedListInterface<PriorityReservation> list,
            LinkedListInterface<Reservation> history) {
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

    private String[][] buildDetailRows(PriorityReservation pr, Reservation r) {
        return new String[][] {
                { "Reservation ID", pr.getReservationId() },
                { "Guest ID", r == null ? "-" : r.getGuestId() },
                { "Priority Level", pr.getPriorityLevel().name() + " (rank " + pr.getPriorityLevel().getRank() + ")" },
                { "Reservation Status", (r == null || r.getStatus() == null) ? "-" : r.getStatus().name() },
                { "Room Type",
                        (r == null || r.getRoomTypeRequested() == null) ? "-" : r.getRoomTypeRequested().name() },
                { "Overridden By", orDash(pr.getOverriddenBy()) },
                { "Override Reason", orDash(pr.getOverrideReason()) }
        };
    }

    private String orDash(String value) {
        return (value == null || value.isEmpty()) ? "-" : value;
    }
}

// todo validation, report