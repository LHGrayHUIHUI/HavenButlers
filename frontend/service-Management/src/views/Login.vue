<template>
  <section class="login">
    <h1>登录</h1>
    <form @submit.prevent="onSubmit" aria-label="登录表单">
      <label>
        用户名
        <input v-model="user" name="username" autocomplete="username" required />
      </label>
      <label>
        密码
        <input v-model="pass" name="password" type="password" autocomplete="current-password" required />
      </label>
      <button type="submit">登录</button>
    </form>
    <p class="hint">凭据以 Basic 串存入 sessionStorage，关闭浏览器失效。</p>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

// 登录页面：构造 Basic 串并写入 Pinia+sessionStorage
const user = ref('');
const pass = ref('');
const router = useRouter();
const auth = useAuthStore();

function onSubmit() {
  // 生成 Basic 凭据（注意：生产环境请配合 HTTPS 与后端认证策略）
  const token = 'Basic ' + btoa(`${user.value}:${pass.value}`);
  auth.setToken(token, user.value);
  router.replace({ name: 'dashboard' });
}
</script>

<style scoped>
.login { max-width: 360px; margin: 10vh auto; padding: 24px; border: 1px solid #eee; border-radius: 8px; }
label { display: block; margin: 12px 0; }
input { width: 100%; padding: 8px; box-sizing: border-box; }
button { width: 100%; padding: 10px; margin-top: 12px; }
.hint { color: #666; font-size: 12px; }
</style>

