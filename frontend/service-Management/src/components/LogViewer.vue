<template>
  <pre class="log" aria-label="日志输出"><code>
{{ lines.join('\n') }}
  </code></pre>
</template>

<script setup lang="ts">
// 日志查看组件：简单展示行文本，后续可扩展高亮与级别着色
defineProps<{ lines: string[] }>();
</script>

<style scoped>
.log { background: #0a0a0a; color: #ddd; padding: 12px; border-radius: 6px; max-height: 360px; overflow: auto; }
</style>

