# Repository Guidelines

面向 `infrastructure/admin` 子仓库的贡献者指南。保持精炼、可执行，命令/路径/标识符不翻译。

## Project Structure & Module Organization
- 代码：`src/main/java/com/haven/admin/...`；资源：`src/main/resources/`（含 `application.yml`、`application-docker.yml`）。
- 容器与编排：`Dockerfile`、`Dockerfile.multi-stage`、`docker/`、`docker-compose*.yml`。
- 配置与脚本：`nacos-configs/`、`setup-nacos.sh`；构建产物：`target/`。

## Build, Test, and Development Commands
- 构建：`mvn clean package -DskipTests`（产物在 `target/`）。
- 测试：`mvn test`；单测运行：`mvn -Dtest=ClassNameTest test`。
- 本地运行：`mvn spring-boot:run`（默认端口见配置）。
- Docker 单服务：`docker compose -f docker-compose-build.yml up --build -d`。
- 全栈（含 Nacos/Prometheus）：先 `docker network create haven-network`，再 `docker compose up -d`，最后 `bash setup-nacos.sh` 初始化配置。

## Coding Style & Naming Conventions
- Java 17，缩进 4 空格，UTF-8，行尾 `LF`；建议在仓库根添加 `.editorconfig` 统一基础格式。
- 命名：包 `com.haven.admin`；类 `PascalCase`；方法/变量 `camelCase`；常量 `UPPER_SNAKE_CASE`。
- 注释：公共类/方法提供中文注释，目标≥30% 注释密度（务实、不过度）。

## Testing Guidelines
- 框架：Spring Boot Test（JUnit 5）。目录：`src/test/java`，文件命名 `*Test.java`。
- 覆盖率：当前不强制；优先覆盖控制器、服务、监控与配置加载关键路径。

## Commit & Pull Request Guidelines
- Commit：中文祈使句，必要时前缀 `feat|fix|docs|chore`；示例：`feat: 增加服务健康检查`。关联 Issue 用 `#123`。
- PR：包含变更说明、测试步骤、影响范围、配置变更（含环境变量）、必要截图（若涉及 UI）。

## Security & Configuration Tips
- 配置：使用 `src/main/resources/application.yml` 与 `application-docker.yml`；优先通过环境变量覆盖敏感项。
- Nacos：密钥/账号不入库；用 `nacos-configs/` 与 `setup-nacos.sh` 初始化，同网络 `haven-network` 下联通依赖。
- 机密：通过 Docker secrets 或环境变量注入，禁止硬编码到代码/配置。
- 访问控制：按最小权限配置 `SecurityConfig`/管理端点，仅在需要时放开。

## Agent-Specific Instructions
- 作用域：本文件约束 `infrastructure/admin` 及子目录。
- 交流：默认使用中文（简体）；命令/路径/标识符不翻译；用户要求其它语言时从优。
- 仅约束交流语言，不强制变更代码命名或注释语言。
- 目录级覆盖：子目录如需差异化，仅新增最小 delta 的 AGENTS.md，避免重复/冲突。
- 跨仓扩散：建议模板仓库（含 AGENTS.md、.editorconfig、PR 模板）；或配置 `git config --global init.templateDir ~/.git-template` 并在 `~/.git-template/AGENTS.md` 放置标准文档；可编写 `scripts/sync-agents.sh` 批量同步（含交互确认）。
- PR 阶段约束：使用 `.github/pull_request_template.md` 列表化核对项；CI 加轻量检查，缺少 AGENTS.md 或关键段落即失败。
- UI：若涉及界面与交互，遵循 Apple HIG。
