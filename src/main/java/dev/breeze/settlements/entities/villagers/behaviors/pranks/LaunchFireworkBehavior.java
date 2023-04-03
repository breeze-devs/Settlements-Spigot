package dev.breeze.settlements.entities.villagers.behaviors.pranks;

import dev.breeze.settlements.utils.RandomUtil;
import dev.breeze.settlements.utils.SoundUtil;
import dev.breeze.settlements.utils.TimeUtil;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import dev.breeze.settlements.utils.particle.ParticleUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;

public final class LaunchFireworkBehavior extends PrankEntityBehavior {

    private static final ItemStack FIREWORK = CraftItemStack.asNMSCopy(new ItemStackBuilder(Material.FIREWORK_ROCKET).build());

    private static final Color[] FIREWORK_COLORS = new Color[]{Color.WHITE, Color.SILVER, Color.GRAY, Color.BLACK, Color.RED, Color.MAROON, Color.YELLOW,
            Color.OLIVE, Color.LIME, Color.GREEN, Color.AQUA, Color.TEAL, Color.BLUE, Color.NAVY, Color.FUCHSIA, Color.PURPLE, Color.ORANGE};

    public LaunchFireworkBehavior() {
        // Preconditions to this behavior
        super(TimeUtil.minutes(5), Math.pow(6, 2), TimeUtil.seconds(2));
    }

    @Override
    protected void performAnnoy(ServerLevel level, Villager villager, long gameTime, World world, Location selfLoc, Location targetLoc) {
        // Launch firework
        Firework firework = (Firework) world.spawnEntity(selfLoc, EntityType.FIREWORK);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.setPower(1);
        meta.addEffect(this.generateRandomEffect());
        firework.setFireworkMeta(meta);

        SoundUtil.playSoundPublic(selfLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1F);

        // Display annoyed effect
        SoundUtil.playSoundPublic(targetLoc, Sound.ENTITY_VILLAGER_NO, 0.2F, RandomUtil.RANDOM.nextFloat() / 3 + 0.2F);
        ParticleUtil.globalParticle(targetLoc, Particle.VILLAGER_ANGRY, RandomUtil.RANDOM.nextInt(2) + 1, 0.1, 0, 0.1, 0.1);
    }

    @Override
    protected ItemStack getItemToHold() {
        return FIREWORK;
    }

    private FireworkEffect generateRandomEffect() {
        FireworkEffect.Builder builder = FireworkEffect.builder();
        builder.withColor(RandomUtil.choice(FIREWORK_COLORS));
        builder.with(RandomUtil.choice(FireworkEffect.Type.values()));

        if (RandomUtil.RANDOM.nextDouble() < 0.3) {
            builder.withFlicker();
        }

        if (RandomUtil.RANDOM.nextDouble() < 0.3) {
            builder.withTrail();
        }

        return builder.build();
    }

}
