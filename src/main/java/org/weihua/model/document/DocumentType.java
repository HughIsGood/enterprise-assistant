package org.weihua.model.document;

import lombok.Getter;

/**
 * FAQ
 * 适合：
 * 常见问题
 * 一问一答文档
 * POLICY
 *
 * 适合：
 * 报销制度
 * 请假制度
 * 安全规范
 * SOP
 * 适合：
 * 故障处理手册
 * 运维操作手册
 * 应急流程
 * TECH_SPEC
 * 适合：
 * 接口规范
 * SQL 规范
 * 开发规范
 */
public enum DocumentType {
    POLICY("policy"),
    TECH_TYPE("tech_type"),
    SOP("sop"),
    FAQ("faq");

    @Getter
    private final String desc;

    DocumentType(String desc) {
        this.desc = desc;
    }
}