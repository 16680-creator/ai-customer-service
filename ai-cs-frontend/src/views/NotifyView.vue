<template>
  <div class="notify-view">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span style="font-weight: 600">发送通知</span></template>
          <el-form label-width="80px">
            <el-form-item label="用户ID">
              <el-input v-model="notifyForm.userId" placeholder="接收用户ID" />
            </el-form-item>
            <el-form-item label="消息内容">
              <el-input v-model="notifyForm.message" type="textarea" :rows="4" placeholder="通知内容" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="sendNotify" :icon="Promotion">发送</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span style="font-weight: 600">广播通知</span></template>
          <el-form label-width="80px">
            <el-form-item label="消息内容">
              <el-input v-model="broadcastMsg" type="textarea" :rows="4" placeholder="广播通知内容（发送给所有在线用户）" />
            </el-form-item>
            <el-form-item>
              <el-button type="warning" @click="broadcast" :icon="Bell">广播</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span style="font-weight: 600">在线用户</span></template>
          <div style="text-align: center; padding: 20px">
            <el-statistic title="当前在线用户数" :value="onlineCount">
              <template #prefix><el-icon><User /></el-icon></template>
            </el-statistic>
            <el-button type="primary" style="margin-top: 20px" @click="fetchOnlineCount" :icon="Refresh">刷新</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header><span style="font-weight: 600">WebSocket 通知 (实时)</span></template>
          <el-alert title="通知服务使用 WebSocket 推送，连接后实时接收通知" type="info" :closable="false" style="margin-bottom: 16px" />
          <el-button type="success" @click="connectWs" :icon="Connection">连接 WebSocket</el-button>
          <el-button type="danger" @click="disconnectWs" :icon="SwitchButton">断开</el-button>
          <el-tag :type="wsConnected ? 'success' : 'danger'" style="margin-left: 10px">
            {{ wsConnected ? '已连接' : '未连接' }}
          </el-tag>
          <div class="notify-log" v-if="notifyLogs.length > 0">
            <div v-for="(log, i) in notifyLogs" :key="i" class="log-item">
              <el-tag size="small" type="info">{{ log.time }}</el-tag>
              <span>{{ log.message }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { notifyApi } from '../api'
import { ElMessage } from 'element-plus'
import { Promotion, Bell, User, Refresh, Connection, SwitchButton } from '@element-plus/icons-vue'

const notifyForm = ref({ userId: '', message: '' })
const broadcastMsg = ref('')
const onlineCount = ref(0)
const wsConnected = ref(false)
const notifyLogs = ref([])
let ws = null

async function sendNotify() {
  try {
    await notifyApi.post('/api/notify/send', null, { params: notifyForm.value })
    ElMessage.success('通知发送成功')
    notifyForm.value.message = ''
  } catch (e) {
    ElMessage.error('发送失败: ' + (e.response?.data?.message || e.message))
  }
}

async function broadcast() {
  try {
    await notifyApi.post('/api/notify/broadcast', null, { params: { message: broadcastMsg.value } })
    ElMessage.success('广播发送成功')
    broadcastMsg.value = ''
  } catch (e) {
    ElMessage.error('广播失败: ' + (e.response?.data?.message || e.message))
  }
}

async function fetchOnlineCount() {
  try {
    const res = await notifyApi.get('/api/notify/online')
    onlineCount.value = res.data?.data ?? 0
  } catch (e) {
    ElMessage.error('获取在线用户数失败')
  }
}

function connectWs() {
  try {
    ws = new WebSocket('ws://localhost:8085/ws/notify')
    ws.onopen = () => {
      wsConnected.value = true
      addLog('WebSocket 已连接')
    }
    ws.onmessage = (e) => {
      addLog('收到通知: ' + e.data)
    }
    ws.onclose = () => {
      wsConnected.value = false
      addLog('WebSocket 已断开')
    }
  } catch (e) {
    ElMessage.error('WebSocket 连接失败')
  }
}

function disconnectWs() {
  if (ws) { ws.close(); ws = null }
}

function addLog(message) {
  notifyLogs.value.unshift({ time: new Date().toLocaleTimeString(), message })
  if (notifyLogs.value.length > 50) notifyLogs.value.pop()
}

fetchOnlineCount()
</script>

<style scoped>
.notify-log { margin-top: 16px; max-height: 300px; overflow-y: auto; display: flex; flex-direction: column; gap: 8px; }
.log-item { display: flex; align-items: center; gap: 10px; padding: 6px 10px; background: #fafafa; border-radius: 6px; font-size: 13px; }
</style>
