package dev.breeze.settlements.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.block.Biome;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static org.bukkit.block.Biome.*;

/**
 * Last updated: 1.19.3
 */
@AllArgsConstructor
@Getter
public enum Habitat {
    AQUATIC("Aquatic", Material.PRISMARINE, List.of(
            OCEAN, DEEP_OCEAN,
            WARM_OCEAN, LUKEWARM_OCEAN, DEEP_LUKEWARM_OCEAN,
            COLD_OCEAN, DEEP_COLD_OCEAN, FROZEN_OCEAN, DEEP_FROZEN_OCEAN,
            RIVER
    )),
    MUSHROOM("Mushroom Islands", Material.MYCELIUM, List.of(
            MUSHROOM_FIELDS
    )),
    HIGHLAND("Highland", Material.ANDESITE, List.of(
            STONY_PEAKS, MEADOW, /* TODO: cherry grove 1.20+ */ WINDSWEPT_HILLS, WINDSWEPT_GRAVELLY_HILLS, WINDSWEPT_FOREST
    )),
    SNOWY("Snowy", Material.SNOW_BLOCK, List.of(
            JAGGED_PEAKS, FROZEN_PEAKS, GROVE, SNOWY_SLOPES, SNOWY_TAIGA, FROZEN_RIVER, SNOWY_PLAINS, ICE_SPIKES
    )),
    FOREST("Forest", Material.OAK_SAPLING, List.of(
            Biome.FOREST, FLOWER_FOREST,
            TAIGA, OLD_GROWTH_PINE_TAIGA, OLD_GROWTH_SPRUCE_TAIGA,
            BIRCH_FOREST, OLD_GROWTH_BIRCH_FOREST,
            DARK_FOREST
    )),
    TROPICAL("Tropical", Material.JUNGLE_SAPLING, List.of(
            JUNGLE, SPARSE_JUNGLE, BAMBOO_JUNGLE
    )),
    GRASSLAND("Grassland", Material.GRASS_BLOCK, List.of(
            PLAINS, SUNFLOWER_PLAINS, MEADOW
    )),
    HILLS("Hills", Material.STONE, List.of(
            WINDSWEPT_HILLS, WINDSWEPT_GRAVELLY_HILLS
    )),
    WETLANDS("Wetlands", Material.LILY_PAD, List.of(
            SWAMP, MANGROVE_SWAMP, BEACH, SNOWY_BEACH, STONY_SHORE
    )),
    DESERT("Desert", Material.DEAD_BUSH, List.of(
            Biome.DESERT, BADLANDS, WOODED_BADLANDS, ERODED_BADLANDS
    )),
    SAVANNA("Savanna", Material.ACACIA_SAPLING, List.of(
            Biome.SAVANNA, SAVANNA_PLATEAU, WINDSWEPT_SAVANNA
    )),
    CAVE("Caves", Material.DEEPSLATE, List.of(
            DEEP_DARK, DRIPSTONE_CAVES, LUSH_CAVES
    )),
    NETHER("The Nether", Material.NETHERRACK, List.of(
            NETHER_WASTES, SOUL_SAND_VALLEY, CRIMSON_FOREST, WARPED_FOREST, BASALT_DELTAS
    )),
    END("The End", Material.END_STONE, List.of(
            THE_END, SMALL_END_ISLANDS, END_MIDLANDS, END_HIGHLANDS, END_BARRENS
    )),
    MISCELLANEOUS("Miscellaneous", Material.GLASS, List.of(
            THE_VOID, CUSTOM
    ));

    @Nonnull
    private final String name;
    @Nonnull
    private final Material material;
    @Nonnull
    private final List<Biome> biomes;

    @Nullable
    public static Habitat fromBiome(@Nonnull Biome biome) {
        for (Habitat habitat : Habitat.values()) {
            if (habitat.getBiomes().contains(biome)) {
                return habitat;
            }
        }
        return null;
    }

    public boolean isHot() {
        return this == TROPICAL || this == DESERT || this == SAVANNA || this == NETHER;
    }

}
