<template>
  <div class="settings container">
    <!-- 页面标题 -->
    <div class="page-header mb-xl">
      <div>
        <h1 class="page-title">系统设置</h1>
        <p class="page-subtitle">个性化配置和系统偏好设置</p>
      </div>
      <div class="header-actions">
        <button class="btn btn-primary" @click="save">
          <span>💾 保存设置</span>
        </button>
      </div>
    </div>

    <!-- 设置分组 -->
    <div class="settings-grid">
      <!-- 常规设置 -->
      <section class="settings-section">
        <div class="settings-card card elevation-1">
          <div class="card-header">
            <h3 class="card-title">⚙️ 常规设置</h3>
            <p class="card-subtitle">基础系统配置</p>
          </div>
          <div class="card-body">
            <div class="setting-item">
              <div class="setting-info">
                <label class="setting-label">自动刷新间隔</label>
                <p class="setting-desc">设置数据自动刷新的时间间隔</p>
              </div>
              <div class="setting-control">
                <input
                  type="number"
                  v-model.number="settings.interval"
                  class="form-input"
                  min="1000"
                  step="500"
                />
                <span class="unit">毫秒</span>
              </div>
            </div>

            <div class="setting-item">
              <div class="setting-info">
                <label class="setting-label">分页大小</label>
                <p class="setting-desc">每页显示的数据条数</p>
              </div>
              <div class="setting-control">
                <select v-model="settings.pageSize" class="form-select">
                  <option :value="10">10 条</option>
                  <option :value="20">20 条</option>
                  <option :value="50">50 条</option>
                  <option :value="100">100 条</option>
                </select>
              </div>
            </div>

            <div class="setting-item">
              <div class="setting-info">
                <label class="setting-label">语言设置</label>
                <p class="setting-desc">选择系统显示语言</p>
              </div>
              <div class="setting-control">
                <select v-model="settings.language" class="form-select">
                  <option value="zh-CN">🇨🇳 简体中文</option>
                  <option value="en-US">🇺🇸 English</option>
                  <option value="ja-JP">🇯🇵 日本語</option>
                </select>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 外观设置 -->
      <section class="settings-section">
        <div class="settings-card card elevation-1">
          <div class="card-header">
            <h3 class="card-title">🎨 外观设置</h3>
            <p class="card-subtitle">个性化界面显示</p>
          </div>
          <div class="card-body">
            <div class="setting-item">
              <div class="setting-info">
                <label class="setting-label">主题模式</label>
                <p class="setting-desc">选择界面主题风格</p>
              </div>
              <div class="setting-control">
                <div class="theme-selector">
                  <button
                    :class="['theme-option', { active: settings.theme === 'light' }]"
                    @click="switchTheme('light')"
                  >
                    <span class="theme-icon">☀️</span>
                    <span>浅色</span>
                  </button>
                  <button
                    :class="['theme-option', { active: settings.theme === 'dark' }]"
                    @click="switchTheme('dark')"
                  >
                    <span class="theme-icon">🌙</span>
                    <span>深色</span>
                  </button>
                  <button
                    :class="['theme-option', { active: settings.theme === 'auto' }]"
                    @click="switchTheme('auto')"
                  >
                    <span class="theme-icon">🔄</span>
                    <span>自动</span>
                  </button>
                </div>
              </div>
            </div>

            <div class="setting-item">
              <div class="setting-info">
                <label class="setting-label">色弱模式</label>
                <p class="setting-desc">优化颜色对比度，提高可读性</p>
              </div>
              <div class="setting-control">
                <label class="switch">
                  <input type="checkbox" v-model="settings.colorWeak" />
                  <span class="slider"></span>
                </label>
              </div>
            </div>

            <div class="setting-item">
              <div class="setting-info">
                <label class="setting-label">紧凑模式</label>
                <p class="setting-desc">减少界面元素间距，显示更多内容</p>
              </div>
              <div class="setting-control">
                <label class="switch">
                  <input type="checkbox" v-model="settings.compactMode" />
                  <span class="slider"></span>
                </label>
              </div>
            </div>

            <div class="setting-item">
              <div class="setting-info">
                <label class="setting-label">动画效果</label>
                <p class="setting-desc">启用界面过渡动画</p>
              </div>
              <div class="setting-control">
                <label class="switch">
                  <input type="checkbox" v-model="settings.animations" />
                  <span class="slider"></span>
                </label>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 通知设置 -->
      <section class="settings-section">
        <div class="settings-card card elevation-1">
          <div class="card-header">
            <h3 class="card-title">🔔 通知设置</h3>
            <p class="card-subtitle">管理系统通知偏好</p>
          </div>
          <div class="card-body">
            <div class="setting-item">
              <div class="setting-info">
                <label class="setting-label">桌面通知</label>
                <p class="setting-desc">在桌面显示系统通知</p>
              </div>
              <div class="setting-control">
                <label class="switch">
                  <input type="checkbox" v-model="settings.desktopNotifications" />
                  <span class="slider"></span>
                </label>
              </div>
            </div>

            <div class="setting-item">
              <div class="setting-info">
                <label class="setting-label">声音提醒</label>
                <p class="setting-desc">重要事件播放提示音</p>
              </div>
              <div class="setting-control">
                <label class="switch">
                  <input type="checkbox" v-model="settings.soundAlerts" />
                  <span class="slider"></span>
                </label>
              </div>
            </div>

            <div class="setting-item">
              <div class="setting-info">
                <label class="setting-label">邮件通知</label>
                <p class="setting-desc">通过邮件接收重要告警</p>
              </div>
              <div class="setting-control">
                <label class="switch">
                  <input type="checkbox" v-model="settings.emailNotifications" />
                  <span class="slider"></span>
                </label>
              </div>
            </div>

            <div class="setting-item" v-if="settings.emailNotifications">
              <div class="setting-info">
                <label class="setting-label">通知邮箱</label>
                <p class="setting-desc">接收通知的邮箱地址</p>
              </div>
              <div class="setting-control">
                <input
                  type="email"
                  v-model="settings.notificationEmail"
                  class="form-input"
                  placeholder="example@domain.com"
                />
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 高级设置 -->
      <section class="settings-section">
        <div class="settings-card card elevation-1">
          <div class="card-header">
            <h3 class="card-title">🔧 高级设置</h3>
            <p class="card-subtitle">高级系统配置选项</p>
          </div>
          <div class="card-body">
            <div class="setting-item">
              <div class="setting-info">
                <label class="setting-label">开发者模式</label>
                <p class="setting-desc">显示调试信息和开发工具</p>
              </div>
              <div class="setting-control">
                <label class="switch">
                  <input type="checkbox" v-model="settings.developerMode" />
                  <span class="slider"></span>
                </label>
              </div>
            </div>

            <div class="setting-item">
              <div class="setting-info">
                <label class="setting-label">性能监控</label>
                <p class="setting-desc">实时显示系统性能指标</p>
              </div>
              <div class="setting-control">
                <label class="switch">
                  <input type="checkbox" v-model="settings.performanceMonitor" />
                  <span class="slider"></span>
                </label>
              </div>
            </div>

            <div class="setting-item">
              <div class="setting-info">
                <label class="setting-label">日志级别</label>
                <p class="setting-desc">控制日志输出详细程度</p>
              </div>
              <div class="setting-control">
                <select v-model="settings.logLevel" class="form-select">
                  <option value="error">错误</option>
                  <option value="warning">警告</option>
                  <option value="info">信息</option>
                  <option value="debug">调试</option>
                </select>
              </div>
            </div>

            <div class="setting-item">
              <div class="setting-info">
                <label class="setting-label">缓存管理</label>
                <p class="setting-desc">清理本地缓存数据</p>
              </div>
              <div class="setting-control">
                <button class="btn btn-ghost" @click="clearCache">
                  <span>🗑️ 清理缓存</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- 操作提示 -->
    <div class="settings-footer">
      <div class="info-card card elevation-1">
        <p>💡 设置将自动保存到本地，刷新页面后仍然有效。部分设置可能需要刷新页面才能生效。</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { useThemeStore } from '@/stores/theme';

