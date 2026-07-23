<template>
  <div class="message-view">
    <el-row :gutter="20">
      <el-col :span="10">
        <el-card shadow="hover">
          <template #header><span style="font-weight: 600">创建会话</span></template>
          <el-form :inline="true">
            <el-form-item label="用户ID">
              <el-input-number v-model="userId" :min="1" />
            </el-form-item>
            <el-form-item label="标题">
              <el-input v-model="sessionTitle" placeholder="会话标题" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="createSession" :icon="Plus">创建</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="hover" style="margin-top: 20px">
          <template #header><span style="font-weight: 600">发送消息 (RocketMQ)</span></template>
          <el-form :model="msgForm" label-width="80px">
            <el-form-item label="会话ID">
              <el-input v-model="msgForm.sessionId" placeholder="会话ID" />
            </el-form-item>
            <el-form-item label="内容">
              <el-input v-model="msgForm.content" type="textarea" :rows="3" placeholder="消息内容" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="sendMessage" :icon="Promotion">发送</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="14">
        <el-card shadow="hover">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span style="font-weight: 600">会话列表</span>
              <el-button size="small" @click="fetchSessions" :icon="Refresh">刷新</el-button>
            </div>
          </template>
          <el-table :data="sessions" border stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="title" label="标题" />
            <el-table-column prop="userId" label="用户ID" width="80" />
            <el-table-column prop="createTime" label="创建时间" width="180" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="viewMessages(row.id)">查看消息</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card shadow="hover" style="margin-top: 20px" v-if="selectedSessionMessages.length > 0">
          <template #header><span style="font-weight: 600">会话消息</span></template>
          <div class="msg-list">
            <div v-for="msg in selectedSessionMessages" :key="msg.id" class="msg-item">
              <el-tag :type="msg.role === 'user' ? '' : 'success'" size="small">{{ msg.role || '消息' }}</el-tag>
              <span>{{ msg.content }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { messageApi } from '../api'
import { ElMessage } from 'element-plus'
import { Plus, Promotion, Refresh } from '@element-plus/icons-vue'

const userId = ref(1)
const sessionTitle = ref('新会话')
const sessions = ref([])
const selectedSessionMessages = ref([])
const msgForm = ref({ sessionId: '', content: '', role: 'user' })

async function createSession() {
  try {
    const res = await messageApi.post('/api/message/session', null, { params: { userId: userId.value, title: sessionTitle.value } })
    ElMessage.success('会话创建成功')
    fetchSessions()
  } catch (e) {
    ElMessage.error('创建失败: ' + (e.response?.data?.message || e.message))
  }
}

async function sendMessage() {
  try {
    await messageApi.post('/api/message/send', msgForm.value)
    ElMessage.success('消息已发送')
    msgForm.value.content = ''
  } catch (e) {
    ElMessage.error('发送失败: ' + (e.response?.data?.message || e.message))
  }
}

async function fetchSessions() {
  try {
    const res = await messageApi.get('/api/message/sessions', { params: { userId: userId.value } })
    sessions.value = res.data?.data || []
  } catch (e) {
    ElMessage.error('加载会话列表失败: ' + (e.response?.data?.message || e.message))
  }
}

async function viewMessages(sessionId) {
  try {
    const res = await messageApi.get(`/api/message/session/${sessionId}/messages`)
    selectedSessionMessages.value = res.data?.data || []
  } catch (e) {
    ElMessage.error('加载消息失败: ' + (e.response?.data?.message || e.message))
  }
}
</script>

<style scoped>
.msg-list { display: flex; flex-direction: column; gap: 10px; max-height: 400px; overflow-y: auto; }
.msg-item { display: flex; align-items: center; gap: 10px; padding: 8px 12px; background: #fafafa; border-radius: 6px; }
</style>
