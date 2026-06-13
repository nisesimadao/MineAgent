package jp.opevista.mineagent.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jp.opevista.mineagent.config.MineAgentConfig;
import jp.opevista.mineagent.util.JsonUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class OpenAiCompatibleClient implements LlmClient {
    private final MineAgentConfig config;
    private final HttpClient httpClient;

    public OpenAiCompatibleClient(MineAgentConfig config) {
        this.config = config;
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(config.llm.timeoutMs)).build();
    }

    @Override
    public LlmResponse complete(LlmRequest request) throws Exception {
        if (!config.llm.enabled) {
            return new MockLlmClient().complete(request);
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("model", config.llm.model);
        payload.addProperty("temperature", config.llm.temperature);
        payload.addProperty("max_tokens", config.llm.maxOutputTokens);
        JsonArray messages = new JsonArray();
        messages.add(message("system", request.systemPrompt()));
        messages.add(message("user", request.observationJson()));
        payload.add("messages", messages);

        String endpoint = config.llm.baseUrl.replaceAll("/+$", "") + "/chat/completions";
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofMillis(config.llm.timeoutMs))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.GSON.toJson(payload)));
        if (!config.llm.apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + config.llm.apiKey);
        }
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("LLM HTTP " + response.statusCode());
        }
        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray choices = root.getAsJsonArray("choices");
        String content = choices.get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString();
        return new LlmResponse(content);
    }

    private JsonObject message(String role, String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content);
        return message;
    }
}
