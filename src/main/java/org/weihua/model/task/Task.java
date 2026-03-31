package org.weihua.model.task;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Task implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String taskId;
    private String sessionId;
    private String userId;
    private String goal;
    private TaskStatus status;
    private String finalAnswer;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

