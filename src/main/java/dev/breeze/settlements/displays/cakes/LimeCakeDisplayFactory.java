package dev.breeze.settlements.displays.cakes;

import dev.breeze.settlements.displays.TransformedBlockDisplay;
import dev.breeze.settlements.displays.TransformedItemDisplay;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class LimeCakeDisplayFactory implements CakeDisplayFactory {

    // Base layers
    private static final TransformedBlockDisplay SPONGE_1 = TransformedBlockDisplay.builder()
            .blockData(Material.LIME_TERRACOTTA.createBlockData())
            .transform(new Matrix4f(
                    0.6055f, 0.0000f, 0.0000f, 0.1973f,
                    0.0000f, 0.0938f, 0.0000f, 0.0000f,
                    0.0000f, 0.0000f, 0.6055f, 0.1973f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    private static final TransformedBlockDisplay CREAM = TransformedBlockDisplay.builder()
            .blockData(Material.WHITE_CARPET.createBlockData())
            .transform(new Matrix4f(
                    0.6250f, 0.0000f, 0.0000f, 0.1875f,
                    0.0000f, 0.7500f, 0.0000f, 0.0938f,
                    0.0000f, 0.0000f, 0.6250f, 0.1875f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    private static final TransformedBlockDisplay SPONGE_2 = TransformedBlockDisplay.builder()
            .blockData(Material.LIME_CONCRETE.createBlockData())
            .transform(new Matrix4f(
                    0.6055f, 0.0000f, 0.0000f, 0.1973f,
                    0.0000f, 0.1172f, 0.0000f, 0.1172f,
                    0.0000f, 0.0000f, 0.6055f, 0.1973f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    // "Chocolates"
    private static final TransformedBlockDisplay BLACKSTONE = TransformedBlockDisplay.builder()
            .blockData(Material.CHISELED_POLISHED_BLACKSTONE.createBlockData())
            .transform(new Matrix4f(
                    0.2031f, 0.0000f, 0.0000f, 0.3984f,
                    0.0000f, 0.2438f, 0.0000f, 0.0000f,
                    0.0000f, 0.0000f, 0.2031f, 0.3984f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    private static final TransformedBlockDisplay EMERALD_BLOCK = TransformedBlockDisplay.builder()
            .blockData(Material.EMERALD_BLOCK.createBlockData())
            .transform(new Matrix4f(
                    0.1523f, 0.0000f, 0.0000f, 0.4238f,
                    0.0000f, 0.1828f, 0.0000f, 0.0938f,
                    0.0000f, 0.0000f, 0.1523f, 0.4238f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    // "Chocolate" emeralds
    private static final ArrayList<TransformedItemDisplay> SURROUNDING_EMERALDS = new ArrayList<>(List.of(
            TransformedItemDisplay.builder()
                    .itemStack(new ItemStackBuilder(Material.EMERALD).build())
                    .transform(new Matrix4f(
                            0.1875f, 0.0000f, 0.0000f, 0.5000f,
                            0.0000f, 0.0485f, -0.1811f, 0.2406f,
                            0.0000f, 0.1811f, 0.0485f, 0.6875f,
                            0.0000f, 0.0000f, 0.0000f, 1.0000f
                    ).transpose())
                    .build(),
            TransformedItemDisplay.builder()
                    .itemStack(new ItemStackBuilder(Material.EMERALD).build())
                    .transform(new Matrix4f(
                            0.1369f, 0.1192f, 0.0469f, 0.6406f,
                            0.0012f, 0.0675f, -0.1749f, 0.2406f,
                            -0.1281f, 0.1281f, 0.0485f, 0.6406f,
                            0.0000f, 0.0000f, 0.0000f, 1.0000f
                    ).transpose())
                    .build(),
            TransformedItemDisplay.builder()
                    .itemStack(new ItemStackBuilder(Material.EMERALD).build())
                    .transform(new Matrix4f(
                            -0.0000f, 0.1811f, 0.0485f, 0.6875f,
                            0.0000f, 0.0485f, -0.1811f, 0.2406f,
                            -0.1875f, -0.0000f, -0.0000f, 0.5000f,
                            0.0000f, 0.0000f, 0.0000f, 1.0000f
                    ).transpose())
                    .build(),
            TransformedItemDisplay.builder()
                    .itemStack(new ItemStackBuilder(Material.EMERALD).build())
                    .transform(new Matrix4f(
                            -0.1281f, 0.1281f, 0.0485f, 0.6406f,
                            0.0012f, 0.0675f, -0.1749f, 0.2406f,
                            -0.1369f, -0.1192f, -0.0469f, 0.3594f,
                            0.0000f, 0.0000f, 0.0000f, 1.0000f
                    ).transpose())
                    .build(),
            TransformedItemDisplay.builder()
                    .itemStack(new ItemStackBuilder(Material.EMERALD).build())
                    .transform(new Matrix4f(
                            -0.1875f, -0.0000f, 0.0000f, 0.5000f,
                            -0.0000f, 0.0485f, -0.1811f, 0.2406f,
                            0.0000f, -0.1811f, -0.0485f, 0.3125f,
                            0.0000f, 0.0000f, 0.0000f, 1.0000f
                    ).transpose())
                    .build(),
            TransformedItemDisplay.builder()
                    .itemStack(new ItemStackBuilder(Material.EMERALD).build())
                    .transform(new Matrix4f(
                            -0.1281f, -0.1281f, -0.0485f, 0.3594f,
                            -0.0012f, 0.0675f, -0.1749f, 0.2406f,
                            0.1369f, -0.1192f, -0.0469f, 0.3594f,
                            0.0000f, 0.0000f, 0.0000f, 1.0000f
                    ).transpose())
                    .build(),
            TransformedItemDisplay.builder()
                    .itemStack(new ItemStackBuilder(Material.EMERALD).build())
                    .transform(new Matrix4f(
                            -0.0000f, -0.1811f, -0.0485f, 0.3125f,
                            -0.0000f, 0.0485f, -0.1811f, 0.2406f,
                            0.1875f, -0.0000f, -0.0000f, 0.5000f,
                            0.0000f, 0.0000f, 0.0000f, 1.0000f
                    ).transpose())
                    .build(),
            TransformedItemDisplay.builder()
                    .itemStack(new ItemStackBuilder(Material.EMERALD).build())
                    .transform(new Matrix4f(
                            0.1281f, -0.1281f, -0.0485f, 0.3594f,
                            0.0012f, 0.0675f, -0.1749f, 0.2406f,
                            0.1369f, 0.1192f, 0.0469f, 0.6406f,
                            0.0000f, 0.0000f, 0.0000f, 1.0000f
                    ).transpose())
                    .build()
    ));

    private static final TransformedItemDisplay FINAL_EMERALD = TransformedItemDisplay.builder()
            .itemStack(new ItemStackBuilder(Material.EMERALD).build())
            .transform(new Matrix4f(
                    -0.1875f, -0.0000f, 0.0000f, 0.5000f,
                    -0.0000f, 0.1875f, -0.0000f, 0.3373f,
                    0.0000f, -0.0000f, -0.1875f, 0.5000f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();


    @Nonnull
    @Override
    public CakeDisplay createCakeDisplay() {
        List<CakeDisplay.CakeStep> cakeSteps = new ArrayList<>(List.of(
                new CakeDisplay.CakeStep(List.of(SPONGE_1), TimeUtil.ticks(10), Sound.BLOCK_WOOL_PLACE, 1),
                new CakeDisplay.CakeStep(List.of(CREAM), TimeUtil.ticks(10), Sound.ENTITY_SLIME_SQUISH_SMALL, 1.2F),
                new CakeDisplay.CakeStep(List.of(SPONGE_2), TimeUtil.ticks(10), Sound.BLOCK_WOOL_PLACE, 1),
                new CakeDisplay.CakeStep(List.of(BLACKSTONE), TimeUtil.ticks(5), Sound.BLOCK_METAL_PLACE, 1.5F),
                new CakeDisplay.CakeStep(List.of(EMERALD_BLOCK), TimeUtil.ticks(5), Sound.ENTITY_VILLAGER_CELEBRATE, 1.2F)
        ));

        // Add surrounding emeralds
        for (TransformedItemDisplay display : SURROUNDING_EMERALDS) {
            cakeSteps.add(new CakeDisplay.CakeStep(List.of(display), TimeUtil.ticks(3), Sound.ENTITY_VILLAGER_CELEBRATE, 1.2F));
        }

        // Add final emerald
        cakeSteps.add(new CakeDisplay.CakeStep(List.of(FINAL_EMERALD), TimeUtil.ticks(6), Sound.ENTITY_VILLAGER_CELEBRATE, 1.2F));
        return new CakeDisplay(cakeSteps);
    }

}
