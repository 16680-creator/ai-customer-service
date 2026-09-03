# BI 看板与 ECharts 可视化

> 对应项目：`ai-cs-frontend/src/views/ChatDashboardView.vue`（本项目 ECharts 唯一消费点，102 行完整走读）、`ai-cs-frontend/package.json:14`（`"echarts": "^5.6.0"`）、后端图表生成链 `ai-cs-chat/src/main/java/com/aics/chat/nl2sql/chart/ChartController.java`、`EChartsOptionBuilder.java`、`ChartAnswer.java`。
> 本篇是缺口补全：`06-前端开发/` 目录此前零覆盖 ECharts（见 [00-学习路线总览/05-技术缺口分析与补全计划](../00-学习路线总览/05-技术缺口分析与补全计划.md) A 类缺口"ECharts 数据可视化"），本篇与 [06-前端开发/04-Pinia状态管理实战](../06-前端开发/04-Pinia状态管理实战.md) 同属前端篇的补全线。
> 相关：[01-OLAP与列式存储原理](./01-OLAP与列式存储原理.md)、[02-ClickHouse实战](./02-ClickHouse实战.md)（看板的数据从哪来）、[07-LLM用量计量与成本分析](./07-LLM用量计量与成本分析.md)（成本看板的指标）、[04-中间件/05-SSE与WebSocket实时通信](../04-中间件/05-SSE与WebSocket实时通信.md)（流式刷新的另一条路线）。

---

## 一、先结论：ECharts 的三层心智模型

很多教程一上来贴 option 全量配置，越看越晕。正确的理解顺序是三层：

```
第 1 层【坐标系 coordinate】  "图放在什么坐标系里画"
    grid(直角坐标系) / polar / geo / calendar / 单一坐标系(饼图)
第 2 层【系列 series】       "在坐标系里画什么图形"
    type: bar / line / pie / scatter / graph ...
    每个系列绑定：数据 + 坐标系(可选) + 样式
第 3 层【组件组件】          "图之外的功能件"
    xAxis/yAxis(坐标轴) / legend(图例) / tooltip(悬浮) /
    dataZoom(缩放) / dataset(数据集) / title / toolbox
```

| 概念 | 是什么 | 一句话理解 |
|---|---|---|
| `series` | **必填核心**，一个系列=一组同类图形 | "画几个东西" |
| `coordinate`（grid/polar/geo） | 承载系列的坐标系 | "画在哪" |
| `dataset` | 独立于系列的**数据源**声明（行列式） | "数据长什么样" |
| `xAxis/yAxis` | 直角坐标系轴配置 | "轴怎么标" |
| `legend/tooltip` | 图例/悬浮提示组件 | "怎么读图" |

**关键认知**：一个 ECharts 实例（option）可以画在**多个 grid**（多子图）、可以有**多个 series**（柱+线混搭）；`dataset` 与 `series.data` 二选一，前者让"换数据"变成改一处。

---

## 二、图表选型决策树

```
你要展示什么？
│
├─ 看【构成/占比】（部分 vs 整体）
│    ├─ 类目 ≤ 6 且差异明显 ──────▶ 饼图 pie
│    └─ 类目多/想看累计排序 ──────▶ 条形图（按值降序）或 堆叠柱
│
├─ 看【对比】（类目间比大小）
│    ├─ 类目少、值差异大 ─────────▶ 柱状图 bar
│    └─ 类目名长 ────────────────▶ 条形图（横向 bar，yAxis 类目）
│
├─ 看【趋势】（随时间变化）
│    ├─ 单/少序列 ───────────────▶ 折线图 line
│    └─ 多序列且要看总量 ─────────▶ 堆叠面积图
│
├─ 看【相关性/分布】
│    ├─ 两数值变量 ──────────────▶ 散点图 scatter
│    └─ 单变量分布 ──────────────▶ 柱状直方（bar + 连续分箱）
│
└─ 看【层级/关系】
     ├─ 层级占比 ────────────────▶ 矩形树图 treemap / 旭日图 sunburst
     └─ 网络关系 ────────────────▶ 关系图 graph（力导向）
```

**两个高频选型纠错**：
1. **趋势别用柱状图**：月度 token 消耗用 line 一眼看出拐点，bar 会让人去"数柱子"。
2. **饼图别超过 6 块**：人眼对角度差不敏感，把小份额合并成"其他"，或直接换成降序条形图。

