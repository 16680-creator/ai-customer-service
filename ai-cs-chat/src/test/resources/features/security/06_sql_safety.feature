Feature: 06 SQL 安全（NL2SQL）
  作为系统
  我想要在 NL2SQL 执行前做多道防线校验
  以便模型生成的 SQL 无法读写、拖库或拖慢数据库

  Background:
    Given SQL 安全守卫已就绪

  Scenario: 非 SELECT 语句被拒绝
    Given 模型生成的 SQL 为 "DELETE FROM orders WHERE id=1"
    When 执行 SQL 安全校验
    Then 返回校验不通过
    And 拒绝原因为"仅允许 SELECT 查询语句"

  Scenario: 系统库探测被拒绝
    Given 模型生成的 SQL 为 "SELECT * FROM information_schema.tables"
    When 执行 SQL 安全校验
    Then 返回校验不通过

  Scenario: 无 LIMIT 时强制追加且限行数
    Given 模型生成的 SQL 为 "SELECT order_no FROM orders WHERE user_id=1"
    When 执行强制行数上限
    Then 自动追加 "LIMIT 100"

  Scenario: 表字段白名单（AST 校验）
    Given 库 "order" 的表白名单为 ["orders"]
    And 库 "order" 的列白名单为 ["orders.order_no", "orders.status"]
    When 对库 "order" 校验 SQL "SELECT password_hash FROM users"
    Then 返回校验不通过
    And 拒绝原因为"表 users 不在白名单内"

  Scenario: 白名单外列被拒绝（AST 校验）
    Given 库 "order" 的表白名单为 ["orders"]
    And 库 "order" 的列白名单为 ["orders.order_no", "orders.status"]
    When 对库 "order" 校验 SQL "SELECT order_no, user_id FROM orders"
    Then 返回校验不通过
    And 拒绝原因为"列 user_id 不在白名单内"

  Scenario Outline: 危险载荷被拒绝
    Given 模型生成的 SQL 为 "<sql>"
    When 执行 SQL 安全校验
    Then 返回校验不通过

    Examples:
      | sql                                          |
      | SELECT * FROM orders -- 注释绕过             |
      | SELECT SLEEP(10) FROM orders                 |
      | SELECT * INTO OUTFILE '/tmp/x' FROM orders   |
      | SELECT * FROM orders; DROP TABLE orders      |

  Scenario: 白名单内查询放行
    Given 库 "order" 的表白名单为 ["orders"]
    And 库 "order" 的列白名单为 ["orders.order_no", "orders.status", "orders.user_id"]
    When 对库 "order" 校验 SQL "SELECT order_no, status FROM orders WHERE user_id=1"
    Then 返回校验通过
