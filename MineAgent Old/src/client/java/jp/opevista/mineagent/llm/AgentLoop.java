package jp.opevista.mineagent.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jp.opevista.mineagent.MineAgentClient;
import jp.opevista.mineagent.task.AgentTask;
import jp.opevista.mineagent.task.TaskManager;
import jp.opevista.mineagent.task.TaskState;
import jp.opevista.mineagent.tools.ToolRegistry;
import jp.opevista.mineagent.util.JsonUtil;
import net.minecraft.client.Minecraft;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AgentLoop {
    private final TaskManager taskManager;
    private final ToolRegistry toolRegistry;
    private final AtomicBoolean inFlight = new AtomicBoolean(false);
    private LlmClient llmClient;
    private int tickCounter;

    public AgentLoop(TaskManager taskManager, ToolRegistry toolRegistry, LlmClient llmClient) {
        this.taskManager = taskManager;
        this.toolRegistry = toolRegistry;
        this.llmClient = llmClient;
    }

    public void setLlmClient(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public void tick(Minecraft client) {
        MineAgentClient mod = MineAgentClient.get();
        if (mod == null || !mod.config().agentLoop.enabled) {
            return;
        }
        tickCounter++;
        if (tickCounter % Math.max(1, mod.config().agentLoop.tickInterval) != 0) {
            return;
        }
        AgentTask task = taskManager.currentTask();
        if (task == null || task.state() != TaskState.RUNNING || inFlight.get()) {
            return;
        }
        if (task.step() >= mod.config().agentLoop.maxSteps) {
            task.state(TaskState.FAILED);
            taskManager.log("task failed: max steps reached");
            return;
        }
        int step = task.nextStep();
        inFlight.set(true);
        String observation = new AgentPromptBuilder(mod).build(task);
        CompletableFuture
                .supplyAsync(() -> callLlm(task, observation))
                .whenComplete((response, error) -> client.execute(() -> {
                    try {
                        if (error != null) {
                            task.state(TaskState.FAILED);
                            taskManager.log("llm error step=" + step + " " + error.getMessage());
                        } else {
                            applyResponse(task, response.rawJson());
                        }
                    } finally {
                        inFlight.set(false);
                    }
                }));
    }

    private LlmResponse callLlm(AgentTask task, String observation) {
        try {
            return llmClient.complete(new LlmRequest(AgentPromptBuilder.SYSTEM_PROMPT, task.instruction(), observation));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void applyResponse(AgentTask task, String raw) {
        taskManager.log("llm raw=" + raw);
        JsonObject json = parseJsonObject(raw);
        JsonArray actions = json.has("actions") && json.get("actions").isJsonArray() ? json.getAsJsonArray("actions") : new JsonArray();
        actions.forEach(element -> {
            JsonObject action = element.getAsJsonObject();
            String tool = JsonUtil.string(action, "tool", "");
            JsonObject args = JsonUtil.object(action, "args");
            if (!tool.isBlank()) {
                toolRegistry.execute(tool, args);
            }
        });
        String status = JsonUtil.string(json, "status", "done");
        if ("continue".equalsIgnoreCase(status)) {
            task.state(TaskState.RUNNING);
        } else if ("failed".equalsIgnoreCase(status)) {
            task.state(TaskState.FAILED);
        } else {
            task.state(TaskState.DONE);
        }
        taskManager.log("task " + task.id() + " state=" + task.state());
    }

    private JsonObject parseJsonObject(String raw) {
        try {
            String text = raw == null ? "{}" : raw.trim();
            int start = text.indexOf('{');
            int end = text.lastIndexOf('}');
            if (start >= 0 && end >= start) {
                text = text.substring(start, end + 1);
            }
            return JsonParser.parseString(text).getAsJsonObject();
        } catch (Exception e) {
            JsonObject failed = new JsonObject();
            failed.addProperty("status", "failed");
            failed.add("actions", new JsonArray());
            return failed;
        }
    }
}
