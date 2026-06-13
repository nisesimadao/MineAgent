package jp.opevista.mineagent.tools;

import com.google.gson.JsonObject;

public interface Tool {
    String name();

    String description();

    JsonObject schema();

    ToolResult execute(JsonObject args, ToolExecutionContext context);
}
