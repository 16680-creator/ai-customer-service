# Git 进阶与误操作恢复

> 对应项目：`.gitattributes`（行尾符规则）、`git log --all`（205 条提交，含乱码提交 `ca34864`、占位提交 `db800ed`，是真实的"事故现场"）、仓库内已入库的大文件 `reports/echarts.min.js`（约 1MB）。
> 相关：[01-Git工作流与分支模型](./01-Git工作流与分支模型.md)（rebase 在工作流中的位置）、[03-提交规范与变更日志](./03-提交规范与变更日志.md)（整理提交时顺手修标题）。

---

## 一、总原则与急救速查

**两条保命原则：**
1. Git 几乎不会真的丢东西——只要提交过（甚至只是 add 过），对象就在 `.git/objects` 里，`reflog` 能找回来；**没提交就被覆盖/删除的，才真丢**。
2. 所有破坏性操作（`reset --hard`、`rebase`、`push --force`）执行前，先 `git status` + `git branch` 确认"我在哪、现在是什么状态"。

> **沙盒声明**：本篇所有"急救场景"都在临时目录的沙盒仓库中练习，不影响本项目仓库。每个场景可直接整段复制到 Git Bash 里跑并看到结果。

| 症状 | 解法 | 场景号 |
|---|---|---|
| 提交错分支了 | `reset --soft` + 切分支重提 | ① |
| 提交信息写错 | `commit --amend`（未推送）/ `rebase -i` + force-with-lease（已推送） | ② |
| `reset --hard` 手滑丢代码 | `git reflog` → `reset --hard HEAD@{n}` | ③ |
| 分支删了/提交"消失" | `reflog` 找 sha → `git branch <名> <sha>` | ④ |
| 一串碎提交想合并 | `git rebase -i HEAD~n` | ⑤ |
| `stash pop` 冲突了 | 冲突不会删 stash，先恢复再手动 drop | ⑥ |
| 上周哪个提交引入的 bug | `git bisect` 二分 | ⑦ |
| 想要别的分支上的某次改动 | `git cherry-pick <sha>` | ⑧ |

---

## 二、八个"误操作急救"场景（沙盒可复制验证）

### 场景①：提交到了错误的分支

```bash
SB=$(mktemp -d) && cd $SB && git init -q repo && cd repo
git config user.email t@t && git config user.name t
echo base > a.txt && git add . && git commit -qm "init"
git checkout -qb hotfix
echo fix > fix.txt && git add . && git commit -qm "fix: 紧急修复"   # 哎，本想提到 main

# 急救：软回退（改动回到暂存区），切回正确分支重新提交
git reset --soft HEAD~1
git stash -q                       # 暂存改动，避免切分支带走
git checkout -q main
git stash pop && git commit -qm "fix: 紧急修复"
git log --oneline main             # ✔ fix 落到 main，hotfix 分支干净
```

**原理**：`--soft` 只移动分支指针，工作区与暂存区原封不动；用 `--mixed`（默认）会顺带清空暂存区，`--hard` 会**连工作区一起覆盖**——急救时永远先想 `--soft`。

### 场景②：提交信息写错了

```bash
# 未推送：直接改最近一条
echo x > b.txt && git add . && git commit -m "fxi: 拼错了"
git commit --amend -m "fix: 修好拼写"
git log --oneline -1               # ✔ 标题已替换（sha 变了，历史被改写）

# 已推送：amend 后用 --force-with-lease 推送（见 §四），不要裸 --force
```

> 本仓库真实案例：`db800ed`、`be28767` 两条提交的标题是 `1111111111111111111111111`——如果发现得早，一条 `amend` 就能救；已推送且被人拉取后，改历史就要协调所有人。

### 场景③：`reset --hard` 之后想找回丢弃的代码

```bash
cd $SB && git checkout -q main
echo v1 > work.txt && git add . && git commit -qm "feat: 干了一天的活"
echo v2 > work.txt                                             # 未提交的新改动
git reset --hard HEAD                                          # 手滑：工作区 v2 丢了！

# 提交过的 v1 一定能找回；未提交的 v2 看下面"唯一的例外"
git reflog | head -5
# 输出示例：a1b2c3d HEAD@{0}: reset: moving to HEAD
#           e4f5g6h HEAD@{1}: commit: feat: 干了一天的活
git reset --hard HEAD@{1}          # 回到 reset 前的位置
cat work.txt                       # ✔ v1 回来了
git reset --hard e4f5g6h           # 用 sha 定位，效果相同
```

