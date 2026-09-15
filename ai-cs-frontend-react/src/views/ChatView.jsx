import { useEffect, useRef, useState } from 'react'
import {
  Avatar,
  Button,
  Card,
  Divider,
  Empty,
  Input,
  Progress,
  Radio,
  Rate,
  Tag,
  Tooltip,
  Typography,
  Upload,
  message,
} from 'antd'
import {
  CommentOutlined,
  DeleteOutlined,
  FileTextOutlined,
  PictureOutlined,
  PlusOutlined,
  DislikeOutlined,
  LikeOutlined,
  SendOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { GATEWAY_URL, chatApi, feedbackApi, messageApi, visionApi } from '../api'
import { getToken, getUser } from '../utils/auth'
import { confirmDialog, promptDialog } from '../utils/dialog'

/** 获取当前登录用户 ID */
function getUserId() {
  const user = getUser()
  return user ? user.userId : null
}

export default function ChatView() {
  const navigate = useNavigate()

  const [sessions, setSessions] = useState([])
  const [currentSession, setCurrentSession] = useState('')
  const [messages, setMessages] = useState([])
  const [inputMessage, setInputMessage] = useState('')
  const [sending, setSending] = useState(false)
  const [chatMode, setChatMode] = useState('normal')
  const [knowledgeBase, setKnowledgeBase] = useState('')
  const [uploadedImageUrl, setUploadedImageUrl] = useState('')

  // 当前会话 / 会话消息缓存 / 发送中标记（流式期间需读最新值，用 ref 避免闭包过期）
  const currentSessionRef = useRef('')
  const sessionMessagesRef = useRef({})
  const sessionsRef = useRef([])
  const sendingRef = useRef(false)
  const messagesRef = useRef(null)

  useEffect(() => {
    sessionsRef.current = sessions
  }, [sessions])

  // 消息变化后滚动到底部
  useEffect(() => {
    const el = messagesRef.current
    if (el) el.scrollTop = el.scrollHeight
  }, [messages])

  /** 更新当前会话消息，同时回写缓存 */
  function updateMessages(updater) {
    setMessages((prev) => {
      const next = typeof updater === 'function' ? updater(prev) : updater
      if (currentSessionRef.current) {
        sessionMessagesRef.current[currentSessionRef.current] = next
      }
      return next
    })
  }

  function updateMessageAt(index, patch) {
    updateMessages((prev) => prev.map((m, i) => (i === index ? { ...m, ...patch } : m)))
  }

  // 从后端加载会话历史并填充到当前消息数组
  async function loadHistory(sessionKey) {
    if (!sessionKey) return
    // 已有消息内容则跳过（避免重复加载）
    if ((sessionMessagesRef.current[sessionKey] || []).length > 0) return
    try {
      const resp = await chatApi.get('/history', { params: { sessionKey } })
      if (resp.data && resp.data.code === 200 && resp.data.data && resp.data.data.length > 0) {
        const list = resp.data.data.map((m) => ({ role: m.role, content: m.content }))
        sessionMessagesRef.current[sessionKey] = list
        if (currentSessionRef.current === sessionKey) setMessages(list)
      }
    } catch (e) {
      // 后端不可用时静默降级
      console.warn('加载历史会话失败:', e.message)
    }
  }

  // 从后端加载用户的所有会话列表
  async function loadSessions() {
    const userId = getUserId()
    if (!userId) return []
    try {
      const resp = await messageApi.get('/sessions', { params: { userId } })
      if (resp.data && resp.data.code === 200 && resp.data.data && resp.data.data.length > 0) {
        const list = resp.data.data.map((s) => ({
          id: String(s.id),
          title: s.title || '默认会话',
        }))
        setSessions(list)
        return list
      }
    } catch (e) {
      console.warn('加载会话列表失败:', e.message)
    }
    return []
  }

  // 切换会话（左侧列表点击）
  function switchSession(id) {
    currentSessionRef.current = String(id)
    setCurrentSession(String(id))
    if (!sessionMessagesRef.current[id]) sessionMessagesRef.current[id] = []
    setMessages(sessionMessagesRef.current[id])
    loadHistory(id)
  }

  // 新建会话（持久化到后端）
  async function newSession() {
    const userId = getUserId()
    if (!userId) {
      message.warning('请先登录')
      return
    }
    try {
      const title = '新会话 ' + (sessionsRef.current.length + 1)
      const resp = await messageApi.post('/session', null, { params: { userId, title } })
      if (resp.data && resp.data.code === 200 && resp.data.data) {
        const session = resp.data.data
        setSessions((prev) => [...prev, { id: String(session.id), title: session.title }])
        switchSession(String(session.id))
      }
    } catch (e) {
      message.error('创建会话失败: ' + (e.message || ''))
    }
  }

  // 删除会话（含其下所有消息）
  async function handleDeleteSession(session) {
    try {
      await confirmDialog({
        title: '删除确认',
        content: `确定删除会话「${session.title}」吗？该会话下的所有消息也会被删除，且不可恢复。`,
        okText: '删除',
        danger: true,
      })
    } catch {
      return // 用户取消
    }
    try {
      const resp = await messageApi.delete(`/session/${session.id}`)
      if (resp.data && resp.data.code === 200) {
        const rest = sessionsRef.current.filter((s) => s.id !== session.id)
        setSessions(rest)
        delete sessionMessagesRef.current[session.id]
        // 删除的是当前会话：切换到剩余会话或清空
        if (currentSessionRef.current === session.id) {
          if (rest.length > 0) {
            switchSession(rest[0].id)
          } else {
            currentSessionRef.current = ''
            setCurrentSession('')
            setMessages([])
          }
        }
        message.success('会话已删除')
      }
    } catch (e) {
      message.error('删除会话失败: ' + (e.message || ''))
    }
  }

  // 初始化：加载会话列表，无会话时创建默认会话
  useEffect(() => {
    let cancelled = false
    ;(async () => {
      const list = await loadSessions()
      if (cancelled) return
      if (list.length > 0) {
        // 切换到最近会话（列表已按 updateTime 倒序，第一个就是最新）
        switchSession(list[0].id)
      } else {
        await newSession()
      }
    })()
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // 上传图片：调 /chat/upload-image 拿到 MinIO URL，暂存待发送
  async function handleImageChange(file) {
    if (!file) return
    try {
      const resp = await visionApi.uploadImage(file)
      if (resp.data && resp.data.code === 200 && resp.data.data) {
        setUploadedImageUrl(resp.data.data)
        message.success('图片已上传')
      } else {
        message.error(resp.data?.message || '图片上传失败')
      }
    } catch (e) {
      message.error('图片上传失败: ' + (e.message || ''))
    }
  }

  /**
   * 真正的流式对话（SSE）：fetch + ReadableStream 解析，逐 token 追加（打字机效果）
   * 支持图片：携带 imageUrl 时走 /chat/vision/sse（多模态图生文）
   */
  async function sendMessage() {
    const text = inputMessage.trim()
    const imageUrl = uploadedImageUrl
    // 图片对话允许只有图片无文字；纯文本对话必须有文字
    if (!text && !imageUrl) return
    // 流式输出期间禁止重复发送（按钮 loading 挡不住 Enter 键，需显式防重入）
    if (sendingRef.current) return
    if (chatMode === 'rag' && !knowledgeBase.trim()) {
      message.warning('RAG 模式请先填写知识库标识')
      return
    }

    const targetSession = currentSessionRef.current
    updateMessages((prev) => [
      ...prev,
      { role: 'user', content: text || '(图片)', imageUrl: imageUrl || null },
    ])
    setInputMessage('')
    setUploadedImageUrl('')
    sendingRef.current = true
    setSending(true)

    // 预置空回复气泡，流式填充
    updateMessages((prev) => [...prev, { role: 'assistant', content: '' }])

    /** 仅当仍停留在发起对话的会话时，才把增量写回该会话的最后一条消息 */
    const patchAssistant = (patch) => {
      if (currentSessionRef.current !== targetSession) return
      updateMessages((prev) => {
        if (!prev.length) return prev
        const next = prev.slice()
        next[next.length - 1] = { ...next[next.length - 1], ...patch }
        return next
      })
    }

    let acc = ''
    let streamError = null

    try {
      const params = new URLSearchParams({ sessionId: targetSession, message: text })
      if (imageUrl) params.set('imageUrl', imageUrl)
      if (chatMode === 'rag') params.set('knowledgeBase', knowledgeBase.trim())

      // 有图片走图片对话 SSE，无图片走普通 SSE
      const endpoint = imageUrl ? 'vision/sse' : 'stream/sse'
      const resp = await fetch(`${GATEWAY_URL}/api/chat/${endpoint}?${params}`, {
        method: 'POST',
        headers: { Authorization: 'Bearer ' + (getToken() || '') },
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
            // done 事件仅携带 citations，不带 content；content 已逐 token 累积，跳过避免重复追加
            if (obj.content && !obj.done) {
              acc += obj.content
              patchAssistant({ content: acc })
            }
            if (obj.citations) {
              patchAssistant({ citations: obj.citations })
            }
            if (obj.error) streamError = obj.error
          }
        }
      }

      if (streamError) throw new Error(streamError)
      if (!acc) patchAssistant({ content: '(无回复)' })
    } catch (e) {
      // 区分"连接被中止"（页面刷新/重复发送导致浏览器中断 fetch）与真实业务错误
      const aborted = e?.name === 'AbortError' || e?.message?.includes('Failed to fetch')
      patchAssistant({
        content: aborted ? '⚠️ 连接中断，回复未完成，请重试。' : '❌ 错误: ' + (e.message || '请求失败'),
      })
      message.error(aborted ? '对话连接中断，请重试' : '对话请求失败: ' + (e.message || ''))
    } finally {
      sendingRef.current = false
      setSending(false)
    }
  }

  // ===== 回答反馈（点赞/点踩/评分/补充）=====
  function rateMessage(index, type) {
    // 切换反馈类型；再次点击已选类型则取消
    const msg = messages[index]
    const next = msg.feedback === type ? null : type
    if (!next) {
      updateMessageAt(index, { feedback: null })
      return
    }
    const score = msg.score || 5
    updateMessageAt(index, { feedback: next, score })
    submitFeedback(index, { ...msg, feedback: next, score })
  }

  function onScoreChange(index, score) {
    const msg = messages[index]
    updateMessageAt(index, { score })
    submitFeedback(index, { ...msg, score })
  }

  async function submitFeedback(index, msg) {
    if (!msg.feedback) return
    try {
      await feedbackApi.submit({
        sessionId: Number(currentSessionRef.current) || null,
        feedbackType: msg.feedback,
        score: msg.score || null,
        comment: msg.feedbackComment || null,
        requestId: msg.requestId || null,
      })
      updateMessageAt(index, { feedbackSubmitted: true })
      message.success('感谢反馈，已记录')
    } catch (e) {
      message.error('反馈提交失败: ' + (e.message || '未知错误'))
    }
  }

  async function openFeedbackComment(index) {
    let value
    try {
      value = await promptDialog({
        title: '反馈补充',
        placeholder: '补充您的反馈意见',
        okText: '提交',
      })
    } catch {
      return // 用户取消
    }
    const msg = messages[index]
    updateMessageAt(index, { feedbackComment: value })
    submitFeedback(index, { ...msg, feedbackComment: value })
  }

  return (
    <div className="chat-view">
      <div style={{ display: 'flex', gap: 20, height: '100%' }}>
        {/* 左侧会话列表 */}
        <div style={{ width: '25%' }}>
          <Card
            hoverable
            className="session-card"
            title={
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontWeight: 600 }}>会话列表</span>
                <Button type="primary" size="small" icon={<PlusOutlined />} onClick={newSession}>
                  新对话
                </Button>
              </div>
            }
          >
            <div className="session-list">
              {sessions.map((s) => (
                <div
                  key={s.id}
                  className={`session-item${currentSession === s.id ? ' active' : ''}`}
                  onClick={() => switchSession(s.id)}
                >
                  <CommentOutlined />
                  <span className="session-title">{s.title}</span>
                  <Tooltip title="删除会话" placement="top">
                    <DeleteOutlined
                      className="session-delete"
                      onClick={(e) => {
                        e.stopPropagation()
                        handleDeleteSession(s)
                      }}
                    />
                  </Tooltip>
                </div>
              ))}
              {sessions.length === 0 && (
                <Empty description="暂无会话" image={Empty.PRESENTED_IMAGE_SIMPLE} />
              )}
            </div>
          </Card>
        </div>

        {/* 右侧聊天区 */}
        <div style={{ width: '75%' }}>
          <Card hoverable className="chat-card">
            <div className="chat-messages" ref={messagesRef}>
              {messages.map((msg, i) => (
                <div key={i} className={`msg-row ${msg.role}`}>
                  <Avatar
                    size={36}
                    style={{
                      background: msg.role === 'user' ? '#409eff' : '#67c23a',
                      flexShrink: 0,
                    }}
                  >
                    {msg.role === 'user' ? '我' : 'AI'}
                  </Avatar>
                  <div className="msg-bubble">
                    {msg.imageUrl && <img src={msg.imageUrl} className="msg-image" alt="图片" />}
                    <div>{msg.content}</div>

                    {/* 引用溯源卡片：RAG 回答有 citations 时展示 */}
                    {msg.citations && msg.citations.length > 0 && (
                      <div className="citation-cards">
                        <Divider orientation="left" style={{ margin: '8px 0' }}>
                          <Tag>引用来源 {msg.citations.length} 条</Tag>
                        </Divider>
                        <div className="citation-list">
                          {msg.citations.map((cit, ci) => (
                            <Card key={ci} className="citation-card" styles={{ body: { padding: '10px 14px' } }}>
                              <div className="citation-header">
                                <FileTextOutlined style={{ fontSize: 14 }} />
                                <span className="citation-title">{cit.title || '未命名文档'}</span>
                                {cit.page ? <span className="citation-page">P.{cit.page}</span> : null}
                              </div>
                              {cit.score != null && (
                                <div className="citation-score">
                                  <span className="score-label">相关度</span>
                                  <Progress
                                    percent={Math.round(cit.score * 100)}
                                    size="small"
                                    showInfo={false}
                                    strokeColor={
                                      cit.score >= 0.7
                                        ? '#67c23a'
                                        : cit.score >= 0.5
                                          ? '#e6a23c'
                                          : '#f56c6c'
                                    }
                                    style={{ flex: 1, marginBottom: 0 }}
                                  />
                                </div>
                              )}
                              <div className="citation-content-preview">
                                <Typography.Paragraph
                                  type="secondary"
                                  style={{ fontSize: 12, marginBottom: 0 }}
                                  ellipsis={{ rows: 3 }}
                                >
                                  {cit.content}
                                </Typography.Paragraph>
                              </div>
                            </Card>
                          ))}
                        </div>
                      </div>
                    )}

                    {/* 回答反馈工具条：点赞/点踩 + 评分 + 补充文本（仅 AI 回答） */}
                    {msg.role === 'assistant' &&
                      (msg.feedbackSubmitted ? (
                        <Tag color="success" style={{ marginTop: 8 }}>
                          已反馈 {msg.feedback === 'LIKE' ? '👍' : '👎'}
                        </Tag>
                      ) : (
                        <div className="feedback-bar">
                          <Button.Group size="small">
                            <Button
                              type={msg.feedback === 'LIKE' ? 'primary' : 'default'}
                              icon={<LikeOutlined />}
                              onClick={() => rateMessage(i, 'LIKE')}
                            >
                              有用
                            </Button>
                            <Button
                              danger={msg.feedback === 'DISLIKE'}
                              icon={<DislikeOutlined />}
                              onClick={() => rateMessage(i, 'DISLIKE')}
                            >
                              没用
                            </Button>
                          </Button.Group>
                          {msg.feedback && (
                            <>
                              <Rate
                                value={msg.score}
                                count={5}
                                onChange={(v) => onScoreChange(i, v)}
                                style={{ fontSize: 14 }}
                              />
                              <Button size="small" type="link" onClick={() => openFeedbackComment(i)}>
                                补充
                              </Button>
                            </>
                          )}
                        </div>
                      ))}
                  </div>
                </div>
              ))}
              {messages.length === 0 && <Empty description="开始和 AI 对话吧" />}
            </div>

            <div className="chat-mode-bar">
              <Radio.Group
                size="small"
                value={chatMode}
                onChange={(e) => setChatMode(e.target.value)}
                optionType="button"
              >
                <Radio.Button value="normal">普通对话</Radio.Button>
                <Radio.Button value="rag">RAG 知识库对话</Radio.Button>
              </Radio.Group>
              {chatMode === 'rag' && (
                <>
                  <Input
                    value={knowledgeBase}
                    onChange={(e) => setKnowledgeBase(e.target.value)}
                    placeholder="知识库标识（如 test-kb）"
                    size="small"
                    allowClear
                    style={{ width: 220, marginLeft: 12 }}
                  />
                  <Tooltip title="需先在「向量知识库」页入库资料" placement="top">
                    <Button
                      type="link"
                      size="small"
                      style={{ marginLeft: 12 }}
                      onClick={() => navigate('/rag-kb')}
                    >
                      前往入库
                    </Button>
                  </Tooltip>
                </>
              )}
            </div>

            <div className="chat-input">
              <Input.TextArea
                value={inputMessage}
                onChange={(e) => setInputMessage(e.target.value)}
                placeholder="输入你的问题..."
                autoSize={{ minRows: 2, maxRows: 4 }}
                onPressEnter={(e) => {
                  if (!e.shiftKey) {
                    e.preventDefault()
                    sendMessage()
                  }
                }}
              />
              <Upload
                showUploadList={false}
                accept="image/jpeg,image/png,image/webp,image/gif"
                beforeUpload={(file) => {
                  handleImageChange(file)
                  return false
                }}
              >
                <Button
                  icon={<PictureOutlined />}
                  disabled={sending}
                  title="上传图片"
                  style={{ marginLeft: 10, height: 54 }}
                />
              </Upload>
              {uploadedImageUrl && (
                <Button
                  danger
                  icon={<DeleteOutlined />}
                  title="移除图片"
                  style={{ marginLeft: 6, height: 54 }}
                  onClick={() => setUploadedImageUrl('')}
                />
              )}
              <Button
                type="primary"
                icon={<SendOutlined />}
                loading={sending}
                onClick={sendMessage}
                style={{ marginLeft: 10, height: 54 }}
              >
                发送
              </Button>
            </div>
          </Card>
        </div>
      </div>
    </div>
  )
}
