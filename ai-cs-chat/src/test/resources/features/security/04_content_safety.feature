Feature: 04 内容安全（输入/输出双向审核）
  作为系统
  我想要对模型输入与输出分别审核
  以便违规内容被拒答或转人工，而不直接呈现给用户

  Background:
    Given 内容安全服务已就绪

  Scenario: 违规输入被拒答
    Given 用户输入含违规内容 "这个傻逼商家，妈的垃圾"
    When 内容审核检查该输入
    Then 返回拦截结果
    And 拦截分类为 "ABUSE"

  Scenario: 违规输出被拦截
    Given 模型生成了违规内容 "这里有赌博网站 xx.com 可以下注"
    When 内容审核检查该输出
    Then 返回拦截结果
    And 拦截分类为 "ILLEGAL"

  Scenario: 正常输出放行
    Given 模型生成了正常售后回答 "您的换货申请已提交成功"
    When 内容审核检查该输出
    Then 返回放行结果

  Scenario: 审核服务故障时按 BLOCK 配置降级拦截
    Given 内容审核服务不可用
    And 降级模式为 "BLOCK"
    When 输入进入审核环节
    Then 返回拦截结果
    And 记录降级审计事件

  Scenario: 审核服务故障时按 ALLOW 配置降级放行
    Given 内容审核服务不可用
    And 降级模式为 "ALLOW"
    When 输入进入审核环节
    Then 返回放行结果
    And 记录降级审计事件