> 本项目的实现（`EChartsOptionBuilder`）恰好覆盖决策树最常用的三分支：**pie（分类占比）、bar（数值对比）、line（时间趋势）**——`ai-cs-chat/.../nl2sql/chart/EChartsOptionBuilder.java:16-17` 的注释原文："三种图表：pie（分类占比）、bar（数值对比）、line（时间趋势），关键是把数据行拆成 name/value 或 categories/values 两个数组"。

---

## 三、项目现场：后端生成 option、前端只渲染（已落地）

### 3.1 为什么"后端拼 option"而不是"LLM 直接生成 option"

`EChartsOptionBuilder.java:8-15` 的类注释给出了答案（原文要点）：**ECharts 配置结构是确定的（series/xAxis/yAxis 等），LLM 可能输出非法 JSON 或错误字段名，纯 Java 构建零风险、可单测**。这是一个通用的架构结论：

> **LLM 负责"决策"（选哪种图），代码负责"结构"（拼合法 JSON）。** 让 LLM 输出自由结构是把概率问题引入确定性问题，必错。

### 3.2 数据流全景

```
用户问题（"各分类销量分布"）
   │  POST /chat/chart {question, rows}
   ▼
ChartController.java:38-40
   │  ChartAnswerGenerator.generate(...)
   ▼
ChartAnswer {question, conclusion, chartType, echartsOption}   ← 结论+类型+完整 option
   │  HTTP 返回
   ▼
ChatDashboardView.vue:renderChart()
   │  dispose 旧实例 → echarts.init(dom) → setOption(option)
   ▼
浏览器渲染（前端零业务逻辑，只消费标准 ECharts 配置）
```

`ChartAnswer.java:11-23` 定义了前后端契约：`question`（原始问题）、`conclusion`（自然语言结论）、`chartType`（PIE/BAR/LINE/NONE）、`echartsOption`（完整配置对象）。**前端不感知后端如何生成，换图表库只需改前端**（`ChartController.java:25-26` 注释要点）。

### 3.3 前端渲染代码走读（ChatDashboardView.vue）

```javascript
// ai-cs-frontend/src/views/ChatDashboardView.vue:39
import * as echarts from 'echarts'          // 全量引入（本项目体量可接受，按需引入见 §5.4）

// ChatDashboardView.vue:88-97 —— 渲染函数（实例生命周期管理是核心考点）
function renderChart(data) {
  // ① 复用前先销毁：避免多次 setOption 叠加状态（旧 series 残留/合并脏状态）
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
  // ② chartType=NONE（单行/空数据）不渲染，避免误导（模板 30-31 行同步显示 el-empty）
  if (!chartRef.value || data.chartType === 'NONE') return
  // ③ init 必须在 DOM 可见后（本页在 await nextTick() 之后调用，见 75-76 行）
  chartInstance = echarts.init(chartRef.value)
  // ④ 后端给什么画什么，前端零加工
  chartInstance.setOption(data.echartsOption || {})
}

// ChatDashboardView.vue:100-102 —— 组件卸载释放，防内存泄漏
onBeforeUnmount(() => {
  if (chartInstance) chartInstance.dispose()
})
```

**四个值得背下来的细节**：

| 细节 | 为什么 |
|---|---|
| `dispose()` 后重建，而不是重复 `setOption` | setOption 默认**合并模式**：换个图表类型渲染，旧 series 的残留配置会污染新图 |
| `await nextTick()` 后再 `init` | `v-if` 刚把容器 DOM 挂出来，不 etcTick 就 init 拿到 0 尺寸容器，图显示为空白 |
| `chartType === 'NONE'` 兜底 | 单行数据/无分布维度强行画图 = 数据可视化事故（无意义的饼图比没有图更糟） |
| `onBeforeUnmount` 里 dispose | ECharts 实例持有 DOM 引用与 resize 监听，不释放会内存泄漏 |

---

## 四、dataset 与增量渲染

### 4.1 series.data vs dataset

```
方式一：series.data（ChatDashboardView 的后端产物就是这个形态，直观）
option = { series: [{ type: 'bar', data: [1200, 800, 650, 420] }] }

方式二：dataset（行列式数据源，维度复用更强）
option = {
  dataset: {
    source: [
      ['category', 'sales'],      // 第一行=维度名
      ['手机', 1200], ['平板', 800], ['笔记本', 650], ['耳机', 420]
    ]
  },
  xAxis: { type: 'category' },
  yAxis: {},
  series: [{ type: 'bar' }]       // 不写 data，自动从 dataset 编码
}
```

