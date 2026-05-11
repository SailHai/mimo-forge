package com.mimoforge.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mimoforge.model.ChatRequest;
import com.mimoforge.model.ChatResponse;
import com.mimoforge.model.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Xiaomi MiMo API 统一客户端
 *
 * 封装 MiMo V2.5 全系列模型调用：
 * - MiMo-Max：旗舰推理模型，复杂长链推理、架构设计、代码审查
 * - MiMo-VL：多模态模型，图像理解、UI 分析
 * - MiMo-TTS：语音合成
 * - MiMo-Lite：轻量模型，快速补全、简单问答
 *
 * 支持同步调用、SSE 流式调用、Tool Calling。
 *
 * @author Senior AI Engineer
 */
@Component
public class MiMoApiClient {

    private static final Logger log = LoggerFactory.getLogger(MiMoApiClient.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String primaryModel;
    private final String visionModel;
    private final String liteModel;
    private final int maxRetries;

    public MiMoApiClient(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${mimo.api.base-url}") String baseUrl,
            @Value("${mimo.api.api-key}") String apiKey,
            @Value("${mimo.api.primary-model}") String primaryModel,
            @Value("${mimo.api.vision-model}") String visionModel,
            @Value("${mimo.api.lite-model}") String liteModel,
            @Value("${mimo.api.max-retries}") int maxRetries
    ) {
        this.objectMapper = objectMapper;
        this.primaryModel = primaryModel;
        this.visionModel = visionModel;
        this.liteModel = liteModel;
        this.maxRetries = maxRetries;
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    // ═══════════════════════════════════════════
    //  核心调用方法
    // ═══════════════════════════════════════════

    /**
     * 同步调用 MiMo-Max 旗舰推理模型
     */
    public Mono<ChatResponse> chat(ChatRequest request) {
        log.debug("MiMo API 调用: model={}, messages={}", request.model(), request.messages().size());
        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(2))
                        .filter(ex -> ex instanceof WebClientResponseException.TooManyRequests)
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                .doOnSuccess(resp -> {
                    if (resp.usage() != null) {
                        log.info("Token 消耗: prompt={}, completion={}, reasoning={}, total={}",
                                resp.usage().promptTokens(), resp.usage().completionTokens(),
                                resp.usage().reasoningTokens(), resp.usage().totalTokens());
                    }
                })
                .doOnError(e -> log.error("MiMo API 调用失败: {}", e.getMessage()));
    }

    /**
     * SSE 流式调用 — 用于实时输出场景
     */
    public Flux<String> chatStream(ChatRequest request) {
        ChatRequest streamReq = new ChatRequest(
                request.model(), request.messages(), request.maxTokens(),
                request.temperature(), request.topP(), null, null,
                true, request.tools(), request.toolChoice(), null);

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(streamReq)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> !line.equals("[DONE]"));
    }

    /**
     * 使用 MiMo-Max 执行复杂推理任务
     */
    public Mono<ChatResponse> reason(String systemPrompt, String userPrompt, int maxTokens) {
        var request = ChatRequest.builder()
                .model(primaryModel)
                .messages(List.of(
                        ChatMessage.system(systemPrompt),
                        ChatMessage.user(userPrompt)))
                .maxTokens(maxTokens)
                .temperature(0.3)  // 推理任务用低温度
                .build();
        return chat(request);
    }

    /**
     * 使用 MiMo-Lite 执行快速任务（补全、分类等）
     */
    public Mono<ChatResponse> quickChat(String systemPrompt, String userPrompt) {
        var request = ChatRequest.builder()
                .model(liteModel)
                .messages(List.of(
                        ChatMessage.system(systemPrompt),
                        ChatMessage.user(userPrompt)))
                .maxTokens(2048)
                .temperature(0.5)
                .build();
        return chat(request);
    }

    /**
     * Tool Calling 调用 — Agent 使用工具时调用
     */
    public Mono<ChatResponse> chatWithTools(List<ChatMessage> messages,
                                             List<Map<String, Object>> tools) {
        var request = ChatRequest.builder()
                .model(primaryModel)
                .messages(messages)
                .maxTokens(8192)
                .tools(tools)
                .temperature(0.2)
                .build();
        return chat(request);
    }

    /**
     * 使用 MiMo-VL 进行多模态分析
     */
    public Mono<ChatResponse> visionAnalyze(String imageUrl, String prompt) {
        var request = ChatRequest.builder()
                .model(visionModel)
                .messages(List.of(
                        ChatMessage.system("你是一个多模态分析专家。"),
                        ChatMessage.user(prompt)))
                .maxTokens(4096)
                .build();
        return chat(request);
    }

    // ═══════════════════════════════════════════
    //  Model 别名
    // ═══════════════════════════════════════════

    public String getPrimaryModel() { return primaryModel; }
    public String getVisionModel() { return visionModel; }
    public String getLiteModel() { return liteModel; }
}
