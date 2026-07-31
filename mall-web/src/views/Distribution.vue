<template>
  <div class="distribution-page">
    <h2>分销中心</h2>

    <!-- Invite Card -->
    <el-card class="invite-card">
      <template #header>我的邀请</template>
      <div class="invite-content">
        <div class="invite-code">
          <span>邀请码: </span>
          <b class="code-text">{{ inviteCode }}</b>
          <el-button size="small" type="primary" @click="copyInviteCode">复制邀请链接</el-button>
        </div>
        <p class="invite-url">邀请链接: http://localhost:5173/register?inviteCode={{ inviteCode }}</p>
      </div>
    </el-card>

    <!-- Commission Summary -->
    <el-card class="commission-summary">
      <template #header>佣金概览</template>
      <div class="summary-content">
        <div class="summary-item">
          <h3>¥{{ totalCommission }}</h3>
          <p>累计佣金</p>
        </div>
        <div class="summary-item">
          <h3>¥{{ pendingCommission }}</h3>
          <p>待结算</p>
        </div>
        <div class="summary-item">
          <h3>¥{{ settledCommission }}</h3>
          <p>已结算</p>
        </div>
      </div>
    </el-card>

    <!-- Commission Records -->
    <el-card class="commission-list">
      <template #header>佣金记录</template>
      <el-table :data="records" v-loading="loading" style="width:100%">
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="orderAmount" label="订单金额" width="100" />
        <el-table-column prop="commissionRatio" label="佣金比例" width="80">
          <template #default="{ row }">{{ row.commissionRatio }}%</template>
        </el-table-column>
        <el-table-column prop="commissionAmount" label="佣金金额" width="100">
          <template #default="{ row }">
            <span style="color:#f56c6c;font-weight:bold">¥{{ row.commissionAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="层级" width="80">
          <template #default="{ row }">{{ row.level === 1 ? '一级' : '二级' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small">
              {{ row.status === 1 ? '已结算' : '待结算' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="160" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../utils/request'
import { ElMessage } from 'element-plus'

const inviteCode = ref('')
const totalCommission = ref('0.00')
const pendingCommission = ref('0.00')
const settledCommission = ref('0.00')
const records = ref([])
const loading = ref(false)

async function fetchData() {
  try {
    const [inviteRes, totalRes, recordsRes] = await Promise.all([
      request.get('/distribution/invite-code'),
      request.get('/distribution/total-commission'),
      request.get('/distribution/commissions', { params: { pageNum: 1, pageSize: 50 } })
    ])
    inviteCode.value = inviteRes.data?.inviteCode || ''
    // These are simplified - would need separate API for pending vs settled
    totalCommission.value = (totalRes.data || 0).toFixed(2)
    records.value = recordsRes.data?.records || []
  } catch { /* ignore */ }
}

function copyInviteCode() {
  const url = `http://localhost:5173/register?inviteCode=${inviteCode.value}`
  navigator.clipboard.writeText(url).then(() => {
    ElMessage.success('邀请链接已复制')
  }).catch(() => {
    ElMessage.warning('复制失败，请手动复制')
  })
}

onMounted(fetchData)
</script>

<style scoped>
.distribution-page { max-width: 900px; margin: 20px auto; padding: 20px; }
.invite-card { margin-bottom: 16px; }
.invite-content { padding: 8px 0; }
.code-text { font-size: 20px; color: #409eff; margin: 0 12px; letter-spacing: 2px; }
.invite-url { color: #999; font-size: 13px; margin-top: 8px; }
.commission-summary { margin-bottom: 16px; }
.summary-content { display: flex; justify-content: space-around; }
.summary-item { text-align: center; }
.summary-item h3 { font-size: 28px; color: #f56c6c; }
.summary-item p { color: #999; margin-top: 4px; }
</style>
