## Purpose

将 RAG 评估从离线 golden 集扩展到线上真实流量：按采样率抽取线上请求，复用 LLM-as-Judge 对真实回答评分并持久化；提供用户反馈接口形成反馈闭环，并扩展 CI 门禁（P95 延迟、单请求平均 Token 上限），使质量与成本治理覆盖线上运行态。

## ADDED Requirements

### Requirement: 线上采样评估
系统 SHALL 按可配置采样率抽取线上对话请求，对采样请求的真实回答执行 LLM-as-Judge 评分，并持久化评估结果（requestId、问题、回答摘要、评分、评估时间）。

#### Scenario: 采样请求被评分
- **WHEN** 一次线上对话请求命中采样率
- **THEN** 系统对该请求的回答执行 LLM-as-Judge 评分并持久化评估记录

#### Scenario: 未命中采样
- **WHEN** 一次线上对话请求未命中采样率
- **THEN** 系统不执行评分，业务不受影响

#### Scenario: 评分失败
- **WHEN** 线上评估的 LLM-as-Judge 调用失败
- **THEN** 评估记录标记为失败或跳过，不影响用户回答与主链路

### Requirement: 用户反馈闭环
系统 SHALL 提供用户反馈接口，支持用户对单次回答提交反馈（点赞/点踩/1-5 分/补充文本），反馈 MUST 关联 `requestId` 与 `sessionId` 并持久化。

#### Scenario: 提交反馈
- **WHEN** 用户对一次回答提交点赞、点踩或评分
- **THEN** 系统持久化该反馈并关联对应 requestId 与 sessionId，返回成功

#### Scenario: 反馈查询
- **WHEN** 按 requestId 或时间窗口查询反馈
- **THEN** 系统返回反馈明细（类型、评分、补充文本、时间）

#### Scenario: 无效请求反馈
- **WHEN** 反馈对应的 requestId 不存在或参数非法
- **THEN** 系统仍持久化该反馈（标记 requestId 未知）或返回参数错误，不中断用户操作

### Requirement: 线上评估统计
系统 SHALL 提供线上评估统计接口，按时间窗口汇总采样评估均分、用户反馈分布（好评率/差评率）与评估样本数。

#### Scenario: 查询统计
- **WHEN** 调用方按时间窗口查询线上评估统计
- **THEN** 系统返回评估样本数、LLM 均分、用户好评率与差评率

#### Scenario: 无数据统计
- **WHEN** 时间窗口内无评估样本或反馈
- **THEN** 系统返回零值统计，不报错

### Requirement: CI 门禁扩展
系统 SHALL 在评估门禁中支持除正确率外的两个阈值：P95 延迟上限与单请求平均 Token 上限；任一超限即判定门禁不通过，并 SHALL 在报告中给出对应指标值。

#### Scenario: 延迟超限
- **WHEN** 评估样本的 P95 延迟超过配置上限
- **THEN** 门禁判定为不通过，报告包含实测 P95 延迟与上限值

#### Scenario: Token 超限
- **WHEN** 单请求平均 Token 数超过配置上限
- **THEN** 门禁判定为不通过，报告包含实测平均 Token 与上限值

#### Scenario: 阈值未配置
- **WHEN** P95 延迟或 Token 上限阈值未配置
- **THEN** 对应维度不参与门禁判定，仅记录指标值

### Requirement: 采样与评估配置
系统 SHALL 支持通过配置控制线上采样评估的开关、采样率、Judge 模型与门禁阈值。

#### Scenario: 关闭线上评估
- **WHEN** 配置 `aics.eval.online.enabled=false`
- **THEN** 系统不执行采样与评分，用户反馈接口仍可用

#### Scenario: 采样率调整
- **WHEN** 调整采样率配置
- **THEN** 新采样率立即生效于后续请求，无需重启
