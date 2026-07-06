<template>
  <div class="home-view">
    <!-- 页头 -->
    <a-layout-header class="header">
      <div class="header-content">
        <h1 class="title">📚 图书馆预约管理系统</h1>
        <p class="subtitle">多账号管理 · Token 实时监控 · 一键预约</p>
      </div>
    </a-layout-header>

    <!-- 主内容 -->
    <a-layout-content class="main-content">
      <div class="main-grid">
        <!-- 左侧：添加/编辑账号 -->
        <AccountForm />

        <!-- 右侧：账号列表和详情 -->
        <div class="right-panel">
          <AccountList />
          <AccountDetail v-if="accountStore.selectedAccount" />
        </div>
      </div>
    </a-layout-content>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useAccountStore } from '@/stores/account'
import AccountForm from '@/components/AccountForm.vue'
import AccountList from '@/components/AccountList.vue'
import AccountDetail from '@/components/AccountDetail.vue'
import gsap from 'gsap'

const accountStore = useAccountStore()

// 初始化加载账号
onMounted(async () => {
  await accountStore.fetchAccounts()

  // 页面动画
  gsap.from('.header', {
    opacity: 0,
    y: -30,
    duration: 0.8,
    ease: 'power3.out'
  })

  gsap.from('.main-grid > *', {
    opacity: 0,
    y: 30,
    duration: 0.8,
    stagger: 0.2,
    ease: 'power3.out',
    delay: 0.3
  })
})

// 定时刷新 Token 状态
let refreshTimer: number | null = null

onMounted(() => {
  refreshTimer = window.setInterval(() => {
    accountStore.fetchAccounts()
  }, 60000) // 每分钟刷新
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>

<style scoped>
.home-view {
  min-height: 100vh;
  padding: 20px;
}

.header {
  background: transparent;
  text-align: center;
  margin-bottom: 40px;
  height: auto;
  padding: 0;
}

.header-content {
  display: inline-block;
}

.title {
  font-size: 2.8rem;
  margin-bottom: 12px;
  color: white;
  text-shadow: 2px 2px 8px rgba(0,0,0,0.2);
  font-weight: 700;
}

.subtitle {
  font-size: 1.15rem;
  color: white;
  opacity: 0.95;
  font-weight: 400;
}

.main-content {
  max-width: 1400px;
  margin: 0 auto;
  background: transparent;
}

.main-grid {
  display: grid;
  grid-template-columns: 420px 1fr;
  gap: 24px;
  align-items: start;
}

@media (max-width: 1200px) {
  .main-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .title {
    font-size: 2rem;
  }

  .subtitle {
    font-size: 1rem;
  }
}
</style>
