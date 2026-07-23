<template>
  <el-container class="app-container">
    <el-aside width="220px" class="app-aside">
      <div class="logo-area">
        <el-icon :size="28" color="#409eff"><ChatDotRound /></el-icon>
        <span class="logo-text">AI 客服平台</span>
      </div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#1d1e2c"
        text-color="#a3a6ad"
        active-text-color="#409eff"
        class="side-menu"
      >
        <el-menu-item index="/">
          <el-icon><HomeFilled /></el-icon>
          <span>首页总览</span>
        </el-menu-item>
        <el-menu-item index="/chat">
          <el-icon><ChatLineSquare /></el-icon>
          <span>AI 对话</span>
        </el-menu-item>
        <el-menu-item index="/user">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/knowledge">
          <el-icon><Collection /></el-icon>
          <span>知识库</span>
        </el-menu-item>
        <el-menu-item index="/message">
          <el-icon><Message /></el-icon>
          <span>消息管理</span>
        </el-menu-item>
        <el-menu-item index="/notify">
          <el-icon><Bell /></el-icon>
          <span>通知中心</span>
        </el-menu-item>
        <el-menu-item index="/search">
          <el-icon><Search /></el-icon>
          <span>全文搜索</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="app-header">
        <div class="header-left">
          <span class="page-title">{{ currentPageTitle }}</span>
        </div>
        <div class="header-right">
          <el-tag type="success" effect="dark" round>在线</el-tag>
        </div>
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { computed } from 'vue'

const route = useRoute()

const titleMap = {
  '/': '首页总览',
  '/chat': 'AI 对话',
  '/user': '用户管理',
  '/knowledge': '知识库',
  '/message': '消息管理',
  '/notify': '通知中心',
  '/search': '全文搜索',
}

const currentPageTitle = computed(() => titleMap[route.path] || 'AI 客服平台')
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body, #app { height: 100%; width: 100%; font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif; }

.app-container { height: 100vh; }

.app-aside {
  background-color: #1d1e2c;
  overflow-y: auto;
  box-shadow: 2px 0 8px rgba(0,0,0,0.15);
}

.logo-area {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border-bottom: 1px solid rgba(255,255,255,0.08);
}

.logo-text {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 1px;
}

.side-menu {
  border-right: none;
}

.side-menu .el-menu-item {
  font-size: 14px;
  height: 50px;
  line-height: 50px;
}

.side-menu .el-menu-item:hover {
  background-color: rgba(64, 158, 255, 0.08) !important;
}

.side-menu .el-menu-item.is-active {
  background-color: rgba(64, 158, 255, 0.12) !important;
  border-right: 3px solid #409eff;
}

.app-header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
  padding: 0 24px;
  height: 64px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #1d1e2c;
}

.app-main {
  background-color: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}
</style>
