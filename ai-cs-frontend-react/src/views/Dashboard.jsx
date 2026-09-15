import { useCallback, useEffect, useRef, useState } from 'react'
import { Button, Card, Col, Divider, Row, Tag, message } from 'antd'
import {
  ApiOutlined,
  AppstoreOutlined,
  BarChartOutlined,
  BellOutlined,
  CloudServerOutlined,
  CommentOutlined,
  DatabaseOutlined,
  DesktopOutlined,
  FileTextOutlined,
  FolderOpenOutlined,
  MailOutlined,
  MessageOutlined,
  ProfileOutlined,
  ReloadOutlined,
  RocketOutlined,
  SearchOutlined,
  ShareAltOutlined,
  ShoppingCartOutlined,
  ShoppingOutlined,
  StarOutlined,
  ThunderboltOutlined,
  UserOutlined,
} from '@ant-design/icons'
import axios from 'axios'
import { useNavigate } from 'react-router-dom'
import { GATEWAY_URL, messageApi, notifyApi } from '../api'
import { getUser } from '../utils/auth'

// 后端 health 接口返回的 icon 字段是原 Element Plus 图标名，这里做一次映射
const ICON_MAP = {
  Monitor: <DesktopOutlined />,
  Cpu: <ThunderboltOutlined />,
  User: <UserOutlined />,
  UserFilled: <UserOutlined />,
  ChatDotSquare: <CommentOutlined />,
  ChatDotRound: <CommentOutlined />,
  Message: <MailOutlined />,
  Bell: <BellOutlined />,
  Search: <SearchOutlined />,
  ShoppingCart: <ShoppingCartOutlined />,
  Goods: <ShoppingOutlined />,
  Tickets: <ProfileOutlined />,
  Files: <FileTextOutlined />,
  Collection: <DatabaseOutlined />,
  Connection: <ApiOutlined />,
  Share: <ShareAltOutlined />,
  DataAnalysis: <BarChartOutlined />,
  Document: <FileTextOutlined />,
}

const TECHS = [
  'Spring Boot 3.2.5',
  'Spring Cloud 2023',
  'Spring AI 1.1.4',
  'Nacos 2.3',
  'MySQL 8.0',
  'Redis 7',
  'Elasticsearch 8.12',
  'RocketMQ 5.1',
  'Chroma 向量库',
  'OpenFeign',
  'MyBatis-Plus',
  'React 18',
  'Ant Design 5',
  'Vite',
]

// 功能入口：type=solid 实心按钮，plain 描边按钮（用 color 还原原配色）
const QUICK_ACTIONS = [
  { path: '/chat', label: 'AI 对话', icon: <MessageOutlined />, variant: 'solid', color: 'primary' },
  { path: '/rag-kb', label: '向量知识库', icon: <FileTextOutlined />, variant: 'plain', color: 'blue' },
  { path: '/knowledge', label: '知识库管理', icon: <DatabaseOutlined />, variant: 'solid', color: 'green' },
  { path: '/cart', label: '购物车', icon: <ShoppingCartOutlined />, variant: 'solid', color: 'orange' },
  { path: '/shop', label: '商品商城', icon: <ShoppingCartOutlined />, variant: 'plain', color: 'green' },
  { path: '/order', label: '订单管理', icon: <ProfileOutlined />, variant: 'plain', color: 'orange' },
  { path: '/product', label: '商品管理', icon: <ShoppingOutlined />, variant: 'solid', color: 'gray' },
  { path: '/search', label: '全文搜索', icon: <SearchOutlined />, variant: 'plain', color: 'gray' },
  { path: '/mq', label: 'MQ 调度', icon: <ApiOutlined />, variant: 'plain', color: 'gray' },
  { path: '/message', label: '消息管理', icon: <MailOutlined />, variant: 'plain', color: 'gray' },
  { path: '/notify', label: '通知中心', icon: <BellOutlined />, variant: 'plain', color: 'gray' },
  { path: '/user', label: '用户管理', icon: <UserOutlined />, variant: 'plain', color: 'gray' },
  { path: '/feedback', label: '对话反馈', icon: <StarOutlined />, variant: 'plain', color: 'primary' },
  { path: '/trace', label: '链路追踪', icon: <ApiOutlined />, variant: 'plain', color: 'orange' },
  { path: '/prompts', label: 'Prompt 管理', icon: <FileTextOutlined />, variant: 'plain', color: 'green' },
  { path: '/agent', label: '售后 Agent', icon: <RocketOutlined />, variant: 'plain', color: 'red' },
  { path: '/graph', label: '知识图谱', icon: <ShareAltOutlined />, variant: 'plain', color: 'gray' },
  { path: '/rag-eval', label: 'RAG 评估', icon: <BarChartOutlined />, variant: 'plain', color: 'gray' },
]

const COLOR_STYLE = {
  primary: undefined,
  green: { color: '#67c23a', borderColor: '#67c23a' },
  orange: { color: '#e6a23c', borderColor: '#e6a23c' },
  red: { color: '#f56c6c', borderColor: '#f56c6c' },
  blue: { color: '#409eff', borderColor: '#409eff' },
  gray: { color: '#909399', borderColor: '#d9d9d9' },
}

