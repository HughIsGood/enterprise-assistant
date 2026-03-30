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

    @SystemMessage("""
        你是企业知识助手的意图路由器。
        你只负责判断用户输入属于哪一种意图，不负责回答问题。

        只允许输出以下四种意图之一：
        KNOWLEDGE_QA
        PROCESS_QA
        ACTION_REQUEST
        CLARIFICATION

        当 intentType=ACTION_REQUEST 且用户是在“列文档/查文档”时，必须输出 documentType：
        POLICY、TECH_TYPE、SOP、FAQ

        其他场景 documentType 输出 NONE。

        输出格式严格为三行：
        intentType=...
        reason=...
        documentType=...
        """)
    @UserMessage("""
        用户输入：
        {{input}}
        """)
    String route(@V("input") String input);
}
