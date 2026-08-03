/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarumtresort.entity;

import java.time.LocalDateTime;

/**
 *
 * @author Wen Ling
 */
public class Inquiry {
    private String inquiryID;
    private String inquiryType; // enum
    private LocalDateTime inquiryDateTime;
    private int inquiryPriority; // enum
    private String description;
    private String status; // enum: PENDING, IN PROGRESS, DONE
    private String reservationID;
    private String staffID;

    public Inquiry(String inquiryID,String inquiryType,LocalDateTime inquiryDateTime,
        int inquiryPriority,String description,String status,String reservationID,String staffID
    ) {
        this.inquiryID = inquiryID;
        this.inquiryType = inquiryType;
        this.inquiryDateTime = inquiryDateTime;
        this.inquiryPriority = inquiryPriority;
        this.description = description;
        this.status = status;
        this.reservationID = reservationID;
        this.staffID = staffID;
    }

    public String getInquiryID() {
        return inquiryID;
    }

    public String inquiryType() {
        return inquiryType;
    }

    public LocalDateTime getInquiryDateTime() {
        return inquiryDateTime;
    }

    public int getInquiryPriority() {
        return inquiryPriority;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getStaffID() {
        return staffID;
    }

    public String getReservationID() {
        return reservationID;
    }

    public void setInquiryPriority(int inquiryPriority) {
        this.inquiryPriority = inquiryPriority;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setStaffID(String staffID) {
        this.staffID = staffID;
    }

    @Override
    public String toString() {
        return "Inquiry ID: " + inquiryID
                + "\nDate Time: " + inquiryDateTime
                + "\nPriority: " + inquiryPriority
                + "\nType: " + inquiryType
                + "\nDescription: " + description
                + "\nStatus: " + status
                + "\nReservation ID: " + reservationID
                + "\nStaff ID: " + staffID;
    }
}
