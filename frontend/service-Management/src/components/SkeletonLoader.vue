<template>
  <div class="skeleton-loader">
    <!-- 统计卡片骨架 -->
    <div v-if="type === 'stats'" class="skeleton-stats">
      <div v-for="i in 4" :key="i" class="skeleton-stat-card card elevation-1">
        <div class="skeleton skeleton-icon"></div>
        <div class="skeleton-content">
          <div class="skeleton skeleton-text" style="width: 60%"></div>
          <div class="skeleton skeleton-title"></div>
        </div>
      </div>
    </div>

    <!-- 服务卡片骨架 -->
    <div v-else-if="type === 'services'" class="skeleton-services">
      <div v-for="i in count" :key="i" class="skeleton-service-card card elevation-1">
        <div class="skeleton skeleton-title" style="width: 70%"></div>
        <div class="skeleton skeleton-text"></div>
        <div class="skeleton skeleton-text" style="width: 50%"></div>
      </div>
    </div>

    <!-- 表格骨架 -->
    <div v-else-if="type === 'table'" class="skeleton-table card elevation-1">
      <div class="skeleton-table-header">
        <div v-for="i in 4" :key="i" class="skeleton skeleton-text"></div>
      </div>
      <div v-for="row in count" :key="row" class="skeleton-table-row">
        <div v-for="col in 4" :key="col" class="skeleton skeleton-text" :style="{ width: `${Math.random() * 40 + 40}%` }"></div>
      </div>
    </div>

    <!-- 默认骨架 -->
    <div v-else class="skeleton-default">
      <div v-for="i in count" :key="i" class="skeleton-item">
        <div class="skeleton skeleton-title"></div>
        <div class="skeleton skeleton-text"></div>
        <div class="skeleton skeleton-text" style="width: 80%"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  type?: 'stats' | 'services' | 'table' | 'default'
  count?: number
}

withDefaults(defineProps<Props>(), {
  type: 'default',
  count: 3
})
</script>

<style scoped>
.skeleton-loader {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* 统计卡片骨架 */
.skeleton-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-xl);
}

.skeleton-stat-card {
  display: flex;
  gap: 16px;
  align-items: center;
  padding: var(--spacing-lg);
}

.skeleton-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-lg);
  flex-shrink: 0;
}

.skeleton-content {
  flex: 1;
}

/* 服务卡片骨架 */
.skeleton-services {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--spacing-lg);
}

.skeleton-service-card {
  padding: var(--spacing-lg);
}

.skeleton-service-card .skeleton {
  margin-bottom: 12px;
}

/* 表格骨架 */
.skeleton-table {
  padding: var(--spacing-lg);
}

.skeleton-table-header {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr;
  gap: var(--spacing-md);
  padding-bottom: var(--spacing-md);
  border-bottom: 1px solid var(--color-border);
  margin-bottom: var(--spacing-md);
}

.skeleton-table-row {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr;
  gap: var(--spacing-md);
  padding: var(--spacing-md) 0;
  border-bottom: 1px solid var(--color-border);
}

/* 默认骨架 */
.skeleton-default {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.skeleton-item {
  padding: var(--spacing-lg);
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
}

.skeleton-item .skeleton {
  margin-bottom: 8px;
}

/* 响应式 */
@media (max-width: 768px) {
  .skeleton-stats {
    grid-template-columns: 1fr;
  }

  .skeleton-services {
    grid-template-columns: 1fr;
  }
}</style>