package dev.breeze.settlements.displays.cakes;

import dev.breeze.settlements.displays.TransformedBlockDisplay;
import dev.breeze.settlements.utils.TimeUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import java.util.List;

public class BlackCakeDisplayFactory implements CakeDisplayFactory {

    // Layer 1
    private static final TransformedBlockDisplay SPONGE_1 = TransformedBlockDisplay.builder()
            .blockData(Material.BLACK_TERRACOTTA.createBlockData())
            .transform(new Matrix4f(
                    0.5859f, 0.0000f, 0.0000f, 0.2070f,
                    0.0000f, 0.1563f, 0.0000f, 0.0000f,
                    0.0000f, 0.0000f, 0.5859f, 0.2070f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    private static final TransformedBlockDisplay CREAM_1 = TransformedBlockDisplay.builder()
            .blockData(Material.BLACK_CARPET.createBlockData())
            .transform(new Matrix4f(
                    0.6250f, 0.0000f, 0.0000f, 0.1875f,
                    0.0000f, 0.3125f, 0.0000f, 0.1563f,
                    0.0000f, 0.0000f, 0.6250f, 0.1875f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    // Layer 2
    private static final TransformedBlockDisplay SPONGE_2 = TransformedBlockDisplay.builder()
            .blockData(Material.BLACK_TERRACOTTA.createBlockData())
            .transform(new Matrix4f(
                    0.4395f, 0.0000f, 0.0000f, 0.2803f,
                    0.0000f, 0.1563f, 0.0000f, 0.1563f,
                    0.0000f, 0.0000f, 0.4395f, 0.2803f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    private static final TransformedBlockDisplay CREAM_2 = TransformedBlockDisplay.builder()
            .blockData(Material.BLACK_CARPET.createBlockData())
            .transform(new Matrix4f(
                    0.4688f, 0.0000f, 0.0000f, 0.2656f,
                    0.0000f, 0.3125f, 0.0000f, 0.3125f,
                    0.0000f, 0.0000f, 0.4688f, 0.2656f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    // Layer 3
    private static final TransformedBlockDisplay SPONGE_3 = TransformedBlockDisplay.builder()
            .blockData(Material.BLACK_TERRACOTTA.createBlockData())
            .transform(new Matrix4f(
                    0.2930f, 0.0000f, 0.0000f, 0.3535f,
                    0.0000f, 0.1563f, 0.0000f, 0.3125f,
                    0.0000f, 0.0000f, 0.2930f, 0.3535f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    private static final TransformedBlockDisplay CREAM_3 = TransformedBlockDisplay.builder()
            .blockData(Material.BLACK_CARPET.createBlockData())
            .transform(new Matrix4f(
                    0.3125f, 0.0000f, 0.0000f, 0.3438f,
                    0.0000f, 0.3125f, 0.0000f, 0.4688f,
                    0.0000f, 0.0000f, 0.3125f, 0.3438f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    // Flower on top
    private static final TransformedBlockDisplay FLOWER = TransformedBlockDisplay.builder()
            .blockData(Material.WITHER_ROSE.createBlockData())
            .transform(new Matrix4f(
                    0.1563f, 0.0000f, 0.0000f, 0.4219f,
                    0.0000f, 0.1563f, 0.0000f, 0.4883f,
                    0.0000f, 0.0000f, 0.1563f, 0.4219f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();


    @Nonnull
    @Override
    public CakeDisplay createCakeDisplay() {
        return new CakeDisplay(List.of(
                new CakeDisplay.CakeStep(List.of(SPONGE_1), TimeUtil.ticks(8), Sound.BLOCK_WOOL_PLACE, 1),
                new CakeDisplay.CakeStep(List.of(CREAM_1), TimeUtil.ticks(8), Sound.ENTITY_SLIME_SQUISH_SMALL, 1.2F),
                new CakeDisplay.CakeStep(List.of(SPONGE_2), TimeUtil.ticks(8), Sound.BLOCK_WOOL_PLACE, 1),
                new CakeDisplay.CakeStep(List.of(CREAM_2), TimeUtil.ticks(8), Sound.ENTITY_SLIME_SQUISH_SMALL, 1.2F),
                new CakeDisplay.CakeStep(List.of(SPONGE_3), TimeUtil.ticks(8), Sound.BLOCK_WOOL_PLACE, 1),
                new CakeDisplay.CakeStep(List.of(CREAM_3), TimeUtil.ticks(8), Sound.ENTITY_SLIME_SQUISH_SMALL, 1.2F),
                new CakeDisplay.CakeStep(List.of(FLOWER), TimeUtil.ticks(8), Sound.ENTITY_CHICKEN_EGG, 1.5F)
        ));
    }

}
