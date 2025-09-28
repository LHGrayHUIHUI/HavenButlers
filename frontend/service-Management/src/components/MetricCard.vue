<template>
  <div class="card" role="group" :aria-label="title">
    <div class="title">{{ title }}</div>
    <div class="value">{{ value }}</div>
  </div>
</template>

<script setup lang="ts">
// 指标卡片组件：显示标题与数值
defineProps<{ title: string; value: number | string }>();
</script>

<style scoped>
.card { border: 1px solid #eee; border-radius: 8px; padding: 12px; background: #fff; }
.title { color: #666; font-size: 12px; }
.value { font-size: 24px; font-weight: 600; margin-top: 6px; }
</style>

