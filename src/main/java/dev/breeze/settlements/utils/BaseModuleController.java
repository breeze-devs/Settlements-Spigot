package dev.breeze.settlements.utils;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class BaseModuleController {

    /**
     * Called before the world loads
     *
     * @return whether the preload operation is successful
     */
    public final boolean performPreload(JavaPlugin plugin) {
        try {
            boolean successful = this.preload(plugin);
            DebugUtil.log("Preloading \"%s\" %s!", getClass().getName(), (successful ? "succeeded" : "failed"));
            return successful;
        } catch (Exception e) {
            LogUtil.exception(e, "Encountered exception while preloading %s module!", getClass().getName());
            return false;
        }
    }

    /**
     * Called by performPreload()
     */
    protected abstract boolean preload(JavaPlugin plugin);


    /**
     * Called after the world loads
     *
     * @return whether the load operation is successful
     */
    public final boolean performLoad(JavaPlugin plugin, PluginManager pm) {
        try {
            boolean successful = this.load(plugin, pm);
            DebugUtil.log("Loading \"%s\" %s!", getClass().getName(), (successful ? "succeeded" : "failed"));
            return successful;
        } catch (Exception e) {
            LogUtil.exception(e, "Encountered exception while loading %s module!", getClass().getName());
            return false;
        }
    }

    /**
     * Called by performLoad()
     */
    protected abstract boolean load(JavaPlugin plugin, PluginManager pm);

    /**
     * Called before the plugin disables
     */
    public final void performTeardown() {
        try {
            this.teardown();
            DebugUtil.log("Successfully torn down \"%s\" module!", getClass().getName());
        } catch (Exception e) {
            LogUtil.exception(e, "Encountered exception while tearing down %s module!", getClass().getName());
        }
    }

    /**
     * Called by performTeardown()
     */
    protected abstract void teardown();

}
