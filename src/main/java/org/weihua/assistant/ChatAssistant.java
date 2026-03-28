package org.weihua.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = "anthropicChatModel", chatMemoryProvider = "chatMySqlMemoryProvider")

public interface ChatAssistant {

    String chat(@MemoryId String memoryId, @UserMessage String message);
}