| | series.data | dataset |
|---|---|---|
| 一个数据源喂多个系列 | 要复制多份 | **一份 source，多个 series 各自 encode** |
| 换数据 | 替换 series.data | `setOption({dataset.source})` 一处 |
| 适合 | 单图/后端已拼好 | 交互式看板、多图联动 |

### 4.2 "增量渲染"的三种真实含义（面试常混）

| 说法 | 真实含义 | ECharts 对应手段 |
|---|---|---|
| **数据追加**（时间序列滚屏） | 新数据 append，旧行移出 | `setOption({series:[{data: newData}]})` 整体替换比增量 push 更可控 |
| **合并更新**（改一处不动其余） | setOption 默认 notMerge=false | `chart.setOption(partial, {lazyUpdate: true})`；**要完全重置时必须 `setOption(option, true)` 或 dispose 重建**（本项目选后者，见 §3.3） |
| **渐进渲染**（大数量首屏） | 分帧画，不卡 UI | `series.progressive: 2000`、`progressiveThreshold: 3000` |

**为什么本项目选"dispose 重建"而不是"合并更新"**：问数场景每次是**全新的图**（类型都可能变），合并语义反而是负担。交互式看板（固定图、只变数据）才该用合并更新 + `appendData`/dataset 替换。**先分清场景，再选更新策略**。

---

## 五、大数据量降采样

### 5.1 什么时候需要降采样

浏览器一屏约 1000~2000 像素宽，**数据点超过像素数就是纯浪费**——5 万点的 line 和 2000 点的 line 在屏幕上几乎一样，但 DOM/canvas 开销差 25 倍。经验阈值：单 series > 5000 点就该降采样。

### 5.2 三种降采样手段

**① ECharts 内置 `sampling`**（line 图专用，一行配置）：

```javascript
series: [{
  type: 'line',
  sampling: 'lttb',   // Largest-Triangle-Three-Buckets：保形状最优；可选 'average'/'min'/'max'/'sum'
  large: true,        // scatter 专用：关闭精细化绘制换吞吐
}]
```

LTTB 原理一句话：每桶选"与前后锚点构成三角形面积最大"的点，**保留视觉拐点**而不是机械取平均——峰值/谷值不会被抹平。

**② 后端预聚合（治本）**：

```
明细层（亿行）──▶ DWS 预聚合（天/小时粒度，03 篇）──▶ 看板只画聚合后数据
前端画 5 万点 = 设计错误；正确姿势是后端先按天聚合再返回 ≤ 720 个点
本项目对应：02 篇物化视图 model_usage_day 就是"看板数据源"的角色
```

**③ dataZoom 只在交互时加载细节**：

```javascript
dataZoom: [
  { type: 'inside' },                        // 滚轮/手势缩放
  { type: 'slider', start: 0, end: 100 }     // 底部滑条
]
// 常与"概览聚合+细节请求"组合：缩放窗口变化时按新窗口向后端请求对应粒度数据
```

### 5.3 渲染性能检查单

| 检查项 | 手段 |
|---|---|
| 单系列点数 > 5000 | `sampling: 'lttb'` 或后端预聚合 |
| 散点 > 1 万 | `large: true` + `largeThreshold` |
| 图表容器尺寸变化 | `window.addEventListener('resize', () => chart.resize())`（本项目单页固定尺寸 380px，未涉及） |
| 数据高频刷新（>1s 一次） | 节流 + `lazyUpdate`，或改走 SSE 推送（[04-中间件/05](../04-中间件/05-SSE与WebSocket实时通信.md)） |
| 内存持续增长 | 确认 dispose 时机（§3.3 第 4 条） |

---

## 六、面试高频问答

**Q1：ECharts 的 option 里 series、grid、dataset 分别是什么？**
A：series 是核心——一个系列代表一组同类图形（bar/line/pie），携带数据与样式；grid 是直角坐标系（承载 bar/line/scatter 的"画布"），一个 option 可有多个 grid 实现多子图；dataset 是与系列解耦的行列式数据源，一份 source 可被多个 series 通过 encode 引用，适合数据替换与多图联动。三者关系：dataset 喂数据，series 决定画什么，grid 决定画在哪。

**Q2：柱状图、折线图、饼图各适合什么数据？最常见的选型错误？**
A：柱状图比类目大小、折线图看时间趋势、饼图看少类目占比（≤6）。高频错误：用饼图表达趋势（时间点超过 6 个的角度差人眼无法分辨）、用柱状图表达趋势（拐点不直观）、饼图塞十几个类目（小份额互相不可辨）。类目多就降序条形图，趋势就折线。

