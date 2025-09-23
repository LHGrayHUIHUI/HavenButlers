package com.haven.storage.proxy;

import com.haven.storage.proxy.handler.MongoProxyHandler;
import com.haven.storage.proxy.handler.PostgresProxyHandler;
import com.haven.storage.proxy.handler.RedisProxyHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.CompletableFuture;

/**
 * 数据库代理服务启动器
 * 基于Netty实现多协议代理服务
 * 支持PostgreSQL、MongoDB、Redis协议代理
 *
 * @author HavenButler
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProxyServerBootstrap {

    private final PostgresProxyHandler postgresHandler;
    private final MongoProxyHandler mongoHandler;
    private final RedisProxyHandler redisHandler;

    @Value("${proxy.postgresql.port:5433}")
    private int postgresPort;

    @Value("${proxy.mongodb.port:27018}")
    private int mongoPort;

    @Value("${proxy.redis.port:6380}")
    private int redisPort;

    @Value("${proxy.enabled:true}")
    private boolean proxyEnabled;

    // Netty事件循环组
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    // 服务器通道
    private Channel postgresChannel;
    private Channel mongoChannel;
    private Channel redisChannel;

    /**
     * 应用启动完成后启动代理服务
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startProxyServers() {
        if (!proxyEnabled) {
            log.info("数据库代理服务已禁用，跳过启动");
            return;
        }

        log.info("开始启动数据库代理服务...");

        // 初始化Netty线程组
        bossGroup = new NioEventLoopGroup(1); // 接受连接的线程组
        workerGroup = new NioEventLoopGroup(); // 处理IO的线程组

        // 并发启动三个代理服务
        CompletableFuture<Void> postgresTask = CompletableFuture.runAsync(this::startPostgresProxy);
        CompletableFuture<Void> mongoTask = CompletableFuture.runAsync(this::startMongoProxy);
        CompletableFuture<Void> redisTask = CompletableFuture.runAsync(this::startRedisProxy);

        // 等待所有代理服务启动完成
        CompletableFuture.allOf(postgresTask, mongoTask, redisTask)
            .thenRun(() -> log.info("✅ 所有数据库代理服务启动完成"))
            .exceptionally(throwable -> {
                log.error("❌ 代理服务启动失败", throwable);
                return null;
            });
    }

    /**
     * 启动PostgreSQL代理服务
     */
    private void startPostgresProxy() {
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();

                        // 添加PostgreSQL协议处理器
                        pipeline.addLast("postgres-handler", postgresHandler);
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true);

            // 绑定端口并启动服务
            ChannelFuture future = bootstrap.bind(postgresPort).sync();
            postgresChannel = future.channel();

            log.info("✅ PostgreSQL代理服务启动成功，监听端口: {}", postgresPort);
            log.info("📝 客户端连接示例: psql -h localhost -p {} -U username -d database", postgresPort);

        } catch (Exception e) {
            log.error("❌ PostgreSQL代理服务启动失败", e);
            throw new RuntimeException("PostgreSQL代理启动失败", e);
        }
    }

    /**
     * 启动MongoDB代理服务
     */
    private void startMongoProxy() {
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();

                        // 添加MongoDB协议处理器
                        pipeline.addLast("mongo-handler", mongoHandler);
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true);

            // 绑定端口并启动服务
            ChannelFuture future = bootstrap.bind(mongoPort).sync();
            mongoChannel = future.channel();

            log.info("✅ MongoDB代理服务启动成功，监听端口: {}", mongoPort);
            log.info("📝 客户端连接示例: mongo --host localhost:{}", mongoPort);

        } catch (Exception e) {
            log.error("❌ MongoDB代理服务启动失败", e);
            throw new RuntimeException("MongoDB代理启动失败", e);
        }
    }

    /**
     * 启动Redis代理服务
     */
    private void startRedisProxy() {
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();

                        // 添加Redis协议处理器
                        pipeline.addLast("redis-handler", redisHandler);
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true);

            // 绑定端口并启动服务
            ChannelFuture future = bootstrap.bind(redisPort).sync();
            redisChannel = future.channel();

            log.info("✅ Redis代理服务启动成功，监听端口: {}", redisPort);
            log.info("📝 客户端连接示例: redis-cli -h localhost -p {}", redisPort);

        } catch (Exception e) {
            log.error("❌ Redis代理服务启动失败", e);
            throw new RuntimeException("Redis代理启动失败", e);
        }
    }

    /**
     * 应用关闭时清理资源
     */
    @PreDestroy
    public void shutdownProxyServers() {
        log.info("开始关闭数据库代理服务...");

        try {
            // 关闭服务器通道
            if (postgresChannel != null && postgresChannel.isActive()) {
                postgresChannel.close().sync();
                log.info("PostgreSQL代理服务已关闭");
            }

            if (mongoChannel != null && mongoChannel.isActive()) {
                mongoChannel.close().sync();
                log.info("MongoDB代理服务已关闭");
            }

            if (redisChannel != null && redisChannel.isActive()) {
                redisChannel.close().sync();
                log.info("Redis代理服务已关闭");
            }

            // 关闭线程组
            if (workerGroup != null) {
                workerGroup.shutdownGracefully().sync();
            }

            if (bossGroup != null) {
                bossGroup.shutdownGracefully().sync();
            }

            log.info("✅ 所有数据库代理服务关闭完成");

        } catch (Exception e) {
            log.error("❌ 代理服务关闭过程中发生错误", e);
        }
    }

    /**
     * 获取代理服务状态
     */
    public ProxyStatus getProxyStatus() {
        return ProxyStatus.builder()
            .enabled(proxyEnabled)
            .postgresPort(postgresPort)
            .postgresActive(postgresChannel != null && postgresChannel.isActive())
            .mongoPort(mongoPort)
            .mongoActive(mongoChannel != null && mongoChannel.isActive())
            .redisPort(redisPort)
            .redisActive(redisChannel != null && redisChannel.isActive())
            .build();
    }

    /**
     * 代理服务状态信息
     */
    public static class ProxyStatus {
        private boolean enabled;
        private int postgresPort;
        private boolean postgresActive;
        private int mongoPort;
        private boolean mongoActive;
        private int redisPort;
        private boolean redisActive;

        // 使用Lombok @Builder注解
        public static ProxyStatusBuilder builder() {
            return new ProxyStatusBuilder();
        }

        public static class ProxyStatusBuilder {
            private boolean enabled;
            private int postgresPort;
            private boolean postgresActive;
            private int mongoPort;
            private boolean mongoActive;
            private int redisPort;
            private boolean redisActive;

            public ProxyStatusBuilder enabled(boolean enabled) {
                this.enabled = enabled;
                return this;
            }

            public ProxyStatusBuilder postgresPort(int postgresPort) {
                this.postgresPort = postgresPort;
                return this;
            }

            public ProxyStatusBuilder postgresActive(boolean postgresActive) {
                this.postgresActive = postgresActive;
                return this;
            }

            public ProxyStatusBuilder mongoPort(int mongoPort) {
                this.mongoPort = mongoPort;
                return this;
            }

            public ProxyStatusBuilder mongoActive(boolean mongoActive) {
                this.mongoActive = mongoActive;
                return this;
            }

            public ProxyStatusBuilder redisPort(int redisPort) {
                this.redisPort = redisPort;
                return this;
            }

            public ProxyStatusBuilder redisActive(boolean redisActive) {
                this.redisActive = redisActive;
                return this;
            }

            public ProxyStatus build() {
                ProxyStatus status = new ProxyStatus();
                status.enabled = this.enabled;
                status.postgresPort = this.postgresPort;
                status.postgresActive = this.postgresActive;
                status.mongoPort = this.mongoPort;
                status.mongoActive = this.mongoActive;
                status.redisPort = this.redisPort;
                status.redisActive = this.redisActive;
                return status;
            }
        }

        // Getters
        public boolean isEnabled() { return enabled; }
        public int getPostgresPort() { return postgresPort; }
        public boolean isPostgresActive() { return postgresActive; }
        public int getMongoPort() { return mongoPort; }
        public boolean isMongoActive() { return mongoActive; }
        public int getRedisPort() { return redisPort; }
        public boolean isRedisActive() { return redisActive; }
    }
}