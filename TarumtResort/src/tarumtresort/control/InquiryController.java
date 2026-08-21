package tarumtresort.control;

import java.time.LocalDateTime;
import java.util.Scanner;
import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;
import tarumtresort.boundary.InquiryUI;
import tarumtresort.boundary.ReservationUI;
import tarumtresort.dao.GuestDAO;
import tarumtresort.dao.InquiryDAO;
import tarumtresort.dao.PaymentDAO;
import tarumtresort.dao.ReservationDAO;
import tarumtresort.entity.Guest;
import tarumtresort.entity.Inquiry;
import tarumtresort.entity.Payment;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.Room;
import tarumtresort.entity.enums.InquiryStatus;
import tarumtresort.entity.enums.InquiryType;
import tarumtresort.entity.enums.ReservationStatus;
import tarumtresort.entity.enums.RoomStatus;
import tarumtresort.entity.enums.RoomType;
import tarumtresort.report.InquiryReport.InquiryReportController;

/**
 *
 * @author Wen Ling
 */
public class InquiryController {

    private static final int PAGE_SIZE = 20;

    // controller (dependencies on other modules)
    private ReservationControl reservationControl = new ReservationControl();
    private HousekeepingController housekeepingController = new HousekeepingController();

    // ui
    private final Scanner scanner = new Scanner(System.in);
    private InquiryUI ui = new InquiryUI(scanner);
    private ReservationUI reservationUI = new ReservationUI();

    // adt
    private ListInterface<Inquiry> inquiryList = new DoublyLinkedList<>();

    // dao
    private static final InquiryDAO inquiryDAO = new InquiryDAO();
    private static final ReservationDAO reservationDAO = new ReservationDAO();
    private static final GuestDAO guestDAO = new GuestDAO();
    private static final PaymentDAO paymentDAO = new PaymentDAO();

    // report generation
    private InquiryReportController inquiryReportController = new InquiryReportController(scanner);

    private int inquiryCounter;

    // Constructor
    public InquiryController() {
        inquiryList = inquiryDAO.retrieveInquiryList();
        inquiryCounter = inquiryList.size();
    }

    // entry point for the inquiry module
    public void runInquiryModule() {
        try {
            int choice;

            do {
                choice = ui.getMenuChoice();

                switch (choice) {
                    case 1:
                        createInquiry();
                        break;
                    case 2:
                        processNextInquiry();
                        break;
                    case 3:
                        viewPendingQueue();
                        break;
                    case 4:
                        cancelInquiry();
                        break;
                    case 5:
                        viewAllInquiries();
                        break;
                    case 6:
                        generateReport();
                        break;
                    case 0:
                        ui.printExitMessage();
                        break;
                    default:
                        ui.printInvalidChoice();
                }

                if (choice != 0) {
                    ui.pressEnterToContinue();
                }
            } while (choice != 0);
        } catch (Exception e) {
            System.err.println("\n An unexpected error occurred in Inquiry module: " + e.getMessage());
        }
    }

    // case 1
    public void createInquiry() {
        String confirmationNumber = ui.inputConfirmationNumber();

        Reservation reservation = searchReservationByConfirmationNumber(confirmationNumber);
        if (reservation == null) {
            ui.printNotFound();
            return;
        }

        InquiryType type = ui.inputInquiryType();

        Inquiry duplicate = searchInquiryByConfirmationNumber(confirmationNumber, type);
        if (duplicate != null) {
            ui.printMessage("An unresolved " + type + " inquiry already exists ("
                    + duplicate.getInquiryId() + ").");
            return;
        }

        String description = ui.inputDescription();

        String inquiryId = generateInquiryId();
        Inquiry newInquiry = new Inquiry(inquiryId, confirmationNumber, reservation.getGuestId(),
                type, description);

        // addSorted = enqueue: inserts by priority rank, then by createdTime (FCFS)
        inquiryList.addSorted(newInquiry);
        inquiryDAO.saveInquiryList(inquiryList);

        ui.printInquiryDetails(ui.buildInquiryDetails(newInquiry));
        ui.printSuccess();
    }

