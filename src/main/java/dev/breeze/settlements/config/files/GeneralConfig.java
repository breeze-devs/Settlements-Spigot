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
@Getter
public final class GeneralConfig {

    private static final String FILE_NAME = "config";
    private static GeneralConfig instance;

    @Nonnull
    private final ConfigField<Boolean> debugEnabled;

    private GeneralConfig() {
        ConfigFileWrapper wrapper;
        try {
            wrapper = new ConfigFileWrapper(FILE_NAME);
        } catch (IOException e) {
            LogUtil.exception(e, "Failed to load config file '%s'!", FILE_NAME);
            throw new RuntimeException(e);
        }

        // Load experimental config fields
        new ConfigField<>(wrapper, ConfigType.EMPTY, "experimental", List.of(
                "WARNING: This section contains unstable/untested features that may cause data loss, crashes, or other serious issues",
                "We strongly recommend against enabling this section unless you are an experienced user or a developer. Use at your own risk"
        ), "");
        this.debugEnabled = new ConfigField<>(wrapper, ConfigType.BOOLEAN, "experimental.debug",
                "Should the plugin broadcast detailed debug messages to everyone?",
                false);
    }

    public static synchronized GeneralConfig getInstance() {
        if (instance == null) {
            instance = new GeneralConfig();
        }
        return instance;
    }

}
