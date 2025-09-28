<template>
  <section>
    <h2>Environment</h2>
    <p>当前环境：<strong>{{ current?.name || '-' }}</strong></p>
    <button @click="onRefresh">刷新配置</button>
    <h3>可用环境</h3>
    <ul>
      <li v-for="e in envs" :key="e">
        {{ e }}
        <button @click="onSwitch(e)">切换</button>
      </li>
    </ul>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { fetchAvailableEnvs, fetchCurrentEnv, refreshConfig, switchEnvironment } from '@/api/service';

const current = ref<any>(null);
const envs = ref<string[]>([]);

async function load() {
  current.value = await fetchCurrentEnv();
  envs.value = await fetchAvailableEnvs();
}

async function onRefresh() {
  if (!confirm('将刷新配置，可能造成短暂抖动，是否继续？')) return;
  await refreshConfig();
  alert('已刷新配置');
}

async function onSwitch(env: string) {
  if (!confirm(`确认切换到环境：${env} ?`)) return;
  await switchEnvironment(env);
  alert('已切换环境');
  await load();
}

onMounted(load);
</script>

<style scoped>
ul { padding-left: 16px; }
li { margin: 6px 0; }
</style>