    // case 2
    public void processNextInquiry() {
        Inquiry inquiry = getNextPendingInquiry();
        if (inquiry == null) {
            ui.printMessage("No pending inquiries in queue.");
            return;
        }

        inquiry.setStatus(InquiryStatus.IN_PROGRESS);
        inquiryDAO.saveInquiryList(inquiryList);

        ui.printInquiryDetails(ui.buildInquiryDetails(inquiry));

        Object extra = retrieveAdditionalInfo(inquiry);
        ui.printAdditionalInfo(inquiry, extra);

        if (extra instanceof Guest) {
            Guest guest = (Guest) extra;
            if (ui.inputConfirmation("View Reservation?")) {
                reservationUI.printGuestReservationHistory(guest.getReservations());
            }
        }

        if (inquiry.getStatus() == InquiryStatus.RESOLVED) {
            ui.printMessage("Inquiry automatically resolved (request forwarded to Housekeeping).");
            return;
        }

        if (ui.inputConfirmation("Mark this inquiry as resolved?")) {
            resolveInquiry(inquiry);
            ui.printSuccess();
        } else {
            inquiry.setStatus(InquiryStatus.PENDING);
            inquiryDAO.saveInquiryList(inquiryList);
            ui.printMessage("Inquiry returned to the queue.");
        }
    }

    // case 3
    public void viewPendingQueue() {
        ui.listAllInquiries(buildInquiryTableData(getFilteredInquiries(InquiryStatus.PENDING, null)));
    }

    // case 4
    public void cancelInquiry() {
        ListInterface<Inquiry> pendingOnly = getFilteredInquiries(InquiryStatus.PENDING, null);
        if (pendingOnly.isEmpty()) {
            ui.printMessage("No pending inquiries to cancel.");
            return;
        }

        ui.listAllInquiries(buildInquiryTableData(pendingOnly));

        String inquiryId = ui.inputInquiryId();

        Inquiry target = getPendingInquiryById(inquiryId);
        if (target == null) {
            ui.printNotFound();
            return;
        }

        if (!ui.inputConfirmation("Cancel this inquiry?")) {
            return;
        }

        target.setStatus(InquiryStatus.CANCELLED);
        target.setResolvedTime(LocalDateTime.now());

        inquiryDAO.saveInquiryList(inquiryList);

        ui.printCancelled();
    }

