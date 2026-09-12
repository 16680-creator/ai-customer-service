# HTML 与 CSS 基础

> **前置知识**：无，本章是前端系列的起点
> **关联篇目**：[00-前端技术全景介绍](./00-前端技术全景介绍.md) · [02-JavaScript核心基础](./02-JavaScript核心基础.md) · [04-Vue3核心基础](./04-Vue3核心基础.md)
> **对应项目**：`ai-cs-frontend/index.html`、每个 `.vue` 文件里的 `<template>`（HTML）与 `<style>`（CSS）

---

## 一、浏览器里发生的事：从 URL 到页面

### 1.1 从 URL 到像素的完整链路

你写惯了 `Controller → Service → Mapper` 的分层，浏览器渲染也有一条类似的流水线：

```
输入 URL
  → DNS 解析 + TCP/TLS 握手        （≈ 建立数据库连接）
  → HTTP 拿到 index.html           （≈ 拿到一份"启动脚本"）
  → 解析 HTML 构建 DOM 树          （≈ 加载类、构建 BeanDefinition）
  → 解析 CSS 构建 CSSOM 树         （≈ 读取配置与主题）
  → 合成渲染树 → 布局（Layout）→ 绘制（Paint）
```

关键区别：Java 应用启动一次常驻内存，浏览器是"边加载边渲染、随时可重渲染"——HTML/CSS/JS 变动都可能触发重排（Layout）与重绘（Paint），这是后续性能优化的切入点。

### 1.2 三层职责分工与 Java 类比

一张网页永远由三层构成（Vue 组件只是把三层拆进一个文件），职责泾渭分明：

| 层 | 前端角色 | Java 后端类比 | 职责 |
| --- | --- | --- | --- |
| HTML | 结构层 | 数据库表结构 / 实体类 | 定义"有什么内容"：骨架与语义 |
| CSS | 表现层 | 皮肤主题 / 样式配置 | 定义"长什么样"：颜色、间距、布局 |
| JavaScript | 行为层 | Service 业务逻辑 | 定义"怎么交互"：事件、请求、状态变更 |
| 浏览器 | 运行环境 | JVM | 解析执行前三层，提供 DOM API 与事件循环 |

---

## 二、HTML 速成

### 2.1 文档骨架

```html
<!-- index.html：ai-cs-frontend 的入口模板（节选） -->
<!DOCTYPE html>          <!-- 声明 HTML5 标准，必须是第一行 -->
<html lang="zh-CN">      <!-- 根节点，lang 影响屏幕阅读器与翻译 -->
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>AI 客服管理端</title>
    <script type="module" src="/src/main.js"></script>
  </head>
  <body>
    <div id="app"></div> <!-- Vue 挂载点：整个前端应用都渲染进这个空 div -->
  </body>
</html>
```

### 2.2 常用标签速览

```html
<!-- demo.html -->
<h1>一级标题</h1>          <!-- h1~h6，类比 Markdown 的 #~###### -->
<p>这是一个段落。</p>
<div>块级容器：独占一行，布局主力</div>
<span>行内容器：随文字流动，包一小段文字</span>
<ul><li>列表项一</li><li>列表项二</li></ul>   <!-- 无序列表 ≈ List -->
<img src="/logo.png" alt="站点 Logo" />       <!-- alt：加载失败时的替代文本 -->
<a href="https://example.com" target="_blank">超链接</a>
```

### 2.3 语义化标签：给页面写"字段注释"

```html
<!-- layout.html：语义化骨架 -->
<header>  <!-- 页头：Logo、登录信息 -->
  <nav>   <!-- 导航菜单 -->
    <a href="/chat">AI 对话</a> <a href="/user">用户管理</a>
  </nav>
</header>
<main>    <!-- 主内容区，一个页面只应有一个 -->
  <section><h2>会话列表</h2>...</section>  <!-- 一块主题内容 -->
</main>
<footer>版本号、备案号</footer>
```

