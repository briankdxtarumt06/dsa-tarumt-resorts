package tarumtresort.entity;

import java.time.LocalDateTime;
import tarumtresort.entity.enums.NotificationType;

// Author: Imam Mahdi Ali Ang Attuko
public class Notification implements Comparable<Notification> {
    private String notificationId;
    private NotificationType type;
    private String message;
    private LocalDateTime date;
    private boolean isRead;
    private boolean isDeleted;

    public Notification() {
    }

    public Notification(String notificationId, NotificationType type, String message,
            LocalDateTime date, boolean isRead) {
        this.notificationId = notificationId;
        this.type = type;
        this.message = message;
        this.date = date;
        this.isRead = isRead;
        this.isDeleted = false;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean isRead) {
        this.isRead = isRead;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    @Override
    public int compareTo(Notification other) {
        if (this.date == null && other.date == null) {
            // both null
        } else if (this.date == null) {
            return -1;
        } else if (other.date == null) {
            return 1;
        } else {
            int byDate = this.date.compareTo(other.date);
            if (byDate != 0) {
                return byDate;
            }
        }
        if (this.notificationId == null && other.notificationId == null) return 0;
        if (this.notificationId == null) return -1;
        if (other.notificationId == null) return 1;
        return this.notificationId.compareTo(other.notificationId);
    }

    @Override
    public String toString() {
        return "Notification{" + "notificationId=" + notificationId + ", type=" + type
                + ", message=" + message + ", date=" + date + ", isRead=" + isRead + '}';
    }
}
