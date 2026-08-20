package tarumtresort.utility;

// Author: Brian Kam Ding Xian
public final class Ansi {

    public static final boolean ENABLED = true;

    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String CYAN = "\u001B[36m";

    private Ansi() {
    }

    // wraps text in a color code + reset; returns plain text when disabled
    public static String color(String code, String text) {
        if (!ENABLED || code == null || text == null) {
            return text;
        }
        return code + text + RESET;
    }

    public static String bold(String text) {
        return color(BOLD, text);
    }

    public static String red(String text) {
        return color(RED, text);
    }

    public static String green(String text) {
        return color(GREEN, text);
    }

    public static String yellow(String text) {
        return color(YELLOW, text);
    }

    public static String cyan(String text) {
        return color(CYAN, text);
    }

    // remove all ANSI code
    public static String strip(String text) {
        if (!ENABLED || text == null) {
            return text;
        }
        return text.replaceAll("\u001B\\[[0-9;]*m", "");
    }
}