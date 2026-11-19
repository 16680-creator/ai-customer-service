/**
 * SSE（Server-Sent Events）逐事件解析器。
 *
 * 为什么不用 EventSource：本项目的流式接口是 POST + JSON body + Authorization 头，
 * EventSource 只支持 GET 且无法自定义请求头，所以统一走 fetch + ReadableStream，
 * 再交给这里做分帧（见 ChatView 的"停止生成"与 AgentView 的步骤流）。
 *
 * 分帧规则（W3C EventSource 规范子集）：
 * - 空行（\n\n 或 \r\n\r\n）结束一个事件；
 * - 一个事件里可有多行 data，按 \n 拼接；
 * - 以 ':' 开头的是注释（心跳 keep-alive），丢弃；
 * - 只处理 data / event / id 三个字段（retry 对 fetch 流无意义）。
 *
 * @param {(event: {data: string, event?: string, id?: string}) => void} onEvent
 */
export function createSseParser(onEvent) {
  let buffer = ''

  function dispatch(rawEvent) {
    const dataLines = []
    let event
    let id

    for (const line of rawEvent.split(/\r?\n/)) {
      if (!line || line.startsWith(':')) continue
      const colon = line.indexOf(':')
      const field = colon === -1 ? line : line.slice(0, colon)
      let value = colon === -1 ? '' : line.slice(colon + 1)
      if (value.startsWith(' ')) value = value.slice(1)

      if (field === 'data') dataLines.push(value)
      else if (field === 'event') event = value
      else if (field === 'id') id = value
    }

    // 纯注释/纯空帧不产生事件
    if (dataLines.length === 0 && event === undefined && id === undefined) return

    const payload = { data: dataLines.join('\n') }
    if (event !== undefined) payload.event = event
    if (id !== undefined) payload.id = id
    onEvent(payload)
  }

  /** 找出缓冲区里最早出现的事件分隔符 */
  function nextBoundary() {
    const lf = buffer.indexOf('\n\n')
    const crlf = buffer.indexOf('\r\n\r\n')
    if (lf === -1 && crlf === -1) return null
    if (crlf === -1 || (lf !== -1 && lf < crlf)) return { index: lf, length: 2 }
    return { index: crlf, length: 4 }
  }

  return {
    /** 追加一段已解码文本，必要时派发完整事件 */
    push(chunk) {
      buffer += chunk
      let boundary = nextBoundary()
      while (boundary) {
        dispatch(buffer.slice(0, boundary.index))
        buffer = buffer.slice(boundary.index + boundary.length)
        boundary = nextBoundary()
      }
    },
    /**
     * 流结束（reader done / 用户点"停止生成"）时调用：
     * 服务端有时来不及补最后一个空行，留下半帧也要派发，否则会丢最后一段回答。
     * @returns {number} 派发的事件数（0 表示没有残留）
     */
    flush() {
      const rest = buffer.trim()
      buffer = ''
      if (!rest) return 0
      dispatch(rest)
      return 1
    },
  }
}
