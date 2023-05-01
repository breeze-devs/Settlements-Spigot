package dev.breeze.settlements.config.files;

import com.google.common.collect.ImmutableSet;
import dev.breeze.settlements.config.ConfigField;
import dev.breeze.settlements.config.ConfigFileWrapper;
import dev.breeze.settlements.config.ConfigType;
import dev.breeze.settlements.utils.*;
import lombok.Getter;
import org.bukkit.Material;

import javax.annotation.Nonnull;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;

@Singleton
public final class EnchantItemsConfig {

    private static final String FILE_NAME = "enchant_items";
    private static EnchantItemsConfig instance;

    private static final Map<Integer, Set<Material>> DEFAULT_CAN_ENCHANT = Map.of(
            1, new ImmutableSet.Builder<Material>()
                    .addAll(ToolUtil.WOOD_TOOLS)
                    .addAll(ToolUtil.LEATHER_ARMOR)
                    .add(Material.WOODEN_SWORD)
                    .build(),
            2, new ImmutableSet.Builder<Material>()
                    .addAll(ToolUtil.STONE_TOOLS)
                    .addAll(ToolUtil.CHAIN_ARMOR)
                    .add(Material.TURTLE_HELMET)
                    .add(Material.STONE_SWORD)
                    .add(Material.FISHING_ROD)
                    .add(Material.SHEARS)
                    .build(),
            3, new ImmutableSet.Builder<Material>()
                    .addAll(ToolUtil.IRON_TOOLS)
                    .addAll(ToolUtil.IRON_ARMOR)
                    .add(Material.IRON_SWORD)
                    .addAll(ToolUtil.GOLD_TOOLS)
                    .addAll(ToolUtil.GOLD_ARMOR)
                    .add(Material.GOLDEN_SWORD)
                    .add(Material.BOW)
                    .add(Material.CROSSBOW)
                    .build(),
            4, new ImmutableSet.Builder<Material>()
                    .addAll(ToolUtil.DIAMOND_TOOLS)
                    .addAll(ToolUtil.DIAMOND_ARMOR)
                    .add(Material.DIAMOND_SWORD)
                    .add(Material.BOOK)
                    .build(),
            5, new ImmutableSet.Builder<Material>()
                    .addAll(ToolUtil.NETHERITE_TOOLS)
                    .addAll(ToolUtil.NETHERITE_ARMOR)
                    .add(Material.NETHERITE_SWORD)
                    .add(Material.TRIDENT)
                    .add(Material.ELYTRA)
                    .build()
    );
    private static final Map<Integer, Integer> DEFAULT_BOOKSHELVES_PER_LEVEL = Map.of(
            1, 2,
            2, 5,
            3, 8,
            4, 11,
            5, 15
    );

    @Getter
    @Nonnull
    private final ConfigField<Boolean> allowTreasure;

    @Getter
    @Nonnull
    private final ConfigField<Integer> enchantCooldown;

    @Nonnull
    private final Map<Integer, Set<Material>> cachedCanEnchantPerExpertiseMap;

    @Nonnull
    private final Map<Integer, Integer> cachedBookshelfPowerPerExpertiseMap;

