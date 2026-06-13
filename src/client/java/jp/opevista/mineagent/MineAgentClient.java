package jp.opevista.mineagent;

import jp.opevista.mineagent.bridge.MineAgentHttpServer;
import jp.opevista.mineagent.bridge.ChatLog;
import net.fabricmc.api.ClientModInitializer;

public final class MineAgentClient implements ClientModInitializer {
    public static final String MOD_ID = "mineagent";
    public static final int DEFAULT_PORT = 17890;

    private MineAgentHttpServer server;

    @Override
    public void onInitializeClient() {
        ChatLog.register();
        server = new MineAgentHttpServer("127.0.0.1", DEFAULT_PORT);
        server.start();
    }
}
