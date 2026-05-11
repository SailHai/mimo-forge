package com.mimoforge.rag;

import com.mimoforge.client.MiMoApiClient;
import com.mimoforge.model.ChatMessage;
import com.mimoforge.model.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG (Retrieval-Augmented Generation) 知识增强引擎
 *
 * 架构：
 *   用户查询 → 向量检索 → Rerank 重排 → MiMo 生成 → 引用标注
 *
 * 特色：
 * - Code-Aware RAG：理解代码结构和依赖关系
 * - 多语言知识库：中英文混合检索
 * - 增量索引：支持文档/代码/API 文档的增量更新
 * - 查询改写：使用 MiMo Lite 对原始查询进行语义扩展
 *
 * @author Senior AI Engineer
 */
@Service
public class RagEngine {

    private static final Logger log = LoggerFactory.getLogger(RagEngine.class);

    private final MiMoApiClient mimoClient;
    private final VectorStore vectorStore;
    private final int topK;
    private final double minScore;

    public RagEngine(
            MiMoApiClient mimoClient,
            VectorStore vectorStore,
            @Value("${mimo.rag.top-k}") int topK,
            @Value("${mimo.rag.min-score}") double minScore
    ) {
        this.mimoClient = mimoClient;
        this.vectorStore = vectorStore;
        this.topK = topK;
        this.minScore = minScore;
    }

    /**
     * 核心 RAG 调用：检索 + 生成
     */
    public Mono<RagResponse> query(String question, String collection, Map<String, Object> filters) {
        log.info("RAG 查询: question='{}', collection={}", truncate(question, 80), collection);

        // Step 1: 查询改写 — 用 MiMo Lite 扩展语义
        return rewriteQuery(question)
                .flatMap(rewritten -> {
                    log.debug("查询改写: '{}' → '{}'", truncate(question, 50), truncate(rewritten, 50));

                    // Step 2: 向量检索
                    return vectorStore.search(rewritten, collection, topK, filters)
                            .filter(doc -> doc.score() >= minScore)
                            .collectList();
                })
                .flatMap(documents -> {
                    if (documents.isEmpty()) {
                        return Mono.just(new RagResponse(question, "抱歉，未找到相关知识。", List.of(), 0));
                    }

                    // Step 3: 构建增强 Prompt
                    String context = documents.stream()
                            .map(doc -> String.format("【来源: %s | 相关度: %.2f】\n%s",
                                    doc.source(), doc.score(), doc.content()))
                            .collect(Collectors.joining("\n\n---\n\n"));

                    String augmentedPrompt = """
                            ## 检索到的知识片段
                            %s

                            ## 用户问题
                            %s

                            请基于以上知识回答问题。如果知识片段中没有相关信息，请明确说明。
                            引用知识时请标注来源。
                            """.formatted(context, question);

                    // Step 4: MiMo 生成回答
                    return mimoClient.reason(
                                    "你是一个知识问答专家，回答准确、结构清晰，优先使用检索到的知识。",
                                    augmentedPrompt,
                                    4096)
                            .map(response -> new RagResponse(
                                    question,
                                    response.firstContent(),
                                    documents,
                                    response.usage() != null ? response.usage().totalTokens() : 0));
                });
    }

    /**
     * 查询改写 — 用 MiMo Lite 对原始查询做语义扩展
     */
    private Mono<String> rewriteQuery(String originalQuery) {
        return mimoClient.quickChat(
                "你是一个查询改写专家。将用户的查询改写为更适合向量检索的形式，" +
                "扩展同义词和相关概念，保持核心语义不变。只输出改写后的查询，不要解释。",
                originalQuery
        ).map(ChatResponse::firstContent)
         .defaultIfEmpty(originalQuery); // 改写失败则用原始查询
    }

    /**
     * 索引文档到知识库
     */
    public Mono<IndexResult> indexDocument(Document document) {
        log.info("索引文档: id={}, source={}, content_length={}",
                document.id(), document.source(), document.content().length());

        // 分块策略
        List<DocumentChunk> chunks = splitDocument(document);
        log.info("文档分块: {} chunks", chunks.size());

        return vectorStore.upsert(chunks, document.collection())
                .map(count -> new IndexResult(document.id(), count, true, null));
    }

    /**
     * 智能文档分块 — 支持代码和自然语言
     */
    private List<DocumentChunk> splitDocument(Document doc) {
        List<DocumentChunk> chunks = new ArrayList<>();
        String content = doc.content();

        if (doc.type() == DocumentType.CODE) {
            // 代码按函数/类分块
            chunks.addAll(splitCode(content, doc));
        } else {
            // 文本按段落分块，带重叠窗口
            int chunkSize = 1000;
            int overlap = 200;
            for (int i = 0; i < content.length(); i += chunkSize - overlap) {
                int end = Math.min(i + chunkSize, content.length());
                String chunkContent = content.substring(i, end);
                chunks.add(new DocumentChunk(
                        doc.id() + "-chunk-" + chunks.size(),
                        chunkContent,
                        doc.source(),
                        doc.metadata()));
            }
        }
        return chunks;
    }

    private List<DocumentChunk> splitCode(String code, Document doc) {
        // 按类定义和方法定义分块
        List<DocumentChunk> chunks = new ArrayList<>();
        String[] lines = code.split("\n");
        StringBuilder current = new StringBuilder();
        int depth = 0;
        int chunkIndex = 0;

        for (String line : lines) {
            current.append(line).append("\n");
            depth += line.chars().filter(c -> c == '{').count();
            depth -= line.chars().filter(c -> c == '}').count();

            if (depth == 0 && !current.isEmpty()) {
                chunks.add(new DocumentChunk(
                        doc.id() + "-code-" + chunkIndex++,
                        current.toString().trim(),
                        doc.source(),
                        doc.metadata()));
                current = new StringBuilder();
            }
        }
        if (!current.isEmpty()) {
            chunks.add(new DocumentChunk(
                    doc.id() + "-code-" + chunkIndex,
                    current.toString().trim(),
                    doc.source(),
                    doc.metadata()));
        }
        return chunks;
    }

    private String truncate(String s, int maxLen) {
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }

    // ═══════════════════════════════════════════
    //  内部类型
    // ═══════════════════════════════════════════

    public record RagResponse(String query, String answer, List<SearchResult> sources, int tokensUsed) {}

    public record SearchResult(String content, String source, double score, Map<String, Object> metadata) {}

    public record Document(String id, String content, String source, DocumentType type,
                            String collection, Map<String, Object> metadata) {}

    public record DocumentChunk(String id, String content, String source, Map<String, Object> metadata) {}

    public record IndexResult(String documentId, int chunksIndexed, boolean success, String error) {}

    public enum DocumentType { TEXT, CODE, MARKDOWN, PDF, API_DOC }
}
