package tarumtresort.control;

import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;

import tarumtresort.entity.*;
import tarumtresort.entity.enums.*;

import tarumtresort.dao.*;

import java.time.LocalDateTime;

public class PriorityReservationController {
    private LinkedListInterface<PriorityReservation> priorityReservations = new LinkedList<>();
    private PriorityReservationDAO priorityReservationDAO = new PriorityReservationDAO();
    private MemberController memberController = new MemberController();

    private ReservationControl reservationControl = new ReservationControl();

    public PriorityReservationController() {
        priorityReservations = priorityReservationDAO.loadFromFile();
    }

    public void addPriorityReservation(PriorityReservation priorityReservation) {
        priorityReservations.addSorted(priorityReservation);
        priorityReservationDAO.saveToFile(priorityReservations);
    }

    public void removePriorityReservation(PriorityReservation priorityReservation) {
        priorityReservations.removeElement(priorityReservation);
        priorityReservationDAO.saveToFile(priorityReservations);
    }

    public LinkedListInterface<PriorityReservation> getAll() {
        return priorityReservations;
    }

    public PriorityReservation searchById(String reservationId) {
        for (int i = 0; i < priorityReservations.size(); i++) {
            PriorityReservation pr = priorityReservations.get(i);
            if (pr.getReservationId().equals(reservationId)) {
                return pr;
            }
        }
        return null;
    }

    public boolean updatePriorityReservation(PriorityReservation updatedReservation) {
        PriorityReservation pr = searchById(updatedReservation.getReservationId());

        if (pr == null) {
            return false;
        }
        removeById(updatedReservation.getReservationId());
        pr.setPriorityLevel(updatedReservation.getPriorityLevel());
        pr.setOverriddenBy(updatedReservation.getOverriddenBy());
        pr.setOverrideReason(updatedReservation.getOverrideReason());
        priorityReservations.addSorted(pr);
        priorityReservationDAO.saveToFile(priorityReservations);
        return true;
    }

    public boolean removeById(String reservationId) {
        PriorityReservation pr = searchById(reservationId);
        if (pr != null) {
            priorityReservations.removeElement(pr);
            priorityReservationDAO.saveToFile(priorityReservations);
            return true;
        }
        return false;
    }

    public LinkedListInterface<PriorityReservation> filterByLevel(PriorityLevel level) {
        LinkedListInterface<PriorityReservation> result = new LinkedList<>();
        for (int i = 0; i < priorityReservations.size(); i++) {
            PriorityReservation pr = priorityReservations.get(i);
            if (pr.getPriorityLevel() == level) {
                result.addBack(pr);
            }
        }
        return result;
    }

    public boolean isMember(String guestId) {
        Member member = findMemberByGuestId(guestId);
        if (member != null) {
            return true;
        }else {
            return false;
        }
    }

    public Member findMemberByGuestId(String guestId) {  // need move to member control
        LinkedListInterface<Member> members = new LinkedList<>();
        memberDAO.loadFromFile(members);

        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).getGuestId().equals(guestId)) {
                return members.get(i);
            }
        }
        return null;
    }

    public LinkedListInterface<PriorityReservation> generatePriorityQueue() {
        int n = priorityReservations.size();
        LinkedListInterface<PriorityReservation> queue = new LinkedList<>();
        boolean[] used = new boolean[n];

        for (int count = 0; count < n; count++) {
            int bestIndex = -1;
            PriorityReservation best = null;
            LocalDateTime bestTime = null;

            for (int i = 0; i < n; i++) {
                if (used[i]) {
                    continue;
                }
                PriorityReservation candidate = priorityReservations.get(i);

                if (best == null) {
                    bestIndex = i;
                    best = candidate;
                    bestTime = getTimestamp(candidate);
                    continue;
                }

                int rankCompare = Integer.compare(
                        candidate.getPriorityLevel().getRank(),
                        best.getPriorityLevel().getRank());

                if (rankCompare > 0) {
                    // strictly higher priority wins
                    bestIndex = i;
                    best = candidate;
                    bestTime = getTimestamp(candidate);
                } else if (rankCompare == 0) {
                    // same priority level -> earlier registration time wins
                    LocalDateTime candidateTime = getTimestamp(candidate);
                    if (bestTime == null && candidateTime != null) {
                        bestIndex = i;
                        best = candidate;
                        bestTime = candidateTime;
                    } else if (bestTime != null && candidateTime != null
                            && candidateTime.isBefore(bestTime)) {
                        bestIndex = i;
                        best = candidate;
                        bestTime = candidateTime;
                    }
                    // if both timestamps are null (reservation not found), keep
                    // the earlier one already found -> preserves original order
                }
            }

            used[bestIndex] = true;
            queue.addBack(best);
        }

        return queue;
    }

    // looks up the registration timestamp of the reservation this override
    // belongs to; returns null if the reservation can no longer be found
    // (e.g. cancelled or removed elsewhere) so callers can fall back gracefully
    private LocalDateTime getTimestamp(PriorityReservation pr) {
        Reservation reservation = reservationControl.getReservationByReservationId(pr.getReservationId());
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

}
