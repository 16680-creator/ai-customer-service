<template>
  <div class="chat-view">
    <el-row :gutter="20" style="height: 100%">
      <!-- 左侧会话列表 -->
      <el-col :span="6">
        <el-card shadow="hover" class="session-card">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span style="font-weight: 600">会话列表</span>
              <el-button type="primary" size="small" @click="newSession" :icon="Plus">新对话</el-button>
            </div>
          </template>
          <div class="session-list">
            <div
              v-for="s in sessions"
              :key="s.id"
              :class="['session-item', { active: currentSession === s.id }]"
              @click="switchSession(s.id)"
            >
              <el-icon><ChatDotSquare /></el-icon>
              <span class="session-title">{{ s.title }}</span>
              <el-tooltip content="删除会话" placement="top">
                <el-icon class="session-delete" @click.stop="handleDeleteSession(s)"><Delete /></el-icon>
              </el-tooltip>
            </div>
            <el-empty v-if="sessions.length === 0" description="暂无会话" :image-size="60" />
          </div>
        </el-card>
      </el-col>

      <!-- 右侧聊天区 -->
      <el-col :span="18">
        <el-card shadow="hover" class="chat-card">
          <div class="chat-messages" ref="messagesRef">
            <div v-for="(msg, i) in currentMessages" :key="i" :class="['msg-row', msg.role]">
              <el-avatar :size="36" :style="{ background: msg.role === 'user' ? '#409eff' : '#67c23a' }">
                {{ msg.role === 'user' ? '我' : 'AI' }}
              </el-avatar>
              <div class="msg-bubble">
                <img v-if="msg.imageUrl" :src="msg.imageUrl" class="msg-image" alt="图片" />
                <div>{{ msg.content }}</div>
                <!-- 引用溯源卡片：RAG 回答有 citations 时展示 -->
                <div v-if="msg.citations && msg.citations.length > 0" class="citation-cards">
                  <el-divider content-position="left">
                    <el-tag size="small" type="info" effect="plain">引用来源 {{ msg.citations.length }} 条</el-tag>
                  </el-divider>
                  <div class="citation-list">
                    <el-card
                      v-for="(cit, ci) in msg.citations"
                      :key="ci"
                      shadow="never"
                      class="citation-card"
                    >
                      <div class="citation-header">
                        <el-icon :size="14"><Document /></el-icon>
                        <span class="citation-title">{{ cit.title || '未命名文档' }}</span>
                        <span v-if="cit.page" class="citation-page">P.{{ cit.page }}</span>
                      </div>
                      <div v-if="cit.score != null" class="citation-score">
                        <span class="score-label">相关度</span>
                        <el-progress
                          :percentage="Math.round(cit.score * 100)"
                          :stroke-width="6"
                          size="small"
                          :color="cit.score >= 0.7 ? '#67c23a' : cit.score >= 0.5 ? '#e6a23c' : '#f56c6c'"
                        />
                      </div>
                      <div class="citation-content-preview">
                        <el-text line-clamp="3" size="small" type="info">
                          {{ cit.content }}
                        </el-text>
                      </div>
                    </el-card>
                  </div>
                </div>
              </div>
            </div>
            <el-empty v-if="currentMessages.length === 0" description="开始和 AI 对话吧" :image-size="120" />
          </div>

          <div class="chat-mode-bar">
            <el-radio-group v-model="chatMode" size="small">
              <el-radio-button value="normal">普通对话</el-radio-button>
              <el-radio-button value="rag">RAG 知识库对话</el-radio-button>
            </el-radio-group>
            <el-input
              v-if="chatMode === 'rag'"
              v-model="knowledgeBase"
              placeholder="知识库标识（如 test-kb）"
              size="small"
              style="width: 220px; margin-left: 12px"
              clearable
            />
            <el-tooltip v-if="chatMode === 'rag'" content="需先在「向量知识库」页入库资料" placement="top">
              <el-link type="primary" :underline="false" style="margin-left: 12px" @click="$router.push('/rag-kb')">
                前往入库
              </el-link>
            </el-tooltip>
          </div>

          <div class="chat-input">
            <el-input
              v-model="inputMessage"
              placeholder="输入你的问题..."
              :autosize="{ minRows: 2, maxRows: 4 }"
              type="textarea"
              @keydown.enter.exact.prevent="sendMessage"
            />
            <el-upload
              :show-file-list="false"
              :auto-upload="false"
              :on-change="handleImageChange"
              accept="image/jpeg,image/png,image/webp,image/gif"
              style="margin-left: 10px"
            >
              <el-button :icon="Picture" :disabled="sending" title="上传图片" style="height: 54px" />
            </el-upload>
            <el-button v-if="uploadedImageUrl" type="warning" :icon="Delete" @click="clearImage" title="移除图片" style="margin-left: 6px; height: 54px" />
            <el-button type="primary" @click="sendMessage" :loading="sending" :icon="Promotion" style="margin-left: 10px; height: 54px">
              发送
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { getToken, getUser } from '../utils/auth'
import { chatApi, messageApi, visionApi } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Promotion, Document, Delete, Picture } from '@element-plus/icons-vue'

