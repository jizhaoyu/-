<template>
  <a-card class="account-form-card" :bordered="false">
    <template #title>
      <div class="card-title">添加/编辑账号</div>
    </template>

    <a-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      layout="vertical"
      @submit="handleSubmit"
    >
      <a-form-item field="name" label="姓名" required>
        <a-input
          v-model="formData.name"
          placeholder="如：张三"
          allow-clear
        />
      </a-form-item>

      <a-form-item field="token" label="Token" required>
        <a-textarea
          v-model="formData.token"
          placeholder="粘贴完整 token（支持 bearer 前缀）"
          :auto-size="{ minRows: 3, maxRows: 5 }"
          allow-clear
        />
      </a-form-item>

      <a-form-item field="area_id" label="区域 ID">
        <a-input
          v-model="formData.area_id"
          placeholder="如：22"
        />
      </a-form-item>

      <a-form-item field="date_preference" label="预约日期">
        <a-select v-model="formData.date_preference">
          <a-option value="today">今天</a-option>
          <a-option value="tomorrow">明天</a-option>
        </a-select>
      </a-form-item>

      <a-form-item field="strategy" label="座位策略">
        <a-select v-model="formData.strategy">
          <a-option value="first">第一个空闲</a-option>
          <a-option value="random">随机选择</a-option>
        </a-select>
      </a-form-item>

      <a-form-item>
        <a-button
          type="primary"
          html-type="submit"
          long
          :loading="loading"
        >
          <template #icon>
            <icon-plus v-if="!editingId" />
            <icon-save v-else />
          </template>
          {{ editingId ? '💾 保存修改' : '➕ 添加账号' }}
        </a-button>
      </a-form-item>
    </a-form>
  </a-card>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { Message } from '@arco-design/web-vue'
import { IconPlus, IconSave } from '@arco-design/web-vue/es/icon'
import { useAccountStore } from '@/stores/account'
import type { FormInstance } from '@arco-design/web-vue'
import gsap from 'gsap'

const accountStore = useAccountStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const editingId = ref<number | null>(null)

const formData = reactive({
  name: '',
  token: '',
  area_id: '22',
  date_preference: 'tomorrow' as 'today' | 'tomorrow',
  strategy: 'first' as 'first' | 'random'
})

const rules = {
  name: [{ required: true, message: '请输入姓名' }],
  token: [{ required: true, message: '请输入 Token' }]
}

// 监听选中账号，用于编辑
watch(
  () => accountStore.selectedAccount,
  (account) => {
    if (account && editingId.value === account.id) {
      Object.assign(formData, {
        name: account.name,
        token: account.token,
        area_id: account.area_id,
        date_preference: account.date_preference,
        strategy: account.strategy
      })
    }
  }
)

const handleSubmit = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) return

  loading.value = true
  try {
    if (editingId.value) {
      // 更新账号
      await accountStore.updateAccount(editingId.value, formData)
      Message.success('账号已更新')
      editingId.value = null
    } else {
      // 添加账号
      await accountStore.addAccount(formData)
      Message.success('账号已添加')
    }

    // 重置表单
    resetForm()

    // 动画效果
    gsap.from('.account-form-card', {
      scale: 1.02,
      duration: 0.3,
      ease: 'power2.out'
    })
  } catch (error: any) {
    // 错误已在 request 拦截器处理
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(formData, {
    name: '',
    token: '',
    area_id: '22',
    date_preference: 'tomorrow',
    strategy: 'first'
  })
}

// 暴露给父组件调用
const startEdit = (id: number) => {
  editingId.value = id
  accountStore.selectAccount(id)

  // 滚动到顶部
  gsap.to(window, {
    scrollTo: { y: 0 },
    duration: 0.8,
    ease: 'power2.inOut'
  })
}

defineExpose({
  startEdit
})
</script>

<style scoped>
.account-form-card {
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
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

:deep(.arco-btn-primary) {
  height: 48px;
  font-size: 1rem;
  font-weight: 600;
}
</style>
