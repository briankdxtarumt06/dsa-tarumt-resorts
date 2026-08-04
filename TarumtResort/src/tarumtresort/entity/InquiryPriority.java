package tarumtresort.entity;

public enum InquiryPriority {
    HIGH(1),
    MEDIUM(2),
    LOW(3);

    private final int level;

    public int getLevel() {
        return level;
    }

    InquiryPriority(int level) {
        this.level = level;
    }
}
