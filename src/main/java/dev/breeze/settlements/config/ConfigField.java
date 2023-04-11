package dev.breeze.settlements.config;

import com.google.common.base.Preconditions;
import dev.breeze.settlements.utils.itemstack.ItemStackBuilder;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A class representing a field in a config file
 */
@ParametersAreNonnullByDefault
@Getter
public final class ConfigField<T> {

    /**
     * The config file wrapper that contains this field
     */
    @Nonnull
    private final ConfigFileWrapper file;

    /**
     * Matching ConfigType value, used to ensure that this field is type-valid
     */
    @Nonnull
    private final ConfigType type;

    /**
     * The name of the configuration field.
     */
    @Nonnull
    private final String name;

    /**
     * Description of the configuration field, often located on top as a comment
     */
    @Nonnull
    private final List<String> description;

    /**
     * The default value of this field
     */
    @Nonnull
    private final T defaultValue;

    /**
     * The current value of the configuration field
     */
    @Nullable
    private T value;

    /*
     * Config GUI fields
     */
    /**
     * The itemstack to represent this field
     * - defaults to a piece of paper
     */
    @Setter
    @Nonnull
    private ItemStack guiItem = new ItemStackBuilder(Material.PAPER).build();

    public ConfigField(ConfigFileWrapper file, ConfigType type, String name, List<String> description, T defaultValue) {
        // Check type correspondence as a pre-condition
        Preconditions.checkArgument(type.isTypeValid(defaultValue.getClass()),
                "Invalid config type to data type mapping! Config type is '%s' while value type is '%s'",
                type, defaultValue.getClass().getSimpleName());

        this.file = file;
        this.type = type;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;

        // Attempt to load value from config if not empty
        if (this.type != ConfigType.EMPTY) {
            this.value = this.file.getAsType(this.name, this.type, this.defaultValue);
        }

        // Register the comment to the wrapper
        this.file.setComment(this.name, this.description);

        // Sync to file
        this.writeToFile();
    }

    /**
     * Shorthand constructor for fields that only require one line of description
     */
    public ConfigField(ConfigFileWrapper file, ConfigType type, String name, String description, T defaultValue) {
        this(file, type, name, Collections.singletonList(description), defaultValue);
    }

    @Nonnull
    public T getValue() {
        if (this.type == ConfigType.EMPTY) {
            throw new IllegalStateException("Cannot get an empty config object!");
        }
        return Objects.requireNonNull(this.value);
    }

    /**
     * Sets a new value for the configuration field and writes it to the config file
     *
     * @param newValue the new value to set for the configuration field
     */
    public void setValue(@Nonnull T newValue) {
        this.value = newValue;
        this.writeToFile();
    }

    /**
     * Writes the current value to file
     */
    private void writeToFile() {
        if (this.type != ConfigType.EMPTY) {
            this.file.setAsType(this.name, this.type, this.value);
        }

        // Append default value to comments
        List<String> comments = new ArrayList<>(this.description);
        if (this.type != ConfigType.EMPTY) {
            comments.add(String.format("Type: %s", this.type.getDescription()));
            comments.add(String.format("Default: %s", this.defaultValue));
        }

        this.file.setComment(this.name, comments);
        this.file.save();
    }

}
