package tarumtresort.entity.enums;

// Author: Brian Kam Ding Xian
public enum Department {
    HOUSEKEEPING("Housekeeping"),
    FRONT_OFFICE("Front Office"),
    MAINTENANCE("Maintenance"),
    UNKNOWN("Unknown");

    private final String label;

    Department(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }

    public static Department fromString(String department) {
        if (department == null) {
            return UNKNOWN;
        }
        return switch (department.trim().toUpperCase().replace(" ", "_")) {
            case "HOUSEKEEPING" -> HOUSEKEEPING;
            case "FRONT_OFFICE" -> FRONT_OFFICE;
            case "MAINTENANCE" -> MAINTENANCE;
            default -> UNKNOWN;
        };
    }
}