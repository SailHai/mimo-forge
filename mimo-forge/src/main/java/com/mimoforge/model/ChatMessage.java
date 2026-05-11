package com.mimoforge.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MiMo Chat API 统一消息模型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessage(
        String role,          // system | user | assistant | tool
        String content,
        @JsonProperty("tool_calls") Object toolCalls,
        @JsonProperty("tool_call_id") String toolCallId
) {
    public static ChatMessage system(String content) {
        return new ChatMessage("system", content, null, null);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage("user", content, null, null);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage("assistant", content, null, null);
    }

    public static ChatMessage toolResult(String toolCallId, String content) {
        return new ChatMessage("tool", content, null, toolCallId);
    }
}
