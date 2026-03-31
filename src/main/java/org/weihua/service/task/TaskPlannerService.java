package org.weihua.service.task;

import org.springframework.stereotype.Service;
import org.weihua.model.workflow.IntentDecision;
import org.weihua.service.IntentRouterService;

@Service
public class TaskPlannerService {

    private final IntentRouterService intentRouterService;

    public TaskPlannerService(IntentRouterService intentRouterService) {
        this.intentRouterService = intentRouterService;
    }

    public IntentDecision planNext(String goal) {
        return intentRouterService.route(goal);
    }
}
