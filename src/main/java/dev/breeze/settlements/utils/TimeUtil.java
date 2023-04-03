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

}
