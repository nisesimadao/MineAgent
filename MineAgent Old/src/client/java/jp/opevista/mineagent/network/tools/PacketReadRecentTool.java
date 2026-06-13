package jp.opevista.mineagent.network.tools;

import com.google.gson.JsonObject;
import jp.opevista.mineagent.tools.Tool;
import jp.opevista.mineagent.tools.ToolExecutionContext;
import jp.opevista.mineagent.tools.ToolResult;
import jp.opevista.mineagent.util.JsonUtil;

public final class PacketReadRecentTool implements Tool {
    @Override
    public String name() {
        return "packet_read_recent";
    }

    @Override
    public String description() {
        return "Read recent summarized packet events.";
    }

    @Override
    public JsonObject schema() {
        return new JsonObject();
    }

    @Override
    public ToolResult execute(JsonObject args, ToolExecutionContext context) {
        if (!context.mod().config().permissions.allowPacketRead) {
            return ToolResult.fail("packet read is disabled by config");
        }
        String direction = JsonUtil.string(args, "direction", "BOTH");
        int limit = JsonUtil.integer(args, "limit", 20);
        String filter = JsonUtil.string(args, "filter", "");
        JsonObject data = new JsonObject();
        data.add("packets", context.mod().packetGateway().recentJson(direction, limit, filter));
        return ToolResult.ok("packet events collected", data);
    }
}
