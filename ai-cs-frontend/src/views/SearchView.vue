<template>
  <div class="search-view">
    <el-row :gutter="20">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header><span style="font-weight: 600">全文搜索 (Elasticsearch)</span></template>

          <div style="display: flex; gap: 10px; margin-bottom: 20px">
            <el-input v-model="indexName" placeholder="索引名称" style="width: 200px" />
            <el-input v-model="searchQuery" placeholder="搜索关键词..." clearable style="flex: 1" @keydown.enter="doSearch" />
            <el-button type="primary" @click="doSearch" :loading="searching" :icon="Search">搜索</el-button>
          </div>

          <!-- 搜索结果 -->
          <div v-if="results.length > 0" class="result-list">
            <div v-for="(item, i) in results" :key="i" class="result-item">
              <div class="result-title">{{ item.title || item._id || '文档 ' + (i + 1) }}</div>
              <div class="result-content">{{ item.content || item.highlight || JSON.stringify(item) }}</div>
              <div class="result-meta">
                <el-tag size="small" type="info">{{ item._index || indexName }}</el-tag>
                <span class="result-score" v-if="item._score">相关度: {{ item._score?.toFixed(2) }}</span>
              </div>
            </div>
          </div>
          <el-empty v-else-if="searched" description="未找到相关结果" />
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span style="font-weight: 600">索引管理</span></template>

          <el-form label-width="80px">
            <el-form-item label="索引名称">
              <el-input v-model="indexMgmt.name" placeholder="索引名称" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="createIndex" :icon="Plus">创建索引</el-button>
              <el-popconfirm title="确认删除索引?" @confirm="deleteIndex">
                <template #reference>
                  <el-button type="danger" :icon="Delete">删除索引</el-button>
                </template>
              </el-popconfirm>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="hover" style="margin-top: 20px">
          <template #header><span style="font-weight: 600">索引文档</span></template>
          <el-form label-width="80px">
            <el-form-item label="索引名称">
              <el-input v-model="docIndex.name" placeholder="索引名称" />
            </el-form-item>
            <el-form-item label="文档内容">
              <el-input v-model="docIndex.content" type="textarea" :rows="5" placeholder='JSON 格式，如 {"title":"xxx","content":"xxx"}' />
            </el-form-item>
            <el-form-item>
              <el-button type="success" @click="indexDoc" :icon="Document">索引文档</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { searchApi } from '../api'
import { ElMessage } from 'element-plus'
import { Search, Plus, Delete, Document } from '@element-plus/icons-vue'

const indexName = ref('knowledge')
const searchQuery = ref('')
const results = ref([])
const searching = ref(false)
const searched = ref(false)
const page = ref(1)

const indexMgmt = ref({ name: '' })
const docIndex = ref({ name: '', content: '' })

async function doSearch() {
  if (!searchQuery.value.trim()) return
  searching.value = true
  searched.value = true
  try {
    const res = await searchApi.get(`/search/${indexName.value}`, {
      params: { query: searchQuery.value, page: page.value, size: 20 }
    })
    results.value = res.data?.data || []
    ElMessage.success(`找到 ${results.value.length} 条结果`)
  } catch (e) {
    results.value = []
    ElMessage.error('搜索失败: ' + (e.response?.data?.message || e.message))
  } finally {
    searching.value = false
  }
}

async function createIndex() {
  if (!indexMgmt.value.name) return ElMessage.warning('请输入索引名称')
  try {
    await searchApi.post(`/search/index/${indexMgmt.value.name}`, {})
    ElMessage.success('索引创建成功')
  } catch (e) {
    ElMessage.error('创建失败: ' + (e.response?.data?.message || e.message))
  }
}

async function deleteIndex() {
  if (!indexMgmt.value.name) return ElMessage.warning('请输入索引名称')
  try {
    await searchApi.delete(`/search/index/${indexMgmt.value.name}`)
    ElMessage.success('索引删除成功')
  } catch (e) {
    ElMessage.error('删除失败: ' + (e.response?.data?.message || e.message))
  }
}

async function indexDoc() {
  if (!docIndex.value.name || !docIndex.value.content) return ElMessage.warning('请填写完整')
  try {
    const doc = JSON.parse(docIndex.value.content)
    await searchApi.post(`/search/document/${docIndex.value.name}`, doc)
    ElMessage.success('文档索引成功')
    docIndex.value.content = ''
  } catch (e) {
    if (e instanceof SyntaxError) return ElMessage.error('文档内容必须是合法 JSON')
    ElMessage.error('索引失败: ' + (e.response?.data?.message || e.message))
  }
}
</script>

<style scoped>
.result-list { display: flex; flex-direction: column; gap: 16px; }
.result-item { padding: 16px; background: #fafafa; border-radius: 8px; border-left: 4px solid #409eff; }
.result-title { font-size: 16px; font-weight: 600; color: #1d1e2c; margin-bottom: 6px; }
.result-content { font-size: 14px; color: #606266; line-height: 1.6; margin-bottom: 8px; }
.result-meta { display: flex; align-items: center; gap: 10px; }
.result-score { font-size: 12px; color: #909399; }
</style>
