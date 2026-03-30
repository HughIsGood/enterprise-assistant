package org.weihua.model.workflow;

public record IntentDecision(
        IntentType intentType,
        String reason,
        String documentType
) {}
