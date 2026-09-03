# Git 工作流与分支模型

> 对应项目：`git branch -a`（本地 5 条分支 + 远端 8 条）、`git log --oneline -30`（205 条提交历史）、`specs/001-cart-checkout` ~ `specs/005-after-sales-agent`（规格编号即分支名）。
> 相关：[03-提交规范与变更日志](./03-提交规范与变更日志.md)（分支上跑的提交长什么样）、[04-CodeReview实战](./04-CodeReview实战.md)（合并前的评审环节）、[05-需求拆解与估时](./05-需求拆解与估时.md)（一个分支对应一份规格）。

---

## 一、结论先行：三种主流模型对比

| 维度 | trunk-based（主干开发） | GitHub Flow | Git Flow |
|---|---|---|---|
| 常驻分支 | 只有 `main` | `main` + 短命 feature | `master`/`develop`/`release`/`hotfix`/`feature` |
| feature 分支寿命 | **小时级**（<1~2 天） | 天级（1~7 天） | 周级（数周） |
| 发布模型 | 持续发布 + 特性开关 | 合入即可发布 | 定期从 develop 拉 release |
| 对 CI/自动化要求 | 极高（合入前必须有门禁） | 高 | 中 |
| 适合团队 | 高成熟度、高频发布、强 CI | 中小团队、SaaS/持续部署 | 有版本化交付、多环境并行维护 |
| 主要风险 | 半成品进主干（需特性开关兜底） | 分支漂久了合不动 | 分支太多，合并地狱 |

**选型口诀**：CI 强、发布勤 → trunk-based；团队小、想简单 → GitHub Flow；多版本交付、发布节奏慢 → Git Flow。**没有 CI 门禁兜底的 trunk-based 是灾难**——这是先有门禁（[08](./08-质量门禁与自动化.md)）还是先有工作流的经典先后依赖。

---

## 二、本仓库现状分析（如实描述，`git branch -a` / `git log` 实测）

### 2.1 分支布局

```
本地分支：
  002-rag-quality-upgrade
  004-vlm-multimodal
  005-after-sales-agent
* feature/ai-chat-and-frontend
  master

远端分支（origin = github.com/16680-creator/ai-customer-service.git）：
  origin/001-cart-checkout
  origin/002-rag-quality-upgrade
  origin/003-rag-advanced-features
  origin/004-vlm-multimodal
  origin/005-after-sales-agent
  origin/HEAD -> origin/master
  origin/feature/ai-chat-and-frontend
  origin/master
```

### 2.2 三个可量化的观察

| 观察 | 证据 | 说明 |
|---|---|---|
| **规格编号做分支名** | 本地/远端共 8 条 feature 分支，全部形如 `001-cart-checkout`、`005-after-sales-agent`，与 `specs/` 目录一一对应 | 命名即文档索引，看到分支名就能找到规格，这是本仓库做得好的地方 |
| **长生命周期 feature 分支** | `git rev-list --count master..feature/ai-chat-and-frontend` = **63**（master 落后 0，未分叉） | 63 个提交攒在一条分支上，一次性合回 master 的评审与回滚粒度都过大 |
| **合并靠手动 merge，无 PR** | 全部合并记录只有 2 条：`4da4838 merge: 合并 005-after-sales-agent 到 master`、`52c6c6c Merge remote-tracking branch 'origin/002-rag-quality-upgrade' into 002-rag-quality-upgrade` | 单人/极小团队直接 push，没有 PR 评审环节；`merge:` 也不是 Conventional Commits 的合法 type（见 [03](./03-提交规范与变更日志.md)） |

### 2.4 远端分支的两个细节观察

```bash
git branch -a        # 本地没有 001-cart-checkout 与 003-rag-advanced-features，
                     # 但远端有 origin/001-cart-checkout、origin/003-rag-advanced-features
```

| 观察 | 含义 | 教训 |
|---|---|---|
| 本地删了已合并的规格分支，远端还在 | 本地做过清理、远端没做（或反之） | 分支清理要**两端成对**：`git push origin --delete <分支>`，否则 `git branch -a` 会一直显示过期远端分支 |
| `remotes/origin/HEAD -> origin/master` | 远端默认分支是 master | 保护规则应配置在 master 上（§五） |
| 五个规格分支中 003 只有远端 | "实施中"的功能没有本地工作副本 | 长期不动又未合并的远端分支 = 潜在的"僵尸分支"，应在规格状态表（`specs/README.md` 的"已有功能"表）里同步标注分支处置方式 |

