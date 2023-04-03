package dev.breeze.settlements.utils;

import org.bukkit.scheduler.BukkitRunnable;

public abstract class SafeRunnable extends BukkitRunnable {

    @Override
    public void run() {
        try {
            safeRun();
        } catch (Exception ex) {
            this.cancel();
            LogUtil.severe("Exception occurred while executing runnable, cancelling future runs!");
            ex.printStackTrace();
        }
    }

    /**
     * Put runnable method body in here, when an exception occurs, the entire runnable task will be cancelled
     */
    public abstract void safeRun();

}