**唯一的例外**：**从未 add 过的改动**（v2）不会进入对象库，`reset --hard` 后无法找回。所以防丢的肌肉记忆是：**干到一半先 `git add`（甚至 `git stash`），让改动进对象库**。

### 场景④：误删分支

```bash
git branch -D feature-x            # 后悔了！
git reflog | grep feature-x        # 从 reflog 里捞出该分支顶端的 sha
# 或全量搜索：git log -g --oneline | head
git branch feature-x <sha>         # 分支原地复活（提交从未被删除）
```

`git branch -D` 只删了"指向"，提交对象要等 30 天（gc 过期）才真正清理，reflog 窗口内都能救。

### 场景⑤：一串碎提交整理成一条（交互式 rebase）

```bash
git checkout -qb tmp
for i in 1 2 3; do echo $i >> log.txt; git add .; git commit -qm "wip:$i"; done
git log --oneline -4               # wip:3 / wip:2 / wip:1 / init

# 交互式 rebase：把最近 3 条 squash 成 1 条
GIT_SEQUENCE_EDITOR="sed -i '2,\$s/^pick/squash/'" git rebase -i HEAD~3
# GIT_SEQUENCE_EDITOR 只是替你自动填写了编辑器里的内容：
#   pick  wip:1
#   squash wip:2
#   squash wip:3
git log --oneline -2               # ✔ 只剩 1 条合并后的提交
```

手动练习时直接跑 `git rebase -i HEAD~3`，在弹出的编辑器里把第 2、3 行的 `pick` 改成 `squash`（缩写 `s`，保留改动并合入上一条；`fixup`/`f` 则丢弃提交消息）即可。

> 本仓库 `feature/ai-chat-and-frontend` 领先 master 63 个提交，合并前用这个手法按模块整理，历史可读性会好一个量级。

### 场景⑥：`stash pop` 冲突——最容易踩的坑

```bash
git checkout -q main
echo A > c.txt && git add . && git commit -qm "A"
echo "my edit" >> c.txt && git stash          # 改动入 stash
echo "remote edit" >> c.txt && git add . && git commit -qm "B"
git stash pop                                  # ⚠️ 冲突！
git stash list                                 # 关键：stash 还在，没有被删！
# 正确流程：
git checkout --theirs c.txt   # 或手动编辑解决冲突（按需选 ours/theirs）
git add c.txt
git stash drop                                 # ⭐ 必须手动清理，否则 stash 越积越多
git stash list                                 # ✔ 干净了
```

**stash 三大陷阱**：

| 陷阱 | 后果 | 正确姿势 |
|---|---|---|
| `pop` 冲突时以为"弹出了就没了" | stash 实际**保留**，之后 `apply` 会重复应用 | 冲突解决后手动 `git stash drop` |
| 新建文件没 `stash -u` | untracked 文件根本不进 stash，工作区"莫名其妙"带着它切分支 | 习惯性 `git stash -u` |
| `stash` 长期当草稿箱 | 多个 stash 无法区分内容与时间，`apply` 错条目 | stash 只做小时级暂存，长期工作用分支 |

### 场景⑦：bisect 二分定位坏提交

```bash
cd $SB && git checkout -q main
for i in 1 2 3 4 5 6 7; do
  echo $i >> seq.txt && git add . && git commit -qm "step $i"
done
git checkout -q .
echo "BUG" > seq.txt && git add . && git commit -qm "step 8 (bug)"
git checkout -q HEAD~2

# 人工二分
git bisect start
git bisect bad HEAD           # 当前有问题
git bisect good HEAD~7        # 7 步前是好的 → Git 自动切到中间提交
git bisect bad                # 根据测试结果标记，约 log2(n) 步收敛
git bisect reset              # 结束，回到原分支

# 自动二分：写个测试脚本，Git 替你跑
git bisect start HEAD HEAD~7
git bisect run sh -c '! grep -q BUG seq.txt'   # 退出码 0=good，非 0=bad
git bisect reset
```

> 本仓库 205 条提交里排查"限流参数改坏了网关"这类回归，用 `bisect run` 配合 `scripts/loadtest/k6/gateway-rate-limit.js` 跑断言脚本，几分钟就能定位到具体 sha。

### 场景⑧：cherry-pick——只想要别的分支的某一次改动

```bash
git checkout -qb side main
echo s1 >> d.txt && git add . && git commit -qm "feat: side-1"
echo s2 >> d.txt && git add . && git commit -qm "fix: side-2 只想要这条"
git checkout -q main
git cherry-pick side~1         # 取 side 上倒数第 2 条（即 fix）
git log --oneline -2           # ✔ main 多了一条 fix，且是全新 sha
# 冲突时：解决 → git cherry-pick --continue；整体放弃 → git cherry-pick --abort
```

