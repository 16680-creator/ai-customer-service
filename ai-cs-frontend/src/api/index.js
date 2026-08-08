import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, logout } from '../utils/auth'

// 所有请求统一走网关（网关负责鉴权 + 路由转发到各微服务）
// 开发/本地：网关地址 http://localhost:8080，可用 VITE_GATEWAY 环境变量覆盖
const GATEWAY = import.meta.env.VITE_GATEWAY || 'http://localhost:8080'
const BASE_URL = `${GATEWAY}/api`

// 各服务的 API 前缀（网关 stripPrefix(1) 后对应各服务 Controller 路径）
const SERVICES = {
  user: `${BASE_URL}/user`,
  chat: `${BASE_URL}/chat`,
  knowledge: `${BASE_URL}/knowledge`,
  message: `${BASE_URL}/message`,
  notify: `${BASE_URL}/notify`,
  search: `${BASE_URL}/search`,
  order: `${BASE_URL}/order`,
  cart: `${BASE_URL}/cart`,
  product: `${BASE_URL}/product`,
  pay: `${BASE_URL}/pay`,
  rag: `${BASE_URL}/rag`,
}

function redirectToLogin() {
  // 避免在登录页重复跳转
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

function createClient(baseURL) {
  const client = axios.create({ baseURL, timeout: 30000 })

  // 请求拦截器：自动附加 JWT Token
  client.interceptors.request.use((config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  })

  // 响应拦截器：401 未认证 -> 清除登录态并跳转登录页
  client.interceptors.response.use(
    (response) => response,
    (error) => {
      if (error.response && error.response.status === 401) {
        logout()
        ElMessage.warning(error.response.data?.message || '登录已过期，请重新登录')
        redirectToLogin()
      }
      return Promise.reject(error)
    }
  )

  return client
}

export const userApi = createClient(SERVICES.user)
export const chatApi = createClient(SERVICES.chat)
export const knowledgeApi = createClient(SERVICES.knowledge)
export const messageApi = createClient(SERVICES.message)
export const notifyApi = createClient(SERVICES.notify)
export const searchApi = createClient(SERVICES.search)
export const orderApi = createClient(SERVICES.order)
export const cartApi = createClient(SERVICES.cart)
export const productApi = createClient(SERVICES.product)
export const payApi = createClient(SERVICES.pay)
export const ragApi = createClient(SERVICES.rag)

export default SERVICES
