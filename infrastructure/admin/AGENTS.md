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

## Agent-Specific Instructions
- 作用域：本文件约束 `infrastructure/admin` 及子目录。
- 交流：默认使用中文（简体）；命令/路径/标识符不翻译；用户要求其它语言时从优。
- 仅约束交流语言，不强制变更代码命名或注释语言。
