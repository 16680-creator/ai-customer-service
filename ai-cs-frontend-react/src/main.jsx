import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { App as AntdApp, ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import 'antd/dist/reset.css'
import './style.css'
import App from './App'

// 主题：沿用原 Vue 版 Element Plus 的配色（主色 #409eff，成功 #67c23a，警告 #e6a23c，危险 #f56c6c）
const theme = {
  token: {
    colorPrimary: '#409eff',
    colorSuccess: '#67c23a',
    colorWarning: '#e6a23c',
    colorError: '#f56c6c',
    colorInfo: '#909399',
    borderRadius: 6,
    fontFamily: "'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif",
  },
  components: {
    Menu: {
      darkItemBg: '#1d1e2c',
      darkSubMenuItemBg: '#1d1e2c',
      darkItemColor: '#a3a6ad',
      darkItemHoverBg: 'rgba(64, 158, 255, 0.08)',
      darkItemHoverColor: '#409eff',
      darkItemSelectedBg: 'rgba(64, 158, 255, 0.12)',
      darkItemSelectedColor: '#409eff',
      itemHeight: 50,
      itemMarginInline: 0,
      itemBorderRadius: 0,
    },
    Layout: {
      headerBg: '#fff',
      headerHeight: 64,
      headerPadding: '0 24px',
    },
  },
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <ConfigProvider locale={zhCN} theme={theme}>
    <AntdApp>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </AntdApp>
  </ConfigProvider>
)