两大意义：
1. **可访问性（a11y）**：屏幕阅读器靠 `nav/main/footer` 跳转朗读，视障用户才能用你的系统；
2. **SEO**：搜索引擎给 `header/main` 内内容更高权重（管理端 SEO 不敏感，但习惯要养成）。

类比：语义化标签 ≈ 给 DTO 写清晰字段名，而不是 `field1/field2`。

### 2.4 属性：id、class 与事件

| 属性 | 作用 | Java 类比 | 约束 |
| --- | --- | --- | --- |
| `id` | 全页面唯一标识 | 主键 | 不能重复；CSS 选择器尽量不用它 |
| `class` | 归类，可多个：`class="btn primary"` | 分类标签 / 角色 | CSS 选择器的主力 |
| `onclick` 等事件属性 | 内联事件回调 | 把 lambda 写进注解里 | ❌ Vue 中禁止，统一走 `@click` |

### 2.5 表单全家桶：v-model 的物质基础

表单是中后台 80% 的交互载体，也是后面 Vue `v-model` 的操作对象：

```html
<!-- login.html：手写登录页（动手练习一的原型） -->
<form action="/api/login" method="post">  <!-- 原生提交会整页跳转，Vue 里会被拦截 -->
  <label for="username">用户名</label>     <!-- for 指向 input 的 id：点文字即聚焦 -->
  <input id="username" name="username" type="text" placeholder="请输入用户名" />
  <label for="password">密码</label>
  <input id="password" name="password" type="password" />
  <textarea name="remark" rows="3">默认内容</textarea>   <!-- 多行文本 -->
  <select name="role">                                     <!-- 下拉框 -->
    <option value="user" selected>普通用户</option>         <!-- selected = 默认选中 -->
    <option value="admin">管理员</option>
  </select>
  <label><input type="checkbox" name="tags" value="vip" checked /> VIP</label>
  <label><input type="radio" name="gender" value="M" /> 男</label>  <!-- 同 name 互斥 -->
  <label><input type="radio" name="gender" value="F" /> 女</label>
  <button type="submit">登录</button>  <!-- type=reset 清空 / type=button 纯按钮 -->
</form>
```

**为什么现在就要学好表单**：表单元素天生有两样东西——`value`（当前值）和用户输入触发的 `input/change` 事件。Vue 的 `v-model` 本质就是「`:value` + `@input`」的语法糖。理解了表单，`v-model` 只是一层皮。

### 2.6 常见坏习惯

```html
<!-- ❌ table 做布局（table 只该用于展示表格数据） -->
<table><tr><td><div>侧边栏</div></td><td><div>内容</div></td></tr></table>
<!-- ❌ 行内样式滥用，样式散落无法复用 -->
<div style="color: red; font-size: 14px; margin: 8px;">错误信息</div>
<!-- ❌ div 堆山，毫无语义，屏幕阅读器和搜索引擎都读不懂 -->
<div class="top"><div class="menu">...</div></div>
<!-- ✅ 正确姿势：语义化标签 + class + 外部样式表 -->
<header class="app-header"><nav class="app-nav">...</nav></header>
<main class="app-main"><h2 class="panel-title">...</h2></main>
```

---

## 三、CSS 速成（重点）

### 3.1 三种引入方式

```html
<div style="color: red">1. 行内样式：优先级最高但最难维护，仅动态计算样式时用</div>
<style> /* 2. 内部样式：适合单页面演示 */ .tip { color: red; } </style>
<link rel="stylesheet" href="/assets/main.css" />  <!-- 3. 外部样式表（推荐）：可缓存 -->
```

类比：行内样式 ≈ 配置硬编码在代码里；外部样式表 ≈ 独立的 `application.yml`，可复用、可缓存。

### 3.2 选择器与优先级

```css
/* main.css */
.card { ... }              /* 类选择器：.class，最常用 */
#app { ... }               /* ID 选择器：#id，页面唯一 */
.card .title { ... }       /* 后代选择器：空格 = "里面任意层级" */
a:hover { color: red; }    /* 伪类：鼠标悬停 */
input:focus { outline: 2px solid #409eff; }  /* 伪类：获得焦点，表单必配 */
```

