package tarumtresort.entity;

public enum InquiryType {
    BILLING(InquiryPriority.HIGH),
    MAINTENANCE(InquiryPriority.HIGH),
    HOUSEKEEPING(InquiryPriority.MEDIUM),
    ROOM_AVAILABILITY(InquiryPriority.MEDIUM),
    GUEST_IDENTIFICATION(InquiryPriority.LOW);

    private final InquiryPriority priority;

    InquiryType(InquiryPriority priority) {
        this.priority = priority;
    }

    public InquiryPriority getPriority() {
        return priority;
    }
}
