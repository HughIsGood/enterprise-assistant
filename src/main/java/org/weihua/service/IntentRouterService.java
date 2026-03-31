package org.weihua.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.weihua.assistant.RouterAssistant;
import org.weihua.model.workflow.ActionCommand;
import org.weihua.model.workflow.ActionType;
import org.weihua.model.workflow.IntentDecision;
import org.weihua.model.workflow.IntentType;

import java.util.HashMap;
import java.util.Map;

@Service
public class IntentRouterService {

    private static final Logger log = LoggerFactory.getLogger(IntentRouterService.class);
    private final RouterAssistant routerAssistant;

    public IntentRouterService(RouterAssistant routerAssistant) {
        this.routerAssistant = routerAssistant;
    }

    public IntentDecision route(String userInput) {
        String result = routerAssistant.route(userInput);
        log.info("Router raw result: {}", sanitize(result));

        Map<String, String> fields = parseFields(result);

        IntentType intentType = parseIntentType(fields.getOrDefault("intentType", "CLARIFICATION"));
        String reason = normalizeNullable(fields.get("reason"));

        ActionCommand actionCommand = null;
        if (intentType == IntentType.ACTION_REQUEST) {
            actionCommand = new ActionCommand(
                    parseActionType(fields.getOrDefault("actionType", "NONE")),
                    normalizeNullable(fields.get("documentType")),
                    normalizeNullable(fields.get("keyword")),
                    normalizeNullable(fields.get("documentId")),
                    normalizeNullable(fields.get("title")),
                    normalizeNullable(fields.get("content"))
            );
        }

        IntentDecision decision = new IntentDecision(intentType, reason, actionCommand);
        log.info("Router decision: intentType={}, reason={}, actionCommand={}",
                decision.intentType(), decision.reason(), decision.actionCommand());
        return decision;
    }

    private Map<String, String> parseFields(String result) {
        Map<String, String> fields = new HashMap<>();
        if (result == null || result.isBlank()) {
            return fields;
        }

        String[] lines = result.split("\\n");
        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine.trim();
            int idx = line.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = line.substring(0, idx).trim();
            String value = line.substring(idx + 1).trim();
            fields.put(key, value);
        }
        return fields;
    }

    private IntentType parseIntentType(String raw) {
        try {
            return IntentType.valueOf(raw.trim().toUpperCase());
        } catch (Exception ignored) {
            return IntentType.CLARIFICATION;
        }
    }

    private ActionType parseActionType(String raw) {
        if (raw == null || raw.isBlank() || "NONE".equalsIgnoreCase(raw.trim())) {
            return null;
        }
        try {
            return ActionType.valueOf(raw.trim().toUpperCase());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String normalizeNullable(String raw) {
        if (raw == null || raw.isBlank() || "NONE".equalsIgnoreCase(raw.trim())) {
            return null;
        }
        return raw.trim();
    }

    private String sanitize(String text) {
        if (text == null) {
            return "null";
        }
        return text.replace("\n", "\\n");
    }
}
