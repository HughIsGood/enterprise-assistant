package org.weihua.service;

import org.springframework.stereotype.Component;

@Component
public class ChatInputValidator {

    public void validate(String userId, String message) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}