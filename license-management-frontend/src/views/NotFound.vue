<template>
  <div class="not-found">
    <div class="not-found-content">
      <div class="not-found-image">
        <el-icon size="200" color="#ddd">
          <Warning />
        </el-icon>
      </div>
      <h1>404</h1>
      <p>抱歉，您访问的页面不存在</p>
      <el-button type="primary" @click="goHome">
        返回首页
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Warning } from '@element-plus/icons-vue'

const router = useRouter()

const goHome = () => {
  router.push('/')
}
</script>

<style scoped>
.not-found {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f5f5f5;
}

.not-found-content {
  text-align: center;
  padding: 40px;
}

.not-found-image {
  margin-bottom: 30px;
}

h1 {
  font-size: 72px;
  color: #333;
  margin: 20px 0;
  font-weight: bold;
}

p {
  font-size: 18px;
  color: #666;
  margin-bottom: 30px;
}
</style>