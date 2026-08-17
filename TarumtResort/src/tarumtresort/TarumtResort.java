package tarumtresort;

import java.util.Scanner;
import tarumtresort.boundary.HousekeepingUI;
import tarumtresort.boundary.LoyaltyRewardsUI;
import tarumtresort.boundary.MainMenuUI;
import tarumtresort.control.HousekeepingController;
import tarumtresort.control.ReservationControl;
import tarumtresort.utility.ConsoleUtil;

/**
 *
 * @author Brian
 */
public class TarumtResort {

    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        try (scanner) {
            MainMenuUI menu = new MainMenuUI(scanner);

            int choice;

            do {
                choice = menu.getModuleChoice();

                switch (choice) {
                    // Reservation module
                    case 1:
                        new ReservationControl().runReservationModule();
                        break;

                    // VIP Reservation module
                    case 2:
                        System.out.println("\n  ⚠ VIP Reservation module not yet integrated.");
                        menu.pressEnterToContinue();
                        break;

                    // Housekeeping module
                    case 3:
                        new HousekeepingController(new HousekeepingUI(scanner)).runHousekeeping();
                        break;

                    // Front-Desk Service module
                    case 4:
                        System.out.println("\n  ⚠ Front-Desk Service module not yet integrated.");
                        menu.pressEnterToContinue();
                        break;

                    // Loyalty & Rewards module
                    case 5:
                        new LoyaltyRewardsUI(scanner).run();
                        break;

                    case 0:
                        menu.printExitMessage();
                        break;

                    default:
                        menu.printInvalidChoice();
                }
            } while (choice != 0);
        } catch (Exception e) {
            ConsoleUtil.printError("\n  ✗ An unexpected error occurred: " + e.getMessage());
        }
    }
}
