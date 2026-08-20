package tarumtresort.utility;

import java.util.Scanner;

// Author: Imam Mahdi Ali Ang Attuko
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
        for(int i = 0; i<100; i++){
            System.out.println("\n");
        }
    }

    public static void pressEnterToContinue(Scanner scanner) {
        System.out.print("\nPress Enter to continue...");
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }
    }
}
