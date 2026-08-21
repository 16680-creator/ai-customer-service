<template>
  <div class="dashboard">
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6" v-for="item in stats" :key="item.label">
        <el-card shadow="hover" class="stat-card" :body-style="{ padding: '24px' }">
          <div class="stat-content">
            <div>
              <div class="stat-value">{{ item.value }}</div>
              <div class="stat-label">{{ item.label }}</div>
            </div>
            <el-icon :size="48" :color="item.color"><component :is="item.icon" /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span style="font-weight: 600">微服务状态</span>
              <div class="header-right">
                <span class="refresh-tip">每 15 秒自动刷新 · 上次更新 {{ lastUpdate }}</span>
                <el-button size="small" :icon="Refresh" @click="loadHealth" :loading="loadingHealth">刷新</el-button>
              </div>
            </div>
          </template>
          <div class="arch-list">
            <div v-for="svc in services" :key="svc.key" class="arch-item">
              <el-icon :size="20" :color="svc.status === 'UP' ? '#67c23a' : '#f56c6c'">
                <component :is="svc.icon || 'Monitor'" />
              </el-icon>
              <span class="arch-name">{{ svc.name }}</span>
              <el-tag :type="svc.status === 'UP' ? 'success' : 'danger'" size="small" round>
                {{ svc.status === 'UP' ? '运行中' : '离线' }}
              </el-tag>
              <span class="arch-port">:{{ svc.port }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span style="font-weight: 600">功能入口</span></template>
          <div class="quick-actions">
            <el-button type="primary" @click="$router.push('/chat')" :icon="ChatLineSquare">AI 对话</el-button>
            <el-button type="primary" plain @click="$router.push('/rag-kb')" :icon="Files">向量知识库</el-button>
            <el-button type="success" @click="$router.push('/knowledge')" :icon="Collection">知识库管理</el-button>
            <el-button type="warning" @click="$router.push('/cart')" :icon="ShoppingCart">购物车</el-button>
            <el-button type="success" plain @click="$router.push('/shop')" :icon="ShoppingCart">商品商城</el-button>
            <el-button type="warning" plain @click="$router.push('/order')" :icon="Tickets">订单管理</el-button>
            <el-button type="info" @click="$router.push('/product')" :icon="Goods">商品管理</el-button>
            <el-button type="info" plain @click="$router.push('/search')" :icon="Search">全文搜索</el-button>
            <el-button type="info" plain @click="$router.push('/mq')" :icon="Connection">MQ 调度</el-button>
            <el-button type="info" plain @click="$router.push('/message')" :icon="Message">消息管理</el-button>
            <el-button type="info" plain @click="$router.push('/notify')" :icon="Bell">通知中心</el-button>
            <el-button type="info" plain @click="$router.push('/user')" :icon="User">用户管理</el-button>
            <el-button type="primary" plain @click="$router.push('/feedback')" :icon="Star">对话反馈</el-button>
            <el-button type="warning" plain @click="$router.push('/trace')" :icon="Connection">链路追踪</el-button>
            <el-button type="success" plain @click="$router.push('/prompts')" :icon="Document">Prompt 管理</el-button>
            <el-button type="danger" plain @click="$router.push('/agent')" :icon="Cpu">售后 Agent</el-button>
            <el-button type="info" plain @click="$router.push('/graph')" :icon="Share">知识图谱</el-button>
            <el-button type="info" plain @click="$router.push('/rag-eval')" :icon="DataAnalysis">RAG 评估</el-button>
          </div>

          <el-divider />

          <div class="feature-tips">
            <div class="tip-item"><el-icon color="#409eff"><ChatDotRound /></el-icon> AI 对话支持「RAG 知识库模式」，知识库标识填 <b>knowledge</b> 可检索知识库文档</div>
            <div class="tip-item"><el-icon color="#e6a23c"><Tickets /></el-icon> 交易链路：购物车 → 结算 → 下单 → 订单管理 → 支付</div>
            <div class="tip-item"><el-icon color="#67c23a"><Goods /></el-icon> 商品支持图片上传、相似检索、编辑、删除、分类管理</div>
            <div class="tip-item"><el-icon color="#9254de"><Cpu /></el-icon> 售后 Agent 编排：意图识别 → 待确认写操作 → 执行，支持转人工</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header><span style="font-weight: 600">技术栈</span></template>
          <div class="tech-tags">
            <el-tag v-for="t in techs" :key="t" size="large" effect="plain" style="margin: 4px">{{ t }}</el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import {
  ChatLineSquare, Collection, User, Search, ShoppingCart, Tickets, Goods,
  Message, Bell, Files, Refresh, ChatDotRound, Monitor, Connection,
  Star, Document, Cpu, Share, DataAnalysis
} from '@element-plus/icons-vue'
import axios from 'axios'
import { notifyApi, messageApi } from '../api'
import { getUser } from '../utils/auth'

const services = ref([])
const loadingHealth = ref(false)
const lastUpdate = ref('-')
const onlineCount = ref('-')
const sessionCount = ref('-')

// 实时统计
const stats = computed(() => [
  { label: '微服务在线', value: `${services.value.filter(s => s.status === 'UP').length}/${services.value.length || 9}`, icon: 'Monitor', color: '#409eff' },
  { label: 'AI 模型', value: 'DeepSeek', icon: 'Cpu', color: '#67c23a' },
  { label: '对话会话', value: sessionCount.value, icon: 'ChatDotSquare', color: '#e6a23c' },
  { label: '在线用户', value: onlineCount.value, icon: 'UserFilled', color: '#f56c6c' },
])

// 从后端健康接口获取服务状态
async function loadHealth() {
  loadingHealth.value = true
  try {
    const { data } = await axios.get('http://localhost:8080/api/health', { timeout: 10000 })
    services.value = Array.isArray(data) ? data : (data?.data || [])
    const now = new Date()
    lastUpdate.value = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`
  } catch {
    services.value = []
    lastUpdate.value = '获取失败'
  } finally {
    loadingHealth.value = false
  }
}

// 在线用户数（notify 服务）
async function loadOnline() {
  try {
    const { data } = await notifyApi.get('/online')
    if (data.code === 200) onlineCount.value = data.data ?? 0
  } catch {
    onlineCount.value = '-'
  }
}

// 对话会话数（message 服务，当前用户）
async function loadSessions() {
  try {
    const uid = getUser()?.userId || localStorage.getItem('userId') || 1
    const { data } = await messageApi.get('/sessions', { params: { userId: uid } })
    if (data.code === 200) sessionCount.value = (data.data || []).length
  } catch {
    sessionCount.value = '-'
  }
}

let timer = null
onMounted(() => {
  loadHealth()
  loadOnline()
  loadSessions()
  timer = setInterval(() => {
    loadHealth()
    loadOnline()
    loadSessions()
  }, 15000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

const techs = [
  'Spring Boot 3.2.5', 'Spring Cloud 2023', 'Spring AI 1.1.4',
  'Nacos 2.3', 'MySQL 8.0', 'Redis 7', 'Elasticsearch 8.12',
  'RocketMQ 5.1', 'Chroma 向量库', 'OpenFeign', 'MyBatis-Plus',
  'Vue 3', 'Element Plus', 'Vite'
]
</script>

<style scoped>
.stat-card { border-radius: 12px; }
.stat-content { display: flex; justify-content: space-between; align-items: center; }
.stat-value { font-size: 32px; font-weight: 700; color: #1d1e2c; }
.stat-label { font-size: 14px; color: #909399; margin-top: 4px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-right { display: flex; align-items: center; gap: 10px; }
.refresh-tip { font-size: 12px; color: #909399; }
.arch-list { display: flex; flex-direction: column; gap: 10px; max-height: 460px; overflow-y: auto; }
.arch-item { display: flex; align-items: center; gap: 10px; padding: 8px 12px; background: #fafafa; border-radius: 8px; }
.arch-name { font-weight: 500; flex: 1; }
.arch-port { color: #909399; font-size: 13px; font-family: monospace; }
.quick-actions { display: flex; flex-wrap: wrap; gap: 10px; }
.feature-tips { display: flex; flex-direction: column; gap: 10px; }
.tip-item { display: flex; align-items: flex-start; gap: 8px; font-size: 13px; color: #606266; line-height: 1.6; }
.tech-tags { display: flex; flex-wrap: wrap; gap: 4px; }
</style>
