# 管理端（Vue3）开发与部署指南（Admin Portal）

> 目标：在 Docker 内网架构下，基于 Vue3 构建“运维管理面板”，对接 admin-service 提供的接口（Basic 认证 + SSE），并通过 Nginx 反向代理同源部署，避免 CORS。

## 技术栈与规范
- 技术：Vue3 + TypeScript + Vite + Pinia + Vue Router + Axios + Element Plus + ECharts
- 架构：同源部署（Nginx 静态 + 反向代理 `/api` → `http://admin-service:8888`）
- 认证：HTTP Basic（UI 登录页收集用户名/密码，运行期以 `Authorization: Basic base64(user:pass)` 发送；不持久化明文）
- SSE：优先 `EventSource('/api/service/stream/health')`；若 401/失败则回退为 5s 轮询 `/api/service/overview`
- UI 规范：遵循 Apple HIG（语义色、可访问性、键盘可达、字号/对比度/留白）

## 页面与路由
- Dashboard（/）：服务健康总览（UP/DEGRADED/DOWN）、实例数、请求速率、错误率、CPU/内存均值（SSE 实时/轮询）
- Services（/services）：服务列表、过滤与搜索；跳转详情
- Service Detail（/services/:name）：健康、指标（跨实例聚合）、日志分页（按级别过滤）、Nacos 实例与元数据
- Alerts（/alerts）：告警列表、详情、处理/忽略、统计、规则 CRUD 与测试
- Environment（/env）：当前环境、可用环境、刷新配置、切换（仅管理员）
- Settings（/settings）：管理端偏好（刷新间隔、主题/色弱模式）、只读

## API 约定
- Base URL：同源 `/api`（Nginx 代理到 `http://admin-service:8888`）
- 认证：`Authorization: Basic base64(user:pass)`（不推荐持久化密码；可存 `Basic token` 于 sessionStorage）
- 返回与错误：后端统一 `AdminResponse<T>` 结构；前端拦截器统一处理错误码/提示与降级（详见 `docs/管理端-API规范.md`）

## 代码结构建议
```
admin-portal/
├─ src/
│  ├─ api/            # Axios 实例、API 模块（service/env/alert/health）
│  ├─ stores/         # Pinia（auth、settings、overview）
│  ├─ views/          # 页面组件（Dashboard/Services/ServiceDetail/Alerts/Env）
│  ├─ components/     # 复用组件（StatusTag/MetricCard/LogViewer）
│  ├─ router/         # 路由（鉴权守卫：未登录跳转 /login）
│  ├─ utils/          # 工具（format、time、sse/poll 开关）
│  └─ main.ts
├─ index.html
├─ vite.config.ts
└─ package.json
```

## 环境变量（Vite）
- `VITE_API_BASE_URL`：默认 `/api`
- `VITE_SSE_PATH`：默认 `/api/service/stream/health`
- `VITE_REFRESH_INTERVAL`：默认 `5000`（ms，轮询间隔）

## 认证实现要点
- 登录表单（/login）：输入 user/password，生成 `Basic ${btoa(`${user}:${password}`)}` 存入 Pinia（sessionStorage 持久化 Basic 串，关闭浏览器失效）
- Axios 拦截器：在请求头自动注入 `Authorization`；401 时清除凭据并跳转登录
- SSE：`EventSource` 无法自定义 header；同源 + 浏览器缓存 Basic 可自动携带；若收到 401/异常，自动降级为轮询

## Nginx 部署（建议）
- Dockerfile（多阶段）
```
# build
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# runtime
FROM nginx:1.25-alpine
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY dist/ /usr/share/nginx/html/
EXPOSE 8080
HEALTHCHECK CMD wget -qO- http://localhost:8080/ || exit 1
```
- nginx.conf（同源代理）
```
server {
  listen 8080;
  server_name _;
  root /usr/share/nginx/html;
  index index.html;

  location /api/ {
    proxy_pass http://admin-service:8888/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
  }

  location / {
    try_files $uri $uri/ /index.html;
  }
}
```

## Docker Compose 片段（与 admin 内网互通）
```
services:
  admin-portal:
    build: ./admin-portal
    container_name: haven-admin-portal
    networks: [haven-network]
    ports:
      - "8081:8080"  # 可选对外（生产建议走 Gateway 同域）
    depends_on:
      - admin-service
```

## 开发/构建
- 开发：`npm ci && npm run dev`（设置 `VITE_API_BASE_URL=http://localhost:8888/api` 直连后端）
- 构建：`npm run build` → `dist/`
- 本地容器：`docker build -t haven/admin-portal:dev . && docker run --rm -p 8081:8080 --network haven-network haven/admin-portal:dev`

## 可访问性与 HIG 要点
- 颜色语义：UP=绿色、DEGRADED=黄色、DOWN=红色；色弱模式加字形/图标冗余
- 键盘导航：焦点可见、Tab 顺序合理、ARIA 标签
- 反馈：加载骨架、空态描述、失败可重试；重要操作（重启/下线）二次确认

---

参考：docs/Nacos-统一配置与服务发现设计.md、docs/Storage-TCP代理模式设计.md、docs/StorageClient-统一规范(TCP代理模式).md
