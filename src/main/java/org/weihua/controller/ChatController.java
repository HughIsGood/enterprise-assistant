package org.weihua.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.weihua.model.chat.ChatRequest;
import org.weihua.model.chat.ChatResponse;
import org.weihua.service.ChatService;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/dialogue")
    public ResponseEntity<ChatResponse> dialogue(@RequestBody ChatRequest request) {
        String answer = chatService.chat(request.userId(), request.message());
        return ResponseEntity.ok(new ChatResponse(answer));
    }
}