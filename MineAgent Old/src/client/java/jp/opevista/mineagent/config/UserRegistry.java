package jp.opevista.mineagent.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class UserRegistry {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path path;
    private final List<RegisteredUser> users = new ArrayList<>();

    public UserRegistry(Path configDir) {
        path = configDir.resolve("registered_users.json");
    }

    public void load() {
        users.clear();
        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                users.add(new RegisteredUser("opevista", "", "owner"));
                save();
                return;
            }
            JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray array = root.has("users") && root.get("users").isJsonArray() ? root.getAsJsonArray("users") : new JsonArray();
            array.forEach(element -> {
                JsonObject user = element.getAsJsonObject();
                users.add(new RegisteredUser(
                        JsonReaders.string(user, "name", ""),
                        JsonReaders.string(user, "uuid", ""),
                        JsonReaders.string(user, "role", "user")
                ));
            });
            if (users.isEmpty()) {
                users.add(new RegisteredUser("opevista", "", "owner"));
            }
            save();
        } catch (Exception e) {
            users.add(new RegisteredUser("opevista", "", "owner"));
            saveQuietly();
        }
    }

    public void save() throws IOException {
        JsonObject root = new JsonObject();
        JsonArray array = new JsonArray();
        for (RegisteredUser user : users) {
            JsonObject json = new JsonObject();
            json.addProperty("name", user.name());
            json.addProperty("uuid", user.uuid());
            json.addProperty("role", user.role());
            array.add(json);
        }
        root.add("users", array);
        Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
    }

    public boolean isRegistered(String name, String uuid) {
        String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
        String id = uuid == null ? "" : uuid;
        return users.stream().anyMatch(user ->
                (!user.uuid().isBlank() && user.uuid().equalsIgnoreCase(id))
                        || (!user.name().isBlank() && user.name().toLowerCase(Locale.ROOT).equals(lower)));
    }

    public List<RegisteredUser> users() {
        return List.copyOf(users);
    }

    private void saveQuietly() {
        try {
            save();
        } catch (IOException ignored) {
        }
    }
}
