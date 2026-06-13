package jp.opevista.mineagent.config;

import com.google.gson.JsonObject;

public final class MineAgentConfig {
    public boolean enabled = true;
    public String agentName = "opevista";
    public String triggerMode = "name_prefix";
    public final WebUi webUi = new WebUi();
    public final Llm llm = new Llm();
    public final AgentLoopConfig agentLoop = new AgentLoopConfig();
    public final Permissions permissions = new Permissions();
    public final PacketLayer packetLayer = new PacketLayer();
    public final Baritone baritone = new Baritone();

    public static MineAgentConfig defaults() {
        return new MineAgentConfig();
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("enabled", enabled);
        root.addProperty("agentName", agentName);
        root.addProperty("triggerMode", triggerMode);
        root.add("webUi", webUi.toJson());
        root.add("llm", llm.toJson());
        root.add("agentLoop", agentLoop.toJson());
        root.add("permissions", permissions.toJson());
        root.add("packetLayer", packetLayer.toJson());
        root.add("baritone", baritone.toJson());
        return root;
    }

    public void read(JsonObject root) {
        enabled = JsonReaders.bool(root, "enabled", enabled);
        agentName = JsonReaders.string(root, "agentName", agentName);
        triggerMode = JsonReaders.string(root, "triggerMode", triggerMode);
        webUi.read(JsonReaders.object(root, "webUi"));
        llm.read(JsonReaders.object(root, "llm"));
        agentLoop.read(JsonReaders.object(root, "agentLoop"));
        permissions.read(JsonReaders.object(root, "permissions"));
        packetLayer.read(JsonReaders.object(root, "packetLayer"));
        baritone.read(JsonReaders.object(root, "baritone"));
    }

    public static final class WebUi {
        public boolean enabled = true;
        public String host = "127.0.0.1";
        public int port = 25712;
        public boolean openOnStart = false;
        public boolean requireTokenWhenNotLocalhost = true;
        public String token = "";

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("enabled", enabled);
            json.addProperty("host", host);
            json.addProperty("port", port);
            json.addProperty("openOnStart", openOnStart);
            json.addProperty("requireTokenWhenNotLocalhost", requireTokenWhenNotLocalhost);
            json.addProperty("token", token);
            return json;
        }

