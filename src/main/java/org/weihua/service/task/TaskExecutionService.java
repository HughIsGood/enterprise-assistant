package org.weihua.service.task;

import org.springframework.stereotype.Service;
import org.weihua.model.tools.ToolCallResult;
import org.weihua.model.workflow.ActionCommand;
import org.weihua.service.tools.ToolExecutionService;

@Service
public class TaskExecutionService {

    private final ToolExecutionService toolExecutionService;

    public TaskExecutionService(ToolExecutionService toolExecutionService) {
        this.toolExecutionService = toolExecutionService;
    }

    public ToolCallResult execute(ActionCommand actionCommand) {
        return toolExecutionService.executeAction(actionCommand);
    }
}
