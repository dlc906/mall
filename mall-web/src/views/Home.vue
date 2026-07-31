<template>
  <div class="home">
    <!-- Search Bar -->
    <div class="search-section">
      <el-input v-model="searchKeyword" placeholder="搜索商品..." size="large" class="search-input" @keyup.enter="handleSearch">
        <template #prefix><el-icon><Search /></el-icon></template>
        <template #append>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
        </template>
      </el-input>
    </div>

    <!-- Categories -->
    <div class="categories" v-if="categories.length > 0">
      <el-button v-for="cat in categories" :key="cat.id" :type="selectedCategory === cat.id ? 'primary' : 'default'" size="small" @click="selectCategory(cat.id)">
        {{ cat.name }}
      </el-button>
    </div>

    <!-- Product Grid -->
    <div class="product-grid" v-loading="loading">
      <div v-for="product in products" :key="product.id" class="product-card" @click="$router.push(`/product/${product.id}`)">
        <el-image :src="product.mainImage || 'https://placehold.co/300x300/EEE/999?text=Product'" fit="cover" class="product-image" />
        <div class="product-info">
          <h3 class="product-name">{{ product.name }}</h3>
          <p class="product-desc">{{ product.description }}</p>
          <div class="product-bottom">
            <span class="price">¥{{ product.price }}</span>
            <span class="sales">已售 {{ product.sales || 0 }}</span>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && products.length === 0" description="暂无商品" />
    </div>

    <!-- Pagination -->
    <div class="pagination" v-if="total > 0">
      <el-pagination
        v-model:current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchProducts" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../utils/request'
import { useCartStore } from '../stores/cart'

const cartStore = useCartStore()
const products = ref([])
const categories = ref([])
const loading = ref(false)
const searchKeyword = ref('')
const selectedCategory = ref(null)
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)

async function fetchProducts() {
  loading.value = true
  try {
    const res = await request.get('/product/list', {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        categoryId: selectedCategory.value || undefined,
        keyword: searchKeyword.value || undefined
      }
    })
    products.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

async function fetchCategories() {
  try {
    const res = await request.get('/product/category')
    categories.value = res.data || []
  } catch (e) { /* ignore */ }
}

function handleSearch() {
  pageNum.value = 1
  fetchProducts()
}

function selectCategory(catId) {
  selectedCategory.value = selectedCategory.value === catId ? null : catId
  pageNum.value = 1
  fetchProducts()
}

onMounted(() => {
  fetchCategories()
  fetchProducts()
})
</script>

<style scoped>
.home { max-width: 1200px; margin: 0 auto; padding: 20px; }
.search-section { margin-bottom: 20px; }
.search-input { max-width: 600px; }
.categories { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 20px; }
.product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 20px; }
.product-card { background: #fff; border-radius: 8px; overflow: hidden; cursor: pointer; box-shadow: 0 2px 8px rgba(0,0,0,0.08); transition: transform 0.2s, box-shadow 0.2s; }
.product-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,0.12); }
.product-image { width: 100%; height: 220px; }
.product-info { padding: 12px; }
.product-name { font-size: 16px; font-weight: 600; color: #333; margin-bottom: 4px; }
.product-desc { font-size: 13px; color: #999; margin-bottom: 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.product-bottom { display: flex; justify-content: space-between; align-items: center; }
.price { font-size: 20px; font-weight: bold; color: #f56c6c; }
.sales { font-size: 12px; color: #999; }
.pagination { margin-top: 30px; display: flex; justify-content: center; }
</style>
