package com.mimoforge.rag;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 向量存储接口 — 抽象化底层实现（Milvus / PgVector / Redis）
 */
public interface VectorStore {

    /**
     * 向量检索
     */
    Flux<RagEngine.SearchResult> search(String query, String collection, int topK,
                                          Map<String, Object> filters);

    /**
     * 写入向量
     */
    Mono<Integer> upsert(java.util.List<RagEngine.DocumentChunk> chunks, String collection);

    /**
     * 删除向量
     */
    Mono<Integer> delete(String documentId, String collection);

    /**
     * 创建集合
     */
    Mono<Void> createCollection(String collection, int dimension);
}
