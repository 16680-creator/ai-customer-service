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
              @click="currentSession = s.id"
            >
              <el-icon><ChatDotSquare /></el-icon>
              <span class="session-title">{{ s.title }}</span>
            </div>
            <el-empty v-if="sessions.length === 0" description="暂无会话" :image-size="60" />
          </div>
        </el-card>
      </el-col>

      <!-- 右侧聊天区 -->
      <el-col :span="18">
        <el-card shadow="hover" class="chat-card">
          <div class="chat-messages" ref="messagesRef">
            <div v-for="(msg, i) in messages" :key="i" :class="['msg-row', msg.role]">
              <el-avatar :size="36" :style="{ background: msg.role === 'user' ? '#409eff' : '#67c23a' }">
                {{ msg.role === 'user' ? '我' : 'AI' }}
              </el-avatar>
              <div class="msg-bubble">
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
            <el-empty v-if="messages.length === 0" description="开始和 AI 对话吧" :image-size="120" />
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
import { ref, nextTick } from 'vue'
import { getToken } from '../utils/auth'
import { ElMessage } from 'element-plus'
import { Plus, Promotion, Document } from '@element-plus/icons-vue'

const GATEWAY = import.meta.env.VITE_GATEWAY || 'http://localhost:8080'

const sessions = ref([{ id: 'session-1', title: '默认会话' }])
const currentSession = ref('session-1')
const messages = ref([])
const inputMessage = ref('')
const sending = ref(false)
const messagesRef = ref(null)
const chatMode = ref('normal')
const knowledgeBase = ref('')

function newSession() {
  const id = 'session-' + Date.now()
  sessions.value.push({ id, title: '新会话 ' + sessions.value.length })
  currentSession.value = id
  messages.value = []
}

// 真正的流式对话（SSE）：fetch + ReadableStream 解析，逐 token 追加（打字机效果）
async function sendMessage() {
  const text = inputMessage.value.trim()
  if (!text) return
  if (chatMode.value === 'rag' && !knowledgeBase.value.trim()) {
    return ElMessage.warning('RAG 模式请先填写知识库标识')
  }

  messages.value.push({ role: 'user', content: text })
  inputMessage.value = ''
  sending.value = true
  await scrollToBottom()

  // 预置空回复气泡，流式填充
  const assistant = { role: 'assistant', content: '' }
  messages.value.push(assistant)
  await scrollToBottom()

  try {
    const params = new URLSearchParams({ sessionId: currentSession.value, message: text })
    if (chatMode.value === 'rag') params.set('knowledgeBase', knowledgeBase.value.trim())

    const resp = await fetch(`${GATEWAY}/api/chat/stream/sse?${params}`, {
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
          if (obj.content) {
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
    assistant.content = '❌ 错误: ' + (e.message || '请求失败')
    ElMessage.error('对话请求失败: ' + (e.message || ''))
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
.session-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

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