优先级按「（内联, ID, 类/伪类, 元素）」四位打分，**比大小而非求和**：

```css
/* 目标元素：<div id="app" class="box"><p>内容</p></div> */
p            { color: gray; }    /* (0,0,0,1) */
.box p       { color: blue; }    /* (0,0,1,1) 胜出上一条 */
#app p       { color: green; }   /* (0,1,0,1) 胜出上一条 */
#app .box p  { color: red; }     /* (0,1,1,1) 最终胜出 */
```

记忆法：**ID 是"王"，class 是"将"，元素是"兵"**。实在压不过才用 `!important`（能不用就不用）；覆盖 Element Plus 内置样式最常用"加深选择器 + Vue 的 `:deep()`"（Vue 篇细讲）。

### 3.3 盒模型：页面排版的原子单位

每个元素都是一个矩形盒子，从内到外四层：

```
┌──────────────────────────────── margin（外边距：与相邻盒子的距离）
│ ┌────────────────────────────── border（边框）
│ │ ┌──────────────────────────── padding（内边距：内容与边框的留白）
│ │ │  content（内容）          ← width/height 默认只算这一层
│ │ └────────────────────────────
│ └──────────────────────────────
└────────────────────────────────
```

类比：快递包裹——商品是 `content`，泡沫填充是 `padding`，纸箱是 `border`，包裹间预留的通道是 `margin`。

陷阱：默认 `box-sizing: content-box`，写 `width: 100px; padding: 10px` 实际占 120px。工程上全局改用 `border-box`（宽度已含 padding 与 border，符合直觉）：

```css
/* reset.css：所有现代项目的第一行 */
*, *::before, *::after { box-sizing: border-box; }
```

### 3.4 文档流与 display

元素默认按"文档流"排布，`display` 决定它怎么参与：

| display | 特征 | 典型标签 | 可设宽高？ |
| --- | --- | --- | --- |
| `block` | 独占一行 | div / p / h1 | 是 |
| `inline` | 随文字流动，宽高无效 | span / a / label | 否 |
| `inline-block` | 行内排布但可设宽高 | 原生 button / input | 是 |
| `flex` / `grid` | 容器级布局模式 | 布局容器 | — |

类比：`block` ≈ 独占一行的代码块，`inline` ≈ 行内代码跟随文字流动。

### 3.5 position 定位：参照物是唯一考点

| 值 | 参照物 | 是否占位 | 典型场景 |
| --- | --- | --- | --- |
| `relative` | 自身原位置偏移 | 占位 | 给 absolute 子元素当"坐标原点" |
| `absolute` | **最近的非 static 祖先** | 脱离文档流 | 角标、下拉面板 |
| `fixed` | 浏览器视口 | 脱离文档流 | 悬浮球、返回顶部 |
| `sticky` | 滚动容器 | 占位，滚过阈值后"钉住" | 表头吸顶、侧边目录 |

```css
/* badge.css：经典"头像右上角红点"——父子联手 */
.avatar { position: relative; }               /* 父：提供坐标系 */
.avatar .dot { position: absolute; top: -2px; right: -2px; width: 8px; height: 8px; border-radius: 50%; background: #f56c6c; }  /* 子：相对父定位 */
```

### 3.6 Flex 布局（重中之重）

后台系统 90% 的布局用 Flex 就够了。容器加 `display: flex`，**所有直接子元素成为 flex item**。先建立主轴/交叉轴直觉：

```
flex-direction: row（默认）：
┌──────────────────────────────────────┐
│  item1   item2   item3  ← 主轴(main)：水平 → │
│  ↑ 交叉轴(cross)：垂直 ↓             │
└──────────────────────────────────────┘
column 时主轴变垂直，justify-content / align-items 的方向随主轴对调
```

| 属性 | 作用 | 常用值 |
| --- | --- | --- |
| `flex-direction` | 主轴方向 | `row` / `column` |
| `justify-content` | 主轴对齐 | `center` / `space-between` |
| `align-items` | 交叉轴对齐 | `center` / `stretch`（默认） |
| `gap` | item 间距 | `8px` / `16px` |
| `flex-wrap` | 放不下是否换行 | `nowrap`（默认）/ `wrap` |
| `flex: 1`（子元素） | 占据剩余空间 | 弹性拉伸 |

