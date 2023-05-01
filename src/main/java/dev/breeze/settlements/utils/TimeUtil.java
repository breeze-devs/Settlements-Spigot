package dev.breeze.settlements.utils;

public class TimeUtil {

    private static final int SECOND_IN_TICKS = 20;
    private static final int MINUTE_IN_TICKS = 60 * SECOND_IN_TICKS;
    private static final int HOUR_IN_TICKS = 60 * MINUTE_IN_TICKS;

    public static int ticks(int ticks) {
        return ticks;
    }

    public static int seconds(int seconds) {
        return seconds * SECOND_IN_TICKS;
    }

    public static int minutes(int minutes) {
        return minutes * MINUTE_IN_TICKS;
    }

    public static int hours(int hours) {
        return hours * HOUR_IN_TICKS;
    }

    public static String ticksToReadableTime(int ticks) {
        // Split into time units
        int hours = ticks / HOUR_IN_TICKS;
        ticks %= HOUR_IN_TICKS;

        int minutes = ticks / MINUTE_IN_TICKS;
        ticks %= MINUTE_IN_TICKS;

        int seconds = ticks / SECOND_IN_TICKS;
        ticks %= SECOND_IN_TICKS;

        // Build the string
        StringBuilder readableTime = new StringBuilder();
        if (hours > 0) {
            readableTime.append(hours).append(" hours ");
        }
        if (minutes > 0) {
            readableTime.append(minutes).append(" minutes ");
        }
        if (seconds > 0) {
            readableTime.append(seconds).append(" seconds ");
        }
        if (ticks > 0) {
            readableTime.append(ticks).append(" ticks");
        }

        return readableTime.toString().trim();
    }

}
