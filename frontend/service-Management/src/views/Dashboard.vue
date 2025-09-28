<template>
  <div class="dashboard">
    <el-page-header>
      <template #content>
        <h2>Dashboard</h2>
        <p class="desc">实时健康总览（SSE 首选，失败回退轮询）</p>
      </template>
    </el-page-header>

    <!-- 统计卡片网格 -->
    <el-row :gutter="16" class="stats-grid">
      <el-col :span="6">
        <MetricCard title="服务总数" :value="stats.total" />
      </el-col>
      <el-col :span="6">
        <MetricCard title="UP" :value="stats.up" status="up" />
      </el-col>
      <el-col :span="6">
        <MetricCard title="DEGRADED" :value="stats.degraded" status="degraded" />
      </el-col>
      <el-col :span="6">
        <MetricCard title="DOWN" :value="stats.down" status="down" />
      </el-col>
    </el-row>

    <!-- 服务列表 -->
    <el-card class="service-list" shadow="never">
      <template #header>
        <div class="card-header">
          <span>服务状态</span>
          <el-tag v-if="sseConnected" type="success" size="small">实时连接</el-tag>
          <el-tag v-else type="warning" size="small">轮询模式</el-tag>
        </div>
      </template>

      <el-table :data="services" style="width: 100%">
        <el-table-column prop="serviceName" label="服务名称" min-width="180">
          <template #default="scope">
            <el-link :href="`#/services/${scope.row.serviceName}`" type="primary">
              {{ scope.row.serviceName }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <StatusTag :status="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="totalInstances" label="实例数" width="100" />
        <el-table-column prop="healthyInstances" label="健康实例" width="100" />
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button
              size="small"
              type="primary"
              link
              @click="$router.push(`/services/${scope.row.serviceName}`)"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref } from 'vue';
import { connectSSE } from '@/utils/sse';
import { fetchOverview } from '@/api/service';
import StatusTag from '@/components/StatusTag.vue';
import MetricCard from '@/components/MetricCard.vue';

// 实时健康数据：优先 SSE，失败回退为 5s 轮询
const services = ref<any[]>([]);
const stats = reactive({ total: 0, up: 0, degraded: 0, down: 0 });
const sseConnected = ref(true); // SSE连接状态
let handler: { stop: () => void } | null = null;

onMounted(() => {
  const url = (import.meta.env.VITE_SSE_PATH as string) || '/api/service/stream/health';
  handler = connectSSE(url, {
    onMessage: (list: any[]) => {
      services.value = list || [];
      stats.total = list.length;
      stats.up = list.filter((i) => i.status === 'UP').length;
      stats.degraded = list.filter((i) => i.status === 'DEGRADED').length;
      stats.down = list.filter((i) => i.status === 'DOWN').length;
    },
    onError: () => {
      // 可在 UI 上提示：实时连接异常，已切换为轮询
      sseConnected.value = false;
    },
    // 轮询回退：调用总览接口
    poll: async () => await fetchOverview()
  });
});

onUnmounted(() => handler?.stop());
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.desc {
  color: #666;
  margin: 8px 0;
}

.stats-grid {
  margin: 20px 0;
}

.service-list {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

