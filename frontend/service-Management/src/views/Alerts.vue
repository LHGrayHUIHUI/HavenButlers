<template>
  <div class="alerts">
    <el-page-header>
      <template #content>
        <h2>告警管理</h2>
        <p>告警列表、详情处理与规则管理</p>
      </template>
    </el-page-header>

    <el-tabs v-model="activeTab" class="alerts-tabs">
      <!-- 告警列表标签页 -->
      <el-tab-pane label="告警列表" name="list">
        <!-- 搜索筛选栏 -->
        <el-card class="search-card" shadow="never">
          <el-form :model="q" inline @submit.prevent="onSearch">
            <el-form-item label="级别">
              <el-select v-model="q.level" placeholder="选择级别" clearable>
                <el-option label="INFO" value="INFO" />
                <el-option label="WARN" value="WARN" />
                <el-option label="ERROR" value="ERROR" />
                <el-option label="CRITICAL" value="CRITICAL" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="q.status" placeholder="选择状态" clearable>
                <el-option label="开放" value="OPEN" />
                <el-option label="已处理" value="HANDLED" />
                <el-option label="已忽略" value="IGNORED" />
              </el-select>
            </el-form-item>
            <el-form-item label="服务名">
              <el-input v-model="q.serviceName" placeholder="输入服务名" clearable />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="onSearch">查询</el-button>
              <el-button @click="onReset">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 告警表格 -->
        <el-table :data="list" style="width: 100%" @row-click="onRowClick">
          <el-table-column prop="level" label="级别" width="100">
            <template #default="scope">
              <el-tag
                :type="getLevelType(scope.row.level)"
                size="small"
              >
                {{ scope.row.level }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="title" label="标题" min-width="200" />
          <el-table-column prop="serviceName" label="服务名" width="150" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag
                :type="getStatusType(scope.row.status)"
                size="small"
              >
                {{ getStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="时间" width="180" />
          <el-table-column label="操作" width="200">
            <template #default="scope">
              <el-button
                v-if="scope.row.status === 'OPEN'"
                size="small"
                type="success"
                @click.stop="onHandle(scope.row)"
              >
                处理
              </el-button>
              <el-button
                v-if="scope.row.status === 'OPEN'"
                size="small"
                type="warning"
                @click.stop="onIgnore(scope.row)"
              >
                忽略
              </el-button>
              <el-button
                size="small"
                type="primary"
                link
                @click.stop="onViewDetail(scope.row)"
              >
                详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页器 -->
        <el-pagination
          v-model:current-page="page.page"
          v-model:page-size="page.size"
          :total="page.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </el-tab-pane>

      <!-- 告警规则标签页 -->
      <el-tab-pane label="告警规则" name="rules">
        <div class="rules-toolbar">
          <el-button type="primary" @click="onCreateRule">创建规则</el-button>
        </div>

        <!-- 规则列表 -->
        <el-table :data="rules" style="width: 100%">
          <el-table-column prop="name" label="规则名称" min-width="150" />
          <el-table-column prop="description" label="描述" min-width="200" />
          <el-table-column prop="serviceName" label="服务名" width="120" />
          <el-table-column prop="metricName" label="指标" width="120" />
          <el-table-column prop="threshold" label="阈值" width="80" />
          <el-table-column prop="enabled" label="启用" width="80">
            <template #default="scope">
              <el-switch
                v-model="scope.row.enabled"
                @change="onToggleRule(scope.row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240">
            <template #default="scope">
              <el-button size="small" @click="onEditRule(scope.row)">编辑</el-button>
              <el-button size="small" type="warning" @click="onTestRule(scope.row)">测试</el-button>
              <el-button size="small" type="danger" @click="onDeleteRule(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 规则编辑对话框 -->
        <el-dialog v-model="ruleEditVisible" :title="ruleForm.id ? '编辑规则' : '创建规则'" width="560px">
          <el-form :model="ruleForm" label-width="90px">
            <el-form-item label="名称" required>
              <el-input v-model="ruleForm.name" />
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="ruleForm.description" />
            </el-form-item>
            <el-form-item label="服务名" required>
              <el-input v-model="ruleForm.serviceName" />
            </el-form-item>
            <el-form-item label="指标" required>
              <el-input v-model="ruleForm.metricName" />
            </el-form-item>
            <el-form-item label="比较符" required>
              <el-select v-model="ruleForm.operator" style="width: 120px">
                <el-option v-for="op in ['>','>=','<','<=','==','!=']" :key="op" :label="op" :value="op" />
              </el-select>
            </el-form-item>
            <el-form-item label="阈值" required>
              <el-input v-model.number="ruleForm.threshold" type="number" />
            </el-form-item>
            <el-form-item label="级别" required>
              <el-select v-model="ruleForm.level" style="width: 140px">
                <el-option v-for="lv in ['INFO','WARN','ERROR','CRITICAL']" :key="lv" :label="lv" :value="lv" />
              </el-select>
            </el-form-item>
            <el-form-item label="通知类型">
              <el-input v-model="ruleForm.notifyType" placeholder="email|sms|..." />
            </el-form-item>
            <el-form-item label="启用">
              <el-switch v-model="ruleForm.enabled" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="ruleEditVisible=false">取消</el-button>
            <el-button type="primary" @click="onSaveRule">保存</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>
    </el-tabs>

    <!-- 告警详情对话框 -->
    <el-dialog v-model="detailVisible" title="告警详情" width="60%">
      <div v-if="currentAlert">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="级别">
            <el-tag :type="getLevelType(currentAlert.level)">
              {{ currentAlert.level }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentAlert.status)">
              {{ getStatusText(currentAlert.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="服务名">{{ currentAlert.serviceName }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentAlert.createTime }}</el-descriptions-item>
          <el-descriptions-item label="标题" :span="2">{{ currentAlert.title }}</el-descriptions-item>
          <el-descriptions-item label="详情" :span="2">
            <pre>{{ currentAlert.description || '暂无详细信息' }}</pre>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <!-- 处理告警对话框 -->
    <el-dialog v-model="handleVisible" title="处理告警" width="40%">
      <el-form :model="handleForm" label-width="80px">
        <el-form-item label="处理人" required>
          <el-input v-model="handleForm.handler" placeholder="输入处理人名称" />
        </el-form-item>
        <el-form-item label="处理备注">
          <el-input
            v-model="handleForm.remark"
            type="textarea"
            :rows="3"
            placeholder="输入处理说明"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" @click="onConfirmHandle">确认处理</el-button>
      </template>
    </el-dialog>

    <!-- 忽略告警对话框 -->
    <el-dialog v-model="ignoreVisible" title="忽略告警" width="40%">
      <el-form :model="ignoreForm" label-width="80px">
        <el-form-item label="忽略原因">
          <el-input
            v-model="ignoreForm.reason"
            type="textarea"
            :rows="3"
            placeholder="输入忽略原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ignoreVisible = false">取消</el-button>
        <el-button type="warning" @click="onConfirmIgnore">确认忽略</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import {
  fetchAlerts,
  fetchAlertDetail,
  handleAlert,
  ignoreAlert,
  fetchAlertRules,
  createAlertRule,
  updateAlertRule,
  deleteAlertRule,
  testAlertRule,
  enableAlertRule
} from '@/api/service';
import { ElMessage, ElMessageBox } from 'element-plus';

// 标签页状态
const activeTab = ref('list');

// 查询参数
const q = reactive({
  level: '',
  status: '',
  serviceName: '',
  page: 1,
  size: 20
});

// 分页数据
const page = reactive({
  list: [] as any[],
  total: 0,
  totalPage: 0,
  page: 1,
  size: 20,
  hasPrevious: false,
  hasNext: false
});

const list = ref<any[]>([]);
const rules = ref<any[]>([]);
const ruleEditVisible = ref(false);
const ruleForm = reactive<any>({ id: undefined, name: '', description: '', serviceName: '', metricName: '', operator: '>', threshold: 0, level: 'WARN', notifyType: '', enabled: true });

// 对话框状态
const detailVisible = ref(false);
const handleVisible = ref(false);
const ignoreVisible = ref(false);

// 当前操作的告警
const currentAlert = ref<any>(null);

// 表单数据
const handleForm = reactive({
  handler: '',
  remark: ''
});

const ignoreForm = reactive({
  reason: ''
});

// 加载告警列表
async function load() {
  try {
    const result = await fetchAlerts(q);
    Object.assign(page, result);
    list.value = result.list;
  } catch (error) {
    ElMessage.error('加载告警列表失败');
  }
}

// 加载规则列表
async function loadRules() {
  try {
    rules.value = await fetchAlertRules();
  } catch (error) {
    ElMessage.error('加载规则列表失败');
  }
}

// 搜索
async function onSearch() {
  q.page = 1;
  await load();
}

// 重置搜索
async function onReset() {
  Object.assign(q, { level: '', status: '', serviceName: '', page: 1, size: 20 });
  await load();
}

// 分页事件
async function onPageChange(page: number) {
  q.page = page;
  await load();
}

async function onSizeChange(size: number) {
  q.size = size;
  q.page = 1;
  await load();
}

// 行点击事件
function onRowClick(row: any) {
  onViewDetail(row);
}

// 查看详情
async function onViewDetail(alert: any) {
  try {
    currentAlert.value = await fetchAlertDetail(alert.id);
    detailVisible.value = true;
  } catch (error) {
    ElMessage.error('获取告警详情失败');
  }
}

// 处理告警
function onHandle(alert: any) {
  currentAlert.value = alert;
  handleForm.handler = '';
  handleForm.remark = '';
  handleVisible.value = true;
}

// 确认处理
async function onConfirmHandle() {
  if (!handleForm.handler.trim()) {
    ElMessage.warning('请输入处理人');
    return;
  }

  try {
    await handleAlert(currentAlert.value.id, handleForm.handler, handleForm.remark);
    ElMessage.success('告警处理成功');
    handleVisible.value = false;
    await load();
  } catch (error) {
    ElMessage.error('处理告警失败');
  }
}

// 忽略告警
function onIgnore(alert: any) {
  currentAlert.value = alert;
  ignoreForm.reason = '';
  ignoreVisible.value = true;
}

// 确认忽略
async function onConfirmIgnore() {
  try {
    await ignoreAlert(currentAlert.value.id, ignoreForm.reason);
    ElMessage.success('告警已忽略');
    ignoreVisible.value = false;
    await load();
  } catch (error) {
    ElMessage.error('忽略告警失败');
  }
}

// 规则管理方法
function onCreateRule() {
  Object.assign(ruleForm, { id: undefined, name: '', description: '', serviceName: '', metricName: '', operator: '>', threshold: 0, level: 'WARN', notifyType: '', enabled: true });
  ruleEditVisible.value = true;
}

function onEditRule(rule: any) {
  Object.assign(ruleForm, rule);
  ruleEditVisible.value = true;
}

async function onDeleteRule(rule: any) {
  try {
    await ElMessageBox.confirm('确定删除此规则吗？', '确认删除');
    await deleteAlertRule(rule.id);
    ElMessage.success('规则删除成功');
    await loadRules();
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除规则失败');
    }
  }
}

async function onTestRule(rule: any) {
  try {
    await testAlertRule(rule);
    ElMessage.success('规则测试成功');
  } catch (error) {
    ElMessage.error('规则测试失败');
  }
}

async function onToggleRule(rule: any) {
  try {
    await enableAlertRule(rule.id, !!rule.enabled);
    ElMessage.success(`规则已${rule.enabled ? '启用' : '禁用'}`);
  } catch (error) {
    ElMessage.error('操作失败');
  }
}

async function onSaveRule() {
  if (!ruleForm.name || !ruleForm.serviceName || !ruleForm.metricName) {
    ElMessage.warning('请完整填写必填项');
    return;
  }
  try {
    if (ruleForm.id) await updateAlertRule(ruleForm.id, ruleForm);
    else await createAlertRule(ruleForm);
    ElMessage.success('规则已保存');
    ruleEditVisible.value = false;
    await loadRules();
  } catch (error) {
    ElMessage.error('保存规则失败');
  }
}

// 工具方法
function getLevelType(level: string) {
  const typeMap: Record<string, string> = {
    INFO: 'info',
    WARN: 'warning',
    ERROR: 'danger',
    CRITICAL: 'danger'
  };
  return typeMap[level] || 'info';
}

function getStatusType(status: string) {
  const typeMap: Record<string, string> = {
    OPEN: 'danger',
    HANDLED: 'success',
    IGNORED: 'info'
  };
  return typeMap[status] || 'info';
}

function getStatusText(status: string) {
  const textMap: Record<string, string> = {
    OPEN: '开放',
    HANDLED: '已处理',
    IGNORED: '已忽略'
  };
  return textMap[status] || status;
}

onMounted(() => {
  load();
  loadRules();
});
</script>

<style scoped>
.alerts {
  padding: 20px;
}

.alerts-tabs {
  margin-top: 20px;
}

.search-card {
  margin-bottom: 20px;
}

.rules-toolbar {
  margin-bottom: 20px;
}

pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  background: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
}
</style>
