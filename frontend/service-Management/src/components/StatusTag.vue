<template>
  <span :class="['tag', status.toLowerCase()]" role="status" :aria-label="`状态 ${status}`">{{ status }}</span>
  </template>

<script setup lang="ts">
// 状态标签组件：语义色与文本并行，照顾可访问性
defineProps<{ status: 'UP' | 'DEGRADED' | 'DOWN' | string }>();
</script>

<style scoped>
.tag { padding: 2px 8px; border-radius: 10px; font-size: 12px; color: #fff; }
.up { background: #2fb344; }
.degraded { background: #f59f00; }
.down { background: #e03131; }
</style>