const GATEWAY = import.meta.env.VITE_GATEWAY || 'http://localhost:8080'

/** 获取当前登录用户 ID */
function getUserId() {
  const user = getUser()
  return user ? user.userId : null
}

const sessions = ref([])
const currentSession = ref('')
// 按会话ID隔离消息存储（key=sessionId, value=消息数组）
const sessionMessages = ref({})
// 当前会话的消息列表（与 sessionMessages 中对应数组引用一致）
const currentMessages = ref([])
const inputMessage = ref('')
const sending = ref(false)
const messagesRef = ref(null)
const chatMode = ref('normal')
const knowledgeBase = ref('')
// 已上传的图片 URL（多模态图生文，非空时走 /chat/vision/sse）
const uploadedImageUrl = ref('')

// 将当前显示切换到指定会话的消息数组
function switchToMessages(sessionId) {
  if (!sessionMessages.value[sessionId]) {
    sessionMessages.value = { ...sessionMessages.value, [sessionId]: [] }
  }
  currentMessages.value = sessionMessages.value[sessionId]
}

// 从后端加载会话历史并填充到当前消息数组
async function loadHistory() {
  const sessionKey = currentSession.value
  if (!sessionKey) return
  // 已有消息内容则跳过（避免重复加载）
  if (sessionMessages.value[sessionKey] && sessionMessages.value[sessionKey].length > 0) return
  try {
    const resp = await chatApi.get('/history', { params: { sessionKey } })
    if (resp.data && resp.data.code === 200 && resp.data.data && resp.data.data.length > 0) {
      const list = resp.data.data.map(m => ({ role: m.role, content: m.content }))
      // 保持数组引用不变，仅替换内容
      const arr = sessionMessages.value[sessionKey]
      arr.length = 0
      arr.push(...list)
    }
  } catch (e) {
    // 后端不可用时静默降级
    console.warn('加载历史会话失败:', e.message)
  }
}

// 从后端加载用户的所有会话列表
async function loadSessions() {
  const userId = getUserId()
  if (!userId) return
  try {
    const resp = await messageApi.get('/sessions', { params: { userId } })
    if (resp.data && resp.data.code === 200 && resp.data.data && resp.data.data.length > 0) {
      sessions.value = resp.data.data.map(s => ({
        id: String(s.id),
        title: s.title || '默认会话'
      }))
    }
  } catch (e) {
    console.warn('加载会话列表失败:', e.message)
  }
}

// 切换会话（左侧列表点击）
function switchSession(id) {
  currentSession.value = id
  switchToMessages(id)
  loadHistory()
}

// 新建会话（持久化到后端）
async function newSession() {
  const userId = getUserId()
  if (!userId) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const title = '新会话 ' + (sessions.value.length + 1)
    const resp = await messageApi.post('/session', null, { params: { userId, title } })
    if (resp.data && resp.data.code === 200 && resp.data.data) {
      const session = resp.data.data
      sessions.value.push({ id: String(session.id), title: session.title })
      switchSession(String(session.id))
    }
  } catch (e) {
    ElMessage.error('创建会话失败: ' + (e.message || ''))
  }
}

