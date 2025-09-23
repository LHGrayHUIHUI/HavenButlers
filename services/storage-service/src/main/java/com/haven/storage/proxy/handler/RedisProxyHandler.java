package com.haven.storage.proxy.handler;

import com.haven.storage.logging.OperationLogService;
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
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

/**
 * Redis协议代理处理器
 * 实现Redis RESP (REdis Serialization Protocol) 的透明代理
 *
 * RESP协议格式:
 * - 简单字符串: +OK\r\n
 * - 错误: -Error message\r\n
 * - 整数: :1000\r\n
 * - 批量字符串: $6\r\nfoobar\r\n
 * - 数组: *2\r\n$3\r\nfoo\r\n$3\r\nbar\r\n
 *
 * 核心功能：
 * 1. 解析Redis RESP协议消息
 * 2. 转发客户端命令到后端Redis服务器
 * 3. 维护连接状态和事务上下文
 * 4. 拦截危险操作并记录安全日志
 * 5. 支持管道(Pipeline)和事务(MULTI/EXEC)
 *
 * @author HavenButler
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class RedisProxyHandler extends ChannelInboundHandlerAdapter {

    private final OperationLogService operationLogService;

    @Value("${datasource.redis.host:localhost}")
    private String backendHost;

    @Value("${datasource.redis.port:6379}")
    private int backendPort;

    @Value("${datasource.redis.password:}")
    private String backendPassword;

    // 连接映射
    private final Map<ChannelId, Channel> clientToBackendChannelMap = new ConcurrentHashMap<>();
    private final Map<ChannelId, Channel> backendToClientChannelMap = new ConcurrentHashMap<>();

    // 连接状态
    private final Map<ChannelId, RedisConnectionState> connectionStates = new ConcurrentHashMap<>();

    /**
     * 客户端连接激活
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        String clientIP = getClientIP(ctx);
        log.info("Redis客户端连接建立: {}", clientIP);

        // 创建到后端Redis的连接
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
                log.warn("Redis后端连接不可用，关闭客户端连接: {}", clientIP);
                ctx.close();
                return;
            }

            // 解析Redis RESP协议消息
            List<RedisCommand> commands = parseRedisCommands(data.duplicate());
            for (RedisCommand command : commands) {
                if (!processRedisCommand(ctx, command, clientIP)) {
                    // 如果命令被拦截，不转发到后端
                    return;
                }
            }

            // 转发数据到后端
            backendChannel.writeAndFlush(data.retain())
                .addListener(future -> {
                    if (!future.isSuccess()) {
                        log.error("转发数据到Redis后端失败: {}", future.cause().getMessage());
                        ctx.close();
                    }
                });

        } catch (Exception e) {
            log.error("处理Redis消息失败: {}", e.getMessage(), e);
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
        log.error("Redis代理连接异常: {} - {}", clientIP, cause.getMessage());

        // 记录安全日志
        operationLogService.logSecurityEvent(
            null,
            clientIP,
            "CONNECTION_ERROR",
            "REDIS",
            "连接异常: " + cause.getMessage(),
            "MEDIUM"
        );

        ctx.close();
    }

    /**
     * 客户端连接断开
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String clientIP = getClientIP(ctx);
        log.info("Redis客户端连接断开: {}", clientIP);

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
     * 创建到后端Redis的连接
     */
    private void createBackendConnection(ChannelHandlerContext clientCtx) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(clientCtx.channel().eventLoop())
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast(new BackendRedisHandler(clientCtx));
                }
            });

        // 连接后端Redis服务器
        bootstrap.connect(backendHost, backendPort)
            .addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    Channel backendChannel = future.channel();

                    // 建立双向连接映射
                    clientToBackendChannelMap.put(clientCtx.channel().id(), backendChannel);
                    backendToClientChannelMap.put(backendChannel.id(), clientCtx.channel());

                    // 初始化连接状态
                    connectionStates.put(clientCtx.channel().id(),
                        new RedisConnectionState(getClientIP(clientCtx)));

                    log.info("成功建立Redis后端连接: {} -> {}:{}",
                        getClientIP(clientCtx), backendHost, backendPort);

                    // 如果配置了密码，先进行认证
                    if (backendPassword != null && !backendPassword.isEmpty()) {
                        authenticateBackend(backendChannel);
                    }

                } else {
                    log.error("无法连接到Redis后端服务器: {}:{} - {}",
                        backendHost, backendPort, future.cause().getMessage());
                    clientCtx.close();
                }
            });
    }

    /**
     * 后端Redis认证
     */
    private void authenticateBackend(Channel backendChannel) {
        String authCommand = "*2\r\n$4\r\nAUTH\r\n$" + backendPassword.length() + "\r\n" + backendPassword + "\r\n";
        ByteBuf authBuf = Unpooled.copiedBuffer(authCommand, StandardCharsets.UTF_8);
        backendChannel.writeAndFlush(authBuf);
        log.debug("向Redis后端发送认证命令");
    }

    /**
     * 解析Redis RESP协议命令
     */
    private List<RedisCommand> parseRedisCommands(ByteBuf data) {
        List<RedisCommand> commands = new ArrayList<>();

        try {
            data.markReaderIndex();

            while (data.readableBytes() > 0) {
                data.markReaderIndex();
                RedisCommand command = parseRESPMessage(data);
                if (command != null) {
                    commands.add(command);
                } else {
                    data.resetReaderIndex();
                    break; // 数据不完整，等待更多数据
                }
            }

            data.resetReaderIndex();

        } catch (Exception e) {
            log.debug("解析Redis命令失败: {}", e.getMessage());
            data.resetReaderIndex();
        }

        return commands;
    }

    /**
     * 解析RESP消息
     */
    private RedisCommand parseRESPMessage(ByteBuf data) {
        if (data.readableBytes() < 1) {
            return null;
        }

        byte firstByte = data.readByte();

        switch (firstByte) {
            case '*': // 数组 (通常是Redis命令)
                return parseArray(data);
            case '+': // 简单字符串
                String simpleString = readLine(data);
                return simpleString != null ? new RedisCommand(Arrays.asList(simpleString)) : null;
            case '-': // 错误
                String error = readLine(data);
                return error != null ? new RedisCommand(Arrays.asList("ERROR", error)) : null;
            case ':': // 整数
                String integer = readLine(data);
                return integer != null ? new RedisCommand(Arrays.asList("INTEGER", integer)) : null;
            case '$': // 批量字符串
                String bulkString = parseBulkString(data);
                return bulkString != null ? new RedisCommand(Arrays.asList(bulkString)) : null;
            default:
                // 未知类型，跳过一行
                readLine(data);
                return null;
        }
    }

    /**
     * 解析数组(Redis命令)
     */
    private RedisCommand parseArray(ByteBuf data) {
        String arrayLengthStr = readLine(data);
        if (arrayLengthStr == null) {
            return null;
        }

        int arrayLength;
        try {
            arrayLength = Integer.parseInt(arrayLengthStr);
        } catch (NumberFormatException e) {
            return null;
        }

        if (arrayLength <= 0) {
            return new RedisCommand(new ArrayList<>());
        }

        List<String> arguments = new ArrayList<>();

        for (int i = 0; i < arrayLength; i++) {
            if (data.readableBytes() < 1) {
                return null; // 数据不完整
            }

            byte type = data.readByte();
            if (type == '$') {
                String bulkString = parseBulkString(data);
                if (bulkString == null) {
                    return null;
                }
                arguments.add(bulkString);
            } else {
                // 其他类型，读取一行
                data.readerIndex(data.readerIndex() - 1);
                String line = readLine(data);
                if (line == null) {
                    return null;
                }
                arguments.add(line);
            }
        }

        return new RedisCommand(arguments);
    }

    /**
     * 解析批量字符串
     */
    private String parseBulkString(ByteBuf data) {
        String lengthStr = readLine(data);
        if (lengthStr == null) {
            return null;
        }

        int length;
        try {
            length = Integer.parseInt(lengthStr);
        } catch (NumberFormatException e) {
            return null;
        }

        if (length == -1) {
            return null; // Null bulk string
        }

        if (length == 0) {
            // 空字符串，但仍需读取\r\n
            readLine(data);
            return "";
        }

        if (data.readableBytes() < length + 2) {
            return null; // 数据不完整
        }

        byte[] bytes = new byte[length];
        data.readBytes(bytes);

        // 跳过\r\n
        if (data.readableBytes() >= 2) {
            data.skipBytes(2);
        }

        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * 读取一行(以\r\n结尾)
     */
    private String readLine(ByteBuf data) {
        StringBuilder sb = new StringBuilder();

        while (data.readableBytes() > 0) {
            byte b = data.readByte();
            if (b == '\r') {
                if (data.readableBytes() > 0 && data.readByte() == '\n') {
                    return sb.toString();
                } else {
                    return null; // 格式错误
                }
            } else {
                sb.append((char) b);
            }
        }

        return null; // 数据不完整
    }

    /**
     * 处理Redis命令
     */
    private boolean processRedisCommand(ChannelHandlerContext ctx, RedisCommand command, String clientIP) {
        if (command == null || command.getArguments().isEmpty()) {
            return true;
        }

        RedisConnectionState state = connectionStates.get(ctx.channel().id());
        if (state == null) {
            return true;
        }

        String commandName = command.getArguments().get(0).toUpperCase();
        String fullCommand = String.join(" ", command.getArguments());

        // 检查是否为危险操作
        if (isDangerousOperation(commandName)) {
            log.warn("检测到危险Redis操作，拒绝执行: {} - {}", clientIP, fullCommand);

            // 发送错误响应给客户端
            sendErrorResponse(ctx, "操作被代理拦截：" + commandName);

            // 记录安全日志
            operationLogService.logSecurityEvent(
                state.getFamilyId(),
                clientIP,
                "DANGEROUS_OPERATION_BLOCKED",
                "REDIS",
                "被拦截的命令: " + fullCommand,
                "HIGH"
            );

            return false; // 拦截命令
        }

        // 更新连接状态
        updateConnectionState(state, commandName, command.getArguments());

        // 记录操作日志
        operationLogService.logOperation(
            state.getFamilyId(),
            "REDIS",
            clientIP,
            state.getUserId(),
            commandName,
            "", // Redis没有数据库名概念
            "", // Redis没有表名概念
            fullCommand,
            0,
            "IN_PROGRESS",
            null
        );

        return true; // 允许命令执行
    }

    /**
     * 更新连接状态
     */
    private void updateConnectionState(RedisConnectionState state, String commandName, List<String> arguments) {
        switch (commandName) {
            case "SELECT":
                if (arguments.size() > 1) {
                    try {
                        int dbIndex = Integer.parseInt(arguments.get(1));
                        state.setCurrentDatabase(dbIndex);
                    } catch (NumberFormatException e) {
                        // 忽略无效的数据库索引
                    }
                }
                break;

            case "MULTI":
                state.setInTransaction(true);
                break;

            case "EXEC":
            case "DISCARD":
                state.setInTransaction(false);
                break;

            case "AUTH":
                state.setAuthenticated(true);
                break;
        }
    }

    /**
     * 检查是否为危险操作
     */
    private boolean isDangerousOperation(String commandName) {
        // 危险操作列表
        List<String> dangerousCommands = Arrays.asList(
            "FLUSHDB", "FLUSHALL",   // 清空数据库
            "CONFIG", "SHUTDOWN",    // 配置和关闭
            "DEBUG", "MONITOR",      // 调试和监控
            "EVAL", "EVALSHA",       // Lua脚本(可能有安全风险)
            "SCRIPT",                // 脚本管理
            "SLAVEOF", "REPLICAOF",  // 主从复制
            "MIGRATE", "RESTORE",    // 数据迁移
            "LATENCY"                // 延迟监控
        );

        return dangerousCommands.contains(commandName);
    }

    /**
     * 发送错误响应给客户端
     */
    private void sendErrorResponse(ChannelHandlerContext ctx, String errorMessage) {
        String response = "-ERR " + errorMessage + "\r\n";
        ByteBuf errorBuf = Unpooled.copiedBuffer(response, StandardCharsets.UTF_8);
        ctx.writeAndFlush(errorBuf);
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIP(ChannelHandlerContext ctx) {
        return ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
    }

    /**
     * 后端Redis连接处理器
     */
    private class BackendRedisHandler extends ChannelInboundHandlerAdapter {
        private final ChannelHandlerContext clientCtx;

        public BackendRedisHandler(ChannelHandlerContext clientCtx) {
            this.clientCtx = clientCtx;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (clientCtx.channel().isActive()) {
                clientCtx.writeAndFlush(msg);
            } else {
                ((ByteBuf) msg).release();
                ctx.close();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("Redis后端连接异常: {}", cause.getMessage());
            ctx.close();
            if (clientCtx.channel().isActive()) {
                clientCtx.close();
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.info("Redis后端连接断开");
            if (clientCtx.channel().isActive()) {
                clientCtx.close();
            }
        }
    }

    /**
     * Redis连接状态
     */
    private static class RedisConnectionState {
        private final String clientIP;
        private String familyId;
        private String userId;
        private int currentDatabase = 0;
        private boolean inTransaction = false;
        private boolean authenticated = false;

        public RedisConnectionState(String clientIP) {
            this.clientIP = clientIP;
        }

        // Getters and Setters
        public String getClientIP() { return clientIP; }
        public String getFamilyId() { return familyId; }
        public void setFamilyId(String familyId) { this.familyId = familyId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public int getCurrentDatabase() { return currentDatabase; }
        public void setCurrentDatabase(int currentDatabase) { this.currentDatabase = currentDatabase; }
        public boolean isInTransaction() { return inTransaction; }
        public void setInTransaction(boolean inTransaction) { this.inTransaction = inTransaction; }
        public boolean isAuthenticated() { return authenticated; }
        public void setAuthenticated(boolean authenticated) { this.authenticated = authenticated; }
    }

    /**
     * Redis命令
     */
    private static class RedisCommand {
        private final List<String> arguments;

        public RedisCommand(List<String> arguments) {
            this.arguments = arguments;
        }

        public List<String> getArguments() {
            return arguments;
        }

        public String getCommandName() {
            return arguments.isEmpty() ? "" : arguments.get(0).toUpperCase();
        }

        @Override
        public String toString() {
            return String.join(" ", arguments);
        }
    }
}