import { http } from './request'
import type { Customer, CustomerForm, PageQuery, PageResult } from '@/types'

// 获取客户列表
export const getCustomers = (params: PageQuery): Promise<PageResult<Customer>> => {
  return http.get('/customers', { params })
}

// 获取客户详情
export const getCustomer = (id: number): Promise<Customer> => {
  return http.get(`/customers/${id}`)
}

// 创建客户
export const createCustomer = (data: CustomerForm): Promise<Customer> => {
  return http.post('/customers', data)
}

// 更新客户
export const updateCustomer = (id: number, data: CustomerForm): Promise<Customer> => {
  return http.put(`/customers/${id}`, data)
}

// 删除客户
export const deleteCustomer = (id: number): Promise<void> => {
  return http.delete(`/customers/${id}`)
}

// 批量删除客户
export const batchDeleteCustomers = (ids: number[]): Promise<void> => {
  return http.post('/customers/batch-delete', { ids })
}

// 获取客户下拉选项
export const getCustomerOptions = (): Promise<Array<{ label: string; value: number }>> => {
  return http.get('/customers/options')
}

// 导出客户数据
export const exportCustomers = (params: any): Promise<Blob> => {
  return http.get('/customers/export', { 
    params,
    responseType: 'blob'
  })
}

// 导入客户数据
export const importCustomers = (formData: FormData): Promise<{
  successCount: number
  failureCount: number
  errors: Array<{ row: number; message: string }>
}> => {
  return http.post('/customers/import', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 下载导入模板
export const downloadImportTemplate = (): Promise<Blob> => {
  return http.get('/customers/import-template', {
    responseType: 'blob'
  })
}