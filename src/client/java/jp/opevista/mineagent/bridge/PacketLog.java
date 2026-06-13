package jp.opevista.mineagent.bridge;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

public final class PacketLog {
    private static final int MAX_PACKETS = 100;
    private static final Deque<Entry> PACKETS = new ArrayDeque<>();

    private PacketLog() {}

    public static synchronized void add(String direction, String packetType) {
        PACKETS.addLast(new Entry(Instant.now().toString(), direction, packetType));
        while (PACKETS.size() > MAX_PACKETS) {
            PACKETS.removeFirst();
        }
    }

    public static synchronized JsonArray recent(int count) {
        int normalizedCount = Math.max(1, Math.min(count, MAX_PACKETS));
        JsonArray result = new JsonArray();
        PACKETS.descendingIterator().forEachRemaining(entry -> {
            if (result.size() < normalizedCount) {
                result.add(entry.toJson());
            }
        });
        return result;
    }

    private record Entry(String time, String direction, String packetType) {
        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("time", time);
            json.addProperty("direction", direction);
            json.addProperty("packetType", packetType);
            return json;
        }
    }
}
