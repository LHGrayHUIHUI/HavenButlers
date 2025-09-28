<template>
  <div class="environment container">
    <!-- 页面标题 -->
    <div class="page-header mb-xl">
      <div>
        <h1 class="page-title">环境管理</h1>
        <p class="page-subtitle">管理和切换系统运行环境配置</p>
      </div>
      <div class="header-actions">
        <button class="btn btn-primary" @click="onRefresh">
          <span>🔄 刷新配置</span>
        </button>
      </div>
    </div>

    <!-- 当前环境卡片 -->
    <section class="page-section">
      <div class="current-env-card card elevation-3">
        <div class="env-icon">🌍</div>
        <div class="env-info">
          <div class="env-label">当前运行环境</div>
          <div class="env-name">{{ current?.name || 'Loading...' }}</div>
          <div class="env-details" v-if="current">
            <div class="detail-item">
              <span class="detail-label">配置版本:</span>
              <span class="detail-value">{{ current.version || 'v1.0.0' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">最后更新:</span>
              <span class="detail-value">{{ formatTime(current.updateTime) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">状态:</span>
              <span class="status-badge success">✅ 正常运行</span>
            </div>
          </div>
        </div>
        <div class="env-actions">
          <div class="refresh-hint">
            <span>💡 提示：刷新配置可能导致服务短暂抖动</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 可用环境列表 -->
    <section class="page-section">
      <div class="section-header mb-lg">
        <h2 class="section-title">可用环境</h2>
        <p class="section-subtitle">选择要切换的目标环境</p>
      </div>

      <div class="env-grid">
        <div
          v-for="env in envs"
          :key="env"
          :class="['env-card', 'card', 'elevation-1', { 'active': env === current?.name }]"
        >
          <div class="env-card-header">
            <div class="env-card-icon">{{ getEnvIcon(env) }}</div>
            <h3 class="env-card-name">{{ env }}</h3>
          </div>
          <div class="env-card-body">
            <p class="env-card-desc">{{ getEnvDescription(env) }}</p>
            <div class="env-features">
              <span class="feature-tag" v-for="feature in getEnvFeatures(env)" :key="feature">
                {{ feature }}
              </span>
            </div>
          </div>
          <div class="env-card-footer">
            <button
              v-if="env !== current?.name"
              class="btn btn-primary btn-block"
              @click="onSwitch(env)"
            >
              切换到此环境
            </button>
            <div v-else class="current-badge">
              <span>✓ 当前环境</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="envs.length === 0" class="empty-state card elevation-1">
        <div class="empty-icon">📭</div>
        <h3>暂无可用环境</h3>
        <p class="text-secondary">请检查配置服务是否正常运行</p>
      </div>
    </section>

    <!-- 环境切换说明 -->
    <section class="page-section">
      <div class="info-card card elevation-1">
        <h3 class="info-title">⚠️ 环境切换注意事项</h3>
        <ul class="info-list">
          <li>切换环境会重新加载系统配置，可能导致服务短暂中断</li>
          <li>建议在业务低峰期进行环境切换操作</li>
          <li>切换前请确保已保存所有未完成的工作</li>
          <li>生产环境切换需要额外的权限验证</li>
        </ul>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { fetchAvailableEnvs, fetchCurrentEnv, refreshConfig, switchEnvironment } from '@/api/service';

const current = ref<any>(null);
const envs = ref<string[]>([]);

// 格式化时间
const formatTime = (time?: string) => {
  if (!time) return new Date().toLocaleString();
  return new Date(time).toLocaleString();
};

// 获取环境图标
const getEnvIcon = (env: string) => {
  const icons: Record<string, string> = {
    'development': '💻',
    'test': '🧪',
    'staging': '🚀',
    'production': '🏭',
    'local': '🏠'
  };
  return icons[env.toLowerCase()] || '📦';
};

// 获取环境描述
const getEnvDescription = (env: string) => {
  const descriptions: Record<string, string> = {
    'development': '开发环境，用于日常开发调试',
    'test': '测试环境，用于功能测试和集成测试',
    'staging': '预发布环境，生产环境的镜像',
    'production': '生产环境，面向最终用户',
    'local': '本地环境，仅供本地开发使用'
  };
  return descriptions[env.toLowerCase()] || '标准运行环境';
};

// 获取环境特性标签
const getEnvFeatures = (env: string) => {
  const features: Record<string, string[]> = {
    'development': ['调试模式', '热重载', '详细日志'],
    'test': ['自动化测试', 'Mock数据', '性能监控'],
    'staging': ['灰度发布', '真实数据', '性能优化'],
    'production': ['高可用', '负载均衡', '数据备份'],
    'local': ['快速启动', '本地存储', '离线可用']
  };
  return features[env.toLowerCase()] || ['标准配置'];
};

async function load() {
  try {
    current.value = await fetchCurrentEnv();
    envs.value = await fetchAvailableEnvs();
  } catch (error) {
    ElMessage.error('加载环境信息失败');
  }
}

async function onRefresh() {
  try {
    await ElMessageBox.confirm(
      '刷新配置将重新加载所有服务配置，可能造成短暂的服务抖动。是否继续？',
      '确认刷新',
      {
        confirmButtonText: '确认刷新',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );

    const loading = ElMessage({
      message: '正在刷新配置...',
      type: 'info',
      duration: 0
    });

    await refreshConfig();
    loading.close();
    ElMessage.success('配置刷新成功');
    await load();
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('刷新配置失败');
    }
  }
}

async function onSwitch(env: string) {
  try {
    await ElMessageBox.confirm(
      `确认要切换到 ${env} 环境吗？这将重启相关服务。`,
      '环境切换确认',
      {
        confirmButtonText: '确认切换',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );

    const loading = ElMessage({
      message: `正在切换到 ${env} 环境...`,
      type: 'info',
      duration: 0
    });

    await switchEnvironment(env);
    loading.close();
    ElMessage.success(`已成功切换到 ${env} 环境`);
    await load();
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('环境切换失败');
    }
  }
}

onMounted(load);
</script>

<style scoped>
.environment {
  padding-top: 24px;
  padding-bottom: 48px;
}

/* 页面标题 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

/* 当前环境卡片 */
.current-env-card {
  background: linear-gradient(135deg, var(--color-primary), #1e88e5);
  color: white;
  padding: 32px;
  display: flex;
  align-items: center;
  gap: 32px;
}

.env-icon {
  font-size: 64px;
  width: 100px;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.2);
  border-radius: var(--radius-xl);
}

.env-info {
  flex: 1;
}

.env-label {
  font-size: var(--text-sm);
  opacity: 0.9;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  margin-bottom: 8px;
}

.env-name {
  font-size: var(--text-3xl);
  font-weight: var(--font-bold);
  margin-bottom: 16px;
}

.env-details {
  display: flex;
  gap: 32px;
  flex-wrap: wrap;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-label {
  font-size: var(--text-xs);
  opacity: 0.8;
}

.detail-value {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: var(--radius-full);
  font-size: var(--text-sm);
}

.refresh-hint {
  background: rgba(255, 255, 255, 0.1);
  padding: 12px 16px;
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
}

/* 环境网格 */
.env-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--spacing-lg);
}

.env-card {
  transition: all var(--transition-base);
  cursor: pointer;
}

.env-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

.env-card.active {
  border: 2px solid var(--color-primary);
  box-shadow: 0 0 0 4px rgba(17, 115, 212, 0.1);
}

.env-card-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: var(--spacing-lg);
  border-bottom: 1px solid var(--color-border);
}

.env-card-icon {
  font-size: 32px;
}

.env-card-name {
  font-size: var(--text-lg);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  margin: 0;
  text-transform: capitalize;
}

.env-card-body {
  padding: var(--spacing-lg);
}

.env-card-desc {
  color: var(--color-text-secondary);
  margin: 0 0 16px 0;
  font-size: var(--text-sm);
}

.env-features {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.feature-tag {
  display: inline-block;
  padding: 4px 12px;
  background: var(--color-surface-hover);
  border-radius: var(--radius-full);
  font-size: var(--text-xs);
  color: var(--color-text-secondary);
}

.env-card-footer {
  padding: var(--spacing-lg);
  border-top: 1px solid var(--color-border);
}

.btn-block {
  width: 100%;
}

.current-badge {
  text-align: center;
  color: var(--color-success);
  font-weight: var(--font-medium);
}

/* 信息卡片 */
.info-card {
  padding: var(--spacing-xl);
  background: var(--color-surface);
}

.info-title {
  font-size: var(--text-lg);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  margin: 0 0 16px 0;
}

.info-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.info-list li {
  position: relative;
  padding-left: 24px;
  margin-bottom: 12px;
  color: var(--color-text-secondary);
  font-size: var(--text-sm);
  line-height: 1.6;
}

.info-list li:before {
  content: '•';
  position: absolute;
  left: 8px;
  color: var(--color-primary);
  font-weight: bold;
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
}

.empty-state h3 {
  font-size: var(--text-xl);
  color: var(--color-text-primary);
  margin: 0 0 8px 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .current-env-card {
    flex-direction: column;
    text-align: center;
  }

  .env-details {
    justify-content: center;
  }

  .env-grid {
    grid-template-columns: 1fr;
  }
}
</style>