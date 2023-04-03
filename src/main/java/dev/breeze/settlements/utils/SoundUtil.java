package dev.breeze.settlements.utils;

import dev.breeze.settlements.Main;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;


public class SoundUtil {

    public static final float DEFAULT_VOLUME = 0.7f;

    public static void playSound(Player player, Sound sound, float pitch) {
        playSound(player, sound, DEFAULT_VOLUME, pitch);
    }

    public static void playSound(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, DEFAULT_VOLUME, pitch);
    }

    public static void playSoundPublic(Location loc, Sound sound, float pitch) {
        playSoundPublic(loc, sound, DEFAULT_VOLUME, pitch);
    }

    public static void playSoundPublic(Location loc, Sound sound, float volume, float pitch) {
        loc.getWorld().playSound(loc, sound, DEFAULT_VOLUME, pitch);
    }

    public static void playNotes(float[] notes, Sound sound, Player p, int interval) {
        new SafeRunnable() {
            int count = 0;

            @Override
            public void safeRun() {
                if (count >= notes.length) {
                    this.cancel();
                    return;
                }
                float tone = notes[count];
                if (tone != 0)
                    p.playSound(p.getLocation(), sound, DEFAULT_VOLUME, tone);
                count++;
            }
        }.runTaskTimer(Main.getPlugin(), 0, interval);
    }

    public static void playNotesPublic(float[] notes, Sound sound, Location loc, int interval) {
        new SafeRunnable() {
            int count = 0;

            @Override
            public void safeRun() {
                if (count >= notes.length) {
                    this.cancel();
                    return;
                }
                float tone = notes[count];
                if (tone == 0)
                    return;
                loc.getWorld().playSound(loc, sound, DEFAULT_VOLUME, tone);
                count++;
            }
        }.runTaskTimer(Main.getPlugin(), 0, interval);
    }


}

