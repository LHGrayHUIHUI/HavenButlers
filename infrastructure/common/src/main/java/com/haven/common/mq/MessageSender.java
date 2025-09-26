package com.haven.common.mq;

import com.haven.base.utils.TraceIdUtil;
import com.haven.common.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 消息发送器 - 基于base-model规范
 * 统一使用convertAndSend + Jackson2JsonMessageConverter序列化
 *
 * @author HavenButler
 * @version 2.0.0 - 对齐base-model消息规范
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "base-model.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MessageSender {

    private final RabbitTemplate rabbitTemplate;

    @Value("${base-model.messaging.enable-metrics:true}")
    private boolean enableMetrics;

    @Value("${base-model.messaging.default-timeout:10000}")
    private long defaultTimeout;

    // 消息统计指标
    private final AtomicLong messagesSent = new AtomicLong(0);
    private final AtomicLong messagesFailed = new AtomicLong(0);
    private final AtomicLong messagesDelayed = new AtomicLong(0);

    /**
     * 发送消息
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param data       消息内容
     */
    public void send(String exchange, String routingKey, Object data) {
        send(exchange, routingKey, data, null);
    }

    /**
     * 发送消息（带消息ID）
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param data       消息内容
     * @param messageId  消息ID
     */
    public void send(String exchange, String routingKey, Object data, String messageId) {
        if (exchange == null || routingKey == null || data == null) {
            throw new IllegalArgumentException("消息参数不能为空: exchange=" + exchange +
                ", routingKey=" + routingKey + ", data=" + data);
        }

        if (messageId == null) {
            messageId = UUID.randomUUID().toString();
        }
        final String finalMessageId = messageId;

        final String traceId = TraceIdUtil.getCurrent();
        long startTime = System.currentTimeMillis();

        try {
            CorrelationData correlationData = new CorrelationData(finalMessageId);

            rabbitTemplate.convertAndSend(exchange, routingKey, data, msg -> {
                MessageProperties props = msg.getMessageProperties();
                props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                props.setContentEncoding("UTF-8");
                props.setMessageId(finalMessageId);
                props.setTimestamp(new Date());

                // 设置追踪相关头
                props.setHeader("traceId", traceId);
                props.setHeader("sendTime", LocalDateTime.now().toString());
                props.setHeader("source", "haven-common-mq");

                return msg;
            }, correlationData);

            // 更新指标
            if (enableMetrics) {
                messagesSent.incrementAndGet();
            }

            long duration = System.currentTimeMillis() - startTime;
            log.debug("发送消息成功: exchange={}, routingKey={}, messageId={}, duration={}ms, traceId={}",
                    exchange, routingKey, messageId, duration, traceId);

        } catch (Exception e) {
            // 更新失败指标
            if (enableMetrics) {
                messagesFailed.incrementAndGet();
            }

            log.error("发送消息失败: exchange={}, routingKey={}, messageId={}, traceId={}",
                     exchange, routingKey, messageId, traceId, e);
            throw new CommonException.MqException("消息发送失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发送延迟消息
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param data       消息内容
     * @param delayMs    延迟时间（毫秒）
     */
    public void sendDelay(String exchange, String routingKey, Object data, long delayMs) {
        sendDelay(exchange, routingKey, data, delayMs, null);
    }

    /**
     * 发送延迟消息（带消息ID）
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param data       消息内容
     * @param delayMs    延迟时间（毫秒）
     * @param messageId  消息ID
     */
    public void sendDelay(String exchange, String routingKey, Object data, long delayMs, String messageId) {
        if (exchange == null || routingKey == null || data == null || delayMs < 0) {
            throw new IllegalArgumentException("延迟消息参数不能为空且延迟时间不能为负数");
        }

        if (messageId == null) {
            messageId = UUID.randomUUID().toString();
        }
        final String finalMessageId = messageId;

        final String traceId = TraceIdUtil.getCurrent();
        long startTime = System.currentTimeMillis();

        try {
            CorrelationData correlationData = new CorrelationData(finalMessageId);

            rabbitTemplate.convertAndSend(exchange, routingKey, data, msg -> {
                MessageProperties props = msg.getMessageProperties();
                props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                props.setContentEncoding("UTF-8");
                props.setMessageId(finalMessageId);
                props.setTimestamp(new Date());

                // 设置追踪相关头
                props.setHeader("traceId", traceId);
                props.setHeader("sendTime", LocalDateTime.now().toString());
                props.setHeader("source", "haven-common-mq");
                props.setHeader("x-delay", delayMs);
                props.setHeader("delayType", "delayed");

                return msg;
            }, correlationData);

            // 更新指标
            if (enableMetrics) {
                messagesSent.incrementAndGet();
                messagesDelayed.incrementAndGet();
            }

            long duration = System.currentTimeMillis() - startTime;
            log.debug("发送延迟消息成功: exchange={}, routingKey={}, messageId={}, delay={}ms, duration={}ms, traceId={}",
                    exchange, routingKey, messageId, delayMs, duration, traceId);

        } catch (Exception e) {
            // 更新失败指标
            if (enableMetrics) {
                messagesFailed.incrementAndGet();
            }

            log.error("发送延迟消息失败: exchange={}, routingKey={}, messageId={}, delay={}ms, traceId={}",
                     exchange, routingKey, messageId, delayMs, traceId, e);
            throw new CommonException.MqException("延迟消息发送失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发送优先级消息
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param data       消息内容
     * @param priority   优先级（0-10）
     */
    public void sendWithPriority(String exchange, String routingKey, Object data, int priority) {
        try {
            String messageId = UUID.randomUUID().toString();
            final int finalPriority = Math.max(0, Math.min(10, priority));
            final String finalMessageId = messageId;

            final String traceId = TraceIdUtil.getCurrent();
            CorrelationData correlationData = new CorrelationData(finalMessageId);

            rabbitTemplate.convertAndSend(exchange, routingKey, data, msg -> {
                MessageProperties props = msg.getMessageProperties();
                props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                props.setContentEncoding("UTF-8");
                props.setMessageId(finalMessageId);
                props.setTimestamp(new Date());
                props.setPriority(finalPriority);
                props.setHeader("traceId", traceId);
                return msg;
            }, correlationData);

            log.debug("发送优先级消息成功: exchange={}, routingKey={}, messageId={}, priority={}, traceId={}",
                    exchange, routingKey, messageId, priority, traceId);
        } catch (Exception e) {
            log.error("发送优先级消息失败: exchange={}, routingKey={}, data={}, priority={}",
                    exchange, routingKey, data, priority, e);
            throw new CommonException.MqException("优先级消息发送失败", e);
        }
    }

    /**
     * 发送设备控制消息
     *
     * @param deviceId 设备ID
     * @param command  控制命令
     */
    public void sendDeviceControl(String deviceId, Object command) {
        String routingKey = "device.control." + deviceId;
        send(RabbitMqConfig.DEVICE_EXCHANGE, routingKey, command);
    }

    /**
     * 发送设备状态消息
     *
     * @param deviceId 设备ID
     * @param status   状态信息
     */
    public void sendDeviceStatus(String deviceId, Object status) {
        String routingKey = "device.status." + deviceId;
        send(RabbitMqConfig.DEVICE_EXCHANGE, routingKey, status);
    }

    /**
     * 发送通知消息
     *
     * @param userId      用户ID
     * @param notification 通知内容
     */
    public void sendNotification(String userId, Object notification) {
        String routingKey = "message.notification";
        send(RabbitMqConfig.MESSAGE_EXCHANGE, routingKey, notification);
    }

    /**
     * 发送AI任务
     *
     * @param task     任务内容
     * @param priority 优先级
     */
    public void sendAiTask(Object task, int priority) {
        sendWithPriority(RabbitMqConfig.AI_EXCHANGE, "ai.task.process", task, priority);
    }

    /**
     * 发送日志消息
     *
     * @param logData 日志数据
     */
    public void sendLog(Object logData) {
        send(RabbitMqConfig.LOG_EXCHANGE, "", logData);
    }

    /**
     * 获取消息统计指标
     */
    public java.util.Map<String, Long> getMessageMetrics() {
        java.util.Map<String, Long> metrics = new java.util.HashMap<>();
        metrics.put("messagesSent", messagesSent.get());
        metrics.put("messagesFailed", messagesFailed.get());
        metrics.put("messagesDelayed", messagesDelayed.get());

        // 计算成功率和失败率
        long total = messagesSent.get() + messagesFailed.get();
        if (total > 0) {
            metrics.put("successRate", (messagesSent.get() * 100) / total);
            metrics.put("failureRate", (messagesFailed.get() * 100) / total);
            metrics.put("delayedRate", (messagesDelayed.get() * 100) / messagesSent.get());
        } else {
            metrics.put("successRate", 0L);
            metrics.put("failureRate", 0L);
            metrics.put("delayedRate", 0L);
        }

        return metrics;
    }

    /**
     * 重置消息指标
     */
    public void resetMessageMetrics() {
        messagesSent.set(0);
        messagesFailed.set(0);
        messagesDelayed.set(0);
        log.info("消息统计指标已重置");
    }

    /**
     * 检查RabbitTemplate是否可用
     */
    public boolean isHealthy() {
        try {
            // 简单检查连接工厂是否可用
            rabbitTemplate.getConnectionFactory().createConnection().close();
            return true;
        } catch (Exception e) {
            log.warn("RabbitMQ连接检查失败", e);
            return false;
        }
    }
}
