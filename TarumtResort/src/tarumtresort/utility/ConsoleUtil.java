package tarumtresort.utility;

import java.util.Scanner;

public class ConsoleUtil {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";

    private ConsoleUtil() {
    }

    public static void printError(String message) {
        System.out.println(ANSI_RED + message + ANSI_RESET);
    }

    public static void printSuccess(String message) {
        System.out.println(ANSI_GREEN + message + ANSI_RESET);
    }

    public static void printWarning(String message) {
        System.out.println(ANSI_YELLOW + message + ANSI_RESET);
    }

    public static void clearScreen() {
        // manual override: -Dtarumtresort.clearScreen=ansi   or   =lines
        String override = System.getProperty("tarumtresort.clearScreen", "");
        if (override.equalsIgnoreCase("ansi")) {
            ansiClear();
        } else if (override.equalsIgnoreCase("lines")) {
            lineClear();
        } else if (isAnsiTerminal()) {
            ansiClear();
        } else {
            lineClear();
        }
    }

    /**
     * ANSI-capable terminals set TERM_PROGRAM (VS Code, iTerm2),
     * WT_SESSION (Windows Terminal) or TERM (xterm-256color etc.).
     * NetBeans' Output window sets none of these, so it gets the
     * blank-line fallback.
     */
    private static boolean isAnsiTerminal() {
        return System.getenv("TERM_PROGRAM") != null
                || System.getenv("WT_SESSION") != null
                || System.getenv("TERM") != null;
    }

    /** Real clear: cursor home + clear screen - next output appears at the TOP. */
    private static void ansiClear() {
        System.out.print("\u001B[2J\u001B[H");
        System.out.flush();
    }

    /** Scroll-away fallback for IDE output windows that ignore ANSI codes. */
    private static void lineClear() {
        for (int i = 0; i < 50; i++) {
            System.out.println();
        }
    }

    public static void pressEnterToContinue(Scanner scanner) {
        System.out.print("\nPress Enter to continue...");
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }
    }
}
