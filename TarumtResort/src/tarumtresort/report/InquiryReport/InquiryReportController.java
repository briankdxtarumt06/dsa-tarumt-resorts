package tarumtresort.report.InquiryReport;

import java.time.LocalDateTime;
import java.util.Scanner;
import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;
import tarumtresort.dao.GuestDAO;
import tarumtresort.dao.InquiryDAO;
import tarumtresort.dao.ReservationDAO;
import tarumtresort.entity.Guest;
import tarumtresort.entity.Inquiry;
import tarumtresort.entity.Reservation;
import tarumtresort.utility.ConsoleUtil;

/**
 *
 * @author Wen Ling
 *
 */
public class InquiryReportController {

    private final InquiryDAO inquiryDAO = new InquiryDAO();
    private final GuestDAO guestDAO = new GuestDAO();
    private final ReservationDAO reservationDAO = new ReservationDAO();

    private final Scanner scanner;

    public InquiryReportController(Scanner scanner) {
        this.scanner = scanner;
    }

    public void generatePendingInquiryReport() {
        InquiryReportUI ui = ui();
        LocalDateTime[] range = ui.inputOptionalDateTimeRange("inquiry created time");

        ListInterface<Inquiry> inquiries = inquiryDAO.retrieveInquiryList();
        ListInterface<Guest> guests = new DoublyLinkedList<>();
        guestDAO.loadFromFile(guests);

        PendingInquiryReport.Result result = new PendingInquiryReport(inquiries, guests)
                .generate(range[0], range[1]);

        if (isEmpty(result.getTable())) {
            ConsoleUtil.printError("No pending inquiries found for the selected date range.");
            ui.pressEnterToContinue();
            return;
        }

        new PendingInquiryUI(ui).render(result);
        ui.pressEnterToContinue();
    }

    public void generateRoomTypeInquiryDistributionReport() {
        InquiryReportUI ui = ui();
        LocalDateTime[] range = ui.inputOptionalDateTimeRange("inquiry created time");

        ListInterface<Inquiry> inquiries = inquiryDAO.retrieveInquiryList();
        ListInterface<Reservation> reservations = new DoublyLinkedList<>();
        reservationDAO.loadAllReservations(reservations);

        RoomTypeInquiryDistributionReport.Result result =
                new RoomTypeInquiryDistributionReport(inquiries, reservations).generate(range[0], range[1]);

        if (isEmpty(result.getTable())) {
            ConsoleUtil.printError("No resolved inquiries found for the selected date range.");
            ui.pressEnterToContinue();
            return;
        }

        new RoomTypeInquiryDistributionUI(ui).render(result);
        ui.pressEnterToContinue();
    }

    private boolean isEmpty(String[][] table) {
        return table == null || table.length <= 1;
    }

    private InquiryReportUI ui() {
        return new InquiryReportUI(scanner);
    }
}