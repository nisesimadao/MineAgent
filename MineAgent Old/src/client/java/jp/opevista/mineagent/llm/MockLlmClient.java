package jp.opevista.mineagent.llm;

public final class MockLlmClient implements LlmClient {
    @Override
    public LlmResponse complete(LlmRequest request) {
        String instruction = request.instruction() == null ? "" : request.instruction().toLowerCase();
        if (instruction.contains("status") || instruction.contains("状態")) {
            return new LlmResponse("""
                    {"status":"done","message":"status requested","actions":[{"tool":"get_status","args":{}}]}""");
        }
        if (instruction.contains("inventory") || instruction.contains("インベントリ")) {
            return new LlmResponse("""
                    {"status":"done","message":"inventory requested","actions":[{"tool":"get_inventory","args":{}}]}""");
        }
        if (instruction.contains("hello")) {
            return new LlmResponse("""
                    {"status":"done","message":"hello","actions":[{"tool":"send_chat","args":{"message":"hello from MineAgent mock"}}]}""");
        }
        return new LlmResponse("""
                {"status":"done","message":"mock fallback","actions":[{"tool":"send_chat","args":{"message":"MineAgent mock response"}}]}""");
    }
}
