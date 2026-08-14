Feature: 03 PII 识别与脱敏
  作为系统
  我想要在日志、模型请求与审计记录之前识别并脱敏个人信息
  以便敏感数据不进入模型厂商与第三方可观测平台

  Background:
    Given PII 脱敏工具已就绪

  Scenario: 手机号脱敏
    Given 原始文本含手机号 "13812345678"
    When 执行 PII 脱敏
    Then 结果为 "138****5678"

  Scenario: 身份证号脱敏
    Given 原始文本含身份证号 "11010119900307771X"
    When 执行 PII 脱敏
    Then 结果为 "110101********771X"

  Scenario: 银行卡号脱敏
    Given 原始文本含银行卡号 "4111111111111111"
    When 执行 PII 脱敏
    Then 结果为 "411111********1111"

  Scenario: 非银行卡数字串不误伤
    Given 原始文本含订单号 "20260814000000123456"
    When 执行 PII 脱敏
    Then 文本保持不变

  Scenario: 邮箱与地址脱敏
    Given 原始文本含邮箱 "test@example.com" 和地址 "北京市朝阳区望京街道1号院"
    When 执行 PII 脱敏
    Then 邮箱被遮蔽为 "***@example.com"
    And 地址门牌号被遮蔽

  Scenario: 模型请求与日志不落明文
    Given 用户输入含手机号 "13900139000"
    When Agent 轨迹记录该输入与工具结果
    Then 轨迹中手机号为 "139****9000"
    And 普通日志与审计中不出现完整手机号
