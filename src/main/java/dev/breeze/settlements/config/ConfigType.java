package dev.breeze.settlements.config;

import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public enum ConfigType {

    /**
     * Should only be used if no subtypes are applicable
     */
    OBJECT(Object.class, new ConfigTypeSpecification<>() {
        @Override
        public Object get(FileConfiguration config, String path) {
            return config.get(path);
        }
    }),

    /**
     * Should only be used for specifying category comments
     * - note that the value and default value of this type will be ignored
     */
    EMPTY(Object.class, new EmptyConfigTypeSpecification()),

    BOOLEAN(Boolean.class, new ConfigTypeSpecification<Boolean>() {
        @Override
        public Boolean get(FileConfiguration config, String path) {
            return config.getBoolean(path);
        }
    }),
    COLOR(Color.class, new ConfigTypeSpecification<Color>() {
        @Override
        protected Color get(FileConfiguration config, String path) {
            return config.getColor(path);
        }
    }),
    DOUBLE(Double.class, new ConfigTypeSpecification<Double>() {
        @Override
        public Double get(FileConfiguration config, String path) {
            return config.getDouble(path);
        }
    }),
    INT(Integer.class, new ConfigTypeSpecification<Integer>() {
        @Override
        public Integer get(FileConfiguration config, String path) {
            return config.getInt(path);
        }
    }),
    ITEM_STACK(ItemStack.class, new ConfigTypeSpecification<ItemStack>() {
        @Override
        protected ItemStack get(FileConfiguration config, String path) {
            return config.getItemStack(path);
        }
    }),
    LOCATION(Location.class, new ConfigTypeSpecification<Location>() {
        @Override
        protected Location get(FileConfiguration config, String path) {
            return config.getLocation(path);
        }
    }),
    LONG(Long.class, new ConfigTypeSpecification<Long>() {
        @Override
        protected Long get(FileConfiguration config, String path) {
            return config.getLong(path);
        }
    }),
    OFFLINE_PLAYER(OfflinePlayer.class, new ConfigTypeSpecification<OfflinePlayer>() {
        @Override
        protected OfflinePlayer get(FileConfiguration config, String path) {
            return config.getOfflinePlayer(path);
        }
    }),
    STRING(String.class, new ConfigTypeSpecification<String>() {
        @Override
        public String get(FileConfiguration config, String path) {
            return config.getString(path);
        }
    }),
    VECTOR(Vector.class, new ConfigTypeSpecification<Vector>() {
        @Override
        protected Vector get(FileConfiguration config, String path) {
            return config.getVector(path);
        }
    }),

    /**
     * Should only be used if no specified list subtypes are applicable
     */
    GENERIC_LIST(List.class, new ConfigTypeSpecification<List<?>>() {
        @Override
        protected List<?> get(FileConfiguration config, String path) {
            return config.getList(path);
        }
    }),
    BOOLEAN_LIST(List.class, new ConfigTypeSpecification<List<Boolean>>() {
        @Override
        protected List<Boolean> get(FileConfiguration config, String path) {
            return config.getBooleanList(path);
        }
    }),
    BYTE_LIST(List.class, new ConfigTypeSpecification<List<Byte>>() {
        @Override
        protected List<Byte> get(FileConfiguration config, String path) {
            return config.getByteList(path);
        }
    }),
    DOUBLE_LIST(List.class, new ConfigTypeSpecification<List<Double>>() {
        @Override
        protected List<Double> get(FileConfiguration config, String path) {
            return config.getDoubleList(path);
        }
    }),
    INT_LIST(List.class, new ConfigTypeSpecification<List<Integer>>() {
        @Override
        protected List<Integer> get(FileConfiguration config, String path) {
            return config.getIntegerList(path);
        }
    }),
    LONG_LIST(List.class, new ConfigTypeSpecification<List<Long>>() {
        @Override
        protected List<Long> get(FileConfiguration config, String path) {
            return config.getLongList(path);
        }
    }),
    STRING_LIST(List.class, new ConfigTypeSpecification<List<String>>() {
        @Override
        protected List<String> get(FileConfiguration config, String path) {
            return config.getStringList(path);
        }
    });


    private final Class<?> valueClass;
    private final ConfigTypeSpecification<?> specification;

    private static final Map<Class<?>, ConfigType> CLASS_TO_TYPE = new HashMap<>();

    static {
        for (ConfigType type : values()) {
            CLASS_TO_TYPE.put(type.valueClass, type);
        }
    }

    /**
     * Creates a new ConfigType with the given specification.
     *
     * @param <T>        the type of value that can be read from and written to a configuration file using this ConfigType
     * @param valueClass the type of the raw value that's stored here, e.g. Integer, Boolean, etc
     * @param configType the specification for reading and writing values of this type from a configuration file
     */
    <T> ConfigType(Class<?> valueClass, ConfigTypeSpecification<T> configType) {
        this.valueClass = valueClass;
        this.specification = configType;
    }

    public boolean isTypeValid(Class<?> valueClass) {
        return this.valueClass.isAssignableFrom(valueClass);
    }

    /**
     * Represents a specification for reading and writing values of a specific type from a configuration file
     *
     * @param <T> the type of value that can be read from and written to a configuration file using this specification
     */
    public abstract static class ConfigTypeSpecification<T> {

        /**
         * Returns the value of the specified type that is stored in the configuration file at the given path
         *
         * @param config the configuration file to read from
         * @param path   the path in the configuration file to read from
         * @return the value of the specified type that is stored in the configuration file at the given path
         */
        @Deprecated
        protected abstract T get(FileConfiguration config, String path);

        /**
         * Returns the value of the specified type that is stored in the configuration file at the given path,
         * or the specified default value if no value is stored at the given path
         *
         * @param config       the configuration file to read from
         * @param path         the path in the configuration file to read from
         * @param defaultValue the default value to return if no value is stored at the given path
         * @return the value stored in the configuration file at the given path, or the default of null
         * @throws ClassCastException if the value stored at the given path cannot be cast to the specified type
         */
        @SuppressWarnings("unchecked")
        public T get(FileConfiguration config, String path, Object defaultValue) throws ClassCastException {
            // Check if path exists
            if (!config.contains(path)) {
                // Path doesn't exist, return default
                return (T) defaultValue;
            }

            // Path exists, get it
            return this.get(config, path);
        }

        /**
         * Sets the value of the configuration file at the given path to the specified value
         *
         * @param config the configuration file to write to
         * @param path   the path in the configuration file to write to
         * @param value  the value to write to the configuration file
         * @throws ClassCastException if the specified value cannot be cast to the appropriate type for this specification
         */
        @SuppressWarnings("unchecked")
        public void set(FileConfiguration config, String path, Object value) throws ClassCastException {
            // Try to cast to the type, throwing errors if needed
            // - this is done to verify type correctness
            T typedData = (T) value;

            // Set the value
            config.set(path, typedData);
        }
    }

    public static class EmptyConfigTypeSpecification extends ConfigTypeSpecification<Object> {

        @Override
        protected Object get(FileConfiguration config, String path) {
            throw new RuntimeException("Cannot get an empty config object!");
        }

        @Override
        public Object get(FileConfiguration config, String path, Object defaultValue) throws ClassCastException {
            throw new RuntimeException("Cannot get an empty config object!");
        }

        @Override
        public void set(FileConfiguration config, String path, Object value) throws ClassCastException {
            throw new RuntimeException("Cannot set an empty config object!");
        }
    }

}
