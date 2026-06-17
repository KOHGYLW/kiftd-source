<template>
  <div class="settings">
    <el-card class="settings-card">
      <template #header>
        <div class="card-header">
          <span>系统设置</span>
        </div>
      </template>
      
      <el-tabs v-model="activeTab" class="settings-tabs">
        <!-- 基础设置 -->
        <el-tab-pane label="基础设置" name="basic">
          <el-form
            ref="basicFormRef"
            :model="basicForm"
            :rules="basicRules"
            label-width="120px"
            class="settings-form"
          >
            <el-form-item label="站点名称" prop="siteName">
              <el-input v-model="basicForm.siteName" placeholder="请输入站点名称" />
            </el-form-item>
            
            <el-form-item label="站点描述" prop="siteDescription">
              <el-input
                v-model="basicForm.siteDescription"
                type="textarea"
                rows="3"
                placeholder="请输入站点描述"
              />
            </el-form-item>
            
            <el-form-item label="Logo URL" prop="logoUrl">
              <el-input v-model="basicForm.logoUrl" placeholder="请输入Logo地址" />
            </el-form-item>
            
            <el-form-item label="默认授权类型" prop="defaultLicenseType">
              <el-select v-model="basicForm.defaultLicenseType" placeholder="请选择默认授权类型">
                <el-option label="试用版" value="trial" />
                <el-option label="标准版" value="standard" />
                <el-option label="企业版" value="enterprise" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="默认授权期限" prop="defaultLicenseDuration">
              <el-input-number
                v-model="basicForm.defaultLicenseDuration"
                :min="1"
                :max="3650"
                placeholder="天数"
              />
              <span class="form-helper">天</span>
            </el-form-item>
            
            <el-form-item>
              <el-button type="primary" :loading="basicLoading" @click="saveBasicSettings">
                保存设置
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 邮件设置 -->
        <el-tab-pane label="邮件设置" name="email">
          <el-form
            ref="emailFormRef"
            :model="emailForm"
            :rules="emailRules"
            label-width="120px"
            class="settings-form"
          >
            <el-form-item label="SMTP服务器" prop="smtpHost">
              <el-input v-model="emailForm.smtpHost" placeholder="请输入SMTP服务器地址" />
            </el-form-item>
            
            <el-form-item label="SMTP端口" prop="smtpPort">
              <el-input-number
                v-model="emailForm.smtpPort"
                :min="1"
                :max="65535"
                placeholder="端口号"
              />
            </el-form-item>
            
            <el-form-item label="用户名" prop="smtpUser">
              <el-input v-model="emailForm.smtpUser" placeholder="请输入SMTP用户名" />
            </el-form-item>
            
            <el-form-item label="密码" prop="smtpPassword">
              <el-input
                v-model="emailForm.smtpPassword"
                type="password"
                placeholder="请输入SMTP密码"
                show-password
              />
            </el-form-item>
            
            <el-form-item label="发件人邮箱" prop="fromEmail">
              <el-input v-model="emailForm.fromEmail" placeholder="请输入发件人邮箱" />
            </el-form-item>
            
            <el-form-item>
              <el-button type="primary" :loading="emailLoading" @click="saveEmailSettings">
                保存设置
              </el-button>
              <el-button @click="testEmail">测试邮件</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 通知设置 -->
        <el-tab-pane label="通知设置" name="notification">
          <el-form
            ref="notificationFormRef"
            :model="notificationForm"
            label-width="120px"
            class="settings-form"
          >
            <el-form-item label="邮件通知">
              <el-switch v-model="notificationForm.enableEmailNotification" />
              <span class="form-helper">开启后将通过邮件发送重要通知</span>
            </el-form-item>
            
            <el-form-item label="短信通知">
              <el-switch v-model="notificationForm.enableSmsNotification" />
              <span class="form-helper">开启后将通过短信发送重要通知</span>
            </el-form-item>
            
            <el-form-item label="授权到期提醒" prop="licenseExpiryReminder">
              <el-input-number
                v-model="notificationForm.licenseExpiryReminder"
                :min="1"
                :max="365"
                placeholder="天数"
              />
              <span class="form-helper">在授权到期前几天发送提醒</span>
            </el-form-item>
            
            <el-form-item>
              <el-button type="primary" :loading="notificationLoading" @click="saveNotificationSettings">
                保存设置
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

const activeTab = ref('basic')
const basicLoading = ref(false)
const emailLoading = ref(false)
const notificationLoading = ref(false)

