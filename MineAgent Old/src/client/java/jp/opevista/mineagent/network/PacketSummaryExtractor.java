package jp.opevista.mineagent.network;

import com.google.gson.JsonObject;

public final class PacketSummaryExtractor {
    private PacketSummaryExtractor() {
    }

    public static JsonObject summarize(Object packet) {
        JsonObject summary = new JsonObject();
        if (packet == null) {
            summary.addProperty("null", true);
            return summary;
        }
        String name = packet.getClass().getSimpleName();
        String text = String.valueOf(packet);
        summary.addProperty("packet", name);
        if (name.toLowerCase().contains("chat") || name.toLowerCase().contains("game")) {
            summary.addProperty("category", "Chat");
        } else if (name.toLowerCase().contains("custom")) {
            summary.addProperty("category", "CustomPayload");
        } else if (name.toLowerCase().contains("entity")) {
            summary.addProperty("category", "Entity");
        } else if (name.toLowerCase().contains("health")) {
            summary.addProperty("category", "Health");
        } else {
            summary.addProperty("unparsed", true);
        }
        summary.addProperty("toString", text.length() > 500 ? text.substring(0, 500) : text);
        return summary;
    }
}
