# 前端工程化：Vite 构建与规范

> **前置知识**：[00-前端技术全景介绍](./00-前端技术全景介绍.md) 第五节（生态对照表）；[02-JavaScript核心基础](./02-JavaScript核心基础.md) 模块化一节。
> **定位**：把你熟悉的 Maven/CI 工程体系"翻译"到前端，让 `ai-cs-frontend` 具备企业级工程规范。

---

## 一、工程化全景：先建立 Maven ↔ npm 对照表

| 前端 | Java 世界 | 说明 |
|---|---|---|
| npm / pnpm / yarn | Maven / Gradle | 包管理器与中央仓库（npmjs.com ≈ Maven Central） |
| `package.json` | `pom.xml` | 依赖声明 + 插件（scripts） + 工程元信息 |
| `package-lock.json` | （锁定传递依赖的 BOM） | **必须提交**，保证团队/CI 构建一致 |
| `node_modules/` | `~/.m2/repository` | 依赖落地目录，**进 .gitignore** |
| `npm run dev / build` | `mvn spring-boot:run / package` | 生命周期命令 |
| Vite / Webpack | maven-compiler + shade/assembly | 编译、打包、资源处理 |
| `.env.development / .env.production` | `application-dev.yml / application-prod.yml` | 多环境配置 |
| ESLint | CheckStyle / SpotBugs / SonarQube | 代码质量静态检查 |
| Prettier | formatter-maven-plugin | 格式化（不管质量只管格式） |
| husky + lint-staged | 流水线质量门禁 | 提交前自动检查（Git hook） |
| Vitest / Playwright | JUnit / Selenium | 单测 / E2E |
| npm workspace（monorepo） | Maven 多模块聚合 | 多包同仓管理 |

> 📌 一句话理解 npm scripts：`package.json` 里的 `"scripts"` 就是你的 pom `<plugins><execution>`——`npm run lint` ≈ `mvn checkstyle:check`。

---

## 二、package.json 深读

```jsonc
{
  "name": "ai-cs-frontend",
  "version": "1.0.0",
  "type": "module",                       // 用 ESM 模块体系（02 篇讲过 ESM vs CommonJS）
  "scripts": {
    "dev": "vite",                        // 启动开发服务器（≈ spring-boot:run）
    "build": "vite build",                // 生产构建到 dist/（≈ mvn package）
    "preview": "vite preview",            // 本地预览构建产物
    "lint": "eslint . --fix"              // 自定义质量命令
  },
  "dependencies": {                       // 运行时依赖 ≈ scope=compile
    "vue": "^3.5.0",
    "element-plus": "^2.9.0",
    "axios": "^1.7.0"
  },
  "devDependencies": {                    // 只在构建期用 ≈ scope=provided/test
    "vite": "^6.0.0",
    "eslint": "^9.0.0"
  }
}
```

### semver 版本号的坑

`^3.5.0` 允许 `3.x.x` 向上小版本、`~3.5.0` 只允许 `3.5.x` 补丁。所以：

- `npm i` 会按 `^` 拉新版 → 环境之间可能不一致；
- **lock 文件就是解药**：CI 一律 `npm ci`（严格按 lock 安装），等价于"别用版本区间，给我可重现构建"。

---

## 三、Vite 核心

### 3.1 为什么 Vite 快（面试高频）

- **Webpack 老路**：启动时把全项目打包成 bundle 才能开服务 → 项目越大越慢。
- **Vite 思路**：dev 阶段直接利用浏览器原生 ESM——浏览器请求哪个模块就编译哪个（按需），依赖预打包用 Go 写的 **esbuild**（比 JS 工具链快 10~100 倍）。
- **build 阶段**切换到 Rollup 做深度优化（tree-shaking、分包），dev/prod 各取所长。

### 3.2 vite.config.js 逐行讲解

```javascript
// vite.config.js —— ≈ pom.xml + application.yml 的合体
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'node:path'

export default defineConfig({
  plugins: [vue()],

  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),   // @ 指向 src，import '@api/user' 不再数 ../
    },
  },

  server: {
    port: 5173,
    proxy: {
      // 开发期跨域解法：前端只认同源 /api，由 Vite 转发给后端
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 后端接口没有 /api 前缀时重写掉；有前缀则删掉这条
        rewrite: (p) => p.replace(/^\/api/, ''),
        // SSE/WebSocket 场景
        ws: true,
      },
    },
  },

  build: {
    outDir: 'dist',
    sourcemap: false,             // 生产不开（暴露源码 + 体积）；排障时可临时开
    rollupOptions: {
      output: {
        // 大依赖单独分包：稳定不变 → 浏览器长久缓存
        manualChunks: { vendor: ['vue', 'vue-router', 'pinia'], element: ['element-plus'] },
      },
    },
  },
})
```

> 跨域三种解法（Vite proxy / 后端 CORS / Nginx 反代）的完整原理与生产配置，见 [10-浏览器原理与HTTP跨域](./10-浏览器原理与HTTP跨域.md) 第三节。

### 3.3 环境变量（≈ application-dev.yml）

```bash
# .env.development —— npm run dev 时生效
VITE_API_BASE=/api          # ⚠️ 只有 VITE_ 前缀的变量才会暴露给前端代码！
VITE_APP_TITLE=AI 客服系统
```

```bash
# .env.production —— npm run build 时生效
VITE_API_BASE=https://api.example.com
```

```javascript
// 代码中使用：import.meta.env ≈ @Value / Environment.getProperty
const base = import.meta.env.VITE_API_BASE
```

> ⚠️ **安全铁律**：`VITE_` 变量会被打进构建产物，人人可见。密钥（数据库密码、私钥）绝不能放这里——需要保密的逻辑放**后端**。这和"配置中心里的密码不进前端"是一个道理。

