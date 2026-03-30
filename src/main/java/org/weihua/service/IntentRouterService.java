package org.weihua.service;

import org.springframework.stereotype.Service;
import org.weihua.assistant.RouterAssistant;
import org.weihua.model.workflow.IntentDecision;
import org.weihua.model.workflow.IntentType;

@Service
public class IntentRouterService {

    private final RouterAssistant routerAssistant;

    public IntentRouterService(RouterAssistant routerAssistant) {
        this.routerAssistant = routerAssistant;
    }

    public IntentDecision route(String userInput) {
        String result = routerAssistant.route(userInput);

        String[] lines = result.split("\\n");
        String intent = "CLARIFICATION";
        String reason = "";
        String documentType = "NONE";

        for (String line : lines) {
            if (line.startsWith("intentType=")) {
                intent = line.substring("intentType=".length()).trim();
            } else if (line.startsWith("reason=")) {
                reason = line.substring("reason=".length()).trim();
            } else if (line.startsWith("documentType=")) {
                documentType = line.substring("documentType=".length()).trim();
            }
        }

        return new IntentDecision(IntentType.valueOf(intent), reason, documentType);
    }
}
