package tarumtresort.control;

import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;

import tarumtresort.entity.*;
import tarumtresort.entity.enums.*;

import tarumtresort.dao.*;

import java.time.LocalDateTime;

public class PriorityReservationController {
    //ADT declaration
    private LinkedListInterface<PriorityReservation> priorityReservations = new LinkedList<>();
    private LinkedListInterface<Member> members = new LinkedList<>();

    //DAO
    private PriorityReservationDAO priorityReservationDAO = new PriorityReservationDAO();

    //Controllers
    private MemberController memberController = new MemberController();
    private ReservationControl reservationControl;


    public PriorityReservationController() {
        priorityReservations = priorityReservationDAO.loadFromFile();
    }

    public void addPriorityReservation(String reservationId, String guestId) {
        Member member = findMemberByGuestId(guestId);
        PriorityLevel priorityLevel = PriorityLevel.convertTierToPriority(member.getTier());
        PriorityReservation priorityReservation = new PriorityReservation(reservationId, priorityLevel);
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

    public boolean checkMember(String guestId) {
        Member member = findMemberByGuestId(guestId);
        if (member != null) {
            return true;
        } else {
            return false;
        }
    }

    public Member findMemberByGuestId(String guestId) { // need move to member control
        members = memberController.getMembers();
        for (int i = 0; i < members.size(); i++) {
            if (guestId.equals(members.get(i).getGuestId())) {
                return members.get(i);
            }
        }
        return null;
    }

    public LinkedListInterface<Reservation> generateVIPQueue() {
        int n = priorityReservations.size();
        LinkedListInterface<Reservation> queue = new LinkedList<>();
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

                if (best == null) {
                    bestIndex = i;
                    best = tmp;
                    bestTime = getTimestamp(tmp);
                    continue;
                }

                int rankCompare = Integer.compare(
                        tmp.getPriorityLevel().getRank(),
                        best.getPriorityLevel().getRank());

                if (rankCompare > 0) { // tmp > best
                    bestIndex = i;
                    best = tmp;
                    bestTime = getTimestamp(tmp);
                } else if (rankCompare == 0) { // tmp == best
                    LocalDateTime tmpTime = getTimestamp(tmp);
                    if (tmpTime.isBefore(bestTime)) {
                        bestIndex = i;
                        best = tmp;
                        bestTime = tmpTime;
                    }
                }
            }

            used[bestIndex] = true;

            Reservation reservation = reservationControl.getReservationByReservationId(best.getReservationId());
            if (reservation != null) {
                queue.addBack(reservation);
            }
        }

        return queue;
    }

    private LocalDateTime getTimestamp(PriorityReservation pr) {
        Reservation reservation = reservationControl.getReservationByReservationId(pr.getReservationId());
        return reservation.getTimestamps().getRegistrationTimestamp();
    }

    public boolean isEmpty() {
        return priorityReservations.isEmpty();
    }

    public int size() {
        return priorityReservations.size();
    }

}
