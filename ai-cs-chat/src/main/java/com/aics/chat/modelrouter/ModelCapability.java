package com.aics.chat.modelrouter;

// 学习点：能力声明独立于模型展示名——供应商命名差异大，不能靠“模型名包含 xxx”猜测能力，配置显式声明才可靠
public enum ModelCapability {
    TOOL_CALLING, VISION
}
