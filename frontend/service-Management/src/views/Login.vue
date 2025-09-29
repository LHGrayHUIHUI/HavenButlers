<template>
  <div class="login-container">
    <div class="bg-decoration"></div>
    <div class="login-card">
      <div class="card-header">
        <div class="logo-container">
          <div class="logo">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
              <path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"/>
            </svg>
          </div>
        </div>
        <h1 class="title">HavenButler</h1>
        <p class="subtitle">智能家庭服务管理平台</p>
      </div>

      <form @submit.prevent="onSubmit" class="login-form" aria-label="登录表单">
        <div class="form-group">
          <label for="username" class="form-label">用户名</label>
          <div class="input-wrapper">
            <input
              id="username"
              v-model="user"
              type="text"
              name="username"
              autocomplete="username"
              required
              placeholder="请输入用户名"
              class="form-input"
              @focus="userFocused = true"
              @blur="userFocused = false"
            />
            <div class="input-border" :class="{ focused: userFocused }"></div>
          </div>
        </div>

        <div class="form-group">
          <label for="password" class="form-label">密码</label>
          <div class="input-wrapper">
            <input
              id="password"
              v-model="pass"
              :type="showPassword ? 'text' : 'password'"
              name="password"
              autocomplete="current-password"
              required
              placeholder="请输入密码"
              class="form-input"
              @focus="passFocused = true"
              @blur="passFocused = false"
            />
            <button
              type="button"
              class="password-toggle"
              @click="showPassword = !showPassword"
              :aria-label="showPassword ? '隐藏密码' : '显示密码'"
            >
              <svg v-if="!showPassword" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/>
              </svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z"/>
              </svg>
            </button>
            <div class="input-border" :class="{ focused: passFocused }"></div>
          </div>
        </div>

        <div class="form-actions">
          <button type="submit" class="submit-button" :disabled="loading">
            <span v-if="!loading">登录</span>
            <span v-else class="loading">
              <svg class="spinner" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <circle class="spinner-circle" cx="12" cy="12" r="10" stroke-width="3" fill="none"/>
              </svg>
              登录中...
            </span>
          </button>
        </div>

        <div class="form-footer">
          <p class="hint">凭据以 Basic 认证存储，关闭浏览器后失效</p>
        </div>
      </form>
    </div>

    <div class="ripple-container" v-if="showRipple" :style="rippleStyle"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

// 登录页面：构造 Basic 串并写入 Pinia+sessionStorage
const user = ref('');
const pass = ref('');
const router = useRouter();
const auth = useAuthStore();

// UI 状态
const userFocused = ref(false);
const passFocused = ref(false);
const showPassword = ref(false);
const loading = ref(false);
const showRipple = ref(false);
const rippleStyle = reactive({
  left: '0px',
  top: '0px'
});

