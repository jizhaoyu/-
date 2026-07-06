/**
 * API 接口定义
 */
import request from './request'

// ============ 账号管理 ============

export interface Account {
  id: number
  name: string
  token: string
  area_id: string
  date_preference: 'today' | 'tomorrow'
  strategy: 'first' | 'random'
  created_at: string
  updated_at: string
  token_status?: {
    is_valid: boolean
    message: string
    info: {
      exp: number
      exp_formatted: string
      is_expired: boolean
      remaining_minutes: number
      user_name: string
      user_id: string
    }
  }
}

export interface CreateAccountDto {
  name: string
  token: string
  area_id?: string
  date_preference?: 'today' | 'tomorrow'
  strategy?: 'first' | 'random'
}

export interface UpdateAccountDto extends Partial<CreateAccountDto> {}

/**
 * 获取所有账号
 */
export const getAccounts = () => {
  return request.get<{ success: boolean; data: Account[] }>('/api/accounts')
}

/**
 * 获取单个账号
 */
export const getAccount = (id: number) => {
  return request.get<{ success: boolean; data: Account }>(`/api/accounts/${id}`)
}

/**
 * 添加账号
 */
export const createAccount = (data: CreateAccountDto) => {
  return request.post<{ success: boolean; message: string; data: Account }>('/api/accounts', data)
}

/**
 * 更新账号
 */
export const updateAccount = (id: number, data: UpdateAccountDto) => {
  return request.put<{ success: boolean; message: string; data: Account }>(`/api/accounts/${id}`, data)
}

/**
 * 删除账号
 */
export const deleteAccount = (id: number) => {
  return request.delete<{ success: boolean; message: string }>(`/api/accounts/${id}`)
}

// ============ 预约管理 ============

export interface TestReserveDto {
  account_id: number
}

export interface TestReserveResult {
  date: string
  segment: {
    id: string
    start: string
    end: string
  }
  seat: {
    id: string
    no: string
    name: string
  }
  available_seats_count: number
  total_seats_count: number
}

/**
 * 测试预约
 */
export const testReserve = (data: TestReserveDto) => {
  return request.post<{ success: boolean; message: string; data: TestReserveResult }>('/api/reservations/test', data)
}

/**
 * 执行预约
 */
export const executeReserve = (data: TestReserveDto) => {
  return request.post<{ success: boolean; message: string; data: any }>('/api/reservations/execute', data)
}

/**
 * 签到
 */
export const checkIn = (data: TestReserveDto) => {
  return request.post<{ success: boolean; message: string }>('/api/reservations/check-in', data)
}

/**
 * 签退
 */
export const signOut = (data: TestReserveDto) => {
  return request.post<{ success: boolean; message: string }>('/api/reservations/sign-out', data)
}

/**
 * 获取当前预约
 */
export const getCurrentReservations = (data: TestReserveDto) => {
  return request.post<{ success: boolean; data: any[] }>('/api/reservations/current', data)
}

/**
 * 取消预约
 */
export const cancelReservation = (data: { account_id: number; reservation_id: string }) => {
  return request.post<{ success: boolean; message: string }>('/api/reservations/cancel', data)
}

/**
 * 获取历史记录
 */
export const getReservationHistory = (accountId?: number) => {
  return request.get<{ success: boolean; data: any[] }>('/api/reservations/history', {
    params: accountId ? { account_id: accountId } : {}
  })
}
