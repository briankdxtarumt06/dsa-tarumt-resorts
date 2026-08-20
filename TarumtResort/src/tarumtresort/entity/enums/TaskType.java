package tarumtresort.entity.enums;

// Author: Brian Kam Ding Xian
public enum TaskType {
    CHECKOUT_CLEAN("Checkout Clean"),
    ROOM_SERVICE("Room Service"),
    INSPECTION("Inspection"),
    MAINTENANCE("Maintenance"),
    UNKNOWN("Unknown");

    private final String label;

    TaskType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }

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