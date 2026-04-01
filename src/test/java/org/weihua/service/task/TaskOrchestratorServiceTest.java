package org.weihua.service.task;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.weihua.assistant.KnowledgeAssistant;
import org.weihua.model.task.Task;
import org.weihua.model.task.TaskApproval;
import org.weihua.model.task.TaskStatus;
import org.weihua.model.tools.ToolCallResult;
import org.weihua.model.workflow.ActionCommand;
import org.weihua.model.workflow.ActionType;
import org.weihua.model.workflow.AgentResponse;
import org.weihua.model.workflow.IntentDecision;
import org.weihua.model.workflow.IntentType;
import org.weihua.repository.task.TaskRepository;
import org.weihua.repository.task.TaskStepRepository;
import org.weihua.service.rag.ContextAssembler;
import org.weihua.service.rag.RetrievalService;
import org.weihua.service.tools.ToolExecutionService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = TaskOrchestratorService.class)
class TaskOrchestratorServiceTest {

    @Autowired
    private TaskOrchestratorService orchestratorService;

    @MockBean
    private TaskRepository taskRepository;

    @MockBean
    private TaskStepRepository taskStepRepository;

    @MockBean
    private TaskPlannerService taskPlannerService;

    @MockBean
    private TaskExecutionService taskExecutionService;

    @MockBean
    private TaskApprovalService taskApprovalService;

    @MockBean
    private ToolExecutionService toolExecutionService;

    @MockBean
    private RetrievalService retrievalService;

    @MockBean
    private ContextAssembler contextAssembler;

    @MockBean
    private KnowledgeAssistant knowledgeAssistant;

    @Test
    void shouldCompleteKnowledgeQaTask() {
        when(taskPlannerService.planNext(anyString())).thenReturn(
                new IntentDecision(IntentType.KNOWLEDGE_QA, "knowledge", null)
        );
        when(taskStepRepository.findByTaskId(anyString())).thenReturn(List.of());
        when(taskStepRepository.nextStepNo(anyString())).thenReturn(1);
        when(retrievalService.retrieve("what is policy", 3, 0.65)).thenReturn(List.of());
        when(contextAssembler.buildContext(any())).thenReturn("ctx");
        when(knowledgeAssistant.answer(any(), any(), any())).thenReturn("answer");

        AgentResponse response = orchestratorService.askSync("u1", "what is policy");

        assertEquals("KNOWLEDGE_QA", response.intentType());
        assertEquals("answer", response.answer());
        assertFalse(response.approvalRequired());
        assertFalse(response.clarificationRequired());
    }

    @Test
    void shouldRequireApprovalForCreateTicketTask() {
        ActionCommand command = new ActionCommand(ActionType.CREATE_TICKET, null, null, null, "VPN issue", "cannot connect");
        when(taskPlannerService.planNext(anyString())).thenReturn(
                new IntentDecision(IntentType.ACTION_REQUEST, "ticket", command)
        );
        when(taskStepRepository.findByTaskId(anyString())).thenReturn(List.of());
        when(taskStepRepository.nextStepNo(anyString())).thenReturn(1, 2);
        when(taskApprovalService.createPendingApproval(anyString(), any(Integer.class), any())).thenReturn("token-1");
        when(toolExecutionService.resolveToolName(ActionType.CREATE_TICKET)).thenReturn("createSupportTicket");

        AgentResponse response = orchestratorService.askSync("u2", "create ticket");

        assertEquals("ACTION_REQUEST", response.intentType());
        assertTrue(response.approvalRequired());
        assertEquals("token-1", response.approvalToken());
        assertFalse(response.clarificationRequired());
    }

    @Test
    void shouldApproveAndContinue() {
        ActionCommand command = new ActionCommand(ActionType.CREATE_TICKET, null, null, null, "VPN issue", "cannot connect");
        when(taskApprovalService.approve("token-x")).thenReturn(new TaskApproval("token-x", "task-1", 1, command, true));

        Task task = new Task();
        task.setTaskId("task-1");
        task.setGoal("what is policy");
        task.setStatus(TaskStatus.WAITING_APPROVAL);
        when(taskRepository.findByTaskId("task-1")).thenReturn(Optional.of(task));

        when(taskExecutionService.execute(command)).thenReturn(new ToolCallResult("createSupportTicket", "ok"));

        AgentResponse response = orchestratorService.approveAndContinue("token-x");

        assertEquals("ACTION_REQUEST", response.intentType());
        assertEquals("ok", response.answer());
        assertFalse(response.approvalRequired());
    }

    @Test
    void shouldWaitForClarificationThenContinue() {
        when(taskPlannerService.planNext(anyString()))
                .thenReturn(new IntentDecision(IntentType.CLARIFICATION, "need more", null))
                .thenReturn(new IntentDecision(IntentType.KNOWLEDGE_QA, "knowledge", null));

        when(taskStepRepository.findByTaskId(anyString())).thenReturn(List.of());
        when(taskStepRepository.nextStepNo(anyString())).thenReturn(1, 2, 3, 4);

        when(retrievalService.retrieve(anyString(), any(Integer.class), any(Double.class))).thenReturn(List.of());
        when(contextAssembler.buildContext(any())).thenReturn("ctx");
        when(knowledgeAssistant.answer(any(), any(), any())).thenReturn("final-answer");

        Task waitingTask = new Task();
        waitingTask.setTaskId("task-clarify");
        waitingTask.setGoal("create ticket");
        waitingTask.setStatus(TaskStatus.WAITING_CLARIFICATION);
        when(taskRepository.findByTaskId("task-clarify")).thenReturn(Optional.of(waitingTask));

        AgentResponse first = orchestratorService.askSync("u3", "create ticket");
        assertTrue(first.clarificationRequired());

        AgentResponse second = orchestratorService.clarifyAndContinue("task-clarify", "account zhangsan, vpn login failed");
        assertEquals("KNOWLEDGE_QA", second.intentType());
        assertEquals("final-answer", second.answer());
        assertFalse(second.clarificationRequired());
    }
}
