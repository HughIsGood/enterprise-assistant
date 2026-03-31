package org.weihua.service.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.weihua.assistant.KnowledgeAssistant;
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
                    "\u5df2\u8bc6\u522b\u4e3a\u52a8\u4f5c\u8bf7\u6c42\uff0c\u4f46\u672a\u8def\u7531\u5230\u53ef\u6267\u884c\u52a8\u4f5c\u3002",
                    List.of(),
                    false,
                    null
            );
        }

        ActionType actionType = command.actionType();
        log.info("Action dispatch: sessionId={}, actionType={}, command={}",
                context.getSessionId(), actionType, command);

        if (actionType != ActionType.CREATE_TICKET) {
            return AgentResponse.answer(
                    context.getIntentDecision().intentType().name(),
                    "\u5f53\u524d ACTION_REQUEST \u4ec5\u652f\u6301 CREATE_TICKET \u52a8\u4f5c\u3002",
                    List.of(),
                    false,
                    null
            );
        }
        return createTicketWithApproval(context, command);
    }

    private AgentResponse createTicketWithApproval(AgentContext context, ActionCommand command) {
        Map<String, Object> params = context.getToolResults() == null ? new HashMap<>() : context.getToolResults();
        params.put("title", defaultIfBlank(command.title(), "\u7528\u6237\u652f\u6301\u8bf7\u6c42"));
        params.put("content", defaultIfBlank(command.content(), context.getUserInput()));
        context.setToolResults(params);

        String approvalToken = approvalService.createPendingApproval(context);
        log.info("Approval token created: sessionId={}, token={}", context.getSessionId(), approvalToken);

        return AgentResponse.answer(
                context.getIntentDecision().intentType().name(),
                "\u521b\u5efa\u5de5\u5355\u9700\u8981\u5ba1\u6279\u786e\u8ba4\uff0c\u662f\u5426\u7ee7\u7eed\uff1f",
                List.of(toolExecutionService.resolveToolName(ActionType.CREATE_TICKET)),
                true,
                approvalToken
        );
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private AgentResponse askForClarification(AgentContext context) {
        return AgentResponse.answer(
                context.getIntentDecision().intentType().name(),
                "\u6211\u6682\u65f6\u65e0\u6cd5\u51c6\u786e\u5224\u65ad\u4f60\u7684\u8bf7\u6c42\u7c7b\u578b\u3002\u4f60\u662f\u60f3\u67e5\u8be2\u77e5\u8bc6\u3001\u67e5\u770b\u6d41\u7a0b\uff0c\u8fd8\u662f\u6267\u884c\u67d0\u4e2a\u52a8\u4f5c\uff1f",
                List.of(),
                false,
                null
        );
    }
}
