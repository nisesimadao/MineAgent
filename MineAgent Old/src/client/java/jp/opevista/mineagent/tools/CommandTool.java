package jp.opevista.mineagent.tools;

import com.google.gson.JsonObject;
import jp.opevista.mineagent.util.JsonUtil;

public final class CommandTool implements Tool {
    @Override
    public String name() {
        return "run_command";
    }

    @Override
    public String description() {
        return "Send a server command from the client.";
    }

    @Override
    public JsonObject schema() {
        return new JsonObject();
    }

    @Override
    public ToolResult execute(JsonObject args, ToolExecutionContext context) {
        String command = JsonUtil.string(args, "command", "");
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        if (command.isBlank()) {
            return ToolResult.fail("command is required");
        }
        if (!context.mod().config().permissions.allowCommands) {
            return ToolResult.fail("commands are disabled by config");
        }
        if (context.client().player == null) {
            return ToolResult.fail("client player is not in-world");
        }
        String finalCommand = command;
        context.client().execute(() -> context.client().player.connection.sendCommand(finalCommand));
        return ToolResult.ok("command queued", new JsonObject());
    }
}
