package dev.breeze.settlements.entities.villagers;

import java.util.Map;

/**
 * Reputation is a number ranging from [-100, 100]
 */
public class ReputationLevels {

    private static final Map<Integer, String> REPUTATION_TITLES = Map.ofEntries(
            Map.entry(100, "Village Hero"),
            Map.entry(90, "Village Champion"),
            Map.entry(80, "Esteemed Guardian"),
            Map.entry(70, "Close Comrade"),
            Map.entry(60, "Trusted Ally"),
            Map.entry(50, "Village Benefactor"),
            Map.entry(40, "Village Friend"),
            Map.entry(30, "Known Traveller"),
            Map.entry(20, "Occasional Guest"),
            Map.entry(10, "Passing Visitor"),
            Map.entry(0, "Unknown Stranger"),
            Map.entry(-10, "Unsettling Visitor"),
            Map.entry(-20, "Unfriendly Outsider"),
            Map.entry(-30, "Disliked Trespasser"),
            Map.entry(-40, "Hostile Intruder"),
            Map.entry(-50, "Village Agitator"),
            Map.entry(-60, "Village Adversary"),
            Map.entry(-70, "Village Threat"),
            Map.entry(-80, "Dreaded Foe"),
            Map.entry(-90, "Imminent Danger"),
            Map.entry(-100, "Sworn Enemy")
    );

    public static String getTitle(int reputation) {
        // Round the reputation to the nearest 10
        reputation = (int) (Math.round(reputation / 10.0) * 10);

        // Clamp reputation to [-100, 100]
        reputation = Math.max(-100, reputation);
        reputation = Math.min(100, reputation);

        // Set color code
        String colorCode = reputation < -60 ? "&4" // dark red
                : reputation < -30 ? "&c" // light red
                : reputation < 0 ? "&6" // orange
                : reputation < 30 ? "&e" // yellow
                : reputation < 60 ? "&a" // light green
                : "&2"; // dark green

        return colorCode + REPUTATION_TITLES.get(reputation);
    }

    public static boolean isComrade(int reputation) {
        return reputation >= 60;
    }

    public static boolean isFriendly(int reputation) {
        return reputation >= 40;
    }

}
