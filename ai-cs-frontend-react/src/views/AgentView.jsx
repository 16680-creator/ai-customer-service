import { useEffect, useRef, useState } from 'react'
import { Alert, Avatar, Button, Card, Empty, Input, Tag, message } from 'antd'
import { SendOutlined } from '@ant-design/icons'
import { GATEWAY_URL, agentApi } from '../api'
import { getToken } from '../utils/auth'
import { buildAgentConfirmRequest, buildAgentRequest, nextAgentRunId } from './agentRequest'

export default function AgentView() {
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [sending, setSending] = useState(false)
  const [confirming, setConfirming] = useState(false)
  const [healthOk, setHealthOk] = useState(true)
  const [handoff, setHandoff] = useState(null)

  const msgRef = useRef(null)
  const sendingRef = useRef(false)
  // 后端 AgentRequestDTO.sessionId 为 Long，必须传数字（不能加 'agent-' 前缀）
  const [sessionId] = useState(() => Date.now())
  const [activeRunId, setActiveRunId] = useState(null)

  useEffect(() => {
    ;(async () => {
      try {
        await agentApi.get('/health')
        setHealthOk(true)
      } catch {
        setHealthOk(false)
      }
    })()
  }, [])

  useEffect(() => {
    const el = msgRef.current
    if (el) el.scrollTop = el.scrollHeight
  }, [messages])

  function updateLastAssistant(patch) {
    setMessages((prev) => {
      if (!prev.length) return prev
      const next = prev.slice()
      next[next.length - 1] = { ...next[next.length - 1], ...patch }
      return next
    })
  }

  /** 流式对话（SSE）：fetch + ReadableStream 解析，步骤进度逐个点亮 + 回复打字机追加 */
  async function send() {
    const text = input.trim()
    if (!text || sendingRef.current) return
    setMessages((prev) => [...prev, { role: 'user', content: text }])
    setInput('')
    sendingRef.current = true
    setSending(true)

    // 预置空回复气泡，流式追加
    setMessages((prev) => [...prev, { role: 'assistant', content: '', steps: [], meta: null }])

    const steps = []
    let acc = ''
    let result = null
    let streamError = null

    try {
      const resp = await fetch(`${GATEWAY_URL}/api/agent/stream/sse`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: 'Bearer ' + (getToken() || ''),
        },
        body: JSON.stringify(buildAgentRequest(sessionId, text, activeRunId)),
      })
      if (!resp.ok || !resp.body) throw new Error('HTTP ' + resp.status)

      const reader = resp.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })

        // 按 SSE 事件分隔（空行）逐条解析
        let sep
        while ((sep = buffer.indexOf('\n\n')) >= 0) {
          const rawEvent = buffer.slice(0, sep)
          buffer = buffer.slice(sep + 2)
          for (const line of rawEvent.split('\n')) {
            if (!line.startsWith('data:')) continue
            const data = line.slice(5).trim()
            if (!data) continue
            let obj
            try {
              obj = JSON.parse(data)
            } catch {
              continue
            }
            if (obj.step !== undefined) {
              steps.push(obj.detail || obj.step)
              updateLastAssistant({ steps: [...steps] })
            }
            if (obj.content) {
              acc += obj.content
              updateLastAssistant({ content: acc })
            }
            if (obj.error) streamError = obj.error
            if (obj.done) result = obj.result
          }
        }
      }

      if (streamError) throw new Error(streamError)

      // done 事件携带完整结果：售后模板类回复无 content 流，用 result.reply 兜底；
      // 已有打字机累积文本时保留，避免重复追加
      const res = result || {}
      updateLastAssistant({
        content: acc || res.reply || '(无回复)',
        meta: {
          state: res.state,
          intents: res.intents || [],
          applicationNo: res.applicationNo,
          confirmationToken: res.confirmationToken || null,
          actionPlan: res.actionPlan || null,
          candidates: res.candidates || [],
          runId: res.runId,
        },
      })
      setActiveRunId(nextAgentRunId(res))
      setHandoff(res.handoff || null)
    } catch (e) {
      if (!acc) updateLastAssistant({ content: '❌ Agent 调用失败' })
      message.error('Agent 调用失败: ' + (e.message || ''))
    } finally {
      sendingRef.current = false
      setSending(false)
    }
  }

  async function confirm(index) {
    const m = messages[index]
    const token = m?.meta?.confirmationToken
    const runId = m?.meta?.runId
    if (!token) return
    setConfirming(true)
    try {
      const { data } = await agentApi.post(
        '/confirm',
        buildAgentConfirmRequest(sessionId, runId, token)
      )
      const res = data?.data ?? data
      setMessages((prev) => {
        const next = prev.slice()
        next[index] = { ...next[index], meta: { ...next[index].meta, confirmationToken: null } }
        next.push({
          role: 'assistant',
          content: res.reply || '操作已完成',
          meta: { state: res.state, intents: res.intents || [], applicationNo: res.applicationNo },
        })
        return next
      })
    } catch (e) {
      message.error('确认执行失败: ' + (e.message || ''))
    } finally {
      setConfirming(false)
    }
  }

  function cancelConfirm(index) {
    setMessages((prev) => {
      const next = prev.slice()
      next[index] = { ...next[index], meta: { ...next[index].meta, confirmationToken: null } }
      next.push({ role: 'assistant', content: '已取消该操作。' })
      return next
    })
  }

  return (
    <div className="agent-page">
      <Card
        hoverable
        className="agent-card"
        title={
          <div className="header">
            <span className="title">售后 Agent 编排</span>
            <Tag color={healthOk ? 'success' : 'error'}>{healthOk ? '服务正常' : '服务异常'}</Tag>
          </div>
        }
      >
        <div className="agent-body">
          <div className="agent-messages" ref={msgRef}>
            {messages.map((m, i) => (
              <div key={i} className={`turn ${m.role}`}>
                <Avatar
                  size={32}
                  style={{
                    background: m.role === 'user' ? '#409eff' : '#9254de',
                    flexShrink: 0,
                  }}
                >
                  {m.role === 'user' ? '我' : 'A'}
                </Avatar>
                <div className="bubble">
                  {/* 流式编排步骤进度（逐个点亮） */}
                  {m.steps && m.steps.length > 0 && (
                    <div className="steps">
                      {m.steps.map((s, si) => (
                        <Tag key={si} color="success">
                          {s}
                        </Tag>
                      ))}
                    </div>
                  )}
                  <div className="reply">{m.content}</div>

                  {m.meta && (
                    <div className="meta">
                      {m.meta.state && <Tag>{m.meta.state}</Tag>}
                      {(m.meta.intents || []).map((it) => (
                        <Tag color="warning" key={it}>
                          {it}
                        </Tag>
                      ))}
                      {m.meta.applicationNo && (
                        <span className="app-no">售后单号：{m.meta.applicationNo}</span>
                      )}
                    </div>
                  )}

                  {/* 待确认写操作 */}
                  {m.meta && m.meta.confirmationToken && (
                    <div className="confirm-box">
                      <div className="plan">
                        <div className="plan-title">待确认操作</div>
                        {m.meta.actionPlan != null && (
                          <pre>{JSON.stringify(m.meta.actionPlan, null, 2)}</pre>
                        )}
                        {m.meta.candidates && m.meta.candidates.length > 0 && (
                          <div className="candidates">
                            候选：
                            {m.meta.candidates.map((c) => (
                              <Tag key={c}>{c}</Tag>
                            ))}
                          </div>
                        )}
                      </div>
                      <div className="confirm-actions">
                        <Button
                          type="primary"
                          size="small"
                          loading={confirming}
                          onClick={() => confirm(i)}
                        >
                          确认执行
                        </Button>
                        <Button size="small" onClick={() => cancelConfirm(i)}>
                          取消
                        </Button>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            ))}
            {messages.length === 0 && (
              <Empty description="和售后 Agent 对话，例如：我要申请退款 / 查一下我的订单" />
            )}
          </div>

          {handoff && (
            <div className="handoff">
              <Alert type="warning" showIcon message={'已转人工：' + (handoff.reason || '')} />
            </div>
          )}

          <div className="input-bar">
            <Input.TextArea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              rows={2}
              placeholder="输入指令，Enter 发送（Shift+Enter 换行）"
              onPressEnter={(e) => {
                if (!e.shiftKey) {
                  e.preventDefault()
                  send()
                }
              }}
              style={{ flex: 1 }}
            />
            <Button type="primary" icon={<SendOutlined />} loading={sending} onClick={send}>
              发送
            </Button>
          </div>
        </div>
      </Card>
    </div>
  )
}
