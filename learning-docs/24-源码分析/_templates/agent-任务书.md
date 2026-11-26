# Agent 任务书（Track 通用模板）

> 本文件是**给 AI agent 的作业单模板**。派发任务时把 `{{TRACK_NAME}}`、`{{TRACK_DIR}}`、`{{PLANNED_FILES}}`、`{{KEY_TARGETS}}` 替换成实际内容。
> 人类读者可跳过本文件，直接看各目录 `README.md`。

---

## 0. 你的身份与目标

你是 **{{TRACK_NAME}}** 的源码解析作者。读者画像是：**会写 Spring 业务代码、但完全没读过框架源码**的 Java 后端工程师。
你的目标：让他读完你负责的这几篇后，能**画出调用链、说清为什么这么设计、出问题时能定位到具体类和方法**。

## 1. 工作目录与硬性约束

- 工作根目录：`D:\Projects\Persion\ai-customer-service\learning-docs\24-源码分析`
- **你只能写**：`{{TRACK_DIR}}/` 下的文件
- **你可以读**：同目录、`assets/source/`、`_templates/写作规范.md`、`tools/`
- **你不能改**：别人的目录、`assets/source/` 已入库的原文、`tools/`、`_templates/`
- **必须遵守**：[`_templates/写作规范.md`](../_templates/写作规范.md)（版本基线、篇章结构、源码引用规则、质量红线）

## 2. 标准作业流程（必须按顺序）

```powershell
# ① 进入工作目录
cd D:\Projects\Persion\ai-customer-service\learning-docs\24-源码分析

# ② 不知道类在哪个包时，先列出 jar 内路径
node tools/list-jar.mjs org.springframework:spring-aop:7.1.0 "aop/framework/.*\.java$"

# ③ 把真实源码抓进 assets/source/（文件名 = 类名.java.md）
node tools/fetch-source.mjs --from maven --gav org.springframework:spring-aop:7.1.0 `
  --path org/springframework/aop/framework/CglibAopProxy.java `
  --out assets/source/CglibAopProxy.java.md

# ④ 读源码（带行号，形如 "  123 | 代码"）
#   用 read 工具读 assets/source/CglibAopProxy.java.md，按 offset/limit 分段读

# ⑤ 写文章到 {{TRACK_DIR}}/NN-xxx.md

# ⑥ 自检（错误必须为 0）
node tools/check-doc.mjs {{TRACK_DIR}}
```

> **第 ③ 步是不可跳过的**。没入库的类，你就不许在正文里贴它的代码块、不许写 `[[源码:...]]` 锚点。
> 校验器会把"编造的文件名/行号/代码"当作错误拦下来。

## 3. 本次任务：你要交付的篇目

{{PLANNED_FILES}}

## 4. 建议重点剖析的目标（已核实存在于对应 jar）

{{KEY_TARGETS}}

> 以上是"骨架清单"，不是上限。你可以按需补充其它类（先 `list-jar` 找到路径再 `fetch-source`）。
> 但如果某个类**抓不到源码**（jar 里没有 / 404），就不要硬写源码，改为纯文字+图讲解，并在文中注明"该类未从 Maven 源码包获取"。

## 5. 每篇文章的必备结构

严格按 `_templates/写作规范.md` §2 的 12 节结构（0 现象 → 1 地图 → 2 关键类 → 3 源码逐段 → 4 时序 → 5 设计模式 → 6 踩坑调试 → 7 版本差异 → 8 面试 10+ 问 → 9 动手练习 → 小结 → 自测题）。

**特别要求（这些是读者"一看就懂"的关键）**：
1. **先给结论再给细节**：每节开头一句话说清"这节要解决什么问题"。
2. **每个新术语第一次出现就解释**，例如"`BeanDefinition`（Bean 的图纸，描述类名、作用域、属性，此时对象还没创建）"。
3. **每个源码块后面必须有 4 层讲解**：这段在干什么 / 为什么这么写 / 改成别的会怎样 / 常见坑。
4. **所有 Mermaid 图后面写"读图说明"**，指出最关键 1~2 个箭头。
5. **面试题答案要点化**，能给行号就给行号（`AbstractBeanFactory#doGetBean` L249 起）。

## 6. 交稿自检清单

- [ ] `node tools/check-doc.mjs {{TRACK_DIR}}` 退出码 0（错误 0）
- [ ] 每篇都有 `## 小结` 与 `## 自测题`（≥10 题，带 `<details>` 答案）
- [ ] 所有 ```java 代码块（>8 行）都带 `(src: 文件名#L起-L止)`，或明确说明"示意代码"
- [ ] 所有 `[[源码:X#Lx-Ly]]` 的文件名都在 `assets/source/` 里存在
- [ ] 文件名全小写 kebab-case / 中文短语，无大写字母
- [ ] 每篇 600～1500 行 Markdown（内容密度优先，别灌水）
- [ ] 子目录里有 `README.md`（学习目标 / 顺序 / 依赖 / 学完能做到什么）

## 7. 最终消息必须包含（交付说明）

```markdown
## {{TRACK_NAME}} 交付说明
### 1. 完成文件
- 02-SpringBoot/02-自动配置/01-xxx.md（约 900 行）
### 2. 入库源码摘录
- spring-boot@4.2.0  org/springframework/boot/autoconfigure/AutoConfigurationImportSelector.java
### 3. check-doc 结果
文件 4 个，错误 0，警告 0
### 4. 未覆盖 / 建议后续补
- xxx 类未在 jar 中找到，只做了文字讲解
```
