package com.mimoforge.agent;

import com.mimoforge.client.MiMoApiClient;
import com.mimoforge.model.ChatMessage;
import com.mimoforge.model.ChatResponse;
import com.mimoforge.service.TokenUsageTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 编排引擎 — MiMo Forge 核心
 *
 * 实现多 Agent 协作的 DAG（有向无环图）编排：
 *
 *   [需求分析] → [架构设计] → [代码生成] → [代码审查] → [测试生成] → [部署配置]
 *        ↓              ↓            ↓            ↓
 *   [知识检索]    [安全审计]    [文档生成]    [性能分析]
 *
 * 每个 Agent 节点可独立配置：
 * - 底层模型（Max/Lite，支持 A/B 测试）
 * - 系统 Prompt
 * - Token 预算
 * - 重试策略
 * - 超时设置
 *
 * @author Senior AI Engineer
 */
@Service
public class AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final MiMoApiClient mimoClient;
    private final TokenUsageTracker tokenTracker;
    private final Map<String, AgentDefinition> agentRegistry = new ConcurrentHashMap<>();
    private final int maxConcurrentAgents;
    private final int maxTokensPerTask;

    public AgentOrchestrator(
            MiMoApiClient mimoClient,
            TokenUsageTracker tokenTracker,
            @Value("${mimo.agent.max-concurrent-agents}") int maxConcurrentAgents,
            @Value("${mimo.agent.max-tokens-per-task}") int maxTokensPerTask
    ) {
        this.mimoClient = mimoClient;
        this.tokenTracker = tokenTracker;
        this.maxConcurrentAgents = maxConcurrentAgents;
        this.maxTokensPerTask = maxTokensPerTask;
        registerDefaultAgents();
    }

    // ═══════════════════════════════════════════
    //  Agent 注册
    // ═══════════════════════════════════════════

    private void registerDefaultAgents() {
        register(AgentDefinition.builder("REQUIREMENT_ANALYZER")
                .systemPrompt("""
                        你是一个资深需求分析师。你的任务是：
                        1. 解析用户的自然语言需求描述
                        2. 提取功能需求、非功能需求、约束条件
                        3. 输出结构化的需求文档（JSON 格式）
                        4. 识别模糊或矛盾的需求并提问澄清
                        """)
                .model("MiMo-Max")
                .maxTokens(8192)
                .temperature(0.3)
                .build());

        register(AgentDefinition.builder("ARCHITECTURE_DESIGNER")
                .systemPrompt("""
                        你是一个系统架构师。基于需求文档：
                        1. 设计系统架构（模块划分、接口定义、数据流）
                        2. 选择技术栈并给出选型理由
                        3. 评估性能瓶颈和扩展性
                        4. 输出架构设计文档（Mermaid 图 + 文字说明）
                        """)
                .model("MiMo-Max")
                .maxTokens(16384)
                .temperature(0.2)
                .build());

        register(AgentDefinition.builder("CODE_GENERATOR")
                .systemPrompt("""
                        你是一个高级全栈工程师。根据架构设计：
                        1. 生成生产级代码（Java/TypeScript/Python）
                        2. 遵循 SOLID 原则和项目编码规范
                        3. 包含完整的错误处理和日志
                        4. 代码可直接编译运行
                        """)
                .model("MiMo-Max")
                .maxTokens(32768)
                .temperature(0.1)
                .build());

        register(AgentDefinition.builder("CODE_REVIEWER")
                .systemPrompt("""
                        你是一个代码审查专家。审查代码：
                        1. 发现潜在 Bug、安全漏洞、性能问题
                        2. 检查是否遵循编码规范和最佳实践
                        3. 评估可读性、可维护性、可测试性
                        4. 输出评分和具体修改建议
                        """)
                .model("MiMo-Max")
                .maxTokens(8192)
                .temperature(0.2)
                .build());

        register(AgentDefinition.builder("TEST_ENGINEER")
                .systemPrompt("""
                        你是一个测试工程师。为代码生成：
                        1. 单元测试（JUnit 5 / Vitest）
                        2. 集成测试
                        3. 边界条件和异常场景测试
                        4. 测试覆盖率目标 > 80%
                        """)
                .model("MiMo-Max")
                .maxTokens(16384)
                .temperature(0.2)
                .build());

        register(AgentDefinition.builder("SECURITY_AUDITOR")
                .systemPrompt("""
                        你是一个安全审计专家。执行安全审查：
                        1. OWASP Top 10 检查
                        2. SQL 注入、XSS、CSRF 等常见漏洞扫描
                        3. 敏感信息泄露检查
                        4. 认证/授权机制评估
                        """)
                .model("MiMo-Max")
                .maxTokens(8192)
                .temperature(0.1)
                .build());

        log.info("注册 {} 个内置 Agent", agentRegistry.size());
    }

    public void register(AgentDefinition agent) {
        agentRegistry.put(agent.type(), agent);
    }

    // ═══════════════════════════════════════════
    //  编排执行
    // ═══════════════════════════════════════════

    /**
     * 执行完整的 Agent Pipeline
     *
     * @param pipelineType Pipeline 类型（FULL / QUICK / REVIEW_ONLY）
     * @param userRequirement 用户需求描述
     * @param context 上下文信息（已有代码、技术栈等）
     * @return Pipeline 执行结果
     */
    public Mono<PipelineResult> executePipeline(PipelineType pipelineType,
                                                  String userRequirement,
                                                  Map<String, String> context) {
        List<String> pipeline = pipelineType.agentSequence();
        log.info("启动 Pipeline: type={}, agents={}, requirement_length={}",
                pipelineType, pipeline.size(), userRequirement.length());

        Map<String, AgentResult> results = new LinkedHashMap<>();
        String accumulatedContext = userRequirement;

        return Mono.defer(() -> {
            // 顺序执行 Pipeline 中的每个 Agent
            Mono<Map<String, AgentResult>> chain = Mono.just(results);

            for (String agentType : pipeline) {
                chain = chain.flatMap(r -> executeAgent(agentType, accumulatedContext, context)
                        .doOnNext(result -> {
                            r.put(agentType, result);
                            accumulatedContext += "\n\n--- " + agentType + " 输出 ---\n" + result.output();
                        })
                        .thenReturn(r));
            }

            return chain.map(r -> new PipelineResult(pipelineType, r, Instant.now()));
        });
    }

    /**
     * 执行单个 Agent
     */
    public Mono<AgentResult> executeAgent(String agentType, String input, Map<String, String> context) {
        AgentDefinition def = agentRegistry.get(agentType);
        if (def == null) {
            return Mono.error(new IllegalArgumentException("未知的 Agent 类型: " + agentType));
        }

        String agentId = agentType + "-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("Agent [{}] 开始执行: model={}, maxTokens={}", agentId, def.model(), def.maxTokens());

        String fullPrompt = buildPrompt(def, input, context);

        return mimoClient.reason(def.systemPrompt(), fullPrompt, def.maxTokens())
                .map(response -> {
                    String output = response.firstContent();
                    int tokens = response.usage() != null ? response.usage().totalTokens() : 0;

                    // 记录 Token 消耗
                    tokenTracker.record(agentId, agentType, def.model(), pipelineTypeFromAgent(agentType),
                            response.usage());

                    log.info("Agent [{}] 完成: tokens={}, output_length={}", agentId, tokens, output.length());
                    return new AgentResult(agentId, agentType, output, tokens, Instant.now());
                })
                .onErrorResume(e -> {
                    log.error("Agent [{}] 执行失败: {}", agentId, e.getMessage());
                    return Mono.just(new AgentResult(agentId, agentType,
                            "ERROR: " + e.getMessage(), 0, Instant.now()));
                });
    }

    private String buildPrompt(AgentDefinition def, String input, Map<String, String> context) {
        StringBuilder sb = new StringBuilder();
        if (context != null && !context.isEmpty()) {
            sb.append("## 上下文信息\n");
            context.forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append("\n"));
            sb.append("\n");
        }
        sb.append("## 任务输入\n").append(input);
        return sb.toString();
    }

    private String pipelineTypeFromAgent(String agentType) {
        return agentType.toLowerCase().replace("_", "-");
    }

    // ═══════════════════════════════════════════
    //  内部类型
    // ═══════════════════════════════════════════

    public record AgentResult(String agentId, String agentType, String output,
                               int tokensUsed, Instant completedAt) {}

    public record PipelineResult(PipelineType type,
                                  Map<String, AgentResult> agentResults,
                                  Instant completedAt) {
        public int totalTokens() {
            return agentResults.values().stream().mapToInt(AgentResult::tokensUsed).sum();
        }
    }

    public enum PipelineType {
        /** 全流程：需求→架构→编码→审查→测试→安全 */
        FULL(List.of("REQUIREMENT_ANALYZER", "ARCHITECTURE_DESIGNER", "CODE_GENERATOR",
                "CODE_REVIEWER", "TEST_ENGINEER", "SECURITY_AUDITOR")),
        /** 快速：编码→审查 */
        QUICK(List.of("CODE_GENERATOR", "CODE_REVIEWER")),
        /** 审查模式：审查→安全 */
        REVIEW_ONLY(List.of("CODE_REVIEWER", "SECURITY_AUDITOR"));

        private final List<String> agentSequence;
        PipelineType(List<String> agentSequence) { this.agentSequence = agentSequence; }
        public List<String> agentSequence() { return agentSequence; }
    }
}
