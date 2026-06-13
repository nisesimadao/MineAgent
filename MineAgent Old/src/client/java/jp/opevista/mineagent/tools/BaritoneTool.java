package jp.opevista.mineagent.tools;

import com.google.gson.JsonObject;
import jp.opevista.mineagent.util.JsonUtil;
import net.fabricmc.loader.api.FabricLoader;

public final class BaritoneTool implements Tool {
    @Override
    public String name() {
        return "baritone";
    }

    @Override
    public String description() {
        return "Send an optional Baritone command through local chat.";
    }

    @Override
    public JsonObject schema() {
        return new JsonObject();
    }

    @Override
    public ToolResult execute(JsonObject args, ToolExecutionContext context) {
        if (!context.mod().config().permissions.allowBaritone || !context.mod().config().baritone.enabled) {
            return ToolResult.fail("baritone is disabled by config");
        }
        if (!FabricLoader.getInstance().isModLoaded("baritone")) {
            return ToolResult.fail("baritone mod is not loaded");
        }
        String command = JsonUtil.string(args, "command", "");
        String prefix = context.mod().config().baritone.commandPrefix;
        if (!command.startsWith(prefix)) {
            command = prefix + command;
        }
        if (context.client().player == null) {
            return ToolResult.fail("client player is not in-world");
        }
        String finalCommand = command;
        context.client().execute(() -> context.client().player.connection.sendChat(finalCommand));
        return ToolResult.ok("baritone command queued", new JsonObject());
    }
}
