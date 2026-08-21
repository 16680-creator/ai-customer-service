<template>
  <div class="rag-kb-view">
    <h2>向量知识库（RAG）</h2>
    <el-alert
      type="info"
      :closable="false"
      title="将资料入库后，可在 AI 对话页选择「RAG 知识库对话」模式，基于知识库内容回答问题。"
      style="margin-bottom: 16px"
    />
    <el-alert
      type="success"
      :closable="false"
      title="知识库文档已自动向量化：知识库页创建的文档会自动入库，在 RAG 对话时知识库标识填 knowledge 即可检索。"
      style="margin-bottom: 16px"
    />

    <el-row :gutter="16">
      <!-- 文本入库 -->
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span>文本入库</span></template>
          <el-form label-width="90px">
            <el-form-item label="知识库标识">
              <el-input v-model="kbName" placeholder="如 product-manual" />
            </el-form-item>
            <el-form-item label="文本内容">
              <el-input v-model="kbText" type="textarea" :rows="8" placeholder="输入要入库的文本..." />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="addingText" @click="addText">提交入库</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 文件入库 -->
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span>文件入库（PDF/TXT/MD/Office/HTML）</span></template>
          <el-form label-width="90px">
            <el-form-item label="知识库标识">
              <el-input v-model="uploadKbName" placeholder="如 product-manual" />
            </el-form-item>
            <el-form-item label="选择文件">
              <el-upload
                :auto-upload="false"
                :limit="1"
                :on-change="handleFileChange"
                accept=".pdf,.txt,.md,.markdown,.docx,.xlsx,.html,.htm"
              >
                <el-button type="primary" plain>选择文件</el-button>
              </el-upload>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="addingFile" @click="uploadFile">上传入库</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 检索测试 -->
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span>语义检索测试</span></template>
          <el-form label-width="90px">
            <el-form-item label="知识库标识">
              <el-input v-model="searchKbName" placeholder="如 product-manual 或 knowledge（知识库）">
                <template #append>
                  <el-button @click="searchKbName = 'knowledge'">知识库</el-button>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="检索模式">
              <el-select v-model="searchMode" style="width: 100%">
                <el-option label="VECTOR（纯向量）" value="VECTOR" />
                <el-option label="HYBRID（混合）" value="HYBRID" />
                <el-option label="RERANK（重排）" value="RERANK" />
              </el-select>
            </el-form-item>
            <el-form-item label="TopK">
              <el-input-number v-model="searchTopK" :min="1" :max="20" />
            </el-form-item>
            <el-form-item label="检索问题">
              <el-input v-model="searchQuery" placeholder="输入问题..." @keyup.enter="doSearch" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="searching" @click="doSearch">检索</el-button>
              <el-tag v-if="degraded" type="warning" size="small" style="margin-left: 8px">已降级（回退向量检索）</el-tag>
            </el-form-item>
          </el-form>
          <el-divider v-if="searchResults.length" />
          <div v-for="(r, i) in searchResults" :key="i" class="search-result">
            <div class="score">相似度：{{ Number(r.score).toFixed(4) }}<span v-if="r.source" class="src"> · {{ r.source }}</span></div>
            <div class="text">{{ r.text }}</div>
          </div>
          <el-empty v-if="searched && !searchResults.length" description="未命中相关文档" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ragApi } from '../api'

// 文本入库
const kbName = ref('')
const kbText = ref('')
const addingText = ref(false)
async function addText() {
  if (!kbName.value.trim() || !kbText.value.trim()) return ElMessage.warning('请填写知识库标识和文本内容')
  addingText.value = true
  try {
    const { data } = await ragApi.post('/knowledge-base/text', null, {
      params: { knowledgeBase: kbName.value.trim(), text: kbText.value.trim() }
    })
    if (data.code === 200) {
      ElMessage.success(`入库成功，共 ${data.data.chunks} 个分块`)
      kbText.value = ''
    } else ElMessage.error(data.message)
  } catch (e) {
    ElMessage.error('入库失败：' + (e.response?.data?.message || e.message))
  } finally {
    addingText.value = false
  }
}

// 文件入库
const uploadKbName = ref('')
const selectedFile = ref(null)
const addingFile = ref(false)
function handleFileChange(file) {
  selectedFile.value = file.raw
}
async function uploadFile() {
  if (!uploadKbName.value.trim() || !selectedFile.value) return ElMessage.warning('请填写知识库标识并选择文件')
  const form = new FormData()
  form.append('knowledgeBase', uploadKbName.value.trim())
  form.append('file', selectedFile.value)
  addingFile.value = true
  try {
    const { data } = await ragApi.post('/knowledge-base/upload', form)
    if (data.code === 200) {
      ElMessage.success(`文件入库成功，共 ${data.data.chunks} 个分块`)
      selectedFile.value = null
    } else ElMessage.error(data.message)
  } catch (e) {
    ElMessage.error('上传失败：' + (e.response?.data?.message || e.message))
  } finally {
    addingFile.value = false
  }
}

// 检索
const searchKbName = ref('')
const searchQuery = ref('')
const searchMode = ref('VECTOR')
const searchTopK = ref(5)
const searchResults = ref([])
const searched = ref(false)
const searching = ref(false)
const degraded = ref(false)
async function doSearch() {
  if (!searchKbName.value.trim() || !searchQuery.value.trim()) return ElMessage.warning('请填写知识库标识和检索问题')
  searching.value = true
  degraded.value = false
  try {
    const { data } = await ragApi.get('/retrieve/test', {
      params: {
        knowledgeBase: searchKbName.value.trim(),
        query: searchQuery.value.trim(),
        mode: searchMode.value,
        topK: searchTopK.value,
      }
    })
    if (data.code === 200) {
      const payload = data.data || {}
      searchResults.value = payload.documents || []
      degraded.value = !!payload.degraded
    } else ElMessage.error(data.message)
    searched.value = true
  } catch (e) {
    ElMessage.error('检索失败：' + (e.response?.data?.message || e.message))
  } finally {
    searching.value = false
  }
}
</script>

<style scoped>
.rag-kb-view { padding: 20px; }
.search-result {
  padding: 8px 10px; border-radius: 6px; background: #f5f7fa; margin-bottom: 8px;
}
.score { color: #e6a23c; font-size: 12px; margin-bottom: 4px; }
.text { font-size: 13px; color: #606266; line-height: 1.6; }
</style>