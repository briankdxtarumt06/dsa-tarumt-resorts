package tarumtresort;

import java.util.Scanner;
import tarumtresort.boundary.HousekeepingUI;
import tarumtresort.boundary.MainMenuUI;
import tarumtresort.control.HousekeepingController;

/**
 *
 * @author Brian
 */
public class TarumtResort {

    private static final Scanner scanner = new Scanner(System.in);

    // Placeholder Main Menu
    public static void main(String[] args) {
        MainMenuUI menu = new MainMenuUI(scanner);

        int choice;

        do {
            choice = menu.getModuleChoice();

            switch (choice) {
                case 1:
                    new HousekeepingController(new HousekeepingUI(scanner)).runHousekeeping();
                    break;
                case 0:
                    menu.printExitMessage();
                    break;
                default:
                    menu.printInvalidChoice();
            }
        } while (choice != 0);
    }
}