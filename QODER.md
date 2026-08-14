# Qoder 项目说明

本项目使用 `CLAUDE.md` 作为 AI Agent 项目规则的唯一事实来源。

在进行任何需求分析、方案设计、代码修改、重构、测试、命令执行或文件变更之前，必须先读取并遵循项目根目录下的 `CLAUDE.md`。

## 规则优先级

1. 用户当前明确提出的要求优先级最高。
2. Spec Kit 工作流相关规则以 `.specify/` 中的规范、宪法和 `specs/` 下的规格文档为准。
3. 项目级通用规则以 `CLAUDE.md` 为准。
4. `QODER.md` 仅作为 Qoder 的入口桥接文件，不重复维护项目规则。

## 执行要求

- 必须先读取 `CLAUDE.md`，再开始处理任务。
- 必须将 `CLAUDE.md` 中的编码规范、架构规则、测试要求、工作流规则、安全约束视为强制要求。
- 如果 `QODER.md` 与 `CLAUDE.md` 存在冲突，以 `CLAUDE.md` 为准。
- 不要复制、改写或重新解释 `CLAUDE.md` 中的规则，应直接应用其中的规则。
- 在执行 Spec Kit 任务时，仍然必须遵循 `CLAUDE.md` 中的项目规则。

## Spec Kit 使用说明

本项目使用 GitHub Spec Kit。

Qoder 相关命令位于：

- `.qoder/commands/`

Spec Kit 公共资产位于：

- `.specify/`
- `specs/`

执行 Spec Kit 工作流时，应按以下顺序推进：

1. `/speckit.constitution`
2. `/speckit.specify`
3. `/speckit.clarify`
4. `/speckit.plan`
5. `/speckit.tasks`
6. `/speckit.analyze`
7. `/speckit.implement`

在执行上述任一命令前，都必须先确认已经读取并遵循 `CLAUDE.md`。
