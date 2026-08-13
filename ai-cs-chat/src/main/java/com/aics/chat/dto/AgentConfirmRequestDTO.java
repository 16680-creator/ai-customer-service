package com.aics.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Agent 写操作确认请求
 */
@Data
@Schema(description = "Agent 写操作确认请求")
public class AgentConfirmRequestDTO {

    @Schema(description = "执行 ID")
    @NotBlank(message = "runId 不能为空")
    private String runId;

    @Schema(description = "确认凭证（由 Agent 返回）")
    // 凭证由 Agent 上一步（确认态）返回，用于防篡改校验
    @NotBlank(message = "确认凭证不能为空")
    private String token;

    @Schema(description = "决策：CONFIRM / REJECT")
    @NotBlank(message = "决策不能为空")
    private String decision;

    @Schema(description = "会话 ID")
    private Long sessionId;
}
