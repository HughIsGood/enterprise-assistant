package org.weihua.service.mcp;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.weihua.assistant.McpAssistant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class McpProviderTest {

    @Autowired
    private McpProvider mcpProvider;

    @Autowired
    private AnthropicChatModel anthropicChatModel;

    @Test
    void buildProvider() {
        McpAssistant assistant = AiServices.builder(McpAssistant.class).chatModel(anthropicChatModel).toolProvider(mcpProvider.buildProvider()).build();
        String chat = assistant.chat("查一下mysql数据库文档表有哪些字段");
        System.out.println(chat);
    }

}