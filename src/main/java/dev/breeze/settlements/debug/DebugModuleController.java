package dev.breeze.settlements.debug;

import dev.breeze.settlements.debug.guis.villager.VillagerDebugBehaviorGui;
import dev.breeze.settlements.debug.guis.villager.VillagerDebugMainGui;
import dev.breeze.settlements.debug.guis.villager.VillagerDebugMemoryGui;
import dev.breeze.settlements.debug.guis.villager.VillagerDebugSensorGui;
import dev.breeze.settlements.utils.BaseModuleController;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class DebugModuleController extends BaseModuleController {

    @Override
    protected boolean preload(JavaPlugin plugin) {
        return true;
    }

    @Override
    protected boolean load(JavaPlugin plugin, PluginManager pm) {
        // Register commands
        plugin.getCommand("settlements_debug").setExecutor(new DebugCommandHandler());

        // Register events
        pm.registerEvents(new MemoryEvent(), plugin);

        // Inventory events
        pm.registerEvents(new VillagerDebugMainGui(), plugin);
        pm.registerEvents(new VillagerDebugBehaviorGui(), plugin);
        pm.registerEvents(new VillagerDebugMemoryGui(), plugin);
        pm.registerEvents(new VillagerDebugSensorGui(), plugin);
        return true;
    }

    @Override
    protected void teardown() {
        // Do nothing
    }

}