**实战一：水平垂直居中（面试必考）**

```css
/* center.css */
.parent { display: flex; justify-content: center; align-items: center; }  /* 主轴+交叉轴都居中 */
```

**实战二：后台「顶部 + 侧边栏 + 内容」布局（ai-cs-frontend 的 App.vue 骨架）**

```css
/* app-layout.css */
.app { display: flex; flex-direction: column; height: 100vh; }  /* 先上下分 */
.app-header { height: 56px; }
.app-body { display: flex; flex: 1; min-height: 0; }  /* min-height:0 允许内部滚动而不撑破父容器 */
.app-aside { width: 220px; }
.app-main { flex: 1; overflow: auto; }  /* 内容区吃掉剩余宽度，自身滚动 */
```

**实战三：左固定右自适应**

```css
/* split.css */
.container { display: flex; }
.left  { width: 260px; flex-shrink: 0; }  /* 固定宽 + 禁止被压缩 */
.right { flex: 1; }                       /* 吃掉全部剩余宽度 */
```

### 3.7 Grid：二维布局一句话入门

```css
/* grid.css：三等分卡片墙 */
.card-list { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; } /* fr ≈ 弹性份额 */
```

适用场景：**二维同时约束行列**（仪表盘、卡片墙）用 Grid；**一维排布**（导航、侧边栏）用 Flex。两者可嵌套混用。

### 3.8 响应式三件套

```css
/* responsive.css：媒体查询，视口宽度满足条件时生效 */
.dashboard { display: grid; grid-template-columns: repeat(3, 1fr); }
@media (max-width: 992px) {   /* ≈ 平板及以下 */
  .dashboard { grid-template-columns: 1fr; }
  .app-aside { display: none; }
}
```

| 单位 | 含义 | 类比 |
| --- | --- | --- |
| `px` | 固定像素 | 硬编码常量 |
| `em` | 相对**父元素**字号 | 相对路径 |
| `rem` | 相对**根元素**字号（默认 16px） | 全局基准配置 |
| `vw` / `vh` | 视口宽/高的 1% | 百分比布局 |

移动端必须配 viewport meta（2.1 已写好），否则手机按 980px 桌面宽度缩放渲染：

```html
<meta name="viewport" content="width=device-width, initial-scale=1.0" />
```

---

## 四、CSS 工程化

### 4.1 scoped 样式与冲突问题

CSS 没有"命名空间"，全局 `.title` 会污染所有同名元素。Vue 的解法——`<style scoped>`：

```vue
<!-- UserCard.vue -->
<style scoped>
.title { color: #303133; }
</style>
```

原理：编译时给组件每个元素追加 `data-v-xxxx` 属性，样式改写为 `.title[data-v-xxxx]`——**编译期自动生成命名空间**，类似不同 ClassLoader 隔离同名类。

### 4.2 BEM 命名法

用命名约定模拟命名空间：`块__元素--修饰符`。

```css
/* bem.css */
.chat-panel { ... }                    /* Block：独立组件 */
.chat-panel__header { ... }            /* Element：块的组成部分 */
.chat-panel__message--active { ... }   /* Modifier：某种状态 */
```

Element Plus 的类名就是 BEM 风格（`el-button--primary`），看懂即可覆盖定制。

### 4.3 CSS 变量

```css
/* theme.css */
:root {                       /* :root ≈ 全局配置文件 */
  --el-color-primary: #409eff;
  --sidebar-width: 220px;
}
.app-aside { width: var(--sidebar-width); }
```

类比 `application.yml` 配置项 + `@Value` 注入：定义一处，全站引用，换肤只改变量。Element Plus 主题定制就是改这批 `--el-*` 变量。

### 4.4 Sass / Less 与 Tailwind

