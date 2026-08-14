Feature: 02 工具调用授权（资源级鉴权）
  作为系统
  我想要在工具端重新校验用户、资源归属与操作权限
  以便即使模型参数被伪造，也无法越权访问他人数据或执行未授权操作

  Background:
    Given 工具授权服务已就绪

  Scenario: 越权订单号被拒绝
    Given 当前用户 userId=1
    And 订单 ORD10002 归属于 userId=2
    When 用户要求查询订单 ORD10002
    Then 订单定位工具拒绝该请求
    And 返回"不存在或不属于当前用户"而不是订单数据
    And 审计记录该越权尝试

  Scenario: 写操作必须二次确认
    Given 售后申请工具已注册到工具注册中心
    When 查询该工具是否要求确认
    Then 返回需要确认
    And 未确认前执行被状态机拒绝

  Scenario: 无权限角色调用受限工具被拒绝
    Given 工具 "adminTool" 仅允许角色 ADMIN
    And 当前用户 userId=1 角色为 USER
    When 校验 userId=1 调用工具 "adminTool"
    Then 返回拒绝
    And 审计记录权限不足

  Scenario: 已授权角色可调用受限工具
    Given 工具 "adminTool" 仅允许角色 ADMIN
    And 当前用户 userId=2 角色为 ADMIN
    When 校验 userId=2 调用工具 "adminTool"
    Then 返回放行
