package tarumtresort.entity;

import tarumtresort.entity.enums.Tier;

/**
 * A personalised promotion available to members of a minimum tier.
 */
public class Promotion implements Comparable<Promotion> {

    private String promotionId;
    private String name;
    private String description;
    private Tier minTier;

    public Promotion() {
    }

    public Promotion(String promotionId, String name, String description, Tier minTier) {
        this.promotionId = promotionId;
        this.name = name;
        this.description = description;
        this.minTier = minTier;
    }

    public String getPromotionId() {
        return promotionId;
    }

    public void setPromotionId(String promotionId) {
        this.promotionId = promotionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Tier getMinTier() {
        return minTier;
    }

    public void setMinTier(Tier minTier) {
        this.minTier = minTier;
    }

    @Override
    public int compareTo(Promotion other) {
        int byTier = this.minTier.compareTo(other.minTier);
        if (byTier != 0) {
            return byTier;
        }
        return this.promotionId.compareTo(other.promotionId);
    }

    @Override
    public String toString() {
        return "Promotion{" + "promotionId=" + promotionId + ", name=" + name
                + ", description=" + description + ", minTier=" + minTier + '}';
    }
}
