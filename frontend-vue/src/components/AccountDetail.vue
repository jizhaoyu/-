<template>
  <a-card class="account-detail-card" :bordered="false">
    <template #title>
      <div class="card-title">账号详情</div>
    </template>

    <div class="detail-section">
      <h3>🔐 认证信息</h3>
      <a-descriptions :column="1" bordered>
        <a-descriptions-item label="姓名">
          {{ account.name }}
        </a-descriptions-item>
        <a-descriptions-item label="Token 状态">
          <a-tag
            :color="account.token_status?.is_valid ? 'green' : 'red'"
          >
            {{ account.token_status?.is_valid ? '✓ 有效' : '✗ 已过期' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="过期时间">
          {{ account.token_status?.info?.exp_formatted || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="剩余时间">
          <span v-if="account.token_status?.info?.remaining_minutes > 0">
            {{ account.token_status.info.remaining_minutes }} 分钟
          </span>
          <span v-else class="expired-text">已过期</span>
        </a-descriptions-item>
      </a-descriptions>
    </div>

    <div class="detail-section">
      <h3>⚙️ 预约配置</h3>
      <a-descriptions :column="1" bordered>
        <a-descriptions-item label="区域 ID">
          {{ account.area_id }}
        </a-descriptions-item>
        <a-descriptions-item label="预约日期">
          {{ account.date_preference === 'today' ? '今天' : '明天' }}
        </a-descriptions-item>
        <a-descriptions-item label="座位策略">
          {{ account.strategy === 'first' ? '第一个空闲' : '随机选择' }}
        </a-descriptions-item>
      </a-descriptions>
    </div>

    <div class="action-bar">
      <a-button type="outline" @click="handleEdit">
        <template #icon>
          <icon-edit />
        </template>
        编辑
      </a-button>

      <a-button
        type="primary"
        status="normal"
        @click="handleTest"
        :loading="testLoading"
      >
        <template #icon>
          <icon-experiment />
        </template>
        测试
      </a-button>

      <a-button
        type="primary"
        @click="handleReserve"
        :loading="reserveLoading"
      >
        <template #icon>
          <icon-check-circle />
        </template>
        预约
      </a-button>

      <a-button
        type="primary"
        status="warning"
        @click="handleCheckIn"
        :loading="checkInLoading"
      >
        <template #icon>
          <icon-pushpin />
        </template>
        签到
      </a-button>

      <a-button
        type="primary"
        status="danger"
        @click="handleDelete"
        :loading="deleteLoading"
      >
        <template #icon>
          <icon-delete />
        </template>
        删除
      </a-button>
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Message, Modal } from '@arco-design/web-vue'
import {
  IconEdit,
  IconExperiment,
  IconCheckCircle,
  IconPushpin,
  IconDelete
} from '@arco-design/web-vue/es/icon'
import { useAccountStore } from '@/stores/account'
import * as api from '@/api'

const accountStore = useAccountStore()

const account = computed(() => accountStore.selectedAccount!)

const testLoading = ref(false)
const reserveLoading = ref(false)
const checkInLoading = ref(false)
const deleteLoading = ref(false)

// 编辑账号
const handleEdit = () => {
  // 这里需要触发父组件的编辑方法
  // 可以通过事件或者直接调用 AccountForm 的方法
  Message.info('请在左侧表单编辑账号信息')
}

// 测试预约
const handleTest = async () => {
  testLoading.value = true
  try {
    const res = await api.testReserve({ account_id: account.value.id })
    const data = res.data

    Modal.success({
      title: '测试成功',
      content: `找到 ${data.available_seats_count} 个空闲座位，将预约座位 ${data.seat.no}（${data.segment.start}-${data.segment.end}）`,
      okText: '知道了'
    })
  } catch (error) {
    // 错误已在拦截器处理
  } finally {
    testLoading.value = false
  }
}

// 执行预约
const handleReserve = async () => {
  Modal.confirm({
    title: '确认预约',
    content: '确定要执行预约吗？',
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      reserveLoading.value = true
      try {
        const res = await api.executeReserve({ account_id: account.value.id })
        const reservation = res.data.reservation

        Message.success(
          `预约成功！座位 ${reservation.seat_no}（${reservation.start_time}-${reservation.end_time}）`
        )
      } catch (error) {
        // 错误已在拦截器处理
      } finally {
        reserveLoading.value = false
      }
    }
  })
}

// 签到
const handleCheckIn = async () => {
  checkInLoading.value = true
  try {
    const res = await api.checkIn({ account_id: account.value.id })
    Message.success(res.message || '签到成功')
  } catch (error) {
    // 错误已在拦截器处理
  } finally {
    checkInLoading.value = false
  }
}

// 删除账号
const handleDelete = () => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这个账号吗？此操作不可恢复。',
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { status: 'danger' },
    onOk: async () => {
      deleteLoading.value = true
      try {
        await accountStore.removeAccount(account.value.id)
        Message.success('账号已删除')
      } catch (error) {
        // 错误已在拦截器处理
      } finally {
        deleteLoading.value = false
      }
    }
  })
}
</script>

<style scoped>
.account-detail-card {
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
  margin-top: 24px;
}

.card-title {
  font-size: 1.4rem;
  font-weight: 700;
  color: #1f2937;
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-title::before {
  content: '';
  width: 4px;
  height: 28px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 2px;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section h3 {
  font-size: 1.05rem;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 16px;
}

.expired-text {
  color: #f53f3f;
  font-weight: 600;
}

.action-bar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.action-bar .arco-btn {
  flex: 1;
  min-width: 100px;
}

@media (max-width: 768px) {
  .action-bar .arco-btn {
    min-width: auto;
  }
}
</style>
