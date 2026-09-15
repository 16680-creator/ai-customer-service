import { useEffect, useState } from 'react'
import { Alert, Badge, Button, Card, Col, Empty, Input, InputNumber, Pagination, Row, Select, Spin, Tag, message } from 'antd'
import { CreditCardOutlined, ProfileOutlined, SearchOutlined, ShoppingCartOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { cartApi, productApi } from '../api'
import { getUser } from '../utils/auth'

export default function ShopView() {
  const navigate = useNavigate()

  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [keyword, setKeyword] = useState('')
  const [searchMode, setSearchMode] = useState('keyword')
  const [categoryId, setCategoryId] = useState(null)
  const [page, setPage] = useState(1)
  const [size] = useState(12)
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [cartCount, setCartCount] = useState(0)
  const [addingId, setAddingId] = useState(null)
  const [qtyMap, setQtyMap] = useState({})

  const userId = getUser()?.userId || ''

  useEffect(() => {
    loadCategories()
    loadCartCount()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    loadProducts()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page])

  async function loadCategories() {
    try {
      const { data } = await productApi.get('/categories')
      if (data.code === 200) setCategories(data.data || [])
    } catch {
      // 分类加载失败不阻塞页面
    }
  }

  async function loadProducts() {
    setLoading(true)
    try {
      if (searchMode === 'semantic' && keyword.trim()) {
        const { data } = await productApi.get('/similar', {
          params: { text: keyword.trim(), topK: 12 },
        })
        if (data.code === 200) {
          const list = (data.data || []).map((r) => ({
            id: r.productId,
            name: r.name,
            price: r.price,
            image: r.image,
            stock: undefined,
            score: r.score,
          }))
          setProducts(list)
          setTotal(list.length)
        }
      } else {
        const params = { page, size, status: 1 }
        if (keyword.trim()) params.keyword = keyword.trim()
        if (categoryId) params.categoryId = categoryId
        const { data } = await productApi.get('/list', { params })
        if (data.code === 200 && data.data) {
          setProducts(data.data.records || [])
          setTotal(data.data.total || 0)
        }
      }
    } catch {
      message.error('加载商品失败')
    } finally {
      setLoading(false)
    }
  }

  function handleSearch() {
    if (page !== 1) setPage(1)
    else loadProducts()
  }

  async function addToCart(p) {
    const qty = qtyMap[p.id] || 1
    setAddingId(p.id)
    try {
      const { data } = await cartApi.post(
        '/add',
        { productId: p.id, quantity: qty },
        { headers: { 'X-User-Id': userId } }
      )
      if (data.code === 200) {
        message.success(`「${p.name}」已加入购物车`)
        loadCartCount()
        loadProducts() // 刷新商城库存，实时反映扣减（库存以商品服务 DB 为权威源）
      } else {
        message.error(data.message || '加入购物车失败')
      }
    } catch (e) {
      message.error(e.response?.data?.message || '加入购物车失败')
    } finally {
      setAddingId(null)
    }
  }

  async function loadCartCount() {
    try {
      const { data } = await cartApi.get('/list', { headers: { 'X-User-Id': userId } })
      if (data.code === 200) setCartCount((data.data?.items || []).length)
    } catch {
      // 忽略
    }
  }

  return (
    <div className="shop-view">
      <div className="shop-toolbar">
        <div className="toolbar-left">
          <h2>商品商城</h2>
          <Select
            value={searchMode}
            onChange={setSearchMode}
            style={{ width: 128 }}
            options={[
              { label: '关键词搜索', value: 'keyword' },
              { label: '语义搜索', value: 'semantic' },
            ]}
          />
          <Input
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="搜索商品名称 / 描述，回车搜索"
            allowClear
            style={{ width: 260 }}
            onPressEnter={handleSearch}
          />
          <Select
            value={categoryId}
            onChange={(v) => {
              setCategoryId(v ?? null)
              handleSearch()
            }}
            placeholder="全部分类"
            allowClear
            style={{ width: 140 }}
            options={categories.map((c) => ({ label: c.name, value: c.id }))}
          />
          <Button type="primary" icon={<SearchOutlined />} loading={loading} onClick={handleSearch}>
            搜索
          </Button>
        </div>
        <div className="toolbar-right">
          <Badge count={cartCount} className="cart-badge">
            <Button icon={<ShoppingCartOutlined />} onClick={() => navigate('/cart')}>
              购物车
            </Button>
          </Badge>
          <Button icon={<ProfileOutlined />} onClick={() => navigate('/order')}>
            我的订单
          </Button>
          <Button type="primary" icon={<CreditCardOutlined />} onClick={() => navigate('/cart')}>
            去结算
          </Button>
        </div>
      </div>

      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="支付流程体验：购物车 → 结算 → 下单 → 订单详情「立即支付」→ 模拟收银台 → 支付成功（含渠道回调 / 验签 / 幂等状态更新）。"
      />

      <Spin spinning={loading}>
        {!loading && products.length === 0 && <Empty description="暂无商品" />}
        <Row gutter={16}>
          {products.map((p) => (
            <Col span={6} key={p.id} style={{ marginBottom: 16 }}>
              <Card hoverable className="product-card" styles={{ body: { padding: 0 } }}>
                <div className="product-img-box">
                  {p.image ? (
                    <img src={p.image} className="img" alt={p.name} />
                  ) : (
                    <div className="img-placeholder">暂无图片</div>
                  )}
                </div>
                <div className="product-body">
                  <div className="product-name" title={p.name}>
                    {p.name}
                  </div>
                  <div className="product-meta">
                    <span className="price">¥{Number(p.price).toFixed(2)}</span>
                    {p.stock !== undefined && <span className="stock">库存 {p.stock}</span>}
                    {p.score !== undefined && (
                      <Tag color="success">相似度 {Number(p.score).toFixed(4)}</Tag>
                    )}
                  </div>
                  <div className="product-actions">
                    <InputNumber
                      min={1}
                      max={99}
                      size="small"
                      value={qtyMap[p.id] ?? 1}
                      onChange={(v) => setQtyMap((prev) => ({ ...prev, [p.id]: v ?? 1 }))}
                    />
                    <Button
                      type="primary"
                      size="small"
                      loading={addingId === p.id}
                      onClick={() => addToCart(p)}
                    >
                      加入购物车
                    </Button>
                  </div>
                </div>
              </Card>
            </Col>
          ))}
        </Row>
      </Spin>

      {total > size && (
        <div style={{ display: 'flex', justifyContent: 'center' }}>
          <Pagination
            className="pager"
            current={page}
            pageSize={size}
            total={total}
            showTotal={(t) => `共 ${t} 条`}
            onChange={(p) => setPage(p)}
          />
        </div>
      )}
    </div>
  )
}
