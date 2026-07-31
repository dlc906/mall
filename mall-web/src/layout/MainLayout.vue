<template>
  <div class="layout">
    <el-header class="header">
      <div class="header-content">
        <router-link to="/" class="logo">🏪 商城</router-link>
        <el-menu mode="horizontal" :default-active="activeMenu" router class="header-menu" :ellipsis="false">
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/cart">购物车
            <el-badge v-if="cartStore.totalCount > 0" :value="cartStore.totalCount" style="margin-left:4px"/>
          </el-menu-item>
          <el-menu-item index="/order/list">我的订单</el-menu-item>
          <el-menu-item index="/distribution">分销中心</el-menu-item>
        </el-menu>
        <div class="header-right">
          <template v-if="userStore.isLoggedIn">
            <el-dropdown>
              <span class="user-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="$router.push('/user/profile')">个人中心</el-dropdown-item>
                  <el-dropdown-item @click="$router.push('/user/address')">地址管理</el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" size="small" @click="$router.push('/login')">登录</el-button>
            <el-button size="small" @click="$router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </el-header>
    <el-main>
      <router-view :key="$route.fullPath" />
    </el-main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useCartStore } from '../stores/cart'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const activeMenu = computed(() => route.path)

// Fetch user info on mount
userStore.fetchUserInfo()
// 初始化购物车（导航栏需要显示购物车商品数量）
cartStore.init()

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.layout { min-height: 100vh; background: #f5f5f5; }
.header { background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.08); position: sticky; top: 0; z-index: 100; height: 60px; padding: 0; }
.header-content { max-width: 1200px; margin: 0 auto; display: flex; align-items: center; height: 100%; padding: 0 20px; }
.logo { font-size: 20px; font-weight: bold; color: #409eff; margin-right: 40px; white-space: nowrap; }
.header-menu { flex: 1; border-bottom: none !important; }
.header-menu .el-menu-item { height: 60px; line-height: 60px; }
.header-right { display: flex; align-items: center; gap: 8px; }
.user-name { cursor: pointer; color: #409eff; }
</style>
