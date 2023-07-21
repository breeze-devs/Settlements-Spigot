package dev.breeze.settlements.displays.cakes;

import dev.breeze.settlements.displays.TransformedBlockDisplay;
import dev.breeze.settlements.displays.TransformedDisplay;
import dev.breeze.settlements.displays.TransformedItemDisplay;
import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class OrangeCakeDisplayFactory implements CakeDisplayFactory {

    // Layer 1
    private static final TransformedBlockDisplay SPONGE_1 = TransformedBlockDisplay.builder()
            .blockData(Material.BROWN_TERRACOTTA.createBlockData())
            .transform(new Matrix4f(
                    0.6152f, 0.0000f, 0.0000f, 0.1924f,
                    0.0000f, 0.0938f, 0.0000f, 0.0000f,
                    0.0000f, 0.0000f, 0.6152f, 0.1924f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    private static final TransformedBlockDisplay CREAM_1 = TransformedBlockDisplay.builder()
            .blockData(Material.ORANGE_CARPET.createBlockData())
            .transform(new Matrix4f(
                    0.6250f, 0.0000f, 0.0000f, 0.1875f,
                    0.0000f, 0.2500f, 0.0000f, 0.0938f,
                    0.0000f, 0.0000f, 0.6250f, 0.1875f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    // Layer 2
    private static final TransformedBlockDisplay SPONGE_2 = TransformedBlockDisplay.builder()
            .blockData(Material.ORANGE_TERRACOTTA.createBlockData())
            .transform(new Matrix4f(
                    0.6152f, 0.0000f, 0.0000f, 0.1924f,
                    0.0000f, 0.0938f, 0.0000f, 0.0938f,
                    0.0000f, 0.0000f, 0.6152f, 0.1924f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    private static final TransformedBlockDisplay CREAM_2 = TransformedBlockDisplay.builder()
            .blockData(Material.ORANGE_CARPET.createBlockData())
            .transform(new Matrix4f(
                    0.6250f, 0.0000f, 0.0000f, 0.1875f,
                    0.0000f, 0.2500f, 0.0000f, 0.1875f,
                    0.0000f, 0.0000f, 0.6250f, 0.1875f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    // Layer 3
    private static final TransformedBlockDisplay SPONGE_3 = TransformedBlockDisplay.builder()
            .blockData(Material.ORANGE_CONCRETE.createBlockData())
            .transform(new Matrix4f(
                    0.6152f, 0.0000f, 0.0000f, 0.1924f,
                    0.0000f, 0.0938f, 0.0000f, 0.1875f,
                    0.0000f, 0.0000f, 0.6152f, 0.1924f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    // Carrot cubes
    private static final ArrayList<TransformedBlockDisplay> CARROT_CUBES = new ArrayList<>(List.of(
            TransformedBlockDisplay.builder()
                    .blockData(Material.YELLOW_CONCRETE.createBlockData())
                    .transform(new Matrix4f(
                            0.0391f, 0.0000f, 0.0000f, 0.5391f,
                            0.0000f, 0.0156f, 0.0000f, 0.2813f,
                            0.0000f, 0.0000f, 0.0391f, 0.2656f,
                            0.0000f, 0.0000f, 0.0000f, 1.0000f
                    ).transpose())
                    .build(),
            TransformedBlockDisplay.builder()
                    .blockData(Material.YELLOW_CONCRETE.createBlockData())
                    .transform(new Matrix4f(
                            0.0391f, 0.0000f, 0.0000f, 0.4219f,
                            0.0000f, 0.0156f, 0.0000f, 0.2813f,
                            0.0000f, 0.0000f, 0.0391f, 0.2656f,
                            0.0000f, 0.0000f, 0.0000f, 1.0000f
                    ).transpose())
                    .build(),
            TransformedBlockDisplay.builder()
                    .blockData(Material.YELLOW_CONCRETE.createBlockData())
                    .transform(new Matrix4f(
                            0.0391f, 0.0000f, 0.0000f, 0.3047f,
                            0.0000f, 0.0156f, 0.0000f, 0.2813f,
                            0.0000f, 0.0000f, 0.0391f, 0.3047f,
                            0.0000f, 0.0000f, 0.0000f, 1.0000f
                    ).transpose())
                    .build(),
            TransformedBlockDisplay.builder()
                    .blockData(Material.YELLOW_CONCRETE.createBlockData())
                    .transform(new Matrix4f(
                            0.0391f, 0.0000f, 0.0000f, 0.2656f,
                            0.0000f, 0.0156f, 0.0000f, 0.2813f,
                            0.0000f, 0.0000f, 0.0391f, 0.4219f,
                            0.0000f, 0.0000f, 0.0000f, 1.0000f
                    ).transpose())
                    .build(),
            TransformedBlockDisplay.builder()
                    .blockData(Material.YELLOW_CONCRETE.createBlockData())
                    .transform(new Matrix4f(
                            0.0391f, 0.0000f, 0.0000f, 0.2656f,
                            0.0000f, 0.0156f, 0.0000f, 0.2813f,
                            0.0000f, 0.0000f, 0.0391f, 0.5391f,
                            0.0000f, 0.0000f, 0.0000f, 1.0000f
                    ).transpose())
                    .build(),
            TransformedBlockDisplay.builder()
                    .blockData(Material.YELLOW_CONCRETE.createBlockData())
                    .transform(new Matrix4f(
                            0.0391f, 0.0000f, 0.0000f, 0.3047f,
                            0.0000f, 0.0156f, 0.0000f, 0.2813f,
                            0.0000f, 0.0000f, 0.0391f, 0.6563f,
                            0.0000f, 0.0000f, 0.0000f, 1.0000f
                    ).transpose())
                    .build(),
            TransformedBlockDisplay.builder()
                    .blockData(Material.YELLOW_CONCRETE.createBlockData())
                    .transform(new Matrix4f(
                            0.0391f, 0.0000f, 0.0000f, 0.4219f,
                            0.0000f, 0.0156f, 0.0000f, 0.2813f,
                            0.0000f, 0.0000f, 0.0391f, 0.6953f,
                            0.0000f, 0.0000f, 0.0000f, 1.0000f
                    ).transpose())
                    .build(),
            TransformedBlockDisplay.builder()
                    .blockData(Material.YELLOW_CONCRETE.createBlockData())
                    .transform(new Matrix4f(
                            0.0391f, 0.0000f, 0.0000f, 0.5391f,
                            0.0000f, 0.0156f, 0.0000f, 0.2813f,
                            0.0000f, 0.0000f, 0.0391f, 0.6953f,
                            0.0000f, 0.0000f, 0.0000f, 1.0000f
                    ).transpose())
                    .build(),
            TransformedBlockDisplay.builder()
                    .blockData(Material.YELLOW_CONCRETE.createBlockData())
                    .transform(new Matrix4f(
                            0.0391f, 0.0000f, 0.0000f, 0.6563f,
                            0.0000f, 0.0156f, 0.0000f, 0.2813f,
                            0.0000f, 0.0000f, 0.0391f, 0.6563f,
                            0.0000f, 0.0000f, 0.0000f, 1.0000f
                    ).transpose())
                    .build(),
            TransformedBlockDisplay.builder()
                    .blockData(Material.YELLOW_CONCRETE.createBlockData())
                    .transform(new Matrix4f(
                            0.0391f, 0.0000f, 0.0000f, 0.6953f,
                            0.0000f, 0.0156f, 0.0000f, 0.2813f,
                            0.0000f, 0.0000f, 0.0391f, 0.5391f,
                            0.0000f, 0.0000f, 0.0000f, 1.0000f
                    ).transpose())
                    .build(),
            TransformedBlockDisplay.builder()
                    .blockData(Material.YELLOW_CONCRETE.createBlockData())
                    .transform(new Matrix4f(
                            0.0391f, 0.0000f, 0.0000f, 0.6953f,
                            0.0000f, 0.0156f, 0.0000f, 0.2813f,
                            0.0000f, 0.0000f, 0.0391f, 0.4219f,
                            0.0000f, 0.0000f, 0.0000f, 1.0000f
                    ).transpose())
                    .build(),
            TransformedBlockDisplay.builder()
                    .blockData(Material.YELLOW_CONCRETE.createBlockData())
                    .transform(new Matrix4f(
                            0.0391f, 0.0000f, 0.0000f, 0.6563f,
                            0.0000f, 0.0156f, 0.0000f, 0.2813f,
                            0.0000f, 0.0000f, 0.0391f, 0.3047f,
                            0.0000f, 0.0000f, 0.0000f, 1.0000f
                    ).transpose())
                    .build()
    ));

    // Golden carrot
    private static final TransformedItemDisplay GOLDEN_CARROT = TransformedItemDisplay.builder()
            .itemStack(new ItemStackBuilder(Material.GOLDEN_CARROT).build())
            .transform(new Matrix4f(
                    0.2031f, 0.0000f, 0.0000f, 0.5000f,
                    0.0000f, -0.0000f, -0.3750f, 0.2875f,
                    0.0000f, 0.2031f, -0.0000f, 0.5000f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();


    @Nonnull
    @Override
    public CakeDisplay createCakeDisplay() {
        List<CakeDisplay.CakeStep> cakeSteps = new ArrayList<>(List.of(
                new CakeDisplay.CakeStep(List.of(SPONGE_1), TimeUtil.ticks(6), Sound.BLOCK_WOOL_PLACE, 1),
                new CakeDisplay.CakeStep(List.of(CREAM_1), TimeUtil.ticks(6), Sound.ENTITY_SLIME_SQUISH_SMALL, 1.2F),
                new CakeDisplay.CakeStep(List.of(SPONGE_2), TimeUtil.ticks(6), Sound.BLOCK_WOOL_PLACE, 1),
                new CakeDisplay.CakeStep(List.of(CREAM_2), TimeUtil.ticks(6), Sound.ENTITY_SLIME_SQUISH_SMALL, 1.2F),
                new CakeDisplay.CakeStep(List.of(SPONGE_3), TimeUtil.ticks(6), Sound.BLOCK_WOOL_PLACE, 1)
        ));

        // Add carrot cubes
        for (TransformedDisplay display : RandomUtil.shuffle(CARROT_CUBES)) {
            cakeSteps.add(new CakeDisplay.CakeStep(List.of(display), TimeUtil.ticks(3), Sound.ENTITY_CHICKEN_EGG, 1.5F));
        }

        // Add golden carrot
        cakeSteps.add(new CakeDisplay.CakeStep(List.of(GOLDEN_CARROT), TimeUtil.ticks(6), Sound.BLOCK_MOSS_BREAK, 1.2F));
        return new CakeDisplay(cakeSteps);
    }

}
