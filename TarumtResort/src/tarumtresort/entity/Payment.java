package tarumtresort.entity;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.adt.LinkedList;
import tarumtresort.entity.enums.*;
import java.time.LocalDateTime;

/**
 *
 * @author Wen Ling
 */

public class Payment implements Comparable<Payment> {
    private String paymentID;
    private double roomCharge;
    private double serviceCharge;
    private double tax;
    private double totalAmount;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private LocalDateTime paymentDateTime;
    private String reservationID;
    private LinkedListInterface<String> confirmationNumbers = new LinkedList<>();

    public Payment (String paymentID, double roomCharge, double serviceCharge,
                    double tax, double totalAmount, PaymentMethod paymentMehod,
                    PaymentStatus paymentStatus, LocalDateTime paymenDateTime, String reservationID
    ) {
        this.paymentID = paymentID;
        this.roomCharge = roomCharge;
        this.serviceCharge = serviceCharge;
        this.tax = tax;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMehod;
        this.paymentStatus = paymentStatus;
        this.paymentDateTime = paymenDateTime;
        this.reservationID = reservationID;
    }

    public String getPaymentID() {
        return paymentID;
    }

    public void setPaymentID(String paymentID) {
        this.paymentID = paymentID;
    }

    public double getRoomCharge() {
        return roomCharge;
    }

    public void setRoomCharge(double roomCharge) {
        this.roomCharge = roomCharge;
    }

    public double getServiceCharge() {
        return serviceCharge;
    }

    public void setServiceCharge(double serviceCharge) {
        this.serviceCharge = serviceCharge;
    }

    public double getTax() {
        return tax;
    }

    public void setTax(double tax) {
        this.tax = tax;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public LocalDateTime getPaymentDateTime() {
        return paymentDateTime;
    }

    public void setPaymentDateTime(LocalDateTime paymentDateTime) {
        this.paymentDateTime = paymentDateTime;
    }

    public String getReservationID() {
        return reservationID;
    }

    public void setReservationID(String reservationID) {
        this.reservationID = reservationID;
    }

    public LinkedListInterface<String> getConfirmationNumbers() {
        return confirmationNumbers;
    }

    public void addConfirmationNumber(String confirmationNumber) {
        confirmationNumbers.addBack(confirmationNumber);
    }
    
    @Override
    public int compareTo(Payment other) {
        return this.reservationID.compareTo(other.reservationID);
    }

    @Override
    public String toString() {

        return "Payment ID: " + paymentID
                + "\nReservation ID: " + reservationID
                + "\nRoom Charge: RM " + String.format("%.2f", roomCharge)
                + "\nService Charge: RM " + String.format("%.2f", serviceCharge)
                + "\nTax: RM " + String.format("%.2f", tax)
                + "\nTotal Amount: RM " + String.format("%.2f", totalAmount)
                + "\nPayment Method: " + paymentMethod
                + "\nPayment Status: " + paymentStatus
                + "\nPayment Date Time: " + paymentDateTime;
    }
}