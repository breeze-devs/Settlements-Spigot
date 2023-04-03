package dev.breeze.settlements.guis;

import dev.breeze.settlements.utils.BaseModuleController;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class GuiModuleController extends BaseModuleController {

    @Override
    protected boolean preload(JavaPlugin plugin) {
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
