import { describe, expect, it, vi } from 'vitest'

import { createSseParser } from './sse'

describe('createSseParser：SSE 分帧', () => {
  it('把一次完整事件在空行处切分并交给回调', () => {
    const events = []
    const parser = createSseParser((event) => events.push(event))

    parser.push('data: {"content":"你"}\n\n')

    expect(events).toHaveLength(1)
    expect(events[0].data).toBe('{"content":"你"}')
  })

  it('chunk 边界把一个事件劈成两半时不丢数据', () => {
    const events = []
    const parser = createSseParser((event) => events.push(event))

    // 后端每 token 一次 flush，网络层可能任意切分：这里故意切在 JSON 中间
    parser.push('data: {"content":"你')
    expect(events).toHaveLength(0)

    parser.push('好"}\n\n')

    expect(events).toHaveLength(1)
    expect(events[0].data).toBe('{"content":"你好"}')
  })

  it('兼容 CRLF 换行（部分反向代理会把 \\n 改写成 \\r\\n）', () => {
    const events = []
    const parser = createSseParser((event) => events.push(event))

    parser.push('data: {"done":true}\r\n\r\n')

    expect(events).toHaveLength(1)
    expect(events[0].data).toBe('{"done":true}')
  })

  it('CRLF 被 chunk 边界劈开时仍能识别事件结束', () => {
    const events = []
    const parser = createSseParser((event) => events.push(event))

    parser.push('data: {"done":true}\r\n\r')
    expect(events).toHaveLength(0)

    parser.push('\n')

    expect(events).toHaveLength(1)
    expect(events[0].data).toBe('{"done":true}')
  })

  it('同一个事件里的多行 data 按规范用 \\n 拼接', () => {
    const events = []
    const parser = createSseParser((event) => events.push(event))

    parser.push('data: line1\ndata: line2\n\n')

    expect(events[0].data).toBe('line1\nline2')
  })

  it('忽略注释行，并保留 event/id 字段', () => {
    const events = []
    const parser = createSseParser((event) => events.push(event))

    parser.push(': keep-alive\nevent: message\nid: 7\ndata: {"step":"检索"}\n\n')

    expect(events).toHaveLength(1)
    expect(events[0]).toEqual({ data: '{"step":"检索"}', event: 'message', id: '7' })
  })

  it('一帧里塞多个事件时逐个回调（批量 flush 场景）', () => {
    const onEvent = vi.fn()
    const parser = createSseParser(onEvent)

    parser.push('data: a\n\ndata: b\n\ndata: c\n\n')

    expect(onEvent).toHaveBeenCalledTimes(3)
    expect(onEvent.mock.calls.map(([event]) => event.data)).toEqual(['a', 'b', 'c'])
  })

  it('流结束时残留半帧通过 flush 交出（服务端未补空行就断开）', () => {
    const events = []
    const parser = createSseParser((event) => events.push(event))

    parser.push('data: {"content":"尾巴"}')

    expect(parser.flush()).toBe(1)
    expect(events[0].data).toBe('{"content":"尾巴"}')
  })

  it('flush 在没有残留时返回 0，重复调用不重复派发', () => {
    const onEvent = vi.fn()
    const parser = createSseParser(onEvent)

    parser.push('data: {"done":true}\n\n')

    expect(parser.flush()).toBe(0)
    expect(onEvent).toHaveBeenCalledTimes(1)
  })

  it('只含注释的残留帧不会被当成数据事件', () => {
    const onEvent = vi.fn()
    const parser = createSseParser(onEvent)

    parser.push(': ping\n: ping\n\n')

    expect(onEvent).not.toHaveBeenCalled()
  })
})
