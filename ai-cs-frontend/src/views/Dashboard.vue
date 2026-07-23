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
            <span style="font-weight: 600">系统架构</span>
          </template>
          <div class="arch-list">
            <div v-for="svc in services" :key="svc.name" class="arch-item">
              <el-icon :size="20" :color="svc.color"><component :is="svc.icon" /></el-icon>
              <span class="arch-name">{{ svc.name }}</span>
              <el-tag :type="svc.status === '运行中' ? 'success' : 'info'" size="small" round>{{ svc.status }}</el-tag>
              <span class="arch-port">:{{ svc.port }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span style="font-weight: 600">快速操作</span>
          </template>
          <div class="quick-actions">
            <el-button type="primary" @click="$router.push('/chat')" :icon="ChatLineSquare">开始 AI 对话</el-button>
            <el-button type="success" @click="$router.push('/knowledge')" :icon="Collection">管理知识库</el-button>
            <el-button type="warning" @click="$router.push('/user')" :icon="User">用户管理</el-button>
            <el-button type="info" @click="$router.push('/search')" :icon="Search">全文搜索</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <span style="font-weight: 600">技术栈</span>
          </template>
          <div class="tech-tags">
            <el-tag v-for="t in techs" :key="t" size="large" effect="plain" style="margin: 4px">{{ t }}</el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ChatLineSquare, Collection, User, Search } from '@element-plus/icons-vue'

const stats = ref([
  { label: '微服务数量', value: '7', icon: 'Monitor', color: '#409eff' },
  { label: 'AI 模型', value: 'MiniMax', icon: 'Cpu', color: '#67c23a' },
  { label: '对话会话', value: '0', icon: 'ChatDotSquare', color: '#e6a23c' },
  { label: '在线用户', value: '0', icon: 'UserFilled', color: '#f56c6c' },
])

const services = ref([
  { name: 'API Gateway', port: 8080, icon: 'SetUp', color: '#409eff', status: '运行中' },
  { name: 'Chat 对话服务', port: 8083, icon: 'ChatDotRound', color: '#67c23a', status: '运行中' },
  { name: 'User 用户服务', port: 8081, icon: 'User', color: '#e6a23c', status: '运行中' },
  { name: 'Knowledge 知识库', port: 8082, icon: 'Collection', color: '#f56c6c', status: '运行中' },
  { name: 'Message 消息服务', port: 8084, icon: 'Message', color: '#909399', status: '运行中' },
  { name: 'Notify 通知服务', port: 8085, icon: 'Bell', color: '#b37feb', status: '运行中' },
  { name: 'Search 搜索服务', port: 8086, icon: 'Search', color: '#36cfc9', status: '运行中' },
])

const techs = [
  'Spring Boot 3.2.5', 'Spring Cloud 2023', 'Spring AI 1.0.0',
  'Nacos 2.3.2', 'MySQL 8.0', 'Redis 7', 'Elasticsearch 8.12',
  'RocketMQ 5.1.4', 'MyBatis-Plus', 'Vue 3', 'Element Plus', 'Vite'
]
</script>

<style scoped>
.stat-card { border-radius: 12px; }
.stat-content { display: flex; justify-content: space-between; align-items: center; }
.stat-value { font-size: 36px; font-weight: 700; color: #1d1e2c; }
.stat-label { font-size: 14px; color: #909399; margin-top: 4px; }
.arch-list { display: flex; flex-direction: column; gap: 12px; }
.arch-item { display: flex; align-items: center; gap: 10px; padding: 8px 12px; background: #fafafa; border-radius: 8px; }
.arch-name { font-weight: 500; flex: 1; }
.arch-port { color: #909399; font-size: 13px; font-family: monospace; }
.quick-actions { display: flex; flex-wrap: wrap; gap: 12px; }
.tech-tags { display: flex; flex-wrap: wrap; gap: 4px; }
</style>
