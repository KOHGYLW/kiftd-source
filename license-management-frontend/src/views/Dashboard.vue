<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-item">
            <div class="stats-icon customer">
              <el-icon><User /></el-icon>
            </div>
            <div class="stats-content">
              <div class="stats-value">{{ stats.totalCustomers }}</div>
              <div class="stats-label">客户总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-item">
            <div class="stats-icon license">
              <el-icon><Key /></el-icon>
            </div>
            <div class="stats-content">
              <div class="stats-value">{{ stats.totalLicenses }}</div>
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
              <div class="stats-value">{{ stats.activeLicenses }}</div>
              <div class="stats-label">活跃授权</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-item">
            <div class="stats-icon revenue">
              <el-icon><Money /></el-icon>
            </div>
            <div class="stats-content">
              <div class="stats-value">¥{{ stats.revenueThisMonth.toLocaleString() }}</div>
              <div class="stats-label">本月收入</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :span="12">
        <el-card title="客户增长趋势">
          <div ref="customerTrendChart" style="height: 300px"></div>
        </el-card>
      </el-col>
      
      <el-col :span="12">
        <el-card title="授权类型分布">
          <div ref="licenseTypeChart" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 即将过期的授权 -->
    <el-card title="即将过期的授权" class="expiring-licenses">
      <el-table :data="expiringLicenses" style="width: 100%">
        <el-table-column prop="customerName" label="客户名称" />
        <el-table-column prop="productName" label="产品名称" />
        <el-table-column prop="endDate" label="到期日期" />
        <el-table-column prop="daysLeft" label="剩余天数">
          <template #default="{ row }">
            <el-tag :type="row.daysLeft <= 7 ? 'danger' : 'warning'">
              {{ row.daysLeft }}天
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleRenew(row)">
              续期
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Key, Check, Money } from '@element-plus/icons-vue'
import { getDashboardStats, getCustomerTrend, getLicenseTypeDistribution, getExpiringLicenses } from '@/api/dashboard'
import type { DashboardStats } from '@/types'

const stats = ref<DashboardStats>({
  totalCustomers: 0,
  totalLicenses: 0,
  activeLicenses: 0,
  expiredLicenses: 0,
  revenueThisMonth: 0,
  revenueGrowth: 0,
  customerGrowth: 0,
  licenseGrowth: 0
})

const expiringLicenses = ref<any[]>([])
const customerTrendChart = ref<HTMLElement>()
const licenseTypeChart = ref<HTMLElement>()

const loadDashboardData = async () => {
  try {
    const [statsData, expiringData] = await Promise.all([
      getDashboardStats(),
      getExpiringLicenses()
    ])
    
    stats.value = statsData
    expiringLicenses.value = expiringData
  } catch (error) {
    console.error('加载仪表板数据失败:', error)
    ElMessage.error('加载数据失败')
  }
}

const handleRenew = (license: any) => {
  ElMessage.info(`续期功能开发中 - ${license.customerName}`)
}

onMounted(() => {
  loadDashboardData()
  
  // 这里可以集成图表库如 ECharts
  nextTick(() => {
    // 初始化图表
    // initCharts()
  })
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
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

.stats-icon.customer {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stats-icon.license {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stats-icon.active {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stats-icon.revenue {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
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

.charts-row {
  margin-bottom: 20px;
}

.expiring-licenses {
  margin-top: 20px;
}
</style>