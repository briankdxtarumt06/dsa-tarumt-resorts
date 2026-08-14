package tarumtresort.test;

import java.nio.file.Path;
import java.time.LocalDateTime;
import tarumtresort.adt.LinkedList;
import tarumtresort.entity.Guest;
import tarumtresort.entity.Member;
import tarumtresort.entity.PointTransaction;
import tarumtresort.entity.RedemptionRecord;
import tarumtresort.entity.Reward;
import tarumtresort.entity.enums.Tier;
import tarumtresort.utility.JsonFileHandler;

/**
 * Generates sample data in the data/ folder so the app can be demoed
 * immediately. Run this class once (or rerun it to reset the demo data).
 * All dates are relative to today, so the demo always shows:
 *   - an expiry alert (points expiring within 7 days)
 *   - a pending redemption request
 *   - EXPIRED / PARTIAL / VALID transaction statuses
 */
public class SeedData {

    public static void main(String[] args) throws Exception {
        LocalDateTime now = LocalDateTime.now();

        // ---------- Guests (notifications embedded) ----------
        LinkedList<Guest> guests = new LinkedList<>();
        guests.addSorted(new Guest("G001", "Alice Tan", "IC990101-14-1234", "012-3456789",
                "Malaysian", "12 Jalan Merdeka, Kuala Lumpur"));
        guests.addSorted(new Guest("G002", "Bob Lee", "IC980202-10-5678", "013-2345678",
                "Malaysian", "45 Persiaran Gurney, Penang"));
        JsonFileHandler.saveList(guests, Path.of("data", "guests.json"));

        // ---------- Rewards ----------
        LinkedList<Reward> rewards = new LinkedList<>();
        rewards.addSorted(new Reward("R001", "Welcome Drink", "Complimentary drink at the pool bar", 200));
        rewards.addSorted(new Reward("R002", "Late Checkout", "Checkout extended to 2pm", 500));
        rewards.addSorted(new Reward("R003", "Room Upgrade", "Upgrade to a deluxe room", 1500));
        rewards.addSorted(new Reward("R004", "1 Free Night Stay", "Free standard room for one night", 3000));
        JsonFileHandler.saveList(rewards, Path.of("data", "rewards.json"));

        // ---------- Members (transactions + redemptions embedded) ----------

        // M001 - Alice: cumulative earned 3100 -> PLATINUM, 1800 pts available
        Member m1 = new Member("M001", 1800, Tier.PLATINUM, now.minusDays(500), "G001");
        // already expired (remaining 0)
        m1.addPointTransaction(new PointTransaction("PT0001", now.minusDays(400), "Room stay at TARUMT Resort",
                1000, now.minusDays(35), 0, "M001"));
        // expiring in 3 days -> alert fires when the points menu opens
        m1.addPointTransaction(new PointTransaction("PT0002", now.minusDays(362), "Spa package",
                600, now.plusDays(3), 600, "M001"));
        // valid but partially redeemed
        m1.addPointTransaction(new PointTransaction("PT0003", now.minusDays(200), "Restaurant dining",
                800, now.plusDays(165), 500, "M001"));
        // valid, untouched
        m1.addPointTransaction(new PointTransaction("PT0004", now.minusDays(100), "Water park tickets",
                700, now.plusDays(265), 700, "M001"));
        // one approved redemption + one pending request
        RedemptionRecord approved = new RedemptionRecord("RR0001", now.minusDays(150), "M001", "R001");
        approved.setStatus("APPROVED");
        m1.addRedemptionRecord(approved);
        m1.addRedemptionRecord(new RedemptionRecord("RR0002", now.minusDays(5), "M001", "R002"));

        // M002 - Bob: cumulative earned 1200 -> GOLD, 1200 pts available
        Member m2 = new Member("M002", 1200, Tier.GOLD, now.minusDays(300), "G002");
        m2.addPointTransaction(new PointTransaction("PT0005", now.minusDays(120), "Weekend getaway",
                1200, now.plusDays(245), 1200, "M002"));

        LinkedList<Member> members = new LinkedList<>();
        members.addSorted(m1);
        members.addSorted(m2);
        JsonFileHandler.saveList(members, Path.of("data", "members.json"));

        System.out.println("Sample data written to data/:");
        System.out.println("  - guests.json   (G001 Alice, G002 Bob)");
        System.out.println("  - members.json  (M001 PLATINUM, M002 GOLD + transactions/redemptions)");
        System.out.println("  - rewards.json  (4 rewards)");
        System.out.println();
        System.out.println("Demo tips:");
        System.out.println("  - Open the Points menu: expiry alert for PT0002 (3 days) generates automatically");
        System.out.println("  - Points > Process Redemption Requests: RR0002 is waiting (approve/reject)");
        System.out.println("  - Points > Transaction History: shows EXPIRED / PARTIAL / VALID statuses");
        System.out.println("  - Points > View Notifications: shows the alert after it is generated");
    }
}
