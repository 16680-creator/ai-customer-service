import axios from 'axios'

// 各微服务 base URL（开发时直连各服务，生产走 Gateway 8080）
const SERVICES = {
  chat: 'http://localhost:8083',
  user: 'http://localhost:8081',
  knowledge: 'http://localhost:8082',
  message: 'http://localhost:8084',
  notify: 'http://localhost:8085',
  search: 'http://localhost:8086',
}

function createClient(baseURL) {
  return axios.create({ baseURL, timeout: 30000 })
}

export const chatApi = createClient(SERVICES.chat)
export const userApi = createClient(SERVICES.user)
export const knowledgeApi = createClient(SERVICES.knowledge)
export const messageApi = createClient(SERVICES.message)
export const notifyApi = createClient(SERVICES.notify)
export const searchApi = createClient(SERVICES.search)

export default SERVICES
