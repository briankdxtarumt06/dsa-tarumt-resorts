package tarumtresort.utility;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ConsoleUtil {

    // ANSI escape codes for coloured console output
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";

    private ConsoleUtil() {
    }

    /** Prints a message in red - use for error/validation messages. */
    public static void printError(String message) {
        System.out.println(ANSI_RED + message + ANSI_RESET);
    }

    /** Prints a message in green - use for success messages. */
    public static void printSuccess(String message) {
        System.out.println(ANSI_GREEN + message + ANSI_RESET);
    }

    /** Prints a message in yellow - use for warnings/notices. */
    public static void printWarning(String message) {
        System.out.println(ANSI_YELLOW + message + ANSI_RESET);
    }

    public static void clearScreen() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls < NUL").inheritIO().start().waitFor();
            } else {
                new ProcessBuilder("clear").inheritIO().start().waitFor();
            }
        } catch (Exception e) {
            printError("Could not clear the screen.");
        }
    }

    public static void pressEnterToContinue(Scanner scanner) {
        System.out.print("Press Enter to continue...");
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }
    }
}