        void read(JsonObject json) {
            enabled = JsonReaders.bool(json, "enabled", enabled);
            host = JsonReaders.string(json, "host", host);
            port = JsonReaders.integer(json, "port", port);
            openOnStart = JsonReaders.bool(json, "openOnStart", openOnStart);
            requireTokenWhenNotLocalhost = JsonReaders.bool(json, "requireTokenWhenNotLocalhost", requireTokenWhenNotLocalhost);
            token = JsonReaders.string(json, "token", token);
        }
    }

    public static final class Llm {
        public boolean enabled = true;
        public String provider = "mock";
        public String apiMode = "chat_completions";
        public String baseUrl = "http://localhost:1234/v1";
        public String apiKey = "";
        public String model = "local-model";
        public double temperature = 0.2;
        public int timeoutMs = 60000;
        public int maxOutputTokens = 2048;
        public int jsonRepairAttempts = 1;

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("enabled", enabled);
            json.addProperty("provider", provider);
            json.addProperty("apiMode", apiMode);
            json.addProperty("baseUrl", baseUrl);
            json.addProperty("apiKey", apiKey);
            json.addProperty("model", model);
            json.addProperty("temperature", temperature);
            json.addProperty("timeoutMs", timeoutMs);
            json.addProperty("maxOutputTokens", maxOutputTokens);
            json.addProperty("jsonRepairAttempts", jsonRepairAttempts);
            return json;
        }

        void read(JsonObject json) {
            enabled = JsonReaders.bool(json, "enabled", enabled);
            provider = JsonReaders.string(json, "provider", provider);
            apiMode = JsonReaders.string(json, "apiMode", apiMode);
            baseUrl = JsonReaders.string(json, "baseUrl", baseUrl);
            apiKey = JsonReaders.string(json, "apiKey", apiKey);
            model = JsonReaders.string(json, "model", model);
            temperature = JsonReaders.decimal(json, "temperature", temperature);
            timeoutMs = JsonReaders.integer(json, "timeoutMs", timeoutMs);
            maxOutputTokens = JsonReaders.integer(json, "maxOutputTokens", maxOutputTokens);
            jsonRepairAttempts = JsonReaders.integer(json, "jsonRepairAttempts", jsonRepairAttempts);
        }
    }

    public static final class AgentLoopConfig {
        public boolean enabled = true;
        public int maxSteps = 100;
        public int tickInterval = 5;
        public boolean autoContinue = true;
        public boolean stopOnDeath = true;

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("enabled", enabled);
            json.addProperty("maxSteps", maxSteps);
            json.addProperty("tickInterval", tickInterval);
            json.addProperty("autoContinue", autoContinue);
            json.addProperty("stopOnDeath", stopOnDeath);
            return json;
        }

        void read(JsonObject json) {
            enabled = JsonReaders.bool(json, "enabled", enabled);
            maxSteps = JsonReaders.integer(json, "maxSteps", maxSteps);
            tickInterval = JsonReaders.integer(json, "tickInterval", tickInterval);
            autoContinue = JsonReaders.bool(json, "autoContinue", autoContinue);
            stopOnDeath = JsonReaders.bool(json, "stopOnDeath", stopOnDeath);
        }
    }

    public static final class Permissions {
        public boolean allowCommands = true;
        public boolean allowBaritone = true;
        public boolean allowPacketRead = true;
        public boolean allowPacketSend = true;
        public boolean allowCustomPayload = true;

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("allowCommands", allowCommands);
            json.addProperty("allowBaritone", allowBaritone);
            json.addProperty("allowPacketRead", allowPacketRead);
            json.addProperty("allowPacketSend", allowPacketSend);
            json.addProperty("allowCustomPayload", allowCustomPayload);
            return json;
        }

        void read(JsonObject json) {
            allowCommands = JsonReaders.bool(json, "allowCommands", allowCommands);
            allowBaritone = JsonReaders.bool(json, "allowBaritone", allowBaritone);
            allowPacketRead = JsonReaders.bool(json, "allowPacketRead", allowPacketRead);
            allowPacketSend = JsonReaders.bool(json, "allowPacketSend", allowPacketSend);
            allowCustomPayload = JsonReaders.bool(json, "allowCustomPayload", allowCustomPayload);
        }
    }

    public static final class PacketLayer {
        public boolean enabled = true;
        public boolean readPackets = true;
        public boolean sendPackets = true;
        public boolean logPackets = true;
        public int maxPacketLogEntries = 5000;
        public String defaultPacketView = "summary";

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("enabled", enabled);
            json.addProperty("readPackets", readPackets);
            json.addProperty("sendPackets", sendPackets);
            json.addProperty("logPackets", logPackets);
            json.addProperty("maxPacketLogEntries", maxPacketLogEntries);
            json.addProperty("defaultPacketView", defaultPacketView);
            return json;
        }

        void read(JsonObject json) {
            enabled = JsonReaders.bool(json, "enabled", enabled);
            readPackets = JsonReaders.bool(json, "readPackets", readPackets);
            sendPackets = JsonReaders.bool(json, "sendPackets", sendPackets);
            logPackets = JsonReaders.bool(json, "logPackets", logPackets);
            maxPacketLogEntries = JsonReaders.integer(json, "maxPacketLogEntries", maxPacketLogEntries);
            defaultPacketView = JsonReaders.string(json, "defaultPacketView", defaultPacketView);
        }
    }

    public static final class Baritone {
        public boolean enabled = true;
        public String mode = "optional";
        public boolean preferApi = false;
        public String commandPrefix = "#";

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("enabled", enabled);
            json.addProperty("mode", mode);
            json.addProperty("preferApi", preferApi);
            json.addProperty("commandPrefix", commandPrefix);
            return json;
        }

        void read(JsonObject json) {
            enabled = JsonReaders.bool(json, "enabled", enabled);
            mode = JsonReaders.string(json, "mode", mode);
            preferApi = JsonReaders.bool(json, "preferApi", preferApi);
            commandPrefix = JsonReaders.string(json, "commandPrefix", commandPrefix);
        }
    }
}
