<template>
  <div class="graph-page">
    <el-row :gutter="16">
      <!-- 新增三元组 -->
      <el-col :span="10">
        <el-card shadow="hover">
          <template #header><span>新增三元组（主体-关系-客体）</span></template>
          <el-form label-width="80px">
            <el-form-item label="知识库">
              <el-input v-model="form.knowledgeBase" placeholder="knowledge" />
            </el-form-item>
            <el-form-item label="主体">
              <el-input v-model="form.subject" placeholder="如：退款政策" />
            </el-form-item>
            <el-form-item label="关系">
              <el-input v-model="form.predicate" placeholder="如：指向" />
            </el-form-item>
            <el-form-item label="客体">
              <el-input v-model="form.object" placeholder="如：申请入口" />
            </el-form-item>
            <el-form-item label="来源文档">
              <el-input v-model="form.sourceDocumentId" placeholder="文档ID（可选）" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="saveTriple">写入图谱</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 多跳检索 -->
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header><span>多跳图谱检索</span></template>
          <el-form inline>
            <el-form-item label="知识库">
              <el-input v-model="q.knowledgeBase" placeholder="knowledge" style="width: 160px" />
            </el-form-item>
            <el-form-item label="实体">
              <el-input v-model="q.entity" placeholder="如：退款政策" style="width: 180px" @keyup.enter="query" />
            </el-form-item>
            <el-form-item label="深度">
              <el-input-number v-model="q.depth" :min="1" :max="4" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" :icon="Search" @click="query">检索</el-button>
            </el-form-item>
          </el-form>

          <el-divider v-if="hits.length" />
          <div v-if="hits.length" class="hop-flow">
            <div v-for="(t, i) in hits" :key="t.id || i" class="triple">
              <el-tag type="primary" effect="plain">{{ t.subject }}</el-tag>
              <span class="rel">— {{ t.predicate }} →</span>
              <el-tag type="success" effect="plain">{{ t.object }}</el-tag>
            </div>
          </div>
          <el-empty v-else-if="queried" description="该实体未命中图谱" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 三元组列表 -->
    <el-card shadow="hover" style="margin-top: 16px">
      <template #header>
        <div class="list-header">
          <span>三元组列表</span>
          <el-button size="small" :icon="Refresh" :loading="listLoading" @click="loadList">刷新</el-button>
        </div>
      </template>
      <el-table :data="triples" stripe size="small" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="subject" label="主体" min-width="160" />
        <el-table-column prop="predicate" label="关系" width="140" />
        <el-table-column prop="object" label="客体" min-width="160" />
        <el-table-column prop="sourceDocumentId" label="来源文档" width="110" />
        <el-table-column prop="knowledgeBase" label="知识库" width="120" />
      </el-table>
      <el-empty v-if="!triples.length && !listLoading" description="暂无三元组" :image-size="80" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ragApi } from '../api'
import { ElMessage } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'

const form = ref({ knowledgeBase: 'knowledge', subject: '', predicate: '', object: '', sourceDocumentId: '' })
const saving = ref(false)

const q = ref({ knowledgeBase: 'knowledge', entity: '', depth: 2 })
const hits = ref([])
const queried = ref(false)
const loading = ref(false)

const triples = ref([])
const listLoading = ref(false)

onMounted(loadList)

async function saveTriple() {
  const f = form.value
  if (!f.subject.trim() || !f.predicate.trim() || !f.object.trim()) {
    return ElMessage.warning('请填写主体、关系、客体')
  }
  saving.value = true
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
      ElMessage.success('三元组已写入')
      await loadList()
      form.value.subject = form.value.predicate = form.value.object = form.value.sourceDocumentId = ''
    } else ElMessage.error(data.message || '写入失败')
  } catch (e) {
    ElMessage.error('写入失败：' + (e.response?.data?.message || e.message))
  } finally {
    saving.value = false
  }
}

async function query() {
  if (!q.value.entity.trim()) return ElMessage.warning('请输入实体')
  loading.value = true
  hits.value = []
  queried.value = false
  try {
    const { data } = await ragApi.get('/graph/query', {
      params: { entity: q.value.entity.trim(), depth: q.value.depth, knowledgeBase: q.value.knowledgeBase.trim() }
    })
    if (data.code === 200 || data.success) {
      hits.value = (data.data?.triples) || []
    } else ElMessage.error(data.message || '检索失败')
    queried.value = true
  } catch (e) {
    ElMessage.error('检索失败：' + (e.response?.data?.message || e.message))
    queried.value = true
  } finally {
    loading.value = false
  }
}

async function loadList() {
  listLoading.value = true
  try {
    const { data } = await ragApi.get('/graph/triples', { params: { knowledgeBase: form.value.knowledgeBase.trim() } })
    if (data.code === 200 || data.success) {
      triples.value = data.data || []
    } else ElMessage.error(data.message || '加载失败')
  } catch (e) {
    ElMessage.error('加载失败：' + (e.response?.data?.message || e.message))
  } finally {
    listLoading.value = false
  }
}
</script>

<style scoped>
.graph-page { padding: 16px; }
.list-header { display: flex; align-items: center; justify-content: space-between; }
.hop-flow { display: flex; flex-direction: column; gap: 8px; }
.triple { display: flex; align-items: center; gap: 8px; }
.rel { font-size: 13px; color: #909399; }
</style>
