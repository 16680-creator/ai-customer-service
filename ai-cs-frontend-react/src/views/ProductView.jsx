import { useEffect, useState } from 'react'
import { Button, Card, Col, Empty, Form, Input, InputNumber, Modal, Row, Table, Tag, Upload, message } from 'antd'
import {
  CloudUploadOutlined,
  CopyOutlined,
  PlusOutlined,
  SearchOutlined,
} from '@ant-design/icons'
import { GATEWAY_URL, productApi } from '../api'
import { getToken } from '../utils/auth'
import { confirmDialog } from '../utils/dialog'

const GATEWAY = GATEWAY_URL
const uploadUrl = `${GATEWAY}/api/product/upload-image`

const EMPTY_FORM = {
  name: '',
  description: '',
  price: 99,
  stock: 100,
  categoryId: 1,
  image: '',
}

export default function ProductView() {
  const [uploadedUrl, setUploadedUrl] = useState('')
  const [productForm, setProductForm] = useState(EMPTY_FORM)
  const [creating, setCreating] = useState(false)

  const [searchText, setSearchText] = useState('')
  const [searching, setSearching] = useState(false)
  const [searched, setSearched] = useState(false)
  const [similarResults, setSimilarResults] = useState([])

  const [products, setProducts] = useState([])
  const [, setLoadingProducts] = useState(false)

  const [editDialogVisible, setEditDialogVisible] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [savingEdit, setSavingEdit] = useState(false)

  const [categoryDialogVisible, setCategoryDialogVisible] = useState(false)
  const [categories, setCategories] = useState([])
  const [newCategoryName, setNewCategoryName] = useState('')
  const [creatingCategory, setCreatingCategory] = useState(false)

  const uploadHeaders = { Authorization: 'Bearer ' + (getToken() || '') }

  useEffect(() => {
    loadProducts()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function setField(key, value) {
    setProductForm((prev) => ({ ...prev, [key]: value }))
  }

  /** el-upload 的 on-success / on-error 等价实现（antd 通过 onChange 上报状态） */
  function onUploadChange(info) {
    const { status, response } = info.file
    if (status === 'done') {
      if (response?.code === 200) {
        setUploadedUrl(response.data)
        setProductForm((prev) => ({ ...prev, image: response.data }))
        message.success('图片上传成功')
      } else {
        message.error('上传失败: ' + (response?.message || '未知错误'))
      }
    } else if (status === 'error') {
      message.error('图片上传失败，请确认商品服务已启动且 MinIO 可用')
    }
  }

  async function copyUrl() {
    if (!uploadedUrl) {
      message.warning('请先上传图片')
      return
    }
    try {
      await navigator.clipboard.writeText(uploadedUrl)
      message.success('URL 已复制')
    } catch {
      message.warning('复制失败，请手动选择复制')
    }
  }

  async function createProduct() {
    if (!productForm.name) {
      message.warning('请输入商品名称')
      return
    }
    setCreating(true)
    try {
      await productApi.post('', { ...productForm })
      message.success('商品创建成功，已建立向量索引')
      setProductForm(EMPTY_FORM)
      loadProducts()
    } catch (e) {
      message.error('创建失败: ' + (e.response?.data?.message || e.message))
    } finally {
      setCreating(false)
    }
  }

  function openEdit(item) {
    setEditingId(item.id)
    setProductForm({
      name: item.name || '',
      description: item.description || '',
      price: Number(item.price || 0),
      stock: Number(item.stock || 0),
      categoryId: item.categoryId || 1,
      image: item.image || '',
    })
    setEditDialogVisible(true)
  }

  async function saveEdit() {
    if (!productForm.name) {
      message.warning('请输入商品名称')
      return
    }
    setSavingEdit(true)
    try {
      await productApi.put(`/${editingId}`, productForm)
      message.success('商品更新成功')
      setEditDialogVisible(false)
      loadProducts()
    } catch (e) {
      message.error('更新失败: ' + (e.response?.data?.message || e.message))
    } finally {
      setSavingEdit(false)
    }
  }

  async function deleteProduct(item) {
    try {
      await confirmDialog({ content: `确定删除商品「${item.name}」？` })
    } catch {
      return // 用户取消
    }
    try {
      await productApi.delete(`/${item.id}`)
      message.success('商品已删除')
      loadProducts()
    } catch (e) {
      message.error('删除失败: ' + (e.response?.data?.message || e.message))
    }
  }

  async function loadCategories() {
    try {
      const res = await productApi.get('/categories')
      setCategories(res.data?.data || [])
    } catch {
      message.error('分类加载失败')
    }
  }

  async function createCategory() {
    if (!newCategoryName.trim()) {
      message.warning('请输入分类名称')
      return
    }
    setCreatingCategory(true)
    try {
      await productApi.post('/category', null, {
        params: { name: newCategoryName.trim(), parentId: 0 },
      })
      message.success('分类创建成功')
      setNewCategoryName('')
      loadCategories()
    } catch (e) {
      message.error('创建失败: ' + (e.response?.data?.message || e.message))
    } finally {
      setCreatingCategory(false)
    }
  }

  function openCategories() {
    setCategoryDialogVisible(true)
    loadCategories()
  }

  async function searchByText() {
    if (!searchText.trim()) {
      message.warning('请输入检索文本')
      return
    }
    setSearching(true)
    setSearched(true)
    try {
      const res = await productApi.get('/similar', {
        params: { text: searchText.trim(), topK: 10 },
      })
      const list = res.data?.data || []
      setSimilarResults(list)
      message.success(`找到 ${list.length} 个相似商品`)
    } catch (e) {
      setSimilarResults([])
      message.error('检索失败: ' + (e.response?.data?.message || e.message))
    } finally {
      setSearching(false)
    }
  }

  async function findSimilar(id) {
    try {
      const res = await productApi.get(`/${id}/similar`, { params: { topK: 10 } })
      const list = res.data?.data || []
      setSimilarResults(list)
      setSearched(true)
      message.success(`找到 ${list.length} 个相似商品`)
    } catch (e) {
      message.error('检索失败: ' + (e.response?.data?.message || e.message))
    }
  }

  async function loadProducts() {
    setLoadingProducts(true)
    try {
      const res = await productApi.get('/list', { params: { page: 1, size: 20 } })
      setProducts(res.data?.data?.records || [])
    } catch {
      message.warning('商品列表加载失败（商品服务未启动？）')
    } finally {
      setLoadingProducts(false)
    }
  }

  function scoreType(score) {
    if (score >= 0.8) return 'success'
    if (score >= 0.6) return 'warning'
    return 'default'
  }

  const categoryColumns = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: '分类名称', dataIndex: 'name' },
    { title: '父分类', dataIndex: 'parentId', width: 80 },
  ]

  return (
    <div className="product-view">
      <Row gutter={20}>
        {/* 左列：图片上传 + 创建商品 */}
        <Col span={9}>
          <Card hoverable title={<span style={{ fontWeight: 600 }}>商品图片上传 (MinIO)</span>}>
            <Upload.Dragger
              className="image-uploader"
              name="file"
              action={uploadUrl}
              headers={uploadHeaders}
              showUploadList={false}
              accept=".jpg,.jpeg,.png,.webp,.gif"
              onChange={onUploadChange}
            >
              {uploadedUrl ? (
                <img src={uploadedUrl} className="upload-preview" alt="商品图预览" />
              ) : (
                <>
                  <p className="ant-upload-drag-icon">
                    <CloudUploadOutlined />
                  </p>
                  <p className="ant-upload-text">拖拽图片到此处，或 点击上传</p>
                </>
              )}
            </Upload.Dragger>
            <div style={{ fontSize: 12, color: '#909399', marginTop: 8 }}>
              仅支持 jpg/png/webp/gif，不超过 5MB
            </div>

            <Input
              className="url-input"
              value={uploadedUrl}
              readOnly
              placeholder="上传成功后自动填充图片 URL"
              addonAfter={
                <Button type="text" size="small" icon={<CopyOutlined />} onClick={copyUrl}>
                  复制
                </Button>
              }
            />
          </Card>

          <Card hoverable style={{ marginTop: 20 }} title={<span style={{ fontWeight: 600 }}>创建商品</span>}>
            <Form labelCol={{ span: 5 }} wrapperCol={{ span: 19 }}>
              <Form.Item label="商品名称">
                <Input
                  value={productForm.name}
                  onChange={(e) => setField('name', e.target.value)}
                  placeholder="如：无线蓝牙耳机"
                />
              </Form.Item>
              <Form.Item label="描述">
                <Input.TextArea
                  value={productForm.description}
                  onChange={(e) => setField('description', e.target.value)}
                  rows={2}
                  placeholder="用于向量检索的文本，尽量描述特征"
                />
              </Form.Item>
              <Form.Item label="价格">
                <InputNumber
                  min={0}
                  precision={2}
                  value={productForm.price}
                  onChange={(v) => setField('price', v ?? 0)}
                  style={{ width: '100%' }}
                />
              </Form.Item>
              <Form.Item label="库存">
                <InputNumber
                  min={0}
                  value={productForm.stock}
                  onChange={(v) => setField('stock', v ?? 0)}
                  style={{ width: '100%' }}
                />
              </Form.Item>
              <Form.Item label="分类ID">
                <InputNumber
                  min={1}
                  value={productForm.categoryId}
                  onChange={(v) => setField('categoryId', v ?? 1)}
                  style={{ width: '100%' }}
                />
              </Form.Item>
              <Form.Item label="图片">
                <Input
                  value={productForm.image}
                  onChange={(e) => setField('image', e.target.value)}
                  placeholder="留空使用刚上传的图片"
                />
              </Form.Item>
              <Form.Item wrapperCol={{ offset: 5, span: 19 }}>
                <Button type="primary" icon={<PlusOutlined />} loading={creating} onClick={createProduct}>
                  创建并建立向量索引
                </Button>
              </Form.Item>
            </Form>
          </Card>
        </Col>

        {/* 中列：以文搜图 */}
        <Col span={9}>
          <Card hoverable title={<span style={{ fontWeight: 600 }}>以文搜图（向量检索）</span>}>
            <div style={{ display: 'flex', gap: 10, marginBottom: 16 }}>
              <Input
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
                placeholder="输入商品描述，如：降噪耳机、蓝牙音箱..."
                allowClear
                onPressEnter={searchByText}
              />
              <Button type="primary" icon={<SearchOutlined />} loading={searching} onClick={searchByText}>
                搜索
              </Button>
            </div>

            {similarResults.length > 0 ? (
              <div className="result-list">
                {similarResults.map((item) => (
                  <div className="result-item" key={item.productId}>
                    {item.image ? (
                      <img src={item.image} className="result-img" alt={item.name} />
                    ) : (
                      <div className="result-img placeholder">无图</div>
                    )}
                    <div className="result-info">
                      <div className="result-name">{item.name}</div>
                      <div className="result-price">¥{item.price}</div>
                    </div>
                    <div className="result-score">
                      <Tag color={scoreType(item.score)}>相似度 {item.score.toFixed(4)}</Tag>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              searched && <Empty description="未找到相似商品" />
            )}
          </Card>
        </Col>

        {/* 右列：商品列表 + 商品找相似 */}
        <Col span={6}>
          <Card
            hoverable
            title={
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontWeight: 600 }}>商品列表</span>
                <Button size="small" type="link" onClick={openCategories}>
                  分类管理
                </Button>
              </div>
            }
          >
            {products.map((item) => (
              <div className="product-item" key={item.id}>
                {item.image ? (
                  <img src={item.image} className="product-img" alt={item.name} />
                ) : (
                  <div className="product-img placeholder">无图</div>
                )}
                <div className="product-info">
                  <div className="product-name">{item.name}</div>
                  <div className="product-meta">
                    ¥{item.price} · 库存 {item.stock}
                  </div>
                </div>
                <Button size="small" type="primary" style={{ background: '#67c23a' }} onClick={() => findSimilar(item.id)}>
                  找相似
                </Button>
                <Button size="small" type="primary" onClick={() => openEdit(item)}>
                  编辑
                </Button>
                <Button size="small" danger onClick={() => deleteProduct(item)}>
                  删除
                </Button>
              </div>
            ))}
            {products.length === 0 && <Empty description="暂无商品" />}
          </Card>
        </Col>
      </Row>

      {/* 编辑商品对话框 */}
      <Modal
        open={editDialogVisible}
        title="编辑商品"
        width={520}
        okText="保存"
        cancelText="取消"
        confirmLoading={savingEdit}
        onOk={saveEdit}
        onCancel={() => setEditDialogVisible(false)}
      >
        <Form labelCol={{ span: 5 }} wrapperCol={{ span: 19 }}>
          <Form.Item label="商品名称">
            <Input value={productForm.name} onChange={(e) => setField('name', e.target.value)} />
          </Form.Item>
          <Form.Item label="描述">
            <Input.TextArea
              value={productForm.description}
              onChange={(e) => setField('description', e.target.value)}
              rows={2}
            />
          </Form.Item>
          <Form.Item label="价格">
            <InputNumber
              min={0}
              precision={2}
              value={productForm.price}
              onChange={(v) => setField('price', v ?? 0)}
              style={{ width: '100%' }}
            />
          </Form.Item>
          <Form.Item label="库存">
            <InputNumber
              min={0}
              value={productForm.stock}
              onChange={(v) => setField('stock', v ?? 0)}
              style={{ width: '100%' }}
            />
          </Form.Item>
          <Form.Item label="分类ID">
            <InputNumber
              min={1}
              value={productForm.categoryId}
              onChange={(v) => setField('categoryId', v ?? 1)}
              style={{ width: '100%' }}
            />
          </Form.Item>
          <Form.Item label="图片">
            <Input value={productForm.image} onChange={(e) => setField('image', e.target.value)} />
          </Form.Item>
        </Form>
      </Modal>

      {/* 分类管理对话框 */}
      <Modal
        open={categoryDialogVisible}
        title="分类管理"
        width={480}
        footer={null}
        onCancel={() => setCategoryDialogVisible(false)}
      >
        <div style={{ display: 'flex', gap: 10, marginBottom: 12 }}>
          <Input
            value={newCategoryName}
            onChange={(e) => setNewCategoryName(e.target.value)}
            placeholder="新分类名称"
          />
          <Button type="primary" loading={creatingCategory} onClick={createCategory}>
            新增
          </Button>
        </div>
        <Table
          rowKey="id"
          columns={categoryColumns}
          dataSource={categories}
          size="small"
          pagination={false}
        />
      </Modal>
    </div>
  )
}
