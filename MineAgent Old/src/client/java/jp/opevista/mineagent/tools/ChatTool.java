package jp.opevista.mineagent.tools;

import com.google.gson.JsonObject;
import jp.opevista.mineagent.util.JsonUtil;

public final class ChatTool implements Tool {
    @Override
    public String name() {
        return "send_chat";
    }

    @Override
    public String description() {
        return "Send a normal chat message.";
    }

    @Override
    public JsonObject schema() {
        return new JsonObject();
    }

    @Override
    public ToolResult execute(JsonObject args, ToolExecutionContext context) {
        String message = JsonUtil.string(args, "message", "");
        if (message.isBlank()) {
            return ToolResult.fail("message is required");
        }
        if (context.client().player == null) {
            return ToolResult.fail("client player is not in-world");
        }
        context.client().execute(() -> context.client().player.connection.sendChat(message));
        return ToolResult.ok("chat queued", new JsonObject());
    }
}
