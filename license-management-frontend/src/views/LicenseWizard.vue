<template>
  <div class="license-wizard">
    <!-- 页面头部 -->
    <div class="page-header">
      <el-button @click="$router.go(-1)" type="text" size="large">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <div class="header-title">
        <h2>授权生成向导</h2>
        <p>按照步骤引导创建新的授权许可</p>
      </div>
    </div>

    <el-card class="wizard-card">
      <!-- 步骤指示器 -->
      <el-steps :active="currentStep" align-center class="wizard-steps">
        <el-step title="选择客户" description="选择或创建客户信息" />
        <el-step title="产品信息" description="配置产品名称和版本" />
        <el-step title="授权配置" description="设置授权类型和权限" />
        <el-step title="预览确认" description="确认信息并生成授权" />
      </el-steps>

      <!-- 步骤内容 -->
      <div class="wizard-content">
        <!-- 步骤 1: 选择客户 -->
        <div v-if="currentStep === 0" class="step-content">
          <h3 class="step-title">选择客户</h3>
          <el-form :model="form" :rules="step1Rules" ref="step1FormRef" label-width="120px">
            <el-form-item label="选择方式" prop="customerSelectType">
              <el-radio-group v-model="form.customerSelectType" @change="handleCustomerSelectTypeChange">
                <el-radio label="existing">选择已有客户</el-radio>
                <el-radio label="new">创建新客户</el-radio>
              </el-radio-group>
            </el-form-item>
            
            <!-- 选择已有客户 -->
            <template v-if="form.customerSelectType === 'existing'">
              <el-form-item label="客户" prop="customerId">
                <el-select
                  v-model="form.customerId"
                  placeholder="请选择客户"
                  style="width: 100%"
                  filterable
                  remote
                  :remote-method="searchCustomers"
                  :loading="customerLoading"
                  @change="handleCustomerChange"
                >
                  <el-option
                    v-for="customer in customerOptions"
                    :key="customer.value"
                    :label="customer.label"
                    :value="customer.value"
                  />
                </el-select>
              </el-form-item>
            </template>
            
            <!-- 创建新客户 -->
            <template v-else>
              <el-form-item label="客户姓名" prop="customerName">
                <el-input v-model="form.customerName" placeholder="请输入客户姓名" />
              </el-form-item>
              <el-form-item label="公司名称" prop="customerCompany">
                <el-input v-model="form.customerCompany" placeholder="请输入公司名称" />
              </el-form-item>
              <el-form-item label="邮箱地址" prop="customerEmail">
                <el-input v-model="form.customerEmail" placeholder="请输入邮箱地址" />
              </el-form-item>
              <el-form-item label="联系电话" prop="customerPhone">
                <el-input v-model="form.customerPhone" placeholder="请输入联系电话" />
              </el-form-item>
            </template>
            
            <!-- 显示选中的客户信息 -->
            <el-form-item v-if="selectedCustomer" label="客户信息">
              <el-descriptions :column="2" border>
                <el-descriptions-item label="姓名">{{ selectedCustomer.name }}</el-descriptions-item>
                <el-descriptions-item label="公司">{{ selectedCustomer.company }}</el-descriptions-item>
                <el-descriptions-item label="邮箱">{{ selectedCustomer.email }}</el-descriptions-item>
                <el-descriptions-item label="电话">{{ selectedCustomer.phone }}</el-descriptions-item>
              </el-descriptions>
            </el-form-item>
          </el-form>
        </div>

        <!-- 步骤 2: 产品信息 -->
        <div v-if="currentStep === 1" class="step-content">
          <h3 class="step-title">产品信息</h3>
          <el-form :model="form" :rules="step2Rules" ref="step2FormRef" label-width="120px">
            <el-form-item label="产品名称" prop="productName">
              <el-input v-model="form.productName" placeholder="请输入产品名称" />
            </el-form-item>
            <el-form-item label="产品版本" prop="productVersion">
              <el-input v-model="form.productVersion" placeholder="请输入产品版本，如 v1.0.0" />
            </el-form-item>
            <el-form-item label="产品描述">
              <el-input
                v-model="form.productDescription"
                type="textarea"
                rows="3"
                placeholder="请输入产品描述（可选）"
              />
            </el-form-item>
          </el-form>
        </div>

        <!-- 步骤 3: 授权配置 -->
        <div v-if="currentStep === 2" class="step-content">
          <h3 class="step-title">授权配置</h3>
          <el-form :model="form" :rules="step3Rules" ref="step3FormRef" label-width="120px">
            <el-form-item label="授权类型" prop="licenseType">
              <el-radio-group v-model="form.licenseType" @change="handleLicenseTypeChange">
                <el-radio label="trial">
                  <div class="license-type-option">
                    <div class="type-title">试用版</div>
                    <div class="type-desc">功能限制，适用于产品试用</div>
                  </div>
                </el-radio>
                <el-radio label="standard">
                  <div class="license-type-option">
                    <div class="type-title">标准版</div>
                    <div class="type-desc">标准功能，适用于一般用户</div>
                  </div>
                </el-radio>
                <el-radio label="enterprise">
                  <div class="license-type-option">
                    <div class="type-title">企业版</div>
                    <div class="type-desc">完整功能，适用于企业客户</div>
                  </div>
                </el-radio>
              </el-radio-group>
            </el-form-item>
            
            <el-form-item label="最大用户数" prop="maxUsers">
              <el-input-number
                v-model="form.maxUsers"
                :min="1"
                :max="10000"
                placeholder="请输入最大用户数"
                style="width: 200px"
              />
              <span class="form-tip">设置此授权最多支持的并发用户数</span>
            </el-form-item>
            
            <el-form-item label="授权期限" prop="startDate">
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                style="width: 300px"
                @change="handleDateChange"
              />
              <span class="form-tip">授权的有效时间范围</span>
            </el-form-item>
            
            <el-form-item label="功能特性" prop="features">
              <el-checkbox-group v-model="form.features">
                <el-row :gutter="20">
                  <el-col :span="8" v-for="feature in availableFeatures" :key="feature.value">
                    <el-checkbox :label="feature.value">
                      <div class="feature-option">
                        <div class="feature-name">{{ feature.label }}</div>
                        <div class="feature-desc">{{ feature.description }}</div>
                      </div>
                    </el-checkbox>
                  </el-col>
                </el-row>
              </el-checkbox-group>
            </el-form-item>
            
            <el-form-item label="备注信息">
              <el-input
                v-model="form.remarks"
                type="textarea"
                rows="3"
                placeholder="请输入备注信息（可选）"
              />
            </el-form-item>
          </el-form>
        </div>

        <!-- 步骤 4: 预览确认 -->
        <div v-if="currentStep === 3" class="step-content">
          <h3 class="step-title">预览确认</h3>
          <div class="preview-content">
            <!-- 客户信息 -->
            <el-card title="客户信息" class="preview-card">
              <el-descriptions :column="2">
                <el-descriptions-item label="客户姓名">
                  {{ form.customerSelectType === 'existing' ? selectedCustomer?.name : form.customerName }}
                </el-descriptions-item>
                <el-descriptions-item label="公司名称">
                  {{ form.customerSelectType === 'existing' ? selectedCustomer?.company : form.customerCompany }}
                </el-descriptions-item>
                <el-descriptions-item label="邮箱地址">
                  {{ form.customerSelectType === 'existing' ? selectedCustomer?.email : form.customerEmail }}
                </el-descriptions-item>
                <el-descriptions-item label="联系电话">
                  {{ form.customerSelectType === 'existing' ? selectedCustomer?.phone : form.customerPhone }}
                </el-descriptions-item>
              </el-descriptions>
            </el-card>

            <!-- 产品信息 -->
            <el-card title="产品信息" class="preview-card">
              <el-descriptions :column="2">
                <el-descriptions-item label="产品名称">{{ form.productName }}</el-descriptions-item>
                <el-descriptions-item label="产品版本">{{ form.productVersion }}</el-descriptions-item>
                <el-descriptions-item label="产品描述" :span="2">
                  {{ form.productDescription || '无' }}
                </el-descriptions-item>
              </el-descriptions>
            </el-card>

            <!-- 授权配置 -->
            <el-card title="授权配置" class="preview-card">
              <el-descriptions :column="2">
                <el-descriptions-item label="授权类型">
                  <el-tag :type="getLicenseTypeColor(form.licenseType)">
                    {{ getLicenseTypeText(form.licenseType) }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="最大用户数">{{ form.maxUsers }}</el-descriptions-item>
                <el-descriptions-item label="授权期限">
                  {{ formatDate(form.startDate, 'YYYY-MM-DD') }} 至 {{ formatDate(form.endDate, 'YYYY-MM-DD') }}
                </el-descriptions-item>
                <el-descriptions-item label="授权天数">
                  {{ getDaysBetween(form.startDate, form.endDate) }} 天
                </el-descriptions-item>
                <el-descriptions-item label="功能特性" :span="2">
                  <el-tag v-for="feature in form.features" :key="feature" style="margin-right: 8px;">
                    {{ getFeatureName(feature) }}
                  </el-tag>
                  <span v-if="form.features.length === 0" class="text-gray">无</span>
                </el-descriptions-item>
                <el-descriptions-item label="备注信息" :span="2">
                  {{ form.remarks || '无' }}
                </el-descriptions-item>
              </el-descriptions>
            </el-card>

            <!-- 生成预览 -->
            <el-card title="授权密钥预览" class="preview-card">
              <el-alert
                title="注意"
                description="确认信息无误后，点击"生成授权"将创建授权密钥，生成后无法修改。"
                type="warning"
                show-icon
                :closable="false"
              />
              <div v-if="generatedLicense" class="generated-license">
                <el-descriptions :column="1" border>
                  <el-descriptions-item label="授权密钥">
                    <div class="license-key-display">
                      <code>{{ generatedLicense.licenseKey }}</code>
                      <el-button type="text" @click="copyToClipboard(generatedLicense.licenseKey)">
                        <el-icon><CopyDocument /></el-icon>
                        复制
                      </el-button>
                    </div>
                  </el-descriptions-item>
                  <el-descriptions-item label="创建时间">
                    {{ formatDate(generatedLicense.createdAt) }}
                  </el-descriptions-item>
                </el-descriptions>
              </div>
            </el-card>
          </div>
        </div>
      </div>

      <!-- 步骤操作按钮 -->
      <div class="wizard-actions">
        <el-button v-if="currentStep > 0" @click="handlePrevStep">上一步</el-button>
        <el-button
          v-if="currentStep < 3"
          type="primary"
          @click="handleNextStep"
          :loading="validating"
        >
          下一步
        </el-button>
        <el-button
          v-if="currentStep === 3 && !generatedLicense"
          type="success"
          @click="handleGenerateLicense"
          :loading="generating"
        >
          生成授权
        </el-button>
        <el-button
          v-if="generatedLicense"
          type="primary"
          @click="handleFinish"
        >
          完成
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  ArrowLeft,
  CopyDocument
} from '@element-plus/icons-vue'
import { getCustomerOptions, getCustomer, createCustomer } from '@/api/customer'
import { createLicense } from '@/api/license'
import { formatDate, getDaysBetween, copyToClipboard, debounce } from '@/utils'
import type { Customer, CustomerForm, License, LicenseForm } from '@/types'

