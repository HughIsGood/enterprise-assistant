package org.weihua.model.workflow;

import java.io.Serializable;
import java.util.List;

public record AgentResponse(
        String intentType,
        String answer,
        List<String> usedTools,
        boolean approvalRequired,
        String approvalToken,
        boolean clarificationRequired,
        String taskId
) implements Serializable {

    public static AgentResponse answer(String intentType,
                                       String answer,
                                       List<String> usedTools,
                                       boolean approvalRequired,
                                       String approvalToken) {
        return new AgentResponse(intentType, answer, usedTools, approvalRequired, approvalToken, false, null);
    }

    public static AgentResponse answer(String intentType,
                                       String answer,
                                       List<String> usedTools,
                                       boolean approvalRequired,
                                       String approvalToken,
                                       String taskId) {
        return new AgentResponse(intentType, answer, usedTools, approvalRequired, approvalToken, false, taskId);
    }

    public static AgentResponse clarification(String intentType,
                                              String answer,
                                              String taskId) {
        return new AgentResponse(intentType, answer, List.of(), false, null, true, taskId);
    }
}
