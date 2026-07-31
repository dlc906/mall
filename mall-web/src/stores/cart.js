import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '../utils/request'
import { ElMessage } from 'element-plus'

export const useCartStore = defineStore('cart', () => {
  const items = ref([])
  const loaded = ref(false)

  /**
   * 从后端 API 加载购物车数据
   */
  async function init() {
    if (loaded.value) return
    try {
      const res = await request.get('/cart/list')
      items.value = (res.data || []).map(i => ({
        productId: i.productId,
        name: i.name,
        image: i.image,
        price: i.price,
        stock: i.stock,
        quantity: i.quantity,
        checked: i.checked !== false
      }))
      loaded.value = true
    } catch (e) {
      // 降级：尝试从 localStorage 加载（未登录或API不可用时）
      const local = JSON.parse(localStorage.getItem('cart') || '[]')
      if (local.length > 0) {
        items.value = local
        loaded.value = true
      }
    }
  }

  /**
   * 重置（退出登录时调用）
   */
  function reset() {
    items.value = []
    loaded.value = false
  }

  async function addItem(product, quantity = 1) {
    try {
      await request.post('/cart/add', { productId: product.id, quantity })
    } catch (e) {
      // API 失败时降级到 localStorage
      fallbackAddItem(product, quantity)
      return
    }
    // 成功后更新本地状态
    const existing = items.value.find(i => i.productId === product.id)
    if (existing) {
      existing.quantity += quantity
    } else {
      items.value.push({
        productId: product.id,
        name: product.name,
        image: product.mainImage,
        price: product.price,
        stock: product.stock,
        quantity,
        checked: true
      })
    }
  }

  async function removeItem(productId) {
    try {
      await request.delete(`/cart/${productId}`)
    } catch (e) {
      ElMessage.error('删除失败')
      return
    }
    items.value = items.value.filter(i => i.productId !== productId)
  }

  async function updateQuantity(productId, quantity) {
    const item = items.value.find(i => i.productId === productId)
    if (!item) return
    const clamped = Math.max(1, Math.min(quantity, item.stock))
    try {
      await request.put('/cart/quantity', { productId, quantity: clamped })
    } catch (e) {
      ElMessage.error('更新失败')
      return
    }
    item.quantity = clamped
  }

  async function toggleCheck(productId) {
    const item = items.value.find(i => i.productId === productId)
    if (!item) return
    const newChecked = !item.checked
    try {
      await request.put('/cart/check', { productId, checked: newChecked })
    } catch (e) {
      ElMessage.error('操作失败')
      return
    }
    item.checked = newChecked
  }

  async function checkAll(checked) {
    try {
      await request.put('/cart/checkAll', null, { params: { checked } })
    } catch (e) {
      ElMessage.error('操作失败')
      return
    }
    items.value.forEach(i => i.checked = checked)
  }

  async function clearCart() {
    try {
      await request.delete('/cart/clear')
    } catch (e) {
      // 忽略清除失败
    }
    items.value = []
  }

  // ---- 降级方案：localStorage 回退 ----
  function fallbackAddItem(product, quantity) {
    const existing = items.value.find(i => i.productId === product.id)
    if (existing) {
      existing.quantity += quantity
    } else {
      items.value.push({
        productId: product.id,
        name: product.name,
        image: product.mainImage,
        price: product.price,
        stock: product.stock,
        quantity,
        checked: true
      })
    }
    saveLocal()
  }

  function saveLocal() {
    localStorage.setItem('cart', JSON.stringify(items.value))
  }
  // ---- 降级方案结束 ----

  const checkedItems = computed(() => items.value.filter(i => i.checked))
  const totalPrice = computed(() => {
    return checkedItems.value.reduce((sum, i) => sum + i.price * i.quantity, 0).toFixed(2)
  })
  const totalCount = computed(() => checkedItems.value.reduce((sum, i) => sum + i.quantity, 0))

  return {
    items, loaded,
    init, reset,
    addItem, removeItem, updateQuantity,
    toggleCheck, checkAll, clearCart,
    checkedItems, totalPrice, totalCount
  }
})
