package tarumtresort.control;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.boundary.InquiryUI;
import tarumtresort.dao.GuestDAO;
import tarumtresort.dao.InquiryDAO;
import tarumtresort.dao.PaymentDAO;
import tarumtresort.dao.ReservationDAO;
import tarumtresort.entity.Guest;
import tarumtresort.entity.Inquiry;
import tarumtresort.entity.Payment;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.Room;
import tarumtresort.entity.Task;
import tarumtresort.entity.enums.InquiryStatus;
import tarumtresort.entity.enums.InquiryType;
import tarumtresort.entity.enums.ReservationStatus;
import tarumtresort.entity.enums.RoomStatus;
import tarumtresort.entity.enums.RoomType;
import tarumtresort.report.ReportChart;
import tarumtresort.report.ReportResult;

/**
 *
 * @author Wen Ling
 */
public class InquiryController {

    // controller (dependencies on other modules)
    private ReservationControl reservationControl = new ReservationControl();
    private HousekeepingController housekeepingController = new HousekeepingController();

    // list declared
    private LinkedListInterface<Inquiry> pendingInquiryList = new LinkedList<>();
    private LinkedListInterface<Inquiry> resolvedInquiryList = new LinkedList<>();
    private LinkedListInterface<Inquiry> getAllInquiries(InquiryStatus filterStatus) {
        LinkedListInterface<Inquiry> combined = new LinkedList<>();

        for (int i = 0; i < pendingInquiryList.size(); i++) {
            Inquiry inq = pendingInquiryList.get(i);
            if (filterStatus == null || inq.getStatus() == filterStatus) {
                combined.addBack(inq);
            }
        }
        for (int i = 0; i < resolvedInquiryList.size(); i++) {
            Inquiry inq = resolvedInquiryList.get(i);
            if (filterStatus == null || inq.getStatus() == filterStatus) {
                combined.addBack(inq);
            }
        }

        return combined;
    }

    // dao
    private static final InquiryDAO inquiryDAO = new InquiryDAO();
    private static final ReservationDAO reservationDAO = new ReservationDAO();
    private static final GuestDAO guestDAO = new GuestDAO();
    private static final PaymentDAO paymentDAO = new PaymentDAO();

    // ui
    private InquiryUI ui = new InquiryUI();

    private int inquiryCounter;

    // Constructor
    public InquiryController() {
        pendingInquiryList = inquiryDAO.retrievePendingInquiryList();
        resolvedInquiryList = inquiryDAO.retrieveResolvedInquiryList();
        inquiryCounter = pendingInquiryList.size() + resolvedInquiryList.size();
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
        pendingInquiryList.addSorted(newInquiry);
        inquiryDAO.savePendingInquiryList(pendingInquiryList);

        ui.printInquiryDetails(ui.buildInquiryDetails(newInquiry));
        ui.printSuccess();
    }

    // case 2
    public void processNextInquiry() {
        if (pendingInquiryList.isEmpty()) {
            ui.printMessage("No pending inquiries in queue.");
            return;
        }

        Inquiry inquiry = pendingInquiryList.removeFront();
        inquiry.setStatus(InquiryStatus.IN_PROGRESS);
        inquiryDAO.savePendingInquiryList(pendingInquiryList);

        ui.printInquiryDetails(ui.buildInquiryDetails(inquiry));

        Object extra = retrieveAdditionalInfo(inquiry);
        ui.printAdditionalInfo(inquiry, extra);

        if (inquiry.getStatus() == InquiryStatus.RESOLVED) {
            ui.printMessage("Inquiry automatically resolved (request forwarded to Housekeeping).");
            return;
        }

        if (ui.inputConfirmation("Mark this inquiry as resolved?")) {
            resolveInquiry(inquiry);
            ui.printSuccess();
        } else {
            inquiry.setStatus(InquiryStatus.PENDING);
            pendingInquiryList.addSorted(inquiry);
            inquiryDAO.savePendingInquiryList(pendingInquiryList);
            ui.printMessage("Inquiry returned to the queue.");
        }
    }

    // case 3
    public void viewPendingQueue() {
        ui.listAllInquiries(buildInquiryTableData(pendingInquiryList));
    }