const route = useRoute()
const router = useRouter()

const currentStep = ref(0)
const validating = ref(false)
const generating = ref(false)
const customerLoading = ref(false)
const generatedLicense = ref<License | null>(null)

const step1FormRef = ref<FormInstance>()
const step2FormRef = ref<FormInstance>()
const step3FormRef = ref<FormInstance>()

const customerOptions = ref<Array<{ label: string; value: number }>>([])
const selectedCustomer = ref<Customer | null>(null)
const dateRange = ref<[Date, Date] | null>(null)

// 表单数据
const form = reactive({
  // 客户信息
  customerSelectType: 'existing' as 'existing' | 'new',
  customerId: 0,
  customerName: '',
  customerCompany: '',
  customerEmail: '',
  customerPhone: '',
  
  // 产品信息
  productName: '',
  productVersion: '',
  productDescription: '',
  
  // 授权配置
  licenseType: 'standard' as 'trial' | 'standard' | 'enterprise',
  maxUsers: 1,
  startDate: '',
  endDate: '',
  features: [] as string[],
  remarks: ''
})

// 可用功能特性
const availableFeatures = [
  { value: '基础功能', label: '基础功能', description: '核心业务功能' },
  { value: '高级分析', label: '高级分析', description: '数据分析和报表' },
  { value: 'API接口', label: 'API接口', description: '开放API接口调用' },
  { value: '数据导出', label: '数据导出', description: '数据导出功能' },
  { value: '多租户', label: '多租户', description: '多租户架构支持' },
  { value: '单点登录', label: '单点登录', description: 'SSO单点登录' },
  { value: '自定义字段', label: '自定义字段', description: '自定义业务字段' },
  { value: '工作流', label: '工作流', description: '业务流程自动化' }
]

