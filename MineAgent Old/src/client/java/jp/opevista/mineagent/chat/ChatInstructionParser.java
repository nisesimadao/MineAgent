package jp.opevista.mineagent.chat;

import java.util.Optional;

public final class ChatInstructionParser {
    public Optional<String> parse(String agentName, String message) {
        if (agentName == null || agentName.isBlank() || message == null) {
            return Optional.empty();
        }
        String trimmed = message.trim();
        String prefix = agentName.trim();
        if (!trimmed.startsWith(prefix)) {
            return Optional.empty();
        }
        if (trimmed.length() == prefix.length()) {
            return Optional.empty();
        }
        char next = trimmed.charAt(prefix.length());
        if (!Character.isWhitespace(next)) {
            return Optional.empty();
        }
        String instruction = trimmed.substring(prefix.length()).trim();
        return instruction.isEmpty() ? Optional.empty() : Optional.of(instruction);
    }
}