// 使用主题store
const themeStore = useThemeStore();

// 监听store中主题的变化，同步到settings
watch(() => themeStore.theme, (newTheme) => {
  settings.value.theme = newTheme;
});

// 设置项
const settings = ref({
  // 常规设置
  interval: 5000,
  pageSize: 20,
  language: 'zh-CN',
  // 外观设置
  theme: 'light',  // 这个会和store同步
  colorWeak: false,
  compactMode: false,
  animations: true,
  // 通知设置
  desktopNotifications: true,
  soundAlerts: false,
  emailNotifications: false,
  notificationEmail: '',
  // 高级设置
  developerMode: false,
  performanceMonitor: false,
  logLevel: 'info'
});

// 加载保存的设置
onMounted(() => {
  const saved = localStorage.getItem('settings');
  if (saved) {
    try {
      const savedSettings = JSON.parse(saved);
      Object.assign(settings.value, savedSettings);

      // 应用其他视觉设置
      if (savedSettings.colorWeak) {
        document.documentElement.classList.add('color-weak');
      }
      if (savedSettings.compactMode) {
        document.documentElement.classList.add('compact-mode');
      }
    } catch (e) {
      console.error('加载设置失败', e);
    }
  }

  // 同步theme store的主题到settings
  settings.value.theme = themeStore.theme;
});


