# ai-cs-frontend-react

AI 客服平台前端 —— React 版本（从 `ai-cs-frontend` 的 Vue 3 版本 1:1 迁移）。

## 技术栈

| 领域 | 选型 |
| --- | --- |
| 框架 | React 18 |
| 构建 | Vite 5 |
| 路由 | React Router 6（`BrowserRouter`） |
| UI | Ant Design 5（`@ant-design/icons`） |
| 图表 | ECharts 5 |
| 请求 | axios（统一走网关 `http://localhost:8080/api`） |
| 二维码 | qrcode |

## 启动

```bash
npm install
npm run dev      # 开发服务：http://localhost:5174
npm run build    # 产物输出到 dist/
npm run preview  # 预览构建产物
```

开发端口为 **5174**（Vue 版占用 5173），网关 CORS 允许任意来源，无需额外配置。

若网关不在本机 8080，可创建 `.env.local` 覆盖：

```
VITE_GATEWAY=http://localhost:8080
```

## 目录结构

```
src/
├── main.jsx                 # 入口：ConfigProvider(中文/主题) + Router + antd App
├── App.jsx                  # 路由表 + 登录守卫（RequireAuth / PublicOnly）
├── style.css                # 全局样式（由原 Vue scoped 样式迁移）
├── api/index.js             # 各微服务 axios 实例 + 接口封装（401 自动跳登录）
├── utils/
│   ├── auth.js              # Token / 用户信息（localStorage）
│   ├── dialog.jsx           # confirm / prompt 弹窗工具（替代 ElMessageBox）
│   └── order.js             # 订单状态映射
├── layouts/MainLayout.jsx   # 侧边栏 + 顶栏用户菜单 + <Outlet/>
└── views/                   # 24 个业务页面（与原 Vue 页面一一对应）
```

## 路由与页面

| 路径 | 页面 | 说明 |
| --- | --- | --- |
| `/login` | LoginView | 登录 / 注册（全屏，无侧边栏） |
| `/` | Dashboard | 首页总览：微服务状态（15s 轮询）、功能入口、技术栈 |
| `/chat`、`/feedback` | ChatView | AI 对话：会话管理、SSE 流式、图片多模态、引用溯源、反馈 |
| `/rag-kb` | VectorKbView | 向量知识库：文本/文件入库、语义检索测试 |
| `/knowledge` | KnowledgeView | 知识库文档 CRUD |
| `/knowledge-ops` | KnowledgeOpsView | 运营看板：问题聚类 + 缺口识别 + FAQ 收录 |
| `/search` | SearchView | 全文搜索与索引管理 |
| `/mq` | MqView | RocketMQ 集群/Topic/消费组 + 工单闭环演示 |
| `/shop` | ShopView | 商品商城（关键词 / 语义搜索） |
| `/product` | ProductView | 商品图片上传、以文搜图、编辑、分类管理 |
| `/cart` | CartView | 购物车 |
| `/checkout` | CheckoutView | 确认订单（优惠券 / 支付方式） |
| `/order`、`/order/:orderNo` | OrderView / OrderDetailView | 订单列表与详情（取消 / 支付 / 退款） |
| `/mock-pay` | MockCashierView | 模拟收银台（二维码扫码 + 轮询 + 倒计时） |
| `/user` | UserView | 用户注册 / 登录 / 查询（接口调试） |
| `/message` | MessageView | 会话与消息管理 |
| `/notify` | NotifyView | 通知 / 广播 / WebSocket 实时推送 |
| `/trace` | TraceView | LLM 调用链追踪（甘特图 + span 明细） |
| `/prompts` | PromptView | Prompt 版本管理与热切换 |
| `/agent` | AgentView | 售后 Agent 编排（步骤流 + 待确认写操作） |
| `/graph` | GraphView | 知识图谱三元组与多跳检索 |
| `/rag-eval` | RagEvalView | RAG 评估报告 |
| `/chat-dashboard` | ChatDashboardView | 问数图表（后端返回 ECharts 配置） |

## 与 Vue 版的差异

- UI 组件库由 Element Plus 换成 Ant Design，主题色沿用原配色（`#409eff` / `#67c23a` / `#e6a23c` / `#f56c6c`）。
- `ElMessage` / `ElMessageBox` 换为 antd `message` 与 `utils/dialog.jsx` 中的 `confirmDialog` / `promptDialog`。
- SSE 流式对话实现保持不变（`fetch` + `ReadableStream` 手动解析 SSE 帧）。
- 回答反馈工具条仅在 AI 回答上展示（原 Vue 版对用户消息也会展示）。
