import { useState } from 'react'
import { Button, Form, Input, Tabs, message } from 'antd'
import { CommentOutlined, LockOutlined, UserOutlined } from '@ant-design/icons'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { userApi } from '../api'
import { setToken, setUser } from '../utils/auth'

export default function LoginView() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  const [activeTab, setActiveTab] = useState('login')
  const [loading, setLoading] = useState(false)
  const [regLoading, setRegLoading] = useState(false)
  const [loginForm] = Form.useForm()
  const [regForm] = Form.useForm()

  function gotoNext() {
    const redirect = searchParams.get('redirect')
    navigate(redirect || '/', { replace: true })
  }

  async function handleLogin() {
    let values
    try {
      values = await loginForm.validateFields()
    } catch {
      return
    }
    setLoading(true)
    try {
      const { data } = await userApi.post('/login', {
        username: values.username.trim(),
        password: values.password,
      })
      if (data.code === 200 && data.data) {
        setToken(data.data.token)
        setUser({
          userId: data.data.userId,
          username: data.data.username,
          nickname: data.data.nickname,
          role: data.data.role,
        })
        message.success(`欢迎回来，${data.data.nickname || data.data.username}`)
        gotoNext()
      } else {
        message.error(data.message || '登录失败')
      }
    } catch (e) {
      message.error(e.response?.data?.message || e.message || '登录失败')
    } finally {
      setLoading(false)
    }
  }

  async function handleRegister() {
    let values
    try {
      values = await regForm.validateFields()
    } catch {
      return
    }
    setRegLoading(true)
    try {
      const { data } = await userApi.post('/register', {
        username: values.username.trim(),
        password: values.password,
        nickname: values.nickname?.trim() || undefined,
      })
      if (data.code === 200) {
        message.success('注册成功，请登录')
        loginForm.setFieldsValue({ username: values.username.trim(), password: '' })
        setActiveTab('login')
      } else {
        message.error(data.message || '注册失败')
      }
    } catch (e) {
      message.error(e.response?.data?.message || e.message || '注册失败')
    } finally {
      setRegLoading(false)
    }
  }

  const loginPane = (
    <>
      <Form form={loginForm} size="large" onFinish={handleLogin} initialValues={{ username: '', password: '' }}>
        <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
          <Input prefix={<UserOutlined style={{ color: '#c0c4cc' }} />} placeholder="用户名" allowClear />
        </Form.Item>
        <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
          <Input.Password
            prefix={<LockOutlined style={{ color: '#c0c4cc' }} />}
            placeholder="密码"
            autoComplete="current-password"
          />
        </Form.Item>
        <Form.Item>
          <Button type="primary" size="large" htmlType="submit" className="login-btn" loading={loading}>
            登 录
          </Button>
        </Form.Item>
      </Form>
      <div className="login-tip">默认管理员账号：admin / admin123</div>
    </>
  )

  const registerPane = (
    <Form form={regForm} size="large" onFinish={handleRegister} initialValues={{ username: '', password: '', nickname: '' }}>
      <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
        <Input prefix={<UserOutlined style={{ color: '#c0c4cc' }} />} placeholder="用户名" allowClear />
      </Form.Item>
      <Form.Item
        name="password"
        rules={[
          { required: true, message: '请输入密码' },
          { min: 6, message: '密码至少6位' },
        ]}
      >
        <Input.Password
          prefix={<LockOutlined style={{ color: '#c0c4cc' }} />}
          placeholder="密码（至少6位）"
          autoComplete="new-password"
        />
      </Form.Item>
      <Form.Item name="nickname">
        <Input prefix={<UserOutlined style={{ color: '#c0c4cc' }} />} placeholder="昵称（选填）" allowClear />
      </Form.Item>
      <Form.Item>
        <Button type="primary" size="large" htmlType="submit" className="login-btn" loading={regLoading}>
          注 册
        </Button>
      </Form.Item>
    </Form>
  )

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-header">
          <CommentOutlined style={{ fontSize: 42, color: '#409eff' }} />
          <h1>AI 客服平台</h1>
          <p>登录后即可使用 AI 对话、知识库、消息等全部功能</p>
        </div>

        <Tabs
          className="login-tabs"
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            { key: 'login', label: '登录', children: loginPane },
            { key: 'register', label: '注册', children: registerPane },
          ]}
        />
      </div>
    </div>
  )
}
