package tarumtresort.entity.enums;

// Author: Imam Mahdi Ali Ang Attuko
public enum Tier {
    SILVER(5),
    GOLD(8),
    PLATINUM(12),
    DIAMOND(15);

    private final int discountPercent;

    Tier(int discountPercent) {
        this.discountPercent = discountPercent;
    }

    public int getDiscountPercent() {
        return discountPercent;
    }
}
