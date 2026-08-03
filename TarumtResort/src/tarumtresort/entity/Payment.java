/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarumtresort.entity;

import java.time.LocalDateTime;

public class Payment {
    private String paymentID;
    private double roomCharge;
    private double serviceCharge;
    private double tax;
    private double totalAmount;
    private String paymentMethod;
    private String paymentStatus;
    private LocalDateTime paymentTimeDate;
    private String reservationID;

    public Payment (String paymentID, double roomCharge, double serviceCharge,
                    double tax, double totalAmount, String paymentMehod,
                    String paymentStatus, LocalDateTime paymenDateTime, String reservationID
    ) {
        this.paymentID = paymentID;
        this.roomCharge = roomCharge;
        this.serviceCharge = serviceCharge;
        this.tax = tax;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMehod;
        this.paymentStatus = paymentStatus;
        this.paymentTimeDate = paymenDateTime;
        this.reservationID = reservationID;

        calculateTotalAmount();
    }

    private void calculateTotalAmount() {
        this.totalAmount = roomCharge + serviceCharge + tax;
    }

    public String getPaymentID() {
        return paymentID;
    }


    public double getRoomCharge() {
        return roomCharge;
    }


    public double getServiceCharge() {
        return serviceCharge;
    }


    public double getTax() {
        return tax;
    }


    public double getTotalAmount() {
        return totalAmount;
    }


    public String getPaymentMethod() {
        return paymentMethod;
    }


    public String getPaymentStatus() {
        return paymentStatus;
    }


    public LocalDateTime getPaymentTimeDate() {
        return paymentTimeDate;
    }


    public String getReservationID() {
        return reservationID;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }


    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }


    public void setRoomCharge(double roomCharge) {
        this.roomCharge = roomCharge;
        calculateTotalAmount();
    }


    public void setServiceCharge(double serviceCharge) {
        this.serviceCharge = serviceCharge;
        calculateTotalAmount();
    }


    public void setTax(double tax) {
        this.tax = tax;
        calculateTotalAmount();
    }

    @Override
    public String toString() {

        return "Payment ID: " + paymentID
                + "\nReservation ID: " + reservationID
                + "\nRoom Charge: RM" + roomCharge
                + "\nService Charge: RM" + serviceCharge
                + "\nTax: RM" + tax
                + "\nTotal Amount: RM" + totalAmount
                + "\nPayment Method: " + paymentMethod
                + "\nPayment Status: " + paymentStatus
                + "\nPayment Date: " + paymentTimeDate;
    }
}
