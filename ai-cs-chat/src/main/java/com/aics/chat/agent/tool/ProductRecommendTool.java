package com.aics.chat.agent.tool;

import com.aics.chat.agent.AgentProperties;
import com.aics.chat.agent.state.AgentStateMachine;
import com.aics.chat.dto.ProductRecommendVO;
import com.aics.chat.feign.ProductRecommendFeignClient;
import com.aics.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品推荐工具（只读）：同价位 ± 容差 + 特性关键词召回在售商品。
 *
 * <p>推荐解释（matchReason）由商品服务基于真实字段拼接，本工具只做透传，不生成任何编造内容。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductRecommendTool implements AgentTool {

    private final ProductRecommendFeignClient productRecommendFeignClient;
    private final AgentProperties properties;

    @Override
    public String name() {
        return AgentStateMachine.TOOL_PRODUCT_RECOMMEND;
    }

    @Override
    public RiskLevel riskLevel() {
        return RiskLevel.READ;
    }

    @Override
    public boolean requiresConfirmation() {
        return false;
    }

    /**
     * 同价位商品召回
     *
     * @param basePrice  基准价（订单商品单价或用户预算）
     * @param keywords   特性关键词（逗号分隔，可为空）
     * @param categoryId 类目（可为空）
     * @return SUCCESS：推荐列表（可能为空列表）；FAIL：服务不可用
     */
    public ToolResult recommend(BigDecimal basePrice, String keywords, Long categoryId) {
        // 基准价非法（空或非正数）：拒绝推荐
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return ToolResult.fail("缺少有效的基准价格，无法推荐");
        }
        try {
            // 同价位召回：基准价 ± 容差 + 特性关键词（透传，不本地编造）
            Result<List<ProductRecommendVO>> result = productRecommendFeignClient.recommend(
                    basePrice, properties.getPriceTolerance(), categoryId,
                    StringUtils.hasText(keywords) ? keywords : null,
                    properties.getRecommendLimit());
            if (result != null && result.isSuccess()) {
                // 成功：空列表也视为正常召回结果
                List<ProductRecommendVO> data = result.getData() == null ? List.of() : result.getData();
                return ToolResult.success("推荐召回完成", data);
            }
            return ToolResult.fail(result != null ? result.getMessage() : "推荐服务返回异常");
        } catch (Exception e) {
            log.warn("商品推荐调用失败: basePrice={}, err={}", basePrice, e.getMessage());
            // 服务不可用：可解释失败，不阻断主流程
            return ToolResult.fail("商品推荐服务暂时不可用，请稍后再试");
        }
    }
}
