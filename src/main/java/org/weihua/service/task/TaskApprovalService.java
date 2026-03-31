package org.weihua.service.task;

import org.springframework.stereotype.Service;
import org.weihua.model.task.TaskApproval;
import org.weihua.model.workflow.ActionCommand;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TaskApprovalService {

    private final Map<String, TaskApproval> pendingApprovals = new ConcurrentHashMap<>();

    public String createPendingApproval(String taskId, int stepNo, ActionCommand actionCommand) {
        String token = UUID.randomUUID().toString();
        pendingApprovals.put(token, new TaskApproval(token, taskId, stepNo, actionCommand, false));
        return token;
    }

    public TaskApproval approve(String token) {
        TaskApproval approval = pendingApprovals.get(token);
        if (approval == null) {
            return null;
        }
        TaskApproval approved = approval.approve();
        pendingApprovals.put(token, approved);
        return approved;
    }
}
