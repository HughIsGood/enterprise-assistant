package org.weihua.model.workflow;

import java.util.List;

public record AgentResponse(
        String intentType,
        String answer,
        List<String> usedTools,
        boolean approvalRequired,
        String approvalToken
) {
    public static AgentResponse answer(String intentType,
                                       String answer,
                                       List<String> usedTools,
                                       boolean approvalRequired,
                                       String approvalToken) {
        return new AgentResponse(intentType, answer, usedTools, approvalRequired, approvalToken);
    }
}
