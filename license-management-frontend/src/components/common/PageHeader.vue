<template>
  <div class="page-header">
    <div class="page-header-content">
      <div class="page-header-main">
        <div class="page-header-title">
          <h1>{{ title }}</h1>
          <div v-if="description" class="page-header-description">
            {{ description }}
          </div>
        </div>
        <div v-if="$slots.extra" class="page-header-extra">
          <slot name="extra" />
        </div>
      </div>
      <div v-if="$slots.content" class="page-header-detail">
        <slot name="content" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  title: string
  description?: string
}

defineProps<Props>()
</script>

<style scoped>
.page-header {
  background: white;
  border-bottom: 1px solid #e8e8e8;
  margin-bottom: 20px;
}

.page-header-content {
  padding: 24px;
}

.page-header-main {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.page-header-title h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

.page-header-description {
  margin-top: 8px;
  color: #666;
  font-size: 14px;
}

.page-header-extra {
  flex-shrink: 0;
}

.page-header-detail {
  margin-top: 20px;
}

@media (max-width: 768px) {
  .page-header-main {
    flex-direction: column;
    gap: 16px;
  }
  
  .page-header-content {
    padding: 16px;
  }
  
  .page-header-title h1 {
    font-size: 20px;
  }
}
</style>