package tarumtresort.entity.enums;

// Author: Brian Kam Ding Xian
public enum AvailabilityStatus {
    AVAILABLE("Available"),
    BUSY("Busy"),
    ON_LEAVE("On Leave"),
    RESIGNED("Resigned");

    private final String label;

    AvailabilityStatus(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }

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