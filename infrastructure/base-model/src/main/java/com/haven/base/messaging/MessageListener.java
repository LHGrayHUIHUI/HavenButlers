package com.haven.base.messaging;

import java.util.Map;

/**
 * 统一消息监听器接口
 * 为微服务架构提供标准化的消息消费能力，支持异步处理、失败重试等
 *
 * 功能特性：
 * - 消息消费处理
 * - 消费确认机制
 * - 失败重试和死信处理
 * - 消息过滤支持
 * - 并发消费控制
 * - 消费进度跟踪
 *
 * @author HavenButler
 */
public interface MessageListener<T> {

    /**
     * 处理接收到的消息
     *
     * @param message 接收到的消息
     * @return 处理结果
     */
    ConsumeResult consume(MessageContext<T> message);

    /**
     * 获取监听器名称
     * 用于标识和日志记录
     *
     * @return 监听器名称
     */
    default String getListenerName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 获取监听的主题
     *
     * @return 主题名称
     */
    String getTopic();

    /**
     * 获取消费者组
     * 用于消息负载均衡和消费进度管理
     *
     * @return 消费者组名称
     */
    default String getConsumerGroup() {
        return getListenerName() + "-group";
    }

    /**
     * 获取最大重试次数
     *
     * @return 重试次数
     */
    default int getMaxRetries() {
        return 3;
    }

    /**
     * 获取并发消费线程数
     *
     * @return 线程数
     */
    default int getConcurrency() {
        return 1;
    }

    /**
     * 是否启用消息过滤
     * 返回true时会调用filter方法进行消息过滤
     *
     * @return 是否启用过滤
     */
    default boolean isFilterEnabled() {
        return false;
    }

    /**
     * 消息过滤逻辑
     * 仅当isFilterEnabled()返回true时生效
     *
     * @param message 待过滤的消息
     * @return true表示消息通过过滤，false表示忽略消息
     */
    default boolean filter(MessageContext<T> message) {
        return true;
    }

    /**
     * 消息处理失败时的回调
     * 可用于记录错误日志、发送告警等
     *
     * @param message 失败的消息
     * @param error 错误信息
     */
    default void onConsumeError(MessageContext<T> message, Throwable error) {
        // 默认空实现，子类可重写
    }

    /**
     * 消息处理成功时的回调
     *
     * @param message 成功处理的消息
     * @param result 处理结果
     */
    default void onConsumeSuccess(MessageContext<T> message, ConsumeResult result) {
        // 默认空实现，子类可重写
    }

    /**
     * 消息上下文
     * 包含消息内容及相关元数据
     */
    class MessageContext<T> {
        private final String messageId;
        private final String topic;
        private final String partition;
        private final long offset;
        private final T payload;
        private final Map<String, Object> properties;
        private final long timestamp;
        private final int retryCount;
        private final String traceId;

        public MessageContext(String messageId, String topic, String partition, long offset,
                            T payload, Map<String, Object> properties, long timestamp,
                            int retryCount, String traceId) {
            this.messageId = messageId;
            this.topic = topic;
            this.partition = partition;
            this.offset = offset;
            this.payload = payload;
            this.properties = properties;
            this.timestamp = timestamp;
            this.retryCount = retryCount;
            this.traceId = traceId;
        }

        // Getters
        public String getMessageId() { return messageId; }
        public String getTopic() { return topic; }
        public String getPartition() { return partition; }
        public long getOffset() { return offset; }
        public T getPayload() { return payload; }
        public Map<String, Object> getProperties() { return properties; }
        public long getTimestamp() { return timestamp; }
        public int getRetryCount() { return retryCount; }
        public String getTraceId() { return traceId; }

        /**
         * 获取消息属性
         */
        @SuppressWarnings("unchecked")
        public <V> V getProperty(String key, Class<V> type) {
            Object value = properties.get(key);
            return type.isInstance(value) ? type.cast(value) : null;
        }

        /**
         * 获取字符串属性
         */
        public String getStringProperty(String key) {
            return getProperty(key, String.class);
        }

        /**
         * 获取用户ID
         */
        public String getUserId() {
            return getStringProperty(MessageProducer.MessageProperties.USER_ID);
        }

        /**
         * 获取设备ID
         */
        public String getDeviceId() {
            return getStringProperty(MessageProducer.MessageProperties.DEVICE_ID);
        }

        /**
         * 获取源服务
         */
        public String getSourceService() {
            return getStringProperty(MessageProducer.MessageProperties.SOURCE_SERVICE);
        }

        /**
         * 是否是重试消息
         */
        public boolean isRetryMessage() {
            return retryCount > 0;
        }
    }

