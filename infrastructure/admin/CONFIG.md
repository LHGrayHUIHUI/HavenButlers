# Admin服务配置说明

## 配置文件架构

Admin服务采用分层配置策略，根据运行环境加载不同的配置：

### 1. application.yml（本地开发环境）
- **用途**：本地开发和测试
- **特点**：包含完整的默认配置，方便本地调试
- **Nacos连接**：默认连接 localhost:8848
- **配置加载**：`optional:nacos` - Nacos不可用时仍可启动

### 2. application-docker.yml（Docker/生产环境）
- **用途**：Docker容器和生产部署
- **特点**：精简配置，依赖Nacos中心化配置
- **Nacos连接**：默认连接 nacos:8848（容器网络）
- **配置加载**：`nacos:` - 强制从Nacos加载配置

## 配置加载优先级

```
1. Nacos配置中心（最高优先级）
   ├── admin-service.yml（服务专属配置）
   ├── havenbutler-common.yml（公共配置）
   └── admin-service-${ENVIRONMENT}.yml（环境特定配置）

2. 本地配置文件
   ├── application-docker.yml（当profile=docker时）
   └── application.yml（默认配置）

3. 环境变量
   └── NACOS_ADDR, NACOS_NAMESPACE等
```

## 使用方式

### 本地开发
```bash
# 直接启动，使用application.yml
mvn spring-boot:run

# 或者
java -jar admin-service.jar
```

### Docker环境
```bash
# 使用docker profile
java -jar admin-service.jar --spring.profiles.active=docker

# 或通过环境变量
export SPRING_PROFILES_ACTIVE=docker
java -jar admin-service.jar
```

### Docker Compose
```yaml
environment:
  - SPRING_PROFILES_ACTIVE=docker
  - NACOS_ADDR=nacos:8848
  - NACOS_NAMESPACE=public
  - ENVIRONMENT=prod
```

## 核心配置项说明

| 配置项 | 本地默认值 | Docker默认值 | 说明 |
|--------|------------|--------------|------|
| server.port | 8888 | 8888 | 服务端口 |
| nacos.server-addr | localhost:8848 | nacos:8848 | Nacos地址 |
| nacos.namespace | public | public | 命名空间 |
| spring.config.import | optional:nacos | nacos: | 配置导入方式 |
| environment | local | docker | 环境标识 |

## 配置最佳实践

1. **敏感信息**：密码、密钥等敏感信息统一存储在Nacos中
2. **环境隔离**：通过namespace和group实现不同环境的配置隔离
3. **动态刷新**：业务配置支持动态刷新，无需重启服务
4. **配置备份**：定期备份Nacos中的配置数据

## 故障排查

### Nacos连接失败
- 本地环境：检查 application.yml 中的 optional 前缀，允许降级启动
- Docker环境：确保 nacos 服务已启动，网络连通

### 配置不生效
1. 检查配置加载顺序
2. 确认 Nacos 中的配置 data-id 和 group
3. 查看启动日志中的配置加载信息