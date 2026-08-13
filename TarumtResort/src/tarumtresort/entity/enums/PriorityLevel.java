package tarumtresort.entity.enums;

public enum PriorityLevel {
    PENALTY(0),
    STANDARD(10),
    ELITE(20),
    DIAMOND(30),
    PLATINUM(40),
    VIP_OVERRIDE(50),
    EMERGENCY(60);

    private final int rank;

    PriorityLevel(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    public static PriorityLevel convertTierToPriority(Tier tier) {
        if (tier == null)
            return STANDARD;
        return switch (tier) {
            case PLATINUM -> PLATINUM;
            case DIAMOND -> DIAMOND;
            default -> STANDARD;
        };
    }
}