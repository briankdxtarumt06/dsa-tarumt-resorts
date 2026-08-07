package tarumtresort.control;

import tarumtresort.dao.ReservationDAO;
import tarumtresort.entity.RoomType;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.ReservationStatus;
import tarumtresort.entity.ReservationType;
import tarumtresort.entity.Room;
import tarumtresort.entity.RoomStatus;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;

import java.time.LocalDateTime;
import java.time.LocalDate;
import tarumtresort.entity.ReservationTimestamps;

public class ReservationControl {

    // controller
    private RoomControl roomControl = new RoomControl();

    // list declared
    private LinkedListInterface<Reservation> guestQueue = new LinkedList<>();
    private LinkedListInterface<Reservation> assignedList = new LinkedList<>();

     // dao 
    private static final ReservationDAO reservationDAO = new ReservationDAO();
   
    //Constructor 
    public ReservationControl(){
        guestQueue = reservationDAO.retrieveGuestQueue();
        assignedList = reservationDAO.retrieveAssignedList();
    }

    public boolean registerGuest (String guestId, RoomType roomTypeRequested, int numberOfGuests, int numberOfNights, ReservationType reservationType, LocalDate expectedCheckInDate){
        String reservationId = generateReservationId();
        String confirmationNumber = generateConfirmationNumber();

        // create timestamps
        ReservationTimestamps timestamps = new ReservationTimestamps(
            LocalDateTime.now(),
            expectedCheckInDate,
            expectedCheckInDate.plusDays(numberOfNights)
        );

        // create reservation 
        Reservation reservation = new Reservation(
            reservationId,
            confirmationNumber,
            guestId,
            null,
            roomTypeRequested,
            numberOfGuests,
            numberOfNights,
            reservationType,
            ReservationStatus.WAITING,
            timestamps
        );

        guestQueue.addSorted(reservation);
        //reservationDAO.saveGuestQueue(guestQueue);
    
        return true;

    }

    //assign room by roomtype
    public Reservation assignRoom(Room room) {

        for (int i = 0; i < guestQueue.size(); i++) {

            Reservation reservation = guestQueue.get(i);

            if (reservation.getRoomTypeRequested() == room.getRoomType() && room.getRoomStatus() == RoomStatus.AVAILABLE) {
                reservation.setRoomId(room.getRoomId());
                reservation.setStatus(ReservationStatus.ASSIGNED);
                reservation.getTimestamps().setAssignedTime(LocalDateTime.now());
                roomControl.updateRoomStatus(room.getRoomId(), RoomStatus.OCCUPIED);
                assignedList.addBack(reservation);
                guestQueue.removeAt(i);

                //reservationDAO.saveGuestQueue(guestQueue);
                //reservationDAO.saveAssignedList(assignedList);

                return reservation;
            }
        }

        return null;
    }

    // cancel reservation / early check out
    public boolean cancelReservation(String confirmationNumber) {

        // check if the guest is in the waiting queue
        for (int i = 0; i < guestQueue.size(); i++) {
            Reservation reservation = guestQueue.get(i);

            if (reservation.getConfirmationNumber().equals(confirmationNumber)) {
                reservation.setStatus(ReservationStatus.CANCELLED);
                guestQueue.removeAt(i);
                //reservationDAO.saveGuestQueue(guestQueue);
                return true;
            }
        }

        // check if the guest already assigned room
        for (int i = 0; i < assignedList.size(); i++) {

            Reservation reservation = assignedList.get(i);

            if (reservation.getConfirmationNumber().equals(confirmationNumber) && reservation.getStatus() == ReservationStatus.ASSIGNED) {
                reservation.setStatus(ReservationStatus.CANCELLED);

                // release room
                roomControl.updateRoomStatus( reservation.getRoomId(), RoomStatus.AVAILABLE);
                assignedList.removeAt(i);
                //reservationDAO.saveAssignedList(assignedList);
                return true;
            }
        }

        return false;
    }

    //check in
    public boolean checkIn(String confirmationNumber) {

        for (int i = 0; i < assignedList.size(); i++) {

            Reservation reservation = assignedList.get(i);

            if (reservation.getConfirmationNumber().equals(confirmationNumber)
                    && reservation.getStatus() == ReservationStatus.ASSIGNED) {

                // guest cannot check in before expected check in date
                if (LocalDate.now().isBefore(
                        reservation.getTimestamps().getExpectedCheckInDate())) {

                    return false;
                }

                reservation.setStatus(ReservationStatus.CHECKED_IN);
                reservation.getTimestamps().setActualCheckInTime(LocalDateTime.now());
                //reservationDAO.saveAssignedList(assignedList);

                return true;
            }
        }

        return false;
    }

    // check out
    public boolean checkOut(String confirmationNumber) {

        for (int i = 0; i < assignedList.size(); i++) {
            Reservation reservation = assignedList.get(i);
            if (reservation.getConfirmationNumber().equals(confirmationNumber) && reservation.getStatus() == ReservationStatus.CHECKED_IN) {
                reservation.setStatus(ReservationStatus.CHECKED_OUT);  // update reservation status
                reservation.getTimestamps().setActualCheckOutTime(LocalDateTime.now()); // record actual check out time
                roomControl.updateRoomStatus( reservation.getRoomId(), RoomStatus.AVAILABLE);  // release room
                //reservationDAO.saveAssignedList(assignedList);
                return true;
            }
        }

        return false;
    }

