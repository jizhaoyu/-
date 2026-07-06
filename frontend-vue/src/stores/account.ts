/**
 * Pinia 状态管理 - 账号管理
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Account } from '@/api'
import * as api from '@/api'

export const useAccountStore = defineStore('account', () => {
  // 状态
  const accounts = ref<Account[]>([])
  const selectedAccountId = ref<number | null>(null)
  const loading = ref(false)

  // 计算属性
  const selectedAccount = computed(() => {
    return accounts.value.find(acc => acc.id === selectedAccountId.value)
  })

  const validAccounts = computed(() => {
    return accounts.value.filter(acc => acc.token_status?.is_valid)
  })

  const expiredAccounts = computed(() => {
    return accounts.value.filter(acc => !acc.token_status?.is_valid)
  })

  // 方法
  const fetchAccounts = async () => {
    loading.value = true
    try {
      const res = await api.getAccounts()
      accounts.value = res.data
    } finally {
      loading.value = false
    }
  }

  const selectAccount = (id: number) => {
    selectedAccountId.value = id
  }

  const addAccount = async (data: api.CreateAccountDto) => {
    const res = await api.createAccount(data)
    await fetchAccounts()
    return res
  }

  const updateAccount = async (id: number, data: api.UpdateAccountDto) => {
    const res = await api.updateAccount(id, data)
    await fetchAccounts()
    return res
  }

  const removeAccount = async (id: number) => {
    await api.deleteAccount(id)
    if (selectedAccountId.value === id) {
      selectedAccountId.value = null
    }
    await fetchAccounts()
  }

  return {
    accounts,
    selectedAccountId,
    loading,
    selectedAccount,
    validAccounts,
    expiredAccounts,
    fetchAccounts,
    selectAccount,
    addAccount,
    updateAccount,
    removeAccount
  }
})
