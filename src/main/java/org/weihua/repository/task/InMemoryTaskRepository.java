package org.weihua.repository.task;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.weihua.model.task.Task;
import org.weihua.model.task.TaskStatus;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("test")
public class InMemoryTaskRepository implements TaskRepository {

    private final Map<String, Task> tasks = new ConcurrentHashMap<>();

    @Override
    public void save(Task task) {
        tasks.put(task.getTaskId(), task);
    }

    @Override
    public void update(Task task) {
        tasks.put(task.getTaskId(), task);
    }

    @Override
    public void updateStatus(String taskId, TaskStatus status) {
        Task task = tasks.get(taskId);
        if (task != null) {
            task.setStatus(status);
        }
    }

    @Override
    public Optional<Task> findByTaskId(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }
}