async function onSubmit() {
  if (loading.value) return;
  if (!user.value.trim() || !pass.value.trim()) {
    alert('请输入用户名和密码');
    return;
  }

  loading.value = true;

  try {
    // 模拟异步登录过程
    await new Promise(resolve => setTimeout(resolve, 800));

    // 生成 Basic 凭据（注意：生产环境请配合 HTTPS 与后端认证策略）
    const token = 'Basic ' + btoa(`${user.value}:${pass.value}`);
    auth.setToken(token, user.value);

    // 触发涟漪动画后跳转
    const button = document.querySelector('.submit-button') as HTMLElement;
    if (button) {
      const rect = button.getBoundingClientRect();
      rippleStyle.left = `${rect.left + rect.width / 2}px`;
      rippleStyle.top = `${rect.top + rect.height / 2}px`;
      showRipple.value = true;
    }

    // 跳转到仪表盘
    setTimeout(() => {
      router.replace({ path: '/' });
    }, 600);
  } catch (error) {
    console.error('登录失败:', error);
    alert('登录失败，请重试');
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
/* Material 3 登录页面样式 - 新设计 */
.login-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(ellipse at top left, rgba(124, 58, 237, 0.15), transparent 50%),
    radial-gradient(ellipse at top right, rgba(59, 130, 246, 0.15), transparent 50%),
    radial-gradient(ellipse at bottom left, rgba(236, 72, 153, 0.15), transparent 50%),
    linear-gradient(135deg, #fafafa 0%, #f1f5f9 30%, #e2e8f0 100%);
  padding: 20px;
  overflow: hidden;
  box-sizing: border-box;
}

/* 动态背景装饰 */
.login-container::before {
  content: '';
  position: fixed;
  top: -30%;
  left: -30%;
  width: 80vw;
  height: 80vh;
  background:
    conic-gradient(from 45deg,
      rgba(124, 58, 237, 0.12),
      rgba(59, 130, 246, 0.08),
      rgba(236, 72, 153, 0.12),
      rgba(124, 58, 237, 0.12));
  border-radius: 50%;
  filter: blur(40px);
  animation: rotate-slow 30s infinite linear;
  z-index: 1;
}

.login-container::after {
  content: '';
  position: fixed;
  bottom: -30%;
  right: -30%;
  width: 70vw;
  height: 70vh;
  background:
    radial-gradient(circle,
      rgba(16, 185, 129, 0.1) 0%,
      rgba(59, 130, 246, 0.08) 50%,
      transparent 100%);
  border-radius: 50%;
  filter: blur(30px);
  animation: rotate-slow 25s infinite linear reverse;
  z-index: 1;
}

/* 新增第三个装饰元素 */
.login-container .bg-decoration {
  position: fixed;
  top: 15%;
  right: 10%;
  width: 15vw;
  height: 15vh;
  min-width: 120px;
  min-height: 120px;
  background: linear-gradient(45deg, rgba(251, 191, 36, 0.1), rgba(245, 101, 101, 0.1));
  border-radius: 30% 70% 70% 30% / 30% 30% 70% 70%;
  animation: morph 15s infinite ease-in-out;
  z-index: 1;
}

@keyframes rotate-slow {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes morph {
  0%, 100% {
    border-radius: 30% 70% 70% 30% / 30% 30% 70% 70%;
    transform: translate(0, 0) rotate(0deg);
  }
  25% {
    border-radius: 58% 42% 75% 25% / 76% 46% 54% 24%;
    transform: translate(10px, -10px) rotate(90deg);
  }
  50% {
    border-radius: 50% 50% 33% 67% / 55% 27% 73% 45%;
    transform: translate(-5px, 5px) rotate(180deg);
  }
  75% {
    border-radius: 33% 67% 58% 42% / 63% 68% 32% 37%;
    transform: translate(-10px, -5px) rotate(270deg);
  }
}

.login-card {
  width: 100%;
  max-width: 420px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(25px) saturate(180%);
  border-radius: 24px;
  box-shadow:
    0 32px 80px rgba(0, 0, 0, 0.08),
    0 16px 40px rgba(0, 0, 0, 0.04),
    0 8px 16px rgba(0, 0, 0, 0.02),
    inset 0 1px 0 rgba(255, 255, 255, 0.8),
    inset 0 0 0 1px rgba(255, 255, 255, 0.3);
  padding: 40px 36px;
  position: relative;
  z-index: 10;
  animation: cardEntry 0.6s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(255, 255, 255, 0.4);
}

@keyframes cardEntry {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.card-header {
  text-align: center;
  margin-bottom: 40px;
}

.logo-container {
  display: inline-flex;
  margin-bottom: 16px;
}

.logo {
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow:
    0 8px 32px rgba(79, 70, 229, 0.3),
    0 4px 16px rgba(79, 70, 229, 0.2);
  animation: logoFloat 4s infinite ease-in-out;
}

@keyframes logoFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}

.logo svg {
  width: 40px;
  height: 40px;
}

.title {
  font-size: 28px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 8px 0;
  letter-spacing: -0.5px;
}

.subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
  font-weight: 500;
}

.login-form {
  margin-top: 32px;
}

/* 表单组样式 */
.form-group {
  margin-bottom: 24px;
}

/* 标签样式 */
.form-label {
  display: block;
  font-size: 18px;
  font-weight: 800;
  color: #ffffff;
  margin-bottom: 12px;
  letter-spacing: 0.5px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

/* 输入框容器 */
.input-wrapper {
  position: relative;
}

/* 输入框样式 */
.form-input {
  width: 100%;
  padding: 16px 16px;
  font-size: 16px;
  border: 2px solid #e5e7eb;
  background: #ffffff;
  border-radius: 12px;
  outline: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  color: #111827;
  box-sizing: border-box;
}

/* 输入框placeholder样式 */
.form-input::placeholder {
  color: #9ca3af;
  font-size: 16px;
}

/* 输入框聚焦状态 */
.form-input:focus {
  border-color: #667eea;
  background: #f8faff;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

/* 输入框选中文字样式 */
.form-input::selection {
  background: #667eea;
  color: white;
}

.form-input::-moz-selection {
  background: #667eea;
  color: white;
}

/* 移除浏览器自动填充样式 */
.form-input:-webkit-autofill,
.form-input:-webkit-autofill:hover,
.form-input:-webkit-autofill:focus {
  -webkit-box-shadow: 0 0 0 30px #f8faff inset !important;
  -webkit-text-fill-color: #111827 !important;
  border-color: #667eea !important;
}

/* 密码切换按钮 */
.password-toggle {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
  color: #6b7280;
  transition: color 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
}

.password-toggle:hover {
  color: #667eea;
  background: rgba(102, 126, 234, 0.1);
}

.password-toggle svg {
  width: 20px;
  height: 20px;
}

/* 移除底部边框动画 */
.input-border {
  display: none;
}

.form-actions {
  margin-top: 36px;
}

.submit-button {
  width: 100%;
  padding: 14px;
  font-size: 16px;
  font-weight: 600;
  color: white;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 14px rgba(102, 126, 234, 0.4);
  position: relative;
  overflow: hidden;
}

.submit-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
}

.submit-button:active:not(:disabled) {
  transform: translateY(0);
}

.submit-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.submit-button .loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.spinner {
  width: 20px;
  height: 20px;
  animation: spin 1s linear infinite;
}

.spinner-circle {
  stroke: white;
  stroke-linecap: round;
  stroke-dasharray: 44;
  stroke-dashoffset: 44;
  animation: dash 1.5s ease-in-out infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes dash {
  0% { stroke-dashoffset: 44; }
  50% { stroke-dashoffset: 11; }
  100% { stroke-dashoffset: 44; }
}

.form-footer {
  text-align: center;
  margin-top: 24px;
}

.hint {
  font-size: 12px;
  color: #999;
  margin: 0;
  line-height: 1.5;
}

.ripple-container {
  position: fixed;
  width: 0;
  height: 0;
  border-radius: 50%;
  background: rgba(102, 126, 234, 0.3);
  transform: translate(-50%, -50%);
  pointer-events: none;
  animation: rippleExpand 0.8s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 9999;
}

@keyframes rippleExpand {
  to {
    width: 200vmax;
    height: 200vmax;
    opacity: 0;
  }
}

/* 全局样式重置 */
body {
  margin: 0 !important;
  padding: 0 !important;
  overflow: hidden;
}

html {
  margin: 0;
  padding: 0;
}

/* 响应式适配 */
@media (max-width: 480px) {
  .login-card {
    padding: 36px 24px;
    border-radius: 20px;
    margin: 16px;
  }

  .title {
    font-size: 24px;
  }

  .logo {
    width: 56px;
    height: 56px;
  }

  .form-label {
    font-size: 17px;
    color: #ffffff;
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.4);
  }

  .login-container .bg-decoration {
    width: 100px;
    height: 100px;
  }
}

/* 暗色模式支持 */
@media (prefers-color-scheme: dark) {
  .login-container {
    background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  }

  .login-card {
    background: rgba(30, 30, 30, 0.95);
    color: #e0e0e0;
  }

  .title {
    color: #ffffff;
  }

  .subtitle {
    color: #aaa;
  }

  .form-field input {
    background: #2a2a2a;
    color: #e0e0e0;
  }

  .form-field input:focus,
  .form-field input:not(:placeholder-shown) {
    background: #1f1f2e;
  }

  .form-field label {
    color: #888;
  }

  .field-line {
    background: #444;
  }

  .toggle-password {
    color: #aaa;
  }

  .hint {
    color: #888;
  }
}
</style>