// 自动保存设置
watch(settings, (newSettings) => {
  localStorage.setItem('settings', JSON.stringify(newSettings));

  // 应用色弱模式
  if (newSettings.colorWeak) {
    document.documentElement.classList.add('color-weak');
  } else {
    document.documentElement.classList.remove('color-weak');
  }

  // 应用紧凑模式
  if (newSettings.compactMode) {
    document.documentElement.classList.add('compact-mode');
  } else {
    document.documentElement.classList.remove('compact-mode');
  }
}, { deep: true });

// 保存设置
function save() {
  localStorage.setItem('settings', JSON.stringify(settings.value));
  ElMessage.success({
    message: '设置已保存',
    duration: 2000
  });

  // 应用主题设置
  if (settings.value.theme === 'dark') {
    document.documentElement.setAttribute('data-theme', 'dark');
  } else if (settings.value.theme === 'light') {
    document.documentElement.setAttribute('data-theme', 'light');
  } else {
    // 自动模式：根据系统偏好设置
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    document.documentElement.setAttribute('data-theme', prefersDark ? 'dark' : 'light');
  }
}

// 切换主题
function switchTheme(theme: 'light' | 'dark' | 'auto') {
  // 更新本地settings
  settings.value.theme = theme;

  // 使用store来设置主题，这会同步到所有使用主题的地方
  themeStore.setTheme(theme);

  ElMessage.success(`已切换到${theme === 'light' ? '浅色' : theme === 'dark' ? '深色' : '自动'}主题`);
}

// 清理缓存
function clearCache() {
  // 清理本地存储（保留设置）
  const savedSettings = localStorage.getItem('settings');
  localStorage.clear();
  if (savedSettings) {
    localStorage.setItem('settings', savedSettings);
  }

  // 清理会话存储
  sessionStorage.clear();

  ElMessage.success({
    message: '缓存已清理',
    duration: 2000
  });
}
</script>

<style scoped>
.settings {
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

/* 设置网格 */
.settings-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(500px, 1fr));
  gap: var(--spacing-lg);
  margin-bottom: 32px;
}

.settings-section {
  width: 100%;
}

/* 设置卡片 */
.settings-card {
  height: 100%;
}

.card-header {
  padding: var(--spacing-lg);
  border-bottom: 1px solid var(--color-border);
}

.card-title {
  font-size: var(--text-lg);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  margin: 0 0 4px 0;
}

.card-subtitle {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  margin: 0;
}

.card-body {
  padding: var(--spacing-lg);
}

/* 设置项 */
.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid var(--color-border);
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-info {
  flex: 1;
  margin-right: 24px;
}

.setting-label {
  display: block;
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-text-primary);
  margin-bottom: 4px;
}

.setting-desc {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
  margin: 0;
  line-height: 1.4;
}

.setting-control {
  display: flex;
  align-items: center;
  gap: 8px;
}

.unit {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

/* 表单元素 */
.form-input,
.form-select {
  padding: 8px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-primary);
  font-size: var(--text-sm);
  min-width: 150px;
  transition: all var(--transition-fast);
}

.form-input:focus,
.form-select:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(17, 115, 212, 0.1);
}

/* 主题选择器 */
.theme-selector {
  display: flex;
  gap: 8px;
}

.theme-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 16px;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.theme-option:hover {
  border-color: var(--color-primary);
  transform: translateY(-2px);
}

.theme-option.active {
  border-color: var(--color-primary);
  background: rgba(17, 115, 212, 0.1);
}

.theme-icon {
  font-size: 24px;
}

/* 开关组件 */
.switch {
  position: relative;
  display: inline-block;
  width: 48px;
  height: 24px;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: var(--color-gray-300);
  transition: var(--transition-fast);
  border-radius: 24px;
}

.slider:before {
  position: absolute;
  content: "";
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: var(--transition-fast);
  border-radius: 50%;
}

input:checked + .slider {
  background-color: var(--color-primary);
}

input:checked + .slider:before {
  transform: translateX(24px);
}

/* 页脚信息 */
.settings-footer {
  margin-top: 32px;
}

.info-card {
  padding: var(--spacing-lg);
  background: var(--color-surface);
}

.info-card p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--text-sm);
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .settings-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .setting-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .setting-info {
    margin-right: 0;
  }

  .setting-control {
    width: 100%;
  }

  .form-input,
  .form-select {
    width: 100%;
  }

  .theme-selector {
    width: 100%;
  }

  .theme-option {
    flex: 1;
  }
}
</style>