package org.weihua.service.tools;

import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Service;
import org.weihua.model.tools.ToolCallResult;
import org.weihua.model.workflow.ActionCommand;
import org.weihua.model.workflow.ActionType;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Service
public class ToolExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutionService.class);

    private final Map<String, RegisteredTool> toolRegistry = new HashMap<>();

    public ToolExecutionService(DocumentTool documentTool, TicketTool ticketTool) {
        registerTools(documentTool);
        registerTools(ticketTool);
        log.info("Registered tools: {}", toolRegistry.keySet());
    }

    public ToolCallResult executeDocumentList(String documentType) {
        log.info("Tool call start: listDocumentsByType, documentType={}", documentType);
        return invokeByName("listDocumentsByType", documentType);
    }

    public ToolCallResult executeDocumentSearch(String keyword) {
        log.info("Tool call start: searchDocumentNames, keyword={}", keyword);
        return invokeByName("searchDocumentNames", keyword);
    }

    public ToolCallResult executeDocumentDetail(String documentId) {
        log.info("Tool call start: getDocumentDetail, documentId={}", documentId);
        return invokeByName("getDocumentDetail", documentId);
    }

    public ToolCallResult executeCreateTicket(String title, String content) {
        log.info("Tool call start: createSupportTicket, title={}, contentLength={}", title, safeLength(content));
        return invokeByName("createSupportTicket", title, content);
    }

    public ToolCallResult executeAction(ActionCommand command) {
        if (command == null || command.actionType() == null) {
            throw new IllegalArgumentException("ActionCommand and actionType must not be null");
        }

        return switch (command.actionType()) {
            case LIST_DOCUMENTS -> executeDocumentList(command.documentType());
            case SEARCH_DOCUMENTS -> executeDocumentSearch(command.keyword());
            case GET_DOCUMENT_DETAIL -> executeDocumentDetail(command.documentId());
            case CREATE_TICKET -> executeCreateTicket(command.title(), command.content());
        };
    }

    public String resolveToolName(ActionType actionType) {
        if (actionType == null) {
            return "";
        }
        return switch (actionType) {
            case LIST_DOCUMENTS -> "listDocumentsByType";
            case SEARCH_DOCUMENTS -> "searchDocumentNames";
            case GET_DOCUMENT_DETAIL -> "getDocumentDetail";
            case CREATE_TICKET -> "createSupportTicket";
        };
    }

    private ToolCallResult invokeByName(String toolName, Object... args) {
        RegisteredTool registeredTool = toolRegistry.get(toolName);
        if (registeredTool == null) {
            throw new IllegalStateException("Tool not registered: " + toolName);
        }
        try {
            Object raw = registeredTool.method().invoke(registeredTool.owner(), args);
            String result = raw == null ? "" : String.valueOf(raw);
            log.info("Tool call done: {}, resultLength={}", toolName, safeLength(result));
            return new ToolCallResult(toolName, result);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to invoke tool: " + toolName, e);
        }
    }

    private void registerTools(Object bean) {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        for (Method method : targetClass.getMethods()) {
            if (method.getAnnotation(Tool.class) == null) {
                continue;
            }
            try {
                Method invokable = bean.getClass().getMethod(method.getName(), method.getParameterTypes());
                toolRegistry.put(method.getName(), new RegisteredTool(bean, invokable));
            } catch (NoSuchMethodException ignored) {
                toolRegistry.put(method.getName(), new RegisteredTool(bean, method));
            }
        }
    }

    private int safeLength(String text) {
        return text == null ? 0 : text.length();
    }

    private record RegisteredTool(Object owner, Method method) {
    }
}