const basicFormRef = ref<FormInstance>()
const emailFormRef = ref<FormInstance>()
const notificationFormRef = ref<FormInstance>()

// 基础设置表单
const basicForm = reactive({
  siteName: '企业授权管理系统',
  siteDescription: '专业的企业软件授权管理平台',
  logoUrl: '',
  defaultLicenseType: 'standard',
  defaultLicenseDuration: 365
})

const basicRules: FormRules = {
  siteName: [{ required: true, message: '请输入站点名称', trigger: 'blur' }],
  defaultLicenseType: [{ required: true, message: '请选择默认授权类型', trigger: 'change' }],
  defaultLicenseDuration: [{ required: true, message: '请输入默认授权期限', trigger: 'blur' }]
}

// 邮件设置表单
const emailForm = reactive({
  smtpHost: '',
  smtpPort: 587,
  smtpUser: '',
  smtpPassword: '',
  fromEmail: ''
})

const emailRules: FormRules = {
  smtpHost: [{ required: true, message: '请输入SMTP服务器地址', trigger: 'blur' }],
  smtpPort: [{ required: true, message: '请输入SMTP端口', trigger: 'blur' }],
  smtpUser: [{ required: true, message: '请输入SMTP用户名', trigger: 'blur' }],
  smtpPassword: [{ required: true, message: '请输入SMTP密码', trigger: 'blur' }],
  fromEmail: [
    { required: true, message: '请输入发件人邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

// 通知设置表单
const notificationForm = reactive({
  enableEmailNotification: true,
  enableSmsNotification: false,
  licenseExpiryReminder: 30
})

// 保存基础设置
const saveBasicSettings = async () => {
  if (!basicFormRef.value) return
  
  const valid = await basicFormRef.value.validate()
  if (!valid) return
  
  basicLoading.value = true
  
  try {
    // 这里调用API保存设置
    await new Promise(resolve => setTimeout(resolve, 1000)) // 模拟API调用
    ElMessage.success('基础设置保存成功')
  } catch (error) {
    console.error('保存基础设置失败:', error)
    ElMessage.error('保存失败')
  } finally {
    basicLoading.value = false
  }
}

// 保存邮件设置
const saveEmailSettings = async () => {
  if (!emailFormRef.value) return
  
  const valid = await emailFormRef.value.validate()
  if (!valid) return
  
  emailLoading.value = true
  
  try {
    // 这里调用API保存设置
    await new Promise(resolve => setTimeout(resolve, 1000)) // 模拟API调用
    ElMessage.success('邮件设置保存成功')
  } catch (error) {
    console.error('保存邮件设置失败:', error)
    ElMessage.error('保存失败')
  } finally {
    emailLoading.value = false
  }
}

// 保存通知设置
const saveNotificationSettings = async () => {
  notificationLoading.value = true
  
  try {
    // 这里调用API保存设置
    await new Promise(resolve => setTimeout(resolve, 1000)) // 模拟API调用
    ElMessage.success('通知设置保存成功')
  } catch (error) {
    console.error('保存通知设置失败:', error)
    ElMessage.error('保存失败')
  } finally {
    notificationLoading.value = false
  }
}

// 测试邮件
const testEmail = async () => {
  if (!emailFormRef.value) return
  
  const valid = await emailFormRef.value.validate()
  if (!valid) return
  
  try {
    // 这里调用API发送测试邮件
    await new Promise(resolve => setTimeout(resolve, 2000)) // 模拟API调用
    ElMessage.success('测试邮件发送成功，请检查收件箱')
  } catch (error) {
    console.error('发送测试邮件失败:', error)
    ElMessage.error('测试邮件发送失败')
  }
}

// 加载设置数据
const loadSettings = async () => {
  try {
    // 这里调用API加载设置
    // const settings = await getSystemSettings()
    // Object.assign(basicForm, settings.basic)
    // Object.assign(emailForm, settings.email)
    // Object.assign(notificationForm, settings.notification)
  } catch (error) {
    console.error('加载设置失败:', error)
    ElMessage.error('加载设置失败')
  }
}

onMounted(() => {
  loadSettings()
})
</script>

<style scoped>
.settings {
  padding: 20px;
}

.settings-card {
  max-width: 800px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.settings-tabs {
  margin-top: 20px;
}

.settings-form {
  max-width: 600px;
  margin: 20px 0;
}

.form-helper {
  margin-left: 10px;
  color: #999;
  font-size: 12px;
}
</style>