典型用途：hotfix 分支上的修复要"摘"回仍在开发的 release 分支；本仓库 `52c6c6c` 那种把整个分支 merge 进自己的场景之外，还有"只挑一条修复"的需求。

---

## 三、force-with-lease vs force

```bash
git push --force             # 无条件覆盖远端：协作者在它之后推的提交全部消失
git push --force-with-lease  # 仅当"远端还停留在我上次 fetch 时的位置"才覆盖
```

| | `--force` | `--force-with-lease` |
|---|---|---|
| 远端有别人的新提交 | **直接覆盖丢失** | 拒绝推送（lease 检查失败） |
| 语义 | "我的就是对的" | "我认为远端=我看到的版本，不是则停下" |

- 改写了自己已推送的分支（amend / rebase 后），永远用 `--force-with-lease`。
- 注意：IDE 图形界面里的"Force Push"多数走 `--force`，命令行操作更可控。
- 保护分支上直接禁止 force push（[01](./01-Git工作流与分支模型.md) §5），让误操作从"丢失数据"降级为"推送失败"。

---

## 四、大文件与 .gitattributes

### 4.1 本仓库现状

```bash
cat .gitattributes
# *.sh text eol=lf
# Jenkinsfile text eol=lf
# Dockerfile text eol=lf
# *.yaml text eol=lf
# *.yml text eol=lf
# *.md text eol=lf

git ls-files | xargs -I{} du -k "{}" | sort -rn | head -3
# 1008  reports/echarts.min.js          ← 约 1MB 的第三方库直接入库
#  996  ai-cs-frontend/public/images/test-earphone.png   ← 约 1MB 的测试图片
#   68  ai-cs-frontend/package-lock.json
```

`.gitattributes` 的**行尾符规则已落地**（Windows 仓库防 CRLF/LF 漂移的关键，`*.sh` 尤其重要——CRLF 会让 shell 脚本在 Linux 容器里直接报错），但没有配置大文件策略。

### 4.2 大文件问题与对策

Git 对二进制文件无法差量压缩：`echarts.min.js` 每次升级都会整份存进历史，克隆体积线性膨胀；PNG 更是永远整份存储。

| 方案 | 适用 | 做法 |
|---|---|---|
| **Git LFS** | 必须进仓库的大二进制（设计稿、模型、数据集） | `git lfs install` → `git lfs track "*.png" "*.min.js"` → `.gitattributes` 出现 `filter=lfs` 条目；⚠️ **工程未落地**，本仓库尚未使用 |
| 构建产物不入库 | npm 包、min.js 这类可由 lockfile 还原的依赖 | 从仓库删除，构建时下载（echarts.min.js 可在 CI/前端构建中产出） |
| 大文件放对象存储 | 超大或需 CDN 的资源 | 仓库只存 URL/指纹 |

> 历史里已经入库的大文件，`git rm` 只删当前版本不删历史，需要 `git filter-repo` 重写历史——破坏性操作且改 sha，单人仓库趁早做，多人仓库要全员协调。

---

## 五、rebase 冲突的通用处置与 rerere

场景⑤、⑧遇到冲突时（rebase/cherry-pick 都会"逐条重放、逐条停下"），处置流程是固定的：

```bash
git status                        # 看哪些文件 both modified
#  ↓ 手动编辑冲突文件（<<<<<<< ======= >>>>>>> 三段标记）
git add <文件>
git rebase --continue             # 处理完当前提交，重放下一条（可能再次停下）
# 整体放弃：git rebase --abort（回到 rebase 前的状态，零损失）
# 跳过本条：git rebase --skip（丢弃当前这条重放的提交）
```

| 要点 | 说明 |
|---|---|
| rebase 冲突 vs merge 冲突 | merge 一次解完所有冲突；rebase **每重放一条提交都可能停一次**——重放 10 条最坏解 10 轮，所以整理长分支前先用 `git log --oneline` 预估轮次 |
| `--abort` 是安全词 | rebase 过程中原始提交始终完好，abort 即回原点；不放心可先 `git branch backup-<分支>` 留个指针 |
| rerere（reuse recorded resolution） | `git config rerere.enabled true`：Git 记住每处冲突"这次是怎么解的"，同样的冲突再次出现（反复 rebase 的分支很常见）自动套用，人不用重复劳动 |

**rerere 值得全局开启**（`git config --global rerere.enabled true`）——它只在你显式提交解决方案后生效，不会自动做任何危险的事，是少有的"零风险、纯收益"的 Git 配置。

