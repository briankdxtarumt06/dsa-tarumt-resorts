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
import tarumtresort.entity.enums.RoomType;

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

    public Payment processGroupCheckoutPayment(LinkedListInterface<Reservation> reservations, ReservationControl reservationControl, boolean isLateCheckout, PaymentMethod method) {
        double totalRoomCharge = 0;
        for (int i = 0; i < reservations.size(); i++) {
            Reservation r = reservations.get(i);
            double pricePerNight = reservationControl.getPriceByRoomType(r.getRoomTypeRequested());
            totalRoomCharge += pricePerNight * r.getNumberOfNights();
        }

        double serviceCharge = totalRoomCharge * 0.10;
        double tax = (totalRoomCharge + serviceCharge) * 0.06;
        double lateFee = isLateCheckout ? 50.0 : 0.0;
        double total = totalRoomCharge + serviceCharge + tax + lateFee;

        paymentUI.printBill(totalRoomCharge, serviceCharge, tax, lateFee, total);

        Payment payment = new Payment(
            generatePaymentId(),
            totalRoomCharge,
            serviceCharge,
            tax,
            total,
            method,
            PaymentStatus.PAID,
            LocalDateTime.now(),
            reservations.get(0).getConfirmationNumber()
        );

        for (int i = 0; i < reservations.size(); i++) {
            payment.addConfirmationNumber(reservations.get(i).getConfirmationNumber());
        }

        paymentList.addBack(payment);
        paymentDAO.saveToFile(paymentList);
        return payment;
    }

    // HELPERS
    private double calculateRoomCharge(Room room, Reservation reservation) {
        return room.getPricePerNight() * reservation.getNumberOfNights();
    }

    // remove the paument if guset cance lthe reservation
    public void removePendingPayment(String confirmationNumber) {
        for (int i = 0; i < paymentList.size(); i++) {
            Payment p = paymentList.get(i);
            if (p.getReservationID().equals(confirmationNumber) && p.getPaymentStatus() == PaymentStatus.UNPAID) {
                paymentList.removeIndex(i);
                paymentDAO.saveToFile(paymentList);
                return;
            }
        }
    }

    private String generatePaymentId() {
        return String.format("PAY%03d", paymentCounter++);
    }

    public PaymentMethod askPaymentMethod(tarumtresort.boundary.ReservationUI reservationUI) {
        String[][] methodOptions = {
            {"1", "Cash"},
            {"2", "Credit Card"},
            {"3", "Debit Card"},
            {"4", "E-Wallet"},
            {"5", "Online Banking"},
            {"0", "Cancel"}
        };
        int methodChoice = reservationUI.showSubMenu("Select Payment Method:", methodOptions);
        switch (methodChoice) {
            case 1: return PaymentMethod.CASH;
            case 2: return PaymentMethod.CREDIT_CARD;
            case 3: return PaymentMethod.DEBIT_CARD;
            case 4: return PaymentMethod.E_WALLET;
            case 5: return PaymentMethod.ONLINE_BANKING;
            default: return null;
        }
    }

    private double calculateServiceCharge(double roomCharge) {
        return roomCharge * 0.10; // 10% service charge
    }

    private double calculateTax(double roomCharge, double serviceCharge) {
        return (roomCharge + serviceCharge) * 0.06; // 6% tax
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
