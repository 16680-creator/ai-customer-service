Feature: 02 身份可信透传
  作为系统
  我想要在网关透传身份时移除客户端伪造的身份头
  以便下游只信任 JWT 解析出的真实用户身份

  Scenario: 伪造身份头不生效
    Given 客户端请求头携带伪造的 "X-User-Id: 999"
    And 网关从 JWT 解析出真实 userId=1
    When 网关透传身份头给下游
    Then 下游收到的 X-User-Id 仅为 "1"
    And 伪造的 "999" 被移除
