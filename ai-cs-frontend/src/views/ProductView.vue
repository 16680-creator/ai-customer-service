<template>
  <div class="product-view">
    <el-row :gutter="20">
      <!-- 左列：图片上传 + 创建商品 -->
      <el-col :span="9">
        <el-card shadow="hover">
          <template #header><span style="font-weight: 600">商品图片上传 (MinIO)</span></template>

          <el-upload
            class="image-uploader"
            drag
            :action="uploadUrl"
            :show-file-list="false"
            :on-success="onUploadSuccess"
            :on-error="onUploadError"
            accept=".jpg,.jpeg,.png,.webp,.gif"
          >
            <el-image v-if="uploadedUrl" :src="uploadedUrl" fit="contain" class="upload-preview" />
            <template v-else>
              <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
              <div class="el-upload__text">拖拽图片到此处，或 <em>点击上传</em></div>
            </template>
            <template #tip>
              <div class="el-upload__tip">仅支持 jpg/png/webp/gif，不超过 5MB</div>
            </template>
          </el-upload>

          <el-input v-model="uploadedUrl" readonly class="url-input" placeholder="上传成功后自动填充图片 URL">
            <template #append>
              <el-button :icon="CopyDocument" @click="copyUrl">复制</el-button>
            </template>
          </el-input>
        </el-card>

        <el-card shadow="hover" style="margin-top: 20px">
          <template #header><span style="font-weight: 600">创建商品</span></template>
          <el-form label-width="70px" size="default">
            <el-form-item label="商品名称">
              <el-input v-model="productForm.name" placeholder="如：无线蓝牙耳机" />
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="productForm.description" type="textarea" :rows="2" placeholder="用于向量检索的文本，尽量描述特征" />
            </el-form-item>
            <el-form-item label="价格">
              <el-input-number v-model="productForm.price" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
            <el-form-item label="库存">
              <el-input-number v-model="productForm.stock" :min="0" style="width: 100%" />
            </el-form-item>
            <el-form-item label="分类ID">
              <el-input-number v-model="productForm.categoryId" :min="1" style="width: 100%" />
            </el-form-item>
            <el-form-item label="图片">
              <el-input v-model="productForm.image" placeholder="留空使用刚上传的图片" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Plus" :loading="creating" @click="createProduct">创建并建立向量索引</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 中列：以文搜图 -->
      <el-col :span="9">
        <el-card shadow="hover">
          <template #header><span style="font-weight: 600">以文搜图（向量检索）</span></template>

          <div style="display: flex; gap: 10px; margin-bottom: 16px">
            <el-input
              v-model="searchText"
              placeholder="输入商品描述，如：降噪耳机、蓝牙音箱..."
              clearable
              @keydown.enter="searchByText"
            />
            <el-button type="primary" :icon="Search" :loading="searching" @click="searchByText">搜索</el-button>
          </div>

          <div v-if="similarResults.length > 0" class="result-list">
            <div v-for="item in similarResults" :key="item.productId" class="result-item">
              <el-image v-if="item.image" :src="item.image" fit="cover" class="result-img" />
              <div class="result-img placeholder" v-else>无图</div>
              <div class="result-info">
                <div class="result-name">{{ item.name }}</div>
                <div class="result-price">¥{{ item.price }}</div>
              </div>
              <div class="result-score">
                <el-tag size="small" :type="scoreType(item.score)">相似度 {{ item.score.toFixed(4) }}</el-tag>
              </div>
            </div>
          </div>
          <el-empty v-else-if="searched" description="未找到相似商品" />
        </el-card>
      </el-col>

      <!-- 右列：商品列表 + 商品找相似 -->
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header><span style="font-weight: 600">商品列表</span></template>

          <div class="product-item" v-for="item in products" :key="item.id">
            <el-image v-if="item.image" :src="item.image" fit="cover" class="product-img" />
            <div class="product-img placeholder" v-else>无图</div>
            <div class="product-info">
              <div class="product-name">{{ item.name }}</div>
              <div class="product-meta">¥{{ item.price }} · 库存 {{ item.stock }}</div>
            </div>
            <el-button size="small" type="success" @click="findSimilar(item.id)">找相似</el-button>
          </div>
          <el-empty v-if="products.length === 0" description="暂无商品" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { productApi } from '../api'
import { ElMessage } from 'element-plus'
import { Search, Plus, CopyDocument, UploadFilled } from '@element-plus/icons-vue'

