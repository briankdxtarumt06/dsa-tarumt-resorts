package tarumtresort.entity.enums;

public enum RoomStatus {
    AVAILABLE("Available"),
    OCCUPIED("Occupied"),
    CLEANING("Cleaning"),
    MAINTENANCE("Maintenance");

    private final String label;

    RoomStatus(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }

    public static RoomStatus fromString(String status) {
        if (status == null) {
            return null;
        }
        return switch (status.trim().toUpperCase().replace(" ", "_")) {
            case "AVAILABLE" -> AVAILABLE;
            case "OCCUPIED" -> OCCUPIED;
            case "CLEANING" -> CLEANING;
            case "MAINTENANCE" -> MAINTENANCE;
            default -> null;
        };
    }
}
