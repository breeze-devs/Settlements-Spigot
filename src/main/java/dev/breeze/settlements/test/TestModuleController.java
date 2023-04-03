package dev.breeze.settlements.test;

import dev.breeze.settlements.utils.BaseModuleController;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class TestModuleController extends BaseModuleController {

    @Override
    protected boolean preload(JavaPlugin plugin) {
        return true;
    }

    @Override
    protected boolean load(JavaPlugin plugin, PluginManager pm) {
        plugin.getCommand("settlements_test").setExecutor(new TestCommandHandler());

        pm.registerEvents(new MemoryEvent(), plugin);
        return true;
    }

    @Override
    protected void teardown() {
        // Do nothing
    }

}
