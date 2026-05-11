package com.mimoforge.agent;

/**
 * Agent 定义 — 描述一个 Agent 的全部配置
 */
public record AgentDefinition(
        String type,
        String systemPrompt,
        String model,
        int maxTokens,
        double temperature,
        boolean enableTools
) {
    public static Builder builder(String type) {
        return new Builder(type);
    }

    public static class Builder {
        private final String type;
        private String systemPrompt = "";
        private String model = "MiMo-Max";
        private int maxTokens = 8192;
        private double temperature = 0.3;
        private boolean enableTools = false;

        Builder(String type) { this.type = type; }
        public Builder systemPrompt(String p) { this.systemPrompt = p; return this; }
        public Builder model(String m) { this.model = m; return this; }
        public Builder maxTokens(int t) { this.maxTokens = t; return this; }
        public Builder temperature(double t) { this.temperature = t; return this; }
        public Builder enableTools(boolean e) { this.enableTools = e; return this; }
        public AgentDefinition build() {
            return new AgentDefinition(type, systemPrompt, model, maxTokens, temperature, enableTools);
        }
    }
}
