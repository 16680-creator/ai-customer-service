import { createSseParser } from './sse'

/**
 * 发起一个 SSE 流式请求并把每个事件回调出去。
 *
 * 为什么是 fetch 而不是 axios / EventSource：
 * - EventSource 只支持 GET、不能带 Authorization 头，本项目的流式接口是 POST + JSON body；
 * - axios 在浏览器端基于 XHR，拿不到增量 body（responseType 不支持流）。
 *
 * @param {string} url
 * @param {object} options
 * @param {string} [options.method='GET']
 * @param {Record<string,string>} [options.headers]
 * @param {string|undefined} [options.body]
 * @param {AbortSignal} [options.signal] 用户点"停止生成"时 abort
 * @param {(event: {data: string, event?: string, id?: string}) => void} options.onEvent
 * @param {{fetchImpl?: typeof fetch}} [deps] 便于单测注入假 fetch
 * @returns {Promise<{aborted: boolean, events: number}>}
 */
export async function streamSse(url, options, deps = {}) {
  const { method = 'GET', headers = {}, body, signal, onEvent } = options
  const doFetch = deps.fetchImpl ?? fetch

  const response = await doFetch(url, { method, headers, body, signal })
  if (!response.ok) throw new Error(`HTTP ${response.status}`)
  if (!response.body) throw new Error('响应无 body，无法读取流')

  let dispatched = 0
  const parser = createSseParser((event) => {
    dispatched += 1
    onEvent?.(event)
  })

  const reader = response.body.getReader()
  const decoder = new TextDecoder()

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      parser.push(decoder.decode(value, { stream: true }))
    }
  } catch (error) {
    // 用户主动停止：把已缓冲的半帧交出去后正常返回，不算异常
    if (signal?.aborted) {
      dispatched += parser.flush()
      return { aborted: true, events: dispatched }
    }
    throw error
  }

  // 服务端未补最后一个空行就 close 时，残留半帧也要派发
  dispatched += parser.flush()
  return { aborted: signal?.aborted === true, events: dispatched }
}
