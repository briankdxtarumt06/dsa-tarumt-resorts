package tarumtresort.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;

import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.control.PriorityReservationController;
import tarumtresort.dao.PriorityReservationDAO;
import tarumtresort.dao.ReservationDAOV2;
import tarumtresort.dao.StaffDAO;
import tarumtresort.entity.PriorityReservation;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.ReservationTimestamps;
import tarumtresort.entity.Staff;
import tarumtresort.entity.enums.AvailabilityStatus;
import tarumtresort.entity.enums.PriorityLevel;
import tarumtresort.entity.enums.ReservationStatus;
import tarumtresort.entity.enums.ReservationType;
import tarumtresort.entity.enums.RoomType;

/**
 * MANUAL UI test driver for the Priority Reservation module.
 *
 * It seeds fake staff / reservations / priority records, launches the module UI
 * so you can click through EVERY screen, then restores your real data files.
 *
 * The seeded data is designed to exercise:
 *   - Landing list with a Status column (BOOKED / WAITING / ASSIGNED / CHECKED_IN / CANCELLED) and PAGING (>20)
 *     -- a staff-deleted record (RES804) is the ONLY one hidden from the list
 *   - View Details -> detail screen -> Update Priority Level (staff picker) / Delete / VIP position
 *   - View VIP Queue (waiting members only, ranked)
 *   - Filter by Priority Level
 *   - Search Priority Reservation
 *   - Add Priority Reservation = emergency grant for a NON-member (reservation picker + staff picker)
 */
public class PriorityReservationUITestDriver {

    private static final String[] FILES = {
        "data/priorityReservations.json",
        "data/allReservationList.json",
        "data/staff.json"
    };

    private static final RoomType[] ROOM_TYPES = {
        RoomType.STANDARD_SINGLE, RoomType.STANDARD_DOUBLE, RoomType.STANDARD_TRIPLE,
        RoomType.DELUXE_SINGLE, RoomType.DELUXE_DOUBLE, RoomType.DELUXE_TRIPLE, RoomType.SUITE
    };

    // levels a member can hold (PENALTY/EMERGENCY are override-only, not seeded here)
    private static final PriorityLevel[] TIERS = {
        PriorityLevel.DIAMOND, PriorityLevel.PLATINUM, PriorityLevel.GOLD, PriorityLevel.SLIVER
    };