// 删除会话（含其下所有消息）
async function handleDeleteSession(session) {
  try {
    await ElMessageBox.confirm(
      `确定删除会话「${session.title}」吗？该会话下的所有消息也会被删除，且不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return // 用户取消
  }
  try {
    const resp = await messageApi.delete(`/session/${session.id}`)
    if (resp.data && resp.data.code === 200) {
      // 从列表移除
      sessions.value = sessions.value.filter(s => s.id !== session.id)
      delete sessionMessages.value[session.id]
      // 删除的是当前会话：切换到剩余会话或清空
      if (currentSession.value === session.id) {
        if (sessions.value.length > 0) {
          switchSession(sessions.value[0].id)
        } else {
          currentSession.value = ''
          currentMessages.value = []
        }
      }
      ElMessage.success('会话已删除')
    }
  } catch (e) {
    ElMessage.error('删除会话失败: ' + (e.message || ''))
  }
}

// 初始化：加载会话列表，无会话时创建默认会话
async function initApp() {
  await loadSessions()
  if (sessions.value.length > 0) {
    // 切换到最近会话（列表已按 updateTime 倒序，第一个就是最新）
    switchSession(sessions.value[0].id)
  } else {
    // 还没有任何会话，自动创建一个
    await newSession()
  }
}

// 监听 currentSession 变化
watch(currentSession, (newId) => {
  if (newId) {
    switchToMessages(newId)
    loadHistory()
  }
})

// 页面初始化
initApp()

// 上传图片：调 /chat/upload-image 拿到 MinIO URL，暂存待发送
async function handleImageChange(uploadFile) {
  const file = uploadFile?.raw
  if (!file) return
  try {
    const resp = await visionApi.uploadImage(file)
    if (resp.data && resp.data.code === 200 && resp.data.data) {
      uploadedImageUrl.value = resp.data.data
      ElMessage.success('图片已上传')
    } else {
      ElMessage.error(resp.data?.message || '图片上传失败')
    }
  } catch (e) {
    ElMessage.error('图片上传失败: ' + (e.message || ''))
  }
}

// 移除已上传的图片
function clearImage() {
  uploadedImageUrl.value = ''
}

// 真正的流式对话（SSE）：fetch + ReadableStream 解析，逐 token 追加（打字机效果）
// 支持图片：携带 imageUrl 时走 /chat/vision/sse（多模态图生文）
async function sendMessage() {
  const text = inputMessage.value.trim()
  const imageUrl = uploadedImageUrl.value
  // 图片对话允许只有图片无文字；纯文本对话必须有文字
  if (!text && !imageUrl) return
  // 流式输出期间禁止重复发送（按钮 loading 挡不住 Enter 键，需显式防重入）
  if (sending.value) return
  if (chatMode.value === 'rag' && !knowledgeBase.value.trim()) {
    return ElMessage.warning('RAG 模式请先填写知识库标识')
  }

  currentMessages.value.push({ role: 'user', content: text || '(图片)', imageUrl: imageUrl || null })
  inputMessage.value = ''
  uploadedImageUrl.value = ''
  sending.value = true
  await scrollToBottom()

  // 预置空回复气泡，流式填充
  const assistant = { role: 'assistant', content: '' }
  currentMessages.value.push(assistant)
  await scrollToBottom()

  try {
    const params = new URLSearchParams({ sessionId: currentSession.value, message: text })
    if (imageUrl) params.set('imageUrl', imageUrl)
    if (chatMode.value === 'rag') params.set('knowledgeBase', knowledgeBase.value.trim())

    // 有图片走图片对话 SSE，无图片走普通 SSE
    const endpoint = imageUrl ? 'vision/sse' : 'stream/sse'
    const resp = await fetch(`${GATEWAY}/api/chat/${endpoint}?${params}`, {
      method: 'POST',
      headers: { Authorization: 'Bearer ' + (getToken() || '') }
    })
    if (!resp.ok || !resp.body) throw new Error('HTTP ' + resp.status)

    const reader = resp.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let streamError = null

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      // 按 SSE 事件分隔（空行）逐条解析
      let sep
      while ((sep = buffer.indexOf('\n\n')) >= 0) {
        const rawEvent = buffer.slice(0, sep)
        buffer = buffer.slice(sep + 2)
        for (const line of rawEvent.split('\n')) {
          if (!line.startsWith('data:')) continue
          const data = line.slice(5).trim()
          if (!data) continue
          let obj
          try { obj = JSON.parse(data) } catch { continue }
          // done 事件仅携带 citations，不带 content；content 已逐 token 累积，跳过避免重复追加
          if (obj.content && !obj.done) {
            assistant.content += obj.content
            assistant.citations = assistant.citations || []
            await scrollToBottom()
          }
          if (obj.citations) {
            assistant.citations = obj.citations
          }
          if (obj.error) streamError = obj.error
          if (obj.done) {
            // done 事件：保留已累积的 citations（已在 obj.citations 中完整返回）
          }
        }
      }
    }

    if (streamError) throw new Error(streamError)
    if (!assistant.content) assistant.content = '(无回复)'
  } catch (e) {
    // 区分"连接被中止"（页面刷新/重复发送导致浏览器中断 fetch）与真实业务错误
    const aborted = e?.name === 'AbortError' || e?.message?.includes('Failed to fetch')
    assistant.content = aborted ? '⚠️ 连接中断，回复未完成，请重试。' : ('❌ 错误: ' + (e.message || '请求失败'))
    ElMessage.error(aborted ? '对话连接中断，请重试' : '对话请求失败: ' + (e.message || ''))
  } finally {
    sending.value = false
    await scrollToBottom()
  }
}

async function scrollToBottom() {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}
</script>

<style scoped>
.chat-view { height: calc(100vh - 140px); }
.session-card { height: 100%; }
.session-card :deep(.el-card__body) { padding: 12px; height: calc(100% - 60px); overflow-y: auto; }
.session-list { display: flex; flex-direction: column; gap: 8px; }
.session-item {
  display: flex; align-items: center; gap: 8px; padding: 10px 12px;
  border-radius: 8px; cursor: pointer; transition: all 0.2s;
}
.session-item:hover { background: #f0f2f5; }
.session-item.active { background: #ecf5ff; color: #409eff; font-weight: 500; }
.session-title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.session-delete {
  visibility: hidden; color: #f56c6c; font-size: 14px; cursor: pointer;
  transition: opacity 0.2s;
}
.session-item:hover .session-delete { visibility: visible; }
.session-delete:hover { opacity: 0.8; }

.chat-card { height: 100%; display: flex; flex-direction: column; }
.chat-card :deep(.el-card__body) { flex: 1; display: flex; flex-direction: column; padding: 0; overflow: hidden; }

.chat-messages {
  flex: 1; overflow-y: auto; padding: 20px;
  display: flex; flex-direction: column; gap: 16px;
}

.msg-row { display: flex; gap: 10px; align-items: flex-start; }
.msg-row.user { flex-direction: row-reverse; }
.msg-bubble {
  max-width: 65%; padding: 12px 16px; border-radius: 12px;
  font-size: 14px; line-height: 1.6; white-space: pre-wrap; word-break: break-word;
}
.msg-row.user .msg-bubble { background: #409eff; color: #fff; border-top-right-radius: 4px; }
.msg-row.assistant .msg-bubble { background: #f0f2f5; color: #1d1e2c; border-top-left-radius: 4px; }
.msg-image {
  max-width: 200px; max-height: 200px; border-radius: 8px;
  display: block; margin-bottom: 8px; object-fit: cover;
}

.chat-mode-bar {
  display: flex; align-items: center; padding: 10px 16px 0;
}
.chat-input { display: flex; padding: 16px; border-top: 1px solid #ebeef5; }

/* 引用溯源卡片样式 */
.citation-cards { margin-top: 12px; }
.citation-list { display: flex; flex-direction: column; gap: 8px; }
.citation-card { border: 1px solid #e8e8e8; border-radius: 8px; padding: 0; }
.citation-card :deep(.el-card__body) { padding: 10px 14px; }
.citation-header {
  display: flex; align-items: center; gap: 6px;
  font-size: 13px; font-weight: 500; color: #303133; margin-bottom: 6px;
}
.citation-title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.citation-page {
  font-size: 11px; color: #909399; background: #f4f4f5;
  padding: 1px 6px; border-radius: 4px; flex-shrink: 0;
}
.citation-score { margin-bottom: 6px; display: flex; align-items: center; gap: 8px; }
.score-label { font-size: 12px; color: #909399; white-space: nowrap; }
.citation-content-preview {
  background: #fafafa; border-radius: 4px; padding: 6px 8px;
  font-size: 12px; line-height: 1.5;
}
</style>