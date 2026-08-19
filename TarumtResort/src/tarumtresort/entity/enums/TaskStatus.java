package tarumtresort.entity.enums;

/**
 *
 * @author Brian
 */
public enum TaskStatus {
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }

    public static TaskStatus fromString(String status) {
        if (status == null) {
            return null;
        }
        return switch (status.trim().toUpperCase().replace(" ", "_")) {
            case "PENDING" -> PENDING;
            case "IN_PROGRESS" -> IN_PROGRESS;
            case "COMPLETED" -> COMPLETED;
            case "CANCELLED" -> CANCELLED;
            case "WORK_FINISHED" -> COMPLETED;
            case "INSPECTED" -> COMPLETED;
            case "HANDED_OFF" -> IN_PROGRESS;
            case "PAUSED" -> IN_PROGRESS;
            default -> null;
        };
    }
}