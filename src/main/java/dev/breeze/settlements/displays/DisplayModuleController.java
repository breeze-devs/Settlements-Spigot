package dev.breeze.settlements.displays;

import dev.breeze.settlements.utils.BaseModuleController;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

public class DisplayModuleController extends BaseModuleController {

    public static final Set<TransformedDisplay> TEMPORARY_DISPLAYS = new HashSet<>();

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
        // Remove all temporary displays
        TEMPORARY_DISPLAYS.forEach(TransformedDisplay::remove);
    }

}
