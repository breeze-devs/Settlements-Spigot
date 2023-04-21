package dev.breeze.settlements.utils;

/**
 * A utility class that provides various methods for parsing and manipulating strings
 */
public class StringUtil {

    /**
     * Parses an integer from the given string
     *
     * @param msg           The input string to parse
     * @param defaultResult The default value to return if parsing fails
     * @return The parsed integer value or the defaultResult if parsing fails
     */
    public static int parseInt(String msg, int defaultResult) {
        try {
            return Integer.parseInt(msg);
        } catch (NumberFormatException e) {
            return defaultResult;
        }
    }

    /**
     * Parses a float from the given string
     *
     * @param msg           The input string to parse
     * @param defaultResult The default value to return if parsing fails
     * @return The parsed float value or the defaultResult if parsing fails
     */
    public static float parseFloat(String msg, float defaultResult) {
        try {
            return Float.parseFloat(msg);
        } catch (NumberFormatException e) {
            return defaultResult;
        }
    }

    /**
     * Parses a double from the given string
     *
     * @param msg           The input string to parse
     * @param defaultResult The default value to return if parsing fails
     * @return The parsed double value or the defaultResult if parsing fails
     */
    public static double parseDouble(String msg, double defaultResult) {
        try {
            return Double.parseDouble(msg);
        } catch (NumberFormatException e) {
            return defaultResult;
        }
    }

    /**
     * Converts the input string to title case, capitalizing the first letter of each word
     *
     * @param input The input string to convert
     * @return The input string in title case format
     */
    public static String toTitleCase(String input) {
        StringBuilder titleCase = new StringBuilder(input.length());
        boolean nextTitleCase = true;
        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            }
            titleCase.append(c);
        }
        return titleCase.toString();
    }

    /**
     * Compares the given message against a variable list of comparison strings
     * Returns true if any of the comparison strings match the message
     *
     * @param message     The input message to compare against
     * @param comparisons The comparison strings
     * @return True if any of the comparison strings match the message, false otherwise
     */
    public static boolean bulkCompare(String message, String... comparisons) {
        for (String comparison : comparisons) {
            if (message.equals(comparison)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compares the given message against a variable list of comparison strings, ignoring case
     * Returns true if any of the comparison strings match the message, ignoring case
     *
     * @param message     The input message to compare against
     * @param comparisons The comparison strings
     * @return True if any of the comparison strings match the message, ignoring case, false otherwise
     */
    public static boolean bulkCompareIgnoreCase(String message, String... comparisons) {
        for (String comparison : comparisons) {
            if (message.equalsIgnoreCase(comparison)) {
                return true;
            }
        }
        return false;
    }

}
