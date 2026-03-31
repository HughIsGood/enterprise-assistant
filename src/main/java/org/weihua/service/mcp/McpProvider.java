package org.weihua.service.mcp;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.service.tool.ToolProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class McpProvider {

    @Tool("查询mysql数据库表")
    public ToolProvider buildProvider() {
        // 1.构建mcp协议
        McpTransport transport = new StdioMcpTransport.Builder()
                .command(List.of("npx.cmd", "-y", "@fhuang/mcp-mysql-server"))
                .environment(Map.of("MYSQL_HOST", "127.0.0.1",
                        "MYSQL_USER", "root",
                        "MYSQL_PORT", "3306",
                        "MYSQL_PASSWORD", "root",
                        "MYSQL_DATABASE", "langchain4j")).build();
        // 2.创建mcp client
        McpClient mcpClient = new DefaultMcpClient.Builder().transport(transport).build();
        // 3.创建工具集
        return McpToolProvider.builder().mcpClients(mcpClient).build();
    }
}
