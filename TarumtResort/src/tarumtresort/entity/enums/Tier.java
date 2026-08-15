package tarumtresort.entity.enums;

public enum Tier {
    SILVER(10),
    GOLD(15),
    PLATINUM(20),
    DIAMOND(25);

    private final int discountPercent;

    Tier(int discountPercent) {
        this.discountPercent = discountPercent;
    }

    public int getDiscountPercent() {
        return discountPercent;
    }
}
