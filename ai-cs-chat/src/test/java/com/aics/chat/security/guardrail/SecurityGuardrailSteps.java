package com.aics.chat.security.guardrail;

import com.aics.chat.agent.model.SafetyCheckResult;
import com.aics.chat.agent.safety.SafetyGuardService;
import com.aics.chat.agent.state.AfterSaleState;
import com.aics.chat.agent.state.AgentStateMachine;
import com.aics.chat.agent.tool.AgentToolRegistry;
import com.aics.chat.agent.tool.CreateAfterSaleTool;
import com.aics.chat.agent.tool.OrderLocatorTool;
import com.aics.chat.agent.tool.ToolResult;
import com.aics.chat.agent.trace.AgentTraceRecorder;
import com.aics.chat.agent.context.AfterSaleContext;
import com.aics.chat.dto.AgentStepDTO;
import com.aics.chat.dto.OrderVO;
import com.aics.chat.dto.SecurityEventDTO;
import com.aics.chat.feign.AfterSaleFeignClient;
import com.aics.chat.feign.AgentTraceFeignClient;
import com.aics.chat.feign.OrderFeignClient;
import com.aics.chat.feign.SecurityEventFeignClient;
import com.aics.chat.security.ContentReviewResult;
import com.aics.chat.security.ContentReviewer;
import com.aics.chat.security.ContentSafetyService;
import com.aics.chat.security.RagAclFilter;
import com.aics.chat.security.RegexContentReviewer;
import com.aics.chat.security.SecurityAuditRecorder;
import com.aics.chat.security.SecurityEventType;
import com.aics.chat.security.SecurityProperties;
import com.aics.chat.security.SqlGuard;
import com.aics.chat.security.ToolAuthResult;
import com.aics.chat.security.ToolAuthorizationService;
import com.aics.chat.security.UserRoleResolver;
import com.aics.chat.util.ChatUserContext;
import com.aics.chat.util.PiiMasker;
import com.aics.common.exception.BusinessException;
import com.aics.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.After;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 3.2 AI 安全网关与 Guardrails —— BDD 步骤定义（对应 features/security/ 下 7 个 Feature）。
 *
 * <p>步骤直接驱动生产组件（SafetyGuardService / ContentSafetyService / ToolAuthorizationService /
 * RagAclFilter / SqlGuard / SecurityAuditRecorder / OrderLocatorTool 等），验证行为契约。</p>
 */
public class SecurityGuardrailSteps {

    /**
     * 列表参数类型：支持 ["a", "b"] 与 a,b 两种写法（用于表/列白名单配置步骤）。
     * 元素两侧的引号会被剥离（["orders"] -> orders）。
     */
    @ParameterType("\\[[^\\]]*\\]|\\S+")
    public List<String> list(String value) {
        String v = value == null ? "" : value.trim();
        if (v.startsWith("[") && v.endsWith("]")) {
            v = v.substring(1, v.length() - 1).trim();
        }
        if (v.isEmpty()) {
            return List.of();
        }
        return java.util.Arrays.stream(v.split(","))
                .map(String::trim)
                .map(e -> e.replaceAll("^\"|\"$", ""))
                .toList();
    }

    // ==================== 共享组件 ====================

    private final SecurityProperties props = new SecurityProperties();
    private SecurityAuditRecorder recorder;
    private SafetyGuardService safetyService;
    private ContentReviewer reviewer;
    private ContentSafetyService contentSafetyService;
    private RagAclFilter ragAclFilter;
    private SqlGuard sqlGuard;
    private ToolAuthorizationService toolAuthorizationService;
    private AgentToolRegistry registry;
    private OrderLocatorTool orderLocatorTool;

    // ==================== 场景状态 ====================

    private String inputText;
    private SafetyCheckResult safetyResult;
    private ContentReviewResult contentResult;
    private String sqlText;
    private String sqlDatabase = "order";
    private String validateResult;
    private String enforcedSql;
    private ToolResult locateResult;
    private boolean requiresConfirmation;
    private ToolAuthResult toolAuthResult;
    private List<Document> aclResult;

    @After
    public void cleanup() {
        ChatUserContext.clear();
    }

