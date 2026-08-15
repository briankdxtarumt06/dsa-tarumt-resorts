package tarumtresort.control;

import java.time.LocalDateTime;

import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.boundary.PaymentUI;
import tarumtresort.boundary.ReservationUI;
import tarumtresort.dao.PaymentDAO;
import tarumtresort.entity.Payment;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.Room;
import tarumtresort.entity.enums.PaymentMethod;
import tarumtresort.entity.enums.PaymentStatus;

public class PaymentControl {

     private final LinkedListInterface<Payment> paymentList = new LinkedList<>();

    // DAO
    private final PaymentDAO paymentDAO = new PaymentDAO();

    // UI
    private ReservationUI reservationUI = new ReservationUI();
    private PaymentUI paymentUI = new PaymentUI();
    private int paymentCounter = 1;

    public PaymentControl() {
        paymentDAO.loadFromFile(paymentList);
    }

    // called only from checkOut()
    public Payment processCheckoutPayment(Reservation reservation, Room room, boolean isLateCheckout) {

        double roomCharge = calculateRoomCharge(room, reservation);
        double serviceCharge = calculateServiceCharge(roomCharge);
        double tax = calculateTax(roomCharge, serviceCharge);
        double lateFee = isLateCheckout ? 50.0 : 0.0;
        double total = roomCharge + serviceCharge + tax + lateFee;

        paymentUI.printBill(roomCharge, serviceCharge, tax, lateFee, total);

        String[][] methodOptions = {
            {"1", "Cash"},
            {"2", "Credit Card"},
            {"3", "Debit Card"},
            {"4", "E-Wallet"},
            {"5", "Online Banking"},
            {"0", "Cancel"}
        };
        int methodChoice = reservationUI.showSubMenu("Select Payment Method:", methodOptions);
        if (methodChoice == 0) return null;

        PaymentMethod method = getPaymentMethod(methodChoice);

        Payment payment = new Payment(
            generatePaymentId(),
            roomCharge,
            serviceCharge,
            tax,
            total,
            method,
            PaymentStatus.PAID,
            LocalDateTime.now(),
            reservation.getConfirmationNumber()
        );

        paymentList.addBack(payment);
        paymentDAO.saveToFile(paymentList);
        return payment;
    }

    // HELPERS
    private double calculateRoomCharge(Room room, Reservation reservation) {
        return room.getPricePerNight() * reservation.getNumberOfNights();
    }

    private double calculateServiceCharge(double roomCharge) {
        return roomCharge * 0.10; // 10% service charge
    }

    private double calculateTax(double roomCharge, double serviceCharge) {
        return (roomCharge + serviceCharge) * 0.06; // 6% tax
    }

    private String generatePaymentId() {
        return String.format("PAY%03d", paymentCounter++);
    }

    private PaymentMethod getPaymentMethod(int choice) {
        switch (choice) {
            case 1: return PaymentMethod.CASH;
            case 2: return PaymentMethod.CREDIT_CARD;
            case 3: return PaymentMethod.DEBIT_CARD;
            case 4: return PaymentMethod.E_WALLET;
            case 5: return PaymentMethod.ONLINE_BANKING;
            default: return PaymentMethod.CASH;
        }
    }
}
