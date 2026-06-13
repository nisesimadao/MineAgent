package jp.opevista.mineagent.bridge;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public final class MineAgentHttpServer {
    private final String host;
    private final int port;
    private HttpServer server;

    public MineAgentHttpServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(host, port), 0);
            server.createContext("/health", exchange -> JsonHttp.write(exchange, 200, health()));
            server.createContext("/tool", this::handleTool);
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            System.out.println("[MineAgent] HTTP bridge listening on http://" + host + ":" + port);
        } catch (IOException e) {
            System.err.println("[MineAgent] Failed to start HTTP bridge: " + e.getMessage());
        }
    }

    private JsonObject health() {
        JsonObject json = new JsonObject();
        json.addProperty("ok", true);
        json.addProperty("name", "MineAgent Fabric bridge");
        json.addProperty("port", port);
        return json;
    }

    private void handleTool(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonHttp.writeError(exchange, 405, "POST required");
            return;
        }

        JsonObject body = JsonHttp.readObject(exchange);
        String name = JsonHttp.string(body, "name", "");
        JsonObject args = JsonHttp.object(body, "arguments");

        MineAgentTools.execute(name, args).whenComplete((result, error) -> {
            try {
                if (error != null) {
                    JsonHttp.writeError(exchange, 500, error.getMessage());
                } else {
                    JsonHttp.write(exchange, 200, result);
                }
            } catch (IOException e) {
                System.err.println("[MineAgent] Failed to write response: " + e.getMessage());
            }
        });
    }
}
