package dev.breeze.settlements.config.files;

import dev.breeze.settlements.config.ConfigField;
import dev.breeze.settlements.config.ConfigFileWrapper;
import dev.breeze.settlements.config.ConfigType;
import dev.breeze.settlements.utils.LogUtil;
import dev.breeze.settlements.utils.TimeUtil;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.inject.Singleton;
import java.io.IOException;

@Singleton
@Getter
public final class NitwitPranksConfig {

    private static final String FILE_NAME = "nitwit_pranks";
    private static NitwitPranksConfig instance;

    // Pranks
    @Nonnull
    private final ConfigField<Float> prankMoveSpeedMin;
    @Nonnull
    private final ConfigField<Float> prankMoveSpeedMax;

    @Nonnull
    private final ConfigField<Integer> launchFireworkCooldown;
    @Nonnull
    private final ConfigField<Double> launchFireworkRange;

    @Nonnull
    private final ConfigField<Integer> ringBellCooldown;
    @Nonnull
    private final ConfigField<Integer> ringBellDistance;

    @Nonnull
    private final ConfigField<Float> runAroundSpeed;
    @Nonnull
    private final ConfigField<Integer> runAroundCooldown;
    @Nonnull
    private final ConfigField<Integer> runAroundDuration;

    @Nonnull
    private final ConfigField<Integer> snowballCooldown;
    @Nonnull
    private final ConfigField<Integer> snowballTargetRange;


    private NitwitPranksConfig() {
        ConfigFileWrapper wrapper;
        try {
            wrapper = new ConfigFileWrapper(FILE_NAME);
        } catch (IOException e) {
            LogUtil.exception(e, "Failed to load config file '%s'!", FILE_NAME);
            throw new RuntimeException(e);
        }

        // Load config fields
        this.prankMoveSpeedMin = new ConfigField<>(wrapper, ConfigType.FLOAT, "generic.speed.min",
                "How fast will the nitwit walk during pranks (lower bound)",
                0.4F);
        this.prankMoveSpeedMax = new ConfigField<>(wrapper, ConfigType.FLOAT, "generic.speed.max",
                "How fast will the nitwit walk during pranks (upper bound)",
                0.7F);

        this.launchFireworkCooldown = new ConfigField<>(wrapper, ConfigType.INT, "firework.cooldown",
                "How long will the nitwit wait before launching another firework (in ticks)",
                TimeUtil.minutes(5));
        this.launchFireworkRange = new ConfigField<>(wrapper, ConfigType.DOUBLE, "firework.range",
                "How close will the nitwit get to the target before launching a firework (in blocks)",
                6.0D);

        this.ringBellCooldown = new ConfigField<>(wrapper, ConfigType.INT, "bell.cooldown",
                "How long will the nitwit wait before ringing the bell again (in ticks)",
                TimeUtil.minutes(30));
        this.ringBellDistance = new ConfigField<>(wrapper, ConfigType.INT, "bell.range",
                "How close to the bell does the nitwit need to be to ring it (in blocks)",
                3);

        this.runAroundSpeed = new ConfigField<>(wrapper, ConfigType.FLOAT, "run.speed",
                "How fast will the nitwit move when they are running around",
                1.0F);
        this.runAroundCooldown = new ConfigField<>(wrapper, ConfigType.INT, "run.cooldown",
                "How long will the nitwit wait before running around again (in ticks)",
                TimeUtil.minutes(3));
        this.runAroundDuration = new ConfigField<>(wrapper, ConfigType.INT, "run.duration",
                "How long will the nitwit run around for (in ticks)",
                TimeUtil.seconds(10));

        this.snowballCooldown = new ConfigField<>(wrapper, ConfigType.INT, "snowball.cooldown",
                "How long will the nitwit wait before throwing snowballs again (in ticks)",
                TimeUtil.minutes(5));
        this.snowballTargetRange = new ConfigField<>(wrapper, ConfigType.INT, "snowball.range",
                "How close to the target does the nitwit need to be to throw snowballs at it (in blocks)",
                20);
    }

    public static synchronized NitwitPranksConfig getInstance() {
        if (instance == null) {
            instance = new NitwitPranksConfig();
        }
        return instance;
    }

}
