#!/bin/bash

# Nacos配置初始化脚本
# 用于初始化HavenButler Admin服务的配置中心

# 配置参数
NACOS_ADDR="${NACOS_ADDR:-http://localhost:8848}"
NACOS_NAMESPACE="${NACOS_NAMESPACE:-public}"
NACOS_GROUP="${NACOS_GROUP:-DEFAULT_GROUP}"
MAX_WAIT_TIME=60  # 最大等待时间（秒）

echo "======================================"
echo "  HavenButler Admin 配置初始化工具"
echo "======================================"
echo ""
echo "📍 Nacos地址: $NACOS_ADDR"
echo "📦 命名空间: $NACOS_NAMESPACE"
echo "👥 配置组: $NACOS_GROUP"
echo ""

# 等待Nacos启动
echo "⏳ 检查Nacos服务状态..."
wait_count=0
while ! curl -s "$NACOS_ADDR/nacos/v1/console/health" > /dev/null; do
    if [ $wait_count -gt $MAX_WAIT_TIME ]; then
        echo "❌ 等待超时！Nacos服务未响应"
        exit 1
    fi
    echo "   等待Nacos启动中... ($wait_count/$MAX_WAIT_TIME)"
    sleep 3
    wait_count=$((wait_count + 3))
done
echo "✅ Nacos服务已就绪"
echo ""

# 创建公共配置
echo "📝 创建公共配置..."
curl -X POST "$NACOS_ADDR/nacos/v1/cs/configs" \
  -d "dataId=havenbutler-common.yml" \
  -d "group=DEFAULT_GROUP" \
  -d "content=# 全局公共配置
datasource:
  postgresql:
    host: \${POSTGRESQL_HOST:postgres}
    port: \${POSTGRESQL_PORT:5432}
    database: \${POSTGRESQL_DB:havenbutler}
    username: \${POSTGRESQL_USER:postgres}
    password: \${POSTGRESQL_PASSWORD:postgres123}

  redis:
    host: \${REDIS_HOST:redis}
    port: \${REDIS_PORT:6379}
    password: \${REDIS_PASSWORD:redis123}

  mongodb:
    host: \${MONGODB_HOST:mongodb}
    port: \${MONGODB_PORT:27017}
    database: \${MONGODB_DB:havenbutler}

# 服务间通信配置
services:
  admin:
    url: http://admin-service:8888
    username: admin
    password: havenbutler2025

  storage:
    url: http://storage-service:8081

# 安全配置
security:
  jwt:
    secret: \${JWT_SECRET:havenbutler-jwt-secret-2024}
    expiration: 7200000

# 环境标识
environment: \${ENVIRONMENT:dev}" \
  -d "type=yaml"

# 创建Admin服务配置
echo "📝 创建Admin服务配置..."
curl -X POST "$NACOS_ADDR/nacos/v1/cs/configs" \
  -d "dataId=admin-service.yml" \
  -d "group=DEFAULT_GROUP" \
  -d "content=# Admin服务配置
server:
  port: 8888

spring:
  application:
    name: admin-service
  boot:
    admin:
      ui:
        title: \"HavenButler管理中心\"
        brand: \"HavenButler管理控制台 - \${environment}\"
      discovery:
        enabled: true

# 环境切换配置
environment-switch:
  enabled: true
  current: \${environment}
  environments:
    dev:
      database-url: \"postgresql://postgres:postgres123@postgres-dev:5432/havenbutler_dev\"
      log-level: DEBUG
      alert-enabled: false
    test:
      database-url: \"postgresql://postgres:test123@postgres-test:5432/havenbutler_test\"
      log-level: INFO
      alert-enabled: true
    prod:
      database-url: \"postgresql://postgres:\${PROD_DB_PASSWORD}@postgres-prod:5432/havenbutler_prod\"
      log-level: WARN
      alert-enabled: true

# 监控配置
monitoring:
  prometheus:
    url: http://prometheus:9090
  alerts:
    enabled: \${environment-switch.environments.\${environment}.alert-enabled:false}" \
  -d "type=yaml"

# 创建Storage服务配置
echo "📝 创建Storage服务配置..."
curl -X POST "$NACOS_ADDR/nacos/v1/cs/configs" \
  -d "dataId=storage-service.yml" \
  -d "group=DEFAULT_GROUP" \
  -d "content=# Storage服务配置
server:
  port: 8081

spring:
  application:
    name: storage-service
  boot:
    admin:
      client:
        url: \${services.admin.url}
        username: \${services.admin.username}
        password: \${services.admin.password}
        instance:
          service-base-url: \${services.storage.url}

# 数据源配置
datasource:
  url: jdbc:postgresql://\${datasource.postgresql.host}:\${datasource.postgresql.port}/\${datasource.postgresql.database}
  username: \${datasource.postgresql.username}
  password: \${datasource.postgresql.password}

redis:
  host: \${datasource.redis.host}
  port: \${datasource.redis.port}
  password: \${datasource.redis.password}

# 存储配置
storage:
  type: \${STORAGE_TYPE:minio}
  minio:
    endpoint: http://minio:9000
    access-key: \${MINIO_ACCESS_KEY:minioadmin}
    secret-key: \${MINIO_SECRET_KEY:minioadmin123}
    bucket: havenbutler-\${environment}

# 环境相关配置
environment-config:
  current: \${environment}
  dev:
    cache-ttl: 300  # 5分钟
    backup-enabled: false
  test:
    cache-ttl: 1800  # 30分钟
    backup-enabled: true
  prod:
    cache-ttl: 3600  # 1小时
    backup-enabled: true" \
  -d "type=yaml"

echo ""
echo "🎉 Nacos配置完成！"
echo "🌐 访问Nacos控制台: http://localhost:8848/nacos (nacos/nacos)"
echo "🔧 访问Admin管理后台: http://localhost:8888 (admin/havenbutler2025)"
echo ""
echo "📋 环境切换方式："
echo "1. 在Admin后台访问: /api/environment/available"
echo "2. 或通过环境变量: ENVIRONMENT=dev/test/prod"
echo ""