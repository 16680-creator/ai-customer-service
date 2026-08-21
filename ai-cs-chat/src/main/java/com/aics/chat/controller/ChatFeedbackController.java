package com.aics.chat.controller;

import com.aics.chat.dto.UserFeedbackDTO;
import com.aics.chat.dto.UserFeedbackVO;
import com.aics.chat.feign.OnlineEvalFeignClient;
import com.aics.common.exception.BusinessException;
import com.aics.common.result.Result;
import com.aics.common.result.ResultCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户反馈控制器：用户对单次回答点赞/点踩/评分，形成线上质量反馈闭环。
 *
 * <p>反馈经 {@link OnlineEvalFeignClient} 落 ai-cs-message 的 user_feedback 表；
 * requestId 未知时照常插入（不校验存在性），保证反馈不因 trace 缺失而丢失。</p>
 *
 * <h3>【AI 技术详解】类级 @Validated 与方法级 @Valid 的分工</h3>
 * <ul>
 *   <li><b>@Validated（类级）</b>：激活方法参数上的约束校验；</li>
 *   <li><b>@Valid @RequestBody</b>：触发对请求体嵌套对象的 Bean Validation。</li>
 * </ul>
 * <p>而 feedbackType/score 这类<b>业务枚举约束</b>没有写在 DTO 注解上，原因：
 * DTO 是跨服务共享契约（message 侧也要反序列化同一结构），把业务规则放到
 * 消费方 Controller 手动校验（{@link #validate}），规则演进时只改消费方，
 * 不污染共享契约、也不影响 message 侧直接入库的通道。</p>
 */
@Tag(name = "用户反馈")
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Validated
public class ChatFeedbackController {

    private final OnlineEvalFeignClient onlineEvalFeignClient;

    /**
     * 提交用户反馈（点赞/点踩/1-5 分/补充文本）。
     *
     * @param dto 反馈内容（feedbackType 必填；requestId 未知可空；score 1-5 可选）
     * @return 空结果包装
     */
    @Operation(summary = "提交用户反馈")
    @PostMapping("/feedback")
    public Result<Void> submitFeedback(@Valid @RequestBody UserFeedbackDTO dto) {
        // 手动校验业务枚举（LIKE/DISLIKE、score∈[1,5]）：见类注释，业务规则放消费方
        validate(dto);
        return onlineEvalFeignClient.saveFeedback(dto);
    }

    /**
     * 查询用户反馈（按 requestId 或时间窗口）。
     */
    @Operation(summary = "查询用户反馈")
    @GetMapping("/feedback")
    public Result<List<UserFeedbackVO>> listFeedback(@RequestParam(value = "requestId", required = false) String requestId) {
        return onlineEvalFeignClient.listFeedback(requestId, null, null);
    }

    // 校验失败抛 BusinessException(BAD_REQUEST)：由全局异常处理器转为 4xx 响应，
    // 与 @Valid 的校验错误走同一错误通道，前端处理逻辑统一
    private void validate(UserFeedbackDTO dto) {
        String type = dto.getFeedbackType();
        // 白名单校验而非黑名单：新反馈类型（如 NEUTRAL）上线前不会静默入库
        if (!"LIKE".equals(type) && !"DISLIKE".equals(type)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "反馈类型仅支持 LIKE/DISLIKE");
        }
        // score 可空（用户只点赞不打分），非空时才校验区间
        if (dto.getScore() != null && (dto.getScore() < 1 || dto.getScore() > 5)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "评分必须在 1-5 之间");
        }
    }
}
