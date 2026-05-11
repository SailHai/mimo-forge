package com.mimoforge.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * MiMo API Chat Completion 响应体
 */
public record ChatResponse(
        String id,
        String object,
        Long created,
        String model,
        List<Choice> choices,
        Usage usage
) {
    public record Choice(
            Integer index,
            ChatMessage message,
            @JsonProperty("finish_reason") String finishReason
    ) {}

    public record Usage(
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("completion_tokens") Integer completionTokens,
            @JsonProperty("total_tokens") Integer totalTokens,
            @JsonProperty("reasoning_tokens") Integer reasoningTokens
    ) {}

    /** 获取第一个回复的文本内容 */
    public String firstContent() {
        if (choices == null || choices.isEmpty()) return "";
        var msg = choices.get(0).message();
        return msg != null && msg.content() != null ? msg.content() : "";
    }
}
