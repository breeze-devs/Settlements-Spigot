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


}

