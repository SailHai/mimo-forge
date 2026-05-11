# MiMo Forge — AI Agent 智能体操作系统

<div align="center">

![MiMo Forge](https://img.shields.io/badge/MiMo%20Forge-v1.0-FF6900?style=for-the-badge&logo=xiaomi&logoColor=white)
![Java 21](https://img.shields.io/badge/Java%2021-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot%203.4-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![React 19](https://img.shields.io/badge/React%2019-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript%205.7-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![Kubernetes](https://img.shields.io/badge/K8s%20Ready-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white)

**基于 Xiaomi MiMo V2.5 系列模型构建的企业级 AI Agent 开发平台**

*多 Agent 协同编排 · MCP 工具链集成 · RAG 知识增强 · 实时 Token 监控*

</div>

---

## 为什么是 MiMo？

MiMo Forge 从第一天起就围绕 **Xiaomi MiMo V2.5** 设计，MiMo 不是"可选的模型之一"，而是整个系统的**推理核心引擎**：

| 能力 | MiMo 在 MiMo Forge 中的角色 |
|------|---------------------------|
| **MiMo-Max 推理** | Agent 编排引擎的"大脑"——架构设计、代码审查、安全审计等复杂多步推理任务全部依赖 MiMo-Max 的长链思维能力 |
| **MiMo-Lite 快速推理** | RAG 查询改写、代码补全、意图分类等低延迟任务，控制成本的同时保持质量 |
| **MiMo-VL 多模态** | UI 截图分析、架构图理解、文档 OCR，为 Agent 提供视觉上下文 |
| **MiMo-TTS 语音** | 代码审查结果的语音播报（企业场景中驾驶/会议时使用） |
| **中文原生优势** | 中文技术文档检索、中文注释生成、中文需求理解——这是 MiMo 相比其他模型的不可替代优势 |

> **核心设计哲学：MiMo 不是被集成进来的，而是 MiMo Forge 为 MiMo 而生。**

---

## 系统架构

```
┌──────────────────────────────────────────────────────────────────────┐
│                        MiMo Forge Platform                            │
│                                                                       │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────┐  ┌───────────┐  │
│  │  VSCode      │  │  Agent       │  │  RAG        │  │  Token    │  │
│  │  Extension   │  │  Studio      │  │  Engine     │  │  Monitor  │  │
│  │             │  │  (Web UI)    │  │  (Code-     │  │  Dashboard│  │
│  │ • 代码补全   │  │ • Pipeline   │  │   Aware)    │  │ • 实时    │  │
│  │ • AI 对话    │  │   编排       │  │ • 查询改写   │  │   消耗    │  │
│  │ • 代码审查   │  │ • Agent 监控  │  │ • 智能分块   │  │ • 预警    │  │
│  │ • 重构建议   │  │ • A/B 测试   │  │ • 引用标注   │  │ • 报告    │  │
│  └──────┬──────┘  └──────┬───────┘  └──────┬──────┘  └─────┬─────┘  │
│         │                │                  │               │         │
│  ┌──────┴────────────────┴──────────────────┴───────────────┴──────┐  │
│  │                    REST API / SSE Gateway                        │  │
│  └───────────────────────────┬─────────────────────────────────────┘  │
│                              │                                        │
│  ┌───────────────────────────┴─────────────────────────────────────┐  │
│  │              Agent Orchestration Engine (DAG)                     │  │
│  │                                                                   │  │
│  │   [需求分析] → [架构设计] → [代码生成] → [代码审查] → [测试] → [安全]│  │
│  │        │            │           │           │                     │  │
│  │   [知识检索]   [安全审计]   [文档生成]   [性能分析]                  │  │
│  └───────────────────────────┬─────────────────────────────────────┘  │
│                              │                                        │
│  ┌───────────────────────────┴─────────────────────────────────────┐  │
│  │                    MCP Tool Integration Layer                     │  │
│  │   ┌──────────┐  ┌──────────┐  ┌───────────┐  ┌──────────┐      │  │
│  │   │ 文件系统  │  │  GitHub  │  │ Playwright│  │ 自定义    │      │  │
│  │   │ Server   │  │  Server  │  │  Server   │  │ Tools    │      │  │
│  │   └──────────┘  └──────────┘  └───────────┘  └──────────┘      │  │
│  └───────────────────────────┬─────────────────────────────────────┘  │
│                              │                                        │
│  ┌───────────────────────────┴─────────────────────────────────────┐  │
│  │                     Unified MiMo Gateway                          │  │
│  │                                                                    │  │
│  │  ┌───────────┐ ┌──────────┐ ┌─────────┐ ┌────────┐ ┌──────────┐ │  │
│  │  │ MiMo-Max  │ │ MiMo-Lite│ │ MiMo-VL │ │MiMo-TTS│ │ Failover │ │  │
│  │  │ (推理引擎) │ │ (快速推理) │ │ (多模态) │ │ (语音) │ │ (容灾)  │ │  │
│  │  └───────────┘ └──────────┘ └─────────┘ └────────┘ └──────────┘ │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                       │
│  ┌──────────┐ ┌────────┐ ┌──────────┐ ┌────────┐ ┌────────────────┐  │
│  │PostgreSQL│ │ Redis  │ │  Milvus  │ │ Kafka  │ │ Prometheus +   │  │
│  │ (持久化)  │ │ (缓存)  │ │ (向量库)  │ │ (事件)  │ │ Grafana (监控) │  │
│  └──────────┘ └────────┘ └──────────┘ └────────┘ └────────────────┘  │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 核心模块

### 1. Agent 编排引擎 (`AgentOrchestrator`)

多 Agent DAG 编排，每个 Agent 节点独立配置模型、Prompt、Token 预算：

```java
// 启动完整 Pipeline：需求分析 → 架构 → 编码 → 审查 → 测试 → 安全
PipelineResult result = orchestrator.executePipeline(
    PipelineType.FULL,
    "开发一个企业级任务管理系统，支持 RBAC 权限、实时通知、审计日志",
    Map.of("tech_stack", "Java 21 + Spring Boot", "database", "PostgreSQL")
).block();

// 总 Token 消耗
System.out.println("Total tokens: " + result.totalTokens());
```

内置 6 种 Agent 类型，支持自定义扩展：
- `REQUIREMENT_ANALYZER` — 需求分析（MiMo-Max）
- `ARCHITECTURE_DESIGNER` — 架构设计（MiMo-Max）
- `CODE_GENERATOR` — 代码生成（MiMo-Max, maxTokens=32768）
- `CODE_REVIEWER` — 代码审查（MiMo-Max）
- `TEST_ENGINEER` — 测试生成（MiMo-Max）
- `SECURITY_AUDITOR` — 安全审计（MiMo-Max）

### 2. MiMo API 客户端 (`MiMoApiClient`)

统一封装 MiMo V2.5 全系列模型调用：

```java
// 同步推理（复杂任务 → MiMo-Max）
ChatResponse response = mimoClient.reason(systemPrompt, userPrompt, 8192).block();

// 快速推理（轻量任务 → MiMo-Lite）
ChatResponse quick = mimoClient.quickChat("分类专家", "将以下文本分类...").block();

// Tool Calling（Agent 使用工具时）
ChatResponse toolResp = mimoClient.chatWithTools(messages, toolSchemas).block();

// SSE 流式输出
mimoClient.chatStream(request).subscribe(chunk -> System.out.print(chunk));
```

### 3. RAG 知识增强引擎 (`RagEngine`)

Code-Aware RAG，理解代码结构的智能检索：

```
用户查询 → MiMo-Lite 查询改写 → 向量检索(Milvus) → 相关度过滤 → MiMo-Max 生成回答
```

特色功能：
- **智能分块**：代码按函数/类分块，文本按段落带重叠窗口分块
- **查询改写**：用 MiMo-Lite 扩展同义词，提升检索召回率
- **引用标注**：回答中标注知识来源，可追溯

### 4. MCP 工具集成 (`McpToolRegistry`)

支持 Model Context Protocol，让 Agent 可以使用外部工具：

```java
// 注册 MCP Server
toolRegistry.registerServer("github", new McpServerConfig("github", "stdio", "mcp-server-github", null));
toolRegistry.registerServer("filesystem", new McpServerConfig("filesystem", "stdio", "mcp-server-filesystem", null));

// 获取工具 schema 传给 MiMo API
List<Map<String, Object>> tools = toolRegistry.getToolSchemas();
ChatResponse response = mimoClient.chatWithTools(messages, tools).block();
```

### 5. Token 用量实时监控 (`TokenUsageTracker`)

Redis 驱动的实时 Token 消耗追踪：

- 按 Agent 类型、模型、日期多维度聚合
- 日/月消耗预警（可配置阈值）
- Grafana 可视化面板
- API: `GET /api/v1/token/usage`

### 6. VSCode 智能插件

集成 MiMo 的 IDE 编程助手：

| 功能 | 使用的模型 | 说明 |
|------|----------|------|
| 行内代码补全 | MiMo-Lite | 低延迟实时补全 |
| AI 对话 | MiMo-Max | 上下文感知的编程问答 |
| 代码解释 | MiMo-Max | 选中代码一键解释 |
| 代码重构 | MiMo-Max | 智能重构建议 |
| 代码审查 | MiMo-Max | 安全/性能/可维护性评估 |
| Agent Pipeline | MiMo-Max | 需求→架构→编码→测试全链路 |
| 知识检索 | MiMo-Lite+Max | RAG 增强的知识库查询 |

---

## Token 消耗场景与预估

| 场景 | 模型 | 频率 | 单次 Token | 日消耗 | 月消耗 |
|------|------|------|-----------|--------|--------|
| Agent Pipeline（全流程） | MiMo-Max | 20次/天 | 50K-100K | 1M-2M | 30M-60M |
| 代码生成 | MiMo-Max | 100次/天 | 4K-8K | 400K-800K | 12M-24M |
| 代码审查 | MiMo-Max | 50次/天 | 4K-8K | 200K-400K | 6M-12M |
| 行内补全 | MiMo-Lite | 500次/天 | 200-500 | 100K-250K | 3M-7.5M |
| RAG 检索+生成 | Lite+Max | 80次/天 | 5K-10K | 400K-800K | 12M-24M |
| 多模型 A/B 测试 | MiMo-Max | 30次/天 | 10K-20K | 300K-600K | 9M-18M |
| Tool Calling 调用 | MiMo-Max | 60次/天 | 8K-15K | 480K-900K | 14.4M-27M |
| **合计** | | | | **2.88M-5.55M** | **86.4M-172.5M** |

> **预估月消耗：8600万 ~ 1.7亿 tokens**，稳定运行后将持续增长。

---

## Tech Stack

| 层级 | 技术 |
|------|------|
| 后端 | Java 21 + Spring Boot 3.4 + WebFlux (Reactive) + GraalVM Native Image |
| 前端 | React 19 + TypeScript 5.7 + Vite 6 + TailwindCSS |
| AI 层 | LangChain4j + MiMo API (OpenAI 兼容) + MCP Protocol |
| 数据库 | PostgreSQL 17 (R2DBC Reactive) + Redis 7 + Milvus 2.x (向量) |
| 消息队列 | Apache Kafka (Agent 事件流) |
| IDE 插件 | VSCode Extension API + TypeScript |
| 部署 | Docker + Kubernetes + HPA 自动扩缩 |
| 监控 | Prometheus + Grafana + Spring Actuator |

---

## Quick Start

```bash
# 1. 克隆项目
git clone https://github.com/[username]/mimo-forge.git
cd mimo-forge

# 2. 配置 MiMo API Key
export MIMO_API_KEY=your_mimo_api_key

# 3. Docker Compose 一键启动
cd docker && docker compose up -d

# 4. 访问 API
curl http://localhost:8080/api/v1/health

# 5. 启动 Agent Pipeline
curl -X POST http://localhost:8080/api/v1/pipeline \
  -H "Content-Type: application/json" \
  -d '{"type":"FULL","requirement":"开发一个 REST API 用户管理系统"}'

# 6. VSCode 插件
cd vscode-plugin && npm install && npm run build
# 在 VSCode 中按 F5 启动调试
```

---

## 项目文件结构

```
mimo-forge/
├── pom.xml                          # Maven 项目配置 (Java 21, Spring Boot 3.4)
├── src/main/java/com/mimoforge/
│   ├── MimoForgeApplication.java    # 启动入口
│   ├── client/
│   │   └── MiMoApiClient.java      # MiMo V2.5 API 统一客户端
│   ├── agent/
│   │   ├── AgentOrchestrator.java   # Agent 编排引擎 (DAG)
│   │   └── AgentDefinition.java     # Agent 定义模型
│   ├── mcp/
│   │   └── McpToolRegistry.java     # MCP 工具注册中心
│   ├── rag/
│   │   ├── RagEngine.java           # RAG 知识增强引擎
│   │   └── VectorStore.java         # 向量存储接口
│   ├── model/
│   │   ├── ChatMessage.java         # 消息模型
│   │   ├── ChatRequest.java         # 请求模型
│   │   ├── ChatResponse.java        # 响应模型
│   │   └── TokenUsageRecord.java    # Token 用量记录
│   ├── service/
│   │   └── TokenUsageTracker.java   # Token 实时追踪器
│   ├── controller/
│   │   └── AgentController.java     # REST API 控制器
│   └── config/
│       ├── MiMoConfig.java          # API 配置
│       └── RedisConfig.java         # Redis 配置
├── src/main/resources/
│   └── application.yml              # 应用配置
├── vscode-plugin/                   # VSCode 智能插件
│   ├── package.json
│   └── src/extension.ts
├── docker/
│   ├── Dockerfile                   # 多阶段构建
│   └── docker-compose.yml           # 全栈一键部署
└── k8s/
    └── deployment.yaml              # K8s 部署 + HPA
```

---

## 开源计划

MiMo Forge 计划以 **Apache 2.0** 协议开源，推动 MiMo 生态发展：

- v1.0 — Agent 编排引擎 + MiMo API 客户端 + VSCode 插件
- v1.1 — Agent Marketplace（Agent 模板市场）
- v1.2 — 多租户支持 + 企业 SSO
- v2.0 — MiMo Fine-tuning Pipeline（模型微调流水线）

---

## License

[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
