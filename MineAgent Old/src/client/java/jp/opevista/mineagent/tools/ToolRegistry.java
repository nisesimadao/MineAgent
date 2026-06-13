package jp.opevista.mineagent.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jp.opevista.mineagent.MineAgentClient;
import jp.opevista.mineagent.network.tools.PacketReadRecentTool;
import jp.opevista.mineagent.network.tools.PacketSendCustomPayloadTool;
import jp.opevista.mineagent.network.tools.PacketSendWrappedTool;
import net.minecraft.client.Minecraft;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ToolRegistry {
    private final MineAgentClient mod;
    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public ToolRegistry(MineAgentClient mod) {
        this.mod = mod;
        register(new StatusTool());
        register(new InventoryTool());
        register(new ChatTool());
        register(new CommandTool());
        register(new BaritoneTool());
        register(new PacketReadRecentTool());
        register(new PacketSendCustomPayloadTool());
        register(new PacketSendWrappedTool());
    }

    private void register(Tool tool) {
        tools.put(tool.name(), tool);
    }

    public ToolResult execute(String name, JsonObject args) {
        Tool tool = tools.get(name);
        if (tool == null) {
            return ToolResult.fail("unknown tool: " + name);
        }
        try {
            ToolResult result = tool.execute(args == null ? new JsonObject() : args, new ToolExecutionContext(mod, Minecraft.getInstance()));
            mod.taskManager().log("tool " + name + " ok=" + result.ok() + " message=" + result.message());
            return result;
        } catch (Exception e) {
            mod.taskManager().log("tool " + name + " error=" + e.getMessage());
            return ToolResult.fail(e.getMessage());
        }
    }

    public JsonArray listTools() {
        JsonArray array = new JsonArray();
        tools.values().forEach(tool -> {
            JsonObject json = new JsonObject();
            json.addProperty("name", tool.name());
            json.addProperty("description", tool.description());
            json.add("schema", tool.schema());
            array.add(json);
        });
        return array;
    }
}
