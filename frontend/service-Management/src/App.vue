<template>
  <div class="app-container">
    <!-- 顶部导航栏 - 玻璃态效果 -->
    <header v-if="!isLoginPage" class="app-header header-sticky" role="banner">
      <div class="header-container">
        <div class="header-left">
          <!-- Logo和品牌 -->
          <div class="brand-wrapper">
            <svg class="logo" fill="none" viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <path clip-rule="evenodd" d="M39.475 21.6262C40.358 21.4363 40.6863 21.5589 40.7581 21.5934C40.7876 21.655 40.8547 21.857 40.8082 22.3336C40.7408 23.0255 40.4502 24.0046 39.8572 25.2301C38.6799 27.6631 36.5085 30.6631 33.5858 33.5858C30.6631 36.5085 27.6632 38.6799 25.2301 39.8572C24.0046 40.4502 23.0255 40.7407 22.3336 40.8082C21.8571 40.8547 21.6551 40.7875 21.5934 40.7581C21.5589 40.6863 21.4363 40.358 21.6262 39.475C21.8562 38.4054 22.4689 36.9657 23.5038 35.2817C24.7575 33.2417 26.5497 30.9744 28.7621 28.762C30.9744 26.5497 33.2417 24.7574 35.2817 23.5037C36.9657 22.4689 38.4054 21.8562 39.475 21.6262ZM4.41189 29.2403L18.7597 43.5881C19.8813 44.7097 21.4027 44.9179 22.7217 44.7893C24.0585 44.659 25.5148 44.1631 26.9723 43.4579C29.9052 42.0387 33.2618 39.5667 36.4142 36.4142C39.5667 33.2618 42.0387 29.9052 43.4579 26.9723C44.1631 25.5148 44.659 24.0585 44.7893 22.7217C44.9179 21.4027 44.7097 19.8813 43.5881 18.7597L29.2403 4.41187C27.8527 3.02428 25.8765 3.02573 24.2861 3.36776C22.6081 3.72863 20.7334 4.58419 18.8396 5.74801C16.4978 7.18716 13.9881 9.18353 11.5858 11.5858C9.18354 13.988 7.18717 16.4978 5.74802 18.8396C4.58421 20.7334 3.72865 22.6081 3.36778 24.2861C3.02574 25.8765 3.02429 27.8527 4.41189 29.2403Z" fill="currentColor" fill-rule="evenodd"></path>
            </svg>
            <h1 class="brand-text">HavenButler Admin</h1>
          </div>

          <!-- 主导航 -->
          <nav class="main-nav" aria-label="主导航">
            <RouterLink to="/" class="nav-link" exact-active-class="nav-link-active">
              <span>Overview</span>
            </RouterLink>
            <RouterLink to="/services" class="nav-link" active-class="nav-link-active">
              <span>Services</span>
            </RouterLink>
            <RouterLink to="/alerts" class="nav-link" active-class="nav-link-active">
              <span>Alerts</span>
            </RouterLink>
            <RouterLink to="/env" class="nav-link" active-class="nav-link-active">
              <span>Environment</span>
            </RouterLink>
            <RouterLink to="/settings" class="nav-link" active-class="nav-link-active">
              <span>Settings</span>
            </RouterLink>
          </nav>
        </div>

        <div class="header-right">
          <!-- 主题切换按钮 -->
          <button class="icon-btn" @click="toggleTheme" aria-label="切换主题">
            <span v-if="theme === 'light'">🌙</span>
            <span v-else>☀️</span>
          </button>

          <!-- 通知按钮 -->
          <button class="icon-btn notification-btn" aria-label="通知">
            <span>🔔</span>
            <span v-if="hasNotifications" class="notification-badge"></span>
          </button>

          <!-- 用户头像和下拉菜单 -->
          <div class="user-menu-wrapper" @click.stop>
            <button class="user-avatar" @click="toggleUserMenu" aria-label="用户菜单">
              <span>👤</span>
            </button>
            <transition name="dropdown">
              <div v-if="showUserMenu" class="user-menu" @click.stop>
                <div class="menu-header">
                  <div class="menu-user-info">
                    <div class="menu-avatar">
                      <span>👤</span>
                    </div>
                    <div class="menu-user-text">
                      <div class="menu-username">{{ authStore.username || 'Admin' }}</div>
                      <div class="menu-role">系统管理员</div>
                    </div>
                  </div>
                </div>
                <div class="menu-divider"></div>
                <button class="menu-item" @click="router.push('/settings')">
                  <span class="menu-icon">⚙️</span>
                  <span>设置</span>
                </button>
                <button class="menu-item" @click="router.push('/profile')">
                  <span class="menu-icon">👤</span>
                  <span>个人资料</span>
                </button>
                <div class="menu-divider"></div>
                <button class="menu-item menu-item-danger" @click="logout">
                  <span class="menu-icon">🚪</span>
                  <span>退出登录</span>
                </button>
              </div>
            </transition>
          </div>
        </div>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="app-main scrollbar-thin" role="main">
      <div class="main-container">
        <RouterView v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </RouterView>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useThemeStore } from '@/stores/theme'
