package jp.opevista.mineagent.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configDir;
    private final Path configPath;
    private MineAgentConfig config = MineAgentConfig.defaults();

    public ConfigManager() {
        configDir = FabricLoader.getInstance().getConfigDir().resolve("mineagent");
        configPath = configDir.resolve("config.json");
    }

    public MineAgentConfig load() {
        try {
            Files.createDirectories(configDir);
            if (Files.notExists(configPath)) {
                config = MineAgentConfig.defaults();
                save();
                return config;
            }
            String text = Files.readString(configPath, StandardCharsets.UTF_8);
            MineAgentConfig loaded = MineAgentConfig.defaults();
            loaded.read(JsonParser.parseString(text).getAsJsonObject());
            config = loaded;
            save();
        } catch (Exception e) {
            config = MineAgentConfig.defaults();
            saveQuietly();
        }
        return config;
    }

    public void save() throws IOException {
        Files.createDirectories(configDir);
        Files.writeString(configPath, GSON.toJson(config.toJson()), StandardCharsets.UTF_8);
    }

    private void saveQuietly() {
        try {
            save();
        } catch (IOException ignored) {
        }
    }

    public Path configDir() {
        return configDir;
    }

    public MineAgentConfig config() {
        return config;
    }
}
