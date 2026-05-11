package com.mimoforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * MiMo Forge — AI Agent 智能体操作系统
 *
 * 基于 Xiaomi MiMo V2.5 系列模型构建的企业级 AI Agent 开发平台，
 * 支持多 Agent 协同编排、MCP 工具链集成、RAG 知识增强。
 *
 * @author Senior AI Engineer (15 years)
 */
@SpringBootApplication
@EnableScheduling
public class MimoForgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MimoForgeApplication.class, args);
    }
}
