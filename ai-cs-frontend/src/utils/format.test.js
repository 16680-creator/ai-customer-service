import { describe, expect, it } from 'vitest'

import { formatSize, truncateMiddle } from './format'

describe('formatSize：上传/下载体积展示', () => {
  it('小于 1KB 用 B', () => {
    expect(formatSize(0)).toBe('0 B')
    expect(formatSize(1023)).toBe('1023 B')
  })

  it('按 1024 进制逐级换算并保留一位小数', () => {
    expect(formatSize(1024)).toBe('1.0 KB')
    expect(formatSize(1536)).toBe('1.5 KB')
    expect(formatSize(1024 * 1024)).toBe('1.0 MB')
    expect(formatSize(5.5 * 1024 * 1024 * 1024)).toBe('5.5 GB')
  })

  it('非法输入返回 "-" 而不是 NaN', () => {
    expect(formatSize(undefined)).toBe('-')
    expect(formatSize(-1)).toBe('-')
  })
})

describe('truncateMiddle：长哈希/订单号中间省略', () => {
  it('未超长时原样返回', () => {
    expect(truncateMiddle('abc', 10)).toBe('abc')
  })

  it('超长时保留首尾并用省略号连接', () => {
    expect(truncateMiddle('20260812215541010002', 10)).toBe('20260…0002')
  })

  it('length <= 0 时返回空串（不抛错）', () => {
    expect(truncateMiddle('abcdef', 0)).toBe('')
  })
})
