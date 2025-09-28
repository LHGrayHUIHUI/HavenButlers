<template>
  <div class="dashboard container">
    <!-- 页面标题区域 -->
    <div class="page-header flex-between mb-xl">
      <div class="header-content">
        <h1 class="page-title">Service Overview</h1>
        <p class="page-subtitle">
          实时服务状态监控 · 最后更新:
          <span class="update-time">{{ lastUpdateTime }}</span>
        </p>
      </div>
      <div class="header-actions">
        <div class="connection-status glass elevation-2">
          <span :class="['status-dot', sseConnected ? 'success' : 'warning', { 'animate-pulse': sseConnected }]"></span>
          <span class="status-text">{{ sseConnected ? 'SSE Connected' : '轮询模式' }}</span>
        </div>
      </div>
    </div>

    <!-- 统计卡片网格 -->
    <section class="page-section">
      <!-- 加载骨架 -->
      <SkeletonLoader v-if="isLoading" type="stats" />

      <!-- 实际内容 -->
      <transition name="fade" mode="out-in">
      <div v-if="!isLoading" class="grid grid-cols-4 gap-lg mb-xl">
        <div class="stat-card card elevation-2" v-for="(stat, index) in statsCards" :key="index" :class="stat.class">
          <div class="stat-icon">{{ stat.icon }}</div>
          <div class="stat-content">
            <p class="stat-label text-secondary">{{ stat.label }}</p>
            <p class="stat-value" :class="stat.valueClass">{{ stat.value }}</p>
            <div class="stat-trend" v-if="stat.trend">
              <span :class="['trend-icon', stat.trendUp ? 'trend-up' : 'trend-down']">{{ stat.trendUp ? '↑' : '↓' }}</span>
              <span class="trend-text">{{ stat.trend }}</span>
            </div>
          </div>
        </div>
      </div>
      </transition>
    </section>

    <!-- 服务状态网格 -->
    <section class="page-section">
      <div class="section-header">
        <h2 class="section-title">服务状态详情</h2>
        <p class="section-subtitle">点击卡片查看服务详细信息</p>
      </div>
      <transition-group name="list" tag="div" class="services-grid card-grid">
        <div
          v-for="service in services"
          :key="service.serviceName"
          :class="['service-card', 'card', 'elevation-2', getServiceCardClass(service.status)]"
          @click="navigateToService(service.serviceName)"
        >
          <div class="service-header">
            <h4 class="service-name">{{ service.serviceName }}</h4>
            <span :class="['badge', getBadgeClass(service.status)]">
              <span :class="['status-dot', getStatusClass(service.status)]"></span>
              {{ service.status }}
            </span>
          </div>
          <div class="service-details">
            <div class="detail-item">
              <strong>实例:</strong>
              <span :class="{ 'text-danger': service.healthyInstances < service.totalInstances }">
                {{ service.healthyInstances }}/{{ service.totalInstances }}
              </span>
            </div>
            <div class="detail-item">
              <strong>正常运行时间:</strong> {{ getUptime(service) }}
            </div>
            <div class="detail-item">
              <strong>响应延迟:</strong> {{ getLatency(service) }}
            </div>
          </div>
        </div>
      </transition-group>

      <!-- 空状态 -->
      <div v-if="services.length === 0" class="empty-state card elevation-1">
        <div class="empty-icon">📡</div>
        <h3>正在连接服务...</h3>
        <p class="text-secondary">等待获取服务状态数据</p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { connectSSE } from '@/utils/sse';
import { fetchOverview } from '@/api/service';
import SkeletonLoader from '@/components/SkeletonLoader.vue';

const router = useRouter();

// 实时健康数据：优先 SSE，失败回退为 5s 轮询
const services = ref<any[]>([]);
const stats = reactive({ total: 0, up: 0, degraded: 0, down: 0 });
const sseConnected = ref(true); // SSE连接状态
const lastUpdateTime = ref(new Date().toLocaleTimeString());
const isLoading = ref(true); // 加载状态
let handler: { stop: () => void } | null = null;

// 计算统计卡片数据
const statsCards = computed(() => [
  {
    icon: '📊',
    label: '服务总数',
    value: stats.total,
    class: '',
    valueClass: '',
    trend: null,
    trendUp: true
  },
  {
    icon: '✅',
    label: '正常运行',
    value: stats.up,
    class: 'card-success',
    valueClass: 'text-success',
    trend: stats.total > 0 ? `${Math.round((stats.up / stats.total) * 100)}%` : null,
    trendUp: true
  },
  {
    icon: '⚠️',
    label: '降级运行',
    value: stats.degraded,
    class: 'card-warning',
    valueClass: 'text-warning',
    trend: stats.total > 0 ? `${Math.round((stats.degraded / stats.total) * 100)}%` : null,
    trendUp: false
  },
  {
    icon: '❌',
    label: '服务异常',
    value: stats.down,
    class: 'card-danger',
    valueClass: 'text-danger',
    trend: stats.total > 0 ? `${Math.round((stats.down / stats.total) * 100)}%` : null,
    trendUp: false
  }
]);

