package tarumtresort.entity.enums;

// Author: Brian Kam Ding Xian
public enum StaffRole {
    SUPERVISOR("Supervisor"),
    CLEANER("Cleaner"),
    RECEPTIONIST("Receptionist"),
    UNKNOWN("Unknown");

    private final String label;

    StaffRole(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }

    public static StaffRole fromString(String role) {
        if (role == null) {
            return UNKNOWN;
        }
        return switch (role.trim().toUpperCase().replace(" ", "_")) {
            case "SUPERVISOR" -> SUPERVISOR;
            case "CLEANER" -> CLEANER;
            case "RECEPTIONIST" -> RECEPTIONIST;
            default -> UNKNOWN;
        };
    }
}