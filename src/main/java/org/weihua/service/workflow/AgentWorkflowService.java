package org.weihua.service.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.weihua.assistant.KnowledgeAssistant;
import org.weihua.model.document.DocumentType;
import org.weihua.model.tools.ToolCallResult;
import org.weihua.model.workflow.ActionCommand;
import org.weihua.model.workflow.ActionType;
import org.weihua.model.workflow.AgentContext;
import org.weihua.model.workflow.AgentResponse;
import org.weihua.model.workflow.IntentDecision;
import org.weihua.service.ApprovalService;
import org.weihua.service.IntentRouterService;
import org.weihua.service.rag.ContextAssembler;
import org.weihua.service.rag.RetrievalService;
import org.weihua.service.tools.ToolExecutionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AgentWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(AgentWorkflowService.class);

    private static final int KNOWLEDGE_QA_MAX_RESULTS = 3;
    private static final double KNOWLEDGE_QA_MIN_SCORE = 0.65;
    private static final int PROCESS_QA_MAX_RESULTS = 6;
    private static final double PROCESS_QA_MIN_SCORE = 0.5;

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
        log.info("Workflow route result: sessionId={}, intentType={}, reason={}",
                sessionId, intentDecision.intentType(), intentDecision.reason());

        return switch (intentDecision.intentType()) {
            case KNOWLEDGE_QA -> handleKnowledgeQa(context);
            case PROCESS_QA -> handleProcessQa(context);
            case ACTION_REQUEST -> handleActionRequest(context);
            case CLARIFICATION -> askForClarification(context);
        };
    }

    private AgentResponse handleKnowledgeQa(AgentContext context) {
        var matches = retrievalService.retrieve(
                context.getUserInput(),
                KNOWLEDGE_QA_MAX_RESULTS,
                KNOWLEDGE_QA_MIN_SCORE
        );
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
        var matches = retrievalService.retrieve(
                context.getUserInput(),
                PROCESS_QA_MAX_RESULTS,
                PROCESS_QA_MIN_SCORE
        );
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
        ActionCommand command = context.getIntentDecision() == null ? null : context.getIntentDecision().actionCommand();
        if (command == null || command.actionType() == null) {
            log.warn("Action request without executable actionCommand: sessionId={}, userInput={}",
                    context.getSessionId(), context.getUserInput());
            return AgentResponse.answer(
                    context.getIntentDecision().intentType().name(),
                    "已识别为动作请求，但未路由到可执行动作。",
                    List.of(),
                    false,
                    null
            );
        }

        ActionType actionType = command.actionType();
        log.info("Action dispatch: sessionId={}, actionType={}, command={}",
                context.getSessionId(), actionType, command);

        return switch (actionType) {
            case LIST_DOCUMENTS -> executeListDocuments(context, command);
            case SEARCH_DOCUMENTS -> executeSearchDocuments(context, command);
            case GET_DOCUMENT_DETAIL -> executeGetDocumentDetail(context, command);
            case CREATE_TICKET -> createTicketWithApproval(context, command);
        };
    }

    private AgentResponse executeListDocuments(AgentContext context, ActionCommand command) {
        String documentType = normalizeDocumentType(command.documentType());
        ActionCommand normalizedCommand = new ActionCommand(
                command.actionType(),
                documentType,
                command.keyword(),
                command.documentId(),
                command.title(),
                command.content()
        );
        ToolCallResult result = toolExecutionService.executeAction(normalizedCommand);
        return toolResultResponse(context, result);
    }

    private AgentResponse executeSearchDocuments(AgentContext context, ActionCommand command) {
        if (command.keyword() == null || command.keyword().isBlank()) {
            return AgentResponse.answer(
                    context.getIntentDecision().intentType().name(),
                    "SEARCH_DOCUMENTS 缺少 keyword。",
                    List.of(),
                    false,
                    null
            );
        }
        ToolCallResult result = toolExecutionService.executeAction(command);
        return toolResultResponse(context, result);
    }

    private AgentResponse executeGetDocumentDetail(AgentContext context, ActionCommand command) {
        if (command.documentId() == null || command.documentId().isBlank()) {
            return AgentResponse.answer(
                    context.getIntentDecision().intentType().name(),
                    "GET_DOCUMENT_DETAIL 缺少 documentId。",
                    List.of(),
                    false,
                    null
            );
        }
        ToolCallResult result = toolExecutionService.executeAction(command);
        return toolResultResponse(context, result);
    }

    private AgentResponse createTicketWithApproval(AgentContext context, ActionCommand command) {
        Map<String, Object> params = context.getToolResults() == null ? new HashMap<>() : context.getToolResults();
        params.put("title", defaultIfBlank(command.title(), "用户支持请求"));
        params.put("content", defaultIfBlank(command.content(), context.getUserInput()));
        context.setToolResults(params);

        String approvalToken = approvalService.createPendingApproval(context);
        log.info("Approval token created: sessionId={}, token={}", context.getSessionId(), approvalToken);

        return AgentResponse.answer(
                context.getIntentDecision().intentType().name(),
                "创建工单需要审批确认，是否继续？",
                List.of(toolExecutionService.resolveToolName(ActionType.CREATE_TICKET)),
                true,
                approvalToken
        );
    }

    private AgentResponse toolResultResponse(AgentContext context, ToolCallResult result) {
        return AgentResponse.answer(
                context.getIntentDecision().intentType().name(),
                result.result(),
                List.of(result.toolName()),
                false,
                null
        );
    }

    private String normalizeDocumentType(String raw) {
        if (raw == null || raw.isBlank()) {
            return DocumentType.TECH_TYPE.name();
        }
        try {
            return DocumentType.valueOf(raw.trim().toUpperCase()).name();
        } catch (Exception ignored) {
            return DocumentType.TECH_TYPE.name();
        }
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
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
