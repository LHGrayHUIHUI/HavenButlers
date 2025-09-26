package com.haven.base.messaging;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 统一消息生产者接口
 * 为微服务架构提供标准化的消息发送能力，支持异步解耦、事件驱动架构
 *
 * 功能特性：
 * - 同步/异步消息发送
 * - 消息路由和分区支持
 * - 消息持久化保证
 * - 发送确认机制
 * - 消息重试和死信队列
 * - 支持多种消息中间件（RabbitMQ、Kafka、RocketMQ等）
 *
 * @author HavenButler
 */
public interface MessageProducer {

    /**
     * 发送消息到指定主题
     *
     * @param topic 主题名称
     * @param message 消息内容
     * @return 发送结果
     */
    SendResult send(String topic, Object message);

    /**
     * 发送消息到指定主题和分区
     *
     * @param topic 主题名称
     * @param partition 分区键，用于消息路由
     * @param message 消息内容
     * @return 发送结果
     */
    SendResult send(String topic, String partition, Object message);

    /**
     * 发送消息（带自定义属性）
     *
     * @param topic 主题名称
     * @param message 消息内容
     * @param properties 消息属性
     * @return 发送结果
     */
    SendResult send(String topic, Object message, Map<String, Object> properties);

    /**
     * 异步发送消息
     *
     * @param topic 主题名称
     * @param message 消息内容
     * @return 发送结果的Future
     */
    CompletableFuture<SendResult> sendAsync(String topic, Object message);

    /**
     * 异步发送消息（带分区和属性）
     *
     * @param topic 主题名称
     * @param partition 分区键
     * @param message 消息内容
     * @param properties 消息属性
     * @return 发送结果的Future
     */
    CompletableFuture<SendResult> sendAsync(String topic, String partition,
                                           Object message, Map<String, Object> properties);

    /**
     * 发送延时消息
     *
     * @param topic 主题名称
     * @param message 消息内容
     * @param delay 延时时间
     * @return 发送结果
     */
    SendResult sendDelayed(String topic, Object message, Duration delay);

    /**
     * 发送事务消息
     *
     * @param topic 主题名称
     * @param message 消息内容
     * @param transactionId 事务ID
     * @return 发送结果
     */
    SendResult sendTransactional(String topic, Object message, String transactionId);

    /**
     * 批量发送消息
     *
     * @param topic 主题名称
     * @param messages 消息列表
     * @return 批量发送结果
     */
    BatchSendResult sendBatch(String topic, java.util.List<Object> messages);

    /**
     * 发送消息结果
     */
    class SendResult {
        private final boolean success;
        private final String messageId;
        private final String topic;
        private final String partition;
        private final long offset;
        private final long timestamp;
        private final Throwable error;

        public SendResult(boolean success, String messageId, String topic,
                         String partition, long offset, long timestamp, Throwable error) {
            this.success = success;
            this.messageId = messageId;
            this.topic = topic;
            this.partition = partition;
            this.offset = offset;
            this.timestamp = timestamp;
            this.error = error;
        }

        // 成功结果构造器
        public static SendResult success(String messageId, String topic, String partition, long offset) {
            return new SendResult(true, messageId, topic, partition, offset, System.currentTimeMillis(), null);
        }

        // 失败结果构造器
        public static SendResult failure(String topic, Throwable error) {
            return new SendResult(false, null, topic, null, -1, System.currentTimeMillis(), error);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessageId() { return messageId; }
        public String getTopic() { return topic; }
        public String getPartition() { return partition; }
        public long getOffset() { return offset; }
        public long getTimestamp() { return timestamp; }
        public Throwable getError() { return error; }

        public boolean isFailure() { return !success; }
    }

    /**
     * 批量发送结果
     */
    class BatchSendResult {
        private final int total;
        private final int success;
        private final int failed;
        private final java.util.List<SendResult> results;

        public BatchSendResult(java.util.List<SendResult> results) {
            this.results = results;
            this.total = results.size();
            this.success = (int) results.stream().filter(SendResult::isSuccess).count();
            this.failed = total - success;
        }

        public int getTotal() { return total; }
        public int getSuccess() { return success; }
        public int getFailed() { return failed; }
        public java.util.List<SendResult> getResults() { return results; }
        public boolean isAllSuccess() { return failed == 0; }
        public boolean hasFailures() { return failed > 0; }

        public java.util.List<SendResult> getFailedResults() {
            return results.stream().filter(SendResult::isFailure).toList();
        }
    }

    /**
     * 消息发送配置
     */
    class SendConfig {
        private Duration timeout = Duration.ofSeconds(5);
        private int retries = 3;
        private boolean persistent = true;
        private String compressionType = "none";
        private int batchSize = 100;
        private Duration batchTimeout = Duration.ofMillis(100);

        // Getters and Setters
        public Duration getTimeout() { return timeout; }
        public SendConfig setTimeout(Duration timeout) { this.timeout = timeout; return this; }

        public int getRetries() { return retries; }
        public SendConfig setRetries(int retries) { this.retries = retries; return this; }

        public boolean isPersistent() { return persistent; }
        public SendConfig setPersistent(boolean persistent) { this.persistent = persistent; return this; }

        public String getCompressionType() { return compressionType; }
        public SendConfig setCompressionType(String compressionType) { this.compressionType = compressionType; return this; }

        public int getBatchSize() { return batchSize; }
        public SendConfig setBatchSize(int batchSize) { this.batchSize = batchSize; return this; }

        public Duration getBatchTimeout() { return batchTimeout; }
        public SendConfig setBatchTimeout(Duration batchTimeout) { this.batchTimeout = batchTimeout; return this; }
    }

    /**
     * 常用主题名称常量
     */
    class Topics {
        // 用户相关事件
        public static final String USER_REGISTERED = "user.registered";
        public static final String USER_LOGIN = "user.login";
        public static final String USER_LOGOUT = "user.logout";

        // 设备相关事件
        public static final String DEVICE_STATUS_CHANGED = "device.status.changed";
        public static final String DEVICE_COMMAND_EXECUTED = "device.command.executed";
        public static final String DEVICE_ALARM = "device.alarm";

        // AI服务事件
        public static final String AI_REQUEST_PROCESSED = "ai.request.processed";
        public static final String NLP_TEXT_ANALYZED = "nlp.text.analyzed";
        public static final String VOICE_RECOGNIZED = "voice.recognized";

        // 系统事件
        public static final String SYSTEM_ERROR = "system.error";
        public static final String SYSTEM_ALERT = "system.alert";
        public static final String AUDIT_LOG = "audit.log";

        // 业务事件
        public static final String ORDER_CREATED = "order.created";
        public static final String ORDER_COMPLETED = "order.completed";
        public static final String PAYMENT_PROCESSED = "payment.processed";
    }

    /**
     * 消息属性常量
     */
    class MessageProperties {
        public static final String TRACE_ID = "traceId";
        public static final String USER_ID = "userId";
        public static final String DEVICE_ID = "deviceId";
        public static final String TIMESTAMP = "timestamp";
        public static final String SOURCE_SERVICE = "sourceService";
        public static final String EVENT_TYPE = "eventType";
        public static final String RETRY_COUNT = "retryCount";
        public static final String CORRELATION_ID = "correlationId";
    }
}