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
  mq: `${BASE_URL}/mq`,
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
export const mqApi = createClient(SERVICES.mq)
export const ragApi = createClient(SERVICES.rag)

// ===== RAG 进阶（003-rag-advanced-features）=====
export const chartApi = {
  /** 问数图表：查询结果 -> 自然语言结论 + ECharts 配置 */
  generate: (question, rows) => chatApi.post('/chart', { question, rows }),
}

export const opsApi = {
  /** 提问聚类 + 缺口分析 */
  cluster: (period, questions, gapHitRateThreshold) =>
    knowledgeApi.post('/ops/cluster', { period, questions, gapHitRateThreshold }),
  /** 收录 FAQ */
  adoptFaq: (payload) => knowledgeApi.post('/ops/faq', payload),
}

export const ragEvalApi = {
  /** 运行 RAG 评估 */
  run: (payload) => ragApi.post('/eval/run', payload),
}

export const graphApi = {
  /** 新增三元组 */
  addTriple: (payload) => ragApi.post('/graph/triple', payload),
  /** 多跳查询 */
  query: (entity, depth, knowledgeBase) =>
    ragApi.get('/graph/query', { params: { entity, depth, knowledgeBase } }),
}

// ===== 多模态图生文（004-vlm-multimodal）=====
export const visionApi = {
  /** 上传图片（返回 MinIO 图片 URL） */
  uploadImage: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return chatApi.post('/upload-image', formData)
  },
}

export default SERVICES
