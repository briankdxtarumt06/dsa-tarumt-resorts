package tarumtresort.entity.enums;

/**
 *
 * @author Brian
 * 
 */
public enum TaskType {
    CHECKOUT_CLEAN,
    ROOM_SERVICE,
    INSPECTION,
    MAINTENANCE,
    UNKNOWN;

    public static TaskType fromString(String type) {
        if (type == null) {
            return UNKNOWN;
        }
        return switch (type.trim().toUpperCase().replace(" ", "_")) {
            case "CHECKOUT_CLEAN", "HOUSEKEEPING" -> CHECKOUT_CLEAN;
            case "ROOM_SERVICE" -> ROOM_SERVICE;
            case "INSPECTION" -> INSPECTION;
            case "MAINTENANCE" -> MAINTENANCE;
            default -> UNKNOWN;
        };
    }
}