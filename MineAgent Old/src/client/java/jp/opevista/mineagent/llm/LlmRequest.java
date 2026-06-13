package jp.opevista.mineagent.llm;

public record LlmRequest(String systemPrompt, String instruction, String observationJson) {
}
