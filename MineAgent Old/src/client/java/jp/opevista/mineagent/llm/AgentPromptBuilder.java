package jp.opevista.mineagent.llm;

import com.google.gson.JsonObject;
import jp.opevista.mineagent.MineAgentClient;
import jp.opevista.mineagent.task.AgentTask;
import jp.opevista.mineagent.tools.ToolResult;
import jp.opevista.mineagent.util.JsonUtil;
import net.minecraft.client.Minecraft;

public final class AgentPromptBuilder {
    public static final String SYSTEM_PROMPT = """
            You are MineAgent, an expert Minecraft automation agent.
            Current Goal: Execute instructions from the user efficiently and safely.
            Rules:
            1. Response must be in STRICT JSON only. No markdown, no pre-amble, no post-amble.
            2. Language: Use Japanese for 'message' and chat content.
            3. Thinking: Analyze the 'task' and current 'player' status, then choose the best 'actions'.
            4. Efficiency: If a task is finished, set status to 'done'. If stuck, set status to 'failed'.
            5. Tools: Use availableTools only. Prefer Baritone for movement and get_status for observation.
            
            JSON Schema:
            {"status":"continue|done|failed","message":"(Japanese) explanation of what you are doing","actions":[{"tool":"tool_name","args":{}}]}
            """;

    private final MineAgentClient mod;

    public AgentPromptBuilder(MineAgentClient mod) {
        this.mod = mod;
    }

    public String build(AgentTask task) {
        JsonObject root = new JsonObject();
        JsonObject agent = new JsonObject();
        agent.addProperty("name", mod.config().agentName);
        agent.addProperty("trustedEnvironment", true);
        root.add("agent", agent);

        JsonObject taskJson = new JsonObject();
        taskJson.addProperty("id", task.id());
        taskJson.addProperty("instruction", task.instruction());
        taskJson.addProperty("step", task.step());
        taskJson.addProperty("maxSteps", mod.config().agentLoop.maxSteps);
        taskJson.addProperty("state", task.state().name().toLowerCase());
        root.add("task", taskJson);

        ToolResult status = mod.toolRegistry().execute("get_status", new JsonObject());
        root.add("player", status.data());
        root.add("availableTools", mod.toolRegistry().listTools());
        root.add("recentPacketEvents", mod.packetGateway().recentJson("BOTH", 20, ""));
        JsonObject client = new JsonObject();
        Minecraft minecraft = Minecraft.getInstance();
        client.addProperty("inWorld", minecraft.player != null);
        root.add("client", client);
        return JsonUtil.GSON.toJson(root);
    }
}
