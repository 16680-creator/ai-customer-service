# Git 工作流规范（Git Workflow）

> 本文件定义项目 Git 工作流规范。

---

## 分支策略

| 分支 | 用途 | 命名规范 |
|------|------|---------|
| `main` | 生产代码，受保护 | - |
| `develop` | 开发主线 | - |
| `feature/*` | 功能开发 | `feature/<编号>-<短描述>` |
| `hotfix/*` | 紧急修复 | `hotfix/<编号>-<短描述>` |
| `release/*` | 发布准备 | `release/<版本号>` |

## 提交规范

### Commit Message 格式

```
<type>: <中文描述>

[可选正文]

[可选脚注]
```

### Type 枚举

| type | 说明 | 示例 |
|------|------|------|
| feat | 新功能 | `feat: 新增 AI 对话流式响应` |
| fix | Bug 修复 | `fix: 修复知识库文档上传空指针` |
| docs | 文档变更 | `docs: 更新部署指南` |
| style | 代码格式（不影响逻辑） | `style: 统一缩进格式` |
| refactor | 重构 | `refactor: 抽取消息服务公共逻辑` |
| test | 测试 | `test: 补充用户服务单元测试` |
| chore | 构建/工具/依赖 | `chore: 升级 Spring Boot 版本` |

### 提交规则

1. 每次提交只做一件事
2. 描述至少 10 个中文字
3. 禁止提交含密钥/密码的文件
4. 提交前确保编译通过

## SDD 分支联动

- `/speckit.specify` 自动创建 `feature/<编号>-<短描述>` 分支
- 每个 SDD 阶段的产出物提交到对应分支
- 门禁通过后合并到 `develop`
- 发布时从 `develop` 创建 `release/*` 分支

## 合并规则

1. Feature 分支合并到 develop 必须通过 Challenger 门禁
2. 使用 Squash Merge 保持主线整洁
3. 合并前必须解决所有冲突
4. 禁止 Force Push 到 main / develop
