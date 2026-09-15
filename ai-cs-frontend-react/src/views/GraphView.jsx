import { useEffect, useState } from 'react'
import { Button, Card, Col, Divider, Empty, Form, Input, InputNumber, Row, Table, Tag, message } from 'antd'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons'
import { ragApi } from '../api'

const TRIPLE_COLUMNS = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '主体', dataIndex: 'subject', minWidth: 160 },
  { title: '关系', dataIndex: 'predicate', width: 140 },
  { title: '客体', dataIndex: 'object', minWidth: 160 },
  { title: '来源文档', dataIndex: 'sourceDocumentId', width: 110 },
  { title: '知识库', dataIndex: 'knowledgeBase', width: 120 },
]

export default function GraphView() {
  const [form, setForm] = useState({
    knowledgeBase: 'knowledge',
    subject: '',
    predicate: '',
    object: '',
    sourceDocumentId: '',
  })
  const [saving, setSaving] = useState(false)

  const [q, setQ] = useState({ knowledgeBase: 'knowledge', entity: '', depth: 2 })
  const [hits, setHits] = useState([])
  const [queried, setQueried] = useState(false)
  const [loading, setLoading] = useState(false)

  const [triples, setTriples] = useState([])
  const [listLoading, setListLoading] = useState(false)

  useEffect(() => {
    loadList()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function setField(key, value) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  async function saveTriple() {
    const f = form
    if (!f.subject.trim() || !f.predicate.trim() || !f.object.trim()) {
      message.warning('请填写主体、关系、客体')
      return
    }
    setSaving(true)
    try {
      const payload = {
        knowledgeBase: f.knowledgeBase.trim(),
        subject: f.subject.trim(),
        predicate: f.predicate.trim(),
        object: f.object.trim(),
        sourceDocumentId: f.sourceDocumentId ? Number(f.sourceDocumentId) : null,
      }
      const { data } = await ragApi.post('/graph/triple', payload)
      if (data.code === 200 || data.success) {
        message.success('三元组已写入')
        await loadList()
        setForm((prev) => ({ ...prev, subject: '', predicate: '', object: '', sourceDocumentId: '' }))
      } else {
        message.error(data.message || '写入失败')
      }
    } catch (e) {
      message.error('写入失败：' + (e.response?.data?.message || e.message))
    } finally {
      setSaving(false)
    }
  }

  async function query() {
    if (!q.entity.trim()) {
      message.warning('请输入实体')
      return
    }
    setLoading(true)
    setHits([])
    setQueried(false)
    try {
      const { data } = await ragApi.get('/graph/query', {
        params: {
          entity: q.entity.trim(),
          depth: q.depth,
          knowledgeBase: q.knowledgeBase.trim(),
        },
      })
      if (data.code === 200 || data.success) {
        setHits(data.data?.triples || [])
      } else {
        message.error(data.message || '检索失败')
      }
      setQueried(true)
    } catch (e) {
      message.error('检索失败：' + (e.response?.data?.message || e.message))
      setQueried(true)
    } finally {
      setLoading(false)
    }
  }

  async function loadList() {
    setListLoading(true)
    try {
      const { data } = await ragApi.get('/graph/triples', {
        params: { knowledgeBase: form.knowledgeBase.trim() },
      })
      if (data.code === 200 || data.success) {
        setTriples(data.data || [])
      } else {
        message.error(data.message || '加载失败')
      }
    } catch (e) {
      message.error('加载失败：' + (e.response?.data?.message || e.message))
    } finally {
      setListLoading(false)
    }
  }

  return (
    <div className="graph-page">
      <Row gutter={16}>
        {/* 新增三元组 */}
        <Col span={10}>
          <Card hoverable title="新增三元组（主体-关系-客体）">
            <Form labelCol={{ span: 5 }} wrapperCol={{ span: 19 }}>
              <Form.Item label="知识库">
                <Input
                  value={form.knowledgeBase}
                  onChange={(e) => setField('knowledgeBase', e.target.value)}
                  placeholder="knowledge"
                />
              </Form.Item>
              <Form.Item label="主体">
                <Input
                  value={form.subject}
                  onChange={(e) => setField('subject', e.target.value)}
                  placeholder="如：退款政策"
                />
              </Form.Item>
              <Form.Item label="关系">
                <Input
                  value={form.predicate}
                  onChange={(e) => setField('predicate', e.target.value)}
                  placeholder="如：指向"
                />
              </Form.Item>
              <Form.Item label="客体">
                <Input
                  value={form.object}
                  onChange={(e) => setField('object', e.target.value)}
                  placeholder="如：申请入口"
                />
              </Form.Item>
              <Form.Item label="来源文档">
                <Input
                  value={form.sourceDocumentId}
                  onChange={(e) => setField('sourceDocumentId', e.target.value)}
                  placeholder="文档ID（可选）"
                />
              </Form.Item>
              <Form.Item wrapperCol={{ offset: 5, span: 19 }}>
                <Button type="primary" loading={saving} onClick={saveTriple}>
                  写入图谱
                </Button>
              </Form.Item>
            </Form>
          </Card>
        </Col>

        {/* 多跳检索 */}
        <Col span={14}>
          <Card hoverable title="多跳图谱检索">
            <Form layout="inline">
              <Form.Item label="知识库">
                <Input
                  value={q.knowledgeBase}
                  onChange={(e) => setQ((prev) => ({ ...prev, knowledgeBase: e.target.value }))}
                  placeholder="knowledge"
                  style={{ width: 160 }}
                />
              </Form.Item>
              <Form.Item label="实体">
                <Input
                  value={q.entity}
                  onChange={(e) => setQ((prev) => ({ ...prev, entity: e.target.value }))}
                  placeholder="如：退款政策"
                  style={{ width: 180 }}
                  onPressEnter={query}
                />
              </Form.Item>
              <Form.Item label="深度">
                <InputNumber
                  min={1}
                  max={4}
                  value={q.depth}
                  onChange={(v) => setQ((prev) => ({ ...prev, depth: v ?? 1 }))}
                />
              </Form.Item>
              <Form.Item>
                <Button type="primary" icon={<SearchOutlined />} loading={loading} onClick={query}>
                  检索
                </Button>
              </Form.Item>
            </Form>

            {hits.length > 0 && <Divider />}
            {hits.length > 0 ? (
              <div className="hop-flow">
                {hits.map((t, i) => (
                  <div className="triple" key={t.id || i}>
                    <Tag color="blue">{t.subject}</Tag>
                    <span className="rel">— {t.predicate} →</span>
                    <Tag color="green">{t.object}</Tag>
                  </div>
                ))}
              </div>
            ) : (
              queried && <Empty description="该实体未命中图谱" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            )}
          </Card>
        </Col>
      </Row>

      {/* 三元组列表 */}
      <Card
        hoverable
        style={{ marginTop: 16 }}
        title={
          <div className="list-header">
            <span>三元组列表</span>
            <Button size="small" icon={<ReloadOutlined />} loading={listLoading} onClick={loadList}>
              刷新
            </Button>
          </div>
        }
      >
        <Table
          rowKey={(r, i) => r.id ?? i}
          columns={TRIPLE_COLUMNS}
          dataSource={triples}
          size="small"
          bordered
          pagination={false}
          locale={{ emptyText: <Empty description="暂无三元组" /> }}
        />
      </Card>
    </div>
  )
}
