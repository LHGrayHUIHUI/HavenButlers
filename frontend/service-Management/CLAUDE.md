# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是HavenButler智能家庭服务平台的管理端前端项目（Admin Portal），用于微服务运维管理。项目采用Vue3 + TypeScript + Vite技术栈，通过同源部署方式对接后端admin-service接口。

## 常用命令

### 开发环境
```bash
# 安装依赖
npm ci

# 启动开发服务器（默认代理到localhost:8888）
npm run dev

# 直连后端（跳过代理）
VITE_API_BASE_URL=http://localhost:8888/api npm run dev
```

### 构建与预览
```bash
# 构建生产版本
npm run build

# 本地预览构建产物
npm run preview
```

### 代码质量
```bash
# ESLint检查
npm run lint

# Prettier格式化
npm run format
```

## 技术架构

### 核心技术栈
- **前端框架**：Vue3 + Composition API
- **类型系统**：TypeScript
- **构建工具**：Vite
- **状态管理**：Pinia
- **路由管理**：Vue Router 4
- **HTTP客户端**：Axios（统一拦截器）
- **UI组件库**：Element Plus（规划中）
- **图表库**：ECharts（规划中）

### 部署架构
- **同源部署**：Nginx静态托管 + 反向代理`/api` → `http://admin-service:8888`
- **认证方式**：HTTP Basic Authentication
- **实时通信**：Server-Sent Events (SSE) + 降级轮询

### 目录结构
```
src/
├── api/           # HTTP客户端配置和API模块
├── stores/        # Pinia状态管理
├── views/         # 页面组件
├── components/    # 复用组件
├── router/        # 路由配置
├── utils/         # 工具函数
└── main.ts        # 应用入口
```

## 开发约定

### API契约要求（重要）
- **统一返回格式**：后端使用`AdminResponse<T>`包装，包含`{success, code, message, data, traceId, timestamp}`
- **分页兼容性**：必须保持旧分页字段`{list, total, totalPage, page, size, hasPrevious, hasNext}`
- **认证头部**：自动注入`Authorization: Basic base64(user:pass)`

### 路由配置
- **路由模式**：Hash模式（`createWebHashHistory`）
- **登录守卫**：所有路由除`/login`外均需要认证
- **页面路由**：
  - `/` - Dashboard总览
  - `/services` - 服务列表
  - `/services/:name` - 服务详情
  - `/alerts` - 告警管理
  - `/env` - 环境管理
  - `/settings` - 设置页面
  - `/login` - 登录页面

### 状态管理
- **认证状态**：`stores/auth.ts`管理Basic Token和登录状态
- **会话持久化**：使用sessionStorage存储认证信息
- **自动清理**：401响应时自动清除凭据并跳转登录

### 实时数据
- **优先方案**：SSE (`/api/service/stream/health`)
- **降级方案**：5秒轮询 (`/api/service/overview`)
- **错误处理**：SSE连接失败自动切换到轮询模式

## 环境变量配置

### Vite环境变量
```bash
# API基础URL（默认：/api）
VITE_API_BASE_URL=/api

# SSE端点路径（默认：/api/service/stream/health）
VITE_SSE_PATH=/api/service/stream/health

# 轮询刷新间隔（默认：5000ms）
VITE_REFRESH_INTERVAL=5000
```

### 代理配置
开发环境中，Vite自动代理`/api`到`http://localhost:8888`，生产环境通过Nginx反向代理实现同源部署。

## 编码规范

### 样式约定
- **变量命名**：camelCase
- **组件命名**：PascalCase
- **文件命名**：kebab-case.vue/ts
- **缩进**：2空格
- **注释密度**：≥30%，使用中文注释

### 组件组织
- **页面组件**：放在`views/`目录
- **复用组件**：放在`components/`目录
- **工具函数**：放在`utils/`目录，按功能分模块

### 类型定义
- **API响应**：使用`AdminResponse<T>`接口
- **分页数据**：保持向后兼容的字段名
- **组件Props**：明确定义TypeScript接口

## 故障排查

### 常见问题
1. **认证失败**：检查Basic Token格式和后端服务状态
2. **SSE连接异常**：查看是否自动降级到轮询模式
3. **代理失败**：确认Vite代理配置和后端服务地址
4. **构建失败**：检查TypeScript类型错误和ESLint规则

### 调试技巧
- 使用浏览器开发者工具查看Network面板的API请求
- 检查Console面板的认证和路由相关错误
- 确认环境变量是否正确设置

## 测试要求

### 测试框架
- **单元测试**：Vitest + Vue Test Utils（规划中）
- **覆盖率要求**：新增/变更代码≥70%
- **关键测试点**：认证逻辑、分页处理、降级机制

### 测试命令
```bash
# 运行单元测试（待配置）
npm test

# 运行集成测试（待配置）
npm run test:integration
```