    private SecurityAuditRecorder newRecorder() {
        // 真实记录器 + Mock Feign（验证内存事件缓存与脱敏；落库调用可 verify）
        return new SecurityAuditRecorder(mock(SecurityEventFeignClient.class), props, new PiiMasker());
    }

    private static List<SecurityEventDTO> eventsOfType(SecurityAuditRecorder recorder, String type) {
        return recorder.recentEvents().stream().filter(e -> type.equals(e.getType())).toList();
    }

    // ==================== F1 注入检测 ====================

    @Given("输入安全检查服务已就绪")
    public void safetyServiceReady() {
        safetyService = new SafetyGuardService();
    }

    @Given("用户输入 {string}")
    public void userInput(String text) {
        inputText = text;
    }

    @Given("用户输入超过 2000 字符的文本")
    public void longInput() {
        inputText = "a".repeat(2001);
    }

    @When("输入 Guardrail 检查该输入")
    public void checkInput() {
        safetyResult = safetyService.check(inputText);
    }

    @Then("返回拦截结果")
    public void assertBlocked() {
        if (safetyResult != null) {
            assertFalse(safetyResult.passed(), "应拦截: " + inputText);
        } else if (contentResult != null) {
            assertFalse(contentResult.passed(), "应拦截: " + inputText);
        } else {
            fail("应先执行检查");
        }
    }

    @Then("拦截原因为{string}")
    public void assertReason(String reason) {
        assertTrue(safetyResult.reason().contains(reason), "实际原因: " + safetyResult.reason());
    }

    @Then("返回放行结果")
    public void assertPassed() {
        if (safetyResult != null) {
            assertTrue(safetyResult.passed(), "应放行: " + inputText);
        } else if (contentResult != null) {
            assertTrue(contentResult.passed(), "应放行: " + inputText);
        } else {
            fail("应先执行检查");
        }
    }

    @And("输入进入意图识别流程")
    public void inputProceeds() {
        assertTrue(safetyResult.passed());
    }

    // ==================== F2 工具授权 ====================

    @Given("工具授权服务已就绪")
    public void toolAuthReady() {
        recorder = newRecorder();
        toolAuthorizationService = new ToolAuthorizationService(props, new UserRoleResolver(props), recorder);
    }

    @Given("当前用户 userId={long}")
    public void currentUser(long userId) {
        ChatUserContext.setUserId(userId);
    }

    @Given("订单 {word} 归属于 userId={long}")
    public void orderOwnedBy(String orderNo, long userId) {
        // 仅记录归属（测试数据用）；本人订单列表由 feign mock 决定
    }

    @When("用户要求查询订单 {word}")
    public void queryOrder(String orderNo) {
        recorder = newRecorder();
        OrderFeignClient feign = mock(OrderFeignClient.class);
        // 本人（userId=1）订单列表：只有自己的订单，不包含他人订单
        when(feign.listOrders(1L)).thenReturn(Result.success(List.of(ownPaidOrder())));
        orderLocatorTool = new OrderLocatorTool(feign, recorder);
        locateResult = orderLocatorTool.locate(orderNo);
    }

    @Then("订单定位工具拒绝该请求")
    public void locateRejected() {
        assertNotNull(locateResult);
        assertTrue(locateResult.isFail(), "应拒绝越权订单查询: " + locateResult.message());
    }

    @And("返回\"不存在或不属于当前用户\"而不是订单数据")
    public void notOwnedMessage() {
        assertTrue(locateResult.message().contains("不存在或不属于当前用户"));
        assertNull(locateResult.data(), "不得返回他人订单数据");
    }

    @And("审计记录该越权尝试")
    public void auditUnauthorized() {
        assertFalse(eventsOfType(recorder, "TOOL_UNAUTHORIZED").isEmpty(), "应记录 TOOL_UNAUTHORIZED 事件");
    }

    @Given("售后申请工具已注册到工具注册中心")
    public void registerAfterSaleTool() {
        CreateAfterSaleTool tool = new CreateAfterSaleTool(mock(AfterSaleFeignClient.class));
        registry = new AgentToolRegistry(new AgentStateMachine(), List.of(tool));
    }

