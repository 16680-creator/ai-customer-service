<template>
  <div class="shop-view">
    <div class="shop-toolbar">
      <div class="toolbar-left">
        <h2>商品商城</h2>
        <el-select v-model="searchMode" style="width: 128px">
          <el-option label="关键词搜索" value="keyword" />
          <el-option label="语义搜索" value="semantic" />
        </el-select>
        <el-input
          v-model="keyword"
          placeholder="搜索商品名称 / 描述，回车搜索"
          clearable
          style="width: 260px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="categoryId" placeholder="全部分类" clearable style="width: 140px" @change="handleSearch">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-button type="primary" :icon="Search" :loading="loading" @click="handleSearch">搜索</el-button>
      </div>
      <div class="toolbar-right">
        <el-badge :value="cartCount" :hidden="cartCount === 0" class="cart-badge">
          <el-button :icon="ShoppingCart" @click="$router.push('/cart')">购物车</el-button>
        </el-badge>
        <el-button :icon="Tickets" @click="$router.push('/order')">我的订单</el-button>
        <el-button type="success" :icon="CreditCard" @click="$router.push('/cart')">去结算</el-button>
      </div>
    </div>

    <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px">
      <template #title>
        支付流程体验：购物车 → 结算 → 下单 → 订单详情「立即支付」→ 模拟收银台 → 支付成功（含渠道回调 / 验签 / 幂等状态更新）。
      </template>
    </el-alert>

    <div v-loading="loading">
      <el-empty v-if="!loading && products.length === 0" description="暂无商品" />
      <el-row :gutter="16">
        <el-col v-for="p in products" :key="p.id" :span="6" style="margin-bottom: 16px">
          <el-card shadow="hover" class="product-card" :body-style="{ padding: '0' }">
            <div class="product-img">
              <el-image v-if="p.image" :src="p.image" fit="cover" class="img" />
              <div v-else class="img-placeholder">暂无图片</div>
            </div>
            <div class="product-body">
              <div class="product-name" :title="p.name">{{ p.name }}</div>
              <div class="product-meta">
                <span class="price">¥{{ Number(p.price).toFixed(2) }}</span>
                <span v-if="p.stock !== undefined" class="stock">库存 {{ p.stock }}</span>
                <el-tag v-if="p.score !== undefined" size="small" type="success">
                  相似度 {{ Number(p.score).toFixed(4) }}
                </el-tag>
              </div>
              <div class="product-actions">
                <el-input-number v-model="qtyMap[p.id]" :min="1" :max="99" size="small" />
                <el-button type="primary" size="small" :loading="addingId === p.id" @click="addToCart(p)">
                  加入购物车
                </el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <el-pagination
      v-if="total > size"
      background
      layout="total, prev, pager, next"
      :total="total"
      :page-size="size"
      :current-page="page"
      class="pager"
      @current-change="handlePageChange"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, ShoppingCart, Tickets, CreditCard } from '@element-plus/icons-vue'
import { productApi, cartApi } from '../api'
import { getUser } from '../utils/auth'

const products = ref([])
const categories = ref([])
const keyword = ref('')
const searchMode = ref('keyword')
const categoryId = ref(null)
const page = ref(1)
const size = ref(12)
const total = ref(0)
const loading = ref(false)
const cartCount = ref(0)
const addingId = ref(null)

const qtyMap = reactive({})
const userId = ref(getUser()?.userId || '')

onMounted(() => {
  loadCategories()
  loadProducts()
  loadCartCount()
})

async function loadCategories() {
  try {
    const { data } = await productApi.get('/categories')
    if (data.code === 200) categories.value = data.data || []
  } catch (e) {
    // 分类加载失败不阻塞页面
  }
}

async function loadProducts() {
  loading.value = true
  try {
    if (searchMode.value === 'semantic' && keyword.value.trim()) {
      const { data } = await productApi.get('/similar', {
        params: { text: keyword.value.trim(), topK: 12 },
      })
      if (data.code === 200) {
        products.value = (data.data || []).map((r) => ({
          id: r.productId,
          name: r.name,
          price: r.price,
          image: r.image,
          stock: undefined,
          score: r.score,
        }))
        total.value = products.value.length
      }
    } else {
      const params = { page: page.value, size: size.value, status: 1 }
      if (keyword.value.trim()) params.keyword = keyword.value.trim()
      if (categoryId.value) params.categoryId = categoryId.value
      const { data } = await productApi.get('/list', { params })
      if (data.code === 200 && data.data) {
        products.value = data.data.records || []
        total.value = data.data.total || 0
      }
    }
  } catch (e) {
    ElMessage.error('加载商品失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadProducts()
}

function handlePageChange(p) {
  page.value = p
  loadProducts()
}

async function addToCart(p) {
  const qty = qtyMap[p.id] || 1
  addingId.value = p.id
  try {
    const { data } = await cartApi.post('/add', { productId: p.id, quantity: qty }, {
      headers: { 'X-User-Id': userId.value },
    })
    if (data.code === 200) {
      ElMessage.success(`「${p.name}」已加入购物车`)
      loadCartCount()
      loadProducts() // 刷新商城库存，实时反映扣减（库存以商品服务 DB 为权威源）
    } else {
      ElMessage.error(data.message || '加入购物车失败')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '加入购物车失败')
  } finally {
    addingId.value = null
  }
}

async function loadCartCount() {
  try {
    const { data } = await cartApi.get('/list', { headers: { 'X-User-Id': userId.value } })
    if (data.code === 200) cartCount.value = (data.data?.items || []).length
  } catch (e) {
    // 忽略
  }
}


</script>

<style scoped>
.shop-view { padding: 20px; }

.shop-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar-left { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.toolbar-left h2 { margin: 0; font-size: 20px; }
.toolbar-right { display: flex; align-items: center; gap: 10px; }
.cart-badge { margin-right: 8px; }

.product-card { overflow: hidden; }

.product-img {
  height: 180px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
}
.product-img .img { width: 100%; height: 100%; }
.img-placeholder { color: #c0c4cc; font-size: 13px; }

.product-body { padding: 12px; }
.product-name {
  font-weight: 600;
  font-size: 14px;
  height: 20px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.product-meta { display: flex; align-items: center; justify-content: space-between; margin: 8px 0; }
.price { color: #f56c6c; font-size: 16px; font-weight: 600; }
.stock { color: #909399; font-size: 12px; }
.product-actions { display: flex; align-items: center; justify-content: space-between; }

.pager { margin-top: 16px; justify-content: center; }
</style>