<template>
  <div class="customer-management">
    <!-- 搜索和操作区域 -->
    <el-card class="search-card">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索客户名称或公司"
            prefix-icon="Search"
            @input="handleSearch"
          />
        </el-col>
        <el-col :span="4">
          <el-select v-model="searchForm.status" placeholder="状态筛选" @change="handleSearch">
            <el-option label="全部" value="" />
            <el-option label="活跃" value="active" />
            <el-option label="未激活" value="inactive" />
          </el-select>
        </el-col>
        <el-col :span="8">
          <el-button type="primary" @click="handleAdd">新增客户</el-button>
          <el-button
            type="danger"
            :disabled="selectedIds.length === 0"
            @click="handleBatchDelete"
          >
            批量删除
          </el-button>
          <el-button
            type="success"
            @click="handleExport"
          >
            导出数据
          </el-button>
          <el-upload
            ref="uploadRef"
            :show-file-list="false"
            :before-upload="handleImport"
            accept=".xlsx,.xls,.csv"
            style="display: inline-block; margin-left: 8px;"
          >
            <el-button type="warning">导入数据</el-button>
          </el-upload>
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
        <el-table-column prop="name" label="客户姓名" />
        <el-table-column prop="company" label="公司名称" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="phone" label="电话" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'warning'">
              {{ row.status === 'active' ? '活跃' : '未激活' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleView(row)">
              查看
            </el-button>
            <el-button type="warning" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">
              删除
            </el-button>
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
      width="600px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="客户姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入客户姓名" />
        </el-form-item>
        <el-form-item label="公司名称" prop="company">
          <el-input v-model="form.company" placeholder="请输入公司名称" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱地址" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入电话号码" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input
            v-model="form.address"
            type="textarea"
            rows="3"
            placeholder="请输入详细地址"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="active">活跃</el-radio>
            <el-radio label="inactive">未激活</el-radio>
          </el-radio-group>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getCustomers, createCustomer, updateCustomer, deleteCustomer, batchDeleteCustomers } from '@/api/customer'
import { formatDate, debounce } from '@/utils'
import type { Customer, CustomerForm, PageQuery } from '@/types'

const router = useRouter()

const loading = ref(false)
const dialogVisible = ref(false)
const submitLoading = ref(false)
const selectedIds = ref<number[]>([])
const currentCustomer = ref<Customer | null>(null)
const formRef = ref<FormInstance>()
const uploadRef = ref()

const searchForm = reactive({
  keyword: '',
  status: ''
})

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const tableData = ref<Customer[]>([])

const form = reactive<CustomerForm>({
  name: '',
  company: '',
  email: '',
  phone: '',
  address: '',
  status: 'active'
})

const formRules: FormRules = {
  name: [
    { required: true, message: '请输入客户姓名', trigger: 'blur' },
    { min: 2, max: 50, message: '姓名长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  company: [
    { required: true, message: '请输入公司名称', trigger: 'blur' },
    { min: 2, max: 100, message: '公司名称长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
    { max: 100, message: '邮箱长度不能超过 100 个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入电话号码', trigger: 'blur' },
    { 
      pattern: /^1[3-9]\d{9}$|^(\d{3,4}-?)?\d{7,8}$/, 
      message: '请输入正确的手机号或座机号', 
      trigger: 'blur' 
    }
  ],
  address: [
    { max: 200, message: '地址长度不能超过 200 个字符', trigger: 'blur' }
  ],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const dialogTitle = computed(() => {
  return currentCustomer.value ? '编辑客户' : '新增客户'
})

// 加载客户列表
const loadCustomers = async () => {
  loading.value = true
  try {
    const params: PageQuery = {
      page: pagination.page,
      size: pagination.size,
      keyword: searchForm.keyword,
      status: searchForm.status
    }
    
    const result = await getCustomers(params)
    tableData.value = result.data
    pagination.total = result.total
  } catch (error) {
    console.error('加载客户列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索处理
const handleSearch = debounce(() => {
  pagination.page = 1
  loadCustomers()
}, 300)

// 分页处理
const handlePageChange = (page: number) => {
  pagination.page = page
  loadCustomers()
}

const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 1
  loadCustomers()
}

// 选择处理
const handleSelectionChange = (selection: Customer[]) => {
  selectedIds.value = selection.map(item => item.id)
}

// 新增客户
const handleAdd = () => {
  currentCustomer.value = null
  resetForm()
  dialogVisible.value = true
}

// 编辑客户
const handleEdit = (customer: Customer) => {
  currentCustomer.value = customer
  Object.assign(form, customer)
  dialogVisible.value = true
}

// 查看客户
const handleView = (customer: Customer) => {
  router.push(`/customers/${customer.id}`)
}

// 删除客户
const handleDelete = async (customer: Customer) => {
  try {
    await ElMessageBox.confirm(`确定要删除客户 "${customer.name}" 吗？`, '删除确认', {
      type: 'warning'
    })
    
    await deleteCustomer(customer.id)
    ElMessage.success('删除成功')
    loadCustomers()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除客户失败:', error)
    }
  }
}

// 批量删除
const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 个客户吗？`, '批量删除确认', {
      type: 'warning'
    })
    
    await batchDeleteCustomers(selectedIds.value)
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    loadCustomers()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
    }
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  const valid = await formRef.value.validate()
  if (!valid) return
  
  submitLoading.value = true
  
  try {
    if (currentCustomer.value) {
      await updateCustomer(currentCustomer.value.id, form)
      ElMessage.success('更新成功')
    } else {
      await createCustomer(form)
      ElMessage.success('创建成功')
    }
    
    dialogVisible.value = false
    loadCustomers()
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitLoading.value = false
  }
}

// 重置表单
const resetForm = () => {
  Object.assign(form, {
    name: '',
    company: '',
    email: '',
    phone: '',
    address: '',
    status: 'active'
  })
  formRef.value?.clearValidate()
}

// 对话框关闭处理
const handleDialogClose = () => {
  resetForm()
  currentCustomer.value = null
}

// 导出数据
const handleExport = async () => {
  try {
    const params = {
      keyword: searchForm.keyword,
      status: searchForm.status
    }
    
    // 这里应该调用导出API
    ElMessage.info('导出功能开发中，将支持Excel格式导出')
    
    // 模拟导出逻辑
    // const blob = await exportCustomers(params)
    // const url = window.URL.createObjectURL(blob)
    // const link = document.createElement('a')
    // link.href = url
    // link.download = `customers_${formatDate(new Date(), 'YYYY-MM-DD')}.xlsx`
    // link.click()
    // window.URL.revokeObjectURL(url)
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  }
}

// 导入数据
const handleImport = (file: File) => {
  const allowedTypes = [
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'application/vnd.ms-excel',
    'text/csv'
  ]
  
  if (!allowedTypes.includes(file.type)) {
    ElMessage.error('只支持 Excel (.xlsx, .xls) 和 CSV 格式')
    return false
  }
  
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 5MB')
    return false
  }
  
  // 这里应该调用导入API
  ElMessage.info('导入功能开发中，将支持Excel和CSV格式导入')
  
  // 模拟导入逻辑
  // const formData = new FormData()
  // formData.append('file', file)
  // 
  // try {
  //   const result = await importCustomers(formData)
  //   ElMessage.success(`导入成功：${result.successCount} 条记录`)
  //   if (result.failureCount > 0) {
  //     ElMessage.warning(`${result.failureCount} 条记录导入失败`)
  //   }
  //   loadCustomers()
  // } catch (error) {
  //   console.error('导入失败:', error)
  //   ElMessage.error('导入失败')
  // }
  
  return false // 阻止自动上传
}

onMounted(() => {
  loadCustomers()
})
</script>

<style scoped>
.customer-management {
  padding: 20px;
}

.search-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>