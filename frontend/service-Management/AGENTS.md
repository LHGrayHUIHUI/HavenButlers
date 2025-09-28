# Repository Guidelines

> 语言：中文（简体）。命令/路径/标识符不翻译；给出可执行结论。UI 交互遵循 Apple HIG。

## 项目结构与模块组织
- 文档：`docs/`（API、UI 与部署说明）。
- 前端（Vue3+Vite）建议：`src/`、`src/api/`、`src/stores/`、`src/views/`、`src/components/`、`src/router/`、`vite.config.ts`、`package.json`，构建产物 `dist/`。参考 `docs/管理端-Vue3-开发与部署.md`。

## 构建、测试与本地开发
- 开发：`npm ci && npm run dev`（如未初始化，请按 docs 指南创建工程）。
- 构建：`npm run build` → 产物在 `dist/`；本地预览：`npm run preview`。
- 测试：如使用 Vitest，运行 `npm test`（或 `npm run test:unit`）。未配置请按下节规范补齐。

## 编码风格与命名约定
- 基础格式：在仓库根添加 `.editorconfig`（UTF-8、LF、文件末尾空行、缩进2空格）。
- 代码工具：TypeScript 优先；`eslint`+`prettier`（示例：`npm run lint`、`npm run format`）。
- 命名：变量/函数 `camelCase`；类/组件 `PascalCase`；文件 `kebab-case.vue/ts`；样式变量 `kebab-case`。
- 注释：方法/类需中文注释，整体≥30% 注释密度（务求有用，不注水）。

## 测试指南
- 框架：Vitest + Vue Test Utils。
- 用例放置：与被测文件同层 `*.spec.ts` 或 `__tests__/`；命名与被测文件对应。
- 覆盖：新增/变更行覆盖率≥70%；关键逻辑（认证、分页、降级）必须有用例。

## API 契约与兼容性（重要）
- 分页返回必须保持旧契约，字段：`{ list, total, totalPage, page, size, hasPrevious, hasNext }`。现有页面依赖 `data.list`（如告警列表、服务日志）。
- 若后端输出 `{ content, totalPages, ... }`，需在后端用 `@JsonProperty` 保持别名，或在前端统一适配：
  ```ts
  export const adaptPage = (p:any)=>({
    list: p.list ?? p.content ?? [],
    total: p.total ?? p.totalElements,
    totalPage: p.totalPage ?? p.totalPages,
    page: p.page ?? p.number,
    size: p.size,
    hasPrevious: p.hasPrevious ?? (p.number>1),
    hasNext: p.hasNext ?? (p.number < (p.totalPages||p.totalPage))
  });
  ```
- 任何破坏契约的变更为 P0，请先兼容发布，再清理调用侧。

## 提交与 Pull Request
- 提交信息：Conventional Commits（如 `feat:`, `fix:`, `chore:`）。
- PR 要求：描述变更与动机、关联 issue、影响范围（含 API 契约/UI）、必要截图/录屏；通过 `lint`/`test` 后再提交。

## 模板与扩散（可选）
- 模板仓库：新仓统一从模板创建（含 `AGENTS.md`、`.editorconfig`、PR 模板）。
- git init 模板：`git config --global init.templateDir ~/.git-template` 并放置标准 `AGENTS.md`。
- 同步脚本：`scripts/sync-agents.sh` 交互式推送标准文件；CI 增加轻量检查，缺少关键段落即失败。

