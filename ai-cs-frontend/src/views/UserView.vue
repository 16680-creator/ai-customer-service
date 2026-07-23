<template>
  <div class="user-view">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span style="font-weight: 600">用户注册</span></template>
          <el-form :model="regForm" label-width="80px">
            <el-form-item label="用户名">
              <el-input v-model="regForm.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="regForm.password" type="password" placeholder="请输入密码" show-password />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="regForm.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="regForm.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="register" :icon="Plus">注册</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span style="font-weight: 600">用户登录</span></template>
          <el-form :model="loginForm" label-width="80px">
            <el-form-item label="用户名">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="login" :icon="Key">登录</el-button>
            </el-form-item>
          </el-form>
          <el-alert v-if="token" :title="'登录成功，Token: ' + token.substring(0, 30) + '...'" type="success" show-icon :closable="false" style="margin-top: 10px; word-break: break-all" />
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span style="font-weight: 600">查询用户</span></template>
          <el-form label-width="80px">
            <el-form-item label="用户 ID">
              <el-input v-model="queryId" placeholder="请输入用户ID" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="queryUser" :icon="Search">查询</el-button>
            </el-form-item>
          </el-form>
          <el-descriptions v-if="userInfo" :column="1" border style="margin-top: 10px">
            <el-descriptions-item label="ID">{{ userInfo.id }}</el-descriptions-item>
            <el-descriptions-item label="用户名">{{ userInfo.username }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ userInfo.email }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ userInfo.phone }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { userApi } from '../api'
import { ElMessage } from 'element-plus'
import { Plus, Key, Search } from '@element-plus/icons-vue'

const regForm = ref({ username: '', password: '', email: '', phone: '' })
const loginForm = ref({ username: '', password: '' })
const queryId = ref('')
const token = ref('')
const userInfo = ref(null)

async function register() {
  try {
    await userApi.post('/user/register', regForm.value)
    ElMessage.success('注册成功')
  } catch (e) {
    ElMessage.error('注册失败: ' + (e.response?.data?.message || e.message))
  }
}

async function login() {
  try {
    const res = await userApi.post('/user/login', null, { params: { username: loginForm.value.username, password: loginForm.value.password } })
    token.value = res.data?.data || res.data
    ElMessage.success('登录成功')
  } catch (e) {
    ElMessage.error('登录失败: ' + (e.response?.data?.message || e.message))
  }
}

async function queryUser() {
  try {
    const res = await userApi.get(`/user/${queryId.value}`)
    userInfo.value = res.data?.data || res.data
    ElMessage.success('查询成功')
  } catch (e) {
    ElMessage.error('查询失败: ' + (e.response?.data?.message || e.message))
  }
}
</script>
