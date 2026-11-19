import { describe, expect, it, vi } from 'vitest'

import { streamSse } from './sseStream'

/** 构造一个把字符串按给定切片吐出的假响应（模拟浏览器把 SSE 字节任意切分） */
function fakeResponse(chunks, { ok = true, status = 200, headers = {} } = {}) {
  const encoder = new TextEncoder()
  const body = new ReadableStream({
    start(controller) {
      for (const chunk of chunks) controller.enqueue(encoder.encode(chunk))
      controller.close()
    },
  })
  return {
    ok,
    status,
    headers: new Headers(headers),
    body,
    json: async () => ({}),
  }
}

describe('streamSse：POST 流式请求 + 分帧', () => {
  it('按顺序把每个事件交给 onEvent', async () => {
    const fetchMock = vi.fn(async () =>
      fakeResponse(['data: {"content":"你"}\n\n', 'data: {"content":"好"}\n\n', 'data: {"done":true}\n\n'])
    )
    const events = []

    const result = await streamSse(
      '/api/chat/stream/sse',
      { method: 'POST', body: '{"message":"hi"}', onEvent: (event) => events.push(event.data) },
      { fetchImpl: fetchMock }
    )

    expect(events).toEqual(['{"content":"你"}', '{"content":"好"}', '{"done":true}'])
    expect(result).toMatchObject({ aborted: false, events: 3 })
  })

  it('把 method/headers/body 原样透传给 fetch（SSE 不能用 axios）', async () => {
    const fetchMock = vi.fn(async () => fakeResponse(['data: x\n\n']))

    await streamSse(
      '/api/agent/stream/sse',
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: 'Bearer jwt' },
        body: '{"sessionId":1}',
        onEvent: () => {},
      },
      { fetchImpl: fetchMock }
    )

    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/agent/stream/sse')
    expect(init.method).toBe('POST')
    expect(init.headers.Authorization).toBe('Bearer jwt')
    expect(init.body).toBe('{"sessionId":1}')
  })

  it('HTTP 非 2xx 时抛错并带上状态码（可被上层转成友好提示）', async () => {
    const fetchMock = vi.fn(async () => fakeResponse([], { ok: false, status: 502 }))

    await expect(
      streamSse('/api/chat/stream/sse', { onEvent: () => {} }, { fetchImpl: fetchMock })
    ).rejects.toThrow('HTTP 502')
  })

  it('响应没有 body（被网关吞掉）时抛错，避免静默空回复', async () => {
    const fetchMock = vi.fn(async () => ({ ok: true, status: 200, body: null }))

    await expect(
      streamSse('/api/chat/stream/sse', { onEvent: () => {} }, { fetchImpl: fetchMock })
    ).rejects.toThrow('响应无 body')
  })

  it('用户点"停止生成"时优雅结束：已收到的内容不丢，返回 aborted=true', async () => {
    const controller = new AbortController()
    const events = []
    // 真实浏览器里 abort 会让 reader.read() 抛 AbortError，这里如实模拟
    const fetchMock = vi.fn(async (url, init) => {
      const encoder = new TextEncoder()
      const body = new ReadableStream({
        start(streamController) {
          streamController.enqueue(encoder.encode('data: {"content":"已生成一半"}\n\n'))
          // 之后不再 close，模拟服务端还在持续输出
          init.signal?.addEventListener('abort', () => {
            const error = new Error('The operation was aborted.')
            error.name = 'AbortError'
            streamController.error(error)
          })
        },
      })
      return { ok: true, status: 200, headers: new Headers(), body }
    })

    const promise = streamSse(
      '/api/chat/stream/sse',
      { onEvent: (event) => events.push(event.data), signal: controller.signal },
      { fetchImpl: fetchMock }
    )

    // 等第一个事件落地后再中止
    await vi.waitFor(() => expect(events).toHaveLength(1))
    controller.abort()

    const result = await promise

    expect(result.aborted).toBe(true)
    expect(events).toEqual(['{"content":"已生成一半"}'])
  })

  it('上游中断（非用户主动）继续抛出，交给上层区分"断线"与"业务错误"', async () => {
    const fetchMock = vi.fn(async () => {
      const body = new ReadableStream({
        start(controller) {
          controller.error(new Error('network down'))
        },
      })
      return { ok: true, status: 200, headers: new Headers(), body }
    })

    await expect(
      streamSse('/api/chat/stream/sse', { onEvent: () => {} }, { fetchImpl: fetchMock })
    ).rejects.toThrow('network down')
  })

  it('流结束但最后一个事件没补空行时，flush 仍派发（丢最后一段是最常见的流式 bug）', async () => {
    const fetchMock = vi.fn(async () => fakeResponse(['data: {"content":"最后一段"}']))
    const events = []

    await streamSse('/api/chat/stream/sse', { onEvent: (event) => events.push(event.data) }, { fetchImpl: fetchMock })

    expect(events).toEqual(['{"content":"最后一段"}'])
  })
})
