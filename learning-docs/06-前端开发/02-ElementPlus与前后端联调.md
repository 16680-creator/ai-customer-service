# Element Plus 组件库与前后端联调

> 本项目使用 **Element Plus 2.14** 作为 UI 组件库，**Axios** 做 HTTP 请求。
> 对应项目文件：`ai-cs-frontend/src/views/ChatView.vue`、`ai-cs-frontend/src/api/index.js`

---

## 一、Element Plus 简介

Element Plus 是 Vue 3 最流行的企业级 UI 组件库，提供 70+ 开箱即用的组件。

本项目用到的组件：

| 组件 | 用途 | 在哪个页面 |
|------|------|-----------|
| el-row / el-col | 栅格布局 | ChatView（左右分栏） |
| el-card | 卡片容器 | 所有页面 |
| el-button | 按钮 | 发送、新建 |
| el-input | 输入框 | 聊天输入 |
| el-avatar | 头像 | 消息气泡 |
| el-empty | 空状态 | 无消息时 |
| el-icon | 图标 | 按钮图标 |
| ElMessage | 消息提示 | 错误提示 |

---

## 二、本项目中的组件用法

### 2.1 栅格布局（左右分栏）

```vue
<!-- ChatView.vue 的布局结构 -->
<el-row :gutter="20" style="height: 100%">
  <!-- 左侧：会话列表（占 6/24 = 25%） -->
  <el-col :span="6">
    <el-card shadow="hover" class="session-card">
      <template #header>
        <span>会话列表</span>
        <el-button type="primary" size="small" @click="newSession">新对话</el-button>
      </template>
      <!-- 会话列表内容 -->
    </el-card>
  </el-col>

  <!-- 右侧：聊天区域（占 18/24 = 75%） -->
  <el-col :span="18">
    <el-card shadow="hover" class="chat-card">
      <!-- 消息列表 + 输入框 -->
    </el-card>
  </el-col>
</el-row>
```

### 2.2 消息气泡

```vue
<div v-for="(msg, i) in messages" :key="i" :class="['msg-row', msg.role]">
  <!-- 头像：用户蓝色，AI 绿色 -->
  <el-avatar :size="36" :style="{ background: msg.role === 'user' ? '#409eff' : '#67c23a' }">
    {{ msg.role === 'user' ? '我' : 'AI' }}
  </el-avatar>
  <!-- 消息内容 -->
  <div class="msg-bubble">{{ msg.content }}</div>
</div>

<!-- 空状态 -->
<el-empty v-if="messages.length === 0" description="开始和 AI 对话吧" :image-size="120" />
```

### 2.3 输入区域

```vue
<div class="chat-input">
  <el-input
    v-model="inputMessage"
    placeholder="输入你的问题..."
    :autosize="{ minRows: 2, maxRows: 4 }"  <!-- 自动高度 -->
    type="textarea"
    @keydown.enter.exact.prevent="sendMessage"  <!-- Enter 发送 -->
  />
  <el-button
    type="primary"
    @click="sendMessage"
    :loading="sending"     <!-- 加载中显示转圈 -->
    :icon="Promotion"
    style="margin-left: 10px; height: 54px"
  >
    发送
  </el-button>
</div>
```

### 2.4 消息提示

```javascript
import { ElMessage } from 'element-plus'

// 成功提示
ElMessage.success('发送成功')

// 错误提示
ElMessage.error('对话请求失败: ' + errMsg)

// 警告
ElMessage.warning('库存不足')
```

---

## 三、前后端联调

### 3.1 请求流程

```
前端 (localhost:5173)
    │
    │ Axios POST http://localhost:8083/chat/send
    │
    ▼
后端 ai-cs-chat (localhost:8083)
    │
    │ 返回 JSON: { "code": 200, "message": "成功", "data": "AI回复内容" }
    │
    ▼
前端解析 res.data.data 显示到聊天框
```

### 3.2 Axios 请求封装

```javascript
// api/index.js
import axios from 'axios'

const SERVICES = {
  chat: 'http://localhost:8083',
  user: 'http://localhost:8081',
  order: 'http://localhost:8087',
}

function createClient(baseURL) {
  const client = axios.create({ baseURL, timeout: 30000 })
  
  // 请求拦截器：自动加 Token
  client.interceptors.request.use(config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  })
  
  // 响应拦截器：统一错误处理
  client.interceptors.response.use(
    response => response,
    error => {
      if (error.response?.status === 401) {
        // Token 过期，跳转登录
        window.location.href = '/login'
      }
      return Promise.reject(error)
    }
  )
  
  return client
}

export const chatApi = createClient(SERVICES.chat)
```

### 3.3 在组件中调用

```javascript
// 发送对话
const res = await chatApi.post('/chat/send', null, {
  params: { sessionId: currentSession.value, message: text }
})

// 解析响应（对应后端 Result<T> 结构）
// res.data = { code: 200, message: "操作成功", data: "AI回复", timestamp: 123 }
const reply = res.data?.data || '未获取到回复'
```

### 3.4 生产环境走网关

```javascript
// 开发时：直连各服务
const SERVICES_DEV = {
  chat: 'http://localhost:8083',
  user: 'http://localhost:8081',
}

// 生产时：统一走 Gateway
const SERVICES_PROD = {
  chat: 'http://your-domain.com/ai-cs-chat',
  user: 'http://your-domain.com/ai-cs-user',
}

// 通过环境变量切换
const SERVICES = import.meta.env.PROD ? SERVICES_PROD : SERVICES_DEV
```

---

## 四、SSE 流式对话（进阶）

```javascript
// 流式接收 AI 回复（打字机效果）
async function sendStreamMessage(text) {
  messages.value.push({ role: 'user', content: text })
  messages.value.push({ role: 'assistant', content: '' })  // 占位
  
  const lastIndex = messages.value.length - 1
  
  const response = await fetch(
    `http://localhost:8083/api/chat/stream?message=${encodeURIComponent(text)}`
  )
  
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    
    const chunk = decoder.decode(value)
    // 逐字追加到消息内容
    messages.value[lastIndex].content += chunk
    await scrollToBottom()
  }
}
```

---

## 五、Vite 构建配置

```javascript
// vite.config.js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    // 开发代理（解决跨域）
    proxy: {
      '/api': {
        target: 'http://localhost:8080',  // 代理到网关
        changeOrigin: true,
      }
    }
  },
  build: {
    outDir: 'dist',
    // 打包优化
    rollupOptions: {
      output: {
        manualChunks: {
          'element-plus': ['element-plus'],
          'vue-vendor': ['vue', 'vue-router'],
        }
      }
    }
  }
})
```

---

## 六、动手练习

1. 修改 ChatView 的头像颜色和大小
2. 给发送按钮加一个禁用状态（输入为空时禁用）
3. 实现消息时间的显示
4. 用 Vite proxy 替代直连地址
5. 实现 SSE 流式对话的打字机效果

---

## 学习检查清单

- [ ] 熟练使用 Element Plus 常用组件
- [ ] 理解 el-row/el-col 栅格系统
- [ ] 会配置 Axios 拦截器（Token、错误处理）
- [ ] 理解前后端联调的数据格式（Result<T>）
- [ ] 理解开发/生产环境的 API 地址切换
- [ ] 了解 SSE 流式输出的前端实现
- [ ] 理解 Vite 的 proxy 和 build 配置

---

## 下一步

→ [07-运维部署/01-Docker容器化](../07-运维部署/01-Docker容器化.md)
