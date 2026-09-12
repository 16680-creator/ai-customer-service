# 12-Nuxt 与 SSR 入门：同构渲染与 BFF

> 前置知识：[06-路由守卫与Axios请求封装](./06-路由守卫与Axios请求封装.md)（SPA 的请求层）、[10-浏览器原理与HTTP跨域](./10-浏览器原理与HTTP跨域.md)（渲染流程与跨域——本篇一半动机来自它）、[11-React快速上手](./11-React快速上手.md)（末节 Next.js 对照）。
> 场景定位：`ai-cs-frontend` 这类登录后才能用的管理端其实用不到 SSR；但官网、营销页、内容站需要，且 Nuxt 的 server/api（BFF 层）对 Java 后端是极实用的架构工具。

---

## 一、为什么需要 SSR：SPA 的两大短板

### 1.1 短板一：首屏白屏（FCP 慢）

SPA 的加载时序：拿到几乎空的 HTML → 下载 JS bundle → 执行 JS → **才**发起数据请求 → 渲染。用户盯着 `<div id="app"></div>` 白屏一到三秒，弱网和低端机上更糟。

```text
CSR 链路：HTML(空壳) → 下载 JS(几百 KB) → 执行 → 请求接口 → 才渲染内容
                      └────────────── 白屏窗口 ──────────────┘
SSR 链路：服务器先调好接口、渲染好完整 HTML → 浏览器直接显示 → JS 到货后水合
                      └─ 白屏只剩 JS 下载与水合时间 ─┘
```

服务器帮忙把"取数 + 渲染"提前做完了，用户拿到响应的第一个字节就看得见内容——这正是 FCP（First Contentful Paint）大幅提前的原因。

### 1.2 短板二：SEO 为零

爬虫拿到的源码就是空壳 div，正文全靠 JS 运行时生成，而多数爬虫不执行 JS——搜索排名无从谈起。

### 1.3 四种渲染模式概念对照表

| 模式 | 渲染时机 | 首屏 | SEO | 服务器成本 | 典型场景 |
|---|---|---|---|---|---|
| CSR 客户端渲染 | 浏览器下载 JS 后渲染 | 慢 | 差 | 低（静态托管） | 管理端、登录后应用（ai-cs-frontend） |
| SSR 服务端渲染 | 每次请求时服务器渲染 | 快 | 好 | 高（每请求都算） | 内容频繁变化、需 SEO 的动态页 |
| SSG 静态生成 | 构建时渲染一次 | 最快（CDN 直出） | 好 | 极低 | 官网、文档、博客 |
| ISR 增量静态再生 | 构建时 + 过期后台重建 | 快 | 好 | 低 | 电商列表、资讯站（Nuxt 用 routeRules 的 swr/isr 配置） |

Java 类比一句话：CSR ≈ 纯前端 SPA；SSR ≈ Thymeleaf/JSP 每请求渲染；SSG ≈ 构建期生成静态 HTML（类似 Maven site）；ISR ≈ 缓存 + 过期重建（`@Cacheable` + TTL + 刷新）。

### 1.4 SSR ≈ Thymeleaf 的高级回归，核心是"同构"与"水合"

写过 Spring + Thymeleaf 的你其实早做过"服务端渲染"：模板在服务器拼成 HTML 发给浏览器。SSR 是它的"高级回归"，但有本质升级——**同构（isomorphic）**：

- **服务端**：执行同一套 Vue 组件代码，把首屏渲染成完整 HTML 直出（用户看到内容时 JS 还没下载完）；
- **客户端**：JS 下载执行后框架**不重新渲染**，而是"接管"现有 DOM——把事件监听、响应式绑定挂回去，这个过程叫 **hydration（水合）**。

类比：服务端先快递一台已装好系统、开了机的机器（HTML 首屏），客户端 JS 到货后不是重装系统，而是把外设（事件、交互）插上——"从静态到可交互"的接管即水合。

水合的完整接管过程分四步：

1. 服务端渲染 HTML 时，把页面状态序列化成 payload 一起注入响应（`<script>` 里的 `window.__NUXT__...`）；
2. 浏览器收到响应立刻显示静态 HTML——此刻**看得见但点不动**；
3. JS bundle 下载执行，Vue 在客户端重建组件树，但**不重建 DOM**，只对已有 DOM 挂事件监听、接响应式数据；
4. 水合完成，之后行为与 CSR 完全一致（路由不整页刷新、状态响应式）。

注意由此引出的 SSR 兼容性纪律：服务端没有 `window/document`、没有用户的鼠标位置，**服务端与客户端渲染结果必须一致**，否则水合 mismatch（渲染不一致警告）。随机数、`new Date()`、直接访问浏览器 API 都要挪到 `onMounted` 之后——这是 SSR 项目最常见的一类报错。

### 1.5 本篇 Java 类比汇总表

