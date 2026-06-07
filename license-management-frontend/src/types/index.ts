// 用户相关类型
export interface User {
  id: number
  username: string
  email: string
  role: 'admin' | 'user'
  status: 'active' | 'inactive'
  createdAt: string
  updatedAt: string
}

export interface LoginForm {
  username: string
  password: string
  remember?: boolean
}

export interface LoginResponse {
  token: string
  user: User
  expiresIn: number
}

// 客户相关类型
export interface Customer {
  id: number
  name: string
  company: string
  email: string
  phone: string
  address: string
  status: 'active' | 'inactive'
  createdAt: string
  updatedAt: string
}

export interface CustomerForm {
  name: string
  company: string
  email: string
  phone: string
  address: string
  status: 'active' | 'inactive'
}

// 授权相关类型
export interface License {
  id: number
  customerId: number
  customerName: string
  licenseKey: string
  productName: string
  productVersion: string
  licenseType: 'trial' | 'standard' | 'enterprise'
  maxUsers: number
  startDate: string
  endDate: string
  status: 'active' | 'expired' | 'suspended'
  features: string[]
  createdAt: string
  updatedAt: string
}

export interface LicenseForm {
  customerId: number
  productName: string
  productVersion: string
  licenseType: 'trial' | 'standard' | 'enterprise'
  maxUsers: number
  startDate: string
  endDate: string
  features: string[]
}

// 分页相关类型
export interface PageQuery {
  page: number
  size: number
  keyword?: string
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}

export interface PageResult<T> {
  data: T[]
  total: number
  page: number
  size: number
}

// API响应类型
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  success: boolean
}

// 统计数据类型
export interface DashboardStats {
  totalCustomers: number
  totalLicenses: number
  activeLicenses: number
  expiredLicenses: number
  revenueThisMonth: number
  revenueGrowth: number
  customerGrowth: number
  licenseGrowth: number
}

export interface ChartData {
  name: string
  value: number
}

// 系统设置类型
export interface SystemSettings {
  siteName: string
  siteDescription: string
  logoUrl: string
  defaultLicenseType: string
  defaultLicenseDuration: number
  emailSettings: {
    smtpHost: string
    smtpPort: number
    smtpUser: string
    smtpPassword: string
    fromEmail: string
  }
  notificationSettings: {
    enableEmailNotification: boolean
    enableSmsNotification: boolean
    licenseExpiryReminder: number
  }
}