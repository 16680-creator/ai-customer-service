# 浏览器原理与 HTTP 跨域

> **前置知识**：[06-路由守卫与Axios请求封装](./06-路由守卫与Axios请求封装.md)（拦截器实践）、[09-前端工程化：Vite构建与规范](./09-前端工程化：Vite构建与规范.md)（proxy 与缓存配置）。
> **定位**：后端人最熟悉的 HTTP，配上"浏览器视角"就通了；**跨域**是前后端联调第一坑，本篇一次讲透三种解法。

---

## 一、浏览器是半个操作系统

### 1.1 进程与线程模型

- 浏览器是多进程的：每个标签页一个**渲染进程**（崩溃不互相传染）、GPU 进程、网络进程等。
- 渲染进程里：**JS 引擎单线程**执行 JS（所以有事件循环，见 02 篇）、渲染线程、合成线程分工协作。
- 前端性能优化的所有"玄学"，根源都在"JS 单线程 + DOM 渲染要排队"。

### 1.2 从输入 URL 到页面渲染（一分钟版）

```text
DNS 解析 → TCP 三次握手 → TLS 握手 → 发送 HTTP 请求
   ↓
服务器返回 HTML → 解析建 DOM 树
   ↓            ↓
 <link> CSS    <script> JS（默认阻塞解析！所以推荐 defer/模块化）
   ↓            ↓
CSSOM 树 ──── 与 DOM 合成 → 渲染树 → 布局 Layout（算几何） → 绘制 Paint → 合成 Composite
```

### 1.3 重绘与回流

| 操作 | 成本 | 例子 |
|---|---|---|
| **回流 reflow** | 高：几何重新计算，可能连带整棵树 | 改宽高、增删节点、改字体 |
| **重绘 repaint** | 中：外观变化不涉及布局 | 改颜色、visibility |
| **合成 composite** | 低：GPU 直接处理 | `transform`、`opacity` |

```javascript
// ❌ 读写交替：每次读 offsetWidth 都强制浏览器立刻回流一次
for (const li of items) {
  li.style.height = li.offsetWidth + 10 + 'px'
}
// ✅ 先批量读，再批量写
const widths = items.map(li => li.offsetWidth)
items.forEach((li, i) => (li.style.height = widths[i] + 10 + 'px'))
```

> 动画用 `transform: translateX()` 而不是 `left/top`，就是上面这张表的直接应用。

---

## 二、前端视角的 HTTP 要点

后端已懂 HTTP，只补"前端怎么消费"：

**状态码处理策略**（axios 拦截器里统一做，见 06 篇）：

| 状态码 | 前端标准动作 |
|---|---|
| 200 | 正常取 `data` |
| 401 | 清登录态 → 跳登录页（token 过期/未登录） |
| 403 | 提示无权限（按钮级权限由前端隐藏 + 后端接口兜底） |
| 404 | 路由兜底页 / 提示资源不存在 |
| 5xx | 统一错误提示 + 上报 |

**前端最关心的响应头**：

| 头 | 作用 | 关键点 |
|---|---|---|
| `Content-Type` | 数据类型 | `application/json` 最常用；`text/event-stream` 是 SSE（AI 流式对话） |
| `Authorization` | `Bearer <JWT>` | axios 请求拦截器统一注入 |
| `Set-Cookie` | 服务端下 Cookie | `HttpOnly`（JS 读不到→防 XSS 窃取）、`Secure`、`SameSite`（防 CSRF） |
| `Cache-Control` / `ETag` | 缓存 | 第六节细讲 |

---

## 三、跨域 CORS（联调第一坑，重点）

### 3.1 同源策略

**同源 = 协议 + 域名 + 端口三者全同**。`http://localhost:5173`（前端 dev）调 `http://localhost:8080`（后端）——端口不同，**跨域**，浏览器直接拦截响应。

```text
http://a.com:80  vs  http://a.com:80      ✅ 同源
http://a.com     vs  https://a.com        ❌ 协议不同
http://a.com     vs  http://api.a.com     ❌ 域名不同
http://a.com:80  vs  http://a.com:8080    ❌ 端口不同（本地联调就栽在这）
```

> ⚠️ **后端常见误解**：跨域是**浏览器行为**，是同源策略这道防线在拦。Postman/curl/Spring 之间互调**根本没有跨域**。所以"我接口用 Postman 测是通的，前端一调就报 CORS"——不是你接口的锅。

