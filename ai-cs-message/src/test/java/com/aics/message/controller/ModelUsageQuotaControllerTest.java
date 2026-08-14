package com.aics.message.controller;

import com.aics.common.result.Result;
import com.aics.message.dto.ModelUsageQuotaDTO;
import com.aics.message.service.ModelUsageQuotaService;
import com.aics.message.vo.ModelUsageQuotaVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 模型用量配额控制器单元测试
 * <p>
 * TDD：验证控制器正确委托 Service 层并返回统一 {@link Result} 结构。
 * 纯 Mockito 直接调用（与模块既有约定一致），不加载 Spring 上下文。
 *
 * <h3>【测试设计】为什么要测 success(null) 这个"异常"形态</h3>
 * <p>配额"没配过"是正常初始态（NULL=不限），控制器必须把它包装成成功响应而非失败，
 * 这个分支最容易在重构时被改坏（误抛 NOT_FOUND），所以单独用用例钉住契约。</p>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class ModelUsageQuotaControllerTest {

    @Mock
    private ModelUsageQuotaService modelUsageQuotaService;

    @InjectMocks
    private ModelUsageQuotaController modelUsageQuotaController;

    // ==================== GET /api/model-usage/quota ====================

    @Test
    @DisplayName("查询配额 - 存在时返回 VO")
    void getQuota_found_delegatesAndReturnsResult() {
        ModelUsageQuotaVO vo = new ModelUsageQuotaVO();
        vo.setUserId(1000L);
        vo.setScenario("chat");
        vo.setWindowType("DAILY");
        when(modelUsageQuotaService.getQuota(1000L, "chat")).thenReturn(vo);

        Result<ModelUsageQuotaVO> result = modelUsageQuotaController.getQuota(1000L, "chat");

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1000L, result.getData().getUserId());
        assertEquals("DAILY", result.getData().getWindowType());
        verify(modelUsageQuotaService).getQuota(1000L, "chat");
    }

    @Test
    @DisplayName("查询配额 - 不存在时返回 success(null)")
    void getQuota_notFound_returnsSuccessNull() {
        when(modelUsageQuotaService.getQuota(1000L, "rag")).thenReturn(null);

        Result<ModelUsageQuotaVO> result = modelUsageQuotaController.getQuota(1000L, "rag");

        assertEquals(200, result.getCode());
        assertNull(result.getData());
        verify(modelUsageQuotaService).getQuota(1000L, "rag");
    }

    // ==================== POST /api/model-usage/quota ====================

    @Test
    @DisplayName("设置配额 - 委托 Service 并返回空结果")
    void upsertQuota_delegatesAndReturnsResult() {
        ModelUsageQuotaDTO dto = new ModelUsageQuotaDTO();
        dto.setUserId(1000L);
        dto.setScenario("chat");

        Result<Void> result = modelUsageQuotaController.upsertQuota(dto);

        assertEquals(200, result.getCode());
        verify(modelUsageQuotaService).upsertQuota(dto);
    }
}