### 2.5 分支与规格目录的双向核对（可直接复制）

分支命名依赖"与 `specs/` 一一对应"这条约定，约定靠命令来维护：

```bash
# 逐个检查远端规格分支是否有对应的规格目录
for b in $(git branch -r | grep -oE '[0-9]{3}-[a-z-]+' | sort -u); do
  [ -d "specs/$b" ] && echo "$b  ✔ 有规格" || echo "$b  ⚠️ 无对应规格目录"
done
```

反方向的检查（规格目录有、分支已删）同样值得跑：`specs/README.md` 的"已有功能"状态表每更新一条状态，就该核对一次分支是否可以清理——**状态表是唯一事实源，分支是它的影子**。

### 2.6 结论：处于"轻量特征分支"阶段

```
现状：master + 长命 feature/规格分支，手动 merge 合回，无 PR、无保护
       │
       ├─ 比 Git Flow 简单：没有 develop/release/hotfix 分支层
       ├─ 比 GitHub Flow 粗放：分支寿命周级、合并不经 PR、master 无保护
       └─ 已有的资产：规格编号命名（好习惯）、提交 type(scope) 风格（见 03）
```

**对照选型口诀**：本项目是自学/单人为主 + 偶尔多人协作的仓库，**GitHub Flow 是最合适的演进方向**——保留 `001~NNN` 命名，把"长分支攒 63 个提交"改为"一个用户故事一条短命分支 + PR 合入"，master 上加保护规则。trunk-based 目前没有 CI 门禁兜底（`Jenkinsfile:17` 测试默认跳过），不宜激进。

---

## 三、三种模型的工作机制详解

### 3.1 trunk-based：一切围绕主干

```
main ──●────●────●────●────●──→  持续发布
        ↖↑↗  ↖↑↗
     短命分支（几小时~2天）：
     只装一个小改动，尽快合回
     半成品用特性开关（Feature Flag）藏起来
```

- 核心不是"不用分支"，而是**分支寿命压缩到小时级**，合并冲突趋近于零。
- 代价：必须配三件套——特性开关、小步提交、合入前自动门禁。
- Google/Facebook 内部均为 trunk-based；开源项目少见，因为无法强制外部贡献者的分支寿命。

### 3.2 GitHub Flow：分支 + PR + 保护

```
main ──●───●────●───●──→
        \  \   /
  feature ─●─●─   ← 从 main 拉出
              ↓
        开 PR → CI 跑 → 评审 → squash merge → 删分支
```

- 规则只有四条：① `main` 随时可发布；② 改动开分支；③ 开 PR 评审；④ 合入即部署。
- **PR 是评审与门禁的载体**，这是它与"直接 push"的本质区别。

### 3.3 Git Flow：为版本化交付而生

| 分支 | 从哪拉 | 合到哪 | 寿命 | 用途 |
|---|---|---|---|---|
| `master` | — | — | 永久 | 只放发布点，打 tag |
| `develop` | master | master（经 release） | 永久 | 集成分支 |
| `feature/*` | develop | develop | 天~周 | 功能开发 |
| `release/*` | develop | master + develop | 天 | 发布前固化 + 修 bug |
| `hotfix/*` | master | master + develop | 小时 | 线上紧急修复 |

- 适合"卖安装包/多客户版本并行"的场景；纯 SaaS 用它是自找复杂度。
- 本仓库没有多版本交付诉求，**不需要引入 develop 层**。

---

## 四、合并策略：merge vs squash vs rebase

### 4.1 三种策略对比

| 策略 | 历史 | 优点 | 缺点 | 适用 |
|---|---|---|---|---|
| **merge**（`git merge --no-ff`） | 保留全部分支结构 + 合并提交 | 上下文完整、可回滚整批 | 历史图复杂，中间提交噪音多 | 集成分支（develop→master）、多人共享分支同步 |
| **squash merge** | 压成 1 个新提交 | PR 历史=提交历史，一行一个功能，干净 | 丢失中间过程；重做需重看 PR | **GitHub Flow 合 PR 的默认选择** |
| **rebase**（`git rebase`） | 把提交搬到目标顶端，线性 | 历史完全线性，`bisect` 友好 | **改写历史**，共享分支禁止 rebase | 个人 feature 分支同步主干（拉完就 rebase） |

### 4.2 一条铁律 + 一套组合拳

