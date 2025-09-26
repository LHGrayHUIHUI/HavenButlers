# Repository Guidelines

> 面向贡献者与自动化代理的简明规范（本仓库优先，必要时最小化目录级覆盖）。

## 项目结构与模块组织
- 类型：Java 17 / Spring Boot 3 工具库（非可执行应用）。
- 代码：`src/main/java/com/haven/base/**`
- 配置：`src/main/resources/`（示例：`application-base.yml`）；自动装配见 `META-INF/spring.factories`。
- 测试：`src/test/java/**`（如缺失请新建）。

## 构建、测试与开发命令
- 构建发布：`mvn -q clean install`（生成 jar + sources + javadoc）。
- 仅打包（跳过测试）：`mvn -q -DskipTests package`。
- 运行测试：`mvn -q test`；集成测试可用 `@SpringBootTest`。
- 发布到 GitHub Packages：`mvn -q deploy`（需在 `~/.m2/settings.xml` 配置 `server id=github`）。

## 编码风格与命名约定
- 基础：UTF-8，缩进4空格，LF，文件末尾保留换行；统一由根 `.editorconfig` 约束（若缺失请提 PR）。
- 命名：包 `com.haven.base.*`；类 `PascalCase`；方法/变量 `camelCase`；常量 `UPPER_SNAKE_CASE`。
- 注释：类/方法必须含中文注释，整体≥30% 注释密度（务实，不注水）。
- 依赖：优先使用 Lombok（如 `@Slf4j`、`@Data`），日志经 `slf4j`。

## 测试规范
- 框架：JUnit 5（随 `spring-boot-starter-test`）。
- 组织：`src/test/java/**`；文件名 `*Test.java`；单元优先、必要时集成。
- 覆盖：建议总体 ≥70%，对 `utils/exception/aspect` 等核心模块优先保证。

## 提交与 Pull Request
- 观察到历史提交较随意，建议采用 Conventional Commits：如 `feat: 新增分布式锁实现`、`fix: 修复 TraceId 丢失`。
- PR 要求：
  - 清晰描述、关联 issue、必要的配置/日志/截图；
  - 通过构建与测试；涉及接口/行为变更请在 `README.md` 追加说明。
- 建议新增 `.github/pull_request_template.md` 与轻量 CI：缺失 `AGENTS.md` 或关键段落即失败。

## 额外说明（安全/配置/Agent）
- 禁止提交密钥；敏感配置走环境变量，默认项参考 `application-base.yml`。
- 若涉及 UI/交互，遵循 Apple HIG（本库通常无 UI）。
- 目录差异化请在更深层追加最小 delta 的 `AGENTS.md`；跨仓统一建议：模板仓库 + `git init` template + `scripts/sync-agents.sh`。
- 变更遵循小改动；不明确先澄清；涉及外部检索与依赖请先征求确认。