// 计算属性和方法
const getServiceCardClass = (status: string) => {
  switch (status) {
    case 'DOWN': return 'card-danger';
    case 'DEGRADED': return 'card-warning';
    default: return '';
  }
};

const getBadgeClass = (status: string) => {
  switch (status) {
    case 'UP': return 'badge-success';
    case 'DOWN': return 'badge-danger';
    case 'DEGRADED': return 'badge-warning';
    default: return 'badge-info';
  }
};

const getStatusClass = (status: string) => {
  switch (status) {
    case 'UP': return 'success';
    case 'DOWN': return 'danger';
    case 'DEGRADED': return 'warning';
    default: return '';
  }
};

const getUptime = (service: any) => {
  // 模拟数据，实际应从后端获取
  const uptimes: { [key: string]: string } = {
    'UP': '99.9%',
    'DEGRADED': '95%',
    'DOWN': 'N/A'
  };
  return uptimes[service.status] || 'N/A';
};

const getLatency = (service: any) => {
  // 模拟数据，实际应从后端获取
  const latencies: { [key: string]: string } = {
    'UP': `${Math.floor(Math.random() * 20 + 5)}ms`,
    'DEGRADED': `${Math.floor(Math.random() * 50 + 30)}ms`,
    'DOWN': 'N/A'
  };
  return latencies[service.status] || 'N/A';
};

const navigateToService = (serviceName: string) => {
  router.push(`/services/${serviceName}`);
};

const updateData = (list: any[]) => {
  services.value = list || [];
  stats.total = list.length;
  stats.up = list.filter((i) => i.status === 'UP').length;
  stats.degraded = list.filter((i) => i.status === 'DEGRADED').length;
  stats.down = list.filter((i) => i.status === 'DOWN').length;
  lastUpdateTime.value = new Date().toLocaleTimeString();
  isLoading.value = false; // 数据加载完成
};

onMounted(() => {
  const url = (import.meta.env.VITE_SSE_PATH as string) || '/api/service/stream/health';
  handler = connectSSE(url, {
    onMessage: updateData,
    onError: () => {
      sseConnected.value = false;
    },
    poll: async () => await fetchOverview()
  });
});

onUnmounted(() => handler?.stop());
</script>

<style scoped>
.dashboard {
  padding-top: 24px;
  padding-bottom: 48px;
}

/* 页面标题区域 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-content {
  flex: 1;
}

.page-title {
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  margin: 0;
}

.update-time {
  color: var(--color-text-tertiary);
  font-weight: var(--font-medium);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: var(--color-surface);
  border-radius: var(--radius-full);
  border: 1px solid var(--color-border);
}

.status-text {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-text-secondary);
}

/* 统计卡片 */
.stat-card {
  position: relative;
  display: flex;
  gap: 16px;
  align-items: center;
  transition: all var(--transition-base);
  cursor: default;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-icon {
  font-size: 32px;
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-surface-hover);
  border-radius: var(--radius-lg);
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  margin: 0 0 4px 0;
}

.stat-value {
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  margin: 0;
  line-height: 1.2;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}

.trend-icon {
  font-size: 12px;
  font-weight: bold;
}

.trend-up {
  color: var(--color-success);
}

.trend-down {
  color: var(--color-danger);
}

.text-success {
  color: var(--color-success);
}

.text-warning {
  color: var(--color-warning);
}

.text-danger {
  color: var(--color-danger);
}

/* 服务状态部分 */
.services-section {
  margin-top: 32px;
}

.section-title {
  font-size: var(--text-xl);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  margin: 0 0 20px 0;
}

.services-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--spacing-lg);
}

.service-card {
  cursor: pointer;
  transition: all var(--transition-base);
  position: relative;
  overflow: hidden;
}

.service-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: var(--color-primary);
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.service-card:hover::before {
  opacity: 1;
}

.service-card:hover {
  transform: translateY(-4px);
}

.service-card.card-danger::before {
  background: var(--color-danger);
  opacity: 1;
}

.service-card.card-warning::before {
  background: var(--color-warning);
  opacity: 1;
}

.service-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.service-name {
  font-size: var(--text-base);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  margin: 0;
}

.service-details {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-item {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.detail-item strong {
  font-weight: var(--font-medium);
  color: var(--color-text-tertiary);
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 24px;
  margin: 40px auto;
  max-width: 400px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
  animation: pulse 2s infinite;
}

.empty-state h3 {
  font-size: var(--text-xl);
  color: var(--color-text-primary);
  margin: 0 0 8px 0;
}

.empty-state p {
  color: var(--color-text-secondary);
  margin: 0;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .grid-cols-4 {
    grid-template-columns: repeat(2, 1fr) !important;
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .grid-cols-4 {
    grid-template-columns: 1fr !important;
  }

  .stat-card {
    padding: 16px;
  }

  .services-grid {
    grid-template-columns: 1fr;
  }
}
</style>