**铁律：已推送到共享分支的提交，不 rebase（只加）；自己的未合并 feature 分支，随便 rebase。**

组合拳（GitHub Flow 标准姿势）：

```bash
# 1. 每天：把主干最新进展同步到自己的分支（不改写共享历史）
git checkout feat/xxx
git fetch origin
git rebase origin/main        # 或 merge，个人分支两者皆可

# 2. 合并前：把自己的脏提交整理干净（见 02 的 rebase -i）
git rebase -i origin/main

# 3. 合 PR：squash 成一个有完整描述的提交
git checkout main && git merge --squash feat/xxx
git commit   # 标题用 Conventional Commits，正文写"为什么"
```

### 4.3 本仓库的两种真实合并形态

```bash
git log --merges --oneline
# 4da4838 merge: 合并 005-after-sales-agent 到 master
# 52c6c6c Merge remote-tracking branch 'origin/002-rag-quality-upgrade' into 002-rag-quality-upgrade
```

| 形态 | 提交 | 特征 | 评价 |
|---|---|---|---|
| 规格分支 → master | `4da4838` | 整个功能一次性进入主干 | 单人项目可接受；批次过大时回滚与定位粒度粗（63 提交的 `feature/ai-chat-and-frontend` 若照此合并会更明显） |
| 远端分支 → 同名分支 | `52c6c6c` | 远端进度同步回本地同名分支 | 多机协作下的常规操作；消息是 Git 默认英文模板，与全库中文 `type(scope):` 风格不一致（见 [03](./03-提交规范与变更日志.md)） |

### 4.4 本仓库的改进点

- 实际：`4da4838 merge: 合并 005-after-sales-agent 到 master` 用普通 merge，保留了完整分支历史——对单人仓库这没问题；问题是**63 个提交一起进来，事后想定位"哪次提交引入回归"只能在整个批次里二分**。
- 改进（配合 [02](./02-Git进阶与误操作恢复.md) 的 rebase -i）：合并前把 63 条整理成按用户故事分组的少量提交再 squash merge；或干脆按用户故事拆成多条短命分支（见 [05](./05-需求拆解与估时.md) 的任务分组），每条独立评审合入。

### 4.5 从规格任务到分支的映射（本仓库的演进蓝本）

`specs/005/tasks.md` 的阶段划分天然就是分支切分方案——每个阶段有独立的"检查点"与"独立测试"，意味着它可以独立合入：

| specs/005 任务阶段 | 建议分支 | 合并策略 | 依据 |
|---|---|---|---|
| 阶段 1 基础层（错误码 + SQL） | `005-after-sales-foundation` | squash | 纯基建，无业务行为，先合后锁 |
| 阶段 2 ai-cs-order 售后命令 | `005-after-sale-order` | squash | US 独立测试：AfterSaleServiceTest |
| 阶段 3 ai-cs-product 推荐 | `005-after-sale-recommend` | squash | 与阶段 2 无文件交集，可并行 |
| 阶段 4+ Agent 编排与转人工 | `005-after-sale-agent` | squash | 依赖前两者，最后合 |

**判断一个任务阶段能不能单独成分支的判据**：tasks.md 里它是否有"**检查点**：xxx 独立可用"——有，就能切；没有，说明拆分粒度还不支持分支化，先回到 [05](./05-需求拆解与估时.md) 补拆解。

---

## 五、保护分支设计（⚠️ 工程未落地，本节为目标态）

> 仓库托管在 GitHub（`origin` 指向 `github.com/16680-creator/ai-customer-service.git`），以下为 GitHub 分支保护的落地建议；当前仓库任何人可直接 push master，**尚未配置任何保护规则**（远端仓库设置无法从本地 git 确认，按"单人直接 push"的实际工作方式推断为未开启，启用时按下表配置即可）。

| 规则 | 配置 | 理由 |
|---|---|---|
| Require pull request reviews | 1 人（单人项目可先关，但 CI 必开） | 评审是 PR 模型的核心环节 |
| Require status checks | `maven-test`、`jacoco-check` 必须通过 | 门禁进合并前层（见 [08](./08-质量门禁与自动化.md)） |
| Require linear history | 开 | 强制 rebase/squash 合入，bisect 友好 |
| Do not allow force pushes | 开 | 防误操作改写共享历史（对应 [02](./02-Git进阶与误操作恢复.md) force-with-lease） |
| Do not allow deletions | 开 | 防手滑删主干 |
| include administrators | 建议开 | 规则对所有人一致，避免"管理员绕过" |

