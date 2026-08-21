<template>
  <div class="agent-page">
    <el-card shadow="hover" class="agent-card">
      <template #header>
        <div class="header">
          <span class="title">售后 Agent 编排</span>
          <el-tag size="small" :type="healthOk ? 'success' : 'danger'" effect="plain">
            {{ healthOk ? '服务正常' : '服务异常' }}
          </el-tag>
        </div>
      </template>

      <div class="agent-body">
        <div class="messages" ref="msgRef">
          <div v-for="(m, i) in messages" :key="i" :class="['turn', m.role]">
            <el-avatar :size="32" :style="{ background: m.role === 'user' ? '#409eff' : '#9254de' }">
              {{ m.role === 'user' ? '我' : 'A' }}
            </el-avatar>
            <div class="bubble">
              <div class="reply">{{ m.content }}</div>
              <div v-if="m.meta" class="meta">
                <el-tag v-if="m.meta.state" size="small" type="info" effect="plain">{{ m.meta.state }}</el-tag>
                <el-tag v-for="it in (m.meta.intents || [])" :key="it" size="small" type="warning" effect="plain">{{ it }}</el-tag>
                <span v-if="m.meta.applicationNo" class="app-no">售后单号：{{ m.meta.applicationNo }}</span>
              </div>
              <!-- 待确认写操作 -->
              <div v-if="m.meta && m.meta.confirmationToken" class="confirm-box">
                <div class="plan">
                  <div class="plan-title">待确认操作</div>
                  <pre v-if="m.meta.actionPlan">{{ JSON.stringify(m.meta.actionPlan, null, 2) }}</pre>
                  <div v-if="m.meta.candidates && m.meta.candidates.length" class="candidates">
                    候选：<el-tag v-for="c in m.meta.candidates" :key="c" size="small">{{ c }}</el-tag>
                  </div>
                </div>
                <div class="confirm-actions">
                  <el-button type="primary" size="small" :loading="confirming" @click="confirm(m)">确认执行</el-button>
                  <el-button size="small" @click="cancelConfirm(m)">取消</el-button>
                </div>
              </div>
            </div>
          </div>
          <el-empty v-if="messages.length === 0" description="和售后 Agent 对话，例如：我要申请退款 / 查一下我的订单" :image-size="100" />
        </div>

        <div v-if="handoff" class="handoff">
          <el-alert type="warning" :closable="false" :title="'已转人工：' + (handoff.reason || '')" />
        </div>

        <div class="input-bar">
          <el-input
            v-model="input"
            type="textarea"
            :rows="2"
            placeholder="输入指令，Enter 发送（Shift+Enter 换行）"
            @keydown.enter.exact.prevent="send"
          />
          <el-button type="primary" :icon="Promotion" :loading="sending" @click="send">发送</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { agentApi } from '../api'
import { ElMessage } from 'element-plus'
import { Promotion } from '@element-plus/icons-vue'

const messages = ref([])
const input = ref('')
const sending = ref(false)
const confirming = ref(false)
const healthOk = ref(true)
const handoff = ref(null)
const msgRef = ref(null)
const sessionId = ref('agent-' + Date.now())

onMounted(checkHealth)

async function checkHealth() {
  try {
    await agentApi.get('/health')
    healthOk.value = true
  } catch {
    healthOk.value = false
  }
}

async function scroll() {
  await nextTick()
  if (msgRef.value) msgRef.value.scrollTop = msgRef.value.scrollHeight
}

async function send() {
  const text = input.value.trim()
  if (!text || sending.value) return
  messages.value.push({ role: 'user', content: text })
  input.value = ''
  sending.value = true
  await scroll()
  try {
    const { data } = await agentApi.post('/chat', {
      sessionId: sessionId.value,
      input: text,
    })
    const res = data?.data ?? data
    handoff.value = res.handoff || null
    messages.value.push({
      role: 'assistant',
      content: res.reply || '(无回复)',
      meta: {
        state: res.state,
        intents: res.intents || [],
        applicationNo: res.applicationNo,
        confirmationToken: res.confirmationToken || null,
        actionPlan: res.actionPlan || null,
        candidates: res.candidates || [],
        runId: res.runId,
      },
    })
  } catch (e) {
    ElMessage.error('Agent 调用失败: ' + (e.message || ''))
  } finally {
    sending.value = false
    await scroll()
  }
}

async function confirm(m) {
  const token = m.meta.confirmationToken
  const runId = m.meta.runId
  if (!token) return
  confirming.value = true
  try {
    const { data } = await agentApi.post('/confirm', {
      sessionId: sessionId.value,
      runId,
      confirmationToken: token,
    })
    const res = data?.data ?? data
    m.meta.confirmationToken = null
    messages.value.push({
      role: 'assistant',
      content: res.reply || '操作已完成',
      meta: { state: res.state, intents: res.intents || [], applicationNo: res.applicationNo },
    })
  } catch (e) {
    ElMessage.error('确认执行失败: ' + (e.message || ''))
  } finally {
    confirming.value = false
    await scroll()
  }
}

function cancelConfirm(m) {
  m.meta.confirmationToken = null
  messages.value.push({ role: 'assistant', content: '已取消该操作。' })
}
</script>

<style scoped>
.agent-page { padding: 16px; }
.header { display: flex; align-items: center; gap: 12px; }
.title { font-weight: 600; color: #303133; }
.agent-card { height: calc(100vh - 120px); display: flex; flex-direction: column; }
.agent-body { flex: 1; display: flex; flex-direction: column; min-height: 0; }
.messages { flex: 1; overflow: auto; padding: 8px; display: flex; flex-direction: column; gap: 14px; }
.turn { display: flex; gap: 10px; }
.turn.user { flex-direction: row-reverse; }
.bubble { max-width: 70%; background: #f5f7fa; border-radius: 8px; padding: 10px 12px; }
.turn.user .bubble { background: #ecf5ff; }
.reply { font-size: 14px; line-height: 1.6; white-space: pre-wrap; }
.meta { margin-top: 6px; display: flex; flex-wrap: wrap; gap: 6px; align-items: center; }
.app-no { font-size: 12px; color: #67c23a; }
.confirm-box { margin-top: 8px; border: 1px dashed #e6a23c; border-radius: 6px; padding: 8px; }
.plan-title { font-size: 12px; color: #e6a23c; margin-bottom: 4px; }
.plan pre { background: #fff; padding: 8px; border-radius: 4px; font-size: 12px; max-height: 160px; overflow: auto; }
.candidates { margin-top: 6px; display: flex; gap: 6px; flex-wrap: wrap; }
.confirm-actions { margin-top: 8px; }
.handoff { margin: 8px 0; }
.input-bar { display: flex; gap: 10px; align-items: flex-end; padding-top: 10px; border-top: 1px solid #ebeef5; }
.input-bar .el-button { flex-shrink: 0; }
</style>
