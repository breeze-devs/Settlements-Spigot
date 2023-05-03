package dev.breeze.settlements.config.files;

import dev.breeze.settlements.config.ConfigField;
import dev.breeze.settlements.config.ConfigFileWrapper;
import dev.breeze.settlements.config.ConfigType;
import dev.breeze.settlements.utils.LogUtil;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.VillagerUtil;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;

@Singleton
@Getter
public final class FarmerHarvestConfig {

    private static final String FILE_NAME = "farmer_harvest";
    private static FarmerHarvestConfig instance;

    /*
     * General configurations
     */
    @Nonnull
    private final ConfigField<Boolean> respectMobGriefing;

    /*
     * Sugarcane configurations
     */
    private static final Map<Integer, Integer> DEFAULT_SUGARCANE_COUNT = Map.of(
            1, 0,
            2, 5,
            3, 10,
            4, 15,
            5, 20
    );

    @Nonnull
    private final ConfigField<Integer> sugarcaneCooldown;

    @Nonnull
    private final Map<Integer, Integer> sugarcaneExpertiseMap;

    private FarmerHarvestConfig() {
        ConfigFileWrapper wrapper;
        try {
            wrapper = new ConfigFileWrapper(FILE_NAME);
        } catch (IOException e) {
            LogUtil.exception(e, "Failed to load config file '%s'!", FILE_NAME);
            throw new RuntimeException(e);
        }

        // Load config fields
        new ConfigField<>(wrapper, ConfigType.EMPTY, "harvest", List.of("Farmer harvest configurations"), "");

        this.respectMobGriefing = new ConfigField<>(wrapper, ConfigType.BOOLEAN, "harvest.respect_mob_griefing", List.of(
                "Whether to respect the 'mobGriefing' gamerule in Minecraft",
                "If true, villager will only harvest crops when 'mobGriefing' is set to true"
        ), true);
        

        /*
         * Load sugarcane fields
         */
        new ConfigField<>(wrapper, ConfigType.EMPTY, "harvest.sugarcane", List.of("Sugarcane harvest configurations"), "");

        this.sugarcaneCooldown = new ConfigField<>(wrapper, ConfigType.INT, "harvest.sugarcane.cooldown",
                "How long will the farmer wait before harvesting another area of sugarcanes (in ticks)",
                TimeUtil.minutes(2));

        /*
         * Load config for maximum bookshelf power of each level of expertise
         */
        new ConfigField<>(wrapper, ConfigType.EMPTY, "harvest.sugarcane.count", List.of(
                "The maximum number of sugarcanes that farmers can harvest in one go",
                "Set to 0 to disallow harvesting at that level entirely"
        ), "");

        Map<Integer, ConfigField<Integer>> sugarcaneMap = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            sugarcaneMap.put(i, new ConfigField<>(wrapper, ConfigType.INT,
                    "harvest.sugarcane.count.%s".formatted(VillagerUtil.getExpertiseName(i, false).toLowerCase(Locale.ROOT)),
                    Collections.emptyList(),
                    DEFAULT_SUGARCANE_COUNT.get(i)));
        }

        // Validate & cache
        this.sugarcaneExpertiseMap = new HashMap<>();
        for (Map.Entry<Integer, ConfigField<Integer>> entry : sugarcaneMap.entrySet()) {
            int clampedValue = Math.max(0, entry.getValue().getValue());
            this.sugarcaneExpertiseMap.put(entry.getKey(), clampedValue);
        }

    }

    public static synchronized FarmerHarvestConfig getInstance() {
        if (instance == null) {
            instance = new FarmerHarvestConfig();
        }
        return instance;
    }

}
