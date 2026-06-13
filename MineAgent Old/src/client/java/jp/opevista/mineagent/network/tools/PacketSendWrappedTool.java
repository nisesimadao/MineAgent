package jp.opevista.mineagent.network.tools;

import com.google.gson.JsonObject;
import jp.opevista.mineagent.network.PacketDirection;
import jp.opevista.mineagent.tools.Tool;
import jp.opevista.mineagent.tools.ToolExecutionContext;
import jp.opevista.mineagent.tools.ToolResult;
import jp.opevista.mineagent.util.JsonUtil;

public final class PacketSendWrappedTool implements Tool {
    @Override
    public String name() {
        return "packet_send_wrapped";
    }

    @Override
    public String description() {
        return "Send supported wrapped packets: chat_message and command.";
    }

    @Override
    public JsonObject schema() {
        return new JsonObject();
    }

    @Override
    public ToolResult execute(JsonObject args, ToolExecutionContext context) {
        if (!context.mod().config().permissions.allowPacketSend) {
            return ToolResult.fail("packet send is disabled by config");
        }
        String type = JsonUtil.string(args, "type", "");
        JsonObject wrappedArgs = JsonUtil.object(args, "args");
        if (context.client().player == null) {
            return ToolResult.fail("client player is not in-world");
        }
        if ("chat_message".equals(type)) {
            String message = JsonUtil.string(wrappedArgs, "message", "");
            context.client().execute(() -> context.client().player.connection.sendChat(message));
            context.mod().packetGateway().log(PacketDirection.C2S, "wrapped chat_message: " + message);
            return ToolResult.ok("wrapped chat_message queued", new JsonObject());
        }
        if ("command".equals(type)) {
            String command = JsonUtil.string(wrappedArgs, "command", "");
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            String finalCommand = command;
            context.client().execute(() -> context.client().player.connection.sendCommand(finalCommand));
            context.mod().packetGateway().log(PacketDirection.C2S, "wrapped command: " + command);
            return ToolResult.ok("wrapped command queued", new JsonObject());
        }
        return ToolResult.fail("wrapped packet type is not implemented in v0.1: " + type);
    }
}
