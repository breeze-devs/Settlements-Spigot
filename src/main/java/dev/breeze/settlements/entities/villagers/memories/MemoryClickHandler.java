package dev.breeze.settlements.entities.villagers.memories;

import org.bukkit.entity.Player;

import javax.annotation.Nonnull;

public interface MemoryClickHandler<T> {

    void onClick(@Nonnull Player player, @Nonnull Object memory);

}