浏览器为什么拦？没有同源策略，你在银行官网登录后的 Cookie 会被任何恶意页面的 JS 偷偷利用（CSRF 的土壤）。**CORS 的本质：服务端"白名单式"地告诉浏览器，哪些跨源请求我授权。**

### 3.2 简单请求 vs 预检请求

| | 简单请求 | 非简单请求 |
|---|---|---|
| 条件 | GET/POST/HEAD + 少数安全头（Content-Type 限 `text/plain`、`application/x-www-form-urlencoded`、`multipart/form-data`） | 其余所有：如 `Content-Type: application/json`、自定义头（`Authorization`）、PUT/DELETE |
| 流程 | 直接发，响应需带 CORS 头 | **先发 OPTIONS 预检**问服务端"我能不能用这些头/方法"，通过才发真请求 |
| 联调现象 | Network 里一次请求 | **每次业务请求前多一条 OPTIONS**（注意：它没有请求体，别在后端拦截器把它当异常） |

> 你项目里 axios 默认发 `application/json` + `Authorization` 头——**必然触发预检**。这就是为什么后端必须允许 `OPTIONS` 方法和这两个头。

### 3.3 解法一：后端 CORS 配置（Spring）

```java
// 方案 A：全局配置（推荐）—— WebMvcConfigurer
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")       // 开发期放开；生产改成具体域名白名单
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")               // 放行 Authorization 等自定义头
                .allowCredentials(true)            // 允许带 Cookie（与 * 混用时必须用 patterns）
                .maxAge(3600);                     // 预检结果缓存 1 小时，减少 OPTIONS 次数
    }
}
```

```java
// 方案 B：Spring Security 项目里用 CorsConfigurationSource（CORS 必须在鉴权 Filter 之前生效）
@Bean
CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(List.of("https://cs.example.com"));
    config.setAllowedMethods(List.of("*"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

### 3.4 解法二：Vite proxy（开发期推荐）

前端把"跨域"变成"同源"——浏览器只请求自己的 5173，由 Vite 服务器转发：

```javascript
// vite.config.js —— 前端侧配置，后端零改动
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,               // 把请求头里的 Host 改成目标地址
      rewrite: (p) => p.replace(/^\/api/, ''),
      ws: true,                          // WebSocket 也代理
    },
  },
}
```

### 3.5 解法三：Nginx 反向代理（生产标准答案）

```nginx
# 前后端同域名不同路径：浏览器视角全程同源，跨域问题从根上消失
server {
    listen 443 ssl;
    server_name cs.example.com;

    location / {
        root /usr/share/nginx/html;       # 前端静态资源
        try_files $uri $uri/ /index.html;
    }
    location /api/ {
        proxy_pass http://backend:8080/;  # 转发给后端（容器网络内）
        proxy_set_header Host $host;
    }
}
```

**选型口诀**：开发期 Vite proxy（不用改后端）；生产 Nginx 同域反代（最干净）；只有"前后端必须不同域"（如前端 CDN、第三方调用）才用后端 CORS。

---

## 四、登录态在前端怎么存

### 4.1 三种存储对照

| | Cookie | localStorage | sessionStorage |
|---|---|---|---|
| 容量 | ~4KB | ~5-10MB | ~5MB |
| 随请求自动携带 | ✅（同源/配置跨域） | ❌ 需手动放头里 | ❌ |
| JS 可读 | HttpOnly 时不可读 | ✅ 可读 | ✅ |
| 生命周期 | 可设过期 | 永久 | 标签页关闭即清 |
| XSS 窃取风险 | HttpOnly 可防 | **可被偷** | 可被偷 |
| CSRF 风险 | **有**（自动携带被利用） | 无（不自动携带） | 无 |

### 4.2 JWT 两大流派

| 方案 | 做法 | 攻防 |
|---|---|---|
| localStorage + Authorization 头 | 拦截器统一注入 `Bearer` | 无 CSRF 问题；但 token 可被 XSS 读走 → **XSS 防线必须硬**（输入消毒、CSP） |
| Cookie + HttpOnly | 服务端 Set-Cookie 下发 | JS 偷不走（防 XSS 窃取）；但要防 CSRF（SameSite=Strict/Lax + 后端 CSRF Token） |

> 后端视角总结：**XSS 偷的是"JS 能读的东西"，CSRF 用的是"浏览器自动带的东西"**。两套方案各堵一头、各漏一头，中小项目 `localStorage + JWT + 严格 XSS 防护` 最常见（本仓库即此路线，见 06 篇）。

### 4.3 XSS / CSRF 防御清单（面试也常问后端）

- **XSS**：输入输出消毒（DOMPurify）、Vue 默认文本插值不碰 `v-html`、CSP 响应头、HttpOnly Cookie。
- **CSRF**：SameSite Cookie、CSRF Token、校验 Origin/Referer、敏感操作二次确认。

---

## 五、实时通信四方案

| 方案 | 方向 | 协议 | 断线重连 | 适用 |
|---|---|---|---|---|
| 短轮询 | 客户端拉 | HTTP | 天然 | 简单低频（配置刷新） |
| 长轮询 | 客户端拉 | HTTP 挂起 | 客户端控制 | 老系统兼容 |
| **SSE** | 服务端推（单向） | 普通 HTTP（`text/event-stream`） | 浏览器**自动重连** | **AI 流式输出**、通知推送 |
| WebSocket | 双向 | 独立协议（`ws://`，握手借道 HTTP） | 需自己实现（见 08 篇 useWebSocket） | 客服坐席聊天、协同编辑 |

