import { useState } from 'react'
import { Alert, Button, Card, Col, Divider, Empty, Form, Input, InputNumber, Row, Select, Tag, Upload, message } from 'antd'
import { ragApi } from '../api'

export default function VectorKbView() {
  // 文本入库
  const [kbName, setKbName] = useState('')
  const [kbText, setKbText] = useState('')
  const [addingText, setAddingText] = useState(false)

  // 文件入库
  const [uploadKbName, setUploadKbName] = useState('')
  const [selectedFile, setSelectedFile] = useState(null)
  const [addingFile, setAddingFile] = useState(false)

  // 检索
  const [searchKbName, setSearchKbName] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [searchMode, setSearchMode] = useState('VECTOR')
  const [searchTopK, setSearchTopK] = useState(5)
  const [searchResults, setSearchResults] = useState([])
  const [searched, setSearched] = useState(false)
  const [searching, setSearching] = useState(false)
  const [degraded, setDegraded] = useState(false)

  async function addText() {
    if (!kbName.trim() || !kbText.trim()) {
      message.warning('请填写知识库标识和文本内容')
      return
    }
    setAddingText(true)
    try {
      const { data } = await ragApi.post('/knowledge-base/text', null, {
        params: { knowledgeBase: kbName.trim(), text: kbText.trim() },
      })
      if (data.code === 200) {
        message.success(`入库成功，共 ${data.data.chunks} 个分块`)
        setKbText('')
      } else {
        message.error(data.message)
      }
    } catch (e) {
      message.error('入库失败：' + (e.response?.data?.message || e.message))
    } finally {
      setAddingText(false)
    }
  }

  async function uploadFile() {
    if (!uploadKbName.trim() || !selectedFile) {
      message.warning('请填写知识库标识并选择文件')
      return
    }
    const form = new FormData()
    form.append('knowledgeBase', uploadKbName.trim())
    form.append('file', selectedFile)
    setAddingFile(true)
    try {
      const { data } = await ragApi.post('/knowledge-base/upload', form)
      if (data.code === 200) {
        message.success(`文件入库成功，共 ${data.data.chunks} 个分块`)
        setSelectedFile(null)
      } else {
        message.error(data.message)
      }
    } catch (e) {
      message.error('上传失败：' + (e.response?.data?.message || e.message))
    } finally {
      setAddingFile(false)
    }
  }

  async function doSearch() {
    if (!searchKbName.trim() || !searchQuery.trim()) {
      message.warning('请填写知识库标识和检索问题')
      return
    }
    setSearching(true)
    setDegraded(false)
    try {
      const { data } = await ragApi.get('/retrieve/test', {
        params: {
          knowledgeBase: searchKbName.trim(),
          query: searchQuery.trim(),
          mode: searchMode,
          topK: searchTopK,
        },
      })
      if (data.code === 200) {
        const payload = data.data || {}
        setSearchResults(payload.documents || [])
        setDegraded(!!payload.degraded)
      } else {
        message.error(data.message)
      }
      setSearched(true)
    } catch (e) {
      message.error('检索失败：' + (e.response?.data?.message || e.message))
    } finally {
      setSearching(false)
    }
  }

  return (
    <div className="rag-kb-view">
      <h2>向量知识库（RAG）</h2>
      <Alert
        type="info"
        showIcon
        message="将资料入库后，可在 AI 对话页选择「RAG 知识库对话」模式，基于知识库内容回答问题。"
        style={{ margin: '16px 0' }}
      />
      <Alert
        type="success"
        showIcon
        message="知识库文档已自动向量化：知识库页创建的文档会自动入库，在 RAG 对话时知识库标识填 knowledge 即可检索。"
        style={{ marginBottom: 16 }}
      />

      <Row gutter={16}>
        {/* 文本入库 */}
        <Col span={8}>
          <Card hoverable title="文本入库">
            <Form labelCol={{ span: 7 }} wrapperCol={{ span: 17 }}>
              <Form.Item label="知识库标识">
                <Input
                  value={kbName}
                  onChange={(e) => setKbName(e.target.value)}
                  placeholder="如 product-manual"
                />
              </Form.Item>
              <Form.Item label="文本内容">
                <Input.TextArea
                  value={kbText}
                  onChange={(e) => setKbText(e.target.value)}
                  rows={8}
                  placeholder="输入要入库的文本..."
                />
              </Form.Item>
              <Form.Item wrapperCol={{ offset: 7, span: 17 }}>
                <Button type="primary" loading={addingText} onClick={addText}>
                  提交入库
                </Button>
              </Form.Item>
            </Form>
          </Card>
        </Col>

        {/* 文件入库 */}
        <Col span={8}>
          <Card hoverable title="文件入库（PDF/TXT/MD/Office/HTML）">
            <Form labelCol={{ span: 7 }} wrapperCol={{ span: 17 }}>
              <Form.Item label="知识库标识">
                <Input
                  value={uploadKbName}
                  onChange={(e) => setUploadKbName(e.target.value)}
                  placeholder="如 product-manual"
                />
              </Form.Item>
              <Form.Item label="选择文件">
                <Upload
                  maxCount={1}
                  accept=".pdf,.txt,.md,.markdown,.docx,.xlsx,.html,.htm"
                  beforeUpload={(file) => {
                    setSelectedFile(file)
                    return false
                  }}
                  onRemove={() => setSelectedFile(null)}
                  fileList={
                    selectedFile
                      ? [{ uid: '-1', name: selectedFile.name, status: 'done' }]
                      : []
                  }
                >
                  <Button>选择文件</Button>
                </Upload>
              </Form.Item>
              <Form.Item wrapperCol={{ offset: 7, span: 17 }}>
                <Button type="primary" loading={addingFile} onClick={uploadFile}>
                  上传入库
                </Button>
              </Form.Item>
            </Form>
          </Card>
        </Col>

        {/* 检索测试 */}
        <Col span={8}>
          <Card hoverable title="语义检索测试">
            <Form labelCol={{ span: 7 }} wrapperCol={{ span: 17 }}>
              <Form.Item label="知识库标识">
                <Input
                  value={searchKbName}
                  onChange={(e) => setSearchKbName(e.target.value)}
                  placeholder="如 product-manual 或 knowledge（知识库）"
                  addonAfter={
                    <Button type="link" size="small" onClick={() => setSearchKbName('knowledge')}>
                      知识库
                    </Button>
                  }
                />
              </Form.Item>
              <Form.Item label="检索模式">
                <Select
                  value={searchMode}
                  onChange={setSearchMode}
                  style={{ width: '100%' }}
                  options={[
                    { label: 'VECTOR（纯向量）', value: 'VECTOR' },
                    { label: 'HYBRID（混合）', value: 'HYBRID' },
                    { label: 'RERANK（重排）', value: 'RERANK' },
                  ]}
                />
              </Form.Item>
              <Form.Item label="TopK">
                <InputNumber
                  min={1}
                  max={20}
                  value={searchTopK}
                  onChange={(v) => setSearchTopK(v ?? 5)}
                />
              </Form.Item>
              <Form.Item label="检索问题">
                <Input
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="输入问题..."
                  onPressEnter={doSearch}
                />
              </Form.Item>
              <Form.Item wrapperCol={{ offset: 7, span: 17 }}>
                <Button type="primary" loading={searching} onClick={doSearch}>
                  检索
                </Button>
                {degraded && (
                  <Tag color="warning" style={{ marginLeft: 8 }}>
                    已降级（回退向量检索）
                  </Tag>
                )}
              </Form.Item>
            </Form>

            {searchResults.length > 0 && <Divider />}
            {searchResults.map((r, i) => (
              <div className="search-result" key={i}>
                <div className="score">
                  相似度：{Number(r.score).toFixed(4)}
                  {r.source && <span> · {r.source}</span>}
                </div>
                <div className="text">{r.text}</div>
              </div>
            ))}
            {searched && searchResults.length === 0 && (
              <Empty description="未命中相关文档" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            )}
          </Card>
        </Col>
      </Row>
    </div>
  )
}
