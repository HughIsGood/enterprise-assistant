package org.weihua.repository.task;

import org.weihua.model.task.Task;
import org.weihua.model.task.TaskStatus;

import java.util.Optional;

public interface TaskRepository {

    void save(Task task);

    void update(Task task);

    void updateStatus(String taskId, TaskStatus status);

    Optional<Task> findByTaskId(String taskId);
}
