/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarumtresort.entity;
import tarumtresort.entity.enums.*;

import java.time.LocalDateTime;

/**
 *
 * @author Wen Ling
 */
public class Inquiry {

    private String inquiryID;
    private InquiryType inquiryType;
    private LocalDateTime inquiryDateTime;
    private InquiryPriority inquiryPriority;
    private String description;
    private InquiryStatus status;
    private String reservationID;
    private String staffID;

    public Inquiry() {

    }

    public Inquiry(String inquiryID, InquiryType inquiryType, LocalDateTime inquiryDateTime,
                    InquiryPriority inquiryPriority, String description,
                    InquiryStatus status, String reservationID, String staffID
    ) {
        this.inquiryID = inquiryID;
        this.inquiryType = inquiryType;
        this.inquiryDateTime = inquiryDateTime;
        this.inquiryPriority = inquiryType.getPriority();
        this.description = description;
        this.status = status;
        this.reservationID = reservationID;
        this.staffID = staffID;
    }

    public String getInquiryID() {
        return inquiryID;
    }

    public InquiryType getInquiryType() {
        return inquiryType;
    }

    public void setInquiryType(InquiryType inquiryType) {
        this.inquiryType = inquiryType;
        this.inquiryPriority = inquiryType.getPriority();
    }

    public LocalDateTime getInquiryDateTime() {
        return inquiryDateTime;
    }

    public InquiryPriority getInquiryPriority() {
        return inquiryPriority;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public InquiryStatus getStatus() {
        return status;
    }

    public void setStatus(InquiryStatus status) {
        this.status = status;
    }

    public String getReservationID() {
        return reservationID;
    }

    public String getStaffID() {
        return staffID;
    }

    public void setStaffID(String staffID) {
        this.staffID = staffID;
    }

    /*TO DO : public int compareTo() {} */

    @Override
    public String toString() {
        return "Inquiry ID: " + inquiryID
                + "\nInquiry Type: " + inquiryType
                + "\nInquiry Date Time: " + inquiryDateTime
                + "\nInquiry Priority: " + inquiryPriority
                + "\nDescription: " + description
                + "\nStatus: " + status
                + "\nReservation ID: " + reservationID
                + "\nStaff ID: " + staffID;
    }
}
