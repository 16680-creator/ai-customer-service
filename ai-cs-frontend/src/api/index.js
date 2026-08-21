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
  observability: `${BASE_URL}/observability`,
  prompts: `${BASE_URL}/prompts`,
  agent: `${BASE_URL}/agent`,
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
export const observabilityApi = createClient(SERVICES.observability)
export const promptApi = createClient(SERVICES.prompts)
export const agentApi = createClient(SERVICES.agent)

// ===== RAG 进阶（003-rag-advanced-features）=====
export const chartApi = {
  /** 问数图表：查询结果 -> 自然语言结论 + ECharts 配置 */
  generate: (question, rows) => chatApi.post('/chart', { question, rows }),
}

// ===== 用户反馈（ChatFeedbackController）=====
export const feedbackApi = {
  /** 提交反馈（点赞/点踩/评分），payload: { sessionId, feedbackType: 'LIKE'|'DISLIKE', score?, comment?, requestId? } */
  submit: (payload) => chatApi.post('/feedback', payload),
  /** 查询反馈（按 requestId，可选） */
  list: (requestId) => chatApi.get('/feedback', { params: requestId ? { requestId } : {} }),
}

// ===== 可观测性（ObservabilityController）=====
export const observabilityApiWrappers = {
  /** 按 requestId 查询一次请求的完整调用链（spansJson 含节点/耗时/费用） */
  getTrace: (requestId) => observabilityApi.get('/traces/{requestId}'.replace('{requestId}', requestId)),
}

// ===== Prompt 管理（PromptController，配置化版本管理）=====
export const promptApiWrappers = {
  /** 列出全部 scenario 的生效版本 */
  list: () => promptApi.get('/'),
  /** 列出某场景的所有版本及内容长度摘要 */
  listVersions: (scenario) => promptApi.get('/{scenario}'.replace('{scenario}', scenario)),
  /** 热切换某场景的生效版本（回滚/灰度收敛） */
  setActive: (scenario, version) =>
    promptApi.post('/{scenario}/active'.replace('{scenario}', scenario), null, { params: { version } }),
}

// ===== Agent 编排（AgentController）=====
export const agentApiWrappers = {
  /** 同步对话 */
  chat: (payload) => agentApi.post('/chat', payload),
  /** 健康检查 */
  health: () => agentApi.get('/health'),
  /** 图调试（返回节点/边，供前端可视化） */
  graphDebug: () => agentApi.get('/graph/debug'),
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
