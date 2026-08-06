package tarumtresort.entity;

import java.time.LocalDateTime;

public class Notification {
    private String notificationId;
    private String type;
    private String message;
    private LocalDateTime date;
    private boolean isRead;
    private String guestId;

    public Notification() {
    }

    public Notification(String notificationId, String type, String message,
            LocalDateTime date, boolean isRead, String guestId) {
        this.notificationId = notificationId;
        this.type = type;
        this.message = message;
        this.date = date;
        this.isRead = isRead;
        this.guestId = guestId;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
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

    public String getGuestId() {
        return guestId;
    }

    public void setGuestId(String guestId) {
        this.guestId = guestId;
    }

    @Override
    public String toString() {
        return "Notification{" + "notificationId=" + notificationId + ", type=" + type
                + ", message=" + message + ", date=" + date + ", isRead=" + isRead
                + ", guestId=" + guestId + '}';
    }
}