    // get queue position
    public int getQueuePosition(String confirmationNumber) {
        for (int i = 0; i < guestQueue.size(); i++) {
            Reservation reservation = guestQueue.get(i);
            if (reservation.getConfirmationNumber().equals(confirmationNumber)) {
                return i + 1;
            }
        }
        return -1;
    }

    // get reservations by room type
    public LinkedList<Reservation> getRoomTypeReservations(RoomType roomType) {
        LinkedList<Reservation> reservationList = new LinkedList<>();
        for (int i = 0; i < guestQueue.size(); i++) {
            Reservation reservation = guestQueue.get(i);
            if (reservation.getRoomTypeRequested() == roomType) {
                reservationList.addBack(reservation);
            }
        }
        return reservationList;
    }

    // get waiting reservation queue
    public LinkedListInterface<Reservation> getGuestQueue() {
        return guestQueue;
    }

    // get assigned reservation list
    public LinkedListInterface<Reservation> getAssignedList() {
        return assignedList;
    }

    // generate reservation id
    private String generateReservationId() {

        int max = 0;

        // check waiting queue
        for (int i = 0; i < guestQueue.size(); i++) {
            String reservationId = guestQueue.get(i).getReservationId();
            int number = Integer.parseInt(reservationId.substring(3));
            if (number > max) {
                max = number;
            }
        }

        // check assigned list
        for (int i = 0; i < assignedList.size(); i++) {
            String reservationId = assignedList.get(i).getReservationId();
            int number = Integer.parseInt(reservationId.substring(3));
            if (number > max) {
                max = number;
            }
        }

        return String.format("RES%03d", max + 1);
    }
        
    // generate confirmation number
    private String generateConfirmationNumber() {

        while (true) {

            String confirmationNumber = String.format("%08d", (int) (Math.random() * 100000000));
            boolean duplicate = false;

            // check if duplicated in waiting queue
            for (int i = 0; i < guestQueue.size(); i++) {
                if (guestQueue.get(i).getConfirmationNumber().equals(confirmationNumber)) {
                    duplicate = true;
                    break;
                }
            }

            // check if duplicated in assigned list
            if (!duplicate) {
                for (int i = 0; i < assignedList.size(); i++) {
                    if (assignedList.get(i).getConfirmationNumber().equals(confirmationNumber)) {
                        duplicate = true;
                        break;
                    }
                }
            }

            if (!duplicate) {
                return confirmationNumber;
            }
        }
    }

    public Reservation getReservationByConfirmationNumber(String confirmationNumber) {
        // search waiting queue
        for (int i = 0; i < guestQueue.size(); i++) {
            Reservation reservation = guestQueue.get(i);
            if (reservation.getConfirmationNumber().equals(confirmationNumber)) {
                return reservation;
            }
        }

        // search assigned list
        for (int i = 0; i < assignedList.size(); i++) {
            Reservation reservation = assignedList.get(i);
            if (reservation.getConfirmationNumber().equals(confirmationNumber)) {
                return reservation;
            }
        }

        return null;
    }
    
    public Reservation getReservationByReservationId(String reservationId) {

        // search waiting queue
        for (int i = 0; i < guestQueue.size(); i++) {
            Reservation reservation = guestQueue.get(i);
            if (reservation.getReservationId().equals(reservationId)) {
                return reservation;
            }
        }

        // search assigned list
        for (int i = 0; i < assignedList.size(); i++) {
            Reservation reservation = assignedList.get(i);
            if (reservation.getReservationId().equals(reservationId)) {
                return reservation;
            }
        }

        return null;
    }

    public boolean reservationExists(String confirmationNumber) {
        return getReservationByConfirmationNumber(confirmationNumber) != null;
    }

    public Reservation getReservationByRoomId(String roomId) {

        for (int i = 0; i < assignedList.size(); i++) {

            Reservation reservation = assignedList.get(i);

            if (reservation.getRoomId() != null
                    && reservation.getRoomId().equals(roomId)) {

                return reservation;
            }
        }

        return null;
    }

    public LinkedListInterface<Reservation> getCheckedInReservations() {
        LinkedListInterface<Reservation> reservationList = new LinkedList<>();

        for (int i = 0; i < assignedList.size(); i++) {
            Reservation reservation = assignedList.get(i);
            if (reservation.getStatus() == ReservationStatus.CHECKED_IN) {
                reservationList.addBack(reservation);
            }
        }

        return reservationList;
    }

    public LinkedListInterface<Reservation> getAssignedReservations() {
        LinkedListInterface<Reservation> reservationList = new LinkedList<>();
        for (int i = 0; i < assignedList.size(); i++) {
            Reservation reservation = assignedList.get(i);
            if (reservation.getStatus() == ReservationStatus.ASSIGNED) {
                reservationList.addBack(reservation);
            }
        }

        return reservationList;
    }

    public LinkedListInterface<Reservation> getCheckedOutReservations() {

        LinkedListInterface<Reservation> reservationList = new LinkedList<>();
        for (int i = 0; i < assignedList.size(); i++) {
            Reservation reservation = assignedList.get(i);
            if (reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
                reservationList.addBack(reservation);
            }
        }

        return reservationList;
    }
}
