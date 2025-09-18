# HavenButler Admin服务 - IDEA Docker 配置指南

## 项目概述

**项目名称**: HavenButler Admin管理服务
**项目路径**: `/Users/yjlh/Documents/code/HavenButler/infrastructure/admin`
**服务端口**: `8888`
**访问地址**: `http://localhost:8888`
**登录信息**: `admin / havenbutler2025`

## 必需的配置参数

### 🔧 IDEA Docker运行配置参数

#### Docker-Compose配置（推荐）
```
配置名称: Admin Service Docker
配置类型: Docker → Docker-compose
Compose文件: ./docker-compose.yml
服务名称: admin-service
工作目录: /Users/yjlh/Documents/code/HavenButler/infrastructure/admin
```

#### Dockerfile配置（备选）
```
配置名称: Build Admin Service
配置类型: Docker → Dockerfile
Dockerfile路径: ./Dockerfile
构建上下文: ./
镜像标签: admin-service:latest
构建选项: --no-cache
```

### 🌐 Docker网络配置
```bash
# 必须先创建网络（一次性操作）
docker network create haven-network
```

### 📋 环境变量配置

**在docker-compose.yml中已配置的环境变量：**
```yaml
environment:
  - SPRING_PROFILES_ACTIVE=docker                    # 激活Docker配置文件
  - SPRING_APPLICATION_NAME=admin-service            # 应用名称
  - SERVER_PORT=8888                                 # 服务端口
  - SPRING_SECURITY_USER_NAME=admin                  # 管理员用户名
  - SPRING_SECURITY_USER_PASSWORD=havenbutler2025    # 管理员密码
  - SPRING_SECURITY_USER_ROLES=ADMIN                 # 用户角色
  - TZ=Asia/Shanghai                                  # 时区设置
  - LOGGING_LEVEL_ROOT=INFO                          # 日志级别
  - LOGGING_LEVEL_COM_HAVEN=DEBUG                    # 项目日志级别
```

## 详细配置步骤

### 第一步：IDEA基础配置

1. **启用Docker插件**
   - `File` → `Settings` → `Plugins`
   - 确保 "Docker" 插件已启用

2. **配置Docker连接**
   - `File` → `Settings` → `Build, Execution, Deployment` → `Docker`
   - 添加连接: `unix:///var/run/docker.sock` (macOS)
   - 测试连接成功

### 第二步：创建Docker运行配置

1. **打开运行配置**
   - 点击右上角运行配置下拉框
   - 选择 `Edit Configurations...`

2. **添加Docker-Compose配置**
   - 点击 `+` → `Docker` → `Docker-compose`
   - 具体参数配置：

| 参数 | 值 | 说明 |
|------|----|----- |
| Name | `Admin Service Docker` | 配置名称 |
| Compose file(s) | `./docker-compose.yml` | compose文件路径 |
| Services | `admin-service` | 指定启动的服务 |
| Project name | `admin` | 项目名称（可选） |

### 第三步：项目构建准备

**必须在运行前执行的命令：**
```bash
# 切换到admin项目目录
cd /Users/yjlh/Documents/code/HavenButler/infrastructure/admin

# 构建jar包（每次代码修改后都需要执行）
mvn clean package

# 创建Docker网络（一次性操作）
docker network create haven-network
```

### 第四步：验证配置文件

**确保以下文件存在且配置正确：**

1. **Dockerfile** (`./Dockerfile`)
```dockerfile
# 简单的Spring Boot应用Dockerfile
FROM openjdk:17-jre-slim

# 复制jar文件
COPY target/admin-service-*.jar app.jar

# 暴露端口
EXPOSE 8888

# 启动命令：激活docker环境，加载application-docker.yml
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]
```

2. **Docker-Compose** (`./docker-compose.yml`)
   - 确保 `image: admin-service:latest`
   - 确保 `ports: - "8888:8888"`
   - 确保 `networks: - haven-network`

3. **Spring配置** (`./src/main/resources/application-docker.yml`)
   - 确保 `server.port: 8888`
   - 确保Spring Boot Admin配置正确

## 运行流程

### 🚀 一键运行步骤

1. **代码修改后构建**
```bash
cd /Users/yjlh/Documents/code/HavenButler/infrastructure/admin
mvn clean package
```

2. **IDEA中运行**
   - 选择运行配置: `Admin Service Docker`
   - 点击绿色运行按钮 ▶️
   - 等待Docker构建和启动完成

3. **验证运行成功**
   - 浏览器访问: `http://localhost:8888`
   - 登录: `admin / havenbutler2025`
   - 健康检查: `curl http://localhost:8888/actuator/health`

### 📊 监控和调试

**在IDEA中查看：**
- **Docker工具窗口**: `View` → `Tool Windows` → `Docker`
- **运行日志**: IDEA底部的 `Run` 或 `Debug` 窗口
- **容器状态**: Docker工具窗口中的容器列表

## 常见问题排查

### ❌ 问题1：构建失败
**现象**: `No such file: target/admin-service-*.jar`
```bash
# 解决方案
cd /Users/yjlh/Documents/code/HavenButler/infrastructure/admin
mvn clean package
ls -la target/admin-service-*.jar  # 验证jar文件存在
```

### ❌ 问题2：端口冲突
**现象**: `Port 8888 is already in use`
```bash
# 查看端口占用
lsof -i :8888
# 停止占用端口的进程或修改端口映射
```

### ❌ 问题3：网络连接失败
**现象**: `network haven-network not found`
```bash
# 创建网络
docker network create haven-network
# 验证网络存在
docker network ls | grep haven
```

### ❌ 问题4：Docker连接失败
**现象**: IDEA无法连接Docker
- 确认Docker Desktop正在运行
- 重启Docker Desktop
- 重新配置IDEA Docker连接

## 🔧 高级配置

### 远程调试配置
如需在容器中进行远程调试：

1. **修改Dockerfile**
```dockerfile
ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "-jar", "app.jar", "--spring.profiles.active=docker"]
```

2. **修改docker-compose.yml**
```yaml
ports:
  - "8888:8888"
  - "5005:5005"  # 调试端口
```

3. **IDEA创建Remote Debug配置**
   - `Run` → `Edit Configurations` → `+` → `Remote JVM Debug`
   - Host: `localhost`, Port: `5005`

### 热重载配置
为提高开发效率：

```yaml
# docker-compose.yml中添加
volumes:
  - ./target:/app/target
  - admin-logs:/app/logs
```

## 总结检查清单

✅ **运行前检查清单：**
- [ ] Docker Desktop正在运行
- [ ] IDEA Docker插件已启用并连接成功
- [ ] 已创建haven-network网络
- [ ] 已执行 `mvn clean package` 构建jar包
- [ ] Docker运行配置已正确设置
- [ ] 端口8888未被占用

✅ **运行后验证清单：**
- [ ] IDEA显示容器运行中
- [ ] 可以访问 `http://localhost:8888`
- [ ] 可以用 `admin/havenbutler2025` 登录
- [ ] 健康检查接口正常: `/actuator/health`

通过以上配置，您就可以在IDEA中一键运行Admin服务到Docker容器中了！