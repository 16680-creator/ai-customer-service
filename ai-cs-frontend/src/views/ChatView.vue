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
              <div class="msg-bubble">{{ msg.content }}</div>
            </div>
            <el-empty v-if="messages.length === 0" description="开始和 AI 对话吧" :image-size="120" />
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
import { chatApi } from '../api'
import { ElMessage } from 'element-plus'
import { Plus, Promotion } from '@element-plus/icons-vue'

const sessions = ref([{ id: 'session-1', title: '默认会话' }])
const currentSession = ref('session-1')
const messages = ref([])
const inputMessage = ref('')
const sending = ref(false)
const messagesRef = ref(null)

function newSession() {
  const id = 'session-' + Date.now()
  sessions.value.push({ id, title: '新会话 ' + sessions.value.length })
  currentSession.value = id
  messages.value = []
}

async function sendMessage() {
  const text = inputMessage.value.trim()
  if (!text) return

  messages.value.push({ role: 'user', content: text })
  inputMessage.value = ''
  sending.value = true
  await scrollToBottom()

  try {
    const res = await chatApi.post(`/chat/send`, null, {
      params: { sessionId: currentSession.value, message: text }
    })
    const reply = res.data?.data || res.data?.message || '未获取到回复'
    messages.value.push({ role: 'assistant', content: reply })
  } catch (e) {
    const errMsg = e.response?.data?.message || e.message || '请求失败'
    messages.value.push({ role: 'assistant', content: '❌ 错误: ' + errMsg })
    ElMessage.error('对话请求失败: ' + errMsg)
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

.chat-input { display: flex; padding: 16px; border-top: 1px solid #ebeef5; }
</style>
