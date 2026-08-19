package tarumtresort.utility;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author Brian
 *
 * Formats LocalDateTime into human-readable text for UI output.
 * Persistence keeps the full ISO-8601 format (see JsonFileHandler);
 * this class is only for display.
 */
public class DateTimeUtil {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a");

    private DateTimeUtil() {
    }

    public static String readable(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(DATE_TIME);
    }

    public static String date(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(DATE);
    }

    public static String time(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(TIME);
    }
}