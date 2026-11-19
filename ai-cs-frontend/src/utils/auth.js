/**
 * 登录态管理：Token 与用户信息存取（localStorage）
 */
const TOKEN_KEY = 'aics_token'
const USER_KEY = 'aics_user'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function getUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY)) || null
  } catch {
    return null
  }
}

export function setUser(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function isAuthenticated() {
  return !!getToken()
}

/**
 * 当前用户角色（小写数组）。
 * 后端 `LoginVO.role` 是逗号分隔串（ai-cs-user/.../HeaderAuthenticationFilter.java:37-41
 * 按 ',' 拆分并补 ROLE_ 前缀），前端只做展示/按钮级权限判断，真正的权限校验仍在后端。
 */
export function getRoles() {
  const role = getUser()?.role
  if (!role) return []
  return role
    .split(',')
    .map((item) => item.trim().toLowerCase())
    .filter(Boolean)
}

/**
 * 是否拥有某角色（大小写不敏感）。用于 v-permission 指令与菜单过滤。
 */
export function hasRole(role) {
  if (!role) return false
  return getRoles().includes(role.trim().toLowerCase())
}

/**
 * 退出登录：清空本地登录态
 */
export function logout() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}