import { useAuthStore } from '@/stores/auth'

// 使用store和路由
const themeStore = useThemeStore()
const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const theme = computed(() => themeStore.theme)
const hasNotifications = ref(false)
const showUserMenu = ref(false)

// 判断是否为登录页
const isLoginPage = computed(() => route.path === '/login')

// 切换主题
const toggleTheme = () => {
  themeStore.toggleTheme()
}

// 切换用户菜单
const toggleUserMenu = () => {
  showUserMenu.value = !showUserMenu.value
}

// 登出
const logout = () => {
  authStore.clear()
  router.push('/login')
  showUserMenu.value = false
}

// 点击外部关闭菜单
const closeUserMenu = () => {
  showUserMenu.value = false
}

onMounted(() => {
  // 初始化主题
  themeStore.initTheme()

  // 模拟通知状态
  setTimeout(() => {
    hasNotifications.value = true
  }, 3000)

  // 点击外部关闭用户菜单
  document.addEventListener('click', closeUserMenu)
})

// 清理事件监听
onUnmounted(() => {
  document.removeEventListener('click', closeUserMenu)
})
</script>

<style scoped>
.app-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--color-background);
  color: var(--color-text-primary);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  transition: background-color var(--transition-base);
}

/* 头部样式 */
.app-header {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 24px;
}

.header-container {
  width: 100%;
  max-width: 1440px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 48px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* Logo和品牌 */
.brand-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  width: 28px;
  height: 28px;
  color: var(--color-primary);
}

.brand-text {
  font-size: var(--text-lg);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  margin: 0;
}

/* 导航样式 */
.main-nav {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-link {
  position: relative;
  padding: 8px 16px;
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-text-secondary);
  text-decoration: none;
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.nav-link:hover {
  color: var(--color-primary);
  background: var(--color-surface-hover);
}

.nav-link-active {
  color: var(--color-primary);
  background: rgba(17, 115, 212, 0.1);
}

/* 图标按钮 */
.icon-btn {
  position: relative;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: var(--color-surface-hover);
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all var(--transition-fast);
  font-size: 18px;
}

.icon-btn:hover {
  background: var(--color-gray-200);
  transform: scale(1.05);
}

[data-theme="dark"] .icon-btn:hover {
  background: var(--color-gray-700);
}

/* 通知徽章 */
.notification-btn {
  position: relative;
}

.notification-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 8px;
  height: 8px;
  background: var(--color-danger);
  border-radius: var(--radius-full);
  animation: pulse 2s infinite;
}

/* 用户菜单容器 */
.user-menu-wrapper {
  position: relative;
}

/* 用户头像 */
.user-avatar {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--color-primary), #1e88e5);
  border-radius: var(--radius-full);
  border: 2px solid rgba(17, 115, 212, 0.3);
  font-size: 20px;
  color: white;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.user-avatar:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(17, 115, 212, 0.25);
}

/* 用户下拉菜单 */
.user-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 280px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  z-index: 1000;
  overflow: hidden;
}

.menu-header {
  padding: 16px;
  background: var(--color-surface-hover);
}

.menu-user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.menu-avatar {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--color-primary), #1e88e5);
  border-radius: var(--radius-full);
  font-size: 24px;
  color: white;
}

.menu-user-text {
  flex: 1;
}

.menu-username {
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
}

.menu-role {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  margin-top: 2px;
}

.menu-divider {
  height: 1px;
  background: var(--color-border);
  margin: 0;
}

.menu-item {
  width: 100%;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  background: transparent;
  border: none;
  color: var(--color-text-primary);
  font-size: var(--text-sm);
  cursor: pointer;
  transition: background var(--transition-fast);
  text-align: left;
}

.menu-item:hover {
  background: var(--color-surface-hover);
}

.menu-item-danger {
  color: var(--color-danger);
}

.menu-icon {
  font-size: 16px;
}

/* 下拉动画 */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.2s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* 主内容区 */
.app-main {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: var(--color-background);
  color: var(--color-text-primary);
  transition: background-color var(--transition-base), color var(--transition-base);
}

.main-container {
  width: 100%;
  max-width: 1440px;
  margin: 0 auto;
}

/* 页面过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity var(--transition-base);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 响应式布局 */
@media (max-width: 768px) {
  .app-header {
    padding: 0 16px;
  }

  .header-left {
    gap: 24px;
  }

  .main-nav {
    display: none;
  }

  .app-main {
    padding: 16px;
  }
}
</style>