    /**
     * 消费结果
     */
    class ConsumeResult {
        private final ConsumeStatus status;
        private final String message;
        private final boolean shouldRetry;
        private final Throwable error;

        private ConsumeResult(ConsumeStatus status, String message, boolean shouldRetry, Throwable error) {
            this.status = status;
            this.message = message;
            this.shouldRetry = shouldRetry;
            this.error = error;
        }

        /**
         * 消费成功
         */
        public static ConsumeResult success() {
            return success("处理成功");
        }

        /**
         * 消费成功（带消息）
         */
        public static ConsumeResult success(String message) {
            return new ConsumeResult(ConsumeStatus.SUCCESS, message, false, null);
        }

        /**
         * 消费失败，需要重试
         */
        public static ConsumeResult retryLater(String message) {
            return new ConsumeResult(ConsumeStatus.RETRY_LATER, message, true, null);
        }

        /**
         * 消费失败，需要重试（带异常）
         */
        public static ConsumeResult retryLater(String message, Throwable error) {
            return new ConsumeResult(ConsumeStatus.RETRY_LATER, message, true, error);
        }

        /**
         * 消费失败，直接丢弃
         */
        public static ConsumeResult discard(String message) {
            return new ConsumeResult(ConsumeStatus.DISCARD, message, false, null);
        }

        /**
         * 消费失败，直接丢弃（带异常）
         */
        public static ConsumeResult discard(String message, Throwable error) {
            return new ConsumeResult(ConsumeStatus.DISCARD, message, false, error);
        }

        // Getters
        public ConsumeStatus getStatus() { return status; }
        public String getMessage() { return message; }
        public boolean shouldRetry() { return shouldRetry; }
        public Throwable getError() { return error; }

        public boolean isSuccess() { return status == ConsumeStatus.SUCCESS; }
        public boolean needsRetry() { return status == ConsumeStatus.RETRY_LATER; }
        public boolean shouldDiscard() { return status == ConsumeStatus.DISCARD; }
    }

    /**
     * 消费状态枚举
     */
    enum ConsumeStatus {
        /**
         * 消费成功
         */
        SUCCESS,

        /**
         * 消费失败，稍后重试
         */
        RETRY_LATER,

        /**
         * 消费失败，直接丢弃
         */
        DISCARD
    }

    /**
     * 抽象消息监听器基类
     * 提供通用的错误处理和日志记录
     */
    abstract class AbstractMessageListener<T> implements MessageListener<T> {

        protected final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

        @Override
        public final ConsumeResult consume(MessageContext<T> message) {
            long startTime = System.currentTimeMillis();
            String traceId = message.getTraceId();

            try {
                // 设置TraceID到MDC
                if (traceId != null) {
                    org.slf4j.MDC.put("traceId", traceId);
                }

                logger.info("开始处理消息: messageId={}, topic={}, retryCount={}",
                        message.getMessageId(), message.getTopic(), message.getRetryCount());

                // 消息过滤
                if (isFilterEnabled() && !filter(message)) {
                    logger.debug("消息被过滤器拒绝: messageId={}", message.getMessageId());
                    return ConsumeResult.success("消息被过滤");
                }

                // 执行业务逻辑
                ConsumeResult result = doConsume(message);

                long duration = System.currentTimeMillis() - startTime;
                logger.info("消息处理完成: messageId={}, status={}, duration={}ms",
                        message.getMessageId(), result.getStatus(), duration);

                // 成功回调
                if (result.isSuccess()) {
                    onConsumeSuccess(message, result);
                }

                return result;

            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                logger.error("消息处理异常: messageId={}, duration={}ms, error={}",
                        message.getMessageId(), duration, e.getMessage(), e);

                // 错误回调
                onConsumeError(message, e);

                // 根据异常类型决定是否重试
                if (shouldRetryOnError(e)) {
                    return ConsumeResult.retryLater("处理异常: " + e.getMessage(), e);
                } else {
                    return ConsumeResult.discard("不可重试的异常: " + e.getMessage(), e);
                }

            } finally {
                // 清理MDC
                org.slf4j.MDC.remove("traceId");
            }
        }

        /**
         * 子类实现具体的消息处理逻辑
         */
        protected abstract ConsumeResult doConsume(MessageContext<T> message);

        /**
         * 判断异常是否需要重试
         * 子类可重写此方法自定义重试逻辑
         */
        protected boolean shouldRetryOnError(Throwable error) {
            // 默认对所有异常都重试，除了参数错误等明显不需要重试的异常
            return !(error instanceof IllegalArgumentException ||
                    error instanceof SecurityException ||
                    error instanceof UnsupportedOperationException);
        }
    }
}