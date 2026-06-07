<template>
  <div class="license-detail">
    <!-- 页面头部 -->
    <div class="page-header">
      <el-button @click="$router.go(-1)" type="text" size="large">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <div class="header-title">
        <h2>授权详情</h2>
        <div class="header-actions">
          <el-button type="primary" @click="handleEdit" v-if="licenseInfo?.status !== 'expired'">
            编辑授权
          </el-button>
          <el-dropdown @command="handleAction" v-if="licenseInfo">
            <el-button type="default">
              更多操作<el-icon><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="renew" v-if="licenseInfo.status === 'expired'">
                  续期授权
                </el-dropdown-item>
                <el-dropdown-item command="suspend" v-if="licenseInfo.status === 'active'">
                  暂停授权
                </el-dropdown-item>
                <el-dropdown-item command="activate" v-if="licenseInfo.status === 'suspended'">
                  激活授权
                </el-dropdown-item>
                <el-dropdown-item command="validate">
                  验证授权
                </el-dropdown-item>
                <el-dropdown-item command="export">
                  导出授权
                </el-dropdown-item>
                <el-dropdown-item command="revoke" divided>
                  撤销授权
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>

    <div v-loading="loading" class="content">
      <!-- 授权基本信息 -->
      <el-card title="基本信息" class="info-card">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="授权密钥">
            <div class="license-key-display">
              <code>{{ licenseInfo?.licenseKey }}</code>
              <el-button type="text" @click="copyToClipboard(licenseInfo?.licenseKey || '')">
                <el-icon><CopyDocument /></el-icon>
                复制
              </el-button>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="授权状态">
            <el-tag :type="getLicenseStatusTagType(licenseInfo?.status || '')">
              {{ getLicenseStatusText(licenseInfo?.status || '') }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="客户信息">
            <el-link type="primary" @click="handleViewCustomer">
              {{ licenseInfo?.customerName }}
            </el-link>
          </el-descriptions-item>
          <el-descriptions-item label="授权类型">
            <el-tag :type="getLicenseTypeTagType(licenseInfo?.licenseType || '')">
              {{ getLicenseTypeText(licenseInfo?.licenseType || '') }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="产品名称">{{ licenseInfo?.productName }}</el-descriptions-item>
          <el-descriptions-item label="产品版本">{{ licenseInfo?.productVersion }}</el-descriptions-item>
          <el-descriptions-item label="最大用户数">{{ licenseInfo?.maxUsers }}</el-descriptions-item>
          <el-descriptions-item label="当前用户数">
            <span :class="{ 'text-warning': usageStats.currentUsers >= licenseInfo?.maxUsers * 0.8 }">
              {{ usageStats.currentUsers }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="授权期限">
            {{ formatDate(licenseInfo?.startDate, 'YYYY-MM-DD') }} 至 
            {{ formatDate(licenseInfo?.endDate, 'YYYY-MM-DD') }}
          </el-descriptions-item>
          <el-descriptions-item label="剩余天数">
            <span :class="getDaysLeftClass(licenseInfo?.endDate || '')">
              {{ getDaysLeft(licenseInfo?.endDate || '') }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDate(licenseInfo?.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="最后更新">
            {{ formatDate(licenseInfo?.updatedAt) }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 功能特性 -->
      <el-card title="功能特性" class="features-card">
        <div class="features-list">
          <el-tag
            v-for="feature in licenseInfo?.features"
            :key="feature"
            size="large"
            class="feature-tag"
          >
            {{ feature }}
          </el-tag>
          <div v-if="!licenseInfo?.features?.length" class="no-features">
            暂无特殊功能特性
          </div>
        </div>
      </el-card>

      <!-- 使用统计 -->
      <el-row :gutter="20">
        <el-col :span="12">
          <el-card title="使用统计" class="stats-card">
            <div class="usage-stats">
              <div class="stat-item">
                <div class="stat-label">当前在线用户</div>
                <div class="stat-value">{{ usageStats.currentUsers }}</div>
                <div class="stat-progress">
                  <el-progress
                    :percentage="Math.round((usageStats.currentUsers / (licenseInfo?.maxUsers || 1)) * 100)"
                    :color="getProgressColor((usageStats.currentUsers / (licenseInfo?.maxUsers || 1)) * 100)"
                  />
                </div>
              </div>
              <div class="stat-item">
                <div class="stat-label">总访问次数</div>
                <div class="stat-value">{{ usageStats.accessCount.toLocaleString() }}</div>
              </div>
              <div class="stat-item">
                <div class="stat-label">最后访问时间</div>
                <div class="stat-value">
                  {{ usageStats.lastAccessTime ? formatDate(usageStats.lastAccessTime) : '暂无访问记录' }}
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="12">
          <el-card title="授权信息" class="license-info-card">
            <div class="license-qr">
              <div class="qr-placeholder">
                <!-- 这里可以集成二维码生成库 -->
                <el-icon size="64"><QrCode /></el-icon>
                <p>授权二维码</p>
                <el-button type="text" @click="generateQRCode">生成二维码</el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 验证历史 -->
      <el-card title="验证历史" class="history-card">
        <template #extra>
          <el-button type="primary" size="small" @click="refreshValidationHistory">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </template>
        
        <el-table :data="validationHistory" v-loading="historyLoading" max-height="400">
          <el-table-column prop="validationTime" label="验证时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.validationTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="clientIp" label="客户端IP" width="140" />
          <el-table-column prop="userAgent" label="用户代理" show-overflow-tooltip />
          <el-table-column prop="success" label="验证结果" width="100">
            <template #default="{ row }">
              <el-tag :type="row.success ? 'success' : 'danger'">
                {{ row.success ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="errorMessage" label="错误信息" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.errorMessage || '-' }}
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination" v-if="historyPagination.total > 0">
          <el-pagination
            v-model:current-page="historyPagination.page"
            v-model:page-size="historyPagination.size"
            :page-sizes="[10, 20, 50]"
            :total="historyPagination.total"
            layout="total, sizes, prev, pager, next"
            @size-change="handleHistorySizeChange"
            @current-change="handleHistoryPageChange"
          />
        </div>
      </el-card>
    </div>

    <!-- 编辑授权对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑授权"
      width="600px"
      @close="handleEditDialogClose"
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="editFormRules"
        label-width="120px"
      >
        <el-form-item label="产品名称" prop="productName">
          <el-input v-model="editForm.productName" placeholder="请输入产品名称" />
        </el-form-item>
        <el-form-item label="产品版本" prop="productVersion">
          <el-input v-model="editForm.productVersion" placeholder="请输入产品版本" />
        </el-form-item>
        <el-form-item label="最大用户数" prop="maxUsers">
          <el-input-number
            v-model="editForm.maxUsers"
            :min="1"
            :max="10000"
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="授权期限" prop="startDate">
          <el-date-picker
            v-model="editDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 100%"
            @change="handleEditDateChange"
          />
        </el-form-item>
        <el-form-item label="功能特性" prop="features">
          <el-checkbox-group v-model="editForm.features">
            <el-checkbox label="基础功能">基础功能</el-checkbox>
            <el-checkbox label="高级分析">高级分析</el-checkbox>
            <el-checkbox label="API接口">API接口</el-checkbox>
            <el-checkbox label="数据导出">数据导出</el-checkbox>
            <el-checkbox label="多租户">多租户</el-checkbox>
            <el-checkbox label="单点登录">单点登录</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitLoading" @click="handleEditSubmit">
          确认
        </el-button>
      </template>
    </el-dialog>

    <!-- 续期对话框 -->
    <el-dialog
      v-model="renewDialogVisible"
      title="续期授权"
      width="500px"
    >
      <el-form :model="renewForm" label-width="120px">
        <el-form-item label="当前到期时间">
          <span>{{ formatDate(licenseInfo?.endDate, 'YYYY-MM-DD') }}</span>
        </el-form-item>
        <el-form-item label="新的到期时间" required>
          <el-date-picker
            v-model="renewForm.endDate"
            type="date"
            placeholder="选择新的到期时间"
            style="width: 100%"
            :disabled-date="(date) => date <= new Date(licenseInfo?.endDate || '')"
          />
        </el-form-item>
        <el-form-item label="续期理由">
          <el-input
            v-model="renewForm.reason"
            type="textarea"
            rows="3"
            placeholder="请输入续期理由（可选）"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="renewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="renewSubmitLoading" @click="handleRenewSubmit">
          确认续期
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  ArrowLeft,
  ArrowDown,
  CopyDocument,
  Refresh,
  QrCode
} from '@element-plus/icons-vue'
import {
  getLicense,
  updateLicense,
  activateLicense,
  suspendLicense,
  revokeLicense,
  renewLicense,
  validateLicense,
  getLicenseUsageStats,
  getLicenseValidationHistory
} from '@/api/license'
import { formatDate, copyToClipboard, getDaysBetween } from '@/utils'
import type { License, LicenseForm, PageQuery } from '@/types'

const route = useRoute()
const router = useRouter()

const licenseId = computed(() => Number(route.params.id))

const loading = ref(false)
const historyLoading = ref(false)
const editDialogVisible = ref(false)
const renewDialogVisible = ref(false)
const editSubmitLoading = ref(false)
const renewSubmitLoading = ref(false)
const editFormRef = ref<FormInstance>()

const licenseInfo = ref<License | null>(null)
const validationHistory = ref<any[]>([])
const editDateRange = ref<[Date, Date] | null>(null)

const usageStats = ref({
  currentUsers: 0,
  maxUsers: 0,
  lastAccessTime: '',
  accessCount: 0
})

const historyPagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const editForm = reactive<Partial<LicenseForm>>({
  productName: '',
  productVersion: '',
  maxUsers: 1,
  startDate: '',
  endDate: '',
  features: []
})

const renewForm = reactive({
  endDate: null as Date | null,
  reason: ''
})

const editFormRules: FormRules = {
  productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  productVersion: [{ required: true, message: '请输入产品版本', trigger: 'blur' }],
  maxUsers: [{ required: true, message: '请输入最大用户数', trigger: 'blur' }],
  startDate: [{ required: true, message: '请选择授权期限', trigger: 'change' }]
}

// 加载授权信息
const loadLicenseInfo = async () => {
  loading.value = true
  try {
    licenseInfo.value = await getLicense(licenseId.value)
    loadUsageStats()
  } catch (error) {
    console.error('加载授权信息失败:', error)
    ElMessage.error('加载授权信息失败')
  } finally {
    loading.value = false
  }
}

// 加载使用统计
const loadUsageStats = async () => {
  try {
    usageStats.value = await getLicenseUsageStats(licenseId.value)
  } catch (error) {
    console.error('加载使用统计失败:', error)
  }
}

// 加载验证历史
const loadValidationHistory = async () => {
  historyLoading.value = true
  try {
    const params: PageQuery = {
      page: historyPagination.page,
      size: historyPagination.size
    }
    
    const result = await getLicenseValidationHistory(licenseId.value, params)
    validationHistory.value = result.data
    historyPagination.total = result.total
  } catch (error) {
    console.error('加载验证历史失败:', error)
  } finally {
    historyLoading.value = false
  }
}

// 刷新验证历史
const refreshValidationHistory = () => {
  loadValidationHistory()
}

// 分页处理
const handleHistoryPageChange = (page: number) => {
  historyPagination.page = page
  loadValidationHistory()
}

const handleHistorySizeChange = (size: number) => {
  historyPagination.size = size
  historyPagination.page = 1
  loadValidationHistory()
}

// 查看客户
const handleViewCustomer = () => {
  if (licenseInfo.value) {
    router.push(`/customers/${licenseInfo.value.customerId}`)
  }
}

// 编辑授权
const handleEdit = () => {
  if (licenseInfo.value) {
    Object.assign(editForm, {
      productName: licenseInfo.value.productName,
      productVersion: licenseInfo.value.productVersion,
      maxUsers: licenseInfo.value.maxUsers,
      startDate: licenseInfo.value.startDate,
      endDate: licenseInfo.value.endDate,
      features: licenseInfo.value.features
    })
    editDateRange.value = [
      new Date(licenseInfo.value.startDate),
      new Date(licenseInfo.value.endDate)
    ]
    editDialogVisible.value = true
  }
}

// 处理操作命令
const handleAction = async (command: string) => {
  if (!licenseInfo.value) return
  
  try {
    switch (command) {
      case 'activate':
        await ElMessageBox.confirm('确定要激活此授权吗？', '激活确认', {
          type: 'warning'
        })
        await activateLicense(licenseInfo.value.id)
        ElMessage.success('激活成功')
        loadLicenseInfo()
        break
        
      case 'suspend':
        await ElMessageBox.confirm('确定要暂停此授权吗？', '暂停确认', {
          type: 'warning'
        })
        await suspendLicense(licenseInfo.value.id)
        ElMessage.success('暂停成功')
        loadLicenseInfo()
        break
        
      case 'renew':
        renewDialogVisible.value = true
        break
        
      case 'validate':
        const result = await validateLicense(licenseInfo.value.licenseKey)
        if (result.valid) {
          ElMessage.success('授权验证成功')
        } else {
          ElMessage.error(`授权验证失败：${result.message}`)
        }
        break
        
      case 'export':
        ElMessage.info('导出功能开发中')
        break
        
      case 'revoke':
        await ElMessageBox.confirm('确定要撤销此授权吗？撤销后无法恢复！', '撤销确认', {
          type: 'warning'
        })
        await revokeLicense(licenseInfo.value.id)
        ElMessage.success('撤销成功')
        loadLicenseInfo()
        break
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('操作失败:', error)
    }
  }
}

// 编辑日期变更
const handleEditDateChange = (dates: [Date, Date] | null) => {
  if (dates) {
    editForm.startDate = dates[0].toISOString().split('T')[0]
    editForm.endDate = dates[1].toISOString().split('T')[0]
  } else {
    editForm.startDate = ''
    editForm.endDate = ''
  }
}

// 提交编辑
const handleEditSubmit = async () => {
  if (!editFormRef.value) return
  
  const valid = await editFormRef.value.validate()
  if (!valid) return
  
  editSubmitLoading.value = true
  
  try {
    await updateLicense(licenseId.value, editForm)
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    loadLicenseInfo()
  } catch (error) {
    console.error('更新失败:', error)
  } finally {
    editSubmitLoading.value = false
  }
}

// 关闭编辑对话框
const handleEditDialogClose = () => {
  editFormRef.value?.clearValidate()
}

// 提交续期
const handleRenewSubmit = async () => {
  if (!renewForm.endDate) {
    ElMessage.error('请选择新的到期时间')
    return
  }
  
  renewSubmitLoading.value = true
  
  try {
    await renewLicense(licenseId.value, {
      endDate: renewForm.endDate.toISOString().split('T')[0]
    })
    ElMessage.success('续期成功')
    renewDialogVisible.value = false
    renewForm.endDate = null
    renewForm.reason = ''
    loadLicenseInfo()
  } catch (error) {
    console.error('续期失败:', error)
  } finally {
    renewSubmitLoading.value = false
  }
}

// 生成二维码
const generateQRCode = () => {
  ElMessage.info('二维码生成功能开发中')
}

// 工具函数
const getLicenseStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    active: '活跃',
    expired: '已过期',
    suspended: '已暂停'
  }
  return statusMap[status] || status
}

const getLicenseStatusTagType = (status: string) => {
  const statusMap: Record<string, string> = {
    active: 'success',
    expired: 'danger',
    suspended: 'warning'
  }
  return statusMap[status] || 'info'
}

const getLicenseTypeText = (type: string) => {
  const typeMap: Record<string, string> = {
    trial: '试用版',
    standard: '标准版',
    enterprise: '企业版'
  }
  return typeMap[type] || type
}

const getLicenseTypeTagType = (type: string) => {
  const typeMap: Record<string, string> = {
    trial: 'info',
    standard: 'success',
    enterprise: 'warning'
  }
  return typeMap[type] || 'info'
}

const getDaysLeft = (endDate: string) => {
  const days = Math.ceil((new Date(endDate).getTime() - new Date().getTime()) / (1000 * 60 * 60 * 24))
  if (days < 0) return '已过期'
  return `${days} 天`
}

const getDaysLeftClass = (endDate: string) => {
  const days = Math.ceil((new Date(endDate).getTime() - new Date().getTime()) / (1000 * 60 * 60 * 24))
  if (days < 0) return 'text-danger'
  if (days <= 7) return 'text-warning'
  return 'text-success'
}

const getProgressColor = (percentage: number) => {
  if (percentage >= 90) return '#f56c6c'
  if (percentage >= 70) return '#e6a23c'
  return '#67c23a'
}

onMounted(() => {
  loadLicenseInfo()
  loadValidationHistory()
})
</script>

<style scoped>
.license-detail {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.header-title {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-left: 16px;
}

.header-title h2 {
  margin: 0;
  color: #333;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.info-card {
  margin-bottom: 20px;
}

.license-key-display {
  display: flex;
  align-items: center;
  gap: 12px;
}

.license-key-display code {
  background-color: #f5f5f5;
  padding: 8px 12px;
  border-radius: 4px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 14px;
  color: #333;
  border: 1px solid #e0e0e0;
}

.features-card {
  margin-bottom: 20px;
}

.features-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.feature-tag {
  margin-bottom: 8px;
}

.no-features {
  color: #999;
  font-style: italic;
}

.stats-card {
  margin-bottom: 20px;
}

.usage-stats {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.stat-progress {
  margin-top: 8px;
}

.license-info-card {
  margin-bottom: 20px;
}

.qr-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #999;
}

.qr-placeholder p {
  margin: 8px 0;
}

.history-card {
  margin-bottom: 20px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.text-success {
  color: #67c23a;
}

.text-warning {
  color: #e6a23c;
}

.text-danger {
  color: #f56c6c;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .license-detail {
    padding: 12px;
  }
  
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .header-title {
    width: 100%;
    flex-direction: column;
    align-items: flex-start;
    margin-left: 0;
    margin-top: 16px;
  }
  
  .header-actions {
    margin-top: 12px;
  }
  
  .features-list {
    flex-direction: column;
  }
  
  .usage-stats {
    gap: 12px;
  }
}
</style>