    private EnchantItemsConfig() {
        ConfigFileWrapper wrapper;
        try {
            wrapper = new ConfigFileWrapper(FILE_NAME);
        } catch (IOException e) {
            LogUtil.exception(e, "Failed to load config file '%s'!", FILE_NAME);
            throw new RuntimeException(e);
        }

        // Load config fields
        new ConfigField<>(wrapper, ConfigType.EMPTY, "enchant", List.of(
                "Enchantment configurations"
        ), "");

        /*
         * Load basic fields
         */
        this.allowTreasure = new ConfigField<>(wrapper, ConfigType.BOOLEAN, "enchant.allow_treasure",
                List.of(
                        "Can villager enchant items with treasure-only enchantments?",
                        "Treasure enchantments are enchantments that cannot be obtained through the enchanting table",
                        "For example: various curses, mending, soul speed, etc",
                        "You can refer to https://minecraft.fandom.com/wiki/Enchanting#Summary_of_enchantments for more detailed information"
                ), true);

        this.enchantCooldown = new ConfigField<>(wrapper, ConfigType.INT, "enchant.cooldown",
                "How long will the librarian wait before enchanting another item (in ticks)",
                TimeUtil.minutes(15));

        /*
         * Load config for maximum bookshelf power of each level of expertise
         */
        new ConfigField<>(wrapper, ConfigType.EMPTY, "enchant.bookshelves", List.of(
                "The maximum number of bookshelves that the villager can use per their expertise",
                "The maximum enchantment level is 2x the number of bookshelves",
                "For example, an enchanting table with 10 bookshelves can enchant up to level 20"
        ), "");

        Map<Integer, ConfigField<Integer>> bookshelfPerExpertiseMap = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            // Create config field & add to map
            bookshelfPerExpertiseMap.put(i, new ConfigField<>(wrapper, ConfigType.INT,
                    "enchant.bookshelves.%s".formatted(VillagerUtil.getExpertiseName(i, false).toLowerCase(Locale.ROOT)),
                    List.of("The maximum number of bookshelves that %s librarians can use".formatted(VillagerUtil.getExpertiseName(i, false))),
                    DEFAULT_BOOKSHELVES_PER_LEVEL.get(i)));
        }

        // Validate & cache
        this.cachedBookshelfPowerPerExpertiseMap = new HashMap<>();
        for (Map.Entry<Integer, ConfigField<Integer>> entry : bookshelfPerExpertiseMap.entrySet()) {
            int clampedValue = Math.max(1, Math.min(15, entry.getValue().getValue()));
            this.cachedBookshelfPowerPerExpertiseMap.put(entry.getKey(), clampedValue);
        }

        /*
         * Load config for items that each level of expertise can enchant
         */
        new ConfigField<>(wrapper, ConfigType.EMPTY, "enchant.expertise", List.of(
                "The types of items that librarians can enchant vary depending on their level of expertise",
                "Librarians with higher expertise can enchant the same items as those with lower expertise, in addition to being able to enchant additional " +
                        "items unique to their level",
                "Note that all item fields in this configuration file are required to be valid Bukkit materials in order to function correctly",
                "You can refer to https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html for more detailed information on Bukkit materials"
        ), "");

        Map<Integer, ConfigField<List<String>>> canEnchantPerExpertiseMap = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            // Create config field & add to map
            canEnchantPerExpertiseMap.put(i, new ConfigField<>(wrapper, ConfigType.STRING_LIST,
                    "enchant.expertise.%s".formatted(VillagerUtil.getExpertiseName(i, false).toLowerCase(Locale.ROOT)),
                    "List of the items that %s librarians can enchant".formatted(VillagerUtil.getExpertiseName(i, false)),
                    StringUtil.materialListToStringList(new ArrayList<>(DEFAULT_CAN_ENCHANT.get(i)))));
        }

        // Validate & cache wanted items to memory
        this.cachedCanEnchantPerExpertiseMap = new HashMap<>();
        Set<Material> canEnchantSoFar = new HashSet<>();
        for (Map.Entry<Integer, ConfigField<List<String>>> entry : canEnchantPerExpertiseMap.entrySet()) {
            for (String materialString : entry.getValue().getValue()) {
                try {
                    canEnchantSoFar.add(Material.valueOf(materialString));
                } catch (IllegalArgumentException e) {
                    LogUtil.warning("Invalid Bukkit material '%s' in config file '%s.yml', ignoring item", materialString, FILE_NAME);
                }
            }
            this.cachedCanEnchantPerExpertiseMap.put(entry.getKey(), new HashSet<>(canEnchantSoFar));
        }

    }

    public Set<Material> canEnchantAtLevel(int expertiseLevel) {
        expertiseLevel = Math.max(1, Math.min(5, expertiseLevel));
        return this.cachedCanEnchantPerExpertiseMap.get(expertiseLevel);
    }

    public int maxBookshelvesAtLevel(int expertiseLevel) {
        expertiseLevel = Math.max(1, Math.min(5, expertiseLevel));
        return this.cachedBookshelfPowerPerExpertiseMap.get(expertiseLevel);
    }

    public static synchronized EnchantItemsConfig getInstance() {
        if (instance == null) {
            instance = new EnchantItemsConfig();
        }
        return instance;
    }

}
