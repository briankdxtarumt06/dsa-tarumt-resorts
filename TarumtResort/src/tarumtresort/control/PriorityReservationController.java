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
    // ADT declaration
    private LinkedListInterface<PriorityReservation> priorityReservations = new LinkedList<>();
    private LinkedListInterface<Reservation> vipQueue = new LinkedList<>();

    // DAO
    private PriorityReservationDAO priorityReservationDAO = new PriorityReservationDAO();
    private ReservationDAO reservationDAO = new ReservationDAO();

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
        int choice;
        do {
            choice = priorityReservationUI.getMenuChoice();
            switch (choice) {
                case 1:
                    viewVIPQueueFlow();
                    break;
                case 2:
                    listAllFlow();
                    break;
                case 3:
                    filterFlow();
                    break;
                case 4:
                    overrideFlow();
                    break;
                case 5:
                    break;
                default:
                    priorityReservationUI.showError("Invalid choice. Please enter 1 - 5.");
            }
        } while (choice != 5);
    }

    private void viewVIPQueueFlow() {
        LinkedListInterface<Reservation> guestQueue = new LinkedList<>();
        reservationDAO.loadGuestQueue(guestQueue);
        priorityReservationUI.displayVIPQueue(
                generateVIPQueue(guestQueue), priorityReservations);
        priorityReservationUI.pause();
    }

    private void listAllFlow() {
        priorityReservationUI.displayPriorityReservations(priorityReservations);
        priorityReservationUI.pause();
    }

    private void filterFlow() {
        PriorityLevel level = priorityReservationUI.selectPriorityLevel("Select a level to filter by");
        if (level == null) {
            return; // cancelled - the UI already paused
        }
        priorityReservationUI.displayPriorityReservations(filterByLevel(level));
        priorityReservationUI.pause();
    }

    // private void overrideFlow() {
    //     String reservationId = priorityReservationUI.selectPriorityReservation(
    //             priorityReservations, "Select a record to override");
    //     if (reservationId == null) {
    //         return;
    //     }

    //     PriorityReservation pr = searchById(reservationId);
    //     if (pr == null) {
    //         priorityReservationUI.showError("Record not found.");
    //         return;
    //     }

    //     priorityReservationUI.displayDetails(pr);

    //     PriorityLevel newLevel = priorityReservationUI.selectPriorityLevel("Select the new priority level");
    //     if (newLevel == null) {
    //         return;
    //     }

    //     String staffId = priorityReservationUI.readNonEmpty("Enter your staff ID");
    //     String reason = priorityReservationUI.readNonEmpty("Enter the reason for this override");

    //     priorityReservationUI.displayOverridePreview(pr, newLevel, staffId, reason);

    //     if (!priorityReservationUI.confirm("Apply this override?")) {
    //         priorityReservationUI.showMessage("Override cancelled. Nothing was changed.");
    //         return;
    //     }

    //     boolean updated = updatePriorityReservation(
    //             new PriorityReservation(reservationId, newLevel, staffId, reason));

    //     if (updated) {
    //         priorityReservationUI.showMessage("Priority for " + reservationId + " updated to " + newLevel + ".");
    //     } else {
    //         priorityReservationUI.showError("Update failed - the record no longer exists.");
    //     }
    // }
}

// todo validation, report