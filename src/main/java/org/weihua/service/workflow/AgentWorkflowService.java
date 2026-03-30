package org.weihua.service.workflow;

import org.springframework.stereotype.Service;
import org.weihua.assistant.KnowledgeAssistant;
import org.weihua.model.document.DocumentType;
import org.weihua.model.tools.ToolCallResult;
import org.weihua.model.workflow.AgentContext;
import org.weihua.model.workflow.AgentResponse;
import org.weihua.model.workflow.IntentDecision;
import org.weihua.service.ApprovalService;
import org.weihua.service.IntentRouterService;
import org.weihua.service.rag.ContextAssembler;
import org.weihua.service.rag.RetrievalService;
import org.weihua.service.tools.ToolExecutionService;

import java.util.List;

@Service
public class AgentWorkflowService {

    private final IntentRouterService intentRouterService;
    private final RetrievalService retrievalService;
    private final ContextAssembler contextAssembler;
    private final KnowledgeAssistant knowledgeAssistant;
    private final ToolExecutionService toolExecutionService;
    private final ApprovalService approvalService;

    public AgentWorkflowService(IntentRouterService intentRouterService,
                                RetrievalService retrievalService,
                                ContextAssembler contextAssembler,
                                KnowledgeAssistant knowledgeAssistant,
                                ToolExecutionService toolExecutionService,
                                ApprovalService approvalService) {
        this.intentRouterService = intentRouterService;
        this.retrievalService = retrievalService;
        this.contextAssembler = contextAssembler;
        this.knowledgeAssistant = knowledgeAssistant;
        this.toolExecutionService = toolExecutionService;
        this.approvalService = approvalService;
    }

    public AgentResponse handle(String sessionId, String userInput) {
        AgentContext context = new AgentContext();
        context.setSessionId(sessionId);
        context.setUserInput(userInput);

        IntentDecision intentDecision = intentRouterService.route(userInput);
        context.setIntentDecision(intentDecision);

        return switch (intentDecision.intentType()) {
            case KNOWLEDGE_QA -> handleKnowledgeQa(context);
            case PROCESS_QA -> handleProcessQa(context);
            case ACTION_REQUEST -> handleActionRequest(context);
            case CLARIFICATION -> askForClarification(context);
        };
    }

    private AgentResponse handleKnowledgeQa(AgentContext context) {
        var matches = retrievalService.retrieve(context.getUserInput());
        String ragContext = contextAssembler.buildContext(matches);
        context.setRetrievedContext(ragContext);

        String answer = knowledgeAssistant.answer(context.getSessionId(), ragContext, context.getUserInput());
        context.setDraftAnswer(answer);

        return AgentResponse.answer(
                context.getIntentDecision().intentType().name(),
                answer,
                List.of(),
                false,
                null
        );
    }

    private AgentResponse handleProcessQa(AgentContext context) {
        var matches = retrievalService.retrieve(context.getUserInput());
        String ragContext = contextAssembler.buildContext(matches);
        context.setRetrievedContext(ragContext);

        String answer = knowledgeAssistant.answer(context.getSessionId(), ragContext, context.getUserInput());
        context.setDraftAnswer(answer);

        return AgentResponse.answer(
                context.getIntentDecision().intentType().name(),
                answer,
                List.of(),
                false,
                null
        );
    }

    private AgentResponse handleActionRequest(AgentContext context) {
        String userInput = context.getUserInput() == null ? "" : context.getUserInput();

        if (userInput.contains("工单")) {
            String approvalToken = approvalService.createPendingApproval(context);
            return AgentResponse.answer(
                    context.getIntentDecision().intentType().name(),
                    "我已识别到你希望创建工单。请确认是否继续执行？",
                    List.of("createSupportTicket"),
                    true,
                    approvalToken
            );
        }

        String routedDocumentType = context.getIntentDecision() == null ? null : context.getIntentDecision().documentType();
        if (routedDocumentType != null && !routedDocumentType.isBlank() && !"NONE".equalsIgnoreCase(routedDocumentType)) {
            String documentType = normalizeDocumentType(routedDocumentType);
            ToolCallResult result = toolExecutionService.executeDocumentList(documentType);
            return AgentResponse.answer(
                    context.getIntentDecision().intentType().name(),
                    result.result(),
                    List.of(result.toolName()),
                    false,
                    null
            );
        }

        return AgentResponse.answer(
                context.getIntentDecision().intentType().name(),
                "我识别到这是一个动作请求，但当前没有匹配到可执行工具。",
                List.of(),
                false,
                null
        );
    }

    private String normalizeDocumentType(String routedDocumentType) {
        try {
            return DocumentType.valueOf(routedDocumentType.trim().toUpperCase()).name();
        } catch (Exception ignored) {
            return DocumentType.TECH_TYPE.name();
        }
    }

    private AgentResponse askForClarification(AgentContext context) {
        return AgentResponse.answer(
                context.getIntentDecision().intentType().name(),
                "我暂时无法准确判断你的请求类型。你是想查询知识、查看流程，还是执行某个动作？",
                List.of(),
                false,
                null
        );
    }
}
