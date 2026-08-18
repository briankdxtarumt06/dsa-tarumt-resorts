package tarumtresort.control;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.boundary.PaymentUI;
import tarumtresort.boundary.ReservationUI;
import tarumtresort.dao.PaymentDAO;
import tarumtresort.entity.Payment;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.enums.PaymentMethod;
import tarumtresort.entity.enums.PaymentStatus;

public class PaymentControl {

    private final LinkedListInterface<Payment> paymentList = new LinkedList<>();

    // DAO
    private final PaymentDAO paymentDAO = new PaymentDAO();

    // UI
    private ReservationUI reservationUI = new ReservationUI();
    private PaymentUI paymentUI = new PaymentUI();

    public PaymentControl() {
        paymentDAO.loadFromFile(paymentList);
    }

    // called from bookRoom() - one combined payment for the whole booking session
    public Payment processBookingPayment(LinkedListInterface<Reservation> reservations, RoomControl roomControl) {
        if (reservations == null || reservations.size() == 0) {
            return null;
        }

        double totalRoomCharge = 0;
        for (int i = 0; i < reservations.size(); i++) {
            Reservation r = reservations.get(i);
            double pricePerNight = roomControl.getPriceByRoomType(r.getRoomTypeRequested());
            totalRoomCharge += pricePerNight * r.getNumberOfNights();
        }

        double serviceCharge = totalRoomCharge * 0.10;
        double tax = (totalRoomCharge + serviceCharge) * 0.06;
        double total = totalRoomCharge + serviceCharge + tax;

        paymentUI.printBill(totalRoomCharge, serviceCharge, tax, 0.0, total);

        PaymentMethod method = askPaymentMethod(reservationUI);
        if (method == null) {
            return null; // guest cancelled payment
        }

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

    // called from checkOut() - late check-out settlement:
    // extra nights at the room's rate (+10% service, +6% tax) + RM50 flat fee per late room
    public Payment processLateCheckoutPayment(LinkedListInterface<Reservation> reservations, RoomControl roomControl) {
        if (reservations == null || reservations.size() == 0) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();

        double extraRoomCharge = 0;
        double lateFee = 0;
        for (int i = 0; i < reservations.size(); i++) {
            Reservation r = reservations.get(i);
            LocalDate expected = r.getTimestamps().getExpectedCheckOutDate();
            long extraDays = ChronoUnit.DAYS.between(expected, now.toLocalDate());
            if (extraDays > 0) {
                extraRoomCharge += extraDays * roomControl.getPriceByRoomType(r.getRoomTypeRequested());
            }
            lateFee += 50.0; // flat fee per late room
        }

        double serviceCharge = extraRoomCharge * 0.10;
        double tax = (extraRoomCharge + serviceCharge) * 0.06;
        double total = extraRoomCharge + serviceCharge + tax + lateFee;

        paymentUI.printBill(extraRoomCharge, serviceCharge, tax, lateFee, total);

        PaymentMethod method = askPaymentMethod(reservationUI);
        if (method == null) {
            return null; // fee not paid - checkout still proceeds with a warning
        }

        Payment payment = new Payment(
            generatePaymentId(),
            extraRoomCharge,
            serviceCharge,
            tax,
            total,
            method,
            PaymentStatus.PAID,
            now,
            reservations.get(0).getConfirmationNumber()
        );

        for (int i = 0; i < reservations.size(); i++) {
            payment.addConfirmationNumber(reservations.get(i).getConfirmationNumber());
        }

        paymentList.addBack(payment);
        paymentDAO.saveToFile(paymentList);
        return payment;
    }

        // refund policy: 100% refund if cancelled at least 24 hours before the
    // 12pm check-in moment; 0% refund if cancelled within the last 24 hours.
    // Called from cancelReservation() after the reservation is removed.
    public double refundReservation(Reservation r, RoomControl roomControl) {
        if (r == null || r.getTimestamps() == null || r.getTimestamps().getExpectedCheckInDate() == null) {
            return 0.0;
        }

        LocalDateTime checkInMoment = r.getTimestamps().getExpectedCheckInDate().atTime(12, 0);
        if (!LocalDateTime.now().isBefore(checkInMoment.minusHours(24))) {
            return 0.0; // cancelled within 24 hours of check-in - no refund
        }

        Payment payment = findPaymentByConfirmationNumber(r.getConfirmationNumber());
        if (payment == null) {
            return 0.0;
        }

        // recompute this room's share of the bill with the same formula used at booking
        double roomCharge = roomControl.getPriceByRoomType(r.getRoomTypeRequested()) * r.getNumberOfNights();
        double serviceCharge = roomCharge * 0.10;
        double tax = (roomCharge + serviceCharge) * 0.06;
        double share = roomCharge + serviceCharge + tax;

        double remaining = payment.getTotalAmount() - payment.getRefundedAmount();
        if (remaining < 0.005) {
            return 0.0; // already fully refunded (within rounding tolerance)
        }
        double refund = Math.min(share, remaining);
        if (refund < 0.005) {
            return 0.0;
        }

        payment.setRefundedAmount(payment.getRefundedAmount() + refund);
        payment.setRefundDateTime(LocalDateTime.now());
        paymentDAO.saveToFile(paymentList);
        return refund;
    }

    // true if any payment record covers this confirmation number
    public boolean hasPaymentFor(String confirmationNumber) {
        return findPaymentByConfirmationNumber(confirmationNumber) != null;
    }

    private Payment findPaymentByConfirmationNumber(String confirmationNumber) {
        for (int i = 0; i < paymentList.size(); i++) {
            Payment p = paymentList.get(i);
            if (confirmationNumber.equals(p.getReservationID())) {
                return p;
            }
            LinkedListInterface<String> numbers = p.getConfirmationNumbers();
            if (numbers != null) {
                for (int j = 0; j < numbers.size(); j++) {
                    if (confirmationNumber.equals(numbers.get(j))) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

    public void displayPaymentRecords() {
        paymentUI.printPaymentRecords(paymentList);
    }

    public LinkedListInterface<Payment> getPaymentList() {
        return paymentList;
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

    // scan existing payments so IDs never collide across app restarts
    private String generatePaymentId() {
        int max = 0;
        for (int i = 0; i < paymentList.size(); i++) {
            String id = paymentList.get(i).getPaymentID();
            if (id != null && id.startsWith("PAY")) {
                try {
                    max = Math.max(max, Integer.parseInt(id.substring(3)));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return String.format("PAY%03d", max + 1);
    }
}