package dev.breeze.settlements.entities.villagers.memories;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.wolves.VillagerWolf;
import dev.breeze.settlements.utils.StringUtil;
import dev.breeze.settlements.utils.particle.ParticlePreset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.npc.Villager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.*;

public class VillagerMemoryType {

    private static final String NBT_TAG_NAME = "settlements_memories";

    public static final VillagerMemory<Set<GlobalPos>> FENCE_GATE_TO_CLOSE = VillagerMemory.<Set<GlobalPos>>builder()
            .identifier("fence_gate_to_close")
            .parser(memory -> {
                if (memory.isEmpty()) {
                    return Collections.singletonList("&cNo gates to close");
                }

                List<String> lore = new ArrayList<>();
                for (GlobalPos globalPos : memory) {
                    BlockPos pos = globalPos.pos();
                    lore.add("&7- &e%s&7: x: %d, y: %d, z: %d".formatted(StringUtil.toTitleCase(globalPos.dimension().location().toShortLanguageKey()),
                            pos.getX(), pos.getY(), pos.getZ()));
                }
                return lore;
            })
            .serializer(null)
            .clickEventHandler(null)
            .displayName("Opened gates")
            .description(Collections.singletonList("&fThe fence gate(s) that the villager should to close when passing by"))
            .itemMaterial(Material.OAK_FENCE_GATE)
            .build();

    public static final VillagerMemory<UUID> OWNED_DOG = VillagerMemory.<UUID>builder()
            .identifier("owned_dog")
            .parser(memory -> List.of("&7UUID: %s".formatted(memory.toString()), "&eClick &7to show the wolf's location"))
            .serializer(new VillagerMemory.MemorySerializer<>() {
                @Nonnull
                @Override
                public StringTag toTag(@Nonnull UUID memory) {
                    return StringTag.valueOf(memory.toString());
                }

                @Nonnull
                @Override
                public UUID fromTag(@Nonnull CompoundTag memoriesTag, @Nonnull String key) {
                    return UUID.fromString(memoriesTag.getString(key));
                }
            })
            .clickEventHandler((player, memory) -> {
                Entity wolf = Bukkit.getEntity((UUID) memory);
                if (wolf == null) {
                    return;
                }

                player.closeInventory();
                highlightLocation(player, wolf.getLocation().add(0, 0.2, 0));
            })
            .displayName("Tamed wolf")
            .description(Collections.singletonList("&fThe wolf that this villager has tamed"))
            .itemMaterial(Material.BONE)
            .build();

    public static final VillagerMemory<UUID> OWNED_CAT = VillagerMemory.<UUID>builder()
            .identifier("owned_cat")
            .parser(memory -> List.of("&7UUID: %s".formatted(memory.toString()), "&eClick &7to show the cat's location"))
            .serializer(new VillagerMemory.MemorySerializer<>() {
                @Nonnull
                @Override
                public StringTag toTag(@Nonnull UUID memory) {
                    return StringTag.valueOf(memory.toString());
                }

                @Nonnull
                @Override
                public UUID fromTag(@Nonnull CompoundTag memoriesTag, @Nonnull String key) {
                    return UUID.fromString(memoriesTag.getString(key));
                }
            })
            .clickEventHandler((player, memory) -> {
                Entity cat = Bukkit.getEntity((UUID) memory);
                if (cat == null) {
                    return;
                }

                // Close inventory & display particles to the tamed cat
                player.closeInventory();
                highlightLocation(player, cat.getLocation().add(0, 0.2, 0));
            })
            .displayName("Tamed cat")
            .description(Collections.singletonList("&fThe cat that this villager has tamed"))
            .itemMaterial(Material.COD)
            .build();

    public static final VillagerMemory<VillagerWolf> WALK_DOG_TARGET = VillagerMemory.<VillagerWolf>builder()
            .identifier("walk_dog_target")
            .parser(memory -> Collections.singletonList("&7UUID: %s".formatted(memory.getUUID().toString())))
            .serializer(null)
            .clickEventHandler(null)
            .displayName("Walk target")
            .description(Collections.singletonList("&fThe tamed wolf that requested to go on a walk"))
            .itemMaterial(Material.LEAD)
            .build();

