import { http } from './request'
import type { DashboardStats, ChartData } from '@/types'

// 获取仪表板统计数据
export const getDashboardStats = (): Promise<DashboardStats> => {
  return http.get('/dashboard/stats')
}

// 获取客户增长趋势
export const getCustomerTrend = (days = 30): Promise<ChartData[]> => {
  return http.get('/dashboard/customer-trend', { params: { days } })
}

// 获取授权类型分布
export const getLicenseTypeDistribution = (): Promise<ChartData[]> => {
  return http.get('/dashboard/license-type-distribution')
}

// 获取收入趋势
export const getRevenueTrend = (days = 30): Promise<ChartData[]> => {
  return http.get('/dashboard/revenue-trend', { params: { days } })
}

// 获取即将过期的授权
export const getExpiringLicenses = (days = 30): Promise<Array<{
  id: number
  customerName: string
  productName: string
  endDate: string
  daysLeft: number
}>> => {
  return http.get('/dashboard/expiring-licenses', { params: { days } })
}