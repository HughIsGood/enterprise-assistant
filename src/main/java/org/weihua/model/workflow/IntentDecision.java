package org.weihua.model.workflow;

import java.io.Serializable;

public record IntentDecision(
        IntentType intentType,
        String reason,
        ActionCommand actionCommand
) implements Serializable {}

