import { beforeEach, describe, expect, it } from 'vitest'

import { getRoles, getToken, getUser, hasRole, isAuthenticated, logout, setToken, setUser } from './auth'

describe('auth：登录态与角色', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('未登录时 getToken 为 null 且 isAuthenticated 为 false', () => {
    expect(getToken()).toBeNull()
    expect(isAuthenticated()).toBe(false)
  })

  it('setToken 后 isAuthenticated 为 true，logout 后恢复未登录', () => {
    setToken('jwt-abc')
    expect(getToken()).toBe('jwt-abc')
    expect(isAuthenticated()).toBe(true)

    logout()
    expect(getToken()).toBeNull()
    expect(isAuthenticated()).toBe(false)
  })

  it('getUser 遇到坏 JSON 时返回 null 而不是抛错（历史脏数据兜底）', () => {
    localStorage.setItem('aics_user', '{not-json')

    expect(getUser()).toBeNull()
  })

  it('getRoles 把后端的逗号分隔角色串拆成小写数组', () => {
    setUser({ username: 'alice', role: 'admin,agent' })

    expect(getRoles()).toEqual(['admin', 'agent'])
  })

  it('getRoles 在无用户或空角色时返回空数组', () => {
    expect(getRoles()).toEqual([])

    setUser({ username: 'bob' })
    expect(getRoles()).toEqual([])
  })

  it('hasRole 大小写不敏感，且支持多角色命中', () => {
    setUser({ username: 'alice', role: 'ADMIN,agent' })

    expect(hasRole('admin')).toBe(true)
    expect(hasRole('AGENT')).toBe(true)
    expect(hasRole('superuser')).toBe(false)
    expect(hasRole('')).toBe(false)
  })
})