| 前端概念 | Java 类比 |
|---|---|
| SSR 每请求渲染 | Thymeleaf / JSP 模板渲染 |
| 同构（一套组件代码两端跑） | "一次编写"覆盖 Web 与批处理的共享领域层 |
| hydration 水合 | 交付静态产物后再"热插拔"接上交互 |
| SSG 静态生成 | Maven site：构建期产出静态 HTML |
| ISR 增量再生 | `@Cacheable` + TTL：过期后后台重建 |
| server/api（BFF） | 迷你 Spring Controller / 聚合网关层 |
| runtimeConfig | application.yml + 环境变量分环境覆盖 |
| `.output` 产物 | Spring Boot fat jar（有运行时就能跑） |

## 二、Nuxt 3 快速上手

### 2.1 初始化

```bash
npx nuxi@latest init my-nuxt-app   # create-nuxt 交互式初始化（选包管理器与模块）
npm install && npm run dev         # http://localhost:3000
```

### 2.2 目录结构即约定（约定优于配置）

```text
my-nuxt-app/
├── pages/              # 文件式路由：目录+文件名即 URL
│   ├── index.vue       #   → /
│   └── user/
│       └── list.vue    #   → /user/list
├── components/         # 组件自动导入（文件名即标签名）
├── composables/        # 组合式函数自动导入
├── layouts/
│   └── default.vue     # 默认布局，<slot> 处渲染页面
├── server/
│   └── api/            # BFF 接口层（本篇主角）
├── nuxt.config.ts      # 全局配置
└── package.json
```

| 目录/文件 | 作用 | Vue SPA 里的对应物 |
|---|---|---|
| `pages/` | 文件式路由：`pages/user/list.vue` → `/user/list` | 手写 `router/index.js` 的 routes 表 |
| `components/` | 自动导入，免 import | 手动 import + 注册 |
| `composables/` | 自动导入组合式函数 | 手动 import |
| `layouts/default.vue` | 布局，`<slot>` 处放页面内容 | App.vue 里手写布局 |
| `server/api/` | 服务端接口（BFF） | 无对应物——通常在 Java 侧 |
| `nuxt.config.ts` | 全局配置 | vite.config + main.ts 合体 |

文件式路由就是"约定优于配置"本身：目录名即 URL，整张路由表被省掉——与 Spring MVC 按注解扫描 handler 省掉 web.xml 是同一种设计哲学。

### 2.3 useAsyncData / useFetch：SSR 下的数据获取

为什么不能只用 onMounted？**服务端渲染时不会执行 mounted 钩子**（服务端没有"挂载"阶段），数据请求写在 onMounted 里的后果：服务端渲染出的 HTML 没数据 → 首屏与爬虫依然白等 → SSR 白做。Nuxt 的解法：

```vue
<!-- pages/products.vue -->
<script setup>
// useFetch 在服务端执行 → 数据已渲染进 HTML；客户端水合时直接复用，不重复请求
const { data: products, pending, error, refresh } = await useFetch('/api/products')
</script>
<template>
  <p v-if="pending">加载中…</p>
  <ul><li v-for="p in products" :key="p.id">{{ p.name }}</li></ul>
</template>
```

- `useAsyncData(handler)`：通用版，包任意异步函数；`useFetch` 是它的 fetch 简写。
- 请求在服务端执行、结果序列化进 HTML（payload 注入），客户端水合时直接读 payload——**同一请求不跑两遍**。

### 2.4 server/api 目录 = BFF 层

`server/api/` 下每个文件就是一个服务端接口：文件名即路由，后缀 `.get.ts` / `.post.ts` 定 HTTP 方法。定位类比：**给前端配了个迷你 Spring Controller**：

- **聚合**：一个页面要 Java 后端 3 个接口？BFF 并行调完拼成一个返回；
- **裁剪**：只吐页面需要的字段，省流量且不泄露敏感列；
- **代理**：浏览器只见 Nuxt 不见 Java 后端——内部接口天然被隐藏，同源也顺带消灭跨域。

这层业内叫 **BFF（Backend For Frontend）**：为前端体验定制的后端，相当于微服务架构里专门做聚合的网关层。

## 三、server/api 实战：聚合调用 Java 后端

### 3.1 chat.post.ts 完整示例

```ts
// server/api/chat.post.ts —— 聚合 Java 后端两个接口，浏览器只请求 Nuxt
export default defineEventHandler(async event => {
  const body = await readBody(event)                       // ≈ @RequestBody

  const config = useRuntimeConfig(event)                   // 读运行时配置
  const headers = { Authorization: `Bearer ${body.token}` }// 透传登录态

  // 并行调用 Java 后端两个接口（$fetch 是 Nuxt 内置请求工具）
  const [chatRes, quotaRes] = await Promise.all([
    $fetch('/api/chat/sessions', { baseURL: config.javaApi, headers }),
    $fetch('/api/quota/remaining', { baseURL: config.javaApi, headers }),
  ])

  // 裁剪：只返回页面真正用到的字段
  return {
    sessions: chatRes.list.map(s => ({ id: s.id, title: s.title, updatedAt: s.updatedAt })),
    remaining: quotaRes.remaining,
  }
})
```

前端从此只认 `POST /api/chat`，不知道背后有几个 Java 接口、后端地址是什么。

### 3.2 runtimeConfig：运行时配置 ≈ application.yml 分环境

