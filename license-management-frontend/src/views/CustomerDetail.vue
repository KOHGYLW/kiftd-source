<template>
  <div class="customer-detail">
    <!-- 页面头部 -->
    <div class="page-header">
      <el-button @click="$router.go(-1)" type="text" size="large">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <div class="header-title">
        <h2>客户详情</h2>
        <div class="header-actions">
          <el-button type="primary" @click="handleEdit">编辑客户</el-button>
          <el-button type="success" @click="handleCreateLicense">创建授权</el-button>
        </div>
      </div>
    </div>

    <div v-loading="loading" class="content">
      <!-- 客户基本信息 -->
      <el-card title="基本信息" class="info-card">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="客户姓名">
            {{ customerInfo?.name }}
          </el-descriptions-item>
          <el-descriptions-item label="公司名称">
            {{ customerInfo?.company }}
          </el-descriptions-item>
          <el-descriptions-item label="邮箱地址">
            {{ customerInfo?.email }}
          </el-descriptions-item>
          <el-descriptions-item label="联系电话">
            {{ customerInfo?.phone }}
          </el-descriptions-item>
          <el-descriptions-item label="详细地址" :span="2">
            {{ customerInfo?.address }}
          </el-descriptions-item>
          <el-descriptions-item label="客户状态">
            <el-tag :type="customerInfo?.status === 'active' ? 'success' : 'warning'">
              {{ customerInfo?.status === 'active' ? '活跃' : '未激活' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDate(customerInfo?.createdAt) }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 授权统计 -->
      <el-row :gutter="20" class="stats-row">
        <el-col :span="6">
          <el-card class="stats-card">
            <div class="stats-item">
              <div class="stats-icon total">
                <el-icon><Key /></el-icon>
              </div>
              <div class="stats-content">
                <div class="stats-value">{{ licenseStats.total }}</div>
                <div class="stats-label">授权总数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="6">
          <el-card class="stats-card">
            <div class="stats-item">
              <div class="stats-icon active">
                <el-icon><Check /></el-icon>
              </div>
              <div class="stats-content">
                <div class="stats-value">{{ licenseStats.active }}</div>
                <div class="stats-label">活跃授权</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="6">
          <el-card class="stats-card">
            <div class="stats-item">
              <div class="stats-icon expired">
                <el-icon><Close /></el-icon>
              </div>
              <div class="stats-content">
                <div class="stats-value">{{ licenseStats.expired }}</div>
                <div class="stats-label">已过期</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="6">
          <el-card class="stats-card">
            <div class="stats-item">
              <div class="stats-icon warning">
                <el-icon><Warning /></el-icon>
              </div>
              <div class="stats-content">
                <div class="stats-value">{{ licenseStats.expiring }}</div>
                <div class="stats-label">即将过期</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 授权历史 -->
      <el-card title="授权历史" class="license-history">
        <template #extra>
          <el-button type="primary" size="small" @click="refreshLicenses">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </template>
        
        <el-table :data="licenseList" v-loading="licenseLoading">
          <el-table-column prop="licenseKey" label="授权码" width="200">
            <template #default="{ row }">
              <el-text type="primary" class="license-key" @click="copyToClipboard(row.licenseKey)">
                {{ row.licenseKey }}
                <el-icon><CopyDocument /></el-icon>
              </el-text>
            </template>
          </el-table-column>
          <el-table-column prop="productName" label="产品名称" />
          <el-table-column prop="productVersion" label="产品版本" />
          <el-table-column prop="licenseType" label="授权类型">
            <template #default="{ row }">
              <el-tag :type="getLicenseTypeTagType(row.licenseType)">
                {{ getLicenseTypeText(row.licenseType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="maxUsers" label="最大用户数" />
          <el-table-column prop="startDate" label="开始日期" width="120">
            <template #default="{ row }">
              {{ formatDate(row.startDate, 'YYYY-MM-DD') }}
            </template>
          </el-table-column>
          <el-table-column prop="endDate" label="结束日期" width="120">
            <template #default="{ row }">
              {{ formatDate(row.endDate, 'YYYY-MM-DD') }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getLicenseStatusTagType(row.status)">
                {{ getLicenseStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button type="primary" size="small" @click="handleViewLicense(row)">
                查看
              </el-button>
              <el-dropdown @command="(command) => handleLicenseAction(command, row)">
                <el-button size="small">
                  更多<el-icon><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="renew" v-if="row.status === 'expired'">
                      续期
                    </el-dropdown-item>
                    <el-dropdown-item command="suspend" v-if="row.status === 'active'">
                      暂停
                    </el-dropdown-item>
                    <el-dropdown-item command="activate" v-if="row.status === 'suspended'">
                      激活
                    </el-dropdown-item>
                    <el-dropdown-item command="revoke" divided>
                      撤销
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination" v-if="licensePagination.total > 0">
          <el-pagination
            v-model:current-page="licensePagination.page"
            v-model:page-size="licensePagination.size"
            :page-sizes="[10, 20, 50]"
            :total="licensePagination.total"
            layout="total, sizes, prev, pager, next"
            @size-change="handleLicenseSizeChange"
            @current-change="handleLicensePageChange"
          />
        </div>
      </el-card>
    </div>

    <!-- 编辑客户对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑客户"
      width="600px"
      @close="handleEditDialogClose"
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="editFormRules"
        label-width="100px"
      >
        <el-form-item label="客户姓名" prop="name">
          <el-input v-model="editForm.name" placeholder="请输入客户姓名" />
        </el-form-item>
        <el-form-item label="公司名称" prop="company">
          <el-input v-model="editForm.company" placeholder="请输入公司名称" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" placeholder="请输入邮箱地址" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="editForm.phone" placeholder="请输入电话号码" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input
            v-model="editForm.address"
            type="textarea"
            rows="3"
            placeholder="请输入详细地址"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="editForm.status">
            <el-radio label="active">活跃</el-radio>
            <el-radio label="inactive">未激活</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitLoading" @click="handleEditSubmit">
          确认
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
  Key,
  Check,
  Close,
  Warning,
  Refresh,
  CopyDocument,
  ArrowDown
} from '@element-plus/icons-vue'
import { getCustomer, updateCustomer } from '@/api/customer'
import { getLicensesByCustomer } from '@/api/license'
import { formatDate, copyToClipboard } from '@/utils'
import type { Customer, CustomerForm, License, PageQuery } from '@/types'

const route = useRoute()
const router = useRouter()

const customerId = computed(() => Number(route.params.id))

const loading = ref(false)
const licenseLoading = ref(false)
const editDialogVisible = ref(false)
const editSubmitLoading = ref(false)
const editFormRef = ref<FormInstance>()

const customerInfo = ref<Customer | null>(null)
const licenseList = ref<License[]>([])

const licensePagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const editForm = reactive<CustomerForm>({
  name: '',
  company: '',
  email: '',
  phone: '',
  address: '',
  status: 'active'
})

const editFormRules: FormRules = {
  name: [{ required: true, message: '请输入客户姓名', trigger: 'blur' }],
  company: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [{ required: true, message: '请输入电话号码', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

// 计算授权统计
const licenseStats = computed(() => {
  const stats = {
    total: licenseList.value.length,
    active: 0,
    expired: 0,
    expiring: 0
  }

  licenseList.value.forEach(license => {
    switch (license.status) {
      case 'active':
        stats.active++
        // 检查是否即将过期（30天内）
        const daysLeft = Math.ceil((new Date(license.endDate).getTime() - new Date().getTime()) / (1000 * 60 * 60 * 24))
        if (daysLeft <= 30 && daysLeft > 0) {
          stats.expiring++
        }
        break
      case 'expired':
        stats.expired++
        break
    }
  })

  return stats
})

// 加载客户信息
const loadCustomerInfo = async () => {
  loading.value = true
  try {
    customerInfo.value = await getCustomer(customerId.value)
  } catch (error) {
    console.error('加载客户信息失败:', error)
    ElMessage.error('加载客户信息失败')
  } finally {
    loading.value = false
  }
}

// 加载客户授权列表
const loadCustomerLicenses = async () => {
  licenseLoading.value = true
  try {
    const params: PageQuery = {
      page: licensePagination.page,
      size: licensePagination.size
    }
    
    const result = await getLicensesByCustomer(customerId.value, params)
    licenseList.value = result.data
    licensePagination.total = result.total
  } catch (error) {
    console.error('加载授权列表失败:', error)
    ElMessage.error('加载授权列表失败')
  } finally {
    licenseLoading.value = false
  }
}

// 刷新授权列表
const refreshLicenses = () => {
  loadCustomerLicenses()
}

// 分页处理
const handleLicensePageChange = (page: number) => {
  licensePagination.page = page
  loadCustomerLicenses()
}

const handleLicenseSizeChange = (size: number) => {
  licensePagination.size = size
  licensePagination.page = 1
  loadCustomerLicenses()
}

// 编辑客户
const handleEdit = () => {
  if (customerInfo.value) {
    Object.assign(editForm, customerInfo.value)
    editDialogVisible.value = true
  }
}

// 创建授权
const handleCreateLicense = () => {
  router.push(`/licenses/create?customerId=${customerId.value}`)
}

// 查看授权详情
const handleViewLicense = (license: License) => {
  router.push(`/licenses/${license.id}`)
}

// 授权操作
const handleLicenseAction = async (command: string, license: License) => {
  try {
    switch (command) {
      case 'renew':
        ElMessage.info(`续期功能开发中 - ${license.licenseKey}`)
        break
      case 'suspend':
        await ElMessageBox.confirm('确定要暂停此授权吗？', '暂停确认', {
          type: 'warning'
        })
        ElMessage.info(`暂停功能开发中 - ${license.licenseKey}`)
        break
      case 'activate':
        await ElMessageBox.confirm('确定要激活此授权吗？', '激活确认', {
          type: 'warning'
        })
        ElMessage.info(`激活功能开发中 - ${license.licenseKey}`)
        break
      case 'revoke':
        await ElMessageBox.confirm('确定要撤销此授权吗？撤销后无法恢复！', '撤销确认', {
          type: 'warning'
        })
        ElMessage.info(`撤销功能开发中 - ${license.licenseKey}`)
        break
    }
  } catch (error) {
    // 用户取消操作
  }
}

// 提交编辑表单
const handleEditSubmit = async () => {
  if (!editFormRef.value) return
  
  const valid = await editFormRef.value.validate()
  if (!valid) return
  
  editSubmitLoading.value = true
  
  try {
    await updateCustomer(customerId.value, editForm)
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    loadCustomerInfo()
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

// 获取授权类型标签类型
const getLicenseTypeTagType = (type: string) => {
  const typeMap: Record<string, string> = {
    trial: 'info',
    standard: 'success',
    enterprise: 'warning'
  }
  return typeMap[type] || 'info'
}

// 获取授权类型文本
const getLicenseTypeText = (type: string) => {
  const typeMap: Record<string, string> = {
    trial: '试用版',
    standard: '标准版',
    enterprise: '企业版'
  }
  return typeMap[type] || type
}

// 获取授权状态标签类型
const getLicenseStatusTagType = (status: string) => {
  const statusMap: Record<string, string> = {
    active: 'success',
    expired: 'danger',
    suspended: 'warning'
  }
  return statusMap[status] || 'info'
}

// 获取授权状态文本
const getLicenseStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    active: '活跃',
    expired: '已过期',
    suspended: '已暂停'
  }
  return statusMap[status] || status
}

onMounted(() => {
  loadCustomerInfo()
  loadCustomerLicenses()
})
</script>

<style scoped>
.customer-detail {
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

.stats-row {
  margin-bottom: 20px;
}

.stats-card {
  height: 120px;
}

.stats-item {
  display: flex;
  align-items: center;
  height: 100%;
}

.stats-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20px;
  font-size: 24px;
  color: white;
}

.stats-icon.total {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stats-icon.active {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stats-icon.expired {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
}

.stats-icon.warning {
  background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%);
}

.stats-content {
  flex: 1;
}

.stats-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 5px;
}

.stats-label {
  font-size: 14px;
  color: #666;
}

.license-history {
  margin-bottom: 20px;
}

.license-key {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}

.license-key:hover {
  color: #409eff;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
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
  
  .stats-row .el-col {
    margin-bottom: 20px;
  }
}
</style>