package dev.breeze.settlements.entities.villagers.memories;

import dev.breeze.settlements.config.files.InternalTradingConfig;
import dev.breeze.settlements.debug.guis.villager.VillagerDebugMemoryGui;
import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.utils.SoundPresets;
import lombok.Builder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.npc.Villager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class VillagerEmeraldBalanceMemory extends VillagerMemory<Integer> {

    public static final int INITIAL_EMERALD_BALANCE_MIN = InternalTradingConfig.getInstance().getInitialEmeraldBalanceMin().getValue();
    public static final int INITIAL_EMERALD_BALANCE_MAX = InternalTradingConfig.getInstance().getInitialEmeraldBalanceMax().getValue();

    @Builder(builderMethodName = "emeraldMemoryBuilder")
    public VillagerEmeraldBalanceMemory() {
        super("emerald_balance",
                balance -> List.of("&7Emerald balance: &e%d".formatted(balance)),
                new VillagerMemory.MemorySerializer<>() {
                    @Nonnull
                    @Override
                    public IntTag toTag(@Nonnull Integer balance) {
                        return IntTag.valueOf(balance);
                    }

                    @Nonnull
                    @Override
                    public Integer fromTag(@Nonnull CompoundTag memoriesTag, @Nonnull String key) {
                        return memoriesTag.getInt(key);
                    }
                },
                new EmeraldClickEventHandler(),
                "Emerald balance",
                List.of(
                        "&fThe number of emeralds that the villager has",
                        "&eLeft &7or &eshift-left click &7to increase by &e1 &7or &e10",
                        "&eRight &7or &eshift-right click &7to decrease by &e1 &7or &e10"
                ),
                Material.EMERALD);
    }

    @Override
    public void load(@Nonnull CompoundTag memoriesTag, @Nonnull Brain<Villager> brain) {
        // Check if we need to generate a random number of emeralds
        if (!memoriesTag.contains(this.getIdentifier())) {
            this.set(brain, generateRandomEmeraldBalance());
            return;
        }

        // Otherwise, load the memory
        super.load(memoriesTag, brain);
    }

    private static class EmeraldClickEventHandler implements MemoryClickHandler<Integer> {

        @Override
        public void onClick(@Nonnull Player player, @Nonnull BaseVillager baseVillager, @Nullable Object memory, @Nonnull ClickType clickType) {
            if (memory == null) {
                return;
            }

            int delta = switch (clickType) {
                case LEFT -> 1;
                case SHIFT_LEFT -> 10;
                case RIGHT -> -1;
                case SHIFT_RIGHT -> -10;
                default -> 0;
            };

            // Check if we need to update the memory
            if (delta == 0) {
                return;
            }

            // Update the memory
            VillagerMemoryType.EMERALD_BALANCE.set(baseVillager.getBrain(), (Integer) memory + delta);
            SoundPresets.inventoryAmountChange(player, delta > 0, !clickType.isShiftClick());

            // Re-open the UI
            VillagerDebugMemoryGui.getViewableInventory(player, baseVillager).showToPlayer(player);
        }

    }

    private static int generateRandomEmeraldBalance() {
        return RandomUtil.RANDOM.nextInt(INITIAL_EMERALD_BALANCE_MIN, INITIAL_EMERALD_BALANCE_MAX + 1);
    }

}
