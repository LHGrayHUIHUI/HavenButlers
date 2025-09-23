package com.haven.storage.proxy.pool;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PostgreSQL连接池管理器
 * 管理到后端PostgreSQL服务器的连接池
 *
 * @author HavenButler
 */
@Slf4j
@Component
public class PostgresConnectionPool {

    @Value("${proxy.postgresql.backend.host:localhost}")
    private String backendHost;

    @Value("${proxy.postgresql.backend.port:5432}")
    private int backendPort;

    @Value("${proxy.postgresql.pool.max-connections:50}")
    private int maxConnections;

    @Value("${proxy.postgresql.pool.min-idle:5}")
    private int minIdle;

    @Value("${proxy.postgresql.pool.connection-timeout:30000}")
    private int connectionTimeout;

    // 连接池队列
    private BlockingQueue<Channel> availableConnections;
    private BlockingQueue<Channel> allConnections;

    // 连接计数器
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicInteger totalConnections = new AtomicInteger(0);

    // Netty引导器
    private Bootstrap bootstrap;
    private EventLoopGroup workerGroup;

    @PostConstruct
    public void initializePool() {
        log.info("初始化PostgreSQL连接池 - 最大连接数: {}, 最小空闲: {}", maxConnections, minIdle);

        availableConnections = new LinkedBlockingQueue<>(maxConnections);
        allConnections = new LinkedBlockingQueue<>(maxConnections);

        // 初始化Netty引导器
        workerGroup = new io.netty.channel.nio.NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
            .channel(NioSocketChannel.class)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout);

        // 预创建最小空闲连接数
        for (int i = 0; i < minIdle; i++) {
            try {
                Channel channel = createConnection();
                if (channel != null) {
                    availableConnections.offer(channel);
                    allConnections.offer(channel);
                    totalConnections.incrementAndGet();
                }
            } catch (Exception e) {
                log.error("预创建PostgreSQL连接失败", e);
            }
        }

        log.info("PostgreSQL连接池初始化完成，当前连接数: {}", totalConnections.get());
    }

    /**
     * 获取可用连接
     */
    public Channel getConnection() throws InterruptedException {
        Channel channel = availableConnections.poll(5, TimeUnit.SECONDS);

        if (channel == null || !channel.isActive()) {
            // 如果没有可用连接或连接无效，尝试创建新连接
            if (totalConnections.get() < maxConnections) {
                channel = createConnection();
                if (channel != null) {
                    allConnections.offer(channel);
                    totalConnections.incrementAndGet();
                    log.debug("创建新的PostgreSQL连接，当前总连接数: {}", totalConnections.get());
                }
            }
        }

        if (channel != null && channel.isActive()) {
            activeConnections.incrementAndGet();
            return channel;
        }

        throw new RuntimeException("无法获取PostgreSQL连接 - 连接池已满或后端服务不可用");
    }

    /**
     * 归还连接到池中
     */
    public void returnConnection(Channel channel) {
        if (channel != null && channel.isActive()) {
            if (availableConnections.offer(channel)) {
                activeConnections.decrementAndGet();
                log.debug("连接已归还到PostgreSQL连接池");
            } else {
                log.warn("无法归还连接到池中，池可能已满");
                channel.close();
                totalConnections.decrementAndGet();
            }
        }
    }

    /**
     * 创建到后端PostgreSQL的连接
     */
    private Channel createConnection() {
        try {
            ChannelFuture future = bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    // 这里可以添加连接特定的处理器
                    ch.pipeline().addLast(new ConnectionPoolHandler());
                }
            }).connect(backendHost, backendPort);

            Channel channel = future.sync().channel();
            log.debug("成功创建PostgreSQL后端连接: {}:{}", backendHost, backendPort);
            return channel;

        } catch (Exception e) {
            log.error("创建PostgreSQL后端连接失败: {}:{}", backendHost, backendPort, e);
            return null;
        }
    }

    /**
     * 获取连接池状态
     */
    public ConnectionPoolStatus getStatus() {
        return new ConnectionPoolStatus(
            totalConnections.get(),
            activeConnections.get(),
            availableConnections.size(),
            maxConnections,
            minIdle
        );
    }

    /**
     * 关闭连接池
     */
    @PreDestroy
    public void shutdown() {
        log.info("开始关闭PostgreSQL连接池...");

        // 关闭所有连接
        while (!allConnections.isEmpty()) {
            Channel channel = allConnections.poll();
            if (channel != null && channel.isActive()) {
                channel.close();
            }
        }

        // 关闭工作线程组
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }

        log.info("PostgreSQL连接池已关闭");
    }

    /**
     * 连接池处理器
     */
    private static class ConnectionPoolHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.debug("PostgreSQL后端连接断开: {}", ctx.channel().remoteAddress());
            ctx.fireChannelInactive();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("PostgreSQL后端连接异常: {}", cause.getMessage());
            ctx.close();
        }
    }

    /**
     * 连接池状态信息
     */
    public static class ConnectionPoolStatus {
        private final int totalConnections;
        private final int activeConnections;
        private final int idleConnections;
        private final int maxConnections;
        private final int minIdle;

        public ConnectionPoolStatus(int totalConnections, int activeConnections,
                                  int idleConnections, int maxConnections, int minIdle) {
            this.totalConnections = totalConnections;
            this.activeConnections = activeConnections;
            this.idleConnections = idleConnections;
            this.maxConnections = maxConnections;
            this.minIdle = minIdle;
        }

        // Getters
        public int getTotalConnections() { return totalConnections; }
        public int getActiveConnections() { return activeConnections; }
        public int getIdleConnections() { return idleConnections; }
        public int getMaxConnections() { return maxConnections; }
        public int getMinIdle() { return minIdle; }
        public double getUtilizationRate() {
            return maxConnections > 0 ? (double) activeConnections / maxConnections * 100 : 0;
        }
    }
}