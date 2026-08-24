package tarumtresort.report.ReservationReport;

import java.time.LocalDateTime;
import java.util.Scanner;
import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;
import tarumtresort.dao.GuestDAO;
import tarumtresort.dao.ReservationDAO;
import tarumtresort.dao.RoomDAO;
import tarumtresort.entity.Guest;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.Room;
import tarumtresort.entity.enums.ReservationStatus;

// Author: Chai Chee Tong

public class ReservationReportController {

    private final GuestDAO guestDAO = new GuestDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final ReservationDAO reservationDAO = new ReservationDAO();

    private final Scanner scanner;

    public ReservationReportController(Scanner scanner) {
        this.scanner = scanner;
    }

    /** Report 1 - which guest nationality books the most / fewest reservations. */
    public void generateNationalityDemandReport() {
        ReservationReportUI ui = ui();

        LocalDateTime[] range = ui.inputOptionalDateTimeRange("registration timestamp");
        ReservationStatus statusFilter = ui.selectStatusFilter();

        NationalityReport.Result result = new NationalityReport(loadGuests(), loadReservations())
                .generate(range[0], range[1], statusFilter);

        new NationalityReportUI(ui).render(result);
        ui.pressEnterToContinue();
    }

    /** Report 2 - which room type is reserved the most / fewest times. */
    public void generateRoomTypeDemandReport() {
        ReservationReportUI ui = ui();

        LocalDateTime[] range = ui.inputOptionalDateTimeRange("registration timestamp");
        ReservationStatus statusFilter = ui.selectStatusFilter();

        RoomTypeReport.Result result = new RoomTypeReport(loadRooms(), loadReservations())
                .generate(range[0], range[1], statusFilter);

        new RoomTypeReportUI(ui).render(result);
        ui.pressEnterToContinue();
    }

    // -------------------- data loading --------------------

    private ListInterface<Guest> loadGuests() {
        ListInterface<Guest> guests = new DoublyLinkedList<>();
        guestDAO.loadFromFile(guests);
        return guests;
    }

    private ListInterface<Room> loadRooms() {
        ListInterface<Room> rooms = new DoublyLinkedList<>();
        roomDAO.loadFromFile(rooms);
        return rooms;
    }

    private ListInterface<Reservation> loadReservations() {
        ListInterface<Reservation> reservations = new DoublyLinkedList<>();
        reservationDAO.loadAllReservations(reservations);
        return reservations;
    }

    private ReservationReportUI ui() {
        return new ReservationReportUI(scanner);
    }
}