---

## 六、面试高频问答

**Q1：`reset --hard` 之后提交还能找回来吗？**
A：能。`git reflog` 记录了 HEAD 的每一次移动，找到操作前的位置（`HEAD@{n}` 或 sha），`git reset --hard HEAD@{n}` 即可恢复。唯一无法恢复的是从未 `git add` 过的工作区改动——它从未进入对象库。所以防丢的关键习惯是经常 `add` 或 `stash`。

**Q2：rebase 和 merge 怎么选？**
A：原则是"改没改写共享历史"。自己的未合并 feature 分支同步主干用 rebase（线性、bisect 友好）；共享分支、集成分支用 merge（不改写历史）。已推送的分支 rebase 后必须 `push --force-with-lease` 并通知协作者重新拉取。

**Q3：`git fetch` 和 `git pull` 的区别？**
A：`fetch` 只下载远端新对象、更新远程跟踪分支，不动工作区；`pull` = fetch + merge（或 rebase）。排查与回退场景应先 `fetch` 后自行决定合并方式，盲 `pull` 产生的意外 merge 提交是历史脏污的常见来源。

**Q4：stash 有哪些坑？**
A：① `pop` 遇到冲突时 stash 不会被删除，解决冲突后要手动 `drop`；② untracked 文件默认不进 stash，需要 `-u`；③ 多个 stash 无法直观区分，只适合小时级暂存，长期工作应开分支。恢复冲突场景的正确流程：恢复冲突 → 编辑/`checkout --theirs` → `add` → `stash drop`。

**Q5：为什么用 `--force-with-lease` 而不是 `--force`？**
A：`--force` 无条件覆盖远端，会静默吞掉协作者在你上次 fetch 之后推送的提交；`--force-with-lease` 带"租约"检查——仅当远端仍停留在你看到的版本时才覆盖，否则拒绝。它把"误删别人工作"从静默事故变成一次推送失败。

**Q6：bisect 的原理和时间复杂度？**
A：提交历史是线性序列，good 和 bad 各标记一个端点后，问题必然落在中间区间，bisect 每次取中点让测试者（人或脚本）标记，n 个提交约 log₂n 步收敛；`git bisect run` 提供判定脚本后可全自动执行。前提是提交历史里每个点都可构建、可测试——这正是保持提交原子性的工程理由。

**Q7：仓库里已经提交了不想要的大文件怎么办？**
A：先在 `.gitattributes` 配 LFS 或把文件移出仓库（构建时下载/对象存储）；但 `git rm` 只影响当前版本，历史中的 blob 还在，需要 `git filter-repo` 重写历史，属于破坏性操作——会改写所有 sha，多人仓库必须全员协调并备份，所以关键是**事前用 LFS/pre-commit 大小检查防住**，而不是事后清洗。

**Q8：`.gitattributes` 除了行尾符还能管什么？**
A：① `eol`/`text`/`binary` 控制换行与二进制识别（跨平台协作必备）；② `filter=lfs diff=lfs merge=lfs` 把指定模式交给 Git LFS；③ `linguist-language` 等仓库平台展示属性。本仓库已用它强制 `*.sh`/`*.yml`/`*.md` 为 LF。

---

## 七、动手练习

1. 把本篇 8 个场景在沙盒里全部跑通一遍；然后把场景③改为"`reset --hard` 前忘了提交"，验证"未 add 的改动真的找不回来"，亲手建立对例外条款的敬畏。
2. 用 `git bisect run` 在沙盒里造 20 个提交、在第 13 个埋一个坏点，写一个 `test.sh` 断言脚本，统计实际二分步数，验证 log₂(20)≈4.3。
3. 检查本仓库：`git stash list` 是否有陈旧条目？`git reflog | head -20` 里有没有值得记住的"上周我在干什么"？给 stash 建立"条目数清零"的周检查习惯。
4. 给本仓库起草一份 `.gitattributes` 增补方案：评估 `reports/echarts.min.js` 与 `ai-cs-frontend/public/images/*.png` 应走 LFS 还是移出仓库，写出迁移步骤与风险（历史重写、协作者协调）。
5. 演练一次真实的整理：在 `feature/ai-chat-and-frontend` 的**本地副本分支**上对最近 10 个提交做 `rebase -i`（pick/squash/fixup/reword 各用一次），`git log --oneline` 对比整理前后——注意全程不要动共享分支。

---

> 上一篇：[01-Git工作流与分支模型](./01-Git工作流与分支模型.md) ｜ 下一篇：[03-提交规范与变更日志](./03-提交规范与变更日志.md)
