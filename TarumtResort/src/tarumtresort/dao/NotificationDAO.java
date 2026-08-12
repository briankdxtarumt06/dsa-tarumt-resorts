package tarumtresort.dao;

import java.nio.file.Path;
import tarumtresort.adt.LinkedList;
import tarumtresort.entity.Notification;
import tarumtresort.utility.JsonFileHandler;

public class NotificationDAO {
    private final String FILE_NAME = "data/notifications.json";

    private final LinkedList<Notification> notifications = new LinkedList<>();

    public void Add(Notification notification) {
        notifications.addSorted(notification);
    }

    public void Remove(String notificationId) {
        LinkedList<Notification> kept = new LinkedList<>();
        for (int i = 0; i < notifications.size(); i++) {
            Notification n = notifications.get(i);
            if (!n.getNotificationId().equals(notificationId)) {
                kept.addBack(n);
            }
        }
        notifications.clear();
        for (int i = 0; i < kept.size(); i++) {
            notifications.addBack(kept.get(i));
        }
    }

    public Notification FindById(String notificationId) {
        for (int i = 0; i < notifications.size(); i++) {
            if (notifications.get(i).getNotificationId().equals(notificationId)) {
                return notifications.get(i);
            }
        }
        return null;
    }

    public LinkedList<Notification> GetAll() {
        return notifications;
    }

    public int Size() {
        return notifications.size();
    }

    public boolean IsEmpty() {
        return notifications.isEmpty();
    }

    public void LoadFromFile() {
        notifications.clear();
        try {
            LinkedList<Notification> loaded = JsonFileHandler.loadList(Path.of(FILE_NAME), Notification.class);
            for (int i = 0; i < loaded.size(); i++) {
                notifications.addBack(loaded.get(i));
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to load " + FILE_NAME + ": " + e.getMessage());
        }
    }

    public void SaveToFile() {
        try {
            JsonFileHandler.saveList(notifications, Path.of(FILE_NAME));
        } catch (java.io.IOException e) {
            System.err.println("Failed to save " + FILE_NAME + ": " + e.getMessage());
        }
    }
}
