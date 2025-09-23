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
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;

/**
 * MongoDB协议代理处理器
 * 实现MongoDB Wire Protocol的透明代理
 *
 * MongoDB Wire Protocol格式:
 * [4字节消息长度][4字节请求ID][4字节响应ID][4字节操作码][消息体]
 *
 * 核心功能：
 * 1. 解析MongoDB二进制协议消息
 * 2. 转发客户端请求到后端MongoDB服务器
 * 3. 维护连接状态和会话上下文
 * 4. 拦截危险操作并记录安全日志
 *
 * @author HavenButler
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class MongoProxyHandler extends ChannelInboundHandlerAdapter {

    private final OperationLogService operationLogService;

    @Value("${datasource.mongodb.host:localhost}")
    private String backendHost;

    @Value("${datasource.mongodb.port:27017}")
    private int backendPort;

    // MongoDB操作码常量
    private static final int OP_REPLY = 1;
    private static final int OP_UPDATE = 2001;
    private static final int OP_INSERT = 2002;
    private static final int OP_QUERY = 2004;
    private static final int OP_GET_MORE = 2005;
    private static final int OP_DELETE = 2006;
    private static final int OP_KILL_CURSORS = 2007;
    private static final int OP_MSG = 2013; // MongoDB 3.6+ 主要使用的操作码

    // 连接映射
    private final Map<ChannelId, Channel> clientToBackendChannelMap = new ConcurrentHashMap<>();
    private final Map<ChannelId, Channel> backendToClientChannelMap = new ConcurrentHashMap<>();

    // 连接状态
    private final Map<ChannelId, MongoConnectionState> connectionStates = new ConcurrentHashMap<>();

    /**
     * 客户端连接激活
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        String clientIP = getClientIP(ctx);
        log.info("MongoDB客户端连接建立: {}", clientIP);

        // 创建到后端MongoDB的连接
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
                log.warn("MongoDB后端连接不可用，关闭客户端连接: {}", clientIP);
                ctx.close();
                return;
            }

            // 解析MongoDB协议消息
            parseAndLogMongoMessage(ctx, data.duplicate(), clientIP);

            // 转发数据到后端
            backendChannel.writeAndFlush(data.retain())
                .addListener(future -> {
                    if (!future.isSuccess()) {
                        log.error("转发数据到MongoDB后端失败: {}", future.cause().getMessage());
                        ctx.close();
                    }
                });

        } catch (Exception e) {
            log.error("处理MongoDB消息失败: {}", e.getMessage(), e);
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
        log.error("MongoDB代理连接异常: {} - {}", clientIP, cause.getMessage());

        // 记录安全日志
        operationLogService.logSecurityEvent(
            null,
            clientIP,
            "CONNECTION_ERROR",
            "MONGODB",
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
        log.info("MongoDB客户端连接断开: {}", clientIP);

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
     * 创建到后端MongoDB的连接
     */
    private void createBackendConnection(ChannelHandlerContext clientCtx) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(clientCtx.channel().eventLoop())
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast(new BackendMongoHandler(clientCtx));
                }
            });

        // 连接后端MongoDB服务器
        bootstrap.connect(backendHost, backendPort)
            .addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    Channel backendChannel = future.channel();

                    // 建立双向连接映射
                    clientToBackendChannelMap.put(clientCtx.channel().id(), backendChannel);
                    backendToClientChannelMap.put(backendChannel.id(), clientCtx.channel());

                    // 初始化连接状态
                    connectionStates.put(clientCtx.channel().id(),
                        new MongoConnectionState(getClientIP(clientCtx)));

                    log.info("成功建立MongoDB后端连接: {} -> {}:{}",
                        getClientIP(clientCtx), backendHost, backendPort);

                } else {
                    log.error("无法连接到MongoDB后端服务器: {}:{} - {}",
                        backendHost, backendPort, future.cause().getMessage());
                    clientCtx.close();
                }
            });
    }

    /**
     * 解析并记录MongoDB协议消息
     */
    private void parseAndLogMongoMessage(ChannelHandlerContext ctx, ByteBuf data, String clientIP) {
        try {
            if (data.readableBytes() < 16) {
                return; // 消息头不完整
            }

            data.markReaderIndex();

            // 读取MongoDB消息头 (小端序)
            int messageLength = data.readIntLE();
            int requestId = data.readIntLE();
            int responseTo = data.readIntLE();
            int opCode = data.readIntLE();

            data.resetReaderIndex();

            MongoConnectionState state = connectionStates.get(ctx.channel().id());
            if (state == null) {
                return;
            }

            String operation = getOperationName(opCode);
            String content = "";
            String databaseName = "";
            String collectionName = "";

            // 根据操作码解析消息内容
            switch (opCode) {
                case OP_MSG:
                    // MongoDB 3.6+ 主要使用OP_MSG
                    MongoMessage msgContent = parseOpMsg(data.duplicate());
                    content = msgContent.getCommand();
                    databaseName = msgContent.getDatabase();
                    collectionName = msgContent.getCollection();
                    break;

                case OP_QUERY:
                    MongoMessage queryContent = parseOpQuery(data.duplicate());
                    content = queryContent.getCommand();
                    databaseName = queryContent.getDatabase();
                    collectionName = queryContent.getCollection();
                    break;

                case OP_INSERT:
                case OP_UPDATE:
                case OP_DELETE:
                    // 解析其他操作类型
                    content = parseBasicOperation(data.duplicate(), opCode);
                    break;

                default:
                    content = "Unknown operation: " + opCode;
            }

            // 检查是否为危险操作
            if (isDangerousOperation(content, databaseName, collectionName)) {
                log.warn("检测到危险MongoDB操作，拒绝执行: {} - {}", clientIP, content);

                // 发送错误响应给客户端
                sendErrorResponse(ctx, "操作被代理拦截：" + content, requestId);

                // 记录安全日志
                operationLogService.logSecurityEvent(
                    state.getFamilyId(),
                    clientIP,
                    "DANGEROUS_OPERATION_BLOCKED",
                    "MONGODB",
                    "被拦截的命令: " + content,
                    "HIGH"
                );
                return;
            }

            // 记录操作日志
            operationLogService.logOperation(
                state.getFamilyId(),
                "MONGODB",
                clientIP,
                state.getUserId(),
                operation,
                databaseName,
                collectionName,
                content,
                0,
                "IN_PROGRESS",
                null
            );

        } catch (Exception e) {
            log.warn("解析MongoDB消息失败: {}", e.getMessage());
        }
    }

    /**
     * 解析OP_MSG消息 (MongoDB 3.6+)
     */
    private MongoMessage parseOpMsg(ByteBuf data) {
        try {
            data.skipBytes(16); // 跳过消息头

            if (data.readableBytes() < 4) {
                return new MongoMessage("", "", "");
            }

            int flagBits = data.readIntLE();

            // 读取第一个section (通常是命令文档)
            if (data.readableBytes() < 1) {
                return new MongoMessage("", "", "");
            }

            byte sectionKind = data.readByte();
            if (sectionKind == 0) {
                // Body section - BSON文档
                String bsonContent = parseBSONDocument(data);
                return parseCommandFromBSON(bsonContent);
            }

            return new MongoMessage("", "", "");

        } catch (Exception e) {
            log.debug("解析OP_MSG失败: {}", e.getMessage());
            return new MongoMessage("", "", "");
        }
    }

    /**
     * 解析OP_QUERY消息
     */
    private MongoMessage parseOpQuery(ByteBuf data) {
        try {
            data.skipBytes(16); // 跳过消息头
            data.skipBytes(4);  // 跳过flags

            // 读取完整集合名称 (database.collection)
            String fullCollectionName = readCString(data);
            String[] parts = fullCollectionName.split("\\.", 2);
            String database = parts.length > 0 ? parts[0] : "";
            String collection = parts.length > 1 ? parts[1] : "";

            data.skipBytes(8); // 跳过numberToSkip和numberToReturn

            // 读取查询文档
            String queryDoc = parseBSONDocument(data);

            return new MongoMessage("find", database, collection);

        } catch (Exception e) {
            log.debug("解析OP_QUERY失败: {}", e.getMessage());
            return new MongoMessage("", "", "");
        }
    }

    /**
     * 简化的BSON文档解析
     */
    private String parseBSONDocument(ByteBuf data) {
        try {
            if (data.readableBytes() < 4) {
                return "";
            }

            int docLength = data.readIntLE();
            if (docLength > data.readableBytes() + 4) {
                return "";
            }

            // 简化处理：只读取前面的字段来识别命令
            StringBuilder result = new StringBuilder();
            int bytesRead = 4;

            while (bytesRead < docLength - 1 && data.readableBytes() > 0) {
                byte elementType = data.readByte();
                bytesRead++;

                if (elementType == 0) break; // 文档结束

                String fieldName = readCString(data);
                bytesRead += fieldName.length() + 1;

                if (result.length() == 0) {
                    result.append(fieldName); // 第一个字段通常是命令名
                }

                // 跳过字段值 (简化处理)
                int valueSize = getValueSize(elementType, data);
                data.skipBytes(valueSize);
                bytesRead += valueSize;

                if (bytesRead >= docLength - 1) break;
            }

            return result.toString();

        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 从BSON内容解析命令信息
     */
    private MongoMessage parseCommandFromBSON(String bsonContent) {
        // 简化的命令解析
        String command = bsonContent.toLowerCase();
        String database = "";
        String collection = "";

        if (command.contains("find")) {
            return new MongoMessage("find", database, collection);
        } else if (command.contains("insert")) {
            return new MongoMessage("insert", database, collection);
        } else if (command.contains("update")) {
            return new MongoMessage("update", database, collection);
        } else if (command.contains("delete")) {
            return new MongoMessage("delete", database, collection);
        } else if (command.contains("drop")) {
            return new MongoMessage("drop", database, collection);
        }

        return new MongoMessage(bsonContent, database, collection);
    }

    /**
     * 读取C风格字符串
     */
    private String readCString(ByteBuf data) {
        StringBuilder sb = new StringBuilder();
        while (data.readableBytes() > 0) {
            byte b = data.readByte();
            if (b == 0) break;
            sb.append((char) b);
        }
        return sb.toString();
    }

    /**
     * 获取BSON值的大小
     */
    private int getValueSize(byte elementType, ByteBuf data) {
        switch (elementType) {
            case 0x01: return 8;  // double
            case 0x02: return data.readIntLE(); // string
            case 0x03: return data.readIntLE(); // document
            case 0x04: return data.readIntLE(); // array
            case 0x05: return data.readIntLE() + 1; // binary
            case 0x07: return 12; // ObjectId
            case 0x08: return 1;  // boolean
            case 0x09: return 8;  // UTC datetime
            case 0x0A: return 0;  // null
            case 0x10: return 4;  // int32
            case 0x12: return 8;  // int64
            default: return 0;
        }
    }

    /**
     * 解析基本操作
     */
    private String parseBasicOperation(ByteBuf data, int opCode) {
        switch (opCode) {
            case OP_INSERT:
                return "insert";
            case OP_UPDATE:
                return "update";
            case OP_DELETE:
                return "delete";
            default:
                return "unknown";
        }
    }

    /**
     * 获取操作名称
     */
    private String getOperationName(int opCode) {
        switch (opCode) {
            case OP_REPLY: return "REPLY";
            case OP_UPDATE: return "UPDATE";
            case OP_INSERT: return "INSERT";
            case OP_QUERY: return "QUERY";
            case OP_GET_MORE: return "GET_MORE";
            case OP_DELETE: return "DELETE";
            case OP_KILL_CURSORS: return "KILL_CURSORS";
            case OP_MSG: return "MSG";
            default: return "UNKNOWN_" + opCode;
        }
    }

    /**
     * 检查是否为危险操作
     */
    private boolean isDangerousOperation(String command, String database, String collection) {
        if (command == null || command.trim().isEmpty()) {
            return false;
        }

        String lowerCommand = command.toLowerCase();

        // 危险操作列表
        List<String> dangerousCommands = Arrays.asList(
            "dropdatabase", "drop",
            "shutdown", "replication",
            "eval", "$where",
            "mapreduced", "mapreducemerge"
        );

        return dangerousCommands.stream().anyMatch(lowerCommand::contains);
    }

    /**
     * 发送错误响应给客户端
     */
    private void sendErrorResponse(ChannelHandlerContext ctx, String errorMessage, int requestId) {
        try {
            // 构造MongoDB错误响应 (简化版本)
            ByteBuf response = Unpooled.buffer();

            // 构造错误文档的BSON
            String errorDoc = "{\"ok\": 0, \"errmsg\": \"" + errorMessage + "\", \"code\": 13}";
            byte[] errorBytes = errorDoc.getBytes(StandardCharsets.UTF_8);

            // MongoDB响应头
            response.writeIntLE(36 + errorBytes.length); // 消息长度
            response.writeIntLE((int) System.currentTimeMillis()); // 响应ID
            response.writeIntLE(requestId); // 响应目标
            response.writeIntLE(OP_REPLY); // 操作码

            // OP_REPLY 标志位
            response.writeIntLE(2); // ResponseFlag: QueryFailure
            response.writeLongLE(0); // CursorID
            response.writeIntLE(0); // StartingFrom
            response.writeIntLE(1); // NumberReturned

            // 错误文档
            response.writeBytes(errorBytes);

            ctx.writeAndFlush(response)
                .addListener(ChannelFutureListener.CLOSE);

        } catch (Exception e) {
            log.error("发送MongoDB错误响应失败", e);
            ctx.close();
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIP(ChannelHandlerContext ctx) {
        return ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
    }

    /**
     * 后端MongoDB连接处理器
     */
    private class BackendMongoHandler extends ChannelInboundHandlerAdapter {
        private final ChannelHandlerContext clientCtx;

        public BackendMongoHandler(ChannelHandlerContext clientCtx) {
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
            log.error("MongoDB后端连接异常: {}", cause.getMessage());
            ctx.close();
            if (clientCtx.channel().isActive()) {
                clientCtx.close();
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.info("MongoDB后端连接断开");
            if (clientCtx.channel().isActive()) {
                clientCtx.close();
            }
        }
    }

    /**
     * MongoDB连接状态
     */
    private static class MongoConnectionState {
        private final String clientIP;
        private String familyId;
        private String userId;
        private String currentDatabase;

        public MongoConnectionState(String clientIP) {
            this.clientIP = clientIP;
        }

        // Getters and Setters
        public String getClientIP() { return clientIP; }
        public String getFamilyId() { return familyId; }
        public void setFamilyId(String familyId) { this.familyId = familyId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getCurrentDatabase() { return currentDatabase; }
        public void setCurrentDatabase(String currentDatabase) { this.currentDatabase = currentDatabase; }
    }

    /**
     * MongoDB消息内容
     */
    private static class MongoMessage {
        private final String command;
        private final String database;
        private final String collection;

        public MongoMessage(String command, String database, String collection) {
            this.command = command;
            this.database = database;
            this.collection = collection;
        }

        public String getCommand() { return command; }
        public String getDatabase() { return database; }
        public String getCollection() { return collection; }
    }
}