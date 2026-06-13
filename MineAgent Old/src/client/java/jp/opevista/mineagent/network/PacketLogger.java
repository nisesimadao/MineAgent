package jp.opevista.mineagent.network;

import java.util.ArrayList;
import java.util.List;

public final class PacketLogger {
    private final List<PacketEvent> events = new ArrayList<>();
    private int maxEntries;

    public PacketLogger(int maxEntries) {
        this.maxEntries = Math.max(100, maxEntries);
    }

    public synchronized void setMaxEntries(int maxEntries) {
        this.maxEntries = Math.max(100, maxEntries);
        trim();
    }

    public synchronized void add(PacketEvent event) {
        events.add(event);
        trim();
    }

    public synchronized List<PacketEvent> recent(String direction, int limit, String filter) {
        String normalizedDirection = direction == null ? "BOTH" : direction;
        String normalizedFilter = filter == null ? "" : filter.toLowerCase();
        List<PacketEvent> matching = events.stream()
                .filter(event -> "BOTH".equalsIgnoreCase(normalizedDirection) || event.direction().name().equalsIgnoreCase(normalizedDirection))
                .filter(event -> normalizedFilter.isBlank()
                        || event.packetName().toLowerCase().contains(normalizedFilter)
                        || event.rawToString().toLowerCase().contains(normalizedFilter))
                .toList();
        int from = Math.max(0, matching.size() - Math.max(1, limit));
        return List.copyOf(matching.subList(from, matching.size()));
    }

    public synchronized void clear() {
        events.clear();
    }

    private void trim() {
        while (events.size() > maxEntries) {
            events.removeFirst();
        }
    }
}