```ts
// nuxt.config.ts
export default defineNuxtConfig({
  runtimeConfig: {
    javaApi: 'http://localhost:8080',     // 服务端私有：内网地址、密钥都放这层
    public: { siteName: 'AI 客服' },       // 客户端可见
  },
})
```

部署时用环境变量覆盖：`NUXT_JAVA_API=https://api.prod.com`（`NUXT_` + 大写下划线映射 key）——和 Spring 的 `application-prod.yml` / 环境变量注入是同一套思路。规则记住一条：**`public` 之外的服务端配置永远不进浏览器**，等价于"敏感配置不进前端包"。

## 四、渲染模式决策表：什么项目该用 SSR

### 4.1 后台管理系统用不用 SSR？——基本不用

`ai-cs-frontend` 这类系统：内容全在登录后、无 SEO 需求、内网/员工使用、强交互弱首屏 → **SPA + Vite 足够**。上 SSR 纯属增加成本：要常驻 Node 进程、写代码要顾忌 SSR 兼容性（window/document 在服务端不存在）。

### 4.2 决策三问 + 场景对照

拍板前问三个问题：

1. **有没有 SEO 需求？**（面向搜索引擎的内容 → SSR/SSG；登录后工具 → CSR）
2. **首屏速度是否直接损伤业务？**（营销页转化率敏感 → SSR/SSG）
3. **内容是否登录后才可见？**（是 → 无需 SSR，登录墙已挡住爬虫）

| 项目类型 | 推荐 | 理由 |
|---|---|---|
| ai-cs 管理端 | CSR（维持现状） | 登录后内容、无 SEO |
| 产品官网/营销落地页 | SSG | 内容构建期已知，CDN 极快 |
| 资讯/内容站 | SSR 或 ISR | 内容动态更新又要 SEO |
| 电商商品列表 | ISR | 静态速度 + 定时刷新 |

Nuxt 里这些模式不用换框架，在 `routeRules` 里按路由声明即可：

```ts
// nuxt.config.ts —— 同一工程里按路由混用渲染模式
export default defineNuxtConfig({
  routeRules: {
    '/about': { prerender: true },    // SSG：构建时预渲染
    '/news/**': { isr: 60 },          // ISR：静态缓存 + 60 秒过期重建
    '/admin/**': { ssr: false },      // 该目录退回 CSR（管理端页面）
  },
})
```

## 五、部署

### 5.1 node server 部署（SSR 模式）

```bash
npm run build                    # 产出 .output/，server/index.mjs 为自包含服务端入口
node .output/server/index.mjs    # 默认 3000 端口，≈ java -jar fat.jar 一把梭
```

`.output` 产物类比 Spring Boot 的 fat jar：依赖打进来，机器有 Node 就能跑。Dockerfile：

```dockerfile
# Dockerfile —— 多阶段构建，与 Java 后端镜像思路一致
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM node:20-alpine
WORKDIR /app
COPY --from=build /app/.output .output
EXPOSE 3000
CMD ["node", ".output/server/index.mjs"]
```

### 5.2 静态化部署（SSG 模式）

```bash
npm run generate   # 全站预渲染为 .output/public/ 纯静态文件
```

产物直接扔 Nginx / CDN，不需要 Node 进程——SSG 模式下 Nuxt 相当于"预渲染器"，成本最低，适合官网：

```nginx
# nginx.conf —— 托管 nuxt generate 产物
server {
  listen 80;
  root /var/www/my-nuxt-app/.output/public;
  location / { try_files $uri $uri/ /index.html; }   # 未命中回退，防止 404
}
```

### 5.3 与 Next.js 一句话对比

Next.js 之于 React ≈ Nuxt 之于 Vue：同样的文件路由 + SSR/SSG/ISR + BFF（Route Handlers）理念，选型只看团队技术栈阵营；概念完全互通，学了一个另一个半天上手。

## 本章小结

- SPA 两大短板：首屏白屏、SEO 为空；SSR 用"服务端直出 HTML + 客户端水合"补齐，本质是 Thymeleaf 的高级回归，升级点是同构。
- 四种模式按"决策三问"选型：CSR 管理端 / SSR 动态+SEO / SSG 静态官网 / ISR 静态+定时刷新。
- Nuxt 约定：pages 文件路由、components/composables 自动导入、layouts 布局；数据获取必须用 useAsyncData/useFetch——onMounted 在服务端不执行。
- server/api = BFF = 给前端的迷你 Spring Controller：聚合、裁剪、代理，隐藏 Java 后端并消灭跨域；runtimeConfig ≈ application.yml 分环境。
- 部署：`build` 出 `.output` 用 node 跑（≈ fat jar + Dockerfile），`generate` 纯静态扔 Nginx。

## 动手练习

1. 把 ai-cs 项目的官网首页用 Nuxt 复刻：SSG 模式 + `npm run generate` 产出静态文件部署到 Nginx，用 curl 抓首页验证 HTML 里直接有标题文字（而不是空 `<div id="app">`）。
2. 写一个 `server/api/user-info.get.ts`：接收前端 token，代理调用 Java 后端 `/user/info`，裁剪掉手机号等敏感字段后返回——体会 BFF 的数据裁剪与接口隐藏价值。