const uploadUrl = 'http://localhost:8088/product/upload-image'
const uploadedUrl = ref('')

const productForm = ref({
  name: '',
  description: '',
  price: 99,
  stock: 100,
  categoryId: 1,
  image: '',
})
const creating = ref(false)

const searchText = ref('')
const searching = ref(false)
const searched = ref(false)
const similarResults = ref([])

const products = ref([])
const loadingProducts = ref(false)

function onUploadSuccess(res) {
  if (res.code === 200) {
    uploadedUrl.value = res.data
    productForm.value.image = res.data
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error('上传失败: ' + (res.message || '未知错误'))
  }
}

function onUploadError() {
  ElMessage.error('图片上传失败，请确认商品服务已启动且 MinIO 可用')
}

async function copyUrl() {
  if (!uploadedUrl.value) return ElMessage.warning('请先上传图片')
  try {
    await navigator.clipboard.writeText(uploadedUrl.value)
    ElMessage.success('URL 已复制')
  } catch {
    ElMessage.warning('复制失败，请手动选择复制')
  }
}

async function createProduct() {
  if (!productForm.value.name) return ElMessage.warning('请输入商品名称')
  creating.value = true
  try {
    const payload = { ...productForm.value }
    await productApi.post('/product', payload)
    ElMessage.success('商品创建成功，已建立向量索引')
    productForm.value = { name: '', description: '', price: 99, stock: 100, categoryId: 1, image: '' }
    loadProducts()
  } catch (e) {
    ElMessage.error('创建失败: ' + (e.response?.data?.message || e.message))
  } finally {
    creating.value = false
  }
}

async function searchByText() {
  if (!searchText.value.trim()) return ElMessage.warning('请输入检索文本')
  searching.value = true
  searched.value = true
  try {
    const res = await productApi.get('/product/similar', {
      params: { text: searchText.value.trim(), topK: 10 },
    })
    similarResults.value = res.data?.data || []
    ElMessage.success(`找到 ${similarResults.value.length} 个相似商品`)
  } catch (e) {
    similarResults.value = []
    ElMessage.error('检索失败: ' + (e.response?.data?.message || e.message))
  } finally {
    searching.value = false
  }
}

async function findSimilar(id) {
  try {
    const res = await productApi.get(`/product/${id}/similar`, { params: { topK: 10 } })
    similarResults.value = res.data?.data || []
    searched.value = true
    ElMessage.success(`找到 ${similarResults.value.length} 个相似商品`)
  } catch (e) {
    ElMessage.error('检索失败: ' + (e.response?.data?.message || e.message))
  }
}

async function loadProducts() {
  loadingProducts.value = true
  try {
    const res = await productApi.get('/product/list', { params: { page: 1, size: 20 } })
    products.value = res.data?.data?.records || []
  } catch {
    ElMessage.warning('商品列表加载失败（商品服务未启动？）')
  } finally {
    loadingProducts.value = false
  }
}

function scoreType(score) {
  if (score >= 0.8) return 'success'
  if (score >= 0.6) return 'warning'
  return 'info'
}

onMounted(loadProducts)
</script>

<style scoped>
.image-uploader :deep(.el-upload) { width: 100%; }
.upload-preview { width: 100%; height: 180px; border-radius: 6px; }
.url-input { margin-top: 12px; }
.result-list { display: flex; flex-direction: column; gap: 12px; }
.result-item { display: flex; align-items: center; gap: 12px; padding: 10px; background: #fafafa; border-radius: 8px; }
.result-img { width: 56px; height: 56px; border-radius: 6px; flex-shrink: 0; }
.result-img.placeholder, .product-img.placeholder {
  width: 56px; height: 56px; border-radius: 6px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  background: #f0f2f5; color: #909399; font-size: 12px;
}
.result-info { flex: 1; min-width: 0; }
.result-name { font-size: 14px; font-weight: 600; color: #1d1e2c; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.result-price { font-size: 14px; color: #f56c6c; margin-top: 4px; }
.result-score { flex-shrink: 0; }
.product-item { display: flex; align-items: center; gap: 10px; padding: 8px 0; border-bottom: 1px dashed #ebeef5; }
.product-item:last-child { border-bottom: none; }
.product-img { width: 44px; height: 44px; border-radius: 6px; flex-shrink: 0; }
.product-info { flex: 1; min-width: 0; }
.product-name { font-size: 13px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.product-meta { font-size: 12px; color: #909399; margin-top: 2px; }
</style>
