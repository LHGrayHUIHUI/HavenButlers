import { createRouter, createWebHashHistory, RouteRecordRaw } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

// 路由表：根据文档定义核心页面
const routes: RouteRecordRaw[] = [
  { path: '/', name: 'dashboard', component: () => import('@/views/Dashboard.vue') },
  { path: '/services', name: 'services', component: () => import('@/views/Services.vue') },
  { path: '/services/:name', name: 'service-detail', component: () => import('@/views/ServiceDetail.vue') },
  { path: '/alerts', name: 'alerts', component: () => import('@/views/Alerts.vue') },
  { path: '/env', name: 'env', component: () => import('@/views/Environment.vue') },
  { path: '/settings', name: 'settings', component: () => import('@/views/Settings.vue') },
  { path: '/login', name: 'login', component: () => import('@/views/Login.vue') }
];

const router = createRouter({
  history: createWebHashHistory(),
  routes
});

// 登录守卫：无凭据跳转登录（SSE 同源，Basic 在会话层传递）
router.beforeEach((to, _from, next) => {
  const auth = useAuthStore();
  const isPublic = to.name === 'login';
  if (isPublic) return next();
  if (!auth.basicToken) return next({ name: 'login' });
  next();
});

export default router;