    @When("查询该工具是否要求确认")
    public void queryConfirmation() {
        requiresConfirmation = registry.requiresConfirmation(AgentStateMachine.TOOL_CREATE_AFTER_SALE);
    }

    @Then("返回需要确认")
    public void needsConfirmation() {
        assertTrue(requiresConfirmation, "写操作工具必须要求确认");
    }

    @And("未确认前执行被状态机拒绝")
    public void unconfirmedRejected() {
        // 未到确认状态（如 LOCATE_ORDER）直接调用写工具 → 状态机门禁拒绝
        try {
            registry.assertToolAllowed(AfterSaleState.LOCATE_ORDER, AgentStateMachine.TOOL_CREATE_AFTER_SALE);
            fail("未确认状态下调用写工具应被拒绝");
        } catch (BusinessException expected) {
            // 拒绝符合预期（AGENT_WRITE_OP_NOT_CONFIRMED 等）
        }
    }

    @Given("工具 {string} 仅允许角色 ADMIN")
    public void adminOnlyTool(String toolName) {
        props.getToolPermissions().put(toolName, List.of("ADMIN"));
    }

    @Given("当前用户 userId={long} 角色为 {word}")
    public void userRole(long userId, String role) {
        props.getUserRoles().put(userId, role);
        ChatUserContext.setUserId(userId);
    }

    @When("校验 userId={long} 调用工具 {string}")
    public void authorizeTool(long userId, String toolName) {
        toolAuthResult = toolAuthorizationService.authorize(userId, toolName, "params-digest");
    }

    @Then("返回拒绝")
    public void assertDenied() {
        assertNotNull(toolAuthResult);
        assertFalse(toolAuthResult.allowed(), "应拒绝: " + toolAuthResult.reason());
    }

    @And("审计记录权限不足")
    public void auditPermissionDenied() {
        assertFalse(eventsOfType(recorder, "TOOL_UNAUTHORIZED").isEmpty(), "应记录权限不足事件");
    }

    @Then("返回放行")
    public void assertToolAllowed() {
        assertNotNull(toolAuthResult);
        assertTrue(toolAuthResult.allowed(), "应放行: " + toolAuthResult);
    }

    // ==================== F3 PII 脱敏 ====================

    @Given("PII 脱敏工具已就绪")
    public void piiReady() {
        piiMasker = new PiiMasker();
    }

    private PiiMasker piiMasker;

    @Given("原始文本含手机号 {string}")
    public void rawWithPhone(String phone) {
        rawText = "用户手机号 " + phone + " 请联系";
    }

    private String rawText;
    private String maskedText;

    @Given("原始文本含身份证号 {string}")
    public void rawWithIdCard(String idCard) {
        rawText = "身份证 " + idCard + " 已登记";
    }

    @Given("原始文本含银行卡号 {string}")
    public void rawWithBankCard(String card) {
        rawText = "银行卡 " + card + " 已绑定";
    }

    @Given("原始文本含订单号 {string}")
    public void rawWithOrderNo(String orderNo) {
        rawText = "订单号 " + orderNo + " 已创建";
    }

    @Given("原始文本含邮箱 {string} 和地址 {string}")
    public void rawWithEmailAndAddress(String email, String address) {
        rawText = "邮箱 " + email + "，地址 " + address;
    }

    @When("执行 PII 脱敏")
    public void mask() {
        maskedText = piiMasker.mask(rawText);
    }

    @Then("结果为 {string}")
    public void assertMasked(String expected) {
        assertTrue(maskedText.contains(expected), "脱敏结果: " + maskedText);
    }

    @Then("文本保持不变")
    public void assertUnchanged() {
        assertTrue(maskedText.contains(rawText.replace("订单号 ", "")), "不应误伤: " + maskedText);
    }

    @Then("邮箱被遮蔽为 {string}")
    public void assertEmailMasked(String expected) {
        assertTrue(maskedText.contains(expected), "脱敏结果: " + maskedText);
    }

    @And("地址门牌号被遮蔽")
    public void assertAddressMasked() {
        assertFalse(maskedText.contains("1号院"), "门牌号应被遮蔽: " + maskedText);
        assertTrue(maskedText.contains("号院"), "后缀保留: " + maskedText);
    }