function nowTime() {
  const now = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${p(now.getHours())}:${p(now.getMinutes())}:${p(now.getSeconds())}`
}

export default function Dashboard() {
  const navigate = useNavigate()
  const [services, setServices] = useState([])
  const [loadingHealth, setLoadingHealth] = useState(false)
  const [lastUpdate, setLastUpdate] = useState('-')
  const [onlineCount, setOnlineCount] = useState('-')
  const [sessionCount, setSessionCount] = useState('-')
  const timerRef = useRef(null)

  // 从后端健康接口获取服务状态
  const loadHealth = useCallback(async () => {
    setLoadingHealth(true)
    try {
      const { data } = await axios.get(`${GATEWAY_URL}/api/health`, { timeout: 10000 })
      setServices(Array.isArray(data) ? data : data?.data || [])
      setLastUpdate(nowTime())
    } catch {
      setServices([])
      setLastUpdate('获取失败')
    } finally {
      setLoadingHealth(false)
    }
  }, [])

  // 在线用户数（notify 服务）
  const loadOnline = useCallback(async () => {
    try {
      const { data } = await notifyApi.get('/online')
      if (data.code === 200) setOnlineCount(data.data ?? 0)
    } catch {
      setOnlineCount('-')
    }
  }, [])

  // 对话会话数（message 服务，当前用户）
  const loadSessions = useCallback(async () => {
    try {
      const uid = getUser()?.userId || localStorage.getItem('userId') || 1
      const { data } = await messageApi.get('/sessions', { params: { userId: uid } })
      if (data.code === 200) setSessionCount((data.data || []).length)
    } catch {
      setSessionCount('-')
    }
  }, [])

  useEffect(() => {
    loadHealth()
    loadOnline()
    loadSessions()
    timerRef.current = setInterval(() => {
      loadHealth()
      loadOnline()
      loadSessions()
    }, 15000)
    return () => {
      if (timerRef.current) clearInterval(timerRef.current)
    }
  }, [loadHealth, loadOnline, loadSessions])

  const upCount = services.filter((s) => s.status === 'UP').length
  const stats = [
    {
      label: '微服务在线',
      value: `${upCount}/${services.length || 9}`,
      icon: <DesktopOutlined />,
      color: '#409eff',
    },
    { label: 'AI 模型', value: 'DeepSeek', icon: <ThunderboltOutlined />, color: '#67c23a' },
    { label: '对话会话', value: sessionCount, icon: <CommentOutlined />, color: '#e6a23c' },
    { label: '在线用户', value: onlineCount, icon: <UserOutlined />, color: '#f56c6c' },
  ]

  return (
    <div className="dashboard">
      <Row gutter={20} className="stat-row">
        {stats.map((item) => (
          <Col span={6} key={item.label}>
            <Card hoverable className="stat-card" styles={{ body: { padding: 24 } }}>
              <div className="stat-content">
                <div>
                  <div className="stat-value">{item.value}</div>
                  <div className="stat-label">{item.label}</div>
                </div>
                <span style={{ fontSize: 48, color: item.color }}>{item.icon}</span>
              </div>
            </Card>
          </Col>
        ))}
      </Row>

      <Row gutter={20} style={{ marginTop: 20 }}>
        <Col span={12}>
          <Card
            hoverable
            title={
              <div className="card-header">
                <span style={{ fontWeight: 600 }}>微服务状态</span>
                <div className="header-right">
                  <span className="refresh-tip">每 15 秒自动刷新 · 上次更新 {lastUpdate}</span>
                  <Button size="small" icon={<ReloadOutlined />} onClick={loadHealth} loading={loadingHealth}>
                    刷新
                  </Button>
                </div>
              </div>
            }
          >
            <div className="arch-list">
              {services.map((svc) => (
                <div className="arch-item" key={svc.key || svc.name}>
                  <span style={{ fontSize: 20, color: svc.status === 'UP' ? '#67c23a' : '#f56c6c' }}>
                    {ICON_MAP[svc.icon] || <DesktopOutlined />}
                  </span>
                  <span className="arch-name">{svc.name}</span>
                  <Tag color={svc.status === 'UP' ? 'success' : 'error'}>
                    {svc.status === 'UP' ? '运行中' : '离线'}
                  </Tag>
                  <span className="arch-port">:{svc.port}</span>
                </div>
              ))}
            </div>
          </Card>
        </Col>

        <Col span={12}>
          <Card hoverable title={<span style={{ fontWeight: 600 }}>功能入口</span>}>
            <div className="quick-actions">
              {QUICK_ACTIONS.map((a) => (
                <Button
                  key={a.path}
                  type={a.variant === 'solid' ? 'primary' : 'default'}
                  danger={a.color === 'red'}
                  icon={a.icon}
                  style={a.variant === 'plain' ? COLOR_STYLE[a.color] : undefined}
                  onClick={() => navigate(a.path)}
                >
                  {a.label}
                </Button>
              ))}
            </div>

            <Divider />

            <div className="feature-tips">
              <div className="tip-item">
                <CommentOutlined style={{ color: '#409eff' }} /> AI 对话支持「RAG 知识库模式」，知识库标识填{' '}
                <b>knowledge</b> 可检索知识库文档
              </div>
              <div className="tip-item">
                <ProfileOutlined style={{ color: '#e6a23c' }} /> 交易链路：购物车 → 结算 → 下单 → 订单管理 → 支付
              </div>
              <div className="tip-item">
                <ShoppingOutlined style={{ color: '#67c23a' }} /> 商品支持图片上传、相似检索、编辑、删除、分类管理
              </div>
              <div className="tip-item">
                <RocketOutlined style={{ color: '#9254de' }} /> 售后 Agent 编排：意图识别 → 待确认写操作 → 执行，支持转人工
              </div>
            </div>
          </Card>
        </Col>
      </Row>

      <Row gutter={20} style={{ marginTop: 20 }}>
        <Col span={24}>
          <Card hoverable title={<span style={{ fontWeight: 600 }}>技术栈</span>}>
            <div className="tech-tags">
              {TECHS.map((t) => (
                <Tag key={t} style={{ margin: 4, fontSize: 14, padding: '4px 10px' }}>
                  {t}
                </Tag>
              ))}
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  )
}
