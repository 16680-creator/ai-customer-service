package com.aics.message.controller;

import com.aics.common.result.Result;
import com.aics.message.dto.LlmTraceDTO;
import com.aics.message.dto.PageResult;
import com.aics.message.service.LlmTraceService;
import com.aics.message.vo.LlmTraceVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * LLM 调用链追踪控制器单元测试
 * <p>
 * TDD：验证控制器正确委托 Service 层并返回统一 {@link Result} 结构。
 * 纯 Mockito 直接调用（与模块既有约定一致），不加载 Spring 上下文。
 *
 * <h3>【测试设计】为什么 Controller 测试只断言"委托 + 返回结构"</h3>
 * <p>Controller 的职责是"薄透传"：参数绑定与 {@code @Valid} 校验由 Spring MVC 容器在
 * 真实请求时执行，纯单测（直接调方法）无法也不应覆盖；
 * 单测聚焦两点：① 把入参原样委托给 Service（verify 方法调用）；
 * ② 把 Service 返回值包装成 Result 并保持 code=200（契约稳定）。</p>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class LlmTraceControllerTest {

    @Mock
    private LlmTraceService llmTraceService;

    @InjectMocks
    private LlmTraceController llmTraceController;

    // ==================== POST /api/observability/traces ====================

    @Test
    @DisplayName("创建调用链追踪 - 委托 Service 并返回 requestId")
    void createTrace_delegatesAndReturnsResult() {
        LlmTraceDTO dto = new LlmTraceDTO();
        dto.setRequestId("trace-1");
        when(llmTraceService.createTrace(dto)).thenReturn("trace-1");

        Result<String> result = llmTraceController.createTrace(dto);

        assertEquals(200, result.getCode());
        assertEquals("操作成功", result.getMessage());
        assertEquals("trace-1", result.getData());
        verify(llmTraceService).createTrace(dto);
    }

    // ==================== GET /api/observability/traces/{requestId} ====================

    @Test
    @DisplayName("查询调用链追踪 - 存在时返回 VO")
    void getTrace_found_delegatesAndReturnsResult() {
        LlmTraceVO vo = new LlmTraceVO();
        vo.setRequestId("trace-1");
        vo.setScenario("chat");
        when(llmTraceService.getTrace("trace-1")).thenReturn(vo);

        Result<LlmTraceVO> result = llmTraceController.getTrace("trace-1");

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals("trace-1", result.getData().getRequestId());
        verify(llmTraceService).getTrace("trace-1");
    }

    @Test
    @DisplayName("查询调用链追踪 - 不存在时返回 success(null)")
    void getTrace_notFound_returnsSuccessNull() {
        when(llmTraceService.getTrace("missing")).thenReturn(null);

        Result<LlmTraceVO> result = llmTraceController.getTrace("missing");

        // 契约断言：查询缺失必须是"200 + data=null"而非错误码，
        // 保证 chat 模块把"无追踪"当正常分支处理（与 getTrace 返回 null 的语义呼应）
        assertEquals(200, result.getCode());
        assertNull(result.getData());
        verify(llmTraceService).getTrace("missing");
    }

    // ==================== GET /api/observability/traces ====================

    @Test
    @DisplayName("分页查询 - 委托 Service 并返回分页结果")
    void pageTraces_delegatesAndReturnsResult() {
        PageResult<LlmTraceVO> pageResult = new PageResult<>(Collections.emptyList(), 0, 1, 20);
        when(llmTraceService.pageTraces(1000L, "chat", 1, 20)).thenReturn(pageResult);

        Result<PageResult<LlmTraceVO>> result = llmTraceController.pageTraces(1000L, "chat", 1, 20);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().getPage());
        assertEquals(20, result.getData().getSize());
        verify(llmTraceService).pageTraces(1000L, "chat", 1, 20);
    }

    @Test
    @DisplayName("分页查询 - 缺省参数时传默认值并允许空过滤")
    void pageTraces_defaultParams() {
        PageResult<LlmTraceVO> pageResult = new PageResult<>(Collections.emptyList(), 0, 1, 20);
        when(llmTraceService.pageTraces(null, null, 1, 20)).thenReturn(pageResult);

        Result<PageResult<LlmTraceVO>> result = llmTraceController.pageTraces(null, null, 1, 20);

        assertEquals(200, result.getCode());
        verify(llmTraceService).pageTraces(null, null, 1, 20);
    }
}
