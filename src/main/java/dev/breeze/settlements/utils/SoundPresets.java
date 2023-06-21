package dev.breeze.settlements.utils;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;


public class SoundPresets {

    public static void inventoryClickEnter(@Nonnull Player player) {
        SoundUtil.playSound(player, Sound.ENTITY_CHICKEN_EGG, 1.3f);
    }

    public static void inventoryClickExit(@Nonnull Player player) {
        SoundUtil.playSound(player, Sound.ENTITY_CHICKEN_EGG, 0.8f);
    }

    public static void inventoryOpen(@Nonnull Player player) {
        SoundUtil.playSound(player, Sound.BLOCK_CHEST_OPEN, 1F + RandomUtil.RANDOM.nextFloat() / 2F);
    }

    public static void inventoryClose(@Nonnull Player player) {
        SoundUtil.playSound(player, Sound.BLOCK_CHEST_CLOSE, 1F + RandomUtil.RANDOM.nextFloat() / 2F);
    }

    public static void inventoryAmountChange(@Nonnull Player player, boolean increase, boolean small) {
        float[] notes;
        if (increase) {
            notes = small ? new float[]{0.707107F, 0.890899F} : new float[]{0.707107F, 0.890899F, 1.059463F};
        } else {
            notes = small ? new float[]{0.890899F, 0.707107F} : new float[]{1.059463F, 0.890899F, 0.707107F};
        }
        SoundUtil.playNotes(notes, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, player, 1);
    }

    public static void zeldaPuzzleSolved(@Nonnull Player player) {
        // G, F#, D#, A-, G#-, E, G#, C
        SoundUtil.playNotes(new float[]{1.059463F, 1F, 0.840896F, 0.594604F, 0.561231F, 0.890899F, 1.122462F, 1.414214F},
                Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, player, 2);
    }

}

