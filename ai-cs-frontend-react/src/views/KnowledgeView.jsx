import { useState, useEffect } from 'react'
import { Button, Card, Descriptions, Form, Input, Modal, Pagination, Popconfirm, Table, Tag, message } from 'antd'
import { DeleteOutlined, EditOutlined, EyeOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons'
import { knowledgeApi } from '../api'

const EMPTY_FORM = { title: '', category: '', content: '', status: 1 }

export default function KnowledgeView() {
  const [documents, setDocuments] = useState([])
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [total, setTotal] = useState(0)
  const [keyword, setKeyword] = useState('')

  const [dialogVisible, setDialogVisible] = useState(false)
  const [viewVisible, setViewVisible] = useState(false)
  const [editingDoc, setEditingDoc] = useState(null)
  const [viewDoc, setViewDoc] = useState(null)
  const [docForm, setDocForm] = useState(EMPTY_FORM)

  useEffect(() => {
    fetchList()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, pageSize])

  async function fetchList() {
    try {
      const res = await knowledgeApi.get('/list', {
        params: { page, pageSize, keyword: keyword || undefined },
      })
      const data = res.data?.data
      const records = data?.records || data || []
      setDocuments(records)
      setTotal(data?.total || records.length)
    } catch (e) {
      message.error('加载文档列表失败: ' + (e.response?.data?.message || e.message))
    }
  }

  function showCreateDialog() {
    setEditingDoc(null)
    setDocForm(EMPTY_FORM)
    setDialogVisible(true)
  }

  function editDoc(row) {
    setEditingDoc(row)
    setDocForm({ ...row })
    setDialogVisible(true)
  }

  function showDoc(row) {
    setViewDoc(row)
    setViewVisible(true)
  }

  async function saveDoc() {
    try {
      if (editingDoc) {
        await knowledgeApi.put('', docForm)
      } else {
        await knowledgeApi.post('', docForm)
      }
      message.success('保存成功')
      setDialogVisible(false)
      fetchList()
    } catch (e) {
      message.error('保存失败: ' + (e.response?.data?.message || e.message))
    }
  }

  async function deleteDoc(id) {
    try {
      await knowledgeApi.delete(`/${id}`)
      message.success('删除成功')
      fetchList()
    } catch (e) {
      message.error('删除失败: ' + (e.response?.data?.message || e.message))
    }
  }

  function search() {
    if (page !== 1) setPage(1)
    else fetchList()
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '标题', dataIndex: 'title' },
    { title: '分类', dataIndex: 'category', width: 120 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (status) => (
        <Tag color={status === 1 ? 'success' : 'default'}>{status === 1 ? '启用' : '禁用'}</Tag>
      ),
    },
    { title: '创建时间', dataIndex: 'createTime', width: 180 },
    {
      title: '操作',
      width: 180,
      fixed: 'right',
      render: (_, row) => (
        <>
          <Button size="small" shape="circle" icon={<EyeOutlined />} onClick={() => showDoc(row)} />
          <Button
            size="small"
            type="primary"
            shape="circle"
            icon={<EditOutlined />}
            style={{ marginLeft: 8 }}
            onClick={() => editDoc(row)}
          />
          <Popconfirm title="确认删除?" onConfirm={() => deleteDoc(row.id)}>
            <Button
              size="small"
              danger
              shape="circle"
              icon={<DeleteOutlined />}
              style={{ marginLeft: 8 }}
            />
          </Popconfirm>
        </>
      ),
    },
  ]

  return (
    <div className="knowledge-view">
      <Card
        hoverable
        title={
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontWeight: 600 }}>知识库文档管理</span>
            <Button type="primary" icon={<PlusOutlined />} onClick={showCreateDialog}>
              新增文档
            </Button>
          </div>
        }
      >
        {/* 搜索栏 */}
        <div style={{ marginBottom: 16, display: 'flex', gap: 10 }}>
          <Input
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="搜索文档..."
            allowClear
            style={{ width: 300 }}
            onClear={() => {
              setKeyword('')
              fetchList()
            }}
          />
          <Button type="primary" icon={<SearchOutlined />} onClick={search}>
            搜索
          </Button>
        </div>

        {/* 文档表格 */}
        <Table
          rowKey="id"
          columns={columns}
          dataSource={documents}
          bordered
          scroll={{ x: 900 }}
          pagination={false}
        />

        <div style={{ marginTop: 16, display: 'flex', justifyContent: 'flex-end' }}>
          <Pagination
            current={page}
            pageSize={pageSize}
            total={total}
            showTotal={(t) => `共 ${t} 条`}
            onChange={(p, ps) => {
              setPage(p)
              setPageSize(ps)
            }}
          />
        </div>
      </Card>

      {/* 新增/编辑弹窗 */}
      <Modal
        open={dialogVisible}
        title={editingDoc ? '编辑文档' : '新增文档'}
        width={600}
        onCancel={() => setDialogVisible(false)}
        onOk={saveDoc}
        okText="保存"
        cancelText="取消"
      >
        <Form labelCol={{ span: 4 }} wrapperCol={{ span: 20 }}>
          <Form.Item label="标题">
            <Input
              value={docForm.title}
              onChange={(e) => setDocForm((p) => ({ ...p, title: e.target.value }))}
              placeholder="文档标题"
            />
          </Form.Item>
          <Form.Item label="分类">
            <Input
              value={docForm.category}
              onChange={(e) => setDocForm((p) => ({ ...p, category: e.target.value }))}
              placeholder="文档分类"
            />
          </Form.Item>
          <Form.Item label="内容">
            <Input.TextArea
              value={docForm.content}
              onChange={(e) => setDocForm((p) => ({ ...p, content: e.target.value }))}
              rows={6}
              placeholder="文档内容"
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* 查看详情弹窗 */}
      <Modal
        open={viewVisible}
        title="文档详情"
        width={600}
        footer={null}
        onCancel={() => setViewVisible(false)}
      >
        {viewDoc && (
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="ID">{viewDoc.id}</Descriptions.Item>
            <Descriptions.Item label="标题">{viewDoc.title}</Descriptions.Item>
            <Descriptions.Item label="分类">{viewDoc.category}</Descriptions.Item>
            <Descriptions.Item label="内容">{viewDoc.content}</Descriptions.Item>
            <Descriptions.Item label="创建时间">{viewDoc.createTime}</Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </div>
  )
}
