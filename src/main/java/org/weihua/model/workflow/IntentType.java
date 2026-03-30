package org.weihua.model.workflow;

/**
 * 意图类型
 */
public enum IntentType {
    // 意图：RAG回答
    KNOWLEDGE_QA,
    // 意图：RAG + 结构化回答
    PROCESS_QA,
    // 意图：工具调用
    ACTION_REQUEST,
    // 意图：追问澄清
    CLARIFICATION
}
