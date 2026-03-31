package org.weihua.repository.task;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.weihua.model.task.TaskStep;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("test")
public class InMemoryTaskStepRepository implements TaskStepRepository {

    private final Map<String, List<TaskStep>> stepsByTaskId = new ConcurrentHashMap<>();

    @Override
    public void save(TaskStep step) {
        stepsByTaskId.computeIfAbsent(step.getTaskId(), k -> new ArrayList<>()).add(step);
    }

    @Override
    public List<TaskStep> findByTaskId(String taskId) {
        List<TaskStep> steps = stepsByTaskId.getOrDefault(taskId, List.of());
        return steps.stream().sorted(Comparator.comparingInt(TaskStep::getStepNo)).toList();
    }

    @Override
    public int nextStepNo(String taskId) {
        return findByTaskId(taskId).size() + 1;
    }
}
