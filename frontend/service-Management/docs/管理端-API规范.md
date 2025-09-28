# 管理端 API 规范（对接 admin-service）

> 认证：HTTP Basic（`Authorization: Basic base64(user:pass)`）；返回统一 `AdminResponse<T>`。
> Base URL：`/api`（建议由 Nginx 反代同源 → `http://admin-service:8888`）。

## 通用返回结构（AdminResponse）
```
{
  "success": true,
  "code": 0,
  "message": "操作成功",
  "data": { ... },
  "traceId": "tr-...",
  "timestamp": 1700000000000
}
```

## 1. 服务管理
- 列表：GET `/api/service/list`
- 详情：GET `/api/service/{serviceName}`
- 健康：GET `/api/service/{serviceName}/health`
- 指标：GET `/api/service/{serviceName}/metrics?startTime&endTime`
- 重启：POST `/api/service/{serviceName}/restart`
- 停止：POST `/api/service/{serviceName}/stop`
- 启动：POST `/api/service/{serviceName}/start`
- 日志：GET `/api/service/{serviceName}/logs?level=INFO&page=1&size=100`
- 依赖：GET `/api/service/dependencies`
- 执行健康检查：POST `/api/service/health-check`

示例-服务健康：
```
GET /api/service/gateway-service/health ->
{
  "success": true,
  "data": {
    "serviceName":"gateway-service",
    "status":"UP",
    "totalInstances":2,
    "healthyInstances":2,
    "instances":[{"instanceId":"...","host":"...","port":...}]
  }
}
```

## 2. 健康总览 / 实时（SSE）
- 总览：GET `/api/service/overview?status=UP&search=keyword`
- 单项：GET `/api/service/overview/{serviceName}`
- 实时流：GET `/api/service/stream/health`（text/event-stream，每 5s 推送）
- 流统计：GET `/api/service/stream/stats`

前端（SSE）示例：
```js
let es; function connect(){
  es = new EventSource('/api/service/stream/health');
  es.onmessage = (e)=>{ const list = JSON.parse(e.data); /* set state */ };
  es.onerror = ()=>{ es.close(); fallbackPolling(); };
}
```

## 3. 环境管理
- 当前环境：GET `/api/environment/current`
- 可用环境：GET `/api/environment/available`
- 切换环境：POST `/api/environment/switch/{environment}`
- 刷新配置：POST `/api/environment/refresh`
- 当前配置：GET `/api/environment/config`

## 4. 告警管理
- 列表：GET `/api/alert/list?serviceName&level&status&startTime&endTime&page&size`
- 详情：GET `/api/alert/{alertId}`
- 处理：POST `/api/alert/{alertId}/handle?handler&remark`
- 忽略：POST `/api/alert/{alertId}/ignore?reason`
- 批量处理：POST `/api/alert/batch/handle`（Body：`[alertId,...]` + `handler, remark`）
- 规则列表：GET `/api/alert/rules?serviceName&enabled`
- 创建规则：POST `/api/alert/rule`（Body：AlertRule）
- 更新规则：PUT `/api/alert/rule/{ruleId}`（Body：AlertRule）
- 删除规则：DELETE `/api/alert/rule/{ruleId}`
- 启用/禁用规则：PUT `/api/alert/rule/{ruleId}/enable?enabled=true|false`
- 统计：GET `/api/alert/statistics?startTime&endTime`
- 测试规则：POST `/api/alert/rule/test`（Body：AlertRule）

`AlertRule` 关键字段：`name, description, serviceName, metricName, operator, threshold, level, notifyType`

## 5. Nacos 辅助
- 服务名列表：GET `/api/service/nacos/services`
- 实例列表：GET `/api/service/nacos/{serviceName}/instances`
- 服务详情：GET `/api/service/nacos/{serviceName}/details`
- 服务健康：GET `/api/service/nacos/{serviceName}/health`
- 系统健康：GET `/api/service/nacos/system/health`
- 临时下线：POST `/api/service/nacos/{serviceName}/deregister?ip&port`
- 重新上线：POST `/api/service/nacos/{serviceName}/register?ip&port`

## 6. 管理信息
- 系统健康：GET `/api/admin/health`
- 系统指标：GET `/api/admin/metrics`
- 服务状态：GET `/api/admin/services`

## 7. 认证与错误
- 认证：所有 `/api/**` 需要 Basic；浏览器端在登录后以 `Authorization` 头传递；后端可同时支持表单登录用于 SBA 页面
- 401：`{"code":20000, "message":"未授权访问，请提供有效令牌|认证失败"}` → 前端清除凭据并跳转 `/login`
- 403：权限不足（若后续细分角色）；500：后端异常

## 8. 频率与退避
- SSE：开发环境启用，生产建议配合降级与速率限制
- 轮询：默认 5s；失败指数退避（5→10→20s，封顶 60s）

## 9. 变更兼容
- 后续若调整到 JWT：建议保留 Basic 作为后备；或在 Gateway 层做统一 JWT→Basic 翻译，前端仅关注登录与 Token 刷新

---

参考：src/main/java/com/haven/admin/controller/*.java、README.md:📡 API 接口