    @Given("用户输入含手机号 {string}")
    public void inputWithPhone(String phone) {
        inputText = "我手机号是" + phone + "，请帮我查订单";
    }

    @When("Agent 轨迹记录该输入与工具结果")
    public void traceRecords() {
        AgentTraceFeignClient feign = mock(AgentTraceFeignClient.class);
        AgentTraceRecorder traceRecorder = new AgentTraceRecorder(feign, new ObjectMapper(), new PiiMasker());
        AfterSaleContext ctx = new AfterSaleContext();
        ctx.setRunId("run-pii-1");
        ctx.setSessionId(1L);
        ctx.setUserId(1L);
        traceRecorder.step(ctx, "EXECUTE", AgentStateMachine.TOOL_CREATE_AFTER_SALE, inputText,
                "申请成功，联系手机 13900139000", 10, "SUCCESS", null);
        ArgumentCaptor<AgentStepDTO> captor = ArgumentCaptor.forClass(AgentStepDTO.class);
        verify(feign).appendStep(anyString(), captor.capture());
        traceStep = captor.getValue();
    }

    private AgentStepDTO traceStep;

    @Then("轨迹中手机号为 {string}")
    public void assertTraceMasked(String expected) {
        assertNotNull(traceStep);
        assertTrue(traceStep.getInputDigest().contains(expected), "输入摘要: " + traceStep.getInputDigest());
        assertTrue(traceStep.getOutputDigest().contains(expected), "输出摘要: " + traceStep.getOutputDigest());
    }

    @And("普通日志与审计中不出现完整手机号")
    public void assertNoPlainPhone() {
        assertFalse(traceStep.getInputDigest().contains("13900139000"));
        assertFalse(traceStep.getOutputDigest().contains("13900139000"));
    }

    // ==================== F4 内容安全 ====================

    @Given("内容安全服务已就绪")
    public void contentSafetyReady() {
        recorder = newRecorder();
        reviewer = new RegexContentReviewer(props);
        contentSafetyService = new ContentSafetyService(reviewer, props, recorder);
    }

    @Given("用户输入含违规内容 {string}")
    public void abusiveInput(String text) {
        inputText = text;
    }

    @When("内容审核检查该输入")
    public void reviewInput() {
        contentResult = contentSafetyService.reviewInput(inputText);
    }

    @When("输入进入审核环节")
    public void reviewInputAtGate() {
        contentResult = contentSafetyService.reviewInput(inputText);
    }

    @Then("拦截分类为 {string}")
    public void assertCategory(String category) {
        assertEquals(category, contentResult.category(), "实际分类: " + contentResult.category());
    }

    @Given("模型生成了违规内容 {string}")
    public void abusiveOutput(String text) {
        inputText = text;
    }

    @When("内容审核检查该输出")
    public void reviewOutput() {
        contentResult = contentSafetyService.reviewOutput(inputText);
    }

    @Given("模型生成了正常售后回答 {string}")
    public void normalOutput(String text) {
        inputText = text;
    }

    @Given("内容审核服务不可用")
    public void reviewerDown() {
        reviewer = new ContentReviewer() {
            @Override
            public ContentReviewResult review(String text, String stage) {
                throw new RuntimeException("内容审核服务不可用（模拟）");
            }
        };
        contentSafetyService = new ContentSafetyService(reviewer, props, recorder);
    }

    @Given("降级模式为 {string}")
    public void failMode(String mode) {
        props.setContentFailMode(mode);
    }

    @And("记录降级审计事件")
    public void assertDegradeAudited() {
        boolean degrade = recorder.recentEvents().stream()
                .anyMatch(e -> "DEGRADE".equals(e.getStage()) && "CONTENT_REVIEW".equals(e.getType()));
        assertTrue(degrade, "应记录降级审计事件: " + recorder.recentEvents());
    }

    // ==================== F5 RAG ACL ====================

    @Given("RAG ACL 过滤器已就绪")
    public void ragAclReady() {
        recorder = newRecorder();
        ragAclFilter = new RagAclFilter(props, new UserRoleResolver(props), recorder);
    }