- **Sass / Less**：CSS 预处理器，补上变量、嵌套、mixin，构建时编译成纯 CSS。一句话定位：**CSS 界的 Lombok**——语法增强，产物不变。
- **Tailwind**：原子化 CSS——不自己起 class 名，直接堆工具类 `class="flex items-center gap-2"`，构建器按需产出极小的 CSS。
- **与 Element Plus 的关系**：EP 管"组件"（表格、弹窗、表单控件），Tailwind 管组件外的"布局微调"，互补不冲突。本项目用 Element Plus，Tailwind 了解概念即可。

---

## 五、Java 开发者常见困惑 Q&A

### Q1：为什么前端没有"编译报错"兜底？

Java 编译器把 90% 的低级错误拦在运行前；前端（纯 JS）的真相是：
- Vite 的"构建"只做打包、压缩、转译，**不做类型检查**——拼错变量名、调用不存在的方法，`npm run dev` 照样一片绿；
- 错误运行时才暴露，且常常只影响局部（某个按钮点了没反应）。

自愈手段：ESLint 静态检查（≈ CheckStyle）、TypeScript 类型系统（后续篇目）、下面的 F12 运行时自检。

### Q2：F12 开发者工具——前端的 "Arthas + Postman"

| 面板 | 解决什么问题 | 后端类比 |
| --- | --- | --- |
| **Elements** | 查看实时 DOM 树、盒模型与生效 CSS，可实时改 | 反编译看运行时对象 |
| **Console** | 看 `console.log` 与报错堆栈，可直接敲 JS 即时执行 | 应用日志 + REPL |
| **Network** | 抓所有 HTTP/WS 请求：URL、头、载荷、响应、耗时 | Postman + 链路追踪 |
| **Application** | localStorage / Cookie / 缓存 | 缓存与 Session 查看 |

**Network 是后端联调的主战场，务必练熟**：
1. 勾选 `Fetch/XHR` 过滤，只看接口请求；
2. `Headers` 看 URL / Method / 状态码 / 请求头（token 带没带就在这看）；`Payload` 看请求体（前后端字段名对不上就在这定位）；`Response` 看响应体（后端说"我返回了"、前端说"没收到"，这里一锤定音）；
3. 耗时瀑布图区分 TTFB（≈ 后端处理耗时）与 Content Download（≈ 网络传输）；右键请求 → `Copy as cURL` 可粘给后端复现；
4. 本项目的 **WebSocket / SSE 流式输出**也在这：WS 用 `Messages` 看帧，SSE 在 `EventStream` 看逐条推送——排查"AI 回答卡住"先看这里。

---

## 本章小结

- 浏览器 = 前端的 JVM：HTML（结构）+ CSS（表现）+ JS（行为）三层分工，Vue 组件只是三层的封装形式；
- HTML 重点：语义化标签 + 表单全家桶——表单的 `value` + `input` 事件是 Vue `v-model` 的原生基础；
- CSS 三板斧：选择器优先级（王 > 将 > 兵）、盒模型 `border-box`、Flex 主轴/交叉轴；position 记参照物：relative 自身 / absolute 最近非 static 祖先 / fixed 视口 / sticky 滚动容器；
- 工程化：scoped 编译期隔离、BEM 命名、CSS 变量 ≈ yml 配置项；Element Plus 管"组件"，Tailwind 管"原子类"；
- 前端没有编译期兜底，F12 的 Network 面板是前后端联调的第一现场。

## 动手练习

1. **手写登录页**：不借助框架，用语义化标签 + 表单全家桶写出登录页静态结构（用户名 / 密码 / 记住我 checkbox / 角色 radio / 登录 button），并让整张表单水平垂直居中。
2. **还原 ai-cs-frontend 骨架**：用 Flex 写出「56px 顶部栏 + 220px 侧边栏 + 自适应内容区」布局，内容区超出时出现滚动条而页面整体不滚（提示：`flex: 1` + `min-height: 0` + `overflow: auto`）。
3. **F12 侦探**：任选网站 → Elements 面板读出一个按钮的盒模型四层尺寸；Network 面板找出耗时最长的 XHR，`Copy as cURL` 并逐行读懂请求头。