    public static void main(String[] args) {
        try {
            for (String f : FILES) {
                backup(f);
            }

            seedStaff();
            seedReservationsAndPriority();

            System.out.println("=== Fake data seeded. Launching Priority Reservation module ===");
            System.out.println("Landing list shows statuses: BOOKED (RES701/702), WAITING, ASSIGNED (RES801),");
            System.out.println("     CHECKED_IN (RES802), CANCELLED (RES803). RES804 is staff-deleted -> hidden.");
            System.out.println("Try: paging, View Details -> override (pick staff), View VIP Queue,");
            System.out.println("     Filter, Search RES005, and Add (emergency for RES901..903).");
            System.out.println("Pick 0 (Back) to exit and restore your real data.\n");

            new PriorityReservationController().run();

        } catch (Exception e) {
            System.out.println("Driver crashed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                for (String f : FILES) {
                    restore(f);
                }
                System.out.println("\n=== Original data files restored. ===");
            } catch (IOException e) {
                System.out.println("WARNING: could not restore data files: " + e.getMessage());
            }
        }
    }

    // ---------- fake staff (for the staff picker in override / emergency add) ----------
    private static void seedStaff() {
        LinkedListInterface<Staff> staff = new LinkedList<>();
        staff.addBack(new Staff("STF001", "Amir Hakim", "Front Office", "Supervisor", AvailabilityStatus.AVAILABLE));
        staff.addBack(new Staff("STF002", "Siti Rahmah", "Front Office", "Receptionist", AvailabilityStatus.AVAILABLE));
        staff.addBack(new Staff("STF003", "Lim Wei Jie", "Management", "Duty Manager", AvailabilityStatus.BUSY));
        new StaffDAO().saveStaffList(staff);
    }

    // ---------- fake reservations (history) + matching priority records ----------
    private static void seedReservationsAndPriority() {
        LinkedListInterface<Reservation> history = new LinkedList<>();
        LinkedListInterface<PriorityReservation> priority = new LinkedList<>();

        // 18 WAITING members (each has an active priority record)
        for (int i = 1; i <= 18; i++) {
            String resId = String.format("RES%03d", i);
            String guestId = String.format("GST%03d", i);
            RoomType room = ROOM_TYPES[i % ROOM_TYPES.length];
            history.addBack(makeRes(resId, guestId, ReservationStatus.WAITING, room, i, false));

            // make a couple of them staff-overridden so "Overridden By" isn't all "-"
            if (i == 3) {
                priority.addBack(new PriorityReservation(resId, PriorityLevel.PLATINUM, "STF001", "Loyal VIP guest", false));
            } else if (i == 7) {
                priority.addBack(new PriorityReservation(resId, PriorityLevel.DIAMOND, "STF003", "Manager escalation", false));
            } else {
                priority.addBack(new PriorityReservation(resId, TIERS[i % TIERS.length]));
            }
        }

        // BOOKED members (advance bookings) -> priority record created at booking, shows as BOOKED
        history.addBack(makeRes("RES701", "GST701", ReservationStatus.BOOKED, RoomType.SUITE, 30, false));
        priority.addBack(new PriorityReservation("RES701", PriorityLevel.DIAMOND));
        history.addBack(makeRes("RES702", "GST702", ReservationStatus.BOOKED, RoomType.DELUXE_SINGLE, 31, false));
        priority.addBack(new PriorityReservation("RES702", PriorityLevel.GOLD));

        // served / checked-in / cancelled members -> priority record KEPT (not deleted) so they show with status
        history.addBack(makeRes("RES801", "GST801", ReservationStatus.ASSIGNED, RoomType.SUITE, 40, false));
        priority.addBack(new PriorityReservation("RES801", PriorityLevel.DIAMOND));
        history.addBack(makeRes("RES802", "GST802", ReservationStatus.CHECKED_IN, RoomType.DELUXE_DOUBLE, 41, false));
        priority.addBack(new PriorityReservation("RES802", PriorityLevel.GOLD));
        history.addBack(makeRes("RES803", "GST803", ReservationStatus.CANCELLED, RoomType.STANDARD_DOUBLE, 42, false));
        priority.addBack(new PriorityReservation("RES803", PriorityLevel.GOLD));

        // staff-DELETED record -> the ONLY thing hidden from the landing list now
        history.addBack(makeRes("RES804", "GST804", ReservationStatus.WAITING, RoomType.STANDARD_SINGLE, 43, false));
        priority.addBack(deleted(new PriorityReservation("RES804", PriorityLevel.SLIVER)));

        // NON-member WAITING reservations (NO priority record) -> eligible for the emergency "Add" flow
        history.addBack(makeRes("RES901", "GST901", ReservationStatus.WAITING, RoomType.DELUXE_TRIPLE, 50, false));
        history.addBack(makeRes("RES902", "GST902", ReservationStatus.WAITING, RoomType.SUITE, 51, false));
        history.addBack(makeRes("RES903", "GST903", ReservationStatus.WAITING, RoomType.STANDARD_SINGLE, 52, false));

        new ReservationDAOV2().saveAllReservations(history);
        new PriorityReservationDAO().saveToFile(priority);
    }

    private static Reservation makeRes(String id, String guestId, ReservationStatus status,
            RoomType roomType, int regOffsetMinutes, boolean isDeleted) {
        ReservationTimestamps ts = new ReservationTimestamps(
                LocalDateTime.now().minusMinutes(regOffsetMinutes),
                LocalDate.now(),
                LocalDate.now().plusDays(1));
        String roomId = (status == ReservationStatus.ASSIGNED || status == ReservationStatus.CHECKED_IN) ? "R101" : null;
        return new Reservation(id, "CN-" + id, guestId, roomId,
                roomType, 1, 1, ReservationType.WALK_IN, status, ts, isDeleted);
    }

    private static PriorityReservation deleted(PriorityReservation pr) {
        pr.setDeleted(true);
        return pr;
    }

    // ---------- backup / restore ----------
    private static void backup(String file) throws IOException {
        Path p = Path.of(file);
        if (Files.exists(p)) {
            Files.copy(p, Path.of(file + ".uibak"), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void restore(String file) throws IOException {
        Path p = Path.of(file);
        Path bak = Path.of(file + ".uibak");
        if (Files.exists(bak)) {
            Files.copy(bak, p, StandardCopyOption.REPLACE_EXISTING);
            Files.delete(bak);
        } else {
            Files.deleteIfExists(p); // no original existed - remove the seeded test file
        }
    }
}
