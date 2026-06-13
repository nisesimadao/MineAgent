package jp.opevista.mineagent.web;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import jp.opevista.mineagent.MineAgentClient;
import jp.opevista.mineagent.util.JsonUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public final class WebServer {
    private final MineAgentClient mod;
    private HttpServer server;

    public WebServer(MineAgentClient mod) {
        this.mod = mod;
    }

    public void start(String host, int port) {
        try {
            server = HttpServer.create(new InetSocketAddress(host, port), 0);
            server.createContext("/", exchange -> WebResponse.html(exchange, WebUiAssets.html()));
            server.createContext("/api/status", exchange -> WebResponse.json(exchange, 200, status()));
            server.createContext("/api/instruct", exchange -> {
                JsonObject body = readBody(exchange);
                String user = JsonUtil.string(body, "user", "webui");
                String instruction = JsonUtil.string(body, "instruction", "");
                mod.taskManager().startTask("webui", user, instruction);
                JsonObject ok = new JsonObject();
                ok.addProperty("ok", true);
                WebResponse.json(exchange, 200, JsonUtil.GSON.toJson(ok));
            });
            server.createContext("/api/stop", exchange -> {
                mod.taskManager().stopCurrent("webui");
                JsonObject ok = new JsonObject();
                ok.addProperty("ok", true);
                WebResponse.json(exchange, 200, JsonUtil.GSON.toJson(ok));
            });
            server.createContext("/api/logs", exchange -> {
                JsonObject logs = new JsonObject();
                logs.add("logs", JsonUtil.GSON.toJsonTree(mod.taskManager().recentLogs(100)));
                WebResponse.json(exchange, 200, JsonUtil.GSON.toJson(logs));
            });
            server.createContext("/api/packets", exchange -> {
                String query = exchange.getRequestURI().getRawQuery();
                Query q = Query.parse(query);
                JsonObject packets = new JsonObject();
                packets.add("packets", mod.packetGateway().recentJson(q.get("direction", "BOTH"), q.getInt("limit", 100), q.get("filter", "")));
                WebResponse.json(exchange, 200, JsonUtil.GSON.toJson(packets));
            });
            server.createContext("/api/packets/send", exchange -> {
                JsonObject result = mod.toolRegistry().execute("packet_send_wrapped", readBody(exchange)).data();
                WebResponse.json(exchange, 200, JsonUtil.GSON.toJson(result));
            });
            server.createContext("/api/custom-payload/send", exchange -> {
                JsonObject result = mod.toolRegistry().execute("packet_send_custom_payload", readBody(exchange)).data();
                WebResponse.json(exchange, 200, JsonUtil.GSON.toJson(result));
            });
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            mod.taskManager().log("WebUI started at http://" + host + ":" + port);
        } catch (IOException e) {
            mod.taskManager().log("WebUI failed to start: " + e.getMessage());
        }
    }

    private String status() {
        JsonObject root = new JsonObject();
        root.addProperty("enabled", mod.config().enabled);
        root.addProperty("statusText", mod.taskManager().statusText());
        if (mod.taskManager().currentTask() != null) {
            JsonObject task = new JsonObject();
            task.addProperty("id", mod.taskManager().currentTask().id());
            task.addProperty("instruction", mod.taskManager().currentTask().instruction());
            task.addProperty("state", mod.taskManager().currentTask().state().name().toLowerCase());
            task.addProperty("step", mod.taskManager().currentTask().step());
            root.add("currentTask", task);
        }
        root.add("tools", mod.toolRegistry().listTools());
        return JsonUtil.GSON.toJson(root);
    }

    private JsonObject readBody(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return JsonUtil.parseObjectOrEmpty(body);
    }

    private record Query(java.util.Map<String, String> values) {
        static Query parse(String raw) {
            java.util.Map<String, String> values = new java.util.HashMap<>();
            if (raw != null && !raw.isBlank()) {
                for (String pair : raw.split("&")) {
                    String[] parts = pair.split("=", 2);
                    values.put(decode(parts[0]), parts.length > 1 ? decode(parts[1]) : "");
                }
            }
            return new Query(values);
        }

        String get(String name, String fallback) {
            return values.getOrDefault(name, fallback);
        }

        int getInt(String name, int fallback) {
            try {
                return Integer.parseInt(values.getOrDefault(name, String.valueOf(fallback)));
            } catch (NumberFormatException e) {
                return fallback;
            }
        }

        private static String decode(String value) {
            return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
        }
    }
}
