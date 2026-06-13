package jp.opevista.mineagent.bridge;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class ChatLog {
    private static final int MAX_MESSAGES = 200;
    private static final Deque<Entry> MESSAGES = new ArrayDeque<>();
    private static boolean registered;

    private ChatLog() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;

        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            String senderName = sender == null ? "" : sender.toString();
            add("chat", message, false, senderName);
        });
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> add("game", message, overlay, ""));
    }

    public static synchronized void add(String type, Component message, boolean overlay, String sender) {
        MESSAGES.addLast(new Entry(
                Instant.now().toString(),
                type,
                message.getString(),
                message.getString(),
                overlay,
                sender == null ? "" : sender
        ));
        while (MESSAGES.size() > MAX_MESSAGES) {
            MESSAGES.removeFirst();
        }
    }

    public static synchronized JsonArray recent(int count, String typeFilter, String contains) {
        int normalizedCount = Math.max(1, Math.min(count, MAX_MESSAGES));
        String normalizedType = typeFilter == null ? "" : typeFilter.trim().toLowerCase();
        String normalizedContains = contains == null ? "" : contains.trim().toLowerCase();

        List<Entry> copy = new ArrayList<>(MESSAGES);
        JsonArray result = new JsonArray();
        for (int i = copy.size() - 1; i >= 0 && result.size() < normalizedCount; i--) {
            Entry entry = copy.get(i);
            if (!normalizedType.isBlank() && !entry.type.equalsIgnoreCase(normalizedType)) {
                continue;
            }
            if (!normalizedContains.isBlank() && !entry.text.toLowerCase().contains(normalizedContains)) {
                continue;
            }
            result.add(entry.toJson());
        }
        return result;
    }

    public static synchronized int size() {
        return MESSAGES.size();
    }

    private record Entry(String time, String type, String text, String rawText, boolean overlay, String sender) {
        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("time", time);
            json.addProperty("type", type);
            json.addProperty("text", text);
            json.addProperty("rawText", rawText);
            json.addProperty("overlay", overlay);
            json.addProperty("sender", sender);
            return json;
        }
    }
}
