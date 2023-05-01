package dev.breeze.settlements.utils;

import net.minecraft.util.Mth;

import java.util.Map;

public class VillagerUtil {

    private static final Map<Integer, String> EXPERTISE_NAME_MAP = Map.of(
            1, "&fNovice",
            2, "&6Apprentice",
            3, "&eJourneyman",
            4, "&aExpert",
            5, "&bMaster"
    );

    public static String getExpertiseName(int level, boolean colored) {
        level = Mth.clamp(level, 1, 5);
        String name = MessageUtil.translateColorCode(EXPERTISE_NAME_MAP.get(level));
        return colored ? name : MessageUtil.stripColor(name);
    }

}
