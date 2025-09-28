<template>
  <section>
    <h2>Service Detail - {{ name }}</h2>
    <el-tabs v-model="tab">
      <el-tab-pane label="Health" name="health">
        <el-card shadow="never"><pre>{{ health }}</pre></el-card>
      </el-tab-pane>
      <el-tab-pane label="Metrics" name="metrics">
        <el-card shadow="never">
          <div ref="chartRef" class="chart" aria-label="指标图表"></div>
          <div v-if="metricRaw && metricRaw.__raw__" class="raw">
            <p>未识别的指标结构，原始数据：</p>
            <pre>{{ metricRaw.__raw__ }}</pre>
          </div>
        </el-card>
      </el-tab-pane>
      <el-tab-pane label="Logs" name="logs">
        <div class="logs-bar">
          <el-select v-model="logQuery.level" style="width:120px">
            <el-option label="INFO" value="INFO" />
            <el-option label="WARN" value="WARN" />
            <el-option label="ERROR" value="ERROR" />
          </el-select>
          <el-button @click="loadLogs(1)">刷新</el-button>
        </div>
        <LogViewer :lines="logs" />
        <div class="pager">
          <el-button :disabled="!logPage.hasPrevious" @click="loadLogs(logPage.page-1)">上一页</el-button>
          <span>{{ logPage.page }}/{{ logPage.totalPage }}</span>
          <el-button :disabled="!logPage.hasNext" @click="loadLogs(logPage.page+1)">下一页</el-button>
        </div>
      </el-tab-pane>
      <el-tab-pane label="Nacos" name="nacos">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">Nacos 实例</div>
          </template>
          <el-table :data="instances" style="width:100%">
            <el-table-column prop="ip" label="IP" width="160" />
            <el-table-column prop="port" label="端口" width="100" />
            <el-table-column prop="healthy" label="健康" width="100">
              <template #default="{ row }">
                <el-tag :type="row.healthy ? 'success' : 'danger'">{{ row.healthy ? 'HEALTHY' : 'DOWN' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="metadata" label="元数据" />
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-popconfirm title="确认临时下线该实例？" @confirm="onDeregister(row)">
                  <template #reference>
                    <el-button size="small" type="danger">下线</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </section>
  
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { fetchServiceHealth, fetchServiceMetrics, fetchServiceLogs, fetchNacosInstances, nacosDeregister } from '@/api/service';
import LogViewer from '@/components/LogViewer.vue';
import * as echarts from 'echarts';
import { ElMessage } from 'element-plus';

// 路由参数：服务名
const route = useRoute();
const name = ref<string>((route.params.name as string) || '');
const tab = ref<'health'|'metrics'|'logs'|'nacos'>('health');

// 健康与指标数据
const health = ref<any>(null);
const metricRaw = ref<any>(null);
const chartRef = ref<HTMLDivElement | null>(null);
let chart: echarts.ECharts | null = null;

// 日志分页与内容
const logQuery = reactive({ level: 'INFO', page: 1, size: 100 });
const logPage = reactive({ page: 1, totalPage: 0, hasPrevious: false, hasNext: false });
const logs = ref<string[]>([]);

// Nacos 实例
const instances = ref<any[]>([]);

async function loadHealth() {
  health.value = await fetchServiceHealth(name.value);
}

// 渲染指标图表（尽量自适应数据结构）
async function loadMetrics() {
  const data = await fetchServiceMetrics(name.value, {});
  metricRaw.value = {};
  try {
    const option = asLineOption(data);
    nextTickRender(option);
  } catch (_) {
    metricRaw.value.__raw__ = JSON.stringify(data, null, 2);
  }
}

function nextTickRender(option: echarts.EChartsOption) {
  setTimeout(() => {
    if (!chartRef.value) return;
    if (!chart) chart = echarts.init(chartRef.value);
    chart.setOption(option);
  });
}

function asLineOption(data: any): echarts.EChartsOption {
  // 期望数据格式：{ timestamps: number[], series: { name: string, values: number[] }[] }
  if (data?.timestamps && Array.isArray(data.timestamps) && Array.isArray(data.series)) {
    return {
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: data.timestamps.map((t: any) => new Date(t).toLocaleTimeString()) },
      yAxis: { type: 'value' },
      series: data.series.map((s: any) => ({ name: s.name, type: 'line', showSymbol: false, data: s.values }))
    };
  }
  // 兜底：尝试从对象键中找可绘制数列
  const keys = Object.keys(data || {}).filter(k => Array.isArray((data as any)[k]));
  if (keys.length) {
    const key = keys[0];
    return {
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: (data[key] as any[]).map((_: any, i: number) => String(i)) },
      yAxis: { type: 'value' },
      series: [{ name: key, type: 'line', showSymbol: false, data: data[key] }]
    };
  }
  throw new Error('unrecognizable metrics');
}

async function loadLogs(p?: number) {
  if (p) logQuery.page = p;
  const res = await fetchServiceLogs(name.value, { ...logQuery });
  Object.assign(logPage, { page: res.page, totalPage: res.totalPage, hasPrevious: res.hasPrevious, hasNext: res.hasNext });
  logs.value = res.list as string[];
}

async function loadNacos() {
  instances.value = await fetchNacosInstances(name.value);
}

async function onDeregister(row: any) {
  try {
    await nacosDeregister(name.value, row.ip, row.port);
    ElMessage.success('实例已下线');
    await loadNacos();
  } catch (e) {
    ElMessage.error('下线失败');
  }
}

onMounted(() => {
  loadHealth();
});

watch(tab, (t) => {
  if (t === 'metrics') loadMetrics();
  if (t === 'logs') loadLogs(1);
  if (t === 'nacos') loadNacos();
}, { immediate: false });
</script>

<style scoped>
.logs-bar { display:flex; align-items:center; gap: 8px; margin: 8px 0; }
.pager { display:flex; gap: 8px; align-items:center; margin-top: 8px; }
.chart { width: 100%; height: 360px; }
.raw pre { background:#f5f5f5; padding:8px; border-radius:6px; }
</style>
