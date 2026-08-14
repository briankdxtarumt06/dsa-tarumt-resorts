package tarumtresort.utility;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ConsoleUtil {

    private ConsoleUtil() {
    }

    public static void clearScreen() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls < NUL").inheritIO().start().waitFor();
            } else {
                new ProcessBuilder("clear").inheritIO().start().waitFor();
            }
        } catch (Exception e) {
            System.out.println("Could not clear the screen.");
        }
    }

    public static void pressEnterToContinue(Scanner scanner) {
        System.out.print("Press Enter to continue...");
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }
    }
}
