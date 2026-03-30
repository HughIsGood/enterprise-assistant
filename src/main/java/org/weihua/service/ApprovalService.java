package org.weihua.service;

import org.springframework.stereotype.Service;
import org.weihua.model.workflow.AgentContext;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ApprovalService {

    private final Map<String, AgentContext> pendingApprovals = new ConcurrentHashMap<>();

    public String createPendingApproval(AgentContext context) {
        String token = UUID.randomUUID().toString();
        context.setApprovalRequired(true);
        context.setApproved(false);
        context.setApprovalToken(token);
        pendingApprovals.put(token, context);
        return token;
    }

    public AgentContext approve(String token) {
        AgentContext context = pendingApprovals.get(token);
        if (context != null) {
            context.setApproved(true);
        }
        return context;
    }

    public AgentContext getPending(String token) {
        return pendingApprovals.get(token);
    }
}
