# Repository Guidelines

## Project Structure & Module Organization
- Maven 模块，打包 `jar`。源码：`src/main/java/com/haven/common/**`；资源：`src/main/resources/**`（含 `application-common.yml`）。
- 测试放 `src/test/java/**`（如无请创建）。通用组件按子包组织：`security/`、`redis/`、`mq/`、`web/`、`utils/` 等。

## Build, Test, and Development Commands
- `mvn clean package -DskipTests`：构建产物到 `target/`。
- `mvn test`：运行单元测试。
- `mvn clean install`：本地安装供其他模块引用。
- 发布到 GitHub Packages：在 `~/.m2/settings.xml` 配置 `server id=github` 凭证后执行 `mvn deploy`。

## Coding Style & Naming Conventions
- Java 17，UTF-8。缩进 4 空格、LF 换行、文件末尾保留空行；优先以仓库根 `.editorconfig` 统一（缺失请补充）。
- 包名小写点分；类 PascalCase；方法/变量 camelCase；常量 UPPER_SNAKE_CASE。
- 注释要求：类/方法必须中文注释，整体≥30% 注释密度；对外 API 需写明用途、参数、异常、并发语义。

## Testing Guidelines
- 测试框架：JUnit 5（来自 `spring-boot-starter-test`）。
- 目录与命名：`src/test/java`，文件以 `*Test.java` 结尾；Mock I/O/外部依赖。
- 覆盖率目标：语句≥70%，核心工具类（如 `JwtUtils`、`RedisUtils`）≥85%。本地运行：`mvn test`。

## Commit & Pull Request Guidelines
- 提交信息：遵循 Conventional Commits（例：`feat: add Redis distributed lock`、`fix: jwt clock skew`）。
- PR 必填：变更说明、关联 issue、测试结果（日志/截图）、影响面与回滚方案。小步提交，避免在同一 PR 混合重构与功能。

## Security & Configuration Tips
- 禁止提交密钥/令牌；`application-common.yml` 仅放非敏感默认值，敏感项以环境变量/密管注入。
- 本模块不含 UI；若上层产生 UI，请遵循 Apple HIG。

## Agent-Specific Instructions
- 本文件对当前目录生效；需差异化时在更深层目录新增最小 delta 的 `AGENTS.md`。
- 不明确先澄清；无法外部检索时向维护者确认；尽量小改动并参考现有代码。

