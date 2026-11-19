import { describe, expect, it } from 'vitest'

import { resolveGateway } from './runtime'

describe('resolveGateway：网关地址解析', () => {
  it('未配置时返回空串，走同源相对路径（由 dev proxy / Nginx 反代）', () => {
    expect(resolveGateway({})).toBe('')
    expect(resolveGateway(undefined)).toBe('')
  })

  it('.env.production 里显式留空时保持空串，不能回退到 localhost', () => {
    // 关键回归点：旧代码用 `env.VITE_GATEWAY || 'http://localhost:8080'`，
    // 空串是 falsy，会把生产构建打回 localhost —— 上线即白屏。
    expect(resolveGateway({ VITE_GATEWAY: '' })).toBe('')
    expect(resolveGateway({ VITE_GATEWAY: '   ' })).toBe('')
  })

  it('配置了绝对地址时原样返回并去掉结尾斜杠', () => {
    expect(resolveGateway({ VITE_GATEWAY: 'https://api.example.com' })).toBe('https://api.example.com')
    expect(resolveGateway({ VITE_GATEWAY: 'https://api.example.com/' })).toBe('https://api.example.com')
  })
})
