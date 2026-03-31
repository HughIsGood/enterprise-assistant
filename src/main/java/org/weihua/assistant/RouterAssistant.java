package org.weihua.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "anthropicChatModel"
)
public interface RouterAssistant {

    @SystemMessage(fromResource = "router_system_message.txt")
    @UserMessage(fromResource = "router_user_message.txt")
    String route(@V("input") String input);
}
