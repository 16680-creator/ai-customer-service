import { useState } from 'react'
import { Button, Card, Col, Empty, Form, Input, Popconfirm, Row, Tag, message } from 'antd'
import { DeleteOutlined, FileTextOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons'
import { searchApi } from '../api'

export default function SearchView() {
  const [indexName, setIndexName] = useState('knowledge')
  const [searchQuery, setSearchQuery] = useState('')
  const [results, setResults] = useState([])
  const [searching, setSearching] = useState(false)
  const [searched, setSearched] = useState(false)
  const [page] = useState(1)

  const [indexMgmt, setIndexMgmt] = useState({ name: '' })
  const [docIndex, setDocIndex] = useState({ name: '', content: '' })

  async function doSearch() {
    if (!searchQuery.trim()) return
    setSearching(true)
    setSearched(true)
    try {
      const res = await searchApi.get(`/${indexName}`, {
        params: { query: searchQuery, page, size: 20 },
      })
      const list = res.data?.data || []
      setResults(list)
      message.success(`找到 ${list.length} 条结果`)
    } catch (e) {
      setResults([])
      message.error('搜索失败: ' + (e.response?.data?.message || e.message))
    } finally {
      setSearching(false)
    }
  }

  async function createIndex() {
    if (!indexMgmt.name) {
      message.warning('请输入索引名称')
      return
    }
    try {
      await searchApi.post(`/index/${indexMgmt.name}`, {})
      message.success('索引创建成功')
    } catch (e) {
      message.error('创建失败: ' + (e.response?.data?.message || e.message))
    }
  }

  async function deleteIndex() {
    if (!indexMgmt.name) {
      message.warning('请输入索引名称')
      return
    }
    try {
      await searchApi.delete(`/index/${indexMgmt.name}`)
      message.success('索引删除成功')
    } catch (e) {
      message.error('删除失败: ' + (e.response?.data?.message || e.message))
    }
  }

  async function indexDoc() {
    if (!docIndex.name || !docIndex.content) {
      message.warning('请填写完整')
      return
    }
    let doc
    try {
      doc = JSON.parse(docIndex.content)
    } catch {
      message.error('文档内容必须是合法 JSON')
      return
    }
    try {
      await searchApi.post(`/document/${docIndex.name}`, doc)
      message.success('文档索引成功')
      setDocIndex((p) => ({ ...p, content: '' }))
    } catch (e) {
      message.error('索引失败: ' + (e.response?.data?.message || e.message))
    }
  }

  return (
    <div className="search-view">
      <Row gutter={20}>
        <Col span={16}>
          <Card hoverable title={<span style={{ fontWeight: 600 }}>全文搜索 (Chroma)</span>}>
            <div style={{ display: 'flex', gap: 10, marginBottom: 20 }}>
              <Input
                value={indexName}
                onChange={(e) => setIndexName(e.target.value)}
                placeholder="索引名称"
                style={{ width: 200 }}
              />
              <Input
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="搜索关键词..."
                allowClear
                style={{ flex: 1 }}
                onPressEnter={doSearch}
              />
              <Button type="primary" icon={<SearchOutlined />} loading={searching} onClick={doSearch}>
                搜索
              </Button>
            </div>

            {/* 搜索结果 */}
            {results.length > 0 ? (
              <div className="result-list">
                {results.map((item, i) => (
                  <div className="result-item" key={i}>
                    <div className="result-title">{item.title || item._id || '文档 ' + (i + 1)}</div>
                    <div className="result-content">
                      {item.content || item.highlight || JSON.stringify(item)}
                    </div>
                    <div className="result-meta">
                      <Tag>{item._index || indexName}</Tag>
                      {item._score ? (
                        <span className="result-score">相关度: {item._score?.toFixed(2)}</span>
                      ) : null}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              searched && <Empty description="未找到相关结果" />
            )}
          </Card>
        </Col>

        <Col span={8}>
          <Card hoverable title={<span style={{ fontWeight: 600 }}>索引管理</span>}>
            <Form labelCol={{ span: 6 }} wrapperCol={{ span: 18 }}>
              <Form.Item label="索引名称">
                <Input
                  value={indexMgmt.name}
                  onChange={(e) => setIndexMgmt({ name: e.target.value })}
                  placeholder="索引名称"
                />
              </Form.Item>
              <Form.Item wrapperCol={{ offset: 6, span: 18 }}>
                <Button type="primary" icon={<PlusOutlined />} onClick={createIndex}>
                  创建索引
                </Button>
                <Popconfirm title="确认删除索引?" onConfirm={deleteIndex}>
                  <Button danger icon={<DeleteOutlined />} style={{ marginLeft: 10 }}>
                    删除索引
                  </Button>
                </Popconfirm>
              </Form.Item>
            </Form>
          </Card>

          <Card hoverable style={{ marginTop: 20 }} title={<span style={{ fontWeight: 600 }}>索引文档</span>}>
            <Form labelCol={{ span: 6 }} wrapperCol={{ span: 18 }}>
              <Form.Item label="索引名称">
                <Input
                  value={docIndex.name}
                  onChange={(e) => setDocIndex((p) => ({ ...p, name: e.target.value }))}
                  placeholder="索引名称"
                />
              </Form.Item>
              <Form.Item label="文档内容">
                <Input.TextArea
                  value={docIndex.content}
                  onChange={(e) => setDocIndex((p) => ({ ...p, content: e.target.value }))}
                  rows={5}
                  placeholder='JSON 格式，如 {"title":"xxx","content":"xxx"}'
                />
              </Form.Item>
              <Form.Item wrapperCol={{ offset: 6, span: 18 }}>
                <Button type="primary" icon={<FileTextOutlined />} onClick={indexDoc}>
                  索引文档
                </Button>
              </Form.Item>
            </Form>
          </Card>
        </Col>
      </Row>
    </div>
  )
}
