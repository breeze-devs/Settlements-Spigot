package dev.breeze.settlements.config.files;

import dev.breeze.settlements.config.ConfigField;
import dev.breeze.settlements.config.ConfigFileWrapper;
import dev.breeze.settlements.config.ConfigType;
import dev.breeze.settlements.utils.LogUtil;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.List;

@Singleton
public final class InternalTradingConfig {

    private static final String FILE_NAME = "internal_trading";
    private static InternalTradingConfig instance;

    @Getter
    @Nonnull
    private final ConfigField<Double> minFriendshipToTrade;

    @Getter
    @Nonnull
    private final ConfigField<Integer> initialEmeraldBalanceMin;
    @Getter
    @Nonnull
    private final ConfigField<Integer> initialEmeraldBalanceMax;


    private InternalTradingConfig() {
        ConfigFileWrapper wrapper;
        try {
            wrapper = new ConfigFileWrapper(FILE_NAME);
        } catch (IOException e) {
            LogUtil.exception(e, "Failed to load config file '%s'!", FILE_NAME);
            throw new RuntimeException(e);
        }

        // Load config fields
        new ConfigField<>(wrapper, ConfigType.EMPTY, "internal_trade", List.of(
                "Internal trading configurations"
        ), "");

        /*
         * Load basic fields
         */
        this.minFriendshipToTrade = new ConfigField<>(wrapper, ConfigType.DOUBLE, "internal_trade.min_friendship",
                List.of(
                        "How friendly does a villager have to be with another villager to trade",
                        "Friendship is a value between -1 and 1, where -1 is the worst possible friendship and 1 is the best possible friendship"
                ), -0.25);

        // Initial emeralds section
        new ConfigField<>(wrapper, ConfigType.EMPTY, "initial_emeralds", List.of(
                "How many emeralds a villager should have when they are first created",
                "This value is randomly generated between the minimum and maximum values"
        ), "");
        this.initialEmeraldBalanceMin = new ConfigField<>(wrapper, ConfigType.INT, "internal_trade.min_initial_emeralds",
                "The lower bound of the initial emerald balance",
                32);
        this.initialEmeraldBalanceMax = new ConfigField<>(wrapper, ConfigType.INT, "internal_trade.max_initial_emeralds",
                "The upper bound of the initial emerald balance",
                64);
    }

    public static synchronized InternalTradingConfig getInstance() {
        if (instance == null) {
            instance = new InternalTradingConfig();
        }
        return instance;
    }

}
