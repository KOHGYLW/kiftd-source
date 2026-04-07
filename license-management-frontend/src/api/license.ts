import { http } from './request'
import type { License, LicenseForm, PageQuery, PageResult } from '@/types'

// 获取授权列表
export const getLicenses = (params: PageQuery): Promise<PageResult<License>> => {
  return http.get('/licenses', { params })
}

// 获取授权详情
export const getLicense = (id: number): Promise<License> => {
  return http.get(`/licenses/${id}`)
}

// 获取客户的授权列表
export const getLicensesByCustomer = (customerId: number, params: PageQuery): Promise<PageResult<License>> => {
  return http.get(`/customers/${customerId}/licenses`, { params })
}

// 创建授权
export const createLicense = (data: LicenseForm): Promise<License> => {
  return http.post('/licenses', data)
}

// 更新授权
export const updateLicense = (id: number, data: Partial<LicenseForm>): Promise<License> => {
  return http.put(`/licenses/${id}`, data)
}

// 删除授权
export const deleteLicense = (id: number): Promise<void> => {
  return http.delete(`/licenses/${id}`)
}

// 批量删除授权
export const batchDeleteLicenses = (ids: number[]): Promise<void> => {
  return http.post('/licenses/batch-delete', { ids })
}

// 激活授权
export const activateLicense = (id: number): Promise<void> => {
  return http.post(`/licenses/${id}/activate`)
}

// 暂停授权
export const suspendLicense = (id: number): Promise<void> => {
  return http.post(`/licenses/${id}/suspend`)
}

// 撤销授权
export const revokeLicense = (id: number): Promise<void> => {
  return http.post(`/licenses/${id}/revoke`)
}

// 续期授权
export const renewLicense = (id: number, data: { endDate: string }): Promise<License> => {
  return http.post(`/licenses/${id}/renew`, data)
}

// 验证授权
export const validateLicense = (licenseKey: string): Promise<{ valid: boolean; license?: License; message: string }> => {
  return http.post('/licenses/validate', { licenseKey })
}

// 获取授权使用统计
export const getLicenseUsageStats = (id: number): Promise<{
  currentUsers: number
  maxUsers: number
  lastAccessTime: string
  accessCount: number
}> => {
  return http.get(`/licenses/${id}/usage-stats`)
}

// 获取授权验证历史
export const getLicenseValidationHistory = (id: number, params: PageQuery): Promise<PageResult<{
  id: number
  licenseId: number
  clientIp: string
  userAgent: string
  validationTime: string
  success: boolean
  errorMessage?: string
}>> => {
  return http.get(`/licenses/${id}/validation-history`, { params })
}

// 生成授权密钥
export const generateLicenseKey = (data: LicenseForm): Promise<{ licenseKey: string }> => {
  return http.post('/licenses/generate-key', data)
}

// 验证授权
export const validateLicense = (licenseKey: string): Promise<{
  valid: boolean
  license?: License
  message: string
}> => {
  return http.post('/licenses/validate', { licenseKey })
}

// 激活授权
export const activateLicense = (id: number): Promise<void> => {
  return http.post(`/licenses/${id}/activate`)
}

// 暂停授权
export const suspendLicense = (id: number): Promise<void> => {
  return http.post(`/licenses/${id}/suspend`)
}

// 续期授权
export const renewLicense = (id: number, endDate: string): Promise<License> => {
  return http.post(`/licenses/${id}/renew`, { endDate })
}

// 导出授权
export const exportLicense = (id: number): Promise<Blob> => {
  return http.get(`/licenses/${id}/export`, { responseType: 'blob' })
}