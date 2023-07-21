package dev.breeze.settlements.displays.cakes;

import dev.breeze.settlements.displays.TransformedBlockDisplay;
import dev.breeze.settlements.displays.TransformedDisplay;
import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.utils.TimeUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class GreenCakeDisplayFactory implements CakeDisplayFactory {

    // Layer 1
    private static final TransformedBlockDisplay SPONGE_1 = TransformedBlockDisplay.builder()
            .blockData(Material.BROWN_TERRACOTTA.createBlockData())
            .transform(new Matrix4f(
                    0.6250f, 0.0000f, 0.0000f, 0.1875f,
                    0.0000f, 0.1250f, 0.0000f, 0.0000f,
                    0.0000f, 0.0000f, 0.6250f, 0.1875f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    private static final TransformedBlockDisplay CREAM_1 = TransformedBlockDisplay.builder()
            .blockData(Material.BROWN_CARPET.createBlockData())
            .transform(new Matrix4f(
                    0.6445f, 0.0000f, 0.0000f, 0.1777f,
                    0.0000f, 0.5000f, 0.0000f, 0.1250f,
                    0.0000f, 0.0000f, 0.6445f, 0.1777f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    // Layer 2
    private static final TransformedBlockDisplay SPONGE_2 = TransformedBlockDisplay.builder()
            .blockData(Material.GREEN_CONCRETE.createBlockData())
            .transform(new Matrix4f(
                    0.6250f, 0.0000f, 0.0000f, 0.1875f,
                    0.0000f, 0.0938f, 0.0000f, 0.1563f,
                    0.0000f, 0.0000f, 0.6250f, 0.1875f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    private static final TransformedBlockDisplay CREAM_2 = TransformedBlockDisplay.builder()
            .blockData(Material.GREEN_CARPET.createBlockData())
            .transform(new Matrix4f(
                    0.6445f, 0.0000f, 0.0000f, 0.1777f,
                    0.0000f, 0.5000f, 0.0000f, 0.2500f,
                    0.0000f, 0.0000f, 0.6445f, 0.1777f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    // Top
    private static final TransformedBlockDisplay CREAM_3 = TransformedBlockDisplay.builder()
            .blockData(Material.MOSS_CARPET.createBlockData())
            .transform(new Matrix4f(
                    0.6250f, 0.0000f, 0.0000f, 0.1875f,
                    0.0000f, 0.5000f, 0.0000f, 0.2813f,
                    0.0000f, 0.0000f, 0.6250f, 0.1875f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    // Creeper
    private static final ArrayList<TransformedBlockDisplay> CREEPER_BLOCKS = new ArrayList<>(List.of(
            TransformedBlockDisplay.builder()
                    .blockData(Material.BLACK_CONCRETE.createBlockData())
                    .transform(new Matrix4f(
                            0.1563f, 0.0000f, 0.0000f, 0.2656f,
                            0.0000f, 0.0313f, 0.0000f, 0.3125f,
                            0.0000f, 0.0000f, 0.1563f, 0.2656f,
                            0.0000f, 0.0000f, 0.0000f, 1.0000f
                    ).transpose())
                    .build(),
            TransformedBlockDisplay.builder()
                    .blockData(Material.BLACK_CONCRETE.createBlockData())
                    .transform(new Matrix4f(
                            0.1563f, 0.0000f, 0.0000f, 0.5781f,
                            0.0000f, 0.0313f, 0.0000f, 0.3125f,
                            0.0000f, 0.0000f, 0.1563f, 0.2656f,
                            0.0000f, 0.0000f, 0.0000f, 1.0000f
                    ).transpose())
                    .build(),
            TransformedBlockDisplay.builder()
                    .blockData(Material.BLACK_CONCRETE.createBlockData())
                    .transform(new Matrix4f(
                            0.1563f, 0.0000f, 0.0000f, 0.4219f,
                            0.0000f, 0.0313f, 0.0000f, 0.3125f,
                            0.0000f, 0.0000f, 0.2344f, 0.4219f,
                            0.0000f, 0.0000f, 0.0000f, 1.0000f
                    ).transpose())
                    .build(),
            TransformedBlockDisplay.builder()
                    .blockData(Material.BLACK_CONCRETE.createBlockData())
                    .transform(new Matrix4f(
                            0.0781f, 0.0000f, 0.0000f, 0.3438f,
                            0.0000f, 0.0313f, 0.0000f, 0.3125f,
                            0.0000f, 0.0000f, 0.2344f, 0.5000f,
                            0.0000f, 0.0000f, 0.0000f, 1.0000f
                    ).transpose())
                    .build(),
            TransformedBlockDisplay.builder()
                    .blockData(Material.BLACK_CONCRETE.createBlockData())
                    .transform(new Matrix4f(
                            0.0781f, 0.0000f, 0.0000f, 0.5781f,
                            0.0000f, 0.0313f, 0.0000f, 0.3125f,
                            0.0000f, 0.0000f, 0.2344f, 0.5000f,
                            0.0000f, 0.0000f, 0.0000f, 1.0000f
                    ).transpose())
                    .build()
    ));

    @Nonnull
    @Override
    public CakeDisplay createCakeDisplay() {
        List<CakeDisplay.CakeStep> cakeSteps = new ArrayList<>(List.of(
                new CakeDisplay.CakeStep(List.of(SPONGE_1), TimeUtil.ticks(8), Sound.BLOCK_WOOL_PLACE, 1),
                new CakeDisplay.CakeStep(List.of(CREAM_1), TimeUtil.ticks(8), Sound.ENTITY_SLIME_SQUISH_SMALL, 1.2F),
                new CakeDisplay.CakeStep(List.of(SPONGE_2), TimeUtil.ticks(8), Sound.BLOCK_WOOL_PLACE, 1),
                new CakeDisplay.CakeStep(List.of(CREAM_2), TimeUtil.ticks(8), Sound.ENTITY_SLIME_SQUISH_SMALL, 1.2F),
                new CakeDisplay.CakeStep(List.of(CREAM_3), TimeUtil.ticks(8), Sound.BLOCK_MOSS_PLACE, 1.2F)
        ));

        // Add creeper blocks
        for (TransformedDisplay display : RandomUtil.shuffle(CREEPER_BLOCKS)) {
            cakeSteps.add(new CakeDisplay.CakeStep(List.of(display), TimeUtil.ticks(5), Sound.BLOCK_METAL_PLACE, 1.3F));
        }

        return new CakeDisplay(cakeSteps);
    }

}
