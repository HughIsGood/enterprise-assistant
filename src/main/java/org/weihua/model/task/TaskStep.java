package org.weihua.model.task;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskStep {
    private String taskId;
    private int stepNo;
    private String thoughtSummary;
    private String actionType;
    private String actionPayloadJson;
    private String observationJson;
    private String stepStatus;
    private LocalDateTime createTime;
}
