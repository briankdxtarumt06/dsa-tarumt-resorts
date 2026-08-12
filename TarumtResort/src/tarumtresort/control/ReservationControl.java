package tarumtresort.control;

import tarumtresort.dao.ReservationDAO;
import tarumtresort.entity.enums.*;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.Room;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.boundary.ReservationUI;

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
   
    // UI
    private ReservationUI reservationUI = new ReservationUI();
;
    //Constructor 
    public ReservationControl(){
        guestQueue = reservationDAO.retrieveGuestQueue();
        assignedList = reservationDAO.retrieveAssignedList();
    }

    public void runReservationModule() {
        int choice = 0;
        do {
            choice = reservationUI.getMenuChoice();
            switch (choice) {
                case 0: break;
                case 1: registerGuest(); break;
                case 2: assignRoom(); break;
                case 3: checkIn(); break;
                case 4: checkOut(); break;
                case 5: viewQueue(); break;
                case 6: checkQueuePosition(); break;
                case 7: cancelReservation(); break;
                case 8: generateReport(); break;
                default: break;
            }
        } while (choice != 0);
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

    public void registerGuest() {
        Reservation newReservation = reservationUI.inputReservationDetails(
            generateReservationId(),
            generateConfirmationNumber()
        );
        guestQueue.addSorted(newReservation);
        reservationDAO.saveGuestQueue(guestQueue);
        reservationUI.printReservationDetails(newReservation);
        reservationUI.pressEnterToContinue();
    }

    public void assignRoom() {
        Room availableRoom = roomControl.getAvailableRoom(reservationUI.inputRoomType());
        if (availableRoom == null) {
            reservationUI.printRoomNotAvailable();
        } else {
            Reservation assigned = assignRoom(availableRoom);
            if (assigned == null) {
                reservationUI.printNotFound();
            } else {
                reservationUI.printReservationDetails(assigned);
                reservationUI.printSuccess();
            }
        }
        reservationUI.pressEnterToContinue();
    }

    public void checkIn() {
        String confirmationNumber = reservationUI.inputConfirmationNumber();
        boolean success = checkIn(confirmationNumber);
        if (!success) {
            reservationUI.printCannotCheckIn();
        } else {
            reservationUI.printSuccess();
        }
        reservationUI.pressEnterToContinue();
    }

    public void checkOut() {
        String confirmationNumber = reservationUI.inputConfirmationNumber();
        boolean success = checkOut(confirmationNumber);
        if (!success) {
            reservationUI.printNotFound();
        } else {
            reservationUI.printSuccess();
        }
        reservationUI.pressEnterToContinue();
    }

    public void viewQueue() {
        int choice = reservationUI.getViewQueueMenuChoice();
        switch (choice) {
            case 0: break;
            case 1:
                // view all
                String[][] allData = buildQueueTableData(guestQueue);
                reservationUI.listAllReservations(allData);
                break;
            case 2:
                // view by room type
                RoomType roomType = reservationUI.inputRoomType();
                String[][] filteredData = buildQueueTableData(getRoomTypeReservations(roomType));
                reservationUI.listAllReservations(filteredData);
                break;
        }
        reservationUI.pressEnterToContinue();
    }

    public void checkQueuePosition() {
        String confirmationNumber = reservationUI.inputConfirmationNumber();
        int position = getQueuePosition(confirmationNumber);
        reservationUI.printQueuePosition(confirmationNumber, position);
        reservationUI.pressEnterToContinue();
    }

    public void cancelReservation() {
        String confirmationNumber = reservationUI.inputConfirmationNumber();
        if (reservationUI.inputConfirmation("Are you sure you want to cancel?")) {
            boolean success = cancelReservation(confirmationNumber);
            if (!success) {
                reservationUI.printNotFound();
            } else {
                reservationUI.printCancelled();
            }
        }
        reservationUI.pressEnterToContinue();
    }

    public void generateReport() {
        int choice = reservationUI.getReportMenuChoice();
        switch (choice) {
            case 0: break;
            // case 1: generateReport1(); break;
            // case 2: generateReport2(); break;
        }
        reservationUI.pressEnterToContinue();
    }

    // helper — build table data from queue
    private String[][] buildQueueTableData(LinkedListInterface<Reservation> list) {
        String[][] data = new String[list.size() + 1][6];
        data[0] = new String[]{"Confirmation No.", "Guest ID", "Room Type", "Type", "Status", "Check-In Date"};
        for (int i = 0; i < list.size(); i++) {
            Reservation r = list.get(i);
            data[i + 1] = new String[]{
                r.getConfirmationNumber(),
                r.getGuestId(),
                r.getRoomTypeRequested().toString(),
                r.getReservationType().toString(),
                r.getStatus().toString(),
                r.getTimestamps().getExpectedCheckInDate().toString()
            };
        }
        return data;
    }

    public static void main(String[] args) {
        ReservationControl reservationControl = new ReservationControl();
        reservationControl.runReservationModule();
    }

}
