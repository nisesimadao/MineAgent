package jp.opevista.mineagent;

import jp.opevista.mineagent.chat.ChatInstructionParser;
import jp.opevista.mineagent.command.MineAgentClientCommands;
import jp.opevista.mineagent.config.ConfigManager;
import jp.opevista.mineagent.config.MineAgentConfig;
import jp.opevista.mineagent.config.UserRegistry;
import jp.opevista.mineagent.llm.AgentLoop;
import jp.opevista.mineagent.llm.MockLlmClient;
import jp.opevista.mineagent.llm.OpenAiCompatibleClient;
import jp.opevista.mineagent.network.PacketGateway;
import jp.opevista.mineagent.task.TaskManager;
import jp.opevista.mineagent.tools.ToolRegistry;
import jp.opevista.mineagent.web.WebServer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public final class MineAgentClient implements ClientModInitializer {
    public static final String MOD_ID = "mineagent";

    private static MineAgentClient instance;

    private ConfigManager configManager;
    private UserRegistry userRegistry;
    private TaskManager taskManager;
    private PacketGateway packetGateway;
    private ToolRegistry toolRegistry;
    private AgentLoop agentLoop;
    private WebServer webServer;
    private ChatInstructionParser chatParser;

    public static MineAgentClient get() {
        return instance;
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        configManager = new ConfigManager();
        MineAgentConfig config = configManager.load();
        userRegistry = new UserRegistry(configManager.configDir());
        userRegistry.load();
        taskManager = new TaskManager();
        packetGateway = new PacketGateway(config);
        toolRegistry = new ToolRegistry(this);
        agentLoop = new AgentLoop(
                taskManager,
                toolRegistry,
                "mock".equalsIgnoreCase(config.llm.provider) ? new MockLlmClient() : new OpenAiCompatibleClient(config)
        );
        chatParser = new ChatInstructionParser();

        MineAgentClientCommands.register(this);
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);

        if (config.webUi.enabled) {
            webServer = new WebServer(this);
            webServer.start(config.webUi.host, config.webUi.port);
        }

        taskManager.log("MineAgent initialized");
    }

    private void tick(Minecraft client) {
        agentLoop.tick(client);
    }

    public void reload() {
        MineAgentConfig config = configManager.load();
        userRegistry.load();
        packetGateway.applyConfig(config);
        agentLoop.setLlmClient("mock".equalsIgnoreCase(config.llm.provider) ? new MockLlmClient() : new OpenAiCompatibleClient(config));
        taskManager.log("Config reloaded");
    }

    public ConfigManager configManager() {
        return configManager;
    }

    public MineAgentConfig config() {
        return configManager.config();
    }

    public UserRegistry userRegistry() {
        return userRegistry;
    }

    public TaskManager taskManager() {
        return taskManager;
    }

    public PacketGateway packetGateway() {
        return packetGateway;
    }

    public ToolRegistry toolRegistry() {
        return toolRegistry;
    }

    public AgentLoop agentLoop() {
        return agentLoop;
    }

    public ChatInstructionParser chatParser() {
        return chatParser;
    }
}
