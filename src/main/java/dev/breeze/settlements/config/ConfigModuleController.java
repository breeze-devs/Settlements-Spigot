package dev.breeze.settlements.config;

import dev.breeze.settlements.config.files.NitwitPranksConfig;
import dev.breeze.settlements.utils.BaseModuleController;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigModuleController extends BaseModuleController {

    @Override
    protected boolean preload(JavaPlugin plugin) {
        // Create instances of all config singletons
//        GeneralConfig.getInstance();
        NitwitPranksConfig.getInstance();
//        VillagerTradeItemConfig.getInstance();
        return true;
    }

    @Override
    protected boolean load(JavaPlugin plugin, PluginManager pm) {
        return true;
    }

    @Override
    protected void teardown() {
        // Do nothing
    }

}
