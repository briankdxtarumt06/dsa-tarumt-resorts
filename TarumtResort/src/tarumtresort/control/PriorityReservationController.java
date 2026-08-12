package tarumtresort.control;

import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.PriorityReservation;
import tarumtresort.entity.enums.PriorityLevel;
import tarumtresort.dao.PriorityReservationDAO;

public class PriorityReservationController {
    private LinkedListInterface<PriorityReservation> priorityReservations = new LinkedList<>();
    private PriorityReservationDAO priorityReservationDAO = new PriorityReservationDAO();

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

    


    public boolean isEmpty() {
        return priorityReservations.isEmpty();
    }

    public int size() {
        return priorityReservations.size();
    }

}