    // case 5
    public void viewAllInquiries() {
        InquiryStatus statusFilter = null;
        InquiryType typeFilter = null;
        int page = 0;

        while (true) {
            ListInterface<Inquiry> display = getFilteredInquiries(statusFilter, typeFilter);

            boolean hasFilter = statusFilter != null || typeFilter != null;
            int pageCount = Math.max(1, (display.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            if (page >= pageCount) {
                page = pageCount - 1;
            }
            ListInterface<Inquiry> pageList = pageOfInquiries(display, page);
            int choice = ui.printInquiryListMenu(pageList, page, pageCount, hasFilter);

            if (choice == 0) {
                break;
            }

            int action = 1;
            if (choice == action++) { // 1. View Details
                viewInquiryDetails(pageList);
            } else if (choice == action++) { // 2. Filter by Inquiry Status
                statusFilter = ui.inputInquiryStatusFilter();
                page = 0;
            } else if (choice == action++) { // 3. Filter by Inquiry Type
                typeFilter = ui.inputInquiryTypeFilter();
                page = 0;
            } else if (choice == action++) { // 4. Search by Inquiry ID
                searchByInquiryId();
            } else if (choice == action++) { // 5. Search by Confirmation Number
                searchByConfirmationNumber();
            } else {
                boolean matched = false;
                if (page < pageCount - 1) {
                    matched = choice == action;
                    action++;
                    if (matched)
                        page++;
                }
                if (!matched && page > 0) {
                    matched = choice == action;
                    action++;
                    if (matched)
                        page--;
                }
                if (!matched && hasFilter) {
                    matched = choice == action;
                    action++;
                    if (matched) {
                        statusFilter = null;
                        typeFilter = null;
                        page = 0;
                    }
                }
            }
        }
    }

    private void viewInquiryDetails(ListInterface<Inquiry> pageList) {
        if (pageList.isEmpty()) {
            ui.printNotFound();
            ui.pressEnterToContinue();
            return;
        }
        int num = ui.inputListIndex("inquiry", pageList.size());
        if (num == 0) {
            return;
        }
        Inquiry inquiry = pageList.get(num - 1);
        ui.printInquiryDetails(ui.buildInquiryDetails(inquiry));
        ui.pressEnterToContinue();
    }

    private void searchByInquiryId() {
        String inquiryId = ui.inputInquiryId();
        Inquiry found = searchInquiryById(inquiryId);
        if (found == null) {
            ui.printNotFound();
            ui.pressEnterToContinue();
            return;
        }
        ListInterface<Inquiry> result = new DoublyLinkedList<>();
        result.addBack(found);
        ui.listAllInquiries(buildInquiryTableData(result));
        ui.pressEnterToContinue();
    }

    private void searchByConfirmationNumber() {
        String confirmationNumber = ui.inputConfirmationNumber();
        ListInterface<Inquiry> matches = searchInquiriesByConfirmationNumber(confirmationNumber);
        if (matches.isEmpty()) {
            ui.printNotFound();
            ui.pressEnterToContinue();
            return;
        }
        ui.listAllInquiries(buildInquiryTableData(matches));
        ui.pressEnterToContinue();
    }

    // case 6
    public void generateReport() {
        int choice = ui.getReportMenuChoice();
        switch (choice) {
            case 1:
                inquiryReportController.generatePendingInquiryReport();
                break;
            case 2:
                inquiryReportController.generateRoomTypeInquiryDistributionReport();
                break;
            case 0:
            default:
                break;
        }
    }

    // matches any status (PENDING / IN_PROGRESS / RESOLVED / CANCELLED)
    public Inquiry searchInquiryById(String inquiryId) {
        for (int i = 0; i < inquiryList.size(); i++) {
            Inquiry inq = inquiryList.get(i);
            if (inq.getInquiryId().equals(inquiryId)) {
                return inq;
            }
        }
        return null;
    }

    public ListInterface<Inquiry> searchInquiriesByConfirmationNumber(String confirmationNumber) {
        ListInterface<Inquiry> matches = new DoublyLinkedList<>();
        for (int i = 0; i < inquiryList.size(); i++) {
            Inquiry inq = inquiryList.get(i);
            if (inq.getConfirmationNumber().equals(confirmationNumber)) {
                matches.addSorted(inq);
            }
        }
        return matches;
    }

    // Searching
    public Reservation searchReservationByConfirmationNumber(String confirmationNumber) {
        ListInterface<Reservation> allReservations = new DoublyLinkedList<>();
        reservationDAO.loadAllReservations(allReservations);

        for (int i = 0; i < allReservations.size(); i++) {
            Reservation r = allReservations.get(i);
            // skip soft-deleted reservations
            if (r.isDeleted()) {
                continue;
            }
            if (r.getConfirmationNumber().equals(confirmationNumber)) {
                return r;
            }
        }
        return null;
    }

    public Guest searchGuestById(String guestId) {
        ListInterface<Guest> guestList = new DoublyLinkedList<>();
        guestDAO.loadFromFile(guestList);

        Guest found = null;
        for (int i = 0; i < guestList.size(); i++) {
            Guest g = guestList.get(i);
            if (g.getGuestId().equals(guestId)) {
                found = g;
                break;
            }
        }
        if (found != null) {
            attachReservations(found);
        }
        return found;
    }

    private void attachReservations(Guest guest) {
        ListInterface<Reservation> allReservations = new DoublyLinkedList<>();
        reservationDAO.loadAllReservations(allReservations);

        for (int i = 0; i < allReservations.size(); i++) {
            Reservation r = allReservations.get(i);
            if (r.getGuestId() != null && r.getGuestId().equals(guest.getGuestId())) {
                guest.getReservations().addBack(r);
            }
        }
    }

    public Payment searchPaymentByConfirmationNumber(String confirmationNumber) {
        ListInterface<Payment> paymentList = new DoublyLinkedList<>();
        paymentDAO.loadFromFile(paymentList);

        for (int i = 0; i < paymentList.size(); i++) {
            Payment p = paymentList.get(i);
            ListInterface<String> confirmationNumbers = p.getConfirmationNumbers();
            if (confirmationNumbers == null) {
                continue;
            }

            for (int j = 0; j < confirmationNumbers.size(); j++) {
                if (confirmationNumbers.get(j).equals(confirmationNumber)) {
                    return p;
                }
            }
        }
        return null;
    }

    // check for an existing PENDING inquiry of the same type
    public Inquiry searchInquiryByConfirmationNumber(String confirmationNumber, InquiryType type) {
        for (int i = 0; i < inquiryList.size(); i++) {
            Inquiry inq = inquiryList.get(i);
            if (inq.getStatus() == InquiryStatus.PENDING
                    && inq.getConfirmationNumber().equals(confirmationNumber)
                    && inq.getInquiryType() == type) {
                return inq;
            }
        }
        return null;
    }

    public Inquiry getPendingInquiryById(String inquiryId) {
        for (int i = 0; i < inquiryList.size(); i++) {
            Inquiry inq = inquiryList.get(i);
            if (inq.getStatus() == InquiryStatus.PENDING && inq.getInquiryId().equals(inquiryId)) {
                return inq;
            }
        }
        return null;
    }

    // first PENDING inquiry in list order == earliest priority/createdTime,
    private Inquiry getNextPendingInquiry() {
        for (int i = 0; i < inquiryList.size(); i++) {
            Inquiry inq = inquiryList.get(i);
            if (inq.getStatus() == InquiryStatus.PENDING) {
                return inq;
            }
        }
        return null;
    }

    // filterStatus == null / filterType == null means that dimension is unfiltered
    private ListInterface<Inquiry> getFilteredInquiries(InquiryStatus filterStatus, InquiryType filterType) {
        ListInterface<Inquiry> result = new DoublyLinkedList<>();
        for (int i = 0; i < inquiryList.size(); i++) {
            Inquiry inq = inquiryList.get(i);
            if (filterStatus != null && inq.getStatus() != filterStatus) {
                continue;
            }
            if (filterType != null && inq.getInquiryType() != filterType) {
                continue;
            }
            result.addBack(inq);
        }
        return result;
    }

    private ListInterface<Inquiry> pageOfInquiries(ListInterface<Inquiry> source, int page) {
        ListInterface<Inquiry> result = new DoublyLinkedList<>();
        int startIndex = page * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, source.size());
        for (int i = startIndex; i < endIndex; i++) {
            result.addBack(source.get(i));
        }
        return result;
    }

    // PROCESSING HELPERS
    private Object retrieveAdditionalInfo(Inquiry inquiry) {
        Reservation reservation = searchReservationByConfirmationNumber(inquiry.getConfirmationNumber());
        if (reservation == null) {
            return null;
        }

        switch (inquiry.getInquiryType()) {
            case GUESTIDENTIFICATION:
                return searchGuestById(reservation.getGuestId());

            case BILLINGDETAILS:
                return searchPaymentByConfirmationNumber(inquiry.getConfirmationNumber());

            case ROOMAVAILABILITY:
                return buildRoomAvailabilityInfo(reservation);

            case ROOMSERVICE:
                // Author: Brian Kam Ding Xian
                String taskId = housekeepingController.createRoomServiceTask(reservation.getRoomId());
                if (taskId != null) {
                    resolveInquiry(inquiry);
                }
                return housekeepingController.getTaskById(taskId);

            default:
                return null;
        }
    }

    // Inquiry -> Reservation -> Room dependency chain
    private String buildRoomAvailabilityInfo(Reservation reservation) {
        if (reservation.getStatus() == ReservationStatus.WAITING
                || reservation.getStatus() == ReservationStatus.BOOKED) {
            RoomType type = reservation.getRoomTypeRequested();
            ListInterface<Room> roomsOfType = reservationControl.getRoomsByType(type);
            int availableCount = 0;
            for (int i = 0; i < roomsOfType.size(); i++) {
                if (roomsOfType.get(i).getRoomStatus() == RoomStatus.AVAILABLE) {
                    availableCount++;
                }
            }
            return ui.buildRoomAvailabilityInfo(reservation, availableCount, roomsOfType.size(), null);
        } else {
            Room room = reservationControl.getRoomById(reservation.getRoomId());
            return ui.buildRoomAvailabilityInfo(reservation, 0, 0, room);
        }
    }

    // IN_PROGRESS -> RESOLVED
    private void resolveInquiry(Inquiry inquiry) {
        if (inquiry.getStatus() == InquiryStatus.RESOLVED) {
            return; // already resolved (e.g. ROOMSERVICE auto-resolve)
        }
        inquiry.setStatus(InquiryStatus.RESOLVED);
        inquiry.setResolvedTime(LocalDateTime.now());
        inquiryDAO.saveInquiryList(inquiryList);
    }

    // convert inquiry list to 2D table
    private String[][] buildInquiryTableData(ListInterface<Inquiry> list) {
        String[][] data = new String[list.size() + 1][5]; // +1 row for the header; size() = record count
        data[0] = new String[]{"Inquiry ID", "Confirm No.", "Type", "Priority", "Status"};
        for (int i = 0; i < list.size(); i++) {
            Inquiry inq = list.get(i);
            data[i + 1] = new String[]{
                    inq.getInquiryId(),
                    inq.getConfirmationNumber(),
                    inq.getInquiryType().toString(),
                    inq.getInquiryType().getPriority().toString(),
                    inq.getStatus().toString()
            };
        }
        return data;
    }

    // -------------------- private helpers --------------------

    private String generateInquiryId() {
        inquiryCounter++;
        return "INQ" + String.format("%03d", inquiryCounter);
    }

    // public static void main(String[] args) {
    //     InquiryController inquiryController = new InquiryController();
    //     inquiryController.runInquiryModule();
    // }
}