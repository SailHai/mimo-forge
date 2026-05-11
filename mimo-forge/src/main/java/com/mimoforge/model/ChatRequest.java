package com.mimoforge.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * MiMo API Chat Completion 请求体
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatRequest(
        String model,
        List<ChatMessage> messages,
        @JsonProperty("max_tokens") Integer maxTokens,
        Double temperature,
        @JsonProperty("top_p") Double topP,
        Double frequencyPenalty,
        Double presencePenalty,
        Boolean stream,
        List<Map<String, Object>> tools,
        @JsonProperty("tool_choice") Object toolChoice,
        @JsonProperty("response_format") Map<String, String> responseFormat
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String model;
        private List<ChatMessage> messages;
        private Integer maxTokens = 8192;
        private Double temperature = 0.7;
        private Double topP = 0.9;
        private boolean stream = false;
        private List<Map<String, Object>> tools;

        public Builder model(String model) { this.model = model; return this; }
        public Builder messages(List<ChatMessage> messages) { this.messages = messages; return this; }
        public Builder maxTokens(int maxTokens) { this.maxTokens = maxTokens; return this; }
        public Builder temperature(double temperature) { this.temperature = temperature; return this; }
        public Builder stream(boolean stream) { this.stream = stream; return this; }
        public Builder tools(List<Map<String, Object>> tools) { this.tools = tools; return this; }

        public ChatRequest build() {
            return new ChatRequest(model, messages, maxTokens, temperature, topP,
                    null, null, stream, tools, null, null);
        }
    }
}
