# SDD Specs

本目录用于存放 **Spec-Kit / SDD 工作流产物**（spec.md、plan.md、tasks.md、research.md、data-model.md 等）。

## 使用方式

通过 Claude Code / Spec-Kit 命令生成：

```bash
# 初始化一个新 feature 的规格
/speckit.specify        # 生成 specs/<feature>/spec.md

# 后续步骤
/speckit.clarify        # 澄清问题
/speckit.plan           # 生成 plan.md
/speckit.tasks          # 生成 tasks.md
/speckit.implement      # 按 tasks 执行
/speckit.analyze        # 跨产物一致性检查
```

## 命名约定

`<编号>-<短描述>`，例如 `001-chat-agent`、`002-knowledge-rag`。编号在团队内顺序分配。

## 已有功能

| 编号 | 功能 | 状态 | 说明 |
|------|------|------|------|
| 001-cart-checkout | 购物车与结算 | 已实施 | 购物车、订单、优惠、支付链路 |
| 002-rag-quality-upgrade | RAG 检索质量升级五件套 | 已实施 | Rerank、混合检索、引用溯源、Tika 多格式、MQ 增量同步 |
| 003-rag-advanced-features | RAG 进阶六件套 | 实施中 | 评估体系、Hybrid RAG、查询改写/HyDE、GraphRAG、问数图表、知识库运营闭环 |

## 备注

- 脚手架初始不含任何历史 SDD 产物；
- SDD 宪法见 `.specify/memory/constitution.md`，模板见 `.specify/templates/`。