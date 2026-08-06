<template>
  <!-- 登录页：全屏展示，不带侧边栏 -->
  <router-view v-if="isLoginPage" />

  <el-container v-else class="app-container">
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
        <el-menu-item index="/product">
          <el-icon><Goods /></el-icon>
          <span>商品图片检索</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="app-header">
        <div class="header-left">
          <span class="page-title">{{ currentPageTitle }}</span>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="30" class="user-avatar">{{ avatarText }}</el-avatar>
              <span class="user-name">{{ displayName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>角色：{{ userRole }}</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { computed, ref, watch } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Goods, ArrowDown } from '@element-plus/icons-vue'
import { getUser, logout } from './utils/auth'

const route = useRoute()
const router = useRouter()

const isLoginPage = computed(() => route.path === '/login')

const currentUser = ref(getUser() || {})
// 路由变化时重新读取登录信息（登录/退出后 localStorage 已更新）
watch(
  () => route.path,
  () => {
    currentUser.value = getUser() || {}
  }
)
const displayName = computed(() => currentUser.value.nickname || currentUser.value.username || '未登录')
const userRole = computed(() => currentUser.value.role || '-')
const avatarText = computed(() => (displayName.value || '?').charAt(0).toUpperCase())

async function handleCommand(command) {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
      logout()
      ElMessage.success('已退出登录')
      router.push('/login')
    } catch {
      // 用户取消
    }
  }
}

const titleMap = {
  '/': '首页总览',
  '/chat': 'AI 对话',
  '/user': '用户管理',
  '/knowledge': '知识库',
  '/message': '消息管理',
  '/notify': '通知中心',
  '/search': '全文搜索',
  '/product': '商品图片检索',
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

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.user-avatar {
  background-color: #409eff;
  color: #fff;
  font-size: 14px;
}

.user-name {
  font-size: 14px;
  color: #1d1e2c;
}

.app-main {
  background-color: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}
</style>
