# 简单的Spring Boot应用Dockerfile
FROM openjdk:17-jre-slim

# 复制jar文件
COPY target/admin-service-*.jar app.jar

# 暴露端口
EXPOSE 8888

# 启动应用
# 启动命令：激活docker环境，加载application-docker.yml
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]