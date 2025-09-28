<template>
  <section>
    <h2>Service Detail - {{ name }}</h2>
    <nav class="tabs" aria-label="详情标签">
      <button :class="{active: tab==='health'}" @click="tab='health'">Health</button>
      <button :class="{active: tab==='metrics'}" @click="tab='metrics'">Metrics</button>
      <button :class="{active: tab==='logs'}" @click="tab='logs'">Logs</button>
      <button :class="{active: tab==='nacos'}" @click="tab='nacos'">Nacos</button>
    </nav>
    <div v-if="tab==='health'">
      <pre>{{ health }}</pre>
    </div>
    <div v-else-if="tab==='metrics'">
      <pre>{{ metrics }}</pre>
    </div>
    <div v-else-if="tab==='logs'">
      <div class="logs-bar">
        <label>
          级别
          <select v-model="logQuery.level">
            <option>INFO</option>
            <option>WARN</option>
            <option>ERROR</option>
          </select>
        </label>
        <button @click="loadLogs(1)">刷新</button>
      </div>
      <LogViewer :lines="logs" />
      <div class="pager">
        <button :disabled="!logPage.hasPrevious" @click="loadLogs(logPage.page-1)">上一页</button>
        <span>{{ logPage.page }}/{{ logPage.totalPage }}</span>
        <button :disabled="!logPage.hasNext" @click="loadLogs(logPage.page+1)">下一页</button>
      </div>
    </div>
    <div v-else>
      <p>Nacos 详情：后端接口联调时补充。</p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { fetchServiceHealth, fetchServiceMetrics, fetchServiceLogs } from '@/api/service';
import LogViewer from '@/components/LogViewer.vue';

// 路由参数：服务名
const route = useRoute();
const name = ref<string>((route.params.name as string) || '');
const tab = ref<'health'|'metrics'|'logs'|'nacos'>('health');

// 健康与指标数据
const health = ref<any>(null);
const metrics = ref<any>(null);

// 日志分页与内容
const logQuery = reactive({ level: 'INFO', page: 1, size: 100 });
const logPage = reactive({ page: 1, totalPage: 0, hasPrevious: false, hasNext: false });
const logs = ref<string[]>([]);

async function loadHealth() {
  health.value = await fetchServiceHealth(name.value);
}

async function loadMetrics() {
  metrics.value = await fetchServiceMetrics(name.value, {});
}

async function loadLogs(p?: number) {
  if (p) logQuery.page = p;
  const res = await fetchServiceLogs(name.value, { ...logQuery });
  Object.assign(logPage, { page: res.page, totalPage: res.totalPage, hasPrevious: res.hasPrevious, hasNext: res.hasNext });
  logs.value = res.list as string[];
}

onMounted(() => {
  loadHealth();
  watch(tab, (t) => {
    if (t === 'metrics') loadMetrics();
    if (t === 'logs') loadLogs(1);
  }, { immediate: false });
});
</script>

<style scoped>
.tabs { display:flex; gap:8px; margin: 8px 0; }
.tabs button { padding: 6px 10px; }
.tabs .active { font-weight: 600; }
.logs-bar { display:flex; align-items:center; gap: 8px; margin: 8px 0; }
.pager { display:flex; gap: 8px; align-items:center; margin-top: 8px; }
</style>

