package org.weihua.assistant;

import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "anthropicChatModel")
public interface McpAssistant {
    String chat(String message);
}
