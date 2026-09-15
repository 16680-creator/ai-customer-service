import { useEffect, useState } from 'react'
import { Button, InputNumber, Table, message } from 'antd'
import { useNavigate } from 'react-router-dom'
import { cartApi } from '../api'
import { getUser } from '../utils/auth'
import { confirmDialog } from '../utils/dialog'

export default function CartView() {
  const navigate = useNavigate()
  const [cartItems, setCartItems] = useState([])
  const [totalAmount, setTotalAmount] = useState(0)
  const [selectedCount, setSelectedCount] = useState(0)
  const [selectedIds, setSelectedIds] = useState([])

  const userId = getUser()?.userId || localStorage.getItem('userId') || ''

  useEffect(() => {
    fetchCartList()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function fetchCartList() {
    try {
      const { data } = await cartApi.get('/list', { headers: { 'X-User-Id': userId } })
      if (data.code === 200) {
        setCartItems(data.data.items)
        setTotalAmount(data.data.totalAmount)
        setSelectedCount(data.data.selectedCount)
      }
    } catch {
      message.error('获取购物车失败')
    }
  }

  async function handleQuantityChange(row, val) {
    try {
      const { data } = await cartApi.put(
        '/quantity',
        { cartItemId: row.id, quantity: val },
        { headers: { 'X-User-Id': userId } }
      )
      if (data.code === 200) {
        setCartItems(data.data.items)
        setTotalAmount(data.data.totalAmount)
        setSelectedCount(data.data.selectedCount)
      } else {
        message.error(data.message)
        fetchCartList()
      }
    } catch {
      message.error('修改数量失败')
      fetchCartList()
    }
  }

  async function handleDelete(row) {
    try {
      await confirmDialog({ content: '确定删除该商品？' })
    } catch {
      return // 用户取消
    }
    try {
      const { data } = await cartApi.delete(`/${row.id}`, { headers: { 'X-User-Id': userId } })
      if (data.code === 200) {
        message.success('删除成功')
        fetchCartList()
      }
    } catch {
      message.error('删除失败')
    }
  }

  function goCheckout() {
    navigate({ pathname: '/checkout', search: `?ids=${selectedIds.join(',')}` })
  }

  const columns = [
    { title: '商品名称', dataIndex: 'productName' },
    {
      title: '单价',
      dataIndex: 'productPrice',
      width: 120,
      render: (v) => `¥${Number(v).toFixed(2)}`,
    },
    {
      title: '数量',
      width: 180,
      render: (_, row) => (
        <InputNumber
          min={1}
          max={99}
          size="small"
          value={row.quantity}
          onChange={(val) => handleQuantityChange(row, val ?? 1)}
        />
      ),
    },
    {
      title: '小计',
      width: 120,
      render: (_, row) => `¥${Number(row.subtotal).toFixed(2)}`,
    },
    {
      title: '操作',
      width: 100,
      render: (_, row) => (
        <Button type="link" danger size="small" onClick={() => handleDelete(row)}>
          删除
        </Button>
      ),
    },
  ]

  return (
    <div className="cart-view">
      <h2>购物车</h2>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={cartItems}
        style={{ marginTop: 16 }}
        pagination={false}
        rowSelection={{
          selectedRowKeys: selectedIds,
          onChange: (keys) => setSelectedIds(keys),
        }}
      />

      <div className="cart-footer">
        <div className="summary">
          <span>已选 {selectedCount} 件商品</span>
          <span className="total">
            合计：<strong>¥{Number(totalAmount).toFixed(2)}</strong>
          </span>
        </div>
        <Button type="primary" size="large" disabled={selectedCount === 0} onClick={goCheckout}>
          去结算
        </Button>
      </div>
    </div>
  )
}
