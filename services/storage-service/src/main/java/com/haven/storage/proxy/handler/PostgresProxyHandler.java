package com.haven.storage.proxy.handler;

import com.haven.storage.logging.OperationLogService;
import com.haven.storage.proxy.pool.PostgresConnectionPool;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * PostgreSQL协议代理处理器
 * 实现PostgreSQL Wire Protocol的透明代理
 *
 * 核心功能：
 * 1. 解析PostgreSQL协议消息
 * 2. 转发客户端请求到后端PostgreSQL服务器
 * 3. 维护连接状态和事务上下文
 * 4. 记录操作日志和安全审计
 *
 * @author HavenButler
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class PostgresProxyHandler extends ChannelInboundHandlerAdapter {

    private final PostgresConnectionPool connectionPool;
    private final OperationLogService operationLogService;

    @Value("${datasource.postgresql.primary.host:localhost}")
    private String backendHost;

    @Value("${datasource.postgresql.primary.port:5432}")
    private int backendPort;

    // 存储客户端连接与后端连接的映射关系
    private final Map<ChannelId, Channel> clientToBackendChannelMap = new ConcurrentHashMap<>();
    private final Map<ChannelId, Channel> backendToClientChannelMap = new ConcurrentHashMap<>();

    // 存储连接状态
    private final Map<ChannelId, PostgresConnectionState> connectionStates = new ConcurrentHashMap<>();

    /**
     * 客户端连接激活时创建到后端的连接
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        String clientIP = getClientIP(ctx);
        log.info("PostgreSQL客户端连接建立: {}", clientIP);

        // 创建到后端PostgreSQL的连接
        createBackendConnection(ctx);
    }

    /**
     * 处理客户端发送的数据
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf data = (ByteBuf) msg;
        String clientIP = getClientIP(ctx);

        try {
            // 获取对应的后端连接
            Channel backendChannel = clientToBackendChannelMap.get(ctx.channel().id());
            if (backendChannel == null || !backendChannel.isActive()) {
                log.warn("后端连接不可用，关闭客户端连接: {}", clientIP);
                ctx.close();
                return;
            }

            // 解析PostgreSQL协议消息(简化版本，实际需要完整协议解析)
            parseAndLogPostgresMessage(ctx, data.duplicate(), clientIP);

            // 转发数据到后端
            backendChannel.writeAndFlush(data.retain())
                .addListener(future -> {
                    if (!future.isSuccess()) {
                        log.error("转发数据到PostgreSQL后端失败: {}", future.cause().getMessage());
                        ctx.close();
                    }
                });

        } catch (Exception e) {
            log.error("处理PostgreSQL消息失败: {}", e.getMessage(), e);
            ctx.close();
        } finally {
            data.release();
        }
    }

    /**
     * 处理连接异常
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String clientIP = getClientIP(ctx);
        log.error("PostgreSQL代理连接异常: {} - {}", clientIP, cause.getMessage());

        // 记录安全日志
        operationLogService.logSecurityEvent(
            null, // familyId暂时为空，实际需要从认证信息获取
            clientIP,
            "CONNECTION_ERROR",
            "POSTGRESQL",
            "连接异常: " + cause.getMessage(),
            "MEDIUM"
        );

        ctx.close();
    }

    /**
     * 客户端连接断开时清理资源
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String clientIP = getClientIP(ctx);
        log.info("PostgreSQL客户端连接断开: {}", clientIP);

        // 清理连接映射
        Channel backendChannel = clientToBackendChannelMap.remove(ctx.channel().id());
        if (backendChannel != null) {
            backendToClientChannelMap.remove(backendChannel.id());
            backendChannel.close();
        }

        // 清理连接状态
        connectionStates.remove(ctx.channel().id());
    }

    /**
     * 创建到后端PostgreSQL的连接
     */
    private void createBackendConnection(ChannelHandlerContext clientCtx) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(clientCtx.channel().eventLoop())
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast(new BackendPostgresHandler(clientCtx));
                }
            });

        // 连接后端PostgreSQL服务器
        bootstrap.connect(backendHost, backendPort)
            .addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    Channel backendChannel = future.channel();

                    // 建立双向连接映射
                    clientToBackendChannelMap.put(clientCtx.channel().id(), backendChannel);
                    backendToClientChannelMap.put(backendChannel.id(), clientCtx.channel());

                    // 初始化连接状态
                    connectionStates.put(clientCtx.channel().id(),
                        new PostgresConnectionState(getClientIP(clientCtx)));

                    log.info("成功建立PostgreSQL后端连接: {} -> {}:{}",
                        getClientIP(clientCtx), backendHost, backendPort);

                } else {
                    log.error("无法连接到PostgreSQL后端服务器: {}:{} - {}",
                        backendHost, backendPort, future.cause().getMessage());
                    clientCtx.close();
                }
            });
    }

    /**
     * 解析并记录PostgreSQL协议消息
     */
    private void parseAndLogPostgresMessage(ChannelHandlerContext ctx, ByteBuf data, String clientIP) {
        try {
            if (data.readableBytes() < 5) {
                return; // 消息太短，跳过
            }

            data.markReaderIndex();
            byte messageType = data.readByte();
            int messageLength = data.readInt();
            data.resetReaderIndex();

            PostgresConnectionState state = connectionStates.get(ctx.channel().id());
            if (state == null) {
                return;
            }

            String operation = "UNKNOWN";
            String content = "";

            // 简化的协议解析(实际需要完整实现PostgreSQL Wire Protocol)
            switch (messageType) {
                case 'Q': // Simple Query
                    operation = "QUERY";
                    if (data.readableBytes() >= messageLength + 1) {
                        data.skipBytes(5); // 跳过类型和长度
                        byte[] sqlBytes = new byte[messageLength - 5];
                        data.readBytes(sqlBytes);
                        content = new String(sqlBytes, StandardCharsets.UTF_8);
                        // 移除末尾的null字符
                        if (content.endsWith("\0")) {
                            content = content.substring(0, content.length() - 1);
                        }
                    }
                    break;
                case 'P': // Parse
                    operation = "PARSE";
                    break;
                case 'B': // Bind
                    operation = "BIND";
                    break;
                case 'E': // Execute
                    operation = "EXECUTE";
                    break;
                case 'X': // Terminate
                    operation = "TERMINATE";
                    break;
                default:
                    operation = "TYPE_" + (char) messageType;
            }

            data.resetReaderIndex();

            // 检查是否为危险操作
            if (isDangerousOperation(content)) {
                log.warn("检测到危险操作，拒绝执行: {} - {}", clientIP, content);

                // 发送错误响应给客户端
                sendErrorResponse(ctx, "操作被代理拦截：" + content);

                // 记录安全日志
                operationLogService.logSecurityEvent(
                    state.getFamilyId(),
                    clientIP,
                    "DANGEROUS_OPERATION_BLOCKED",
                    "POSTGRESQL",
                    "被拦截的SQL: " + content,
                    "HIGH"
                );
                return;
            }

            // 记录操作日志
            operationLogService.logOperation(
                state.getFamilyId(),
                "POSTGRESQL",
                clientIP,
                state.getUserId(),
                operation,
                state.getDatabaseName(),
                "", // 表名需要从SQL解析
                content,
                0, // 执行时间稍后更新
                "IN_PROGRESS",
                null
            );

        } catch (Exception e) {
            log.warn("解析PostgreSQL消息失败: {}", e.getMessage());
        }
    }

    /**
     * 检查是否为危险操作
     */
    private boolean isDangerousOperation(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }

        String upperSQL = sql.trim().toUpperCase();

        // 危险操作列表
        String[] dangerousPatterns = {
            "DROP DATABASE",
            "DROP SCHEMA",
            "TRUNCATE TABLE",
            "DELETE FROM",
            "ALTER SYSTEM",
            "CREATE ROLE",
            "DROP ROLE"
        };

        for (String pattern : dangerousPatterns) {
            if (upperSQL.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 发送错误响应给客户端
     */
    private void sendErrorResponse(ChannelHandlerContext ctx, String errorMessage) {
        // 构造PostgreSQL错误响应消息
        // 这是简化版本，实际需要按照PostgreSQL协议格式构造
        String response = "E\0\0\0\u008BERROR\0C42000\0M" + errorMessage + "\0\0";
        ByteBuf errorBuf = Unpooled.copiedBuffer(response, StandardCharsets.UTF_8);

        ctx.writeAndFlush(errorBuf)
            .addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIP(ChannelHandlerContext ctx) {
        return ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
    }

    /**
     * 后端PostgreSQL连接处理器
     */
    private class BackendPostgresHandler extends ChannelInboundHandlerAdapter {
        private final ChannelHandlerContext clientCtx;

        public BackendPostgresHandler(ChannelHandlerContext clientCtx) {
            this.clientCtx = clientCtx;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            // 将后端响应转发给客户端
            if (clientCtx.channel().isActive()) {
                clientCtx.writeAndFlush(msg);
            } else {
                ((ByteBuf) msg).release();
                ctx.close();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("PostgreSQL后端连接异常: {}", cause.getMessage());
            ctx.close();
            if (clientCtx.channel().isActive()) {
                clientCtx.close();
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.info("PostgreSQL后端连接断开");
            if (clientCtx.channel().isActive()) {
                clientCtx.close();
            }
        }
    }

    /**
     * PostgreSQL连接状态信息
     */
    private static class PostgresConnectionState {
        private final String clientIP;
        private String familyId;
        private String userId;
        private String databaseName;
        private boolean inTransaction = false;

        public PostgresConnectionState(String clientIP) {
            this.clientIP = clientIP;
        }

        // Getters and Setters
        public String getClientIP() { return clientIP; }
        public String getFamilyId() { return familyId; }
        public void setFamilyId(String familyId) { this.familyId = familyId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getDatabaseName() { return databaseName; }
        public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }
        public boolean isInTransaction() { return inTransaction; }
        public void setInTransaction(boolean inTransaction) { this.inTransaction = inTransaction; }
    }
}