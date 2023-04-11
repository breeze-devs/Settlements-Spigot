package dev.breeze.settlements.config.files;

import dev.breeze.settlements.config.ConfigField;
import dev.breeze.settlements.config.ConfigFileWrapper;
import dev.breeze.settlements.config.ConfigType;
import dev.breeze.settlements.utils.LogUtil;
import net.minecraft.world.entity.npc.VillagerProfession;
import org.bukkit.Material;

import javax.annotation.Nonnull;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;

@Singleton
public final class WolfFetchItemConfig {

    private static final String FILE_NAME = "wolf_fetch";
    private static final String DESCRIPTION_FORMAT = "The items that wolves tamed by %s villagers will pick up";
    private static WolfFetchItemConfig instance;

    @Nonnull
    private final Map<VillagerProfession, Set<Material>> cachedProfessionWantItemMap;

    private WolfFetchItemConfig() {
        ConfigFileWrapper wrapper;
        try {
            wrapper = new ConfigFileWrapper(FILE_NAME);
        } catch (IOException e) {
            LogUtil.exception(e, "Failed to load config file '%s'!", FILE_NAME);
            throw new RuntimeException(e);
        }

        // Add a new comment config field
        new ConfigField<>(wrapper, ConfigType.EMPTY, "professions", List.of(
                "Note that all item fields in this configuration file are required to be valid Bukkit materials in order to function correctly",
                "You can refer to https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html for more detailed information on Bukkit materials"
        ), "");

        // Load config fields
        Map<VillagerProfession, ConfigField<List<String>>> professionWantItemMap = new HashMap<>();
        professionWantItemMap.put(VillagerProfession.BUTCHER, new ConfigField<>(wrapper, ConfigType.STRING_LIST, "professions.butcher",
                DESCRIPTION_FORMAT.formatted("butcher"),
                this.materialListToStringList(List.of(Material.BEEF, Material.MUTTON, Material.CHICKEN, Material.PORKCHOP, Material.RABBIT))));

        professionWantItemMap.put(VillagerProfession.FARMER, new ConfigField<>(wrapper, ConfigType.STRING_LIST, "professions.farmer",
                DESCRIPTION_FORMAT.formatted("farmer"),
                this.materialListToStringList(List.of(Material.WHEAT, Material.WHEAT_SEEDS, Material.POTATO, Material.POISONOUS_POTATO, Material.CARROT,
                        Material.BEETROOT, Material.BEETROOT_SEEDS, Material.PUMPKIN, Material.PUMPKIN_SEEDS, Material.MELON_SLICE, Material.MELON,
                        Material.MELON_SEEDS, Material.SUGAR_CANE, Material.EGG))));

        professionWantItemMap.put(VillagerProfession.LEATHERWORKER, new ConfigField<>(wrapper, ConfigType.STRING_LIST, "professions.leather_worker",
                DESCRIPTION_FORMAT.formatted("leather_worker"),
                this.materialListToStringList(List.of(Material.LEATHER, Material.RABBIT_HIDE))));

        professionWantItemMap.put(VillagerProfession.SHEPHERD, new ConfigField<>(wrapper, ConfigType.STRING_LIST, "professions.shepherd",
                DESCRIPTION_FORMAT.formatted("shepherd"),
                this.materialListToStringList(List.of(Material.WHITE_WOOL, Material.ORANGE_WOOL, Material.MAGENTA_WOOL, Material.LIGHT_BLUE_WOOL,
                        Material.YELLOW_WOOL, Material.LIME_WOOL, Material.PINK_WOOL, Material.GRAY_WOOL, Material.LIGHT_GRAY_WOOL, Material.CYAN_WOOL,
                        Material.PURPLE_WOOL, Material.BLUE_WOOL, Material.BROWN_WOOL, Material.GREEN_WOOL, Material.RED_WOOL, Material.BLACK_WOOL))));


        // Validate & cache wanted items to memory
        this.cachedProfessionWantItemMap = new HashMap<>();
        for (Map.Entry<VillagerProfession, ConfigField<List<String>>> entry : professionWantItemMap.entrySet()) {
            Set<Material> materials = new HashSet<>();
            for (String materialString : entry.getValue().getValue()) {
                try {
                    materials.add(Material.valueOf(materialString));
                } catch (IllegalArgumentException e) {
                    LogUtil.warning("Invalid Bukkit material '%s' in config file '%s.yml', ignoring item", materialString, FILE_NAME);
                }
            }
            this.cachedProfessionWantItemMap.put(entry.getKey(), materials);
        }
    }

    /**
     * Check if a certain villager profession wants to pick up an item of this material
     *
     * @param profession     NMS villager profession
     * @param bukkitMaterial Bukkit itemstack material
     * @return true if this profession wants to pick the item up; false otherwise
     */
    public boolean wantsItem(@Nonnull VillagerProfession profession, @Nonnull Material bukkitMaterial) {
        // If the profession does not exist in the config file, default to false
        if (!this.cachedProfessionWantItemMap.containsKey(profession)) {
            return false;
        }

        // Check if the material is in the map
        return this.cachedProfessionWantItemMap.get(profession).contains(bukkitMaterial);
    }


    /**
     * Converts a list of Material objects to a list of their string representations
     *
     * @param materials the list of Material objects to convert
     * @return the list of string representations of the Material objects
     */
    @Nonnull
    private List<String> materialListToStringList(@Nonnull List<Material> materials) {
        return materials.stream().map(Enum::toString).toList();
    }

    public static synchronized WolfFetchItemConfig getInstance() {
        if (instance == null) {
            instance = new WolfFetchItemConfig();
        }
        return instance;
    }

}
