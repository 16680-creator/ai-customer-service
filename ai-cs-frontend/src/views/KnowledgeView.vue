<template>
  <div class="knowledge-view">
    <el-card shadow="hover">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span style="font-weight: 600">知识库文档管理</span>
          <el-button type="primary" @click="showCreateDialog" :icon="Plus">新增文档</el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div style="margin-bottom: 16px; display: flex; gap: 10px">
        <el-input v-model="keyword" placeholder="搜索文档..." clearable @clear="fetchList" style="width: 300px" />
        <el-button type="primary" @click="fetchList" :icon="Search">搜索</el-button>
      </div>

      <!-- 文档表格 -->
      <el-table :data="documents" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewDoc(row)" :icon="View" circle />
            <el-button size="small" type="primary" @click="editDoc(row)" :icon="Edit" circle />
            <el-popconfirm title="确认删除?" @confirm="deleteDoc(row.id)">
              <template #reference>
                <el-button size="small" type="danger" :icon="Delete" circle />
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 16px; display: flex; justify-content: flex-end">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchList"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editingDoc ? '编辑文档' : '新增文档'" width="600px">
      <el-form :model="docForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="docForm.title" placeholder="文档标题" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="docForm.category" placeholder="文档分类" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="docForm.content" type="textarea" :rows="6" placeholder="文档内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDoc">保存</el-button>
      </template>
    </el-dialog>

    <!-- 查看详情弹窗 -->
    <el-dialog v-model="viewVisible" title="文档详情" width="600px">
      <el-descriptions :column="1" border v-if="viewDoc_data">
        <el-descriptions-item label="ID">{{ viewDoc_data.id }}</el-descriptions-item>
        <el-descriptions-item label="标题">{{ viewDoc_data.title }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ viewDoc_data.category }}</el-descriptions-item>
        <el-descriptions-item label="内容">{{ viewDoc_data.content }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ viewDoc_data.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { knowledgeApi } from '../api'
import { ElMessage } from 'element-plus'
import { Plus, Search, View, Edit, Delete } from '@element-plus/icons-vue'

const documents = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')
const dialogVisible = ref(false)
const viewVisible = ref(false)
const editingDoc = ref(null)
const viewDoc_data = ref(null)
const docForm = ref({ title: '', category: '', content: '', status: 1 })

onMounted(fetchList)

async function fetchList() {
  try {
    const res = await knowledgeApi.get('/knowledge/list', { params: { page: page.value, pageSize: pageSize.value, keyword: keyword.value || undefined } })
    const data = res.data?.data
    documents.value = data?.records || data || []
    total.value = data?.total || documents.value.length
  } catch (e) {
    ElMessage.error('加载文档列表失败: ' + (e.response?.data?.message || e.message))
  }
}

function showCreateDialog() {
  editingDoc.value = null
  docForm.value = { title: '', category: '', content: '', status: 1 }
  dialogVisible.value = true
}

function editDoc(row) {
  editingDoc.value = row
  docForm.value = { ...row }
  dialogVisible.value = true
}

function viewDoc(row) {
  viewDoc_data.value = row
  viewVisible.value = true
}

async function saveDoc() {
  try {
    if (editingDoc.value) {
      await knowledgeApi.put('/knowledge', docForm.value)
    } else {
      await knowledgeApi.post('/knowledge', docForm.value)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchList()
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.response?.data?.message || e.message))
  }
}

async function deleteDoc(id) {
  try {
    await knowledgeApi.delete(`/knowledge/${id}`)
    ElMessage.success('删除成功')
    fetchList()
  } catch (e) {
    ElMessage.error('删除失败: ' + (e.response?.data?.message || e.message))
  }
}
</script>
