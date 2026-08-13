package com.aics.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Agent 对话请求
 */
@Data
@Schema(description = "Agent 对话请求")
public class AgentRequestDTO {

    @Schema(description = "会话 ID")
    private Long sessionId;

    @Schema(description = "续跑的 runId（首次为空）")
    private String runId;

    @Schema(description = "用户输入")
    @NotBlank(message = "用户输入不能为空")
    private String input;
}
