package org.weihua.model.workflow;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AgentContext {

    private String sessionId;
    private String userInput;
    private IntentDecision intentDecision;
    private String retrievedContext;
    private String draftAnswer;
    private boolean approvalRequired;
    private boolean approved;
    private String approvalToken;
    private List<String> usedTools;
    private Map<String, Object> toolResults;

    // getter/setter
}