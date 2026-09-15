import { Avatar, Dropdown, Layout, Menu, Modal, message } from 'antd'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import {
  ApiOutlined,
  BellOutlined,
  CommentOutlined,
  DatabaseOutlined,
  DownOutlined,
  FileTextOutlined,
  HomeOutlined,
  MailOutlined,
  MessageOutlined,
  SearchOutlined,
  ShoppingCartOutlined,
  ShoppingOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { getUser, logout } from '../utils/auth'

const { Sider, Header, Content } = Layout

const MENU_ITEMS = [
  { key: '/', icon: <HomeOutlined />, label: '首页总览' },
  { key: '/chat', icon: <MessageOutlined />, label: 'AI 对话' },
  { key: '/rag-kb', icon: <FileTextOutlined />, label: '向量知识库' },
  { key: '/user', icon: <UserOutlined />, label: '用户管理' },
  { key: '/knowledge', icon: <DatabaseOutlined />, label: '知识库' },
  { key: '/message', icon: <MailOutlined />, label: '消息管理' },
  { key: '/notify', icon: <BellOutlined />, label: '通知中心' },
  { key: '/search', icon: <SearchOutlined />, label: '全文搜索' },
  { key: '/mq', icon: <ApiOutlined />, label: 'MQ 调度' },
  { key: '/shop', icon: <ShoppingCartOutlined />, label: '商品商城' },
  { key: '/product', icon: <ShoppingOutlined />, label: '商品图片检索' },
]

const TITLE_MAP = {
  '/': '首页总览',
  '/chat': 'AI 对话',
  '/rag-kb': '向量知识库',
  '/user': '用户管理',
  '/knowledge': '知识库',
  '/message': '消息管理',
  '/notify': '通知中心',
  '/search': '全文搜索',
  '/mq': 'MQ 调度中心',
  '/shop': '商品商城',
  '/product': '商品图片检索',
  '/cart': '购物车',
  '/order': '订单管理',
  '/mock-pay': '模拟收银台',
}

// 侧边栏 + 顶栏外壳（对应原 App.vue）
export default function MainLayout() {
  const location = useLocation()
  const navigate = useNavigate()
  const path = location.pathname

  // 路由变化时重新读取登录信息（登录/退出后 localStorage 已更新）
  const currentUser = getUser() || {}
  const displayName = currentUser.nickname || currentUser.username || '未登录'
  const userRole = currentUser.role || '-'
  const avatarText = String(displayName || '?').charAt(0).toUpperCase()

  const activeMenu = path.startsWith('/order') ? '/order' : path
  const currentPageTitle = TITLE_MAP[path] || 'AI 客服平台'

  function handleCommand(key) {
    if (key !== 'logout') return
    Modal.confirm({
      title: '提示',
      content: '确定退出登录吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: () => {
        logout()
        message.success('已退出登录')
        navigate('/login')
      },
    })
  }

  const dropdownItems = [
    { key: 'role', label: `角色：${userRole}`, disabled: true },
    { type: 'divider' },
    { key: 'logout', label: '退出登录' },
  ]

  return (
    <Layout className="app-container">
      <Sider width={220} className="app-aside" theme="dark">
        <div className="logo-area">
          <CommentOutlined style={{ fontSize: 28, color: '#409eff' }} />
          <span className="logo-text">AI 客服平台</span>
        </div>
        <Menu
          className="side-menu"
          theme="dark"
          mode="inline"
          selectedKeys={[activeMenu]}
          items={MENU_ITEMS}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>

      <Layout>
        <Header className="app-header">
          <div className="header-left">
            <span className="page-title">{currentPageTitle}</span>
          </div>
          <div className="header-right">
            <Dropdown
              menu={{ items: dropdownItems, onClick: ({ key }) => handleCommand(key) }}
              trigger={['click']}
            >
              <span className="user-info">
                <Avatar size={30} className="user-avatar">
                  {avatarText}
                </Avatar>
                <span className="user-name">{displayName}</span>
                <DownOutlined style={{ fontSize: 12, color: '#909399' }} />
              </span>
            </Dropdown>
          </div>
        </Header>
        <Content className="app-main">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
