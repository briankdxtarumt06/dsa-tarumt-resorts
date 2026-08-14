package tarumtresort.entity;
import tarumtresort.entity.enums.*;

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

    public Inquiry() {
    }

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
    }

    public String getInquiryId() {
        return inquiryId;
    }

    public String getConfirmationNumber() { 
        return confirmationNumber; 
    }

    public String getGuestId() { 
        return guestId; 
    }

    public InquiryType getInquiryType() { 
        return inquiryType; 
    }

    public String getDescription() { 
        return description; 
    }

    public InquiryStatus getStatus() { 
        return status; 
    }

    public void setStatus(InquiryStatus status) { 
        this.status = status; 
    }

    public LocalDateTime getCreatedTime() { 
        return createdTime; 
    }

    public LocalDateTime getResolvedTime() { 
        return resolvedTime; 
    }

    public void setResolvedTime(LocalDateTime resolvedTime) { 
        this.resolvedTime = resolvedTime; 
    }


    @Override
    public int compareTo(Inquiry other) {
        int cmp = Integer.compare(
                this.inquiryType.getPriority().getRank(),
                other.inquiryType.getPriority().getRank()
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
                '}';
    }
}