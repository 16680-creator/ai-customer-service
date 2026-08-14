Feature: 05 RAG 数据防泄漏
  作为系统
  我想要在检索阶段按租户、角色与文档 ACL 过滤
  以便用户只能检索到有权限的知识文档

  Background:
    Given RAG ACL 过滤器已就绪

  Scenario: 无权限文档不被召回
    Given 文档 "doc-secret" 仅对角色 INTERNAL 可见
    And 当前用户 userId=1 角色为 USER
    And 检索结果包含文档 "doc-secret" 和 "doc-public"
    When 执行 ACL 过滤
    Then 仅返回文档 "doc-public"
    And 回答不引用无权限文档
    And 审计记录 ACL 过滤事件

  Scenario: 租户隔离
    Given 知识库 "tenant-b" 仅对角色 TENANT_B 可见
    And 当前用户 userId=1 角色为 TENANT_A
    And 检索结果包含文档 "doc-1"
    When 对知识库 "tenant-b" 执行 ACL 过滤
    Then 返回空结果

  Scenario: 多轮上下文中的权限一致
    Given 文档 "doc-1" 当前允许角色 USER
    And 当前用户 userId=1 角色为 USER
    When 第一轮执行 ACL 过滤
    Then 返回文档 "doc-1"
    Given 文档 "doc-1" 权限被回收（仅允许 INTERNAL）
    When 第二轮再次执行 ACL 过滤
    Then 不再召回文档 "doc-1"
