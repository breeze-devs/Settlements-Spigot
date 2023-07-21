package dev.breeze.settlements.displays;

import org.bukkit.Location;

import javax.annotation.Nonnull;
import java.util.List;

public class MultiEntityDisplay {

    @Nonnull
    protected final List<TransformedDisplay> displayEntities;

    public MultiEntityDisplay(@Nonnull List<TransformedDisplay> displayEntities) {
        this.displayEntities = displayEntities;
    }

    public void spawnAll(@Nonnull Location location) {
        for (TransformedDisplay displayEntity : this.displayEntities) {
            displayEntity.spawn(location);
        }
    }

    public void removeAll() {
        for (TransformedDisplay displayEntity : this.displayEntities) {
            displayEntity.remove();
        }
    }

}
