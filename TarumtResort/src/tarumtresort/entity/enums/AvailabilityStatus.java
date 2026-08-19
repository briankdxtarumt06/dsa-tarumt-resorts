package tarumtresort.entity.enums;

/**
 *
 * @author Brian
 *
 */
public enum AvailabilityStatus {
    AVAILABLE,
    BUSY,
    ON_LEAVE,
    RESIGNED;

    public static AvailabilityStatus fromString(String status) {
        if (status == null) {
            return AVAILABLE;
        }
        return switch (status.trim().toUpperCase().replace(" ", "_")) {
            case "AVAILABLE" -> AVAILABLE;
            case "BUSY" -> BUSY;
            case "ON_LEAVE", "UNAVAILABLE" -> ON_LEAVE;
            case "RESIGNED" -> RESIGNED;
            default -> AVAILABLE;
        };
    }
}