**为什么 AI 对话选 SSE**：大模型逐 token 吐字是纯单向推送，SSE 复用 HTTP（网关/Nginx 零改造、无状态、自动重连），比 WebSocket 轻。后端 Spring 用 `SseEmitter` 或 WebFlux `Flux<ServerSentEvent>` 即可对接。

```javascript
// SSE 消费端 5 行起步
const es = new EventSource('/api/chat/stream?convId=1')
es.onmessage = (e) => appendToBubble(JSON.parse(e.data).delta)   // 逐字追加
es.addEventListener('done', () => es.close())                    // 约定结束事件
es.onerror = () => { /* 浏览器会自动重连，或按需 close */ }
```

---

## 六、HTTP 缓存（前端发版的底层逻辑）

### 6.1 强缓存 vs 协商缓存

```text
浏览器请求 /assets/app.js
  ├─ 本地有强缓存（Cache-Control: max-age 未过期）
  │    → 直接用，一个请求都不发（Network 面板显示 "from disk cache"）
  └─ 过期了 → 协商缓存：带上 If-None-Match: "etag值" 问服务端
       ├─ 没变 → 304 Not Modified（不传 body，省流量）
       └─ 变了 → 200 + 新内容 + 新 ETag
```

| 头 | 角色 | 类比 |
|---|---|---|
| `Cache-Control: max-age=31536000, immutable` | 强缓存有效期 | 本地缓存 TTL |
| `ETag` / `Last-Modified` | 资源指纹 / 修改时间 | 数据版本号（乐观锁 version） |
| `304` | 协商缓存命中 | 版本一致返回空 diff |

### 6.2 前端发版策略（闭环 Vite 的 hash 机制）

```text
index.html        → Cache-Control: no-cache   （每次都协商，拿最新"入口"）
/assets/*.[hash].js/css → max-age=31536000, immutable（文件名带内容 hash，永不冲突）
```

发版时只有内容变了的文件 hash 才变 → 新用户拿全新文件，老用户 index.html 一变就发现新版本 → **秒级生效 + 零浪费下载**。这就是 [09 篇](./09-前端工程化：Vite构建与规范.md) 里 Nginx 那两段 location 配置的原理。

---

## 本章小结

- JS 单线程 + 渲染流水线 → 回流贵、合成便宜；批量读写、transform 动画。
- **跨域是浏览器的拦截**，Postman 没有跨域；`json` + `Authorization` 必触发 OPTIONS 预检。
- 跨域三解法：Vite proxy（开发）/ Nginx 同域反代（生产首选）/ 后端 CORS（必配兜底）。
- token 存储攻防：localStorage 防 CSRF 但怕 XSS，HttpOnly Cookie 防 XSS 但要防 CSRF。
- 推送选型：单向流式 → SSE（自动重连、零网关改造）；双向 → WebSocket（自己做心跳重连）。
- 缓存闭环：`index.html` 不缓存 + hash 资源永久缓存 = 发版秒生效。

## 动手练习

1. 造一次跨域：把 vite proxy 注释掉直连后端，观察 Console 报错与 Network 里的 OPTIONS；再分别用 proxy、后端 `addCorsMappings` 复现"能通"。
2. 打开 Network 面板勾选 Disable cache 前后各刷新一次，找出哪些请求 304、哪些 from disk cache，对照响应头解释。
3. 给项目 AI 对话抓包：确认响应头 `Content-Type: text/event-stream`，观察 delta 数据逐条到达的时序（Timing 面板）。
