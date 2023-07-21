package dev.breeze.settlements.displays.cakes;

import dev.breeze.settlements.displays.TransformedBlockDisplay;
import dev.breeze.settlements.displays.TransformedDisplay;
import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.utils.TimeUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class WhiteCakeDisplay {

    /*
     * Cake step 1
     */
    // Sponge layer 1
    private static final TransformedBlockDisplay SPONGE_1 = TransformedBlockDisplay.builder()
            .blockData(Material.BROWN_TERRACOTTA.createBlockData())
            .transform(new Matrix4f(
                    0.6250f, 0.0000f, 0.0000f, 0.1875f,
                    0.0000f, 0.1250f, 0.0000f, 0.0000f,
                    0.0000f, 0.0000f, 0.6250f, 0.1875f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    // Cream layer 1
    private static final TransformedBlockDisplay CREAM_1 = TransformedBlockDisplay.builder()
            .blockData(Material.PINK_CARPET.createBlockData())
            .transform(new Matrix4f(
                    0.6445f, 0.0000f, 0.0000f, 0.1777f,
                    0.0000f, 0.5000f, 0.0000f, 0.1250f,
                    0.0000f, 0.0000f, 0.6445f, 0.1777f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    /*
     * Cake step 2
     */
    // Sponge layer 2
    private static final TransformedBlockDisplay SPONGE_2 = TransformedBlockDisplay.builder()
            .blockData(Material.BROWN_CONCRETE.createBlockData())
            .transform(new Matrix4f(
                    0.6250f, 0.0000f, 0.0000f, 0.1875f,
                    0.0000f, 0.0938f, 0.0000f, 0.1563f,
                    0.0000f, 0.0000f, 0.6250f, 0.1875f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    // Cream layer 2
    private static final TransformedBlockDisplay CREAM_2 = TransformedBlockDisplay.builder()
            .blockData(Material.WHITE_CARPET.createBlockData())
            .transform(new Matrix4f(
                    0.6445f, 0.0000f, 0.0000f, 0.1777f,
                    0.0000f, 0.5000f, 0.0000f, 0.2500f,
                    0.0000f, 0.0000f, 0.6445f, 0.1777f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    /*
     * Cake step 3
     */
    // Cream layer 3
    private static final TransformedBlockDisplay CREAM_3 = TransformedBlockDisplay.builder()
            .blockData(Material.SNOW.createBlockData("[layers=1]"))
            .transform(new Matrix4f(
                    0.6250f, 0.0000f, 0.0000f, 0.1875f,
                    0.0000f, 0.2500f, 0.0000f, 0.2813f,
                    0.0000f, 0.0000f, 0.6250f, 0.1875f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    // Cherry center
    private static final TransformedBlockDisplay CHERRY_CENTER = TransformedBlockDisplay.builder()
            .blockData(Material.RED_CONCRETE.createBlockData())
            .transform(new Matrix4f(
                    0.1172f, 0.0000f, 0.0000f, 0.4414f,
                    0.0000f, 0.0313f, 0.0000f, 0.3125f,
                    0.0000f, 0.0000f, 0.1172f, 0.4414f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    /*
     * Toppings
     */
    private static final TransformedBlockDisplay TOPPING_1 = TransformedBlockDisplay.builder()
            .blockData(Material.YELLOW_CONCRETE.createBlockData())
            .transform(new Matrix4f(
                    0.0391f, 0.0000f, 0.0000f, 0.2266f,
                    0.0000f, 0.0313f, 0.0000f, 0.3125f,
                    0.0000f, 0.0000f, 0.0391f, 0.3438f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    private static final TransformedBlockDisplay TOPPING_2 = TransformedBlockDisplay.builder()
            .blockData(Material.RED_CONCRETE.createBlockData())
            .transform(new Matrix4f(
                    0.0391f, 0.0000f, 0.0000f, 0.3438f,
                    0.0000f, 0.0313f, 0.0000f, 0.3125f,
                    0.0000f, 0.0000f, 0.0391f, 0.3438f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    private static final TransformedBlockDisplay TOPPING_3 = TransformedBlockDisplay.builder()
            .blockData(Material.ORANGE_CONCRETE.createBlockData())
            .transform(new Matrix4f(
                    0.0391f, 0.0000f, 0.0000f, 0.5000f,
                    0.0000f, 0.0313f, 0.0000f, 0.3125f,
                    0.0000f, 0.0000f, 0.0391f, 0.2266f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    private static final TransformedBlockDisplay TOPPING_4 = TransformedBlockDisplay.builder()
            .blockData(Material.RED_CONCRETE.createBlockData())
            .transform(new Matrix4f(
                    0.0391f, 0.0000f, 0.0000f, 0.7344f,
                    0.0000f, 0.0313f, 0.0000f, 0.3125f,
                    0.0000f, 0.0000f, 0.0391f, 0.3047f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    private static final TransformedBlockDisplay TOPPING_5 = TransformedBlockDisplay.builder()
            .blockData(Material.YELLOW_CONCRETE.createBlockData())
            .transform(new Matrix4f(
                    0.0781f, 0.0000f, 0.0000f, 0.3047f,
                    0.0000f, 0.0313f, 0.0000f, 0.3125f,
                    0.0000f, 0.0000f, 0.0781f, 0.6172f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    private static final TransformedBlockDisplay TOPPING_6 = TransformedBlockDisplay.builder()
            .blockData(Material.RED_CONCRETE.createBlockData())
            .transform(new Matrix4f(
                    0.0781f, 0.0000f, 0.0000f, 0.6953f,
                    0.0000f, 0.0313f, 0.0000f, 0.3125f,
                    0.0000f, 0.0000f, 0.0781f, 0.6172f, 0.0000f,
                    0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    private static final TransformedBlockDisplay TOPPING_7 = TransformedBlockDisplay.builder()
            .blockData(Material.ORANGE_CONCRETE.createBlockData())
            .transform(new Matrix4f(
                    0.0391f, 0.0000f, 0.0000f, 0.4609f,
                    0.0000f, 0.0313f, 0.0000f, 0.3125f,
                    0.0000f, 0.0000f, 0.0391f, 0.7344f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f
            ).transpose())
            .build();

    public static CakeDisplay getCakeDisplay() {
        List<CakeDisplay.CakeStep> cakeSteps = new ArrayList<>(List.of(
                new CakeDisplay.CakeStep(List.of(SPONGE_1), TimeUtil.ticks(8), Sound.BLOCK_WOOL_PLACE, 1),
                new CakeDisplay.CakeStep(List.of(CREAM_1), TimeUtil.ticks(8), Sound.ENTITY_SLIME_SQUISH_SMALL, 1.2F),
                new CakeDisplay.CakeStep(List.of(SPONGE_2), TimeUtil.ticks(8), Sound.BLOCK_WOOL_PLACE, 1),
                new CakeDisplay.CakeStep(List.of(CREAM_2, CREAM_3), TimeUtil.ticks(8), Sound.ENTITY_SLIME_SQUISH_SMALL, 1.2F),
                new CakeDisplay.CakeStep(List.of(CHERRY_CENTER), TimeUtil.ticks(5), Sound.ENTITY_CHICKEN_EGG, 1.5F)
        ));

        for (TransformedDisplay display : RandomUtil.shuffle(new ArrayList<>(List.of(TOPPING_1, TOPPING_2, TOPPING_3, TOPPING_4, TOPPING_5, TOPPING_6, TOPPING_7)))) {
            cakeSteps.add(new CakeDisplay.CakeStep(List.of(display), TimeUtil.ticks(3), Sound.ENTITY_CHICKEN_EGG, 1.5F));
        }

        return new CakeDisplay(cakeSteps);
    }

}