    @Given("文档 {string} 仅对角色 INTERNAL 可见")
    public void docInternalOnly(String docId) {
        props.getRagAclDocuments().put(docId, List.of("INTERNAL"));
    }

    @Given("文档 {string} 当前允许角色 USER")
    public void docAllowedUser(String docId) {
        props.getRagAclDocuments().put(docId, List.of("USER"));
    }

    @Given("文档 {string} 权限被回收（仅允许 INTERNAL）")
    public void docRevoked(String docId) {
        props.getRagAclDocuments().put(docId, List.of("INTERNAL"));
    }

    @Given("检索结果包含文档 {string} 和 {string}")
    public void retrievalDocs(String docA, String docB) {
        retrievalDocs = List.of(doc(docA), doc(docB));
    }

    @Given("检索结果包含文档 {string}")
    public void retrievalDocs(String doc) {
        retrievalDocs = List.of(doc(doc));
    }

    private List<Document> retrievalDocs = new ArrayList<>();

    private static Document doc(String docId) {
        return new Document(docId, "文档 " + docId + " 内容", Map.of("documentId", docId));
    }

    @When("执行 ACL 过滤")
    public void filterDocs() {
        aclResult = ragAclFilter.filter("kb-default", retrievalDocs, ChatUserContext.getUserId() == null ? 1L : ChatUserContext.getUserId());
    }

    @Then("仅返回文档 {string}")
    public void onlyDoc(String docId) {
        assertEquals(1, aclResult.size(), "实际: " + aclResult.stream().map(Document::getId).toList());
        assertEquals(docId, aclResult.get(0).getId());
    }

    @And("回答不引用无权限文档")
    public void noForbiddenCitation() {
        // 被过滤文档不在结果中，即不会被引用
        assertTrue(aclResult.stream().noneMatch(d -> d.getId().equals("doc-secret")));
    }

    @And("审计记录 ACL 过滤事件")
    public void auditAclFiltered() {
        assertFalse(eventsOfType(recorder, "RAG_ACL_DENIED").isEmpty(), "应记录 RAG_ACL_DENIED 事件");
    }

    @Given("知识库 {string} 仅对角色 TENANT_B 可见")
    public void kbTenantB(String kb) {
        props.getRagAclKnowledgeBases().put(kb, List.of("TENANT_B"));
    }

    @When("对知识库 {string} 执行 ACL 过滤")
    public void filterKb(String kb) {
        aclResult = ragAclFilter.filter(kb, retrievalDocs, ChatUserContext.getUserId());
    }

    @Then("返回空结果")
    public void emptyResult() {
        assertNotNull(aclResult);
        assertTrue(aclResult.isEmpty(), "应整库过滤: " + aclResult);
    }

    @When("第一轮执行 ACL 过滤")
    public void firstRound() {
        aclResult = ragAclFilter.filter("kb-default", List.of(doc("doc-1")), 1L);
    }

    @Then("返回文档 {string}")
    public void returnsDoc(String docId) {
        assertEquals(1, aclResult.size());
        assertEquals(docId, aclResult.get(0).getId());
    }

    @When("第二轮再次执行 ACL 过滤")
    public void secondRound() {
        aclResult = ragAclFilter.filter("kb-default", List.of(doc("doc-1")), 1L);
    }

    @Then("不再召回文档 {string}")
    public void noLongerRecalled(String docId) {
        assertTrue(aclResult.isEmpty(), "权限回收后不得再召回: " + aclResult);
    }

    // ==================== F6 SQL 安全 ====================

    @Given("SQL 安全守卫已就绪")
    public void sqlGuardReady() {
        recorder = newRecorder();
        sqlGuard = new SqlGuard(props, recorder);
    }

    @Given("模型生成的 SQL 为 {string}")
    public void generatedSql(String sql) {
        sqlText = sql;
    }

    @When("执行 SQL 安全校验")
    public void validateSql() {
        validateResult = sqlGuard.validate(sqlDatabase, sqlText);
    }

