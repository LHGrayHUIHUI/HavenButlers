import { http } from './http';
import { adaptPage } from '@/utils/page';

// 服务管理 API，依据 docs/管理端-API规范.md

/** 服务列表（分页/筛选） */
export async function fetchServiceList(params: Record<string, any>) {
  const resp = await http.get('/service/list', { params });
  return adaptPage(resp.data);
}

/** 服务详情 */
export async function fetchServiceDetail(serviceName: string) {
  const resp = await http.get(`/service/${encodeURIComponent(serviceName)}`);
  return resp.data;
}

/** 服务健康 */
export async function fetchServiceHealth(serviceName: string) {
  const resp = await http.get(`/service/${encodeURIComponent(serviceName)}/health`);
  return resp.data;
}

/** 服务指标（时间区间） */
export async function fetchServiceMetrics(serviceName: string, params: Record<string, any>) {
  const resp = await http.get(`/service/${encodeURIComponent(serviceName)}/metrics`, { params });
  return resp.data;
}

/** 服务日志（级别/分页） */
export async function fetchServiceLogs(serviceName: string, params: Record<string, any>) {
  const resp = await http.get(`/service/${encodeURIComponent(serviceName)}/logs`, { params });
  return adaptPage<string>(resp.data);
}

/** 健康总览（列表） */
export async function fetchOverview(params?: Record<string, any>) {
  const resp = await http.get('/service/overview', { params });
  return resp.data;
}

// ===== 告警管理 API =====

/** 告警列表（分页/筛选） */
export async function fetchAlerts(params: Record<string, any>) {
  const resp = await http.get('/alert/list', { params });
  return adaptPage(resp.data);
}

/** 告警详情 */
export async function fetchAlertDetail(alertId: string) {
  const resp = await http.get(`/alert/${alertId}`);
  return resp.data;
}

/** 处理告警 */
export async function handleAlert(alertId: string, handler: string, remark?: string) {
  const resp = await http.post(`/alert/${alertId}/handle`, null, {
    params: { handler, remark }
  });
  return resp.data;
}

/** 忽略告警 */
export async function ignoreAlert(alertId: string, reason?: string) {
  const resp = await http.post(`/alert/${alertId}/ignore`, null, {
    params: { reason }
  });
  return resp.data;
}

/** 告警规则列表 */
export async function fetchAlertRules(params?: Record<string, any>) {
  const resp = await http.get('/alert/rules', { params });
  return resp.data;
}

/** 创建告警规则 */
export async function createAlertRule(rule: any) {
  const resp = await http.post('/alert/rule', rule);
  return resp.data;
}

/** 更新告警规则 */
export async function updateAlertRule(ruleId: string, rule: any) {
  const resp = await http.put(`/alert/rule/${ruleId}`, rule);
  return resp.data;
}

/** 删除告警规则 */
export async function deleteAlertRule(ruleId: string) {
  const resp = await http.delete(`/alert/rule/${ruleId}`);
  return resp.data;
}

/** 测试告警规则 */
export async function testAlertRule(rule: any) {
  const resp = await http.post('/alert/rule/test', rule);
  return resp.data;
}

// ===== 环境管理 API =====

/** 获取当前环境 */
export async function fetchCurrentEnv() {
  const resp = await http.get('/environment/current');
  return resp.data;
}

/** 获取可用环境 */
export async function fetchAvailableEnvs() {
  const resp = await http.get('/environment/available');
  return resp.data;
}

/** 切换环境 */
export async function switchEnvironment(environment: string) {
  const resp = await http.post(`/environment/switch/${environment}`);
  return resp.data;
}

/** 刷新配置 */
export async function refreshConfig() {
  const resp = await http.post('/environment/refresh');
  return resp.data;
}

// ===== 服务操作 API =====

/** 重启服务 */
export async function restartService(serviceName: string) {
  const resp = await http.post(`/service/${encodeURIComponent(serviceName)}/restart`);
  return resp.data;
}

/** 停止服务 */
export async function stopService(serviceName: string) {
  const resp = await http.post(`/service/${encodeURIComponent(serviceName)}/stop`);
  return resp.data;
}

/** 启动服务 */
export async function startService(serviceName: string) {
  const resp = await http.post(`/service/${encodeURIComponent(serviceName)}/start`);
  return resp.data;
}

