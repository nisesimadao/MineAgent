package jp.opevista.mineagent.bridge;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class KeyPulse {
    private static final List<Pulse> PULSES = new ArrayList<>();

    static {
        ClientTickEvents.END_CLIENT_TICK.register(KeyPulse::tick);
    }

    private KeyPulse() {
    }

    static void schedule(Minecraft client, String direction, int ticks) {
        KeyMapping key = switch (direction) {
            case "back", "backward" -> client.options.keyDown;
            case "left" -> client.options.keyLeft;
            case "right" -> client.options.keyRight;
            case "sneak" -> client.options.keyShift;
            case "sprint" -> client.options.keySprint;
            default -> client.options.keyUp;
        };
        key.setDown(true);
        PULSES.add(new Pulse(key, ticks));
    }

    static void schedule(KeyMapping key, int ticks) {
        PULSES.add(new Pulse(key, ticks));
    }

    static void clearAndRelease(Minecraft client) {
        for (Pulse pulse : PULSES) {
            pulse.key.setDown(false);
        }
        PULSES.clear();
        for (KeyMapping mapping : client.options.keyMappings) {
            mapping.setDown(false);
        }
    }

    private static void tick(Minecraft client) {
        Iterator<Pulse> iterator = PULSES.iterator();
        while (iterator.hasNext()) {
            Pulse pulse = iterator.next();
            pulse.remainingTicks--;
            if (pulse.remainingTicks <= 0) {
                pulse.key.setDown(false);
                iterator.remove();
            }
        }
    }

    private static final class Pulse {
        private final KeyMapping key;
        private int remainingTicks;

        private Pulse(KeyMapping key, int remainingTicks) {
            this.key = key;
            this.remainingTicks = remainingTicks;
        }
    }
}
