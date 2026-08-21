package tarumtresort.report.InquiryReport;

import java.util.Scanner;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.dao.GuestDAO;
import tarumtresort.dao.InquiryDAO;
import tarumtresort.dao.ReservationDAO;
import tarumtresort.entity.Guest;
import tarumtresort.entity.Inquiry;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.enums.InquiryType;
import tarumtresort.entity.enums.RoomType;
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
        InquiryType filterType = ui().inputInquiryTypeFilter();

        LinkedListInterface<Inquiry> inquiries = inquiryDAO.retrieveInquiryList();
        LinkedListInterface<Guest> guests = new LinkedList<>();
        guestDAO.loadFromFile(guests);

        PendingInquiryReport.Result result = new PendingInquiryReport(inquiries, guests).generate(filterType);

        if (isEmpty(result.getTable())) {
            ConsoleUtil.printError("No pending inquiries found"
                    + (filterType == null ? "." : " for query type " + filterType + "."));
            ui().pressEnterToContinue();
            return;
        }

        new PendingInquiryUI(ui()).render(result);
        ui().pressEnterToContinue();
    }

    public void generateRoomTypeInquiryDistributionReport() {
        RoomType filterType = ui().inputRoomTypeFilter();

        LinkedListInterface<Inquiry> inquiries = inquiryDAO.retrieveInquiryList();
        LinkedListInterface<Reservation> reservations = new LinkedList<>();
        reservationDAO.loadAllReservations(reservations);

        RoomTypeInquiryDistributionReport.Result result =
                new RoomTypeInquiryDistributionReport(inquiries, reservations).generate(filterType);

        if (isEmpty(result.getTable())) {
            ConsoleUtil.printError("No resolved inquiries found"
                    + (filterType == null ? "." : " for room type " + filterType + "."));
            ui().pressEnterToContinue();
            return;
        }

        new RoomTypeInquiryDistributionUI(ui()).render(result);
        ui().pressEnterToContinue();
    }

    private boolean isEmpty(String[][] table) {
        return table == null || table.length <= 1;
    }

    private InquiryReportUI ui() {
        return new InquiryReportUI(scanner);
    }
}