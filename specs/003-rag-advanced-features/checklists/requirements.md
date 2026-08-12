# 规格质量检查清单：RAG 进阶六件套

**Purpose**: 校验 specs/003-rag-advanced-features/spec.md 的完整性与可测试性
**Created**: 2026-08-12
**Feature**: [spec.md](../spec.md)

## 规格完整性

- [x] CHK001 用户故事按价值排序并分配优先级（P1/P2）
- [x] CHK002 每个用户故事有"独立测试"说明，可独立交付价值
- [x] CHK003 每个用户故事有可验证的验收场景（假设/当/则）
- [x] CHK004 边界情况已覆盖（降级、空数据、异常）
- [x] CHK005 功能需求可测试且无歧义（FR-001 ~ FR-015）
- [x] CHK006 成功标准可衡量（SC-001 ~ SC-006）
- [x] CHK007 成功标准不包含实现细节（仅描述结果）
- [x] CHK008 范围边界清晰（模块、默认行为、约束）
- [x] CHK009 依赖与假设已识别（LLM/ES/Neo4j/golden 集）

## 需求可测试性

- [x] CHK010 评估指标（Recall@k/MRR/HitRate/LLM 分）可计算可断言
- [x] CHK011 Hybrid 模式的开关与降级行为可测
- [x] CHK012 改写/HyDE 的生成与降级可测
- [x] CHK013 图谱链路命中/未命中/降级三分支可测
- [x] CHK014 图表类型判定（分布/单行/空）可测
- [x] CHK015 聚类分组与缺口判定可测

## Notes

- 待 `/speckit-plan` 阶段将 NEEDS CLARIFICATION 项在 research.md 中解决
- 图表与聚类涉及前端展示，验收以接口契约 + 前端组件渲染为准
