package com.aics.chat.agent.tool;

/**
 * 工具执行结果
 *
 * @param outcome 结果类型
 * @param message 消息
 * @param data    数据（订单/推荐列表/申请单等）
 */
public record ToolResult(Outcome outcome, String message, Object data) {

    public enum Outcome {
        /** 成功 */
        SUCCESS,
        /** 需要用户选择（多候选） */
        CANDIDATES,
        /** 失败 */
        FAIL
    }

    public static ToolResult success(String message, Object data) {
        return new ToolResult(Outcome.SUCCESS, message, data);
    }

    public static ToolResult candidates(String message, Object data) {
        return new ToolResult(Outcome.CANDIDATES, message, data);
    }

    public static ToolResult fail(String message) {
        return new ToolResult(Outcome.FAIL, message, null);
    }

    public boolean isSuccess() {
        return outcome == Outcome.SUCCESS;
    }

    public boolean isCandidates() {
        return outcome == Outcome.CANDIDATES;
    }

    public boolean isFail() {
        return outcome == Outcome.FAIL;
    }
}
