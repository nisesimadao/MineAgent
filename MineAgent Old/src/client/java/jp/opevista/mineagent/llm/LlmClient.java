package jp.opevista.mineagent.llm;

public interface LlmClient {
    LlmResponse complete(LlmRequest request) throws Exception;
}
