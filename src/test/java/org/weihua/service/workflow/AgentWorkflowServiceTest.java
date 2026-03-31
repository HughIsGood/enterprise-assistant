package org.weihua.service.workflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.weihua.assistant.KnowledgeAssistant;
import org.weihua.model.tools.ToolCallResult;
import org.weihua.model.workflow.ActionCommand;
import org.weihua.model.workflow.ActionType;
import org.weihua.model.workflow.AgentContext;
import org.weihua.model.workflow.AgentResponse;
import org.weihua.model.workflow.IntentDecision;
import org.weihua.model.workflow.IntentType;
import org.weihua.service.ApprovalService;
import org.weihua.service.IntentRouterService;
import org.weihua.service.rag.ContextAssembler;
import org.weihua.service.rag.RetrievalService;
import org.weihua.service.tools.ToolExecutionService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = AgentWorkflowService.class)
class AgentWorkflowServiceTest {

    @Autowired
    private AgentWorkflowService agentWorkflowService;

    @MockBean
    private IntentRouterService intentRouterService;

    @MockBean
    private RetrievalService retrievalService;

    @MockBean
    private ContextAssembler contextAssembler;

    @MockBean
    private KnowledgeAssistant knowledgeAssistant;

    @MockBean
    private ToolExecutionService toolExecutionService;

    @MockBean
    private ApprovalService approvalService;

    @Test
    void shouldUseKnowledgeQaRetrievalParams() {
        when(intentRouterService.route("知识问题")).thenReturn(
                new IntentDecision(IntentType.KNOWLEDGE_QA, "知识问答", null)
        );
        when(retrievalService.retrieve(eq("知识问题"), eq(3), eq(0.65))).thenReturn(List.of());
        when(contextAssembler.buildContext(any())).thenReturn("context");
        when(knowledgeAssistant.answer("u1", "context", "知识问题")).thenReturn("知识答案");

        AgentResponse response = agentWorkflowService.handle("u1", "知识问题");

        assertEquals("KNOWLEDGE_QA", response.intentType());
        assertEquals("知识答案", response.answer());
        assertFalse(response.approvalRequired());
        verify(retrievalService).retrieve("知识问题", 3, 0.65);
    }

    @Test
    void shouldUseProcessQaRetrievalParams() {
        when(intentRouterService.route("流程问题")).thenReturn(
                new IntentDecision(IntentType.PROCESS_QA, "流程问答", null)
        );
        when(retrievalService.retrieve(eq("流程问题"), eq(6), eq(0.5))).thenReturn(List.of());
        when(contextAssembler.buildContext(any())).thenReturn("process-context");
        when(knowledgeAssistant.answer("u2", "process-context", "流程问题")).thenReturn("流程答案");

        AgentResponse response = agentWorkflowService.handle("u2", "流程问题");

        assertEquals("PROCESS_QA", response.intentType());
        assertEquals("流程答案", response.answer());
        assertFalse(response.approvalRequired());
        verify(retrievalService).retrieve("流程问题", 6, 0.5);
    }

    @Test
    void shouldRequireApprovalForCreateTicketAction() {
        ActionCommand actionCommand = new ActionCommand(
                ActionType.CREATE_TICKET, null, null, null, "VPN无法登录", "今天上午开始无法连接"
        );
        when(intentRouterService.route("帮我提工单")).thenReturn(
                new IntentDecision(IntentType.ACTION_REQUEST, "创建工单", actionCommand)
        );
        when(approvalService.createPendingApproval(any(AgentContext.class))).thenReturn("token-123");
        when(toolExecutionService.resolveToolName(ActionType.CREATE_TICKET)).thenReturn("createSupportTicket");

        AgentResponse response = agentWorkflowService.handle("u4", "帮我提工单");

        assertEquals("ACTION_REQUEST", response.intentType());
        assertTrue(response.approvalRequired());
        assertEquals("token-123", response.approvalToken());
        assertEquals(List.of("createSupportTicket"), response.usedTools());
    }
}
