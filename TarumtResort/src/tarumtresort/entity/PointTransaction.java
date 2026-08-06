package tarumtresort.entity;

import java.time.LocalDateTime;

public class PointTransaction implements Comparable<PointTransaction> {
    private String transactionId;
    private LocalDateTime date;
    private String description;
    private int pointChange;
    private LocalDateTime expiryDate;
    private int remainingPoints;
    private String memberId;

    public PointTransaction() {
    }

    public PointTransaction(String transactionId, LocalDateTime date, String description,
            int pointChange, LocalDateTime expiryDate, int remainingPoints, String memberId) {
        this.transactionId = transactionId;
        this.date = date;
        this.description = description;
        this.pointChange = pointChange;
        this.expiryDate = expiryDate;
        this.remainingPoints = remainingPoints;
        this.memberId = memberId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPointChange() {
        return pointChange;
    }

    public void setPointChange(int pointChange) {
        this.pointChange = pointChange;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public int getRemainingPoints() {
        return remainingPoints;
    }

    public void setRemainingPoints(int remainingPoints) {
        this.remainingPoints = remainingPoints;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    @Override
    public int compareTo(PointTransaction other) {
        int byDate = this.expiryDate.compareTo(other.expiryDate);
        if (byDate != 0) {
            return byDate;
        }
        return this.transactionId.compareTo(other.transactionId); 
    }

    @Override
    public String toString() {
        return "PointTransaction{" + "transactionId=" + transactionId + ", date=" + date
                + ", description=" + description + ", pointChange=" + pointChange
                + ", expiryDate=" + expiryDate + ", remainingPoints=" + remainingPoints
                + ", memberId=" + memberId + '}';
    }
}
