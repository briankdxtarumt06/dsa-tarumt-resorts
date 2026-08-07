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
public class Inquiry implements Comparable<Inquiry> {
    private String inquiryId;
    private String confirmationNumber;
    private String guestId;
    private InquiryType inquiryType;
    private String description;
    private InquiryStatus status;
    private LocalDateTime createdTime;
    private LocalDateTime resolvedTime;
    private String assignedStaffId;

    public Inquiry(String inquiryId, String confirmationNumber, String guestId,
                    InquiryType inquiryType, String description) {
        this.inquiryId = inquiryId;
        this.confirmationNumber = confirmationNumber;
        this.guestId = guestId;
        this.inquiryType = inquiryType;
        this.description = description;
        this.status = InquiryStatus.PENDING;
        this.createdTime = LocalDateTime.now();
        this.resolvedTime = null;
        this.assignedStaffId = null;
    }

    // getters
    public String getInquiryId() { return inquiryId; }
    public String getConfirmationNumber() { return confirmationNumber; }
    public String getGuestId() { return guestId; }
    public InquiryType getQueryType() { return inquiryType; }
    public String getDescription() { return description; }
    public InquiryStatus getStatus() { return status; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public LocalDateTime getResolvedTime() { return resolvedTime; }
    public String getAssignedStaffId() { return assignedStaffId; }

    // setters
    public void setDescription(String description) { this.description = description; }
    public void setStatus(InquiryStatus status) { this.status = status; }
    public void setResolvedTime(LocalDateTime resolvedTime) { this.resolvedTime = resolvedTime; }
    public void setAssignedStaffId(String assignedStaffId) { this.assignedStaffId = assignedStaffId; }

    @Override
    public int compareTo(Inquiry other) {
        int cmp = Integer.compare(
            this.inquiryType.getPriority().getLevel(),
            other.inquiryType.getPriority().getLevel()
        );
        if (cmp == 0) {
            cmp = this.createdTime.compareTo(other.createdTime);
        }
        return cmp;
    }

    @Override
    public String toString() {
        return "Inquiry{" +
                "inquiryId='" + inquiryId + '\'' +
                ", confirmationNumber='" + confirmationNumber + '\'' +
                ", inquiryType=" + inquiryType +
                ", description='" + description + '\'' +
                ", priority=" + inquiryType.getPriority() +
                ", status=" + status +
                ", createdTime=" + createdTime +
                ", assignedStaffId='" + assignedStaffId + '\'' +
                '}';
    }
}