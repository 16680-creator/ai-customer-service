<!--
  知识库运营看板（US6 知识库运营闭环的前端落地）
  学习要点：
  1. 闭环：聚类主题 → 缺口标记(gapFlag) → 一键收录 FAQ → 触发知识向量更新
  2. report.status = INSUFFICIENT_DATA 表示提问样本不足（<20 条），提示而非报错
  3. hitRate = 主题代表问题在知识库的检索命中率，低命中=知识库缺口
-->
<template>
  <div class="knowledge-ops">
    <el-card shadow="hover">
      <template #header>
        <span style="font-weight: 600">知识库运营看板（高频问题聚类 + 缺口识别）</span>
      </template>
      <el-form inline>
        <el-form-item label="周期">
          <el-input v-model="period" placeholder="如 2026-08-01~2026-08-12" style="width: 220px" />
        </el-form-item>
        <el-form-item label="提问(JSON)">
          <el-input v-model="questionsText" type="textarea" :rows="4" style="width: 460px"
                    placeholder='[{"id":1,"text":"怎么退款？"},{"id":2,"text":"退款多久到账？"}]' />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="runCluster" :loading="loading">运行聚类</el-button>
          <el-button @click="loadSample">示例数据</el-button>
        </el-form-item>
      </el-form>

      <template v-if="report">
        <el-alert v-if="report.status === 'INSUFFICIENT_DATA'" type="warning" :closable="false"
                  title="提问数据不足（至少 20 条），无法聚类" />
        <template v-else>
          <el-divider content-position="left">
            <el-tag type="info">主题 {{ report.topics?.length || 0 }} 个 · 缺口 {{ report.gapTopics?.length || 0 }} 个</el-tag>
          </el-divider>
          <el-table :data="report.topics" border>
            <el-table-column prop="topic" label="主题" min-width="220" />
            <el-table-column prop="count" label="次数" width="80" />
            <el-table-column prop="ratio" label="占比" width="90">
              <template #default="{ row }">{{ (row.ratio * 100).toFixed(1) }}%</template>
            </el-table-column>
            <el-table-column label="知识库命中率" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.hitRate != null" :type="row.gapFlag ? 'danger' : 'success'">
                  {{ (row.hitRate * 100).toFixed(0) }}%
                </el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="缺口" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.gapFlag" type="danger">缺口</el-tag>
                <el-tag v-else type="success">正常</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="adoptFaq(row)">收录 FAQ</el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { knowledgeApi } from '../api'

const period = ref('2026-08-01~2026-08-12')  // 统计周期
const questionsText = ref('')                 // 提问 JSON（[{id, text}]）
const report = ref(null)                      // 聚类报告（主题+缺口）
const loading = ref(false)

/** 生成 24 条示例提问（两个语义相近的退款问题），便于体验聚类 */
function loadSample() {
  const samples = []
  for (let i = 1; i <= 24; i++) {
    if (i % 2 === 1) samples.push({ id: i, text: '怎么申请退款？' })
    else samples.push({ id: i, text: '退款多久能到账？' })
  }
  questionsText.value = JSON.stringify(samples)
}

/** 调用后端 /knowledge/ops/cluster 运行聚类 + 缺口分析 */
async function runCluster() {
  let questions = []
  try {
    questions = questionsText.value ? JSON.parse(questionsText.value) : []
  } catch (e) {
    ElMessage.warning('提问 JSON 格式不正确')
    return
  }
  loading.value = true
  try {
    const resp = await knowledgeApi.post('/ops/cluster', { period: period.value, questions })
    if (resp.data && resp.data.code === 200) {
      report.value = resp.data.data
    } else {
      ElMessage.error(resp.data?.message || '聚类失败')
    }
  } catch (e) {
    ElMessage.error('运营服务不可用：' + (e.message || ''))
  } finally {
    loading.value = false
  }
}

/** 一键收录 FAQ：把缺口主题转成 FAQ 知识文档，自动触发向量化 */
async function adoptFaq(row) {
  const { value } = await ElMessageBox.prompt('请输入 FAQ 答案（留空使用主题问题）', '收录 FAQ', {
    inputValue: '',
  }).catch(() => ({ value: undefined }))
  if (value === undefined) return
  try {
    const resp = await knowledgeApi.post('/ops/faq', {
      question: row.topic,
      answer: value || '（待运营补充答案）',
      knowledgeBase: 'faq',
      clusterTopicId: row.topic,
    })
    if (resp.data && resp.data.code === 200) {
      ElMessage.success('FAQ 已收录并触发向量更新')
    } else {
      ElMessage.error(resp.data?.message || '收录失败')
    }
  } catch (e) {
    ElMessage.error('收录失败：' + (e.message || ''))
  }
}
</script>