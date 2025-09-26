package com.haven.base.messaging;

import com.haven.base.utils.JsonUtil;
import com.haven.base.utils.TraceIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认消息生产者实现
 * 基于内存队列的简单实现，适合开发和测试环境
 * 生产环境建议使用RabbitMQ、Kafka、RocketMQ等专业消息中间件
 *
 * @author HavenButler
 */
@Slf4j
// 移除@Component注解，改由BaseModelAutoConfiguration中@Bean方式注册

public class DefaultMessageProducer implements MessageProducer {

    /**
     * 内存队列存储，仅用于演示和测试
     * 实际生产环境应该连接真实的消息中间件
     */
    private final Map<String, java.util.concurrent.BlockingQueue<MessageRecord>> topicQueues =
            new ConcurrentHashMap<>();

    private final SendConfig defaultConfig = new SendConfig();

    @Override
    public SendResult send(String topic, Object message) {
        return send(topic, message, null);
    }

    @Override
    public SendResult send(String topic, String partition, Object message) {
        return send(topic, message, buildDefaultProperties(partition));
    }

    @Override
    public SendResult send(String topic, Object message, Map<String, Object> properties) {
        try {
            String messageId = generateMessageId();
            String partition = extractPartition(properties);
            long timestamp = System.currentTimeMillis();

            // 构建消息记录
            MessageRecord record = new MessageRecord(
                    messageId, topic, partition, message, properties, timestamp
            );

            // 序列化消息（模拟发送过程）
            String serializedMessage = JsonUtil.toJson(message);
            log.debug("序列化消息: messageId={}, topic={}, size={}bytes",
                    messageId, topic, serializedMessage.length());

            // 存储到内存队列（模拟发送到消息中间件）
            storeMessage(topic, record);

            // 记录发送日志
            log.info("消息发送成功: messageId={}, topic={}, partition={}",
                    messageId, topic, partition);

            return SendResult.success(messageId, topic, partition, getNextOffset(topic));

        } catch (Exception e) {
            log.error("消息发送失败: topic={}, error={}", topic, e.getMessage(), e);
            return SendResult.failure(topic, e);
        }
    }

    @Override
    public CompletableFuture<SendResult> sendAsync(String topic, Object message) {
        return CompletableFuture.supplyAsync(() -> send(topic, message));
    }

    @Override
    public CompletableFuture<SendResult> sendAsync(String topic, String partition,
                                                  Object message, Map<String, Object> properties) {
        return CompletableFuture.supplyAsync(() -> send(topic, message,
                mergeProperties(buildDefaultProperties(partition), properties)));
    }

    @Override
    public SendResult sendDelayed(String topic, Object message, Duration delay) {
        // 模拟延时发送
        CompletableFuture.delayedExecutor(delay.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
                .execute(() -> send(topic, message));

        String messageId = generateMessageId();
        log.info("延时消息已调度: messageId={}, topic={}, delay={}ms",
                messageId, topic, delay.toMillis());

        return SendResult.success(messageId, topic, null, getNextOffset(topic));
    }

    @Override
    public SendResult sendTransactional(String topic, Object message, String transactionId) {
        // 模拟事务消息发送
        Map<String, Object> properties = buildDefaultProperties(null);
        properties.put("transactionId", transactionId);

        log.info("发送事务消息: topic={}, transactionId={}", topic, transactionId);
        return send(topic, message, properties);
    }

    @Override
    public BatchSendResult sendBatch(String topic, List<Object> messages) {
        List<SendResult> results = messages.stream()
                .map(message -> send(topic, message))
                .toList();

        BatchSendResult batchResult = new BatchSendResult(results);
        log.info("批量消息发送完成: topic={}, total={}, success={}, failed={}",
                topic, batchResult.getTotal(), batchResult.getSuccess(), batchResult.getFailed());

        return batchResult;
    }

    /**
     * 生成消息ID
     */
    private String generateMessageId() {
        return "msg-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 构建默认属性
     */
    private Map<String, Object> buildDefaultProperties(String partition) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(MessageProperties.TIMESTAMP, System.currentTimeMillis());
        properties.put(MessageProperties.TRACE_ID, TraceIdUtil.getCurrentOrGenerate());

        if (partition != null) {
            properties.put("partition", partition);
        }

        return properties;
    }

    /**
     * 合并属性
     */
    private Map<String, Object> mergeProperties(Map<String, Object> base, Map<String, Object> additional) {
        if (additional != null) {
            base.putAll(additional);
        }
        return base;
    }

    /**
     * 提取分区信息
     */
    private String extractPartition(Map<String, Object> properties) {
        if (properties == null) {
            return null;
        }
        Object partition = properties.get("partition");
        return partition != null ? partition.toString() : null;
    }

    /**
     * 存储消息到内存队列
     */
    private void storeMessage(String topic, MessageRecord record) {
        java.util.concurrent.BlockingQueue<MessageRecord> queue = topicQueues.computeIfAbsent(
                topic, k -> new java.util.concurrent.LinkedBlockingQueue<>()
        );
        queue.offer(record);
    }

    /**
     * 获取下一个偏移量
     */
    private long getNextOffset(String topic) {
        java.util.concurrent.BlockingQueue<MessageRecord> queue = topicQueues.get(topic);
        return queue != null ? queue.size() : 0;
    }

    /**
     * 消息记录（内部使用）
     */
    private static class MessageRecord {
        private final String messageId;
        private final String topic;
        private final String partition;
        private final Object payload;
        private final Map<String, Object> properties;
        private final long timestamp;

        public MessageRecord(String messageId, String topic, String partition,
                           Object payload, Map<String, Object> properties, long timestamp) {
            this.messageId = messageId;
            this.topic = topic;
            this.partition = partition;
            this.payload = payload;
            this.properties = properties;
            this.timestamp = timestamp;
        }

        // Getters
        public String getMessageId() { return messageId; }
        public String getTopic() { return topic; }
        public String getPartition() { return partition; }
        public Object getPayload() { return payload; }
        public Map<String, Object> getProperties() { return properties; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * 获取主题队列（用于测试和监控）
     */
    public java.util.concurrent.BlockingQueue<MessageRecord> getTopicQueue(String topic) {
        return topicQueues.get(topic);
    }

    /**
     * 获取所有主题
     */
    public java.util.Set<String> getTopics() {
        return topicQueues.keySet();
    }

    /**
     * 清空所有队列（用于测试）
     */
    public void clearAllQueues() {
        topicQueues.clear();
        log.info("所有消息队列已清空");
    }

    /**
     * 获取主题统计信息
     */
    public Map<String, Integer> getTopicStats() {
        Map<String, Integer> stats = new HashMap<>();
        topicQueues.forEach((topic, queue) -> stats.put(topic, queue.size()));
        return stats;
    }
}