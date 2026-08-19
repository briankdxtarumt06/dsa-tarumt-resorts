package tarumtresort.entity.enums;

public enum Tier {
    SILVER(5),
    GOLD(10),
    PLATINUM(15),
    DIAMOND(20);

    private final int discountPercent;

    Tier(int discountPercent) {
        this.discountPercent = discountPercent;
    }

    public int getDiscountPercent() {
        return discountPercent;
    }
}