// 表单验证规则
const step1Rules: FormRules = {
  customerId: [
    { required: true, message: '请选择客户', trigger: 'change' }
  ],
  customerName: [
    { required: true, message: '请输入客户姓名', trigger: 'blur' },
    { min: 2, max: 50, message: '姓名长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  customerCompany: [
    { required: true, message: '请输入公司名称', trigger: 'blur' },
    { min: 2, max: 100, message: '公司名称长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  customerEmail: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  customerPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$|^(\d{3,4}-?)?\d{7,8}$/, message: '请输入正确的手机号或座机号', trigger: 'blur' }
  ]
}

const step2Rules: FormRules = {
  productName: [
    { required: true, message: '请输入产品名称', trigger: 'blur' },
    { min: 2, max: 50, message: '产品名称长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  productVersion: [
    { required: true, message: '请输入产品版本', trigger: 'blur' },
    { min: 1, max: 20, message: '产品版本长度在 1 到 20 个字符', trigger: 'blur' }
  ]
}

const step3Rules: FormRules = {
  licenseType: [{ required: true, message: '请选择授权类型', trigger: 'change' }],
  maxUsers: [{ required: true, message: '请输入最大用户数', trigger: 'blur' }],
  startDate: [{ required: true, message: '请选择授权期限', trigger: 'change' }]
}

// 初始化
onMounted(() => {
  loadCustomerOptions()
  
  // 检查是否有预设的客户ID
  const customerId = route.query.customerId
  if (customerId) {
    form.customerId = Number(customerId)
    loadCustomerInfo(Number(customerId))
  }
  
  // 设置默认日期范围（30天）
  const today = new Date()
  const endDate = new Date(today.getTime() + 30 * 24 * 60 * 60 * 1000)
  dateRange.value = [today, endDate]
  handleDateChange([today, endDate])
})

// 加载客户选项
const loadCustomerOptions = async () => {
  try {
    customerOptions.value = await getCustomerOptions()
  } catch (error) {
    console.error('加载客户选项失败:', error)
  }
}

// 搜索客户
const searchCustomers = debounce(async (keyword: string) => {
  if (!keyword) {
    loadCustomerOptions()
    return
  }
  
  customerLoading.value = true
  try {
    // 这里应该调用搜索API
    const allOptions = await getCustomerOptions()
    customerOptions.value = allOptions.filter(option => 
      option.label.toLowerCase().includes(keyword.toLowerCase())
    )
  } catch (error) {
    console.error('搜索客户失败:', error)
  } finally {
    customerLoading.value = false
  }
}, 300)

// 加载客户信息
const loadCustomerInfo = async (customerId: number) => {
  try {
    selectedCustomer.value = await getCustomer(customerId)
  } catch (error) {
    console.error('加载客户信息失败:', error)
  }
}

// 事件处理
const handleCustomerSelectTypeChange = (type: string) => {
  if (type === 'existing') {
    form.customerName = ''
    form.customerCompany = ''
    form.customerEmail = ''
    form.customerPhone = ''
  } else {
    form.customerId = 0
    selectedCustomer.value = null
  }
}

const handleCustomerChange = (customerId: number) => {
  if (customerId) {
    loadCustomerInfo(customerId)
  } else {
    selectedCustomer.value = null
  }
}

const handleLicenseTypeChange = (type: string) => {
  // 根据授权类型设置默认功能
  switch (type) {
    case 'trial':
      form.features = ['基础功能']
      form.maxUsers = 5
      break
    case 'standard':
      form.features = ['基础功能', '数据导出']
      form.maxUsers = 50
      break
    case 'enterprise':
      form.features = ['基础功能', '高级分析', 'API接口', '数据导出', '多租户', '单点登录']
      form.maxUsers = 500
      break
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

// 步骤导航
const handlePrevStep = () => {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

const handleNextStep = async () => {
  validating.value = true
  
  try {
    let valid = false
    
    switch (currentStep.value) {
      case 0:
        if (form.customerSelectType === 'existing') {
          valid = form.customerId > 0
          if (!valid) {
            ElMessage.error('请选择客户')
          }
        } else {
          valid = await step1FormRef.value?.validate() || false
        }
        break
      case 1:
        valid = await step2FormRef.value?.validate() || false
        break
      case 2:
        valid = await step3FormRef.value?.validate() || false
        break
    }
    
    if (valid && currentStep.value < 3) {
      currentStep.value++
    }
  } catch (error) {
    console.error('表单验证失败:', error)
  } finally {
    validating.value = false
  }
}

const handleGenerateLicense = async () => {
  generating.value = true
  
  try {
    let finalCustomerId = form.customerId
    
    // 如果是新客户，先创建客户
    if (form.customerSelectType === 'new') {
      const customerData: CustomerForm = {
        name: form.customerName,
        company: form.customerCompany,
        email: form.customerEmail,
        phone: form.customerPhone,
        address: '',
        status: 'active'
      }
      
      const newCustomer = await createCustomer(customerData)
      finalCustomerId = newCustomer.id
    }
    
    // 创建授权
    const licenseData: LicenseForm = {
      customerId: finalCustomerId,
      productName: form.productName,
      productVersion: form.productVersion,
      licenseType: form.licenseType,
      maxUsers: form.maxUsers,
      startDate: form.startDate,
      endDate: form.endDate,
      features: form.features
    }
    
    generatedLicense.value = await createLicense(licenseData)
    ElMessage.success('授权生成成功！')
  } catch (error) {
    console.error('生成授权失败:', error)
    ElMessage.error('生成授权失败')
  } finally {
    generating.value = false
  }
}

const handleFinish = () => {
  router.push('/licenses')
}

// 工具函数
const getLicenseTypeText = (type: string) => {
  const map = { trial: '试用版', standard: '标准版', enterprise: '企业版' }
  return map[type] || type
}

const getLicenseTypeColor = (type: string) => {
  const map = { trial: 'info', standard: 'success', enterprise: 'warning' }
  return map[type] || 'info'
}

const getFeatureName = (value: string) => {
  const feature = availableFeatures.find(f => f.value === value)
  return feature?.label || value
}
</script>

<style scoped>
.license-wizard {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.header-title {
  margin-left: 16px;
}

.header-title h2 {
  margin: 0 0 4px 0;
  color: #333;
}

.header-title p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.wizard-card {
  min-height: 600px;
}

.wizard-steps {
  margin-bottom: 40px;
}

.wizard-content {
  min-height: 400px;
  padding: 0 20px;
}

.step-content {
  max-width: 800px;
  margin: 0 auto;
}

.step-title {
  margin-bottom: 24px;
  color: #333;
  font-size: 18px;
  font-weight: 600;
}

.license-type-option {
  margin-left: 8px;
}

.type-title {
  font-weight: 600;
  color: #333;
}

.type-desc {
  font-size: 12px;
  color: #666;
  margin-top: 2px;
}

.feature-option {
  margin-left: 8px;
}

.feature-name {
  font-weight: 500;
  color: #333;
}

.feature-desc {
  font-size: 12px;
  color: #666;
  margin-top: 2px;
}

.form-tip {
  margin-left: 12px;
  color: #666;
  font-size: 12px;
}

.preview-content {
  max-width: 800px;
  margin: 0 auto;
}

.preview-card {
  margin-bottom: 20px;
}

.generated-license {
  margin-top: 16px;
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

.wizard-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 40px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.text-gray {
  color: #666;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .license-wizard {
    padding: 12px;
  }
  
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .header-title {
    margin-left: 0;
    margin-top: 12px;
  }
  
  .wizard-content {
    padding: 0 8px;
  }
  
  .wizard-steps {
    margin-bottom: 24px;
  }
  
  .wizard-actions {
    flex-direction: column-reverse;
  }
}
</style>