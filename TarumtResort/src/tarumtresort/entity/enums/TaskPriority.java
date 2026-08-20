package tarumtresort.entity.enums;

// Author: Brian Kam Ding Xian
public enum TaskPriority {
    HIGH(1, "High"),
    MEDIUM(2, "Medium"),
    LOW(3, "Low"),
    UNKNOWN(4, "Unknown");

    private final int rank;
    private final String label;

    TaskPriority(int rank, String label) {
        this.rank = rank;
        this.label = label;
    }

    public int getRank() {
        return rank;
    }

    @Override
    public String toString() {
        return label;
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