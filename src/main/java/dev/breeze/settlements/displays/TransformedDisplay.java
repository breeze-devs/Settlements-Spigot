package dev.breeze.settlements.displays;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Getter
public abstract class TransformedDisplay {

    @Nonnull
    protected final Matrix4f transform;
    @Nonnull
    protected final DisplayType displayType;

    @Nullable
    protected Display displayEntity;

    protected final boolean temporary;
    private boolean spawned;

    public TransformedDisplay(@Nonnull Matrix4f transform, @Nonnull DisplayType displayType, boolean temporary) {
        this.transform = transform;
        this.displayType = displayType;
        this.temporary = temporary;

        this.spawned = false;
    }

    @Nonnull
    public Display spawn(@Nonnull Location location) {
        if (this.spawned) {
            throw new IllegalStateException("Tried to spawn a display entity that has already been spawned!");
        }

        this.displayEntity = this.createEntity(location);
        this.displayEntity.setTransformationMatrix(this.transform);

        this.spawned = true;

        // Add to removal list if temporary
        if (this.temporary) {
            DisplayModuleController.TEMPORARY_DISPLAYS.add(this);
        }

        return this.displayEntity;
    }

    @Nonnull
    public abstract Display createEntity(@Nonnull Location location);

    public void remove() {
        if (this.displayEntity != null && !this.displayEntity.isDead()) {
            this.displayEntity.remove();
        }

        this.spawned = false;
    }

    @Nonnull
    public abstract TransformedDisplay cloneWithoutEntity(boolean temporary);

    public enum DisplayType {
        BLOCK,
        ITEM
    }

}
