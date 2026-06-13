package jp.opevista.mineagent.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

final class JsonReaders {
    private JsonReaders() {
    }

    static JsonObject object(JsonObject json, String name) {
        if (json == null || !json.has(name) || !json.get(name).isJsonObject()) {
            return new JsonObject();
        }
        return json.getAsJsonObject(name);
    }

    static String string(JsonObject json, String name, String fallback) {
        JsonElement value = value(json, name);
        return value == null ? fallback : value.getAsString();
    }

    static boolean bool(JsonObject json, String name, boolean fallback) {
        JsonElement value = value(json, name);
        return value == null ? fallback : value.getAsBoolean();
    }

    static int integer(JsonObject json, String name, int fallback) {
        JsonElement value = value(json, name);
        return value == null ? fallback : value.getAsInt();
    }

    static double decimal(JsonObject json, String name, double fallback) {
        JsonElement value = value(json, name);
        return value == null ? fallback : value.getAsDouble();
    }

    private static JsonElement value(JsonObject json, String name) {
        if (json == null || !json.has(name) || json.get(name).isJsonNull()) {
            return null;
        }
        return json.get(name);
    }
}
