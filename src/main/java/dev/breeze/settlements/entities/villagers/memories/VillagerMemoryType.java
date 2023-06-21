package dev.breeze.settlements.entities.villagers.memories;

import dev.breeze.settlements.debug.guis.villager.VillagerDebugShoppingListGui;
import dev.breeze.settlements.entities.villagers.BaseVillager;
import dev.breeze.settlements.entities.villagers.sensors.VillagerMealTimeSensor;
import dev.breeze.settlements.entities.wolves.VillagerWolf;
import dev.breeze.settlements.utils.*;
import dev.breeze.settlements.utils.particle.ParticlePreset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
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
            .clickEventHandler((player, baseVillager, memory, clickType) -> {
                if (memory == null) {
                    return;
                }

                Entity wolf = Bukkit.getEntity((UUID) memory);
                if (wolf == null) {
                    return;
                }

                player.closeInventory();
                SoundPresets.inventoryClickExit(player);
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
            .clickEventHandler((player, baseVillager, memory, clickType) -> {
                if (memory == null) {
                    return;
                }

                Entity cat = Bukkit.getEntity((UUID) memory);
                if (cat == null) {
                    return;
                }

                // Close inventory & display particles to the tamed cat
                player.closeInventory();
                SoundPresets.inventoryClickExit(player);
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
            .parser(memory -> Collections.singletonList(memory ? "&aYes" : "&cNo"))
            .serializer(null)
            .clickEventHandler(null)
            .displayName("Meal time")
            .description(List.of(
                    "&fIs it a good time to eat now?",
                    "&fMeal times:",
                    "&7- Breakfast: %d - %d ticks".formatted(VillagerMealTimeSensor.BREAKFAST_START, VillagerMealTimeSensor.BREAKFAST_END),
                    "&7- Lunch: %d - %d ticks".formatted(VillagerMealTimeSensor.LUNCH_START, VillagerMealTimeSensor.LUNCH_END),
                    "&7- Dinner: %d - %d ticks".formatted(VillagerMealTimeSensor.DINNER_START, VillagerMealTimeSensor.DINNER_END)
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

    public static final VillagerMemory<Habitat> CURRENT_HABITAT = VillagerMemory.<Habitat>builder()
            .identifier("current_habitat")
            .parser(habitat -> List.of("&7Current habitat: &e" + habitat.getName()))
            .serializer(new VillagerMemory.MemorySerializer<>() {
                @Nonnull
                @Override
                public StringTag toTag(@Nonnull Habitat habitat) {
                    return StringTag.valueOf(habitat.toString());
                }

                @Nonnull
                @Override
                public Habitat fromTag(@Nonnull CompoundTag memoriesTag, @Nonnull String key) {
                    String habitatString = memoriesTag.getString(key);
                    try {
                        return Habitat.valueOf(habitatString);
                    } catch (IllegalArgumentException ex) {
                        LogUtil.exception(ex, "Unable to parse villager's habitat memory %s!", habitatString);
                        throw ex;
                    }
                }
            })
            .clickEventHandler(null)
            .displayName("Current habitat")
            .description(List.of("&fThe habitat that the villager is currently in"))
            .itemMaterial(Material.GRASS_BLOCK)
            .build();

    public static final VillagerMemory<HashMap<Material, Integer>> SHOPPING_LIST = VillagerMemory.<HashMap<Material, Integer>>builder()
            .identifier("shopping_list")
            .parser(shopping_list -> List.of("&7Shopping list has &e%d &7item types".formatted(shopping_list.size())))
            .serializer(new VillagerMemory.MemorySerializer<>() {
                @Nonnull
                @Override
                public ListTag toTag(@Nonnull HashMap<Material, Integer> memory) {
                    ListTag listTag = new ListTag();
                    for (Map.Entry<Material, Integer> entry : memory.entrySet()) {
                        CompoundTag itemTag = new CompoundTag();
                        itemTag.putString("material", entry.getKey().name());
                        itemTag.putInt("amount", entry.getValue());
                        listTag.add(itemTag);
                    }
                    return listTag;
                }

                @Nonnull
                @Override
                public HashMap<Material, Integer> fromTag(@Nonnull CompoundTag memoriesTag, @Nonnull String key) {
                    ListTag listTag = memoriesTag.getList(key, ListTag.TAG_COMPOUND);
                    HashMap<Material, Integer> shoppingList = new HashMap<>();
                    for (Tag tag : listTag) {
                        CompoundTag itemTag = (CompoundTag) tag;
                        try {
                            Material material = Material.valueOf(itemTag.getString("material"));
                            int amount = itemTag.getInt("amount");
                            shoppingList.put(material, amount);
                        } catch (IllegalArgumentException ex) {
                            LogUtil.exception(ex, "Unable to parse villager's shopping list memory (%s, %d), ignoring item",
                                    itemTag.getString("material"), itemTag.getInt("amount"));
                        }
                    }
                    return shoppingList;
                }
            })
            .clickEventHandler((player, baseVillager, memory, clickType) -> {
                VillagerDebugShoppingListGui.getViewableInventory(player, baseVillager).showToPlayer(player);
                SoundPresets.inventoryOpen(player);
            })
            .displayName("Shopping list")
            .description(List.of("&fThe items that the villager wants to trade for", "&eClick &7to view/edit the shopping list"))
            .itemMaterial(Material.CHEST)
            .build();

    @SuppressWarnings("unchecked")
    public static final VillagerMemory<Map<UUID, Material>> NEARBY_SELLERS = VillagerMemory.<Map<UUID, Material>>builder()
            .identifier("nearby_sellers")
            .parser(sellers -> List.of("&7Found &e%d &7sellers".formatted(sellers.size())))
            .serializer(null)
            .clickEventHandler((player, baseVillager, memory, clickType) -> {
                if (memory == null) {
                    return;
                }

                Map<UUID, Material> memoryMap = (Map<UUID, Material>) memory;
                // Do nothing if there are no sellers
                if (memoryMap.isEmpty()) {
                    return;
                }
                for (UUID sellerUuid : memoryMap.keySet()) {
                    Entity sellerVillager = Bukkit.getEntity(sellerUuid);
                    if (sellerVillager == null) {
                        continue;
                    }
                    highlightLocation(player, sellerVillager.getLocation().add(0, 0.2, 0));
                }
                player.closeInventory();
                SoundPresets.inventoryClickExit(player);
            })
            .displayName("Nearby sellers")
            .description(List.of("&fThe sellers that the villager wants to trade with", "&eClick &7to show the sellers' locations"))
            .itemMaterial(Material.VILLAGER_SPAWN_EGG)
            .build();

    public static final VillagerMemory<Integer> EMERALD_BALANCE = VillagerEmeraldBalanceMemory.emeraldMemoryBuilder().build();

    /**
     * List of all memories for bulk memory operations such as save/load
     */
    public static final List<VillagerMemory<?>> ALL_MEMORIES = Arrays.asList(
            // Pet related memories
            OWNED_DOG, OWNED_CAT, WALK_DOG_TARGET,
            // Point of interest (POI) related memories
            NEAREST_WATER_AREA, NEAREST_ENCHANTING_TABLE, NEAREST_HARVESTABLE_SUGARCANE,
            // Trading related memories
            SHOPPING_LIST, NEARBY_SELLERS, EMERALD_BALANCE,
            // Miscellaneous memories
            FENCE_GATE_TO_CLOSE, IS_MEAL_TIME, CURRENT_HABITAT
    );

    /**
     * Export important memories to NBT
     * - only certain memories are persistent
     * - others are deleted upon unloading
     */
    public static void save(@Nonnull CompoundTag nbt, @Nonnull BaseVillager villager) {
        Brain<Villager> brain = villager.getBrain();
        CompoundTag memories = new CompoundTag();
        for (VillagerMemory<?> memory : ALL_MEMORIES) {
            memory.save(memories, brain);
        }

        // Write to NBT tag
        nbt.put(NBT_TAG_NAME, memories);

        DebugUtil.log("Saved villager (%s) memories: %s", villager.getStringUUID(), memories.toString());
    }

    /**
     * Attempts to load the custom memories to the villager brain
     */
    public static void load(@Nonnull CompoundTag nbt, @Nonnull BaseVillager villager) {
        Brain<Villager> brain = villager.getBrain();

        // Use empty tag if not found (to facilitate default memory values)
        CompoundTag memories;
        if (!nbt.contains(NBT_TAG_NAME)) {
            memories = new CompoundTag();
        } else {
            memories = nbt.getCompound(NBT_TAG_NAME);
        }

        // Load memories
        for (VillagerMemory<?> memory : ALL_MEMORIES) {
            memory.load(memories, brain);
        }

        DebugUtil.log("Loaded villager (%s) memories: %s", villager.getStringUUID(), memories.toString());
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
