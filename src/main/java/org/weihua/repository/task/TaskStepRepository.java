package org.weihua.repository.task;

import org.weihua.model.task.TaskStep;

import java.util.List;

public interface TaskStepRepository {

    void save(TaskStep step);

    List<TaskStep> findByTaskId(String taskId);

    int nextStepNo(String taskId);
}
