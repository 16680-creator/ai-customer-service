<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <el-icon :size="42" color="#409eff"><ChatDotRound /></el-icon>
        <h1>AI 客服平台</h1>
        <p>登录后即可使用 AI 对话、知识库、消息等全部功能</p>
      </div>

      <el-tabs v-model="activeTab" class="login-tabs">
        <!-- 账号密码登录 -->
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" size="large" @keyup.enter="handleLogin">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="用户名" :prefix-icon="User" clearable />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">登 录</el-button>
            </el-form-item>
          </el-form>
          <div class="login-tip">默认管理员账号：admin / admin123</div>
        </el-tab-pane>

        <!-- 注册 -->
        <el-tab-pane label="注册" name="register">
          <el-form ref="regFormRef" :model="regForm" :rules="regRules" size="large" @keyup.enter="handleRegister">
            <el-form-item prop="username">
              <el-input v-model="regForm.username" placeholder="用户名" :prefix-icon="User" clearable />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="regForm.password" type="password" placeholder="密码（至少6位）" :prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item prop="nickname">
              <el-input v-model="regForm.nickname" placeholder="昵称（选填）" :prefix-icon="UserFilled" clearable />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" class="login-btn" :loading="regLoading" @click="handleRegister">注 册</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, User, UserFilled, Lock } from '@element-plus/icons-vue'
import { userApi } from '../api'
import { setToken, setUser } from '../utils/auth'

const route = useRoute()
const router = useRouter()

const activeTab = ref('login')
const loading = ref(false)
const regLoading = ref(false)

const loginFormRef = ref(null)
const regFormRef = ref(null)

const loginForm = reactive({ username: '', password: '' })
const regForm = reactive({ username: '', password: '', nickname: '' })

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const regRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
}

function gotoNext() {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
  router.push(redirect)
}

async function handleLogin() {
  await loginFormRef.value.validate()
  loading.value = true
  try {
    const { data } = await userApi.post('/login', {
      username: loginForm.username.trim(),
      password: loginForm.password,
    })
    if (data.code === 200 && data.data) {
      setToken(data.data.token)
      setUser({
        userId: data.data.userId,
        username: data.data.username,
        nickname: data.data.nickname,
        role: data.data.role,
      })
      ElMessage.success(`欢迎回来，${data.data.nickname || data.data.username}`)
      gotoNext()
    } else {
      ElMessage.error(data.message || '登录失败')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || e.message || '登录失败')
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  await regFormRef.value.validate()
  regLoading.value = true
  try {
    const { data } = await userApi.post('/register', {
      username: regForm.username.trim(),
      password: regForm.password,
      nickname: regForm.nickname.trim() || undefined,
    })
    if (data.code === 200) {
      ElMessage.success('注册成功，请登录')
      loginForm.username = regForm.username.trim()
      loginForm.password = ''
      activeTab.value = 'login'
    } else {
      ElMessage.error(data.message || '注册失败')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || e.message || '注册失败')
  } finally {
    regLoading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1d1e2c 0%, #2a3f5f 60%, #409eff 130%);
}

.login-card {
  width: 400px;
  padding: 40px 36px 28px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.25);
}

.login-header {
  text-align: center;
  margin-bottom: 12px;
}

.login-header h1 {
  margin: 10px 0 6px;
  font-size: 22px;
  color: #1d1e2c;
}

.login-header p {
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
}

.login-btn {
  width: 100%;
}

.login-tip {
  text-align: center;
  font-size: 12px;
  color: #c0c4cc;
}
</style>
