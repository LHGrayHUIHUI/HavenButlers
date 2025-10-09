# Spring Boot 应用 Dockerfile
FROM eclipse-temurin:17-jre

# 安装 curl 用于健康检查（推荐方案，避免 bash 兼容性问题）
RUN apt-get update && \
    apt-get install -y curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 设置工作目录
WORKDIR /app

# 复制jar文件
COPY target/admin-service-*.jar app.jar

# 暴露端口
EXPOSE 8888

# 健康检查 - 使用 curl 检测（更稳妥，避免 /dev/tcp 兼容性问题）
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8888/actuator/health || exit 1

# 启动应用
# 启动命令：激活docker环境，加载application-docker.yml
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]