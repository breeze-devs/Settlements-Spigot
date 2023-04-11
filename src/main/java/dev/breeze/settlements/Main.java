package dev.breeze.settlements;

import dev.breeze.settlements.config.ConfigModuleController;
import dev.breeze.settlements.entities.EntityModuleController;
import dev.breeze.settlements.guis.GuiModuleController;
import dev.breeze.settlements.test.TestModuleController;
import dev.breeze.settlements.utils.BaseModuleController;
import dev.breeze.settlements.utils.LogUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class Main extends JavaPlugin {

    @Getter
    @Setter
    private static JavaPlugin plugin;

    private boolean disablePlugin = false;

    private final BaseModuleController[] moduleControllers = new BaseModuleController[]{
            new ConfigModuleController(),
            new EntityModuleController(),
            new GuiModuleController(),
            new TestModuleController(),
    };

    /**
     * Called before the world loads
     */
    @Override
    public void onLoad() {
        super.onLoad();

        // Allow global access to this plugin
        setPlugin(this);
        LogUtil.init(this);

        // Initialize plugin data folder
        File f = new File(String.format("%s/", plugin.getDataFolder()));
        if (!f.exists())
            f.mkdir();

        // Preload modules
        LogUtil.info("Preloading modules...");
        for (BaseModuleController bmc : this.moduleControllers) {
            if (!bmc.performPreload(this)) {
                this.disableSelf("Failed to preload module '%s'!", bmc.getClass().getName());
            }
        }
    }

    /**
     * Called after the world loads
     */
    @Override
    public void onEnable() {
        // Abandon loading if disabled
        if (this.disablePlugin) {
            LogUtil.info("Disabling plugin due to failed initialization...");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Load modules
        LogUtil.info("Loading modules...");
        PluginManager pm = this.getServer().getPluginManager();
        for (BaseModuleController bmc : this.moduleControllers) {
            if (!bmc.performLoad(this, pm)) {
                this.disableSelf("Failed to load module '%s'!", bmc.getClass());
            }
        }

        // Signal that plugin is enabled
        PluginDescriptionFile desc = this.getDescription();
        LogUtil.info("Enabled %s v%s", desc.getName(), desc.getVersion());
    }

    @Override
    public void onDisable() {
        // Tear down modules
        for (BaseModuleController bmc : this.moduleControllers) {
            bmc.performTeardown();
        }

        // Signal that the plugin is disabled
        PluginDescriptionFile desc = this.getDescription();
        LogUtil.info("Disabled %s v%s", desc.getName(), desc.getVersion());
    }

    private void disableSelf(String errorMessage, Object... args) {
        LogUtil.severe(errorMessage, args);
        this.disablePlugin = true;
    }

}
