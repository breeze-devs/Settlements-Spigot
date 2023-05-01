package dev.breeze.settlements.config;

import dev.breeze.settlements.config.files.EnchantItemsConfig;
import dev.breeze.settlements.config.files.GeneralConfig;
import dev.breeze.settlements.config.files.NitwitPranksConfig;
import dev.breeze.settlements.config.files.WolfFetchItemConfig;
import dev.breeze.settlements.utils.BaseModuleController;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigModuleController extends BaseModuleController {

    @Override
    protected boolean preload(JavaPlugin plugin) {
        // Create instances of all config singletons
        GeneralConfig.getInstance();
        NitwitPranksConfig.getInstance();
        WolfFetchItemConfig.getInstance();
        EnchantItemsConfig.getInstance();
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
