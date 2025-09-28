<template>
  <section>
    <h2>Services</h2>
    <form class="toolbar" @submit.prevent="onSearch">
      <input v-model="q.keyword" placeholder="搜索关键字" aria-label="搜索关键字" />
      <select v-model="q.status" aria-label="状态筛选">
        <option value="">全部状态</option>
        <option value="UP">UP</option>
        <option value="DEGRADED">DEGRADED</option>
        <option value="DOWN">DOWN</option>
      </select>
      <button type="submit">查询</button>
    </form>
    <table class="table" role="table" aria-label="服务列表">
      <thead>
        <tr><th>名称</th><th>状态</th><th>实例</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="it in list" :key="it.serviceName">
          <td><RouterLink :to="`/services/${it.serviceName}`">{{ it.serviceName }}</RouterLink></td>
          <td><StatusTag :status="it.status" /></td>
          <td>{{ it.totalInstances }}</td>
          <td>
            <RouterLink :to="`/services/${it.serviceName}`">详情</RouterLink>
          </td>
        </tr>
      </tbody>
    </table>
    <div class="pager">
      <button :disabled="!page.hasPrevious" @click="go(page.page - 1)">上一页</button>
      <span>第 {{ page.page }} / {{ page.totalPage }} 页（共 {{ page.total }} 条）</span>
      <button :disabled="!page.hasNext" @click="go(page.page + 1)">下一页</button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { fetchServiceList } from '@/api/service';
import StatusTag from '@/components/StatusTag.vue';

// 列表查询参数
const q = reactive({ keyword: '', status: '', page: 1, size: 20 });
// 分页数据（使用 adaptPage 统一转换）
const page = reactive({ list: [] as any[], total: 0, totalPage: 0, page: 1, size: 20, hasPrevious: false, hasNext: false });
const list = ref<any[]>([]);

async function load() {
  const data = await fetchServiceList({ ...q });
  Object.assign(page, data);
  list.value = data.list;
}

function onSearch() {
  q.page = 1;
  load();
}

function go(p: number) {
  q.page = p;
  load();
}

onMounted(load);
</script>

<style scoped>
.toolbar { display: flex; gap: 8px; margin-bottom: 12px; }
.table { width: 100%; border-collapse: collapse; }
th, td { border-bottom: 1px solid #eee; padding: 8px; text-align: left; }
.pager { display: flex; gap: 12px; align-items: center; margin-top: 12px; }
</style>

