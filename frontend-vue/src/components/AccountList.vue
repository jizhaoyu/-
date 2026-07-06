<template>
  <a-card class="account-list-card" :bordered="false">
    <template #title>
      <div class="card-header">
        <span class="card-title">账号列表</span>
        <a-button
          type="text"
          @click="accountStore.fetchAccounts()"
          :loading="accountStore.loading"
        >
          <template #icon>
            <icon-refresh />
          </template>
        </a-button>
      </div>
    </template>

    <!-- 空状态 -->
    <a-empty
      v-if="accountStore.accounts.length === 0 && !accountStore.loading"
      description="暂无账号，点击左侧添加第一个账号"
    >
      <template #image>
        <icon-plus-circle :style="{ fontSize: '80px', color: '#c9cdd4' }" />
      </template>
    </a-empty>

    <!-- 账号列表 -->
    <div v-else class="account-list">
      <div
        v-for="account in accountStore.accounts"
        :key="account.id"
        class="account-item"
        :class="{ active: accountStore.selectedAccountId === account.id }"
        @click="handleSelectAccount(account.id)"
      >
        <div class="account-name">{{ account.name }}</div>
        <div class="account-meta">
          <a-tag
            :color="account.token_status?.is_valid ? 'green' : 'red'"
            size="small"
          >
            {{ account.token_status?.is_valid ? '有效' : '已过期' }}
          </a-tag>
          <span class="meta-item">
            <icon-location />
            区域 {{ account.area_id }}
          </span>
          <span
            v-if="account.token_status?.info?.remaining_minutes > 0"
            class="meta-item"
          >
            <icon-clock-circle />
            剩余 {{ account.token_status.info.remaining_minutes }} 分钟
          </span>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <a-spin v-if="accountStore.loading" class="loading-spin" />
  </a-card>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import {
  IconRefresh,
  IconPlusCircle,
  IconLocation,
  IconClockCircle
} from '@arco-design/web-vue/es/icon'
import { useAccountStore } from '@/stores/account'
import gsap from 'gsap'

const accountStore = useAccountStore()

const handleSelectAccount = (id: number) => {
  accountStore.selectAccount(id)

  // 动画效果
  gsap.from('.account-detail-card', {
    opacity: 0,
    y: 20,
    duration: 0.5,
    ease: 'power2.out'
  })
}

onMounted(() => {
  // 列表项动画
  gsap.from('.account-item', {
    opacity: 0,
    x: -20,
    duration: 0.4,
    stagger: 0.1,
    ease: 'power2.out'
  })
})
</script>

<style scoped>
.account-list-card {
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
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

.account-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 500px;
  overflow-y: auto;
  padding-right: 8px;
}

.account-item {
  background: #f9fafb;
  border: 2px solid #e5e7eb;
  border-radius: 14px;
  padding: 18px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.account-item:hover {
  border-color: #667eea;
  transform: translateX(6px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
}

.account-item.active {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.08) 0%, rgba(118, 75, 162, 0.08) 100%);
  border-color: #667eea;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.2);
}

.account-name {
  font-weight: 700;
  font-size: 1.15rem;
  color: #1f2937;
  margin-bottom: 10px;
}

.account-meta {
  display: flex;
  gap: 12px;
  font-size: 0.88rem;
  color: #6b7280;
  flex-wrap: wrap;
  align-items: center;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.loading-spin {
  display: flex;
  justify-content: center;
  padding: 40px;
}
</style>