    @Then("返回校验不通过")
    public void assertSqlBlocked() {
        assertNotNull(validateResult, "应被拦截: " + sqlText);
    }

    @And("拒绝原因为{string}")
    public void assertSqlReason(String reason) {
        assertTrue(validateResult.contains(reason), "实际原因: " + validateResult);
    }

    @When("执行强制行数上限")
    public void enforceLimit() {
        enforcedSql = sqlGuard.enforceLimit(sqlText, 100);
    }

    @Then("自动追加 {string}")
    public void assertLimitAppended(String expected) {
        assertTrue(enforcedSql.endsWith(expected), "实际: " + enforcedSql);
    }

    @Given("库 {string} 的表白名单为 {list}")
    public void tableWhitelist(String db, List<String> tables) {
        props.getSqlTableWhitelist().put(db, tables);
    }

    @Given("库 {string} 的列白名单为 {list}")
    public void columnWhitelist(String db, List<String> columns) {
        props.getSqlColumnWhitelist().put(db, columns);
    }

    @When("对库 {string} 校验 SQL {string}")
    public void validateSqlForDb(String db, String sql) {
        validateResult = sqlGuard.validate(db, sql);
    }

    @Then("返回校验通过")
    public void assertSqlPassed() {
        assertNull(validateResult, "应通过: " + sqlText);
    }

    // ==================== F7 审计留痕 ====================

    @Given("安全审计记录器已就绪")
    public void auditReady() {
        recorder = newRecorder();
    }

    @Given("用户 userId={long} 输入 {string}")
    public void userInputWithId(long userId, String text) {
        inputText = text;
        auditUserId = userId;
    }

    private Long auditUserId;

    @When("输入 Guardrail 拦截该输入")
    public void guardBlock() {
        SafetyGuardService service = new SafetyGuardService();
        SafetyCheckResult result = service.check(inputText);
        assertFalse(result.passed(), "前置：输入应被拦截");
        recorder.record(SecurityEventType.PROMPT_INJECTION, "INPUT", auditUserId,
                "SafetyGuardService", inputText, "BLOCK", result.reason());
    }

    @Then("审计记录一条 {word} 事件")
    public void assertAuditEvent(String type) {
        assertFalse(eventsOfType(recorder, type).isEmpty(), "应记录 " + type + " 事件: " + recorder.recentEvents());
    }

    @And("事件包含用户ID、命中规则与处理动作")
    public void assertEventFields() {
        SecurityEventDTO event = eventsOfType(recorder, "PROMPT_INJECTION").get(0);
        assertEquals(auditUserId, event.getUserId());
        assertNotNull(event.getRule());
        assertEquals("BLOCK", event.getAction());
    }

    @And("敏感输入仅保存摘要（脱敏后无完整敏感信息）")
    public void assertEventMasked() {
        SecurityEventDTO event = eventsOfType(recorder, "PROMPT_INJECTION").get(0);
        assertNotNull(event.getInputDigest());
        assertFalse(event.getInputDigest().contains("13800138000"), "审计不得含明文手机号: " + event.getInputDigest());
        assertTrue(event.getInputDigest().contains("138****8000"), "应保存脱敏摘要: " + event.getInputDigest());
    }

    @And("事件详情包含{string}")
    public void assertEventDetailContains(String keyword) {
        SecurityEventDTO event = eventsOfType(recorder, "TOOL_UNAUTHORIZED").get(0);
        assertNotNull(event.getDetail());
        assertTrue(event.getDetail().contains(keyword), "事件详情: " + event.getDetail());
    }

    // ==================== 测试数据辅助 ====================

    private static OrderVO ownPaidOrder() {
        OrderVO order = new OrderVO();
        order.setOrderNo("ORD10001");
        order.setStatus("PAID");
        order.setPayAmount(new BigDecimal("199.00"));
        order.setCreateTime(LocalDateTime.now().minusDays(1));
        OrderVO.OrderItemVO item = new OrderVO.OrderItemVO();
        item.setProductId(1001L);
        item.setProductName("无线蓝牙耳机");
        item.setProductPrice(new BigDecimal("199.00"));
        item.setQuantity(1);
        order.setItems(List.of(item));
        return order;
    }
}
