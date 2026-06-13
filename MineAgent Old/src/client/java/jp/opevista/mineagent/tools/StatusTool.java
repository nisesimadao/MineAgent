package jp.opevista.mineagent.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;

public final class StatusTool implements Tool {
    @Override
    public String name() {
        return "get_status";
    }

    @Override
    public String description() {
        return "Return current client player status.";
    }

    @Override
    public JsonObject schema() {
        return new JsonObject();
    }

    @Override
    public ToolResult execute(JsonObject args, ToolExecutionContext context) {
        LocalPlayer player = context.client().player;
        JsonObject data = new JsonObject();
        if (player == null) {
            return ToolResult.ok("client player is not in-world", data);
        }
        JsonArray pos = new JsonArray();
        pos.add(player.getX());
        pos.add(player.getY());
        pos.add(player.getZ());
        data.add("position", pos);
        data.addProperty("yaw", player.getYRot());
        data.addProperty("pitch", player.getXRot());
        data.addProperty("health", player.getHealth());
        data.addProperty("food", player.getFoodData().getFoodLevel());
        data.addProperty("dimension", player.level().dimension().identifier().toString());
        data.addProperty("name", player.getName().getString());
        return ToolResult.ok("status collected", data);
    }
}