单人项目的务实起步：先只开 **status checks + 禁 force push**，评审一条等多人协作时再开——门禁的边际价值比"自己审自己"高得多。

---

## 六、面试高频问答

**Q1：trunk-based、GitHub Flow、Git Flow 怎么选？**
A：看两个变量——发布节奏与 CI 成熟度。高频发布 + 强 CI 选 trunk-based（分支寿命小时级，特性开关藏半成品）；中小团队、想有评审环节又不想要复杂分支层，选 GitHub Flow；有多版本并行交付（卖安装包、多客户定制）才需要 Git Flow 的 release/hotfix 层。没有 CI 门禁时不要上 trunk-based。

**Q2：你们项目用的是什么分支模型？**
A：master + 规格编号命名的 feature 分支（`001-cart-checkout` ~ `005-after-sales-agent`，与 specs/ 目录一一对应）。可以指出不足：分支偏长（示例分支领先主干 60+ 提交），演进方向是 GitHub Flow——按用户故事拆短命分支、PR 合入、主干加保护。

**Q3：merge、squash、rebase 各适合什么场景？**
A：merge 保留完整拓扑，适合集成分支与同步共享分支；squash merge 把一个 PR 压成一个提交，适合合 PR（历史=功能清单）；rebase 产生线性历史，适合个人分支同步主干与合并前整理提交。铁律：共享分支上的提交绝不 rebase，因为改写历史会让协作者的本地仓库错乱。

**Q4：为什么 feature 分支寿命要短？**
A：三个原因：① 与主干的偏离越大，合并冲突与回归风险呈超线性增长；② 大分支的 Code Review 质量骤降（>400 行后问题发现率显著下降，见 04）；③ 出问题时的回滚粒度是整个批次而非单个改动。控制手段：按用户故事竖切分支、用特性开关隐藏未完成功能。

**Q5：保护分支一般保护什么？**
A：至少四类：① 禁止直接 push（必须走 PR）；② 必须通过指定 status checks（测试/覆盖率/静态扫描）；③ 禁止 force push 与删除；④ 线性历史（可选）。单人项目可先开②③，评审规则等人多了再开。

**Q6：`git merge --no-ff` 和默认 merge 有什么区别？**
A：默认 merge 在可以 fast-forward 时只移动指针、不产生合并提交，分支结构会丢失；`--no-ff` 强制生成合并提交，保证"这一批改动来自同一个 PR"在历史中可见，也支持整体 revert。团队协作的集成分支建议 `--no-ff`。

**Q7：长分支怎么安全地收敛？**
A：先 rebase 到最新主干解决冲突，再 `rebase -i` 把几十条小提交按主题 squash 分组，最后 squash merge 合入或拆成多个小 PR 分批合。关键是：整理只发生在自己的未合并分支上；已推送的共享分支用 `rebase` 后必须 `push --force-with-lease` 并通知协作者。

---

## 七、动手练习

1. 在本仓库跑 `git branch -a`、`git rev-list --count master..feature/ai-chat-and-frontend`、`git log --merges --oneline`，把结果与本篇 §2 的表格核对，写出一条你自己观察到的、本篇没提的分支使用特征。
2. `git log --oneline master..feature/ai-chat-and-frontend | wc -l` 数出领先提交数，再按 `git log --oneline master..HEAD | grep -oE "^[a-z]+\([a-z-]+\)"` 按模块分组——这就是"按用户故事拆短命分支"的拆分依据草稿。
3. 在 GitHub 仓库 Settings → Branches 给 `master` 添加保护规则：开启禁 force push + 禁删除（本仓库为单人项目，评审规则先不开），写一段 README 说明团队扩大后的启用计划。
4. 在临时目录建一个沙盒仓库，分别用 `git merge --no-ff` 与 `git merge --squash` 合并一条 3 提交的分支，`git log --graph --oneline` 对比两种历史的差别，体会"合并策略改变的是历史形态"。
5. 给本仓库写一页《分支约定》：命名沿用 `NNN-<短描述>`（与 specs/ 对应）、寿命上限（建议 ≤3 天）、合并策略（squash + Conventional Commits 标题）、master 保护规则，放进团队文档或 README。

---

> 下一篇：[02-Git进阶与误操作恢复](./02-Git进阶与误操作恢复.md)
