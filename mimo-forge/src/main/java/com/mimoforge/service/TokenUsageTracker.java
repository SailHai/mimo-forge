package com.mimoforge.service;

import com.mimoforge.model.ChatResponse;
import com.mimoforge.model.TokenUsageRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Token 用量实时追踪器
 *
 * 功能：
 * - 实时记录每次 API 调用的 Token 消耗
 * - 按 Agent 类型、模型、日期维度聚合统计
 * - 日/月消耗预警
 * - 生成消耗报告
 *
 * @author Senior AI Engineer
 */
@Service
public class TokenUsageTracker {

    private static final Logger log = LoggerFactory.getLogger(TokenUsageTracker.class);
    private static final String KEY_PREFIX = "mimoforge:token:";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReactiveRedisTemplate<String, String> redis;
    private final long dailyAlertThreshold;
    private final long monthlyAlertThreshold;

    public TokenUsageTracker(
            ReactiveRedisTemplate<String, String> redis,
            @Value("${token-usage.daily-alert-threshold}") long dailyAlertThreshold,
            @Value("${token-usage.monthly-alert-threshold}") long monthlyAlertThreshold
    ) {
        this.redis = redis;
        this.dailyAlertThreshold = dailyAlertThreshold;
        this.monthlyAlertThreshold = monthlyAlertThreshold;
    }

    /**
     * 记录 Token 消耗
     */
    public void record(String agentId, String agentType, String model,
                        String taskType, ChatResponse.Usage usage) {
        if (usage == null) return;

        int total = usage.totalTokens() != null ? usage.totalTokens() : 0;
        int prompt = usage.promptTokens() != null ? usage.promptTokens() : 0;
        int completion = usage.completionTokens() != null ? usage.completionTokens() : 0;
        int reasoning = usage.reasoningTokens() != null ? usage.reasoningTokens() : 0;

        String today = LocalDate.now().format(DATE_FMT);
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        // 原子递增各维度计数器
        redis.opsForValue().increment(KEY_PREFIX + "daily:" + today, total).subscribe();
        redis.opsForValue().increment(KEY_PREFIX + "monthly:" + month, total).subscribe();
        redis.opsForValue().increment(KEY_PREFIX + "agent:" + agentType + ":" + today, total).subscribe();
        redis.opsForValue().increment(KEY_PREFIX + "model:" + model + ":" + today, total).subscribe();

        // 设置过期时间（保留 90 天）
        redis.expire(KEY_PREFIX + "daily:" + today, Duration.ofDays(90)).subscribe();
        redis.expire(KEY_PREFIX + "monthly:" + month, Duration.ofDays(365)).subscribe();

        log.debug("Token 记录: agent={}, model={}, prompt={}, completion={}, reasoning={}, total={}",
                agentId, model, prompt, completion, reasoning, total);
    }

    /**
     * 获取今日消耗
     */
    public Mono<Long> getDailyUsage() {
        String key = KEY_PREFIX + "daily:" + LocalDate.now().format(DATE_FMT);
        return redis.opsForValue().get(key).map(Long::parseLong).defaultIfEmpty(0L);
    }

    /**
     * 获取本月消耗
     */
    public Mono<Long> getMonthlyUsage() {
        String key = KEY_PREFIX + "monthly:" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return redis.opsForValue().get(key).map(Long::parseLong).defaultIfEmpty(0L);
    }

    /**
     * 定时检查消耗预警 — 每 10 分钟
     */
    @Scheduled(fixedRate = 600000)
    public void checkAlerts() {
        getDailyUsage().subscribe(daily -> {
            if (daily > dailyAlertThreshold) {
                log.warn("⚠️ Token 日消耗预警: {} / {} (阈值 {})",
                        daily, dailyAlertThreshold, String.format("%.1f%%", daily * 100.0 / dailyAlertThreshold));
            }
        });
        getMonthlyUsage().subscribe(monthly -> {
            if (monthly > monthlyAlertThreshold) {
                log.warn("🚨 Token 月消耗预警: {} / {} (阈值 {})",
                        monthly, monthlyAlertThreshold, String.format("%.1f%%", monthly * 100.0 / monthlyAlertThreshold));
            }
        });
    }

    /**
     * 获取消耗总览
     */
    public Mono<Map<String, Object>> getUsageSummary() {
        return Mono.zip(getDailyUsage(), getMonthlyUsage())
                .map(tuple -> Map.<String, Object>of(
                        "daily_tokens", tuple.getT1(),
                        "monthly_tokens", tuple.getT2(),
                        "daily_alert_threshold", dailyAlertThreshold,
                        "monthly_alert_threshold", monthlyAlertThreshold,
                        "daily_usage_percent", String.format("%.2f%%", tuple.getT1() * 100.0 / dailyAlertThreshold),
                        "monthly_usage_percent", String.format("%.2f%%", tuple.getT2() * 100.0 / monthlyAlertThreshold)
                ));
    }
}
