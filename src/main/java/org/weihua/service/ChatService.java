package org.weihua.service;

import org.springframework.stereotype.Service;
import org.weihua.assistant.ChatAssistant;

@Service
public class ChatService {

    private final ChatAssistant chatAssistant;
    private final ChatInputValidator chatInputValidator;
    private final KnowledgeRetriever knowledgeRetriever;

    public ChatService(ChatAssistant chatAssistant,
                       ChatInputValidator chatInputValidator,
                       KnowledgeRetriever knowledgeRetriever) {
        this.chatAssistant = chatAssistant;
        this.chatInputValidator = chatInputValidator;
        this.knowledgeRetriever = knowledgeRetriever;
    }

    public String chat(String userId, String message) {
        chatInputValidator.validate(userId, message);

        KnowledgeRetriever.RetrievedContext retrievedContext = knowledgeRetriever.retrieve(message);
        return chatAssistant.chat(userId, retrievedContext.context(), message);
    }
}