package jp.opevista.mineagent.tools;

import com.google.gson.JsonObject;

public record ToolResult(boolean ok, String message, JsonObject data) {
    public static ToolResult ok(String message, JsonObject data) {
        return new ToolResult(true, message, data == null ? new JsonObject() : data);
    }

    public static ToolResult fail(String message) {
        return new ToolResult(false, message, new JsonObject());
    }
}