**Q3：为什么你们项目让后端生成 ECharts option，而不是前端或 LLM 生成？**
A：问数（NL2SQL）场景下图表类型由数据形态决定，后端纯 Java 构建 option 有三个收益：结构合法性有保证（LLM 直出 JSON 可能非法或字段名错）、可单测可回归、前端与生成逻辑解耦（换图表库只改前端）。LLM 只负责"决策"（pie/bar/line + 结论文案），确定性结构交给代码。

**Q4：多次 setOption 会有什么坑？怎么正确切换一个全新的图？**
A：setOption 默认是合并模式，新旧配置按属性合并——从饼图切到柱状图时旧 series/旧配色可能残留，出现"脏状态"。切换全新图表的正确做法：先 `dispose()` 旧实例再 `init` 新实例（本项目 ChatDashboardView 的做法），或 `setOption(option, true)` 传入 notMerge 整体替换。固定图表只更新数据时才用默认合并。

**Q5：echarts.init 报"容器宽度/高度为 0"或图空白，什么原因？**
A：init 时容器 DOM 还不可见或尺寸为 0。典型场景：容器在 v-if 里刚挂载就 init。解法：等 DOM 渲染完成（`await nextTick()` 后 init，本项目 ChatDashboardView.vue:75-76 的顺序）、确认容器有显式高度；窗口 resize 时还要手动调 `chart.resize()`。

**Q6：数据量很大（几万点）时 ECharts 怎么优化？**
A：三层：① 内置 `sampling: 'lttb'` 降采样保形状；② 散点用 `large: true`；③ 治本靠后端预聚合——看板只画 DWS 层聚合结果（天/小时粒度），点数控制在千级；交互细节用 dataZoom 缩放窗口触发按窗口粒度的后端请求。原则：屏幕像素决定有意义的数据点上限，超出部分都是开销。

**Q7：前端图表组件怎么防内存泄漏？**
A：ECharts 实例持有 DOM 引用与事件监听，必须在组件卸载时 `dispose()`（Vue 的 onBeforeUnmount / React 的 useEffect cleanup）。另一个泄漏点是定时器/订阅刷新场景反复 init 不 dispose。本项目 ChatDashboardView 在卸载钩子里统一释放，且渲染前对旧实例先行销毁。

**Q8：看板要实时刷新，用轮询还是推送？**
A：刷新频率秒级以内用推送（本项目已有 SSE 基建，见 04-中间件/05），服务端有新数据再推图表增量；分钟级用轮询足够且实现简单。共同注意点：刷新回调里更新数据应走 setOption 合并/替换而非重复 init，并配合节流避免高频重绘阻塞主线程。

---

## 七、动手练习

1. 走读 `ChatDashboardView.vue`（102 行），把 §3.3 四个细节（dispose 重建、nextTick、NONE 兜底、卸载释放）逐条对应到行号；然后故意删掉 `dispose()` 逻辑，在页面连续生成 pie→bar→pie 三次，观察旧图残留现象。
2. 读 `EChartsOptionBuilder.java`，为它补第四种图表 `line`（时间趋势）的实现：从 rows 里识别时间列与数值列，输出 xAxis(category)+series(line) 的 option，并写单测（沿用"LLM 决策、代码拼结构"的原则）。
3. 把 §4.1 的 dataset 版 option 手写进一个本地 HTML，体会"一份 source 两个 series"（bar + line 同图）的 encode 写法。
4. 造 5 万点随机时间序列，分别用 `sampling:'lttb'` 与不降采样渲染，用 Performance 面板对比帧率与脚本耗时；再把数据换成 720 点的"按小时预聚合"版本，验证视觉上无明显差异。
5. ⚠️ 目标态练习：结合 02 篇 §6 与 07 篇 §5，为"LLM 成本看板"画一张组件图——`model_usage_day`（CH 预聚合）→ 新的 stats 只读接口 → 复用 `ChatDashboardView` 的渲染范式 → ECharts（按天 cost 用 line、按 scenario 占比用 pie），并标出哪些环节已存在、哪些待建。

---

> 上一篇：[05-批处理与调度编排](./05-批处理与调度编排.md) ｜ 下一篇：[07-LLM用量计量与成本分析](./07-LLM用量计量与成本分析.md)