    // case 4
    public void cancelInquiry() {
        if (pendingInquiryList.isEmpty()) {
            ui.printMessage("No pending inquiries to cancel.");
            return;
        }

        ui.listAllInquiries(buildInquiryTableData(pendingInquiryList));
        
        String inquiryId = ui.inputInquiryId();

        Inquiry target = getPendingInquiryById(inquiryId);
        if (target == null) {
            ui.printNotFound();
            return;
        }

        if (!ui.inputConfirmation("Cancel this inquiry?")) {
            return;
        }

        for (int i = 0; i < pendingInquiryList.size(); i++) {
            if (pendingInquiryList.get(i).getInquiryId().equals(inquiryId)) {
                pendingInquiryList.removeIndex(i);
                break;
            }
        }

        target.setStatus(InquiryStatus.CANCELLED);
        target.setResolvedTime(LocalDateTime.now());
        resolvedInquiryList.addBack(target);

        inquiryDAO.savePendingInquiryList(pendingInquiryList);
        inquiryDAO.saveResolvedInquiryList(resolvedInquiryList);

        ui.printCancelled();
    }

    // case 5
    public void viewAllInquiries() {
        InquiryStatus filter = ui.inputInquiryStatusFilter();
        ui.listAllInquiries(buildInquiryTableData(getAllInquiries(filter)));
    }

    // case 6
    public void generateReport() {
        int choice = ui.getReportMenuChoice();
        switch (choice) {
            case 1:
                InquiryType filter1 = ui.inputInquiryTypeFilter();
                ui.printReport(generatePendingInquiryReport(filter1));
                break;
            case 2:
                RoomType filter2 = ui.inputRoomTypeFilter();
                ui.printReport(generateRoomTypeInquiryDistributionReport(filter2));
                break;
            case 0:
            default:
                break;
        }
    }

    // Searching
    public Reservation searchReservationByConfirmationNumber(String confirmationNumber) {
        LinkedListInterface<Reservation> allReservations = new LinkedList<>();
        reservationDAO.loadAllReservations(allReservations);

        for (int i = 0; i < allReservations.size(); i++) {
            Reservation r = allReservations.get(i);
            if (r.getConfirmationNumber().equals(confirmationNumber)) {
                return r;
            }
        }
        return null;
    }

    public Guest searchGuestById(String guestId) {
        LinkedListInterface<Guest> guestList = new LinkedList<>();
        guestDAO.loadFromFile(guestList);

        for (int i = 0; i < guestList.size(); i++) {
            Guest g = guestList.get(i);
            if (g.getGuestId().equals(guestId)) {
                return g;
            }
        }
        return null;
    }

