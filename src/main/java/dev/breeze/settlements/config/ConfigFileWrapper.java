package dev.breeze.settlements.config;

import dev.breeze.settlements.Main;
import dev.breeze.settlements.utils.LogUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * A wrapper class for a YAML configuration file
 */
public final class ConfigFileWrapper {

    @Nonnull
    private final String fileName;
    @Nonnull
    private final File fileInstance;
    @Nonnull
    private FileConfiguration config;

    @Nonnull
    private final HashMap<String, List<String>> comments;

    /**
     * Creates a new instance of the ConfigFileWrapper class with the specified file name.
     *
     * @param fileName the name of the configuration file (without the .yml extension)
     * @throws IOException if an I/O error occurs while creating or loading the configuration file
     */
    public ConfigFileWrapper(@Nonnull String fileName) throws IOException {
        this.fileName = fileName;

        // Get or create config file instance
        this.fileInstance = new File(Main.getPlugin().getDataFolder(), String.format("%s.yml", this.fileName));
        if (!this.fileInstance.exists()) {
            boolean created = this.fileInstance.createNewFile();
            if (!created) {
                LogUtil.warning("Tried to create config file '%s.yml' when it already exists!", this.fileName);
            }
        }

        // Load file into memory
        this.config = YamlConfiguration.loadConfiguration(this.fileInstance);

        // Initialize comment map
        this.comments = new HashMap<>();
    }

    /**
     * Attempts to save the loaded configuration file to disk
     *
     * @throws RuntimeException if config file saving failed
     */
    public void save() {
        try {
            this.config.save(this.fileInstance);
            this.commentFile();
        } catch (IOException e) {
            LogUtil.exception(e, "Failed to save config file '%s.yml'!", this.fileName);
            throw new RuntimeException(e);
        }
    }

    /**
     * Reloads the in-memory configuration file from disk
     *
     * @throws IllegalArgumentException if the configuration file does not exist
     */
    public void reload() throws IllegalArgumentException {
        this.config = YamlConfiguration.loadConfiguration(this.fileInstance);
    }

    /**
     * Gets the value of the specified path in the configuration file as the specified type
     *
     * @param path         the path of the value to get
     * @param type         the type of the value to get; use {@link ConfigType#OBJECT} if none of the other types apply to the variable
     * @param defaultValue the default value to return if no value is found at the specified path
     * @return the value at the specified path, converted to the specified type, or the default value if no value is found
     * @throws ClassCastException if the value at the specified path cannot be cast to the specified type
     */
    @SuppressWarnings("unchecked")
    public <T> T getAsType(String path, ConfigType type, T defaultValue) throws ClassCastException {
        return (T) type.getSpecification().get(this.config, path, defaultValue);
    }

    /**
     * Sets the value of the specified path in the configuration file to the specified value, using the specified type.
     *
     * @param path  the path of the value to set
     * @param type  the type of the value to set; use {@link ConfigType#OBJECT} if none of the other types apply to the variable
     * @param value the value to set
     */
    public <T> void setAsType(String path, ConfigType type, T value) {
        type.getSpecification().set(config, path, value);
    }

    public void setComment(String path, List<String> comments) {
        this.comments.put(path, comments);
    }

    /**
     * Attempts to comment the file with the registered comments
     * <p>
     * Reference:
     * <a href="https://github.com/BentoBoxWorld/BentoBox/blob/develop/src/main/java/world/bentobox/bentobox/database/yaml/YamlDatabaseConnector.java#L144">Bento box config API</a>
     * <p>
     * TODO: in the future, it might be better to use a YAML library like SnakeYAML
     */
    private void commentFile() throws IOException {
        // Run through the file and add in the comments
        File tempFile = File.createTempFile("stm", null);
        tempFile.deleteOnExit();

        List<String> linesToWrite = new ArrayList<>();
        try (Scanner scanner = new Scanner(this.fileInstance, StandardCharsets.UTF_8)) {
            // Save indentations
            Deque<String> indentationStack = new ArrayDeque<>();

            // Loop through all lines in the original config file
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                // Check base conditions
                // 1. ignore blank lines
                // 2. ignore comment lines (we'll regenerate them)
                if (line.isBlank() || line.strip().startsWith("#")) {
                    continue;
                }

                // Calculate indentation level
                int level = line.indexOf(line.strip()) / 2;
                while (indentationStack.size() > level) {
                    indentationStack.pop();
                }

                String key = line.trim().split(":", 2)[0];
                indentationStack.push(key);
                String compositeKey = joinStack(indentationStack);

                for (Map.Entry<String, List<String>> entry : this.comments.entrySet()) {
                    // Check if the comment is for the current line
                    if (!compositeKey.equals(entry.getKey())) {
                        continue;
                    }

                    // Add all comment lines with the correct indentation
                    String indentation = " ".repeat(level * 2);
                    for (String comment : entry.getValue()) {
                        linesToWrite.add(String.format("%s# %s", indentation, comment));
                    }
                    break;
                }

                // Add original line
                linesToWrite.add(line);
            }

            // Write lines to the temporary file
            Files.write(tempFile.toPath(), (Iterable<String>) linesToWrite.stream()::iterator);

            // Try to copy the temporary file to the original YAML file
            try (InputStream is = new FileInputStream(tempFile); OutputStream os = new FileOutputStream(this.fileInstance)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                }
            }
        } finally {
            // Delete the temporary file
            Files.delete(tempFile.toPath());
        }
    }

    private String joinStack(Deque<String> stack) {
        StringBuilder builder = new StringBuilder();
        for (String string : stack) {
            builder.insert(0, string + ".");
        }

        // Delete trailing period
        if (builder.charAt(builder.length() - 1) == '.') {
            builder.deleteCharAt(builder.length() - 1);
        }

        return builder.toString();
    }

}
