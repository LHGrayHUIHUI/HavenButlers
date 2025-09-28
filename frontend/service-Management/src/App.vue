<template>
  <!-- 应用根布局：顶部导航 + 主内容。遵循可访问性与 HIG -->
  <div class="layout">
    <header class="header" role="banner">
      <nav aria-label="主导航">
        <a class="brand" href="#/">HavenButler Admin</a>
        <ul class="nav">
          <li><RouterLink to="/">Dashboard</RouterLink></li>
          <li><RouterLink to="/services">Services</RouterLink></li>
          <li><RouterLink to="/alerts">Alerts</RouterLink></li>
          <li><RouterLink to="/env">Environment</RouterLink></li>
          <li><RouterLink to="/settings">Settings</RouterLink></li>
        </ul>
      </nav>
    </header>
    <main class="main" role="main">
      <RouterView />
    </main>
  </div>
  
</template>

<script setup lang="ts">
// 根组件仅负责布局与导航展示，业务逻辑在各 View 内处理
</script>

<style scoped>
.layout { display: flex; flex-direction: column; min-height: 100vh; }
.header { display: flex; align-items: center; padding: 8px 16px; border-bottom: 1px solid #eee; }
.brand { margin-right: 16px; font-weight: 600; text-decoration: none; color: #222; }
.nav { display: flex; gap: 12px; list-style: none; padding: 0; margin: 0; }
.main { flex: 1; padding: 16px; }
a { color: #2c7be5; text-decoration: none; }
a.router-link-active { font-weight: 600; }
</style>

