package org.weihua.repository.task;

import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import org.springframework.stereotype.Repository;
import org.weihua.model.task.TaskStep;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class TaskStepRepositoryImpl implements TaskStepRepository {

    private static final String TABLE_NAME = "agent_task_step";

    @Override
    public void save(TaskStep step) {
        if (step == null || step.getTaskId() == null || step.getTaskId().isBlank()) {
            throw new IllegalArgumentException("step and taskId must not be null");
        }

        Entity entity = Entity.create(TABLE_NAME)
                .set("task_id", step.getTaskId())
                .set("step_no", step.getStepNo())
                .set("thought_summary", step.getThoughtSummary())
                .set("action_type", step.getActionType())
                .set("action_payload_json", step.getActionPayloadJson())
                .set("observation_json", step.getObservationJson())
                .set("step_status", step.getStepStatus());

        try {
            Db.use().insert(entity);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save task step", e);
        }
    }

    @Override
    public List<TaskStep> findByTaskId(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            return List.of();
        }

        try {
            List<Entity> entities = Db.use().find(Entity.create(TABLE_NAME).set("task_id", taskId));
            return entities.stream()
                    .map(this::toStep)
                    .sorted(Comparator.comparingInt(TaskStep::getStepNo))
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query task steps", e);
        }
    }

    @Override
    public int nextStepNo(String taskId) {
        return findByTaskId(taskId).size() + 1;
    }

    private TaskStep toStep(Entity entity) {
        TaskStep step = new TaskStep();
        step.setTaskId(entity.getStr("task_id"));
        Integer stepNo = entity.getInt("step_no");
        step.setStepNo(stepNo == null ? 0 : stepNo);
        step.setThoughtSummary(entity.getStr("thought_summary"));
        step.setActionType(entity.getStr("action_type"));
        step.setActionPayloadJson(entity.getStr("action_payload_json"));
        step.setObservationJson(entity.getStr("observation_json"));
        step.setStepStatus(entity.getStr("step_status"));
        step.setCreateTime(toLocalDateTime(entity.get("create_time")));
        return step;
    }

    private LocalDateTime toLocalDateTime(Object dateValue) {
        if (dateValue == null) {
            return null;
        }
        if (dateValue instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (dateValue instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (dateValue instanceof java.util.Date date) {
            return new Timestamp(date.getTime()).toLocalDateTime();
        }
        return null;
    }
}
