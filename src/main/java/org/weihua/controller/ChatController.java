package org.weihua.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.weihua.model.chat.ChatRequest;
import org.weihua.model.chat.ChatResponse;
import org.weihua.model.task.Task;
import org.weihua.model.task.TaskStep;
import org.weihua.model.workflow.AgentResponse;
import org.weihua.service.ChatService;
import org.weihua.service.task.TaskOrchestratorService;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final TaskOrchestratorService taskOrchestratorService;

    public ChatController(ChatService chatService,
                          TaskOrchestratorService taskOrchestratorService) {
        this.chatService = chatService;
        this.taskOrchestratorService = taskOrchestratorService;
    }

    @Deprecated
    @PostMapping("/dialogue")
    public ResponseEntity<ChatResponse> dialogue(@RequestBody ChatRequest request) {
        String answer = chatService.chat(request.userId(), request.message());
        return ResponseEntity.ok(new ChatResponse(answer));
    }

    @PostMapping("/agent/ask")
    public AgentResponse ask(@RequestBody ChatRequest request) {
        return taskOrchestratorService.askSync(request.userId(), request.message());
    }

    @PostMapping("/agent/approve")
    public AgentResponse approve(@RequestParam("token") String token) {
        return taskOrchestratorService.approveAndContinue(token);
    }

    @GetMapping("/agent/tasks/{taskId}")
    public TaskDetailResponse taskDetail(@PathVariable String taskId) {
        Task task = taskOrchestratorService.getTask(taskId);
        List<TaskStep> steps = taskOrchestratorService.getTaskSteps(taskId);
        return new TaskDetailResponse(task, steps);
    }

    public record TaskDetailResponse(Task task, List<TaskStep> steps) {
    }
}
