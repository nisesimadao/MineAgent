package jp.opevista.mineagent.network.tools;

import com.google.gson.JsonObject;
import jp.opevista.mineagent.network.PacketDirection;
import jp.opevista.mineagent.tools.Tool;
import jp.opevista.mineagent.tools.ToolExecutionContext;
import jp.opevista.mineagent.tools.ToolResult;
import jp.opevista.mineagent.util.JsonUtil;

public final class PacketSendCustomPayloadTool implements Tool {
    @Override
    public String name() {
        return "packet_send_custom_payload";
    }

    @Override
    public String description() {
        return "Queue a MineAgent JSON custom payload event; raw 26.1 payload wiring is deferred.";
    }

    @Override
    public JsonObject schema() {
        return new JsonObject();
    }

    @Override
    public ToolResult execute(JsonObject args, ToolExecutionContext context) {
        if (!context.mod().config().permissions.allowCustomPayload) {
            return ToolResult.fail("custom payload is disabled by config");
        }
        String channel = JsonUtil.string(args, "channel", "mineagent:agent_to_server");
        JsonObject payload = JsonUtil.object(args, "payload");
        JsonObject logged = new JsonObject();
        logged.addProperty("channel", channel);
        logged.add("payload", payload);
        context.mod().packetGateway().log(PacketDirection.C2S, "custom_payload " + logged);
        return ToolResult.ok("custom payload logged for bridge channel", logged);
    }
}
