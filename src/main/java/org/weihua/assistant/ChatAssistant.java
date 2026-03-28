package org.weihua.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "anthropicChatModel",
        chatMemoryProvider = "chatMySqlMemoryProvider"
)
public interface ChatAssistant {

    @SystemMessage(fromResource = "enterprise_system_message.txt")
    @UserMessage(fromResource = "enterprise_user_message.txt")
    String chat(@MemoryId String memoryId,
                @V("context") String context,
                @V("question") String question);
}