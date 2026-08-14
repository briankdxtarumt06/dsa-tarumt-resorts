package tarumtresort.entity;

public enum InquiryPriority {
    URGENT(1),
    HIGH(2),
    MEDIUM(3),
    LOW(4);

    private final int rank;

    InquiryPriority(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }
}