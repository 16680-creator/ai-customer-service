package com.aics.chat.agent.model;

/**
 * 售后规则条目（来自知识库 RAG 或静态种子）
 *
 * @param id       规则条款编号（如 ASR-001）
 * @param actionType 适用售后动作
 * @param days     期限（天）
 * @param content  规则原文（用于引用展示）
 */
public record PolicyRule(String id, AfterSaleActionType actionType, int days, String content) {
}
