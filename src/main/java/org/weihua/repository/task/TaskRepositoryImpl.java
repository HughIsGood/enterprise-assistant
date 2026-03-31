package org.weihua.repository.task;

import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import org.springframework.stereotype.Repository;
import org.weihua.model.task.Task;
import org.weihua.model.task.TaskStatus;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class TaskRepositoryImpl implements TaskRepository {

    private static final String TABLE_NAME = "agent_task";

    @Override
    public void save(Task task) {
        validateTask(task);

        Entity entity = Entity.create(TABLE_NAME)
                .set("task_id", task.getTaskId())
                .set("session_id", task.getSessionId())
                .set("user_id", task.getUserId())
                .set("goal", task.getGoal())
                .set("status", task.getStatus() == null ? TaskStatus.CREATED.name() : task.getStatus().name())
                .set("final_answer", task.getFinalAnswer())
                .set("error_message", task.getErrorMessage());

        try {
            Db.use().insert(entity);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save task", e);
        }
    }

    @Override
    public void update(Task task) {
        validateTask(task);

        Entity entity = Entity.create()
                .set("session_id", task.getSessionId())
                .set("user_id", task.getUserId())
                .set("goal", task.getGoal())
                .set("status", task.getStatus() == null ? null : task.getStatus().name())
                .set("final_answer", task.getFinalAnswer())
                .set("error_message", task.getErrorMessage());

        try {
            Db.use().update(entity, Entity.create(TABLE_NAME).set("task_id", task.getTaskId()));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update task", e);
        }
    }

    @Override
    public void updateStatus(String taskId, TaskStatus status) {
        if (isBlank(taskId) || status == null) {
            throw new IllegalArgumentException("taskId and status must not be null");
        }

        Entity entity = Entity.create().set("status", status.name());
        try {
            Db.use().update(entity, Entity.create(TABLE_NAME).set("task_id", taskId));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update task status", e);
        }
    }

    @Override
    public Optional<Task> findByTaskId(String taskId) {
        if (isBlank(taskId)) {
            return Optional.empty();
        }

        try {
            Entity entity = Db.use().get(Entity.create(TABLE_NAME).set("task_id", taskId));
            return Optional.ofNullable(entity).map(this::toTask);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query task by taskId", e);
        }
    }

    private Task toTask(Entity entity) {
        Task task = new Task();
        task.setTaskId(entity.getStr("task_id"));
        task.setSessionId(entity.getStr("session_id"));
        task.setUserId(entity.getStr("user_id"));
        task.setGoal(entity.getStr("goal"));
        String status = entity.getStr("status");
        if (!isBlank(status)) {
            task.setStatus(TaskStatus.valueOf(status));
        }
        task.setFinalAnswer(entity.getStr("final_answer"));
        task.setErrorMessage(entity.getStr("error_message"));
        task.setCreateTime(toLocalDateTime(entity.get("create_time")));
        task.setUpdateTime(toLocalDateTime(entity.get("update_time")));
        return task;
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

    private void validateTask(Task task) {
        if (task == null || isBlank(task.getTaskId())) {
            throw new IllegalArgumentException("task and taskId must not be null");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
