import { useEffect, useState } from 'react'
import { Button, Card, Col, Empty, Input, Menu, Modal, Row, Table, Tag, message } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import { promptApiWrappers } from '../api'

export default function PromptView() {
  const [scenarios, setScenarios] = useState([])
  const [filter, setFilter] = useState('')
  const [activeScenario, setActiveScenario] = useState('')
  const [versions, setVersions] = useState([])
  const [activeVersion, setActiveVersion] = useState(null)
  const [loadingVersions, setLoadingVersions] = useState(false)

  const [showPreview, setShowPreview] = useState(false)
  const [previewVersion, setPreviewVersion] = useState('')
  const [previewContent, setPreviewContent] = useState('')

  useEffect(() => {
    loadScenarios()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function loadScenarios() {
    try {
      const resp = await promptApiWrappers.list()
      const data = resp?.data?.data ?? resp?.data ?? []
      const list = Array.isArray(data) ? data : []
      setScenarios(list)
      if (list.length > 0) {
        selectScenario(list[0].scenario)
      }
    } catch (e) {
      message.error('加载 Prompt 场景失败: ' + (e.message || ''))
    }
  }

  async function selectScenario(scenario) {
    setActiveScenario(scenario)
    await loadVersions(scenario)
  }

  async function loadVersions(scenario = activeScenario) {
    if (!scenario) return
    setLoadingVersions(true)
    try {
      const resp = await promptApiWrappers.listVersions(scenario)
      const data = resp?.data?.data ?? resp?.data ?? {}
      setVersions(data.versions || [])
      setActiveVersion(data.activeVersion ?? null)
    } catch (e) {
      message.error('加载版本失败: ' + (e.message || ''))
    } finally {
      setLoadingVersions(false)
    }
  }

  async function setActive(version) {
    try {
      await promptApiWrappers.setActive(activeScenario, version)
      message.success(`已将 ${activeScenario} 切换至 v${version}`)
      await loadVersions()
    } catch (e) {
      message.error('切换失败: ' + (e.message || ''))
    }
  }

  function openVersionPreview(row) {
    setPreviewVersion(row.version)
    setPreviewContent(row.content || '')
    setShowPreview(true)
  }

  const f = filter.trim().toLowerCase()
  const filteredScenarios = f
    ? scenarios.filter((s) => s.scenario.toLowerCase().includes(f))
    : scenarios

  const columns = [
    { title: '版本', dataIndex: 'version', width: 90 },
    {
      title: '生效',
      width: 80,
      render: (_, row) =>
        row.version === activeVersion ? <Tag color="success">生效中</Tag> : <span>-</span>,
    },
    {
      title: '内容长度',
      width: 100,
      render: (_, row) => row.contentLength ?? '-',
    },
    {
      title: '操作',
      width: 180,
      render: (_, row) => (
        <>
          <Button
            size="small"
            type="primary"
            disabled={row.version === activeVersion}
            onClick={() => setActive(row.version)}
          >
            设为生效
          </Button>
          <Button size="small" type="link" onClick={() => openVersionPreview(row)}>
            预览
          </Button>
        </>
      ),
    },
  ]

  return (
    <div className="prompt-page">
      <Card
        hoverable
        title={
          <div className="header">
            <span className="title">Prompt 配置管理</span>
            <Tag>配置化版本管理 · 热切换无需重启</Tag>
          </div>
        }
      >
        <Row gutter={16}>
          {/* 左：场景列表 */}
          <Col span={8}>
            <Input
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
              placeholder="筛选场景"
              allowClear
              size="small"
              style={{ marginBottom: 12 }}
            />
            {filteredScenarios.length > 0 ? (
              <Menu
                className="scenario-menu"
                mode="inline"
                selectedKeys={[activeScenario]}
                onClick={({ key }) => selectScenario(key)}
                items={filteredScenarios.map((s) => ({
                  key: s.scenario,
                  label: (
                    <div className="scenario-item">
                      <span>{s.scenario}</span>
                      {s.activeVersion ? <Tag color="success">v{s.activeVersion}</Tag> : null}
                    </div>
                  ),
                }))}
              />
            ) : (
              <Empty description="暂无场景" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            )}
          </Col>

          {/* 右：版本列表 + 内容预览 */}
          <Col span={16}>
            {!activeScenario ? (
              <div className="placeholder">
                <Empty description="选择左侧场景查看 Prompt 版本" />
              </div>
            ) : (
              <>
                <div className="version-header">
                  <span className="scenario-name">{activeScenario}</span>
                  <Button
                    size="small"
                    icon={<ReloadOutlined />}
                    loading={loadingVersions}
                    onClick={() => loadVersions()}
                  >
                    刷新
                  </Button>
                </div>

                <Table
                  rowKey="version"
                  columns={columns}
                  dataSource={versions}
                  size="small"
                  bordered
                  pagination={false}
                />
              </>
            )}
          </Col>
        </Row>

        <Modal
          open={showPreview}
          title={`${activeScenario} · 版本 ${previewVersion}`}
          width={720}
          footer={null}
          onCancel={() => setShowPreview(false)}
        >
          <pre className="prompt-content">{previewContent || '（无内容）'}</pre>
        </Modal>
      </Card>
    </div>
  )
}
