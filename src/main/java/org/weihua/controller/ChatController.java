package org.weihua.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.weihua.model.chat.ChatRequest;
import org.weihua.model.chat.ChatResponse;
import org.weihua.model.tools.ToolCallResult;
import org.weihua.model.workflow.AgentContext;
import org.weihua.model.workflow.AgentResponse;
import org.weihua.service.ApprovalService;
import org.weihua.service.ChatService;
import org.weihua.service.tools.ToolExecutionService;
import org.weihua.service.workflow.AgentWorkflowService;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final AgentWorkflowService agentWorkflowService;
    private final ApprovalService approvalService;
    private final ToolExecutionService toolExecutionService;

    public ChatController(ChatService chatService,
                          AgentWorkflowService agentWorkflowService,
                          ApprovalService approvalService,
                          ToolExecutionService toolExecutionService) {
        this.chatService = chatService;
        this.agentWorkflowService = agentWorkflowService;
        this.approvalService = approvalService;
        this.toolExecutionService = toolExecutionService;
    }

    @Deprecated
    @PostMapping("/dialogue")
    public ResponseEntity<ChatResponse> dialogue(@RequestBody ChatRequest request) {
        String answer = chatService.chat(request.userId(), request.message());
        return ResponseEntity.ok(new ChatResponse(answer));
    }

    @PostMapping("/agent/ask")
    public AgentResponse ask(@RequestBody ChatRequest request) {
        return agentWorkflowService.handle(request.userId(), request.message());
    }

    @PostMapping("/agent/approve")
    public AgentResponse approve(@RequestParam("token") String token) {
        AgentContext context = approvalService.approve(token);

        if (context == null) {
            return AgentResponse.answer("ACTION_REQUEST", "No pending approval found.", List.of(), false, null);
        }

        ToolCallResult result = toolExecutionService.executeCreateTicket(
                "User support request",
                context.getUserInput()
        );

        return AgentResponse.answer(
                "ACTION_REQUEST",
                result.result(),
                List.of(result.toolName()),
                false,
                null
        );
    }
}
