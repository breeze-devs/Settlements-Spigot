package dev.breeze.settlements.utils;

import org.bukkit.Sound;
import org.bukkit.entity.Player;


public class SoundPresets {

    public static void inventoryClickEnter(Player player) {
        SoundUtil.playSound(player, Sound.ENTITY_CHICKEN_EGG, 1.3f);
    }

    public static void inventoryClickExit(Player player) {
        SoundUtil.playSound(player, Sound.ENTITY_CHICKEN_EGG, 0.8f);
    }

    public static void inventorySave(Player player) {
        // G, F#, D#, A-, G#-, E, G#, C
        SoundUtil.playNotes(new float[]{1.059463F, 1F, 0.840896F, 0.594604F, 0.561231F, 0.890899F, 1.122462F, 1.414214F},
                Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, player, 2);
    }


}

