<template>
  <section>
    <h2>Settings</h2>
    <label>
      刷新间隔（ms）
      <input type="number" v-model.number="interval" min="1000" step="500" />
    </label>
    <label>
      色弱模式
      <input type="checkbox" v-model="colorWeak" />
    </label>
    <button @click="save">保存</button>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';

// 简单设置项示例：本地存储，不影响后端
const interval = ref(5000);
const colorWeak = ref(false);

onMounted(() => {
  const saved = localStorage.getItem('settings');
  if (saved) {
    const obj = JSON.parse(saved);
    interval.value = obj.interval ?? 5000;
    colorWeak.value = !!obj.colorWeak;
  }
});

function save() {
  localStorage.setItem('settings', JSON.stringify({ interval: interval.value, colorWeak: colorWeak.value }));
  alert('已保存设置');
}
</script>

<style scoped>
label { display:block; margin: 8px 0; }
</style>

