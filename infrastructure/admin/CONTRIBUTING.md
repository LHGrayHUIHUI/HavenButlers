# 贡献指南

感谢你对 HavenButler Admin Service 的贡献！

## 开发流程

### 1. 环境准备
- Java 17+
- Maven 3.8+
- Docker (可选)

### 2. 代码规范
- 遵循 AGENTS.md 中的编码规范
- 添加中文注释，注释密度 ≥ 30%
- 使用 4 空格缩进

### 3. 提交规范
- feat: 新功能
- fix: 修复问题
- docs: 文档更新
- style: 代码格式调整
- refactor: 代码重构
- test: 测试相关
- chore: 构建工具等

### 4. Pull Request
- 基于最新的 main 分支创建功能分支
- 确保所有测试通过
- 添加必要的测试用例
- 更新相关文档

### 5. 代码审查
- 至少需要一名维护者审查
- 确保符合项目架构规范
- 验证 YAML 配置无重复键

## 本地开发

```bash
# 编译
mvn clean compile

# 运行测试
mvn test

# 本地启动
mvn spring-boot:run
```

## 问题反馈

如有问题，请创建 Issue 并提供：
- 详细的问题描述
- 复现步骤
- 环境信息
- 相关日志

感谢你的贡献！