package org.weihua.model.workflow;

import java.io.Serializable;

public record ActionCommand(
        ActionType actionType,
        String documentType,
        String keyword,
        String documentId,
        String title,
        String content
) implements Serializable {}

