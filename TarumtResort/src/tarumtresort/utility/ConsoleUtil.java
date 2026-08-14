package tarumtresort.utility;

import java.util.Scanner;

/**
 * Small console helpers shared by all boundary UIs.
 */
public class ConsoleUtil {

    private ConsoleUtil() { // static-only utility
    }

    /** Clears the console screen (cls on Windows, clear on Unix-like systems). */
    public static void clearScreen() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                // route cls stdin from NUL so it does not consume the app's
                // piped/console input
                new ProcessBuilder("cmd", "/c", "cls < NUL").inheritIO().start().waitFor();
            } else {
                new ProcessBuilder("clear").inheritIO().start().waitFor();
            }
        } catch (Exception e) {
            System.out.println("Could not clear the screen.");
        }
    }

    /**
     * Pauses until the user presses Enter. Use at the end of any screen that
     * displays content, so it is not wiped by the next clearScreen().
     */
    public static void pressEnterToContinue(Scanner scanner) {
        System.out.print("Press Enter to continue...");
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }
    }
}
