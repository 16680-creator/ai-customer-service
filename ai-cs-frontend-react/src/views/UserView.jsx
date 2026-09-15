import { useState } from 'react'
import { Alert, Button, Card, Col, Descriptions, Form, Input, Row, message } from 'antd'
import { KeyOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons'
import { userApi } from '../api'

export default function UserView() {
  const [regForm, setRegForm] = useState({ username: '', password: '', email: '', phone: '' })
  const [loginForm, setLoginForm] = useState({ username: '', password: '' })
  const [queryId, setQueryId] = useState('')
  const [token, setToken] = useState('')
  const [userInfo, setUserInfo] = useState(null)

  async function register() {
    try {
      const { data } = await userApi.post('/register', regForm)
      if (data.code === 200) {
        message.success('注册成功')
      } else {
        message.error(data.message || '注册失败')
      }
    } catch (e) {
      message.error('注册失败: ' + (e.response?.data?.message || e.message))
    }
  }

  async function login() {
    try {
      const { data } = await userApi.post('/login', {
        username: loginForm.username,
        password: loginForm.password,
      })
      if (data.code === 200) {
        setToken(data.data?.token || '')
        message.success('登录成功')
      } else {
        message.error(data.message || '登录失败')
      }
    } catch (e) {
      message.error('登录失败: ' + (e.response?.data?.message || e.message))
    }
  }

  async function queryUser() {
    try {
      const res = await userApi.get(`/${queryId}`)
      setUserInfo(res.data?.data || res.data)
      message.success('查询成功')
    } catch (e) {
      message.error('查询失败: ' + (e.response?.data?.message || e.message))
    }
  }

  return (
    <div className="user-view">
      <Row gutter={20}>
        <Col span={8}>
          <Card hoverable title={<span style={{ fontWeight: 600 }}>用户注册</span>}>
            <Form labelCol={{ span: 5 }} wrapperCol={{ span: 19 }}>
              <Form.Item label="用户名">
                <Input
                  value={regForm.username}
                  onChange={(e) => setRegForm((p) => ({ ...p, username: e.target.value }))}
                  placeholder="请输入用户名"
                />
              </Form.Item>
              <Form.Item label="密码">
                <Input.Password
                  value={regForm.password}
                  onChange={(e) => setRegForm((p) => ({ ...p, password: e.target.value }))}
                  placeholder="请输入密码"
                />
              </Form.Item>
              <Form.Item label="邮箱">
                <Input
                  value={regForm.email}
                  onChange={(e) => setRegForm((p) => ({ ...p, email: e.target.value }))}
                  placeholder="请输入邮箱"
                />
              </Form.Item>
              <Form.Item label="手机号">
                <Input
                  value={regForm.phone}
                  onChange={(e) => setRegForm((p) => ({ ...p, phone: e.target.value }))}
                  placeholder="请输入手机号"
                />
              </Form.Item>
              <Form.Item wrapperCol={{ offset: 5, span: 19 }}>
                <Button type="primary" icon={<PlusOutlined />} onClick={register}>
                  注册
                </Button>
              </Form.Item>
            </Form>
          </Card>
        </Col>

        <Col span={8}>
          <Card hoverable title={<span style={{ fontWeight: 600 }}>用户登录</span>}>
            <Form labelCol={{ span: 5 }} wrapperCol={{ span: 19 }}>
              <Form.Item label="用户名">
                <Input
                  value={loginForm.username}
                  onChange={(e) => setLoginForm((p) => ({ ...p, username: e.target.value }))}
                  placeholder="请输入用户名"
                />
              </Form.Item>
              <Form.Item label="密码">
                <Input.Password
                  value={loginForm.password}
                  onChange={(e) => setLoginForm((p) => ({ ...p, password: e.target.value }))}
                  placeholder="请输入密码"
                />
              </Form.Item>
              <Form.Item wrapperCol={{ offset: 5, span: 19 }}>
                <Button type="primary" icon={<KeyOutlined />} onClick={login}>
                  登录
                </Button>
              </Form.Item>
            </Form>
            {token && (
              <Alert
                type="success"
                showIcon
                message={'登录成功，Token: ' + token.substring(0, 30) + '...'}
                style={{ marginTop: 10, wordBreak: 'break-all' }}
              />
            )}
            <Alert
              type="info"
              showIcon
              message="提示：全站登录请使用右上角用户菜单；此处仅作接口调试"
              style={{ marginTop: 10 }}
            />
          </Card>
        </Col>

        <Col span={8}>
          <Card hoverable title={<span style={{ fontWeight: 600 }}>查询用户</span>}>
            <Form labelCol={{ span: 5 }} wrapperCol={{ span: 19 }}>
              <Form.Item label="用户 ID">
                <Input
                  value={queryId}
                  onChange={(e) => setQueryId(e.target.value)}
                  placeholder="请输入用户ID"
                />
              </Form.Item>
              <Form.Item wrapperCol={{ offset: 5, span: 19 }}>
                <Button type="primary" icon={<SearchOutlined />} onClick={queryUser}>
                  查询
                </Button>
              </Form.Item>
            </Form>
            {userInfo && (
              <Descriptions column={1} bordered size="small" style={{ marginTop: 10 }}>
                <Descriptions.Item label="ID">{userInfo.id}</Descriptions.Item>
                <Descriptions.Item label="用户名">{userInfo.username}</Descriptions.Item>
                <Descriptions.Item label="邮箱">{userInfo.email}</Descriptions.Item>
                <Descriptions.Item label="手机号">{userInfo.phone}</Descriptions.Item>
              </Descriptions>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  )
}
