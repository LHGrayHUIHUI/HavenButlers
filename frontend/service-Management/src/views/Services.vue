<template>
  <div class="services-container container">
    <!-- 页面头部 -->
    <div class="page-header flex-between mb-xl">
      <div>
        <h1 class="page-title">Service Management</h1>
        <p class="page-subtitle">管理和监控所有微服务状态</p>
      </div>
      <div class="header-stats">
        <div class="stat-badge elevation-2">
          <span class="stat-icon">📦</span>
          <div>
            <span class="stat-label">服务总计</span>
            <span class="stat-value">{{ page.total }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 搜索工具栏 -->
    <section class="page-section">
    <div class="toolbar-card card elevation-1">
      <form class="search-form" @submit.prevent="onSearch">
        <div class="search-group">
          <div class="input-wrapper">
            <span class="input-icon">🔍</span>
            <input
              v-model="q.keyword"
              class="search-input"
              placeholder="搜索服务名称..."
              aria-label="搜索关键字"
            />
          </div>
          <select v-model="q.status" class="status-select" aria-label="状态筛选">
            <option value="">全部状态</option>
            <option value="UP">✅ UP</option>
            <option value="DEGRADED">⚠️ DEGRADED</option>
            <option value="DOWN">❌ DOWN</option>
          </select>
          <button type="submit" class="btn btn-primary">
            搜索
          </button>
        </div>
      </form>
    </div>
    </section>

    <!-- 服务列表表格 -->
    <section class="page-section">
    <div class="table-card card elevation-2">
      <table class="table-modern" role="table" aria-label="服务列表">
        <thead>
          <tr>
            <th>服务名称</th>
            <th>状态</th>
            <th>实例数量</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="it in list" :key="it.serviceName" class="table-row">
            <td>
              <RouterLink :to="`/services/${it.serviceName}`" class="service-link">
                {{ it.serviceName }}
              </RouterLink>
            </td>
            <td>
              <StatusTag :status="it.status" />
            </td>
            <td>
              <span class="instance-count">
                {{ it.totalInstances }}
                <span class="instance-label">个实例</span>
              </span>
            </td>
            <td>
              <RouterLink :to="`/services/${it.serviceName}`" class="action-link">
                <span>查看详情 →</span>
              </RouterLink>
            </td>
          </tr>
          <tr v-if="list.length === 0">
            <td colspan="4" class="empty-state">
              <div class="empty-content">
                <span class="empty-icon">📭</span>
                <p>暂无匹配的服务</p>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    </section>

    <!-- 分页控制 -->
    <div class="pagination-card elevation-1">
      <div class="pagination-info">
        显示 {{ (page.page - 1) * page.size + 1 }}-{{ Math.min(page.page * page.size, page.total) }} 条，
        共 {{ page.total }} 条
      </div>
      <div class="pagination-controls">
        <button
          class="page-btn"
          :disabled="!page.hasPrevious"
          @click="go(page.page - 1)"
        >
          ← 上一页
        </button>
        <div class="page-numbers">
          <span class="current-page">{{ page.page }}</span>
          <span class="page-separator">/</span>
          <span class="total-pages">{{ page.totalPage }}</span>
        </div>
        <button
          class="page-btn"
          :disabled="!page.hasNext"
          @click="go(page.page + 1)"
        >
          下一页 →
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { fetchServiceList } from '@/api/service';
import StatusTag from '@/components/StatusTag.vue';

// 列表查询参数
const q = reactive({ keyword: '', status: '', page: 1, size: 20 });
// 分页数据（使用 adaptPage 统一转换）
const page = reactive({ list: [] as any[], total: 0, totalPage: 0, page: 1, size: 20, hasPrevious: false, hasNext: false });
const list = ref<any[]>([]);

async function load() {
  const data = await fetchServiceList({ ...q });
  Object.assign(page, data);
  list.value = data.list;
}

function onSearch() {
  q.page = 1;
  load();
}

function go(p: number) {
  q.page = p;
  load();
}

onMounted(load);
</script>

<style scoped>
.services-container {
  padding-top: 24px;
  padding-bottom: 48px;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
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

.header-stats {
  display: flex;
  gap: 16px;
}

.stat-badge {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  background: linear-gradient(135deg, var(--color-primary), #1e88e5);
  color: white;
  border-radius: var(--radius-xl);
  transition: all var(--transition-base);
}

.stat-badge:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(17, 115, 212, 0.3);
}

.stat-icon {
  font-size: 24px;
}

.stat-label {
  display: block;
  font-size: var(--text-xs);
  opacity: 0.9;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 2px;
}

.stat-value {
  display: block;
  font-size: var(--text-xl);
  font-weight: var(--font-bold);
}

/* 搜索工具栏 */
.toolbar-card {
  margin-bottom: 24px;
}

.search-form {
  width: 100%;
}

.search-group {
  display: flex;
  gap: 12px;
  align-items: center;
}

.input-wrapper {
  position: relative;
  flex: 1;
  max-width: 400px;
}

.input-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 18px;
}

.search-input {
  width: 100%;
  padding: 10px 12px 10px 40px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-primary);
  font-size: var(--text-sm);
  transition: all var(--transition-fast);
}

.search-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(17, 115, 212, 0.1);
}

.status-select {
  padding: 10px 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-primary);
  font-size: var(--text-sm);
  min-width: 150px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.status-select:hover {
  border-color: var(--color-primary);
}

.status-select:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(17, 115, 212, 0.1);
}

/* 表格样式 */
.table-card {
  margin-bottom: 24px;
  overflow-x: auto;
}

.table-row {
  transition: background var(--transition-fast);
}

.service-link {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: var(--font-medium);
  transition: color var(--transition-fast);
}

.service-link:hover {
  color: #0e5fb3;
  text-decoration: underline;
}

.instance-count {
  display: flex;
  align-items: center;
  gap: 4px;
}

.instance-label {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}

.action-link {
  color: var(--color-primary);
  text-decoration: none;
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  transition: all var(--transition-fast);
}

.action-link:hover {
  color: #0e5fb3;
  transform: translateX(2px);
}

/* 空状态 */
.empty-state {
  padding: 48px 24px !important;
  text-align: center;
}

.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.empty-icon {
  font-size: 48px;
  opacity: 0.5;
}

.empty-content p {
  margin: 0;
  color: var(--color-text-tertiary);
  font-size: var(--text-sm);
}

/* 分页控制 */
.pagination-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.pagination-info {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-btn {
  padding: 8px 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-primary);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.page-btn:hover:not(:disabled) {
  background: var(--color-surface-hover);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-numbers {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: var(--color-surface-hover);
  border-radius: var(--radius-md);
}

.current-page {
  font-weight: var(--font-bold);
  color: var(--color-primary);
}

.page-separator {
  color: var(--color-text-tertiary);
}

.total-pages {
  color: var(--color-text-secondary);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .search-group {
    flex-direction: column;
    align-items: stretch;
  }

  .input-wrapper {
    max-width: 100%;
  }

  .pagination-card {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }
}
</style>

