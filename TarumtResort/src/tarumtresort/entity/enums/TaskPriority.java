package tarumtresort.entity.enums;

/**
 *
 * @author Brian
 */
public enum TaskPriority {
    HIGH(1),
    MEDIUM(2),
    LOW(3),
    UNKNOWN(4);

    private final int rank;

    TaskPriority(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    public static TaskPriority fromString(String priority) {
        if (priority == null) {
            return UNKNOWN;
        }
        return switch (priority.trim().toUpperCase()) {
            case "HIGH" -> HIGH;
            case "MEDIUM" -> MEDIUM;
            case "LOW" -> LOW;
            default -> UNKNOWN;
        };
    }
}