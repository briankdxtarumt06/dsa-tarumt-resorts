package tarumtresort.entity;

public enum InquiryType {
    ROOMSERVICE(InquiryPriority.URGENT),
    BILLINGDETAILS(InquiryPriority.HIGH),
    ROOMAVAILABILITY(InquiryPriority.MEDIUM),
    GUESTIDENTIFICATION(InquiryPriority.LOW);

    private final InquiryPriority priority;

    InquiryType(InquiryPriority priority) {
        this.priority = priority;
    }

    public InquiryPriority getPriority() {
        return priority;
    }
}