<template>
  <div class="trace-page">
    <el-card shadow="hover" class="query-card">
      <div class="query-bar">
        <el-input
          v-model="requestId"
          placeholder="输入 requestId 查询一次请求的完整调用链"
          clearable
          style="max-width: 480px"
          @keyup.enter="queryTrace"
        >
          <template #prepend>requestId</template>
        </el-input>
        <el-button type="primary" :icon="Search" :loading="loading" @click="queryTrace">查询链路</el-button>
        <el-button :icon="RefreshLeft" @click="requestId = ''">清空</el-button>
      </div>
      <el-alert
        v-if="notFound"
        type="warning"
        :closable="false"
        title="未找到该 requestId 的调用链"
        description="可观测性服务仅保留最近一段时间内的 trace；若请求较久或后端未开启采样，可能查不到。"
        show-icon
      />
      <el-alert
        v-if="loadError"
        type="error"
        :closable="false"
        :title="'查询失败: ' + loadError"
        show-icon
      />
    </el-card>

    <template v-if="trace">
      <!-- 概览 -->
      <el-card shadow="hover" class="overview-card">
        <el-descriptions :column="4" border size="small">
          <el-descriptions-item label="requestId">{{ trace.requestId }}</el-descriptions-item>
          <el-descriptions-item label="总耗时">{{ trace.totalDurationMs }} ms</el-descriptions-item>
          <el-descriptions-item label="span 数">{{ spans.length }}</el-descriptions-item>
          <el-descriptions-item label="预估费用">${{ (trace.totalCostUsd || 0).toFixed(6) }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="trace.error" class="trace-error">
          <el-tag type="danger" size="small">执行异常</el-tag>
          <span class="error-text">{{ trace.error }}</span>
        </div>
      </el-card>

      <!-- 调用链甘特图（按 startOffset 排列） -->
      <el-card shadow="hover" class="gantt-card">
        <template #header>
          <span class="card-title">调用耗时分布（甘特图）</span>
        </template>
        <div class="gantt">
          <div v-for="(s, i) in spans" :key="i" class="gantt-row" :title="s.name">
            <div class="gantt-label" :title="s.name">{{ s.name }}</div>
            <div class="gantt-track">
              <div
                class="gantt-bar"
                :class="'type-' + (s.type || 'OTHER')"
                :style="barStyle(s)"
              >
                <span class="gantt-bar-text">{{ s.durationMs }}ms</span>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <!-- span 明细 -->
      <el-card shadow="hover" class="detail-card">
        <template #header>
          <span class="card-title">Span 明细</span>
        </template>
        <el-table :data="spans" stripe size="small" border>
          <el-table-column prop="name" label="节点" min-width="200" />
          <el-table-column prop="type" label="类型" width="110">
            <template #default="{ row }">
              <el-tag size="small" :type="typeTag(row.type)">{{ row.type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="model" label="模型" width="160" />
          <el-table-column prop="startOffsetMs" label="相对起点(ms)" width="120" />
          <el-table-column prop="durationMs" label="耗时(ms)" width="100" />
          <el-table-column prop="tokenUsage" label="Token" min-width="140" />
          <el-table-column prop="costUsd" label="费用($)" width="110">
            <template #default="{ row }">{{ row.costUsd != null ? Number(row.costUsd).toFixed(6) : '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="90">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="activeSpan = row">详情</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-dialog v-model="showSpan" title="Span 详情" width="640px">
          <pre class="span-json">{{ spanJson }}</pre>
        </el-dialog>
      </el-card>
    </template>

    <el-empty v-else-if="!loading && !notFound && !loadError" description="输入 requestId 开始追踪 LLM 调用链" />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { observabilityApiWrappers } from '../api'
import { ElMessage } from 'element-plus'
import { Search, RefreshLeft } from '@element-plus/icons-vue'

const requestId = ref('')
const trace = ref(null)
const spans = ref([])
const loading = ref(false)
const notFound = ref(false)
const loadError = ref('')
const activeSpan = ref(null)
const showSpan = ref(false)

const maxEnd = computed(() =>
  spans.value.reduce((m, s) => Math.max(m, (s.startOffsetMs || 0) + (s.durationMs || 0)), 1)
)

function barStyle(s) {
  const start = s.startOffsetMs || 0
  const dur = s.durationMs || 0
  const left = (start / maxEnd.value) * 100
  const width = Math.max((dur / maxEnd.value) * 100, 1.5)
  return { left: left + '%', width: width + '%' }
}

function typeTag(t) {
  return { LLM: 'warning', TOOL: 'success', RETRIEVE: 'info', EMBED: 'primary' }[t] || ''
}

const spanJson = computed(() =>
  activeSpan.value ? JSON.stringify(activeSpan.value, null, 2) : ''
)

watch(activeSpan, (v) => { if (v) showSpan.value = true })

async function queryTrace() {
  if (!requestId.value) return ElMessage.warning('请输入 requestId')
  loading.value = true
  notFound.value = false
  loadError.value = ''
  trace.value = null
  spans.value = []
  try {
    const resp = await observabilityApiWrappers.getTrace(requestId.value)
    const data = resp?.data?.data ?? resp?.data
    if (!data) {
      notFound.value = true
      return
    }
    trace.value = data
    // spansJson 可能是字符串(JSON)或已解析数组；spans 字段也可能直接是数组
    spans.value = normalizeSpans(data)
  } catch (e) {
    if (e?.response?.status === 404) {
      notFound.value = true
    } else {
      loadError.value = e.message || '未知错误'
    }
  } finally {
    loading.value = false
  }
}

function normalizeSpans(data) {
  if (Array.isArray(data.spans)) return data.spans
  if (data.spansJson) {
    try {
      const parsed = typeof data.spansJson === 'string' ? JSON.parse(data.spansJson) : data.spansJson
      return Array.isArray(parsed) ? parsed : []
    } catch {
      return []
    }
  }
  return []
}
</script>

<style scoped>
.trace-page { padding: 16px; }
.query-bar { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.query-card, .overview-card, .gantt-card, .detail-card { margin-bottom: 16px; }
.card-title { font-weight: 600; color: #303133; }
.trace-error { margin-top: 12px; display: flex; align-items: center; gap: 8px; }
.error-text { color: #f56c6c; font-size: 13px; }
.gantt { display: flex; flex-direction: column; gap: 8px; }
.gantt-row { display: flex; align-items: center; gap: 12px; }
.gantt-label { width: 200px; font-size: 13px; color: #606266; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex-shrink: 0; }
.gantt-track { position: relative; flex: 1; height: 22px; background: #f5f7fa; border-radius: 4px; }
.gantt-bar { position: absolute; top: 2px; height: 18px; border-radius: 3px; display: flex; align-items: center; padding: 0 6px; min-width: 30px; }
.gantt-bar-text { font-size: 11px; color: #fff; white-space: nowrap; }
.type-LLM { background: #e6a23c; }
.type-TOOL { background: #67c23a; }
.type-RETRIEVE { background: #909399; }
.type-EMBED { background: #409eff; }
.type-OTHER { background: #c0c4cc; }
.span-json { background: #f5f7fa; padding: 12px; border-radius: 6px; font-size: 12px; max-height: 420px; overflow: auto; }
</style>
