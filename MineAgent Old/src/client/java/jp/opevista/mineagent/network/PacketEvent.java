package jp.opevista.mineagent.network;

import com.google.gson.JsonObject;

import java.util.UUID;

public record PacketEvent(
        String id,
        long timeMs,
        PacketDirection direction,
        String packetClass,
        String packetName,
        JsonObject summary,
        String rawToString
) {
    public static PacketEvent of(PacketDirection direction, Object packet) {
        String className = packet == null ? "null" : packet.getClass().getName();
        String simpleName = packet == null ? "null" : packet.getClass().getSimpleName();
        JsonObject summary = PacketSummaryExtractor.summarize(packet);
        return new PacketEvent(UUID.randomUUID().toString(), System.currentTimeMillis(), direction, className, simpleName, summary, String.valueOf(packet));
    }
}
