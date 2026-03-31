package org.weihua.model.task;

import org.weihua.model.workflow.ActionCommand;

public record TaskApproval(
        String token,
        String taskId,
        int stepNo,
        ActionCommand actionCommand,
        boolean approved
) {
    public TaskApproval approve() {
        return new TaskApproval(token, taskId, stepNo, actionCommand, true);
    }
}
