package tarumtresort.entity.enums;

/**
 *
 * @author Brian
 */
public enum TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

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