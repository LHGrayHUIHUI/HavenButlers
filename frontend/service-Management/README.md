# HavenButler Admin（Service Management）

一站式微服务运维管理面板（Admin Portal）。聚合服务健康、实时监控（SSE/轮询）、日志查看、告警与环境/配置管理，面向内网同源部署与安全运行。

## 功能与页面
- Dashboard：全局健康总览、关键指标，SSE 实时更新（失败回退 5s 轮询）。
- Services：服务列表、搜索/筛选；服务详情含 Health/Metrics/Logs/Nacos。
- Alerts：告警列表与处理，规则 CRUD 与测试。
- Environment：当前/可用环境、刷新配置、环境切换（需二次确认）。
- Settings：主题、色弱/高对比、刷新间隔等偏好。

## 技术栈与目录
- 技术：Vue3 + TypeScript + Vite + Pinia + Vue Router + Axios + Element Plus + ECharts。
- 同源：Nginx 静态托管 + 反代 `/api` → `http://admin-service:8888`，认证使用 HTTP Basic。
- 结构（建议）：
```
src/
  api/ stores/ views/ components/ router/ utils/
index.html  vite.config.ts  package.json  dist/
```
更多见：`docs/管理端-Vue3-开发与部署.md`、`docs/项目概述与UI架构提示.md`。

## 快速开始
- 开发：`npm ci && npm run dev`（或 `VITE_API_BASE_URL=http://localhost:8888/api npm run dev` 直连后端）
- 构建：`npm run build`（产物在 `dist/`）；预览：`npm run preview`
- 若仓库尚未初始化前端工程，请按 `docs/管理端-Vue3-开发与部署.md` 脚本化创建。

## 环境变量（Vite）
- `VITE_API_BASE_URL`（默认 `/api`）
- `VITE_SSE_PATH`（默认 `/api/service/stream/health`）
- `VITE_REFRESH_INTERVAL`（默认 `5000` ms）

## API 契约要点（P0）
- 统一返回：`AdminResponse<T>`，含 `success|code|message|data|traceId|timestamp`，见 `docs/管理端-API规范.md`。
- 分页响应需保留旧字段：`{ list, total, totalPage, page, size, hasPrevious, hasNext }`。
  - 若后端返回 `{ content, totalPages, ... }`，需后端 `@JsonProperty` 兼容或前端统一适配，避免打破 `data.list` 依赖。

## 部署（Nginx 同源）
```
location /api/ {
  proxy_pass http://admin-service:8888/;
  proxy_set_header Host $host;
}
location / { try_files $uri $uri/ /index.html; }
```
- Docker：多阶段构建见 `docs/管理端-Vue3-开发与部署.md`；示例镜像监听 `8080`。

## 可访问性与 HIG
- 语义色：UP=绿色、DEGRADED=黄色、DOWN=红色；色弱模式加字形/图标冗余。
- 键盘可达与 ARIA 标签；加载/空态/失败均提供清晰反馈与重试。

## 贡献
- 请先阅读 `AGENTS.md`（编码风格、注释密度、PR 要求与分页兼容）。
- 截图示例：`docs/dashboard_overview/screen.png`

---

若需我初始化脚手架（Vite+TS+ESLint+Prettier）或添加 `.editorconfig`/PR 模板，请在 Issue/PR 中指明需求。
