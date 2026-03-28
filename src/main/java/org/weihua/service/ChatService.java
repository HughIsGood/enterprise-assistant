package org.weihua.service;

import org.springframework.stereotype.Service;
import org.weihua.assistant.ChatAssistant;

@Service
public class ChatService {

    private final ChatAssistant chatAssistant;

    public ChatService(ChatAssistant chatAssistant) {
        this.chatAssistant = chatAssistant;
    }

    public String chat(String userId, String message) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        return chatAssistant.chat(userId, message);
    }
}