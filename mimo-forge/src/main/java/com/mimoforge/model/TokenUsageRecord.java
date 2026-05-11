package com.mimoforge.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Token 用量追踪记录 — 用于实时监控消耗并生成报告
 */
@Table("token_usage")
public record TokenUsageRecord(
        @Id Long id,
        String agentId,
        String agentType,
        String model,
        String taskType,
        Integer promptTokens,
        Integer completionTokens,
        Integer reasoningTokens,
        Integer totalTokens,
        Double estimatedCostUsd,
        String requestId,
        Instant createdAt
) {
    public static TokenUsageRecord of(String agentId, String agentType, String model,
                                       String taskType, ChatResponse.Usage usage) {
        int total = usage.totalTokens() != null ? usage.totalTokens() : 0;
        double cost = calculateCost(model, usage);
        return new TokenUsageRecord(null, agentId, agentType, model, taskType,
                usage.promptTokens(), usage.completionTokens(),
                usage.reasoningTokens(), total, cost, null, Instant.now());
    }

    private static double calculateCost(String model, ChatResponse.Usage usage) {
        int prompt = usage.promptTokens() != null ? usage.promptTokens() : 0;
        int completion = usage.completionTokens() != null ? usage.completionTokens() : 0;
        // MiMo API 定价估算（实际以官方为准）
        return (prompt * 0.002 + completion * 0.006) / 1000.0;
    }
}
