package com.mimoforge.controller;

import com.mimoforge.agent.AgentOrchestrator;
import com.mimoforge.agent.AgentOrchestrator.PipelineType;
import com.mimoforge.rag.RagEngine;
import com.mimoforge.service.TokenUsageTracker;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * MiMo Forge REST API
 *
 * 提供 Agent 编排、RAG 查询、Token 监控等接口。
 * 支持 SSE 流式输出，适配前端实时展示。
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class AgentController {

    private final AgentOrchestrator orchestrator;
    private final RagEngine ragEngine;
    private final TokenUsageTracker tokenTracker;

    public AgentController(AgentOrchestrator orchestrator,
                           RagEngine ragEngine,
                           TokenUsageTracker tokenTracker) {
        this.orchestrator = orchestrator;
        this.ragEngine = ragEngine;
        this.tokenTracker = tokenTracker;
    }

    /**
     * 启动 Agent Pipeline
     * POST /api/v1/pipeline
     */
    @PostMapping("/pipeline")
    public Mono<AgentOrchestrator.PipelineResult> executePipeline(@RequestBody PipelineRequest request) {
        PipelineType type = PipelineType.valueOf(request.type());
        return orchestrator.executePipeline(type, request.requirement(), request.context());
    }

    /**
     * RAG 知识查询
     * POST /api/v1/rag/query
     */
    @PostMapping("/rag/query")
    public Mono<RagEngine.RagResponse> ragQuery(@RequestBody RagQueryRequest request) {
        return ragEngine.query(request.question(), request.collection(), request.filters());
    }

    /**
     * 索引文档到知识库
     * POST /api/v1/rag/index
     */
    @PostMapping("/rag/index")
    public Mono<RagEngine.IndexResult> indexDocument(@RequestBody RagEngine.Document document) {
        return ragEngine.indexDocument(document);
    }

    /**
     * Token 用量总览
     * GET /api/v1/token/usage
     */
    @GetMapping("/token/usage")
    public Mono<Map<String, Object>> getTokenUsage() {
        return tokenTracker.getUsageSummary();
    }

    /**
     * 健康检查
     * GET /api/v1/health
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "MiMo Forge",
                "version", "1.0.0",
                "engine", "Xiaomi MiMo V2.5"
        );
    }

    // ═══════════════════════════════════════════
    //  Request DTOs
    // ═══════════════════════════════════════════

    record PipelineRequest(String type, String requirement, Map<String, String> context) {}

    record RagQueryRequest(String question, String collection, Map<String, Object> filters) {}
}
