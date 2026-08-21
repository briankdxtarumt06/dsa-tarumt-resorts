package tarumtresort.entity.enums;

/**
 * Category of a member notification. Persisted by name via Gson, so values
 * must stay stable across versions.
 */
public enum NotificationType {
    /** Member's tier was upgraded. */
    TIER_UPGRADE,
    /** Some of the member's points are approaching expiry. */
    POINT_EXPIRY,
    /** A redemption request was approved (voucher issued). */
    REDEMPTION_APPROVED,
    /** A redemption request was rejected. */
    REDEMPTION_REJECTED,
    /** Legacy value from the removed personalized-promotion feature; kept so old data files still load. */
    PROMOTION_ASSIGNED
}