### 3.4 构建产物与 Nginx 托管

```bash
npm run build
# dist/
# ├── index.html                ← 入口（不带 hash，发版会变）
# ├── assets/
# │   ├── index-3f9a1b2c.js     ← 内容 hash：内容变 → 文件名变
# │   ├── index-8c11d0ee.css
# │   └── vendor-e5f2a1b9.js
# └── favicon.svg
```

后端是 `java -jar` 一键跑，前端必须由 **Nginx 托管静态文件**，并处理 SPA 路由刷新 404 问题：

```nginx
server {
    listen 80;
    root /usr/share/nginx/html;          # dist 挂载点

    location / {
        try_files $uri $uri/ /index.html;  # ✅ 关键：SPA 深链接刷新时回退到 index.html
    }

    location /api/ {                     # 同域反代：顺带消灭跨域
        proxy_pass http://backend:8080/;
    }

    location /assets/ {
        add_header Cache-Control "public, max-age=31536000, immutable";  # 带 hash 资源永久缓存
    }
}
```

缓存与 hash 的闭环原理见 [10-浏览器原理与HTTP跨域](./10-浏览器原理与HTTP跨域.md) 第六节。

---

## 四、代码规范三件套（可直接落地）

```bash
# 1. 安装（Vite 项目用 flat config 的 eslint 9）
npm i -D eslint eslint-plugin-vue prettier eslint-config-prettier husky lint-staged
```

```javascript
// eslint.config.js —— ESLint 管质量（未用变量、误改 props），Prettier 管格式（缩进引号），分工不打架
import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'

export default [
  js.configs.recommended,
  ...pluginVue.configs['flat/recommended'],
  { rules: { 'no-unused-vars': ['error', { argsIgnorePattern: '^_' }] } },
]
```

```jsonc
// .prettierrc —— 格式统一，终结"格式之争"的 code review 浪费
{
  "semi": false,
  "singleQuote": true,
  "printWidth": 100,
  "trailingComma": "es5"
}
```

```jsonc
// package.json 增量：Git 提交门禁
{
  "scripts": { "prepare": "husky", "lint": "eslint . --fix", "format": "prettier --write ." },
  "lint-staged": {
    "*.{js,vue,ts}": ["eslint --fix", "prettier --write"],
    "*.{css,html,json}": ["prettier --write"]
  }
}
```

```bash
npx husky init
# .husky/pre-commit 内容：只检查本次暂存的文件（快，不拖累整个仓库）
echo "npx lint-staged" > .husky/pre-commit
```

> 类比：`pre-commit` hook ≈ 流水线的质量门禁（SonarQube quality gate），只是把检查点前移到了提交瞬间——**问题越早暴露，修复成本越低**，这条理念前后端完全一致。

---

## 五、构建部署与 CI/CD

### 5.1 Dockerfile 多阶段构建（对照后端镜像分层思想）

```dockerfile
# Dockerfile —— 第一阶段构建，第二阶段运行，最终镜像不含 node_modules
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./          # 先拷依赖清单 → 利用 Docker 层缓存，依赖没变不重装
RUN npm ci                     # 严格按 lock 安装
COPY . .
RUN npm run build

FROM nginx:alpine              # 运行镜像只有 nginx + 静态文件，几十 MB
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

> 和后端 `maven:3.9-eclipse-temurin` 构建 → `temurin:17-jre` 运行是同一个分层套路。

### 5.2 GitHub Actions 流水线

```yaml
# .github/workflows/frontend.yml
name: frontend-ci
on:
  push: { paths: ['ai-cs-frontend/**'] }   # 只在前端目录变更时触发（≈ mvn -pl 模块化触发）
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: 20, cache: npm }
      - run: npm ci
        working-directory: ai-cs-frontend
      - run: npm run lint          # 质量门禁
        working-directory: ai-cs-frontend
      - run: npm run build         # 构建本身就是最好的类型/语法检查
        working-directory: ai-cs-frontend
      # 后续可接 docker build & push、镜像部署（对接本仓库 07-运维部署 的 K8s 流程）
```

---

## 六、monorepo 一瞥

`pnpm workspace`：一个仓库管多个前端包（如 `web-admin` + `web-mobile` + 共享 `types` 包）。类比 Maven 聚合工程 `<modules>`。**判据**：出现"两个前端要共享同一份 TS 类型/工具函数"时再上，单人单项目没必要。

---

## 本章小结

- 对照表是钥匙：npm≈Maven、package.json≈pom、lock 文件≈可重现构建、`.env.*`≈profile 配置。
- semver 的 `^` 区间 + lock 文件：CI 用 `npm ci` 保证一致性。
- Vite dev 快在"原生 ESM 按需编译 + esbuild"，build 走 Rollup；`server.proxy` 是开发期跨域首选。
- 环境变量带 `VITE_` 前缀才会进产物——**密钥永远不进前端**。
- 产物 hash 决定缓存策略：`index.html` 不缓存，`assets/*` 永久缓存；Nginx `try_files` 解决 SPA 刷新 404。
- 规范三件套 ESLint（质量）+ Prettier（格式）+ husky（门禁）一次配好，全队受益。

## 动手练习

1. 给 `ai-cs-frontend` 接入 ESLint + Prettier + husky，故意提交一个 `const a=1` 未使用变量，验证 pre-commit 拦截。
2. 配置 `server.proxy` 把 `/api` 转发到本地后端，并在 Network 面板确认请求同源、响应正常（跨域原理见 10 篇）。
3. 手写本节 Dockerfile + nginx.conf，`docker build` 后浏览器访问，验证深链接刷新不 404、`/api` 反代可用。
