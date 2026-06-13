package jp.opevista.mineagent.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jp.opevista.mineagent.MineAgentClient;
import jp.opevista.mineagent.config.MineAgentConfig;

import java.time.Instant;
import java.util.List;

public final class PacketGateway {
    private final PacketLogger logger;
    private MineAgentConfig config;

    public PacketGateway(MineAgentConfig config) {
        this.config = config;
        logger = new PacketLogger(config.packetLayer.maxPacketLogEntries);
    }

    public static void capture(PacketDirection direction, Object packet) {
        MineAgentClient mod = MineAgentClient.get();
        if (mod == null) {
            return;
        }
        mod.packetGateway().log(direction, packet);
    }

    public void applyConfig(MineAgentConfig config) {
        this.config = config;
        logger.setMaxEntries(config.packetLayer.maxPacketLogEntries);
    }

    public void log(PacketDirection direction, Object packet) {
        if (!config.packetLayer.enabled || !config.packetLayer.logPackets) {
            return;
        }
        if (direction == PacketDirection.C2S && !config.packetLayer.sendPackets) {
            return;
        }
        if (direction == PacketDirection.S2C && !config.packetLayer.readPackets) {
            return;
        }
        logger.add(PacketEvent.of(direction, packet));
    }

    public JsonArray recentJson(String direction, int limit, String filter) {
        JsonArray array = new JsonArray();
        for (PacketEvent event : logger.recent(direction, limit, filter)) {
            JsonObject json = new JsonObject();
            json.addProperty("id", event.id());
            json.addProperty("time", Instant.ofEpochMilli(event.timeMs()).toString());
            json.addProperty("direction", event.direction().name());
            json.addProperty("packetClass", event.packetClass());
            json.addProperty("packetName", event.packetName());
            json.add("summary", event.summary());
            json.addProperty("rawToString", event.rawToString());
            array.add(json);
        }
        return array;
    }

    public String recentAsText(int limit) {
        StringBuilder builder = new StringBuilder();
        List<PacketEvent> events = logger.recent("BOTH", limit, "");
        if (events.isEmpty()) {
            return "No MineAgent packet events yet";
        }
        for (PacketEvent event : events) {
            builder.append(event.direction()).append(" ").append(event.packetName()).append(" ").append(event.summary()).append("\n");
        }
        return builder.toString();
    }

    public void clear() {
        logger.clear();
    }
}
