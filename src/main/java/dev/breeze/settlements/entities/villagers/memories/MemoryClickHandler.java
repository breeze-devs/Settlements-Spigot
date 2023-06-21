package dev.breeze.settlements.entities.villagers.memories;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface MemoryClickHandler<T> {

    void onClick(@Nonnull Player player, @Nonnull BaseVillager baseVillager, @Nullable Object memory, @Nonnull ClickType clickType);

}
