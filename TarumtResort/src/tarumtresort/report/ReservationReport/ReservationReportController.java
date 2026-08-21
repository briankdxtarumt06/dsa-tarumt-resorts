package tarumtresort.report.ReservationReport;

import java.time.LocalDateTime;
import java.util.Scanner;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.dao.GuestDAO;
import tarumtresort.dao.ReservationDAO;
import tarumtresort.dao.RoomDAO;
import tarumtresort.entity.Guest;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.Room;
import tarumtresort.entity.enums.ReservationStatus;

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

    private LinkedListInterface<Guest> loadGuests() {
        LinkedListInterface<Guest> guests = new LinkedList<>();
        guestDAO.loadFromFile(guests);
        return guests;
    }

    private LinkedListInterface<Room> loadRooms() {
        LinkedListInterface<Room> rooms = new LinkedList<>();
        roomDAO.loadFromFile(rooms);
        return rooms;
    }

    private LinkedListInterface<Reservation> loadReservations() {
        LinkedListInterface<Reservation> reservations = new LinkedList<>();
        reservationDAO.loadAllReservations(reservations);
        return reservations;
    }

    private ReservationReportUI ui() {
        return new ReservationReportUI(scanner);
    }
}
