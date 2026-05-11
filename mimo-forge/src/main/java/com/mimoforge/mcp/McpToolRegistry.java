package com.mimoforge.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP (Model Context Protocol) 工具注册中心
 *
 * 管理所有 MCP Server 暴露的工具，将工具 schema 转换为
 * MiMo API 可理解的 function calling 格式。
 *
 * 支持的 MCP 传输协议：
 * - stdio: 本地进程通信
 * - sse: Server-Sent Events 远程通信
 * - streamable-http: HTTP 流式通信
 *
 * @author Senior AI Engineer
 */
@Component
public class McpToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(McpToolRegistry.class);

    private final ObjectMapper objectMapper;
    private final Map<String, McpServerConnection> connections = new ConcurrentHashMap<>();
    private final Map<String, ToolDefinition> tools = new ConcurrentHashMap<>();

    public McpToolRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 注册一个 MCP Server 及其工具
     */
    public void registerServer(String serverName, McpServerConfig config) {
        log.info("注册 MCP Server: name={}, transport={}", serverName, config.transport());
        var conn = new McpServerConnection(serverName, config, objectMapper);
        connections.put(serverName, conn);

        // 获取服务器暴露的工具列表
        conn.listTools().forEach(tool -> {
            String qualifiedName = serverName + ":" + tool.name();
            tools.put(qualifiedName, tool);
            log.info("  注册工具: {} - {}", qualifiedName, tool.description());
        });
    }

    /**
     * 获取所有已注册工具的 OpenAI Function Calling 格式定义
     * 传给 MiMo API 的 tools 参数
     */
    public List<Map<String, Object>> getToolSchemas() {
        return tools.values().stream()
                .map(this::toFunctionSchema)
                .toList();
    }

    /**
     * 执行指定工具
     */
    public ToolResult executeTool(String toolName, Map<String, Object> arguments) {
        ToolDefinition tool = tools.get(toolName);
        if (tool == null) {
            return new ToolResult(toolName, false, "工具不存在: " + toolName, null);
        }

        String serverName = toolName.split(":")[0];
        McpServerConnection conn = connections.get(serverName);
        if (conn == null) {
            return new ToolResult(toolName, false, "MCP Server 未连接: " + serverName, null);
        }

        log.info("执行 MCP 工具: {} args={}", toolName, arguments);
        try {
            JsonNode result = conn.callTool(tool.name(), arguments);
            return new ToolResult(toolName, true, result.toString(), null);
        } catch (Exception e) {
            log.error("MCP 工具执行失败: {} - {}", toolName, e.getMessage());
            return new ToolResult(toolName, false, null, e.getMessage());
        }
    }

    private Map<String, Object> toFunctionSchema(ToolDefinition tool) {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", tool.name(),
                        "description", tool.description(),
                        "parameters", tool.inputSchema() != null ? tool.inputSchema() : Map.of()
                )
        );
    }

    // ═══════════════════════════════════════════
    //  内部类型
    // ═══════════════════════════════════════════

    public record ToolDefinition(String name, String description, Map<String, Object> inputSchema) {}

    public record ToolResult(String toolName, boolean success, String output, String error) {}

    public record McpServerConfig(String name, String transport, String command, String url) {}

    /**
     * MCP Server 连接封装
     * 实际生产中需要实现 MCP 协议的 JSON-RPC 通信
     */
    public static class McpServerConnection {
        private final String name;
        private final McpServerConfig config;
        private final ObjectMapper objectMapper;

        public McpServerConnection(String name, McpServerConfig config, ObjectMapper objectMapper) {
            this.name = name;
            this.config = config;
            this.objectMapper = objectMapper;
            initialize();
        }

        private void initialize() {
            // 根据 transport 类型初始化连接
            // stdio: 启动子进程，通过 stdin/stdout 通信
            // sse: 建立 SSE 连接
            // streamable-http: 建立 HTTP 连接
        }

        public List<ToolDefinition> listTools() {
            // 发送 tools/list JSON-RPC 请求
            return List.of();
        }

        public JsonNode callTool(String toolName, Map<String, Object> arguments) {
            // 发送 tools/call JSON-RPC 请求
            return objectMapper.createObjectNode();
        }
    }
}