    public Payment searchPaymentByConfirmationNumber(String confirmationNumber) {
    LinkedListInterface<Payment> paymentList = new LinkedList<>();
    paymentDAO.loadFromFile(paymentList);

    for (int i = 0; i < paymentList.size(); i++) {
        Payment p = paymentList.get(i);
        LinkedListInterface<String> confirmationNumbers = p.getConfirmationNumbers();
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

    // check pending queue for an existing unresolved inquiry of the same type
    public Inquiry searchInquiryByConfirmationNumber(String confirmationNumber, InquiryType type) {
        for (int i = 0; i < pendingInquiryList.size(); i++) {
            Inquiry inq = pendingInquiryList.get(i);
            if (inq.getConfirmationNumber().equals(confirmationNumber)
                    && inq.getInquiryType() == type) {
                return inq;
            }
        }
        return null;
    }

    public Inquiry getPendingInquiryById(String inquiryId) {
        for (int i = 0; i < pendingInquiryList.size(); i++) {
            Inquiry inq = pendingInquiryList.get(i);
            if (inq.getInquiryId().equals(inquiryId)) {
                return inq;
            }
        }
        return null;
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
        if (reservation.getStatus() == ReservationStatus.WAITING) {
            RoomType type = reservation.getRoomTypeRequested();
            LinkedListInterface<Room> roomsOfType = reservationControl.getRoomsByType(type);
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
        resolvedInquiryList.addBack(inquiry);
        inquiryDAO.saveResolvedInquiryList(resolvedInquiryList);
    }

    // convert inquiry list to 2D table
    private String[][] buildInquiryTableData(LinkedListInterface<Inquiry> list) {
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

    // -------------------- reports --------------------

    /**
     * Pending Inquiry Overview Report (Inquiry + Reservation + Guest).
     * Filter: query type.
     */
    public ReportResult generatePendingInquiryReport(InquiryType filterType) {

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Inquiry ID", "Confirm No.", "Guest Name", "Type", "Priority", "Waiting"});

        int[] countPerPriority = new int[tarumtresort.entity.enums.InquiryPriority.values().length];

        for (int i = 0; i < pendingInquiryList.size(); i++) {
            Inquiry inq = pendingInquiryList.get(i);
            if (filterType != null && inq.getInquiryType() != filterType) {
                continue;
            }

            Guest guest = searchGuestById(inq.getGuestId());
            String guestName = guest == null ? "-" : guest.getName();

            rows.add(new String[]{
                    inq.getInquiryId(),
                    inq.getConfirmationNumber(),
                    guestName,
                    inq.getInquiryType().toString(),
                    inq.getInquiryType().getPriority().toString(),
                    formatDuration(calculateWaitingTime(inq))
            });

            countPerPriority[inq.getInquiryType().getPriority().ordinal()]++;
        }

        String[][] table = rows.toArray(new String[0][]);
        String[] summary = {"Total pending inquiries shown: " + (rows.size() - 1)};

        ReportChart chart = new ReportChart("Pending Inquiries by Priority");
        for (tarumtresort.entity.enums.InquiryPriority p : tarumtresort.entity.enums.InquiryPriority.values()) {
            chart.addBar(p.name(), countPerPriority[p.ordinal()], countPerPriority[p.ordinal()] + " inquiries");
        }
        List<ReportChart> charts = new ArrayList<>();
        charts.add(chart);

        return new ReportResult(table, summary, charts, null);
    }

    /**
     * Room Type Inquiry Distribution Report (Inquiry + Reservation + Room).
     * Filter: room type. Only RESOLVED (not CANCELLED) inquiries are counted.
     */
    public ReportResult generateRoomTypeInquiryDistributionReport(RoomType filterType) {

        RoomType[] types = RoomType.values();
        int[] totalCount = new int[types.length];
        int[] guestIdCount = new int[types.length];
        int[] roomAvailCount = new int[types.length];
        int[] billingCount = new int[types.length];
        int[] roomServiceCount = new int[types.length];

        for (int i = 0; i < resolvedInquiryList.size(); i++) {
            Inquiry inq = resolvedInquiryList.get(i);
            if (inq.getStatus() != InquiryStatus.RESOLVED) {
                continue; // exclude CANCELLED
            }
            Reservation reservation = searchReservationByConfirmationNumber(inq.getConfirmationNumber());
            if (reservation == null) {
                continue;
            }
            RoomType type = reservation.getRoomTypeRequested();
            if (filterType != null && type != filterType) {
                continue;
            }
            int idx = indexOfRoomType(types, type);
            totalCount[idx]++;
            switch (inq.getInquiryType()) {
                case GUESTIDENTIFICATION: guestIdCount[idx]++; break;
                case ROOMAVAILABILITY: roomAvailCount[idx]++; break;
                case BILLINGDETAILS: billingCount[idx]++; break;
                case ROOMSERVICE: roomServiceCount[idx]++; break;
            }
        }

        // sort room types by total inquiries descending (simple selection sort, small n)
        Integer[] order = new Integer[types.length];
        for (int i = 0; i < types.length; i++) order[i] = i;
        for (int i = 0; i < order.length - 1; i++) {
            int maxIdx = i;
            for (int j = i + 1; j < order.length; j++) {
                if (totalCount[order[j]] > totalCount[order[maxIdx]]) {
                    maxIdx = j;
                }
            }
            int temp = order[i]; order[i] = order[maxIdx]; order[maxIdx] = temp;
        }

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Room Type", "Total", "GuestID", "RoomAvail", "Billing", "RoomServ"});

        ReportChart chart = new ReportChart("Total Inquiries by Room Type");
        int grandTotal = 0;

        for (int idx : order) {
            if (totalCount[idx] == 0) continue;
            rows.add(new String[]{
                    types[idx].toString(),
                    String.valueOf(totalCount[idx]),
                    String.valueOf(guestIdCount[idx]),
                    String.valueOf(roomAvailCount[idx]),
                    String.valueOf(billingCount[idx]),
                    String.valueOf(roomServiceCount[idx])
            });
            chart.addBar(types[idx].toString(), totalCount[idx], totalCount[idx] + " inquiries");
            grandTotal += totalCount[idx];
        }

        String[][] table = rows.toArray(new String[0][]);
        String[] summary = {"Total resolved inquiries counted: " + grandTotal};

        List<ReportChart> charts = new ArrayList<>();
        charts.add(chart);

        return new ReportResult(table, summary, charts, null);
    }

    private int indexOfRoomType(RoomType[] types, RoomType target) {
        for (int i = 0; i < types.length; i++) {
            if (types[i] == target) return i;
        }
        return -1;
    }

    // -------------------- private helpers --------------------

    private String generateInquiryId() {
        inquiryCounter++;
        return "INQ" + String.format("%03d", inquiryCounter);
    }

    private Duration calculateWaitingTime(Inquiry inquiry) {
        return Duration.between(inquiry.getCreatedTime(), LocalDateTime.now());
    }

    private String formatDuration(Duration d) {
        long minutes = d.toMinutes();
        long seconds = d.minusMinutes(minutes).getSeconds();
        return minutes + "m " + seconds + "s";
    }

    // public static void main(String[] args) {
    //     InquiryController inquiryController = new InquiryController();
    //     inquiryController.runInquiryModule();
    // }
}