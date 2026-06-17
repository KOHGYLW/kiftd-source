<template>
  <div class="license-management">
    <!-- 搜索和操作区域 -->
    <el-card class="search-card">
      <el-row :gutter="20">
        <el-col :span="5">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索授权密钥或客户名称"
            prefix-icon="Search"
            @input="handleSearch"
          />
        </el-col>
        <el-col :span="3">
          <el-select v-model="searchForm.status" placeholder="状态筛选" @change="handleSearch" clearable>
            <el-option label="全部状态" value="" />
            <el-option label="活跃" value="active" />
            <el-option label="已过期" value="expired" />
            <el-option label="已暂停" value="suspended" />
          </el-select>
        </el-col>
        <el-col :span="3">
          <el-select v-model="searchForm.licenseType" placeholder="类型筛选" @change="handleSearch" clearable>
            <el-option label="全部类型" value="" />
            <el-option label="试用版" value="trial" />
            <el-option label="标准版" value="standard" />
            <el-option label="企业版" value="enterprise" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="searchForm.customerId" placeholder="客户筛选" @change="handleSearch" clearable>
            <el-option label="全部客户" value="" />
            <el-option
              v-for="customer in customerOptions"
              :key="customer.value"
              :label="customer.label"
              :value="customer.value"
            />
          </el-select>
        </el-col>
        <el-col :span="5">
          <el-date-picker
            v-model="searchForm.expireDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="过期开始日期"
            end-placeholder="过期结束日期"
            @change="handleSearch"
            style="width: 100%"
          />
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="handleAdd">生成授权</el-button>
          <el-button type="default" @click="handleReset">重置</el-button>
        </el-col>
      </el-row>
      
      <!-- 第二行操作按钮 -->
      <el-row :gutter="20" style="margin-top: 16px;">
        <el-col :span="24">
          <el-button
            type="danger"
            :disabled="selectedIds.length === 0"
            @click="handleBatchDelete"
          >
            批量删除 ({{ selectedIds.length }})
          </el-button>
          <el-button
            type="success"
            @click="handleExport"
          >
            导出数据
          </el-button>
          <el-button
            type="warning"
            @click="handleBatchActivate"
            :disabled="selectedIds.length === 0"
          >
            批量激活
          </el-button>
          <el-button
            type="info"
            @click="handleBatchSuspend"
            :disabled="selectedIds.length === 0"
          >
            批量暂停
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 表格区域 -->
    <el-card class="table-card">
      <el-table
        v-loading="loading"
        :data="tableData"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="customerName" label="客户名称" width="120" />
        <el-table-column prop="productName" label="产品名称" width="120" />
        <el-table-column prop="licenseKey" label="授权密钥" width="200">
          <template #default="{ row }">
            <div class="license-key">
              <span>{{ maskLicenseKey(row.licenseKey) }}</span>
              <el-button
                type="text"
                size="small"
                @click="copyLicenseKey(row.licenseKey)"
              >
                复制
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="licenseType" label="授权类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getLicenseTypeColor(row.licenseType)">
              {{ getLicenseTypeText(row.licenseType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="maxUsers" label="最大用户数" width="100" />
        <el-table-column prop="endDate" label="到期时间" width="120">
          <template #default="{ row }">
            {{ formatDate(row.endDate, 'YYYY-MM-DD') }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusColor(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleView(row)">
              查看
            </el-button>
            <el-button type="warning" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-dropdown @command="(command) => handleAction(command, row)">
              <el-button type="info" size="small">
                更多<el-icon class="el-icon--right"><arrow-down /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="row.status === 'suspended'" command="activate">
                    激活
                  </el-dropdown-item>
                  <el-dropdown-item v-if="row.status === 'active'" command="suspend">
                    暂停
                  </el-dropdown-item>
                  <el-dropdown-item command="renew">续期</el-dropdown-item>
                  <el-dropdown-item command="export">导出</el-dropdown-item>
                  <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="120px"
      >
        <el-form-item label="客户" prop="customerId">
          <el-select
            v-model="form.customerId"
            placeholder="请选择客户"
            style="width: 100%"
            filterable
          >
            <el-option
              v-for="customer in customerOptions"
              :key="customer.value"
              :label="customer.label"
              :value="customer.value"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="产品名称" prop="productName">
          <el-input v-model="form.productName" placeholder="请输入产品名称" />
        </el-form-item>
        
        <el-form-item label="产品版本" prop="productVersion">
          <el-input v-model="form.productVersion" placeholder="请输入产品版本" />
        </el-form-item>
        
        <el-form-item label="授权类型" prop="licenseType">
          <el-radio-group v-model="form.licenseType">
            <el-radio label="trial">试用版</el-radio>
            <el-radio label="standard">标准版</el-radio>
            <el-radio label="enterprise">企业版</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="最大用户数" prop="maxUsers">
          <el-input-number
            v-model="form.maxUsers"
            :min="1"
            :max="10000"
            placeholder="请输入最大用户数"
            style="width: 100%"
          />
        </el-form-item>
        
        <el-form-item label="授权期限" prop="startDate">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 100%"
            @change="handleDateChange"
          />
        </el-form-item>
        
        <el-form-item label="功能特性" prop="features">
          <el-checkbox-group v-model="form.features">
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
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确认
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import {
  getLicenses,
  createLicense,
  updateLicense,
  deleteLicense,
  batchDeleteLicenses,
  activateLicense,
  suspendLicense,
  renewLicense
} from '@/api/license'
import { getCustomerOptions } from '@/api/customer'
import { formatDate, debounce, copyToClipboard } from '@/utils'
import type { License, LicenseForm, PageQuery } from '@/types'

const router = useRouter()

const loading = ref(false)
const dialogVisible = ref(false)
const submitLoading = ref(false)
const selectedIds = ref<number[]>([])
const currentLicense = ref<License | null>(null)
const formRef = ref<FormInstance>()
const customerOptions = ref<Array<{ label: string; value: number }>>([])

const searchForm = reactive({
  keyword: '',
  status: '',
  licenseType: '',
  customerId: '',
  expireDateRange: null as [Date, Date] | null
})

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const tableData = ref<License[]>([])
const dateRange = ref<[Date, Date] | null>(null)

const form = reactive<LicenseForm>({
  customerId: 0,
  productName: '',
  productVersion: '',
  licenseType: 'standard',
  maxUsers: 1,
  startDate: '',
  endDate: '',
  features: []
})

const formRules: FormRules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  productVersion: [{ required: true, message: '请输入产品版本', trigger: 'blur' }],
  licenseType: [{ required: true, message: '请选择授权类型', trigger: 'change' }],
  maxUsers: [{ required: true, message: '请输入最大用户数', trigger: 'blur' }],
  startDate: [{ required: true, message: '请选择授权期限', trigger: 'change' }]
}

const dialogTitle = computed(() => {
  return currentLicense.value ? '编辑授权' : '生成授权'
})

// 工具函数
const maskLicenseKey = (key: string) => {
  if (key.length <= 8) return key
  return key.substring(0, 4) + '****' + key.substring(key.length - 4)
}

const getLicenseTypeText = (type: string) => {
  const map = { trial: '试用版', standard: '标准版', enterprise: '企业版' }
  return map[type] || type
}

const getLicenseTypeColor = (type: string) => {
  const map = { trial: 'info', standard: 'success', enterprise: 'warning' }
  return map[type] || 'info'
}

const getStatusText = (status: string) => {
  const map = { active: '活跃', expired: '已过期', suspended: '已暂停' }
  return map[status] || status
}

const getStatusColor = (status: string) => {
  const map = { active: 'success', expired: 'danger', suspended: 'warning' }
  return map[status] || 'info'
}

// 复制授权密钥
const copyLicenseKey = (key: string) => {
  copyToClipboard(key)
}

// 加载数据
const loadLicenses = async () => {
  loading.value = true
  try {
    const params: any = {
      page: pagination.page,
      size: pagination.size,
      keyword: searchForm.keyword
    }
    
    if (searchForm.status) params.status = searchForm.status
    if (searchForm.licenseType) params.licenseType = searchForm.licenseType
    if (searchForm.customerId) params.customerId = searchForm.customerId
    if (searchForm.expireDateRange) {
      params.expireStartDate = searchForm.expireDateRange[0].toISOString().split('T')[0]
      params.expireEndDate = searchForm.expireDateRange[1].toISOString().split('T')[0]
    }
    
    const result = await getLicenses(params)
    tableData.value = result.data
    pagination.total = result.total
  } catch (error) {
    console.error('加载授权列表失败:', error)
  } finally {
    loading.value = false
  }
}

const loadCustomerOptions = async () => {
  try {
    customerOptions.value = await getCustomerOptions()
  } catch (error) {
    console.error('加载客户选项失败:', error)
  }
}

// 事件处理
const handleSearch = debounce(() => {
  pagination.page = 1
  loadLicenses()
}, 300)

const handlePageChange = (page: number) => {
  pagination.page = page
  loadLicenses()
}

const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 1
  loadLicenses()
}

const handleSelectionChange = (selection: License[]) => {
  selectedIds.value = selection.map(item => item.id)
}

// 重置搜索表单
const handleReset = () => {
  Object.assign(searchForm, {
    keyword: '',
    status: '',
    licenseType: '',
    customerId: '',
    expireDateRange: null
  })
  pagination.page = 1
  loadLicenses()
}

// 导出数据
const handleExport = async () => {
  try {
    ElMessage.info('导出功能开发中，将支持导出所选授权数据')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  }
}

// 批量激活
const handleBatchActivate = async () => {
  try {
    await ElMessageBox.confirm(`确定要激活选中的 ${selectedIds.value.length} 个授权吗？`, '批量激活确认', {
      type: 'warning'
    })
    
    // 这里应该调用批量激活API
    ElMessage.info('批量激活功能开发中')
    selectedIds.value = []
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量激活失败:', error)
    }
  }
}

// 批量暂停
const handleBatchSuspend = async () => {
  try {
    await ElMessageBox.confirm(`确定要暂停选中的 ${selectedIds.value.length} 个授权吗？`, '批量暂停确认', {
      type: 'warning'
    })
    
    // 这里应该调用批量暂停API
    ElMessage.info('批量暂停功能开发中')
    selectedIds.value = []
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量暂停失败:', error)
    }
  }
}

const handleAdd = () => {
  router.push('/licenses/create')
}

const handleEdit = (license: License) => {
  currentLicense.value = license
  Object.assign(form, {
    customerId: license.customerId,
    productName: license.productName,
    productVersion: license.productVersion,
    licenseType: license.licenseType,
    maxUsers: license.maxUsers,
    startDate: license.startDate,
    endDate: license.endDate,
    features: license.features
  })
  dateRange.value = [new Date(license.startDate), new Date(license.endDate)]
  dialogVisible.value = true
}

const handleView = (license: License) => {
  router.push(`/licenses/${license.id}`)
}

const handleAction = async (command: string, license: License) => {
  try {
    switch (command) {
      case 'activate':
        await activateLicense(license.id)
        ElMessage.success('激活成功')
        loadLicenses()
        break
      case 'suspend':
        await suspendLicense(license.id)
        ElMessage.success('暂停成功')
        loadLicenses()
        break
      case 'renew':
        // 这里可以打开续期对话框
        ElMessage.info('续期功能开发中')
        break
      case 'export':
        ElMessage.info('导出功能开发中')
        break
      case 'delete':
        await ElMessageBox.confirm(`确定要删除授权 "${license.licenseKey}" 吗？`, '删除确认', {
          type: 'warning'
        })
        await deleteLicense(license.id)
        ElMessage.success('删除成功')
        loadLicenses()
        break
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('操作失败:', error)
    }
  }
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 个授权吗？`, '批量删除确认', {
      type: 'warning'
    })
    
    await batchDeleteLicenses(selectedIds.value)
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    loadLicenses()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
    }
  }
}

const handleDateChange = (dates: [Date, Date] | null) => {
  if (dates) {
    form.startDate = dates[0].toISOString().split('T')[0]
    form.endDate = dates[1].toISOString().split('T')[0]
  } else {
    form.startDate = ''
    form.endDate = ''
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  const valid = await formRef.value.validate()
  if (!valid) return
  
  submitLoading.value = true
  
  try {
    if (currentLicense.value) {
      await updateLicense(currentLicense.value.id, form)
      ElMessage.success('更新成功')
    } else {
      await createLicense(form)
      ElMessage.success('生成成功')
    }
    
    dialogVisible.value = false
    loadLicenses()
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitLoading.value = false
  }
}

const resetForm = () => {
  Object.assign(form, {
    customerId: 0,
    productName: '',
    productVersion: '',
    licenseType: 'standard',
    maxUsers: 1,
    startDate: '',
    endDate: '',
    features: []
  })
  dateRange.value = null
  formRef.value?.clearValidate()
}

const handleDialogClose = () => {
  resetForm()
  currentLicense.value = null
}

onMounted(() => {
  loadLicenses()
  loadCustomerOptions()
})
</script>

<style scoped>
.license-management {
  padding: 20px;
}

.search-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

.license-key {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>