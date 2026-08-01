<template>
  <div class="cart-view">
    <h2>购物车</h2>

    <el-table :data="cartItems" style="width: 100%" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" />
      <el-table-column prop="productName" label="商品名称" min-width="200" />
      <el-table-column prop="productPrice" label="单价" width="120">
        <template #default="{ row }">¥{{ row.productPrice.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="数量" width="180">
        <template #default="{ row }">
          <el-input-number
            v-model="row.quantity"
            :min="1"
            :max="99"
            size="small"
            @change="(val) => handleQuantityChange(row, val)"
          />
        </template>
      </el-table-column>
      <el-table-column label="小计" width="120">
        <template #default="{ row }">¥{{ row.subtotal.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button type="danger" size="small" text @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="cart-footer">
      <div class="summary">
        <span>已选 {{ selectedCount }} 件商品</span>
        <span class="total">合计：<strong>¥{{ totalAmount.toFixed(2) }}</strong></span>
      </div>
      <el-button type="primary" size="large" :disabled="selectedCount === 0" @click="goCheckout">
        去结算
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi } from '@/api'

const router = useRouter()
const cartItems = ref([])
const totalAmount = ref(0)
const selectedCount = ref(0)
const selectedIds = ref([])

const userId = ref(localStorage.getItem('userId') || '100')

onMounted(() => {
  fetchCartList()
})

async function fetchCartList() {
  try {
    const { data } = await orderApi.get('/cart/list', {
      headers: { 'X-User-Id': userId.value }
    })
    if (data.code === 200) {
      cartItems.value = data.data.items
      totalAmount.value = data.data.totalAmount
      selectedCount.value = data.data.selectedCount
    }
  } catch (e) {
    ElMessage.error('获取购物车失败')
  }
}

async function handleQuantityChange(row, val) {
  try {
    const { data } = await orderApi.put('/cart/quantity', {
      cartItemId: row.id,
      quantity: val
    }, {
      headers: { 'X-User-Id': userId.value }
    })
    if (data.code === 200) {
      cartItems.value = data.data.items
      totalAmount.value = data.data.totalAmount
      selectedCount.value = data.data.selectedCount
    } else {
      ElMessage.error(data.message)
      fetchCartList()
    }
  } catch (e) {
    ElMessage.error('修改数量失败')
    fetchCartList()
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该商品？', '提示', { type: 'warning' })
    const { data } = await orderApi.delete(`/cart/${row.id}`, {
      headers: { 'X-User-Id': userId.value }
    })
    if (data.code === 200) {
      ElMessage.success('删除成功')
      fetchCartList()
    }
  } catch (e) {
    // 用户取消
  }
}

function handleSelectionChange(selection) {
  selectedIds.value = selection.map(item => item.id)
}

function goCheckout() {
  router.push({ path: '/checkout', query: { ids: selectedIds.value.join(',') } })
}
</script>

<style scoped>
.cart-view {
  padding: 20px;
}
.cart-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}
.summary {
  display: flex;
  gap: 24px;
  align-items: center;
}
.total strong {
  color: #e6a23c;
  font-size: 20px;
}
</style>
