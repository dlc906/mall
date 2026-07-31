<template>
  <div class="profile-page">
    <h2>个人中心</h2>
    <el-card v-if="userStore.userInfo" class="profile-card">
      <div class="avatar-section">
        <el-avatar :size="80" :src="userStore.userInfo.avatar" />
        <div class="user-basic">
          <h3>{{ userStore.userInfo.nickname || userStore.userInfo.username }}</h3>
          <p>积分: {{ userStore.userInfo.points || 0 }}</p>
        </div>
      </div>
      <el-descriptions :column="1" border style="margin-top: 20px">
        <el-descriptions-item label="用户名">{{ userStore.userInfo.username }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ userStore.userInfo.phone || '未绑定' }}</el-descriptions-item>
        <el-descriptions-item label="邀请码">{{ userStore.userInfo.inviteCode || '暂无' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
onMounted(() => userStore.fetchUserInfo())
</script>

<style scoped>
.profile-page { max-width: 600px; margin: 20px auto; padding: 20px; }
.profile-card { padding: 20px; }
.avatar-section { display: flex; align-items: center; gap: 20px; }
.user-basic h3 { font-size: 20px; margin-bottom: 4px; }
.user-basic p { color: #f56c6c; }
</style>
