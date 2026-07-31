<template>
  <div class="product-detail" v-loading="loading">
    <div class="detail-main" v-if="product">
      <el-image :src="product.mainImage" fit="cover" class="detail-image" />
      <div class="detail-info">
        <h1>{{ product.name }}</h1>
        <p class="detail-title">{{ product.title }}</p>
        <div class="detail-price">¥{{ product.price }} <span class="original-price" v-if="product.originalPrice">¥{{ product.originalPrice }}</span></div>
        <div class="detail-stock">库存: {{ product.stock }} 件 | 已售: {{ product.sales || 0 }}</div>
        <div class="detail-quantity">
          <span>数量:</span>
          <el-input-number v-model="quantity" :min="1" :max="product.stock" />
        </div>
        <div class="detail-buttons">
          <el-button type="primary" size="large" @click="addToCart">加入购物车</el-button>
          <el-button type="danger" size="large" @click="buyNow">立即购买</el-button>
        </div>
      </div>
    </div>
    <div class="detail-desc" v-if="product">
      <h3>商品详情</h3>
      <div v-html="product.detail || product.description"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../utils/request'
import { useCartStore } from '../stores/cart'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()
const product = ref(null)
const loading = ref(false)
const quantity = ref(1)

async function fetchDetail() {
  loading.value = true
  try {
    const res = await request.get(`/product/detail/${route.params.id}`)
    product.value = res.data
  } finally {
    loading.value = false
  }
}

async function addToCart() {
  await cartStore.addItem(product.value, quantity.value)
  ElMessage.success('已加入购物车')
}

async function buyNow() {
  await cartStore.addItem(product.value, quantity.value)
  router.push('/order/confirm')
}

onMounted(fetchDetail)

// 监听路由参数变化（浏览器返回/前进时重新加载数据）
watch(() => route.params.id, () => {
  quantity.value = 1
  fetchDetail()
})
</script>

<style scoped>
.product-detail { max-width: 1200px; margin: 20px auto; padding: 20px; background: #fff; border-radius: 8px; }
.detail-main { display: flex; gap: 40px; }
.detail-image { width: 450px; height: 450px; border-radius: 8px; }
.detail-info h1 { font-size: 24px; margin-bottom: 8px; }
.detail-title { color: #999; margin-bottom: 16px; }
.detail-price { font-size: 28px; color: #f56c6c; font-weight: bold; margin-bottom: 12px; }
.original-price { font-size: 16px; color: #999; text-decoration: line-through; margin-left: 8px; }
.detail-stock { color: #666; margin-bottom: 16px; }
.detail-quantity { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; }
.detail-buttons { display: flex; gap: 12px; }
.detail-desc { margin-top: 40px; padding-top: 20px; border-top: 1px solid #eee; }
</style>
