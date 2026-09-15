import { useEffect, useRef, useState } from 'react'
import { Alert, Button, Card, Col, Form, Input, Row, Statistic, Tag, message } from 'antd'
import { ApiOutlined, BellOutlined, PoweroffOutlined, ReloadOutlined, SendOutlined, UserOutlined } from '@ant-design/icons'
import { notifyApi } from '../api'

export default function NotifyView() {
  const [notifyForm, setNotifyForm] = useState({ userId: '', message: '' })
  const [broadcastMsg, setBroadcastMsg] = useState('')
  const [onlineCount, setOnlineCount] = useState(0)
  const [wsConnected, setWsConnected] = useState(false)
  const [notifyLogs, setNotifyLogs] = useState([])
  const wsRef = useRef(null)

  useEffect(() => {
    fetchOnlineCount()
    return () => {
      if (wsRef.current) {
        wsRef.current.close()
        wsRef.current = null
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function sendNotify() {
    try {
      await notifyApi.post('/send', null, { params: notifyForm })
      message.success('通知发送成功')
      setNotifyForm((prev) => ({ ...prev, message: '' }))
    } catch (e) {
      message.error('发送失败: ' + (e.response?.data?.message || e.message))
    }
  }

  async function broadcast() {
    try {
      await notifyApi.post('/broadcast', null, { params: { message: broadcastMsg } })
      message.success('广播发送成功')
      setBroadcastMsg('')
    } catch (e) {
      message.error('广播失败: ' + (e.response?.data?.message || e.message))
    }
  }

  async function fetchOnlineCount() {
    try {
      const res = await notifyApi.get('/online')
      setOnlineCount(res.data?.data ?? 0)
    } catch {
      message.error('获取在线用户数失败')
    }
  }

  function addLog(text) {
    setNotifyLogs((prev) => {
      const next = [{ time: new Date().toLocaleTimeString(), message: text }, ...prev]
      return next.slice(0, 50)
    })
  }

  function connectWs() {
    try {
      const ws = new WebSocket('ws://localhost:8085/ws/notify')
      wsRef.current = ws
      ws.onopen = () => {
        setWsConnected(true)
        addLog('WebSocket 已连接')
      }
      ws.onmessage = (e) => {
        addLog('收到通知: ' + e.data)
      }
      ws.onclose = () => {
        setWsConnected(false)
        addLog('WebSocket 已断开')
      }
    } catch {
      message.error('WebSocket 连接失败')
    }
  }

  function disconnectWs() {
    if (wsRef.current) {
      wsRef.current.close()
      wsRef.current = null
    }
  }

  return (
    <div className="notify-view">
      <Row gutter={20}>
        <Col span={8}>
          <Card hoverable title={<span style={{ fontWeight: 600 }}>发送通知</span>}>
            <Form labelCol={{ span: 5 }} wrapperCol={{ span: 19 }}>
              <Form.Item label="用户ID">
                <Input
                  value={notifyForm.userId}
                  onChange={(e) => setNotifyForm((p) => ({ ...p, userId: e.target.value }))}
                  placeholder="接收用户ID"
                />
              </Form.Item>
              <Form.Item label="消息内容">
                <Input.TextArea
                  value={notifyForm.message}
                  onChange={(e) => setNotifyForm((p) => ({ ...p, message: e.target.value }))}
                  rows={4}
                  placeholder="通知内容"
                />
              </Form.Item>
              <Form.Item wrapperCol={{ offset: 5, span: 19 }}>
                <Button type="primary" icon={<SendOutlined />} onClick={sendNotify}>
                  发送
                </Button>
              </Form.Item>
            </Form>
          </Card>
        </Col>

        <Col span={8}>
          <Card hoverable title={<span style={{ fontWeight: 600 }}>广播通知</span>}>
            <Form labelCol={{ span: 5 }} wrapperCol={{ span: 19 }}>
              <Form.Item label="消息内容">
                <Input.TextArea
                  value={broadcastMsg}
                  onChange={(e) => setBroadcastMsg(e.target.value)}
                  rows={4}
                  placeholder="广播通知内容（发送给所有在线用户）"
                />
              </Form.Item>
              <Form.Item wrapperCol={{ offset: 5, span: 19 }}>
                <Button type="primary" style={{ background: '#e6a23c' }} icon={<BellOutlined />} onClick={broadcast}>
                  广播
                </Button>
              </Form.Item>
            </Form>
          </Card>
        </Col>

        <Col span={8}>
          <Card hoverable title={<span style={{ fontWeight: 600 }}>在线用户</span>}>
            <div style={{ textAlign: 'center', padding: 20 }}>
              <Statistic title="当前在线用户数" value={onlineCount} prefix={<UserOutlined />} />
              <Button
                type="primary"
                style={{ marginTop: 20 }}
                icon={<ReloadOutlined />}
                onClick={fetchOnlineCount}
              >
                刷新
              </Button>
            </div>
          </Card>
        </Col>
      </Row>

      <Row gutter={20} style={{ marginTop: 20 }}>
        <Col span={24}>
          <Card hoverable title={<span style={{ fontWeight: 600 }}>WebSocket 通知 (实时)</span>}>
            <Alert
              type="info"
              showIcon
              message="通知服务使用 WebSocket 推送，连接后实时接收通知"
              style={{ marginBottom: 16 }}
            />
            <Button type="primary" style={{ background: '#67c23a' }} icon={<ApiOutlined />} onClick={connectWs}>
              连接 WebSocket
            </Button>
            <Button danger icon={<PoweroffOutlined />} style={{ marginLeft: 10 }} onClick={disconnectWs}>
              断开
            </Button>
            <Tag color={wsConnected ? 'success' : 'error'} style={{ marginLeft: 10 }}>
              {wsConnected ? '已连接' : '未连接'}
            </Tag>

            {notifyLogs.length > 0 && (
              <div className="notify-log">
                {notifyLogs.map((log, i) => (
                  <div className="log-item" key={i}>
                    <Tag>{log.time}</Tag>
                    <span>{log.message}</span>
                  </div>
                ))}
              </div>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  )
}
