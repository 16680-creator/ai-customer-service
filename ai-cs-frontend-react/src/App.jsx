import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { isAuthenticated } from './utils/auth'
import MainLayout from './layouts/MainLayout'

import LoginView from './views/LoginView'
import Dashboard from './views/Dashboard'
import ChatView from './views/ChatView'
import ChatDashboardView from './views/ChatDashboardView'
import AgentView from './views/AgentView'
import GraphView from './views/GraphView'
import RagEvalView from './views/RagEvalView'
import VectorKbView from './views/VectorKbView'
import KnowledgeView from './views/KnowledgeView'
import KnowledgeOpsView from './views/KnowledgeOpsView'
import SearchView from './views/SearchView'
import PromptView from './views/PromptView'
import TraceView from './views/TraceView'
import UserView from './views/UserView'
import MessageView from './views/MessageView'
import NotifyView from './views/NotifyView'
import MqView from './views/MqView'
import ShopView from './views/ShopView'
import ProductView from './views/ProductView'
import CartView from './views/CartView'
import CheckoutView from './views/CheckoutView'
import OrderView from './views/OrderView'
import OrderDetailView from './views/OrderDetailView'
import MockCashierView from './views/MockCashierView'

/** 全局路由守卫：未登录用户只能访问公开页面（登录页），其余一律跳转登录页 */
function RequireAuth({ children }) {
  const location = useLocation()
  if (!isAuthenticated()) {
    const redirect = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/login?redirect=${redirect}`} replace />
  }
  return children
}

/** 已登录访问登录页，直接进首页 */
function PublicOnly({ children }) {
  if (isAuthenticated()) {
    return <Navigate to="/" replace />
  }
  return children
}

export default function App() {
  return (
    <Routes>
      <Route
        path="/login"
        element={
          <PublicOnly>
            <LoginView />
          </PublicOnly>
        }
      />

      <Route
        element={
          <RequireAuth>
            <MainLayout />
          </RequireAuth>
        }
      >
        <Route path="/" element={<Dashboard />} />
        <Route path="/chat" element={<ChatView />} />
        <Route path="/chat-dashboard" element={<ChatDashboardView />} />
        <Route path="/feedback" element={<ChatView />} />
        <Route path="/agent" element={<AgentView />} />
        <Route path="/graph" element={<GraphView />} />
        <Route path="/rag-eval" element={<RagEvalView />} />
        <Route path="/rag-kb" element={<VectorKbView />} />
        <Route path="/knowledge" element={<KnowledgeView />} />
        <Route path="/knowledge-ops" element={<KnowledgeOpsView />} />
        <Route path="/search" element={<SearchView />} />
        <Route path="/prompts" element={<PromptView />} />
        <Route path="/trace" element={<TraceView />} />
        <Route path="/user" element={<UserView />} />
        <Route path="/message" element={<MessageView />} />
        <Route path="/notify" element={<NotifyView />} />
        <Route path="/mq" element={<MqView />} />
        <Route path="/shop" element={<ShopView />} />
        <Route path="/product" element={<ProductView />} />
        <Route path="/cart" element={<CartView />} />
        <Route path="/checkout" element={<CheckoutView />} />
        <Route path="/order" element={<OrderView />} />
        <Route path="/order/:orderNo" element={<OrderDetailView />} />
        <Route path="/mock-pay" element={<MockCashierView />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  )
}
