# 开发面板（HavenButler Admin）

> 依据 `docs/` 文档拆分的任务看板。优先级：P0 最高。使用复选框标记进度。

## 快速命令（本地）
- 开发：`npm ci && npm run dev`
- 构建：`npm run build`（产物在 `dist/`）
- 预览：`npm run preview`
- 质量：`npm run lint` / `npm run format`

## 里程碑与任务
### M1 基础与脚手架（P0）
- [x] 初始化前端工程（Vite+Vue3+TS+Pinia+Router+Axios）。
- [x] 引入 Element Plus（组件与主题，全局导入）。
- [x] 配置 `eslint`/`prettier`、新增脚本：`lint`/`format`。
- [x] 根添加 `.editorconfig`（UTF-8、LF、缩进2空格、末尾空行）。
- [x] Axios 实例与拦截器（注入 `Authorization: Basic ...`，401 统一退出）。
- [x] 路由与登录守卫（无凭据跳转 `/login`）。

### M2 核心页面与数据流（P0）
- [x] Dashboard：SSE `/api/service/stream/health` 实时 + 失败回退 5s 轮询；使用 Element Plus 表格和卡片布局。
- [x] Services 列表：筛选/搜索/分页；使用分页适配器 `adaptPage`。
- [x] Service Detail：Tabs【Health/Metrics/Logs/Nacos】；指标(ECharts)与 Nacos 实例管理；（待）日志本地分页优化。
- [x] Alerts：完成列表/详情/处理/忽略 + 规则 CRUD/测试。

### M3 环境与设置（P1）
- [x] Environment：当前/可用环境、刷新配置、环境切换（二次确认）。
- [x] Settings：刷新间隔、色弱模式；持久化至 localStorage。

### M4 API 契约与兼容（P0）
- [x]（前端）统一分页适配：`adaptPage` 输出旧契约字段。
- [x]（前端）扩展 API 接口：告警管理、环境管理、服务操作等完整 API。
- [ ]（后端）分页 DTO 别名：`@JsonProperty` 兼容 `{ content,totalPages }`。
- [x] 统一错误包装：`AdminResponse<T>` 拦截处理与 401 退出。

### M5 可访问性与 HIG（P1）
- [x] 语义色与状态标签：UP/DEGRADED/DOWN（`StatusTag`）。
- [ ] 键盘可达、焦点可见、ARIA 标签全量核查。
- [ ] 微文案与空态/失败反馈一致，遵循 Apple HIG。

### M6 部署与运维（P1）
- [x] Docker 多阶段镜像（Node 构建 + Nginx 运行）。
- [x] Nginx 反代同源：`/api` → `http://admin-service:8888`。
- [ ]（可选）Compose 片段与健康检查。

### M7 质量保障（P1）
- [ ] 测试：Vitest + Vue Test Utils，覆盖率≥70%。
- [ ] 关键场景用例：认证失败回退、SSE→轮询降级、分页适配器正确性。
- [ ] 性能：Dashboard 首屏 TTI ≤1.5s（内网）、列表 100 行渲染 ≤200ms。

## 验收标准（示例）
- Services 页可稳定展示 `data.list`，前后端任何分页 DTO 变动不影响 UI。
- 断网或 401 情况下提示清晰并可恢复；风险操作均有二次确认。

## 下阶段 Backlog（建议）
- 日志本地分页优化（Service Detail / Logs）。
- CI：轻量检查 AGENTS.md 与分页契约关键字段；PR 模板与检查清单。
- 可访问性核查（焦点环、ARIA 标签、空态/失败反馈统一）。

## 2025-09-28 更新记录
### 已完成功能
- ✅ Element Plus 全局导入和配置
- ✅ Dashboard 使用 Element Plus 重构（表格、卡片、状态标签）
- ✅ Alerts 页面大幅增强：
  - 标签页结构（告警列表 + 规则管理）
  - 完整的告警处理/忽略流程
  - 告警详情对话框
  - 规则列表和基础操作
- ✅ API 接口扩展：告警管理、环境管理、服务操作等完整 API
- ✅ SSE 连接状态显示（实时连接/轮询模式）

### 待优化功能
- 日志本地分页（避免长列表滚动卡顿）
- Environment 和 Settings 页面 Element Plus 深度整合（表单校验/对话框）
- 主题系统和深色模式支持

## 参考
- `docs/管理端-Vue3-开发与部署.md`
- `docs/管理端-API规范.md`
- `docs/项目概述与UI架构提示.md`
