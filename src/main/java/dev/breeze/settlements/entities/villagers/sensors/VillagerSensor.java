package dev.breeze.settlements.entities.villagers.sensors;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.ai.sensing.SensorType;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public class VillagerSensor<T extends BaseVillagerSensor> {

    /**
     * A formatter for generating the identifier string, used as the Minecraft registry key
     */
    private static final String IDENTIFIER_FORMATTER = "settlements_villager_%s_sensor";

    @Getter
    @Nonnull
    private final String identifier;

    @Getter
    private final Supplier<T> sensorSupplier;

    @Getter
    @Setter
    private SensorType<T> sensorType;

    @Builder
    protected VillagerSensor(@Nonnull String identifier, @Nonnull Supplier<T> sensorSupplier) {
        this.identifier = IDENTIFIER_FORMATTER.formatted(identifier);
        this.sensorSupplier = sensorSupplier;
    }

    public ItemStack getGuiItem() {
        return this.sensorSupplier.get().getGuiItemBuilder().build();
    }

}
