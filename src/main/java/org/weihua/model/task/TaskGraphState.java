package org.weihua.model.task;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;
import org.weihua.model.workflow.ActionCommand;
import org.weihua.model.workflow.AgentResponse;
import org.weihua.model.workflow.IntentDecision;

import java.util.Map;

public class TaskGraphState extends AgentState {

    public static final String KEY_TASK = "task";
    public static final String KEY_GOAL = "goal";
    public static final String KEY_LAST_OBSERVATION = "lastObservation";
    public static final String KEY_DECISION = "decision";
    public static final String KEY_ACTION_COMMAND = "actionCommand";
    public static final String KEY_EXECUTED_STEPS = "executedSteps";
    public static final String KEY_LAST_TOOL_NAME = "lastToolName";
    public static final String KEY_RESPONSE = "response";

    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            KEY_TASK, Channels.base(() -> ""),
            KEY_GOAL, Channels.base(() -> ""),
            KEY_LAST_OBSERVATION, Channels.base(() -> ""),
            KEY_DECISION, Channels.base(() -> ""),
            KEY_ACTION_COMMAND, Channels.base(() -> ""),
            KEY_EXECUTED_STEPS, Channels.base(() -> 0),
            KEY_LAST_TOOL_NAME, Channels.base(() -> ""),
            KEY_RESPONSE, Channels.base(() -> "")
    );

    public TaskGraphState(Map<String, Object> initData) {
        super(initData);
    }

    public Task task() {
        return value(KEY_TASK).filter(Task.class::isInstance).map(Task.class::cast).orElse(null);
    }

    public String goal() {
        return value(KEY_GOAL).filter(String.class::isInstance).map(String.class::cast).orElse("");
    }

    public String lastObservation() {
        return value(KEY_LAST_OBSERVATION).filter(String.class::isInstance).map(String.class::cast).orElse("");
    }

    public IntentDecision decision() {
        return value(KEY_DECISION).filter(IntentDecision.class::isInstance).map(IntentDecision.class::cast).orElse(null);
    }

    public ActionCommand actionCommand() {
        return value(KEY_ACTION_COMMAND).filter(ActionCommand.class::isInstance).map(ActionCommand.class::cast).orElse(null);
    }

    public int executedSteps() {
        return value(KEY_EXECUTED_STEPS).filter(Integer.class::isInstance).map(Integer.class::cast).orElse(0);
    }

    public String lastToolName() {
        return value(KEY_LAST_TOOL_NAME).filter(String.class::isInstance).map(String.class::cast).orElse("");
    }

    public AgentResponse response() {
        return value(KEY_RESPONSE).filter(AgentResponse.class::isInstance).map(AgentResponse.class::cast).orElse(null);
    }
}
