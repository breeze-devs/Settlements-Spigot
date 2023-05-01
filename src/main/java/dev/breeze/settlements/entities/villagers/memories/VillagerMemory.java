package dev.breeze.settlements.entities.villagers.memories;

import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a custom villager memory
 *
 * @param <T> The type of the memory value, check out {@link MemoryModuleType} for more details
 */
public class VillagerMemory<T> {

    /**
     * A formatter for generating the identifier string, used as the Minecraft registry key
     */
    private static final String IDENTIFIER_FORMATTER = "settlements_villager_%s_memory";

    @Getter
    @Nonnull
    private final String identifier;

    @Nonnull
    private final MemoryParser<T> parser;

    /**
     * The serializer responsible for converting this memory to and from NBT format
     * - if null, this memory will be considered transient and deleted upon entity/chunk unload
     * - otherwise, this memory will be considered persistent and saved as NBT data
     */
    @Nullable
    private final MemorySerializer<T> serializer;

    @Getter
    @Nullable
    private final MemoryClickHandler<T> clickEventHandler;

    /**
     * The memory module type for this memory
     * - set externally at entity registration time through the lombok setter
     */
    @Getter
    @Setter
    private MemoryModuleType<T> memoryModuleType;

    /*
     * GUI variables
     */
    @Nonnull
    private final String displayName;
    @Nonnull
    private final List<String> description;

    @Setter
    @Nonnull
    private Material itemMaterial = Material.PAPER;

    @Builder
    protected VillagerMemory(@Nonnull String identifier,
                             @Nonnull MemoryParser<T> parser, @Nullable MemorySerializer<T> serializer, @Nullable MemoryClickHandler<T> clickEventHandler,
                             @Nonnull String displayName, @Nonnull List<String> description, @Nullable Material itemMaterial) {
        this.identifier = IDENTIFIER_FORMATTER.formatted(identifier);
        this.parser = parser;
        this.serializer = serializer;
        this.clickEventHandler = clickEventHandler;

        this.displayName = displayName;
        this.description = description;

        // If null, use default value (paper)
        if (itemMaterial != null) {
            this.itemMaterial = itemMaterial;
        }
    }

    /**
     * Retrieves the memory value for this memory type from the given villager's brain
     *
     * @param brain the villager's brain
     * @return the memory value, or null if not present
     */
    @Nullable
    public T get(@Nonnull Brain<Villager> brain) {
        // If no memory exists, return null
        if (!brain.hasMemoryValue(this.memoryModuleType)) {
            return null;
        }

        // Return the memory
        return brain.getMemory(this.memoryModuleType).get();
    }

    /**
     * Sets the memory value for this memory type in the given villager's brain
     *
     * @param brain the villager's brain.
     * @param value the memory value, or null to clear the memory
     */
    public void set(@Nonnull Brain<Villager> brain, @Nullable T value) {
        // Clear memory if set to null
        if (value == null) {
            brain.eraseMemory(this.memoryModuleType);
            return;
        }

        brain.setMemory(this.memoryModuleType, value);
    }

    public ItemStack getGuiItem(@Nonnull Brain<Villager> brain) {
        List<String> lore = new ArrayList<>(this.description);
        lore.add("&7---");
        T memory = this.get(brain);
        if (memory == null) {
            lore.add("&cNo memory");
        } else {
            lore.addAll(this.parser.getDisplayString(memory));
        }

        return new ItemStackBuilder(this.itemMaterial)
                .setDisplayName("&e%s".formatted(this.displayName))
                .setLore(lore)
                .build();
    }

    /**
     * Saves this memory entry to the given NBT tag if persistent and villager has memory
     *
     * @param memoriesTag the NBT tag to save the memory entry to
     * @param brain       the villager's brain
     */
    public void save(@Nonnull CompoundTag memoriesTag, @Nonnull Brain<Villager> brain) {
        // Check persistence and if villager has memory
        if (!this.isPersistent() || !brain.hasMemoryValue(this.memoryModuleType)) {
            return;
        }

        // Save to the memories tag
        memoriesTag.put(this.identifier, this.serializer.toTag(brain.getMemory(this.memoryModuleType).get()));
    }

    /**
     * Loads this memory entry from the given NBT tag if persistent and previously saved
     *
     * @param memoriesTag the NBT tag to load the memory entry from
     * @param brain       the villager's brain to load the memory entry into
     */
    public void load(@Nonnull CompoundTag memoriesTag, @Nonnull Brain<Villager> brain) {
        // Check persistence and if memory is saved to NBT
        if (!this.isPersistent() || !memoriesTag.contains(this.identifier)) {
            return;
        }

        // Load memories to brain
        brain.setMemory(this.memoryModuleType, this.serializer.fromTag(memoriesTag, this.identifier));
    }

    /**
     * Helper method for determining whether this memory is persistent
     *
     * @return true if this memory is persistent, false otherwise
     */
    public boolean isPersistent() {
        return this.serializer != null;
    }


    /**
     * The interface for converting memory values to user-friendly strings
     *
     * @param <T> the type of the memory value
     */
    public interface MemoryParser<T> {

        /**
         * Converts the memory to a list of color-coded string(s)
         *
         * @param memory the memory value to convert
         * @return the debug string(s) representing the memory value
         */
        @Nonnull
        List<String> getDisplayString(@Nonnull T memory);

    }

    /**
     * The interface for converting memory values to and from NBT format
     *
     * @param <T> the type of the memory value
     */
    public interface MemorySerializer<T> {

        /**
         * Converts the memory entry to an NBT tag
         *
         * @param memory the memory value to convert
         * @return the NBT tag representing the memory value
         */
        @Nonnull
        Tag toTag(@Nonnull T memory);

        /**
         * Loads the memory entry from an NBT tag
         *
         * @param memoriesTag the NBT tag containing the memory value
         * @param key         the key for the memory value in the NBT tag
         * @return the memory value represented by the NBT tag
         */
        @Nonnull
        T fromTag(@Nonnull CompoundTag memoriesTag, @Nonnull String key);

    }

}
