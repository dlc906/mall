<template>
  <div class="cart-page">
    <h2>购物车</h2>
    <el-empty v-if="cartStore.items.length === 0" description="购物车是空的">
      <el-button type="primary" @click="$router.push('/')">去逛逛</el-button>
    </el-empty>
    <template v-else>
      <div class="cart-list">
        <div class="cart-header">
          <el-checkbox v-model="checkAll" @change="cartStore.checkAll" style="margin-right:8px">全选</el-checkbox>
          <span>商品信息</span>
          <span>单价</span>
          <span>数量</span>
          <span>小计</span>
          <span>操作</span>
        </div>
        <div v-for="item in cartStore.items" :key="item.productId" class="cart-item">
          <el-checkbox v-model="item.checked" @change="cartStore.toggleCheck(item.productId)" />
          <div class="item-info" @click="$router.push(`/product/${item.productId}`)">
            <el-image :src="item.image" fit="cover" style="width:80px;height:80px;border-radius:4px" />
            <span>{{ item.name }}</span>
          </div>
          <span class="item-price">¥{{ item.price }}</span>
          <el-input-number v-model="item.quantity" :min="1" :max="Math.max(item.stock, 1)" size="small" @change="val => cartStore.updateQuantity(item.productId, val)" />
          <span class="item-total">¥{{ (item.price * item.quantity).toFixed(2) }}</span>
          <el-button type="danger" link @click="cartStore.removeItem(item.productId)">删除</el-button>
        </div>
      </div>
      <div class="cart-footer">
        <div class="footer-left">
          已选 <b>{{ cartStore.totalCount }}</b> 件
        </div>
        <div class="footer-right">
          合计: <span class="total-price">¥{{ cartStore.totalPrice }}</span>
          <el-button type="danger" size="large" :disabled="cartStore.totalCount === 0" @click="$router.push('/order/confirm')">去结算</el-button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useCartStore } from '../stores/cart'

const cartStore = useCartStore()
cartStore.init()

const checkAll = computed({
  get: () => cartStore.items.length > 0 && cartStore.items.every(i => i.checked),
  set: (val) => cartStore.checkAll(val)
})
</script>

<style scoped>
.cart-page { max-width: 1200px; margin: 20px auto; padding: 20px; }
.cart-header, .cart-item { display: flex; align-items: center; padding: 12px; background: #fff; margin-bottom: 8px; border-radius: 4px; gap: 16px; }
.cart-header { background: #f5f5f5; font-size: 14px; color: #666; }
.cart-header span { flex: 1; text-align: center; }
.cart-item { }
.item-info { display: flex; align-items: center; gap: 12px; flex: 1; cursor: pointer; }
.item-price, .item-total { flex: 1; text-align: center; }
.item-total { color: #f56c6c; font-weight: bold; }
.cart-footer { display: flex; justify-content: space-between; align-items: center; padding: 16px; background: #fff; border-radius: 4px; margin-top: 16px; }
.footer-right { display: flex; align-items: center; gap: 16px; }
.total-price { font-size: 24px; color: #f56c6c; font-weight: bold; }
</style>