    public static final VillagerMemory<BlockPos> NEAREST_WATER_AREA = VillagerBlockPosMemory.blockPosMemoryBuilder()
            .identifier("nearest_water_area")
            .displayName("Nearest fishing location")
            .description(List.of("&fThe closest location big enough for the villager to fish in", "&eClick &7to show the fishing location"))
            .itemMaterial(Material.FISHING_ROD)
            .build();

    public static final VillagerMemory<Boolean> IS_MEAL_TIME = VillagerMemory.<Boolean>builder()
            .identifier("meal_time")
            .parser(memory -> Collections.singletonList(memory ? "Yes" : "No"))
            .serializer(null)
            .clickEventHandler(null)
            .displayName("Meal time")
            .description(List.of(
                    "&fIs it a good time to eat now?",
                    "&fMeal times:",
                    "&7- Breakfast: 1800 - 2200 ticks",
                    "&7- Lunch: 5800 - 6200 ticks",
                    "&7- Dinner: 10800 - 11200 ticks"
            ))
            .itemMaterial(Material.BREAD)
            .build();

    public static final VillagerMemory<BlockPos> NEAREST_ENCHANTING_TABLE = VillagerBlockPosMemory.blockPosMemoryBuilder()
            .identifier("nearest_enchanting_table")
            .displayName("Nearest enchanting table")
            .description(List.of("&fThe closest visible enchanting table", "&eClick &7to show the enchanting table's location"))
            .itemMaterial(Material.ENCHANTING_TABLE)
            .build();

    public static final VillagerMemory<BlockPos> NEAREST_HARVESTABLE_SUGARCANE = VillagerBlockPosMemory.blockPosMemoryBuilder()
            .identifier("nearest_harvestable_sugarcane")
            .displayName("Nearest harvestable sugarcane")
            .description(List.of(
                    "&fThe closest visible sugarcane block that is ready for harvest",
                    "&fSugarcane that are at least 2 blocks tall are deemed harvestable",
                    "&eClick &7to show the sugarcane's location"
            ))
            .itemMaterial(Material.SUGAR_CANE)
            .build();

    /**
     * List of all memories for bulk memory operations such as save/load
     */
    public static final List<VillagerMemory<?>> ALL_MEMORIES = Arrays.asList(FENCE_GATE_TO_CLOSE, OWNED_DOG, OWNED_CAT, WALK_DOG_TARGET, NEAREST_WATER_AREA,
            IS_MEAL_TIME, NEAREST_ENCHANTING_TABLE, NEAREST_HARVESTABLE_SUGARCANE);

    /**
     * Export important memories to NBT
     * - only certain memories are persistent
     * - other are deleted upon unloading
     */
    public static void save(@Nonnull CompoundTag nbt, @Nonnull BaseVillager villager) {
        Brain<Villager> brain = villager.getBrain();
        CompoundTag memories = new CompoundTag();
        for (VillagerMemory<?> memory : ALL_MEMORIES) {
            memory.save(memories, brain);
        }

        // Write to NBT tag
        nbt.put(NBT_TAG_NAME, memories);
    }

    /**
     * Attempts to load the custom memories to the villager brain
     */
    public static void load(@Nonnull CompoundTag nbt, @Nonnull BaseVillager villager) {
        // Safety check
        if (!nbt.contains(NBT_TAG_NAME))
            return;

        // Load memories to brain
        Brain<Villager> brain = villager.getBrain();
        CompoundTag memories = nbt.getCompound(NBT_TAG_NAME);
        for (VillagerMemory<?> memory : ALL_MEMORIES) {
            memory.load(memories, brain);
        }
    }

    /*
     * Utility methods
     */

    /**
     * Displays particles to highlight a location that's only visible to the player
     *
     * @param player player to display the particles to
     * @param target location to highlight
     */
    private static void highlightLocation(Player player, Location target) {
        ParticlePreset.displayLinePrivate(player, player.getEyeLocation(), target, 15, Particle.END_ROD, 2, 0, 0, 0, 0);
        ParticlePreset.displayCirclePrivate(player, target, 1, 20, Particle.VILLAGER_HAPPY, 1, 0, 0, 0, 0);
    }

}
