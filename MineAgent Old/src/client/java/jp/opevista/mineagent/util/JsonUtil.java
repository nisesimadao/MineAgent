package jp.opevista.mineagent.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class JsonUtil {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private JsonUtil() {
    }

    public static JsonObject parseObjectOrEmpty(String text) {
        try {
            JsonElement element = JsonParser.parseString(text == null || text.isBlank() ? "{}" : text);
            return element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    public static String string(JsonObject object, String name, String fallback) {
        if (object == null || !object.has(name) || object.get(name).isJsonNull()) {
            return fallback;
        }
        return object.get(name).getAsString();
    }

    public static int integer(JsonObject object, String name, int fallback) {
        if (object == null || !object.has(name) || object.get(name).isJsonNull()) {
            return fallback;
        }
        return object.get(name).getAsInt();
    }

    public static JsonObject object(JsonObject object, String name) {
        if (object == null || !object.has(name) || !object.get(name).isJsonObject()) {
            return new JsonObject();
        }
        return object.getAsJsonObject(name);
    }
}
