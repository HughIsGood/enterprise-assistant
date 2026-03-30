package org.weihua.service;

import org.springframework.stereotype.Service;
import org.weihua.assistant.KnowledgeAssistant;

@Service
public class ChatService {

    private final KnowledgeAssistant knowledgeAssistant;
    private final ChatInputValidator chatInputValidator;
    private final KnowledgeRetriever knowledgeRetriever;

    public ChatService(KnowledgeAssistant knowledgeAssistant,
                       ChatInputValidator chatInputValidator,
                       KnowledgeRetriever knowledgeRetriever) {
        this.knowledgeAssistant = knowledgeAssistant;
        this.chatInputValidator = chatInputValidator;
        this.knowledgeRetriever = knowledgeRetriever;
    }

    public String chat(String userId, String message) {
        chatInputValidator.validate(userId, message);

        KnowledgeRetriever.RetrievedContext retrievedContext = knowledgeRetriever.retrieve(message);
        return knowledgeAssistant.answer(userId, retrievedContext.context(), message);
    }
}