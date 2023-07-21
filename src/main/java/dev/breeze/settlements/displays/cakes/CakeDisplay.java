package dev.breeze.settlements.displays.cakes;

import dev.breeze.settlements.displays.MultiEntityDisplay;
import dev.breeze.settlements.displays.TransformedDisplay;
import dev.breeze.settlements.utils.LogUtil;
import dev.breeze.settlements.utils.SoundUtil;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Sound;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CakeDisplay extends MultiEntityDisplay {

    @Nonnull
    private final List<CakeStep> steps;

    private int currentStep;

    public CakeDisplay(@Nonnull List<CakeStep> steps) {
        super(steps.stream().flatMap(CakeStep::displayStream).collect(Collectors.toList()));
        this.steps = steps;
        this.currentStep = 0;
    }

    /**
     * Spawns the display entities for the next step of the cake
     */
    public int spawnNextStep(@Nonnull Location location) {
        // Safety check
        if (this.currentStep >= this.steps.size()) {
            LogUtil.severe("Tried to spawn next step of the cake display when there are no more steps remaining!");
            return -1;
        }

        CakeStep step = this.steps.get(this.currentStep);
        for (TransformedDisplay entity : step.displays) {
            entity.spawn(location);
        }

        SoundUtil.playSoundPublic(location, step.sound, step.volume, step.pitch);
        this.currentStep++;

        return step.delayTicks;
    }

    public boolean hasNextStep() {
        return this.currentStep < this.steps.size();
    }

    @Getter
    public static final class CakeStep {

        @Nonnull
        private final List<TransformedDisplay> displays;
        private final int delayTicks;

        private final Sound sound;
        private final float volume;
        private final float pitch;

        public CakeStep(@Nonnull List<TransformedDisplay> displays, int delayTicks, Sound sound, float volume, float pitch) {
            this.displays = displays.stream().map(display -> display.cloneWithoutEntity(true)).collect(Collectors.toList());
            this.delayTicks = delayTicks;

            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }

        public CakeStep(@Nonnull List<TransformedDisplay> displays, int delayTicks, Sound sound, float baseVolume) {
            this(displays, delayTicks, sound, 0.2F, SoundUtil.randomPitch(baseVolume, 0.3F));
        }

        @Nonnull
        public Stream<TransformedDisplay> displayStream() {
            return this.displays.stream();
        }

    }

}
