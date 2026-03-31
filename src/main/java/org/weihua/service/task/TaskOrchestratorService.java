package org.weihua.service.task;

import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.StateGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.weihua.assistant.KnowledgeAssistant;
import org.weihua.model.document.DocumentType;
import org.weihua.model.task.Task;
import org.weihua.model.task.TaskApproval;
import org.weihua.model.task.TaskGraphState;
import org.weihua.model.task.TaskStatus;
import org.weihua.model.task.TaskStep;
import org.weihua.model.tools.ToolCallResult;
import org.weihua.model.workflow.ActionCommand;
import org.weihua.model.workflow.ActionType;
import org.weihua.model.workflow.AgentResponse;
import org.weihua.model.workflow.IntentDecision;
import org.weihua.model.workflow.IntentType;
import org.weihua.repository.task.TaskRepository;
import org.weihua.repository.task.TaskStepRepository;
import org.weihua.service.rag.ContextAssembler;
import org.weihua.service.rag.RetrievalService;
import org.weihua.service.tools.ToolExecutionService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Service
public class TaskOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(TaskOrchestratorService.class);
    private static final int KNOWLEDGE_QA_MAX_RESULTS = 3;
    private static final double KNOWLEDGE_QA_MIN_SCORE = 0.65;
    private static final int PROCESS_QA_MAX_RESULTS = 6;
    private static final double PROCESS_QA_MIN_SCORE = 0.5;
    private static final int MAX_STEPS = 5;

    private static final String NODE_PLAN = "plan";
    private static final String NODE_ACTION = "action";
    private static final String NODE_APPROVAL = "approval";
    private static final String NODE_KNOWLEDGE = "knowledgeQa";
    private static final String NODE_PROCESS = "processQa";
    private static final String NODE_CLARIFICATION = "clarification";

    private final TaskRepository taskRepository;
    private final TaskStepRepository taskStepRepository;
    private final TaskPlannerService taskPlannerService;
    private final TaskExecutionService taskExecutionService;
    private final TaskApprovalService taskApprovalService;
    private final ToolExecutionService toolExecutionService;
    private final RetrievalService retrievalService;
    private final ContextAssembler contextAssembler;
    private final KnowledgeAssistant knowledgeAssistant;

    private final CompiledGraph<TaskGraphState> taskGraph;

    public TaskOrchestratorService(TaskRepository taskRepository,
                                   TaskStepRepository taskStepRepository,
                                   TaskPlannerService taskPlannerService,
                                   TaskExecutionService taskExecutionService,
                                   TaskApprovalService taskApprovalService,
                                   ToolExecutionService toolExecutionService,
                                   RetrievalService retrievalService,
                                   ContextAssembler contextAssembler,
                                   KnowledgeAssistant knowledgeAssistant) {
        this.taskRepository = taskRepository;
        this.taskStepRepository = taskStepRepository;
        this.taskPlannerService = taskPlannerService;
        this.taskExecutionService = taskExecutionService;
        this.taskApprovalService = taskApprovalService;
        this.toolExecutionService = toolExecutionService;
        this.retrievalService = retrievalService;
        this.contextAssembler = contextAssembler;
        this.knowledgeAssistant = knowledgeAssistant;
        this.taskGraph = buildGraph();
    }

    public AgentResponse askSync(String userId, String message) {
        Task task = newTask(userId, message);
        taskRepository.save(task);
        return runGraph(task, message, "", 0);
    }

    public AgentResponse approveAndContinue(String token) {
        TaskApproval approval = taskApprovalService.approve(token);
        if (approval == null) {
            return AgentResponse.answer("ACTION_REQUEST", "未找到待确认操作。", List.of(), false, null);
        }

        Task task = taskRepository.findByTaskId(approval.taskId()).orElse(null);
        if (task == null) {
            return AgentResponse.answer("ACTION_REQUEST", "任务不存在或已失效。", List.of(), false, null);
        }

        ActionCommand normalizedCommand = normalizeActionCommand(approval.actionCommand());
        ToolCallResult result = taskExecutionService.execute(normalizedCommand);
        appendStep(task.getTaskId(), null, normalizedCommand, result.result(), "DONE");

        return completeTask(
                task,
                IntentType.ACTION_REQUEST.name(),
                result.result(),
                List.of(result.toolName())
        );
    }

    public Task getTask(String taskId) {
        return taskRepository.findByTaskId(taskId).orElse(null);
    }

    public List<TaskStep> getTaskSteps(String taskId) {
        return taskStepRepository.findByTaskId(taskId);
    }

    private CompiledGraph<TaskGraphState> buildGraph() {
        try {
            StateGraph<TaskGraphState> graph = new StateGraph<>(TaskGraphState.SCHEMA, TaskGraphState::new)
                    .addNode(NODE_PLAN, node_async(this::planNode))
                    .addNode(NODE_ACTION, node_async(this::actionNode))
                    .addNode(NODE_APPROVAL, node_async(this::approvalNode))
                    .addNode(NODE_KNOWLEDGE, node_async(this::knowledgeQaNode))
                    .addNode(NODE_PROCESS, node_async(this::processQaNode))
                    .addNode(NODE_CLARIFICATION, node_async(this::clarificationNode))
                    .addEdge(StateGraph.START, NODE_PLAN)
                    .addConditionalEdges(
                            NODE_PLAN,
                            edge_async(this::routeAfterPlan),
                            Map.of(
                                    "ACTION", NODE_ACTION,
                                    "APPROVAL", NODE_APPROVAL,
                                    "KNOWLEDGE", NODE_KNOWLEDGE,
                                    "PROCESS", NODE_PROCESS,
                                    "CLARIFICATION", NODE_CLARIFICATION,
                                    "END", StateGraph.END
                            )
                    )
                    .addEdge(NODE_ACTION, NODE_PLAN)
                    .addEdge(NODE_APPROVAL, StateGraph.END)
                    .addEdge(NODE_KNOWLEDGE, StateGraph.END)
                    .addEdge(NODE_PROCESS, StateGraph.END)
                    .addEdge(NODE_CLARIFICATION, StateGraph.END);
            return graph.compile();
        } catch (Exception e) {
            throw new IllegalStateException("Task graph init failed", e);
        }
    }

    private AgentResponse runGraph(Task task, String goal, String lastObservation, int executedSteps) {
        try {
            Map<String, Object> input = new HashMap<>();
            input.put(TaskGraphState.KEY_TASK, task);
            input.put(TaskGraphState.KEY_GOAL, goal);
            input.put(TaskGraphState.KEY_LAST_OBSERVATION, lastObservation == null ? "" : lastObservation);
            input.put(TaskGraphState.KEY_EXECUTED_STEPS, executedSteps);

            Optional<TaskGraphState> output = taskGraph.invoke(input);
            TaskGraphState state = output.orElse(null);
            if (state == null || state.response() == null) {
                return failTask(task, "任务执行失败：图执行结果为空");
            }
            return state.response();
        } catch (Exception e) {
            log.error("Task graph execute error: taskId={}, msg={}", task.getTaskId(), e.getMessage(), e);
            return failTask(task, "任务执行异常：" + e.getMessage());
        }
    }

    private Map<String, Object> planNode(TaskGraphState state) {
        Task task = state.task();
        if (task == null) {
            return stateUpdateWithResponse(AgentResponse.answer(IntentType.ACTION_REQUEST.name(), "任务不存在", List.of(), false, null));
        }

        if (state.executedSteps() >= MAX_STEPS) {
            String answer = state.lastObservation().isBlank() ? "任务超过最大执行步数" : state.lastObservation();
            AgentResponse response = completeTask(task, IntentType.ACTION_REQUEST.name(), answer, List.of(state.lastToolName()));
            return stateUpdateWithResponse(response);
        }

        String plannerInput = buildPlannerInput(task, state.goal(), state.lastObservation());
        IntentDecision decision = safePlan(plannerInput);
        if (decision == null) {
            AgentResponse response = failTask(task, "任务规划失败");
            return stateUpdateWithResponse(response);
        }

        ActionCommand normalizedCommand = normalizeActionCommand(decision.actionCommand());
        appendStep(task.getTaskId(), decision, normalizedCommand, state.lastObservation(), "PLANNED");

        Map<String, Object> update = new HashMap<>();
        update.put(TaskGraphState.KEY_DECISION, decision);
        update.put(TaskGraphState.KEY_ACTION_COMMAND, normalizedCommand);
        return update;
    }

    private String routeAfterPlan(TaskGraphState state) {
        if (state.response() != null) {
            return "END";
        }

        IntentDecision decision = state.decision();
        if (decision == null || decision.intentType() == null) {
            return "CLARIFICATION";
        }

        if (decision.intentType() == IntentType.KNOWLEDGE_QA) {
            return "KNOWLEDGE";
        }
        if (decision.intentType() == IntentType.PROCESS_QA) {
            return "PROCESS";
        }
        if (decision.intentType() == IntentType.CLARIFICATION) {
            return "CLARIFICATION";
        }
        if (decision.intentType() != IntentType.ACTION_REQUEST) {
            return "CLARIFICATION";
        }

        ActionCommand command = state.actionCommand();
        if (command == null || command.actionType() == null) {
            return "CLARIFICATION";
        }

        if (command.actionType() == ActionType.CREATE_TICKET) {
            return "APPROVAL";
        }
        return "ACTION";
    }

    private Map<String, Object> actionNode(TaskGraphState state) {
        Task task = state.task();
        ActionCommand command = state.actionCommand();

        if (task == null) {
            return stateUpdateWithResponse(AgentResponse.answer(IntentType.ACTION_REQUEST.name(), "任务不存在", List.of(), false, null));
        }
        if (command == null || command.actionType() == null) {
            return stateUpdateWithResponse(failTask(task, "动作请求缺少 actionCommand"));
        }

        ToolCallResult result = taskExecutionService.execute(command);
        appendStep(task.getTaskId(), state.decision(), command, result.result(), "DONE");

        Map<String, Object> update = new HashMap<>();
        update.put(TaskGraphState.KEY_LAST_OBSERVATION, result.result());
        update.put(TaskGraphState.KEY_LAST_TOOL_NAME, result.toolName());
        update.put(TaskGraphState.KEY_EXECUTED_STEPS, state.executedSteps() + 1);
        return update;
    }

    private Map<String, Object> approvalNode(TaskGraphState state) {
        Task task = state.task();
        ActionCommand command = state.actionCommand();
        if (task == null || command == null || command.actionType() == null) {
            return stateUpdateWithResponse(AgentResponse.answer(IntentType.ACTION_REQUEST.name(), "审批动作参数缺失", List.of(), false, null));
        }

        int stepNo = taskStepRepository.nextStepNo(task.getTaskId());
        String token = taskApprovalService.createPendingApproval(task.getTaskId(), stepNo, command);

        task.setStatus(TaskStatus.WAITING_APPROVAL);
        task.setUpdateTime(LocalDateTime.now());
        taskRepository.update(task);

        AgentResponse response = AgentResponse.answer(
                IntentType.ACTION_REQUEST.name(),
                "创建工单需要审批确认，是否继续？",
                List.of(toolExecutionService.resolveToolName(ActionType.CREATE_TICKET)),
                true,
                token
        );
        return stateUpdateWithResponse(response);
    }

    private Map<String, Object> knowledgeQaNode(TaskGraphState state) {
        Task task = state.task();
        if (task == null) {
            return stateUpdateWithResponse(AgentResponse.answer(IntentType.KNOWLEDGE_QA.name(), "任务不存在", List.of(), false, null));
        }

        var matches = retrievalService.retrieve(state.goal(), KNOWLEDGE_QA_MAX_RESULTS, KNOWLEDGE_QA_MIN_SCORE);
        String ragContext = contextAssembler.buildContext(matches);
        String answer = knowledgeAssistant.answer(task.getSessionId(), ragContext, state.goal());
        AgentResponse response = completeTask(task, IntentType.KNOWLEDGE_QA.name(), answer, List.of());
        return stateUpdateWithResponse(response);
    }

    private Map<String, Object> processQaNode(TaskGraphState state) {
        Task task = state.task();
        if (task == null) {
            return stateUpdateWithResponse(AgentResponse.answer(IntentType.PROCESS_QA.name(), "任务不存在", List.of(), false, null));
        }

        var matches = retrievalService.retrieve(state.goal(), PROCESS_QA_MAX_RESULTS, PROCESS_QA_MIN_SCORE);
        String ragContext = contextAssembler.buildContext(matches);
        String answer = knowledgeAssistant.answer(task.getSessionId(), ragContext, state.goal());
        AgentResponse response = completeTask(task, IntentType.PROCESS_QA.name(), answer, List.of());
        return stateUpdateWithResponse(response);
    }

    private Map<String, Object> clarificationNode(TaskGraphState state) {
        Task task = state.task();
        if (task == null) {
            return stateUpdateWithResponse(AgentResponse.answer(IntentType.CLARIFICATION.name(), "请补充更多信息", List.of(), false, null));
        }
        AgentResponse response = completeTask(
                task,
                IntentType.CLARIFICATION.name(),
                "我暂时无法准确判断你的请求类型。请补充更多信息。",
                List.of()
        );
        return stateUpdateWithResponse(response);
    }

    private Map<String, Object> stateUpdateWithResponse(AgentResponse response) {
        Map<String, Object> update = new HashMap<>();
        update.put(TaskGraphState.KEY_RESPONSE, response);
        return update;
    }

    private IntentDecision safePlan(String plannerInput) {
        try {
            return taskPlannerService.planNext(plannerInput);
        } catch (Exception e) {
            log.warn("Task planner failed: {}", e.getMessage());
            return null;
        }
    }

    private String buildPlannerInput(Task task, String goal, String latestObservation) {
        StringBuilder sb = new StringBuilder();
        sb.append("任务目标: ").append(goal == null ? "" : goal).append("\\n");

        List<TaskStep> history = taskStepRepository.findByTaskId(task.getTaskId());
        if (!history.isEmpty()) {
            sb.append("历史步骤:\\n");
            for (TaskStep step : history) {
                sb.append("- step=").append(step.getStepNo())
                        .append(", action=").append(step.getActionType())
                        .append(", status=").append(step.getStepStatus())
                        .append(", observation=").append(step.getObservationJson())
                        .append("\\n");
            }
        }

        if (latestObservation != null && !latestObservation.isBlank()) {
            sb.append("最新观察: ").append(latestObservation).append("\\n");
        }
        return sb.toString();
    }

    private AgentResponse completeTask(Task task, String intentType, String answer, List<String> usedTools) {
        task.setStatus(TaskStatus.COMPLETED);
        task.setFinalAnswer(answer);
        task.setUpdateTime(LocalDateTime.now());
        taskRepository.update(task);
        return AgentResponse.answer(intentType, answer, usedTools, false, null);
    }

    private AgentResponse failTask(Task task, String message) {
        task.setStatus(TaskStatus.FAILED);
        task.setErrorMessage(message);
        task.setUpdateTime(LocalDateTime.now());
        taskRepository.update(task);
        log.warn("Task failed: taskId={}, reason={}", task.getTaskId(), message);
        return AgentResponse.answer(IntentType.ACTION_REQUEST.name(), message, List.of(), false, null);
    }

    private void appendStep(String taskId,
                            IntentDecision decision,
                            ActionCommand command,
                            String observation,
                            String status) {
        TaskStep step = new TaskStep();
        step.setTaskId(taskId);
        step.setStepNo(taskStepRepository.nextStepNo(taskId));
        step.setThoughtSummary(decision == null ? "" : decision.reason());
        step.setActionType(command == null || command.actionType() == null ? "NONE" : command.actionType().name());
        step.setActionPayloadJson(String.valueOf(command));
        step.setObservationJson(observation == null ? "" : observation);
        step.setStepStatus(status);
        step.setCreateTime(LocalDateTime.now());
        taskStepRepository.save(step);
    }

    private ActionCommand normalizeActionCommand(ActionCommand command) {
        if (command == null || command.actionType() == null) {
            return command;
        }
        if (command.actionType() != ActionType.LIST_DOCUMENTS) {
            return command;
        }

        String documentType = command.documentType();
        if (documentType == null || documentType.isBlank()) {
            documentType = DocumentType.TECH_TYPE.name();
        }

        return new ActionCommand(
                command.actionType(),
                documentType,
                command.keyword(),
                command.documentId(),
                command.title(),
                command.content()
        );
    }

    private Task newTask(String userId, String goal) {
        Task task = new Task();
        task.setTaskId(UUID.randomUUID().toString().replace("-", ""));
        task.setSessionId(userId);
        task.setUserId(userId);
        task.setGoal(goal);
        task.setStatus(TaskStatus.RUNNING);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        return task;
    }

    private int countDoneSteps(String taskId) {
        return (int) taskStepRepository.findByTaskId(taskId).stream()
                .filter(step -> "DONE".equals(step.getStepStatus()))
                .count();
    }
}
