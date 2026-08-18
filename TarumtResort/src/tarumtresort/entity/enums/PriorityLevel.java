package tarumtresort.entity.enums;

public enum PriorityLevel {
    PENALTY(0),
    SLIVER(10),
    GOLD(20),
    PLATINUM(30),
    DIAMOND(40),
    EMERGENCY(50);

    private final int rank;

    PriorityLevel(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    public static PriorityLevel convertTierToPriority(Tier tier) {
        return switch (tier) {
            case DIAMOND -> DIAMOND;
            case PLATINUM -> PLATINUM;
            case GOLD -> GOLD;
            default -> SLIVER;
        };
    }
}