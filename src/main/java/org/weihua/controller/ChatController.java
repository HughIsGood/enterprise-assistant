package org.weihua.controller;

import org.springframework.web.bind.annotation.*;
import org.weihua.model.chat.ChatRequest;
import org.weihua.model.chat.ClarifyRequest;
import org.weihua.model.task.Task;
import org.weihua.model.task.TaskStep;
import org.weihua.model.workflow.AgentResponse;
import org.weihua.service.task.TaskOrchestratorService;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final TaskOrchestratorService taskOrchestratorService;

    public ChatController(TaskOrchestratorService taskOrchestratorService) {
        this.taskOrchestratorService = taskOrchestratorService;
    }

    @PostMapping("/agent/ask")
    public AgentResponse ask(@RequestBody ChatRequest request) {
        return taskOrchestratorService.askSync(request.userId(), request.message());
    }

    @PostMapping("/agent/approve")
    public AgentResponse approve(@RequestParam("token") String token) {
        return taskOrchestratorService.approveAndContinue(token);
    }

    @PostMapping("/agent/clarify")
    public AgentResponse clarify(@RequestBody ClarifyRequest request) {
        return taskOrchestratorService.clarifyAndContinue(request.taskId(), request.message());
    }

    @GetMapping("/agent/tasks/{taskId}")
    public TaskDetailResponse taskDetail(@PathVariable("taskId") String taskId) {
        Task task = taskOrchestratorService.getTask(taskId);
        List<TaskStep> steps = taskOrchestratorService.getTaskSteps(taskId);
        return new TaskDetailResponse(task, steps);
    }

    public record TaskDetailResponse(Task task, List<TaskStep> steps) {
    }
}
