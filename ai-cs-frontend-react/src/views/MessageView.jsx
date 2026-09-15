import { useState } from 'react'
import { Button, Card, Col, Form, Input, InputNumber, Row, Table, Tag, message } from 'antd'
import { PlusOutlined, ReloadOutlined, SendOutlined } from '@ant-design/icons'
import { messageApi } from '../api'

export default function MessageView() {
  const [userId, setUserId] = useState(1)
  const [sessionTitle, setSessionTitle] = useState('新会话')
  const [sessions, setSessions] = useState([])
  const [selectedSessionMessages, setSelectedSessionMessages] = useState([])
  const [msgForm, setMsgForm] = useState({ sessionId: '', content: '', role: 'user' })

  async function createSession() {
    try {
      await messageApi.post('/session', null, {
        params: { userId, title: sessionTitle },
      })
      message.success('会话创建成功')
      fetchSessions()
    } catch (e) {
      message.error('创建失败: ' + (e.response?.data?.message || e.message))
    }
  }

  async function sendMessage() {
    try {
      await messageApi.post('/send', msgForm)
      message.success('消息已发送')
      setMsgForm((prev) => ({ ...prev, content: '' }))
    } catch (e) {
      message.error('发送失败: ' + (e.response?.data?.message || e.message))
    }
  }

  async function fetchSessions() {
    try {
      const res = await messageApi.get('/sessions', { params: { userId } })
      setSessions(res.data?.data || [])
    } catch (e) {
      message.error('加载会话列表失败: ' + (e.response?.data?.message || e.message))
    }
  }

  async function viewMessages(sessionId) {
    try {
      const res = await messageApi.get(`/session/${sessionId}/messages`)
      setSelectedSessionMessages(res.data?.data || [])
    } catch (e) {
      message.error('加载消息失败: ' + (e.response?.data?.message || e.message))
    }
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '标题', dataIndex: 'title' },
    { title: '用户ID', dataIndex: 'userId', width: 80 },
    { title: '创建时间', dataIndex: 'createTime', width: 180 },
    {
      title: '操作',
      width: 120,
      render: (_, row) => (
        <Button size="small" type="primary" onClick={() => viewMessages(row.id)}>
          查看消息
        </Button>
      ),
    },
  ]

  return (
    <div className="message-view">
      <Row gutter={20}>
        <Col span={10}>
          <Card hoverable title={<span style={{ fontWeight: 600 }}>创建会话</span>}>
            <Form layout="inline">
              <Form.Item label="用户ID">
                <InputNumber min={1} value={userId} onChange={(v) => setUserId(v ?? 1)} />
              </Form.Item>
              <Form.Item label="标题">
                <Input
                  value={sessionTitle}
                  onChange={(e) => setSessionTitle(e.target.value)}
                  placeholder="会话标题"
                />
              </Form.Item>
              <Form.Item>
                <Button type="primary" icon={<PlusOutlined />} onClick={createSession}>
                  创建
                </Button>
              </Form.Item>
            </Form>
          </Card>

          <Card hoverable style={{ marginTop: 20 }} title={<span style={{ fontWeight: 600 }}>发送消息 (RocketMQ)</span>}>
            <Form labelCol={{ span: 5 }} wrapperCol={{ span: 19 }}>
              <Form.Item label="会话ID">
                <Input
                  value={msgForm.sessionId}
                  onChange={(e) => setMsgForm((p) => ({ ...p, sessionId: e.target.value }))}
                  placeholder="会话ID"
                />
              </Form.Item>
              <Form.Item label="内容">
                <Input.TextArea
                  value={msgForm.content}
                  onChange={(e) => setMsgForm((p) => ({ ...p, content: e.target.value }))}
                  rows={3}
                  placeholder="消息内容"
                />
              </Form.Item>
              <Form.Item wrapperCol={{ offset: 5, span: 19 }}>
                <Button type="primary" icon={<SendOutlined />} onClick={sendMessage}>
                  发送
                </Button>
              </Form.Item>
            </Form>
          </Card>
        </Col>

        <Col span={14}>
          <Card
            hoverable
            title={
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontWeight: 600 }}>会话列表</span>
                <Button size="small" icon={<ReloadOutlined />} onClick={fetchSessions}>
                  刷新
                </Button>
              </div>
            }
          >
            <Table
              rowKey="id"
              columns={columns}
              dataSource={sessions}
              bordered
              pagination={false}
            />
          </Card>

          {selectedSessionMessages.length > 0 && (
            <Card hoverable style={{ marginTop: 20 }} title={<span style={{ fontWeight: 600 }}>会话消息</span>}>
              <div className="msg-list">
                {selectedSessionMessages.map((msg) => (
                  <div className="msg-item" key={msg.id}>
                    <Tag color={msg.role === 'user' ? undefined : 'success'}>{msg.role || '消息'}</Tag>
                    <span>{msg.content}</span>
                  </div>
                ))}
              </div>
            </Card>
          )}
        </Col>
      </Row>
    </div>
  )
}
