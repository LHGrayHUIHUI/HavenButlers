package com.haven.common.mq;

import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 消息发送器
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "common.mq", name = "enabled", havingValue = "true")
public class MessageSender {

    private final RabbitTemplate rabbitTemplate;

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
        try {
            if (messageId == null) {
                messageId = UUID.randomUUID().toString();
            }

            Message message = MessageBuilder
                    .withBody(JSON.toJSONString(data).getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setContentEncoding("UTF-8")
                    .setMessageId(messageId)
                    .setTimestamp(System.currentTimeMillis())
                    .build();

            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            log.debug("发送消息成功: exchange={}, routingKey={}, messageId={}",
                    exchange, routingKey, messageId);
        } catch (Exception e) {
            log.error("发送消息失败: exchange={}, routingKey={}, data={}",
                    exchange, routingKey, data, e);
            throw new RuntimeException("消息发送失败", e);
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
        try {
            if (messageId == null) {
                messageId = UUID.randomUUID().toString();
            }

            Message message = MessageBuilder
                    .withBody(JSON.toJSONString(data).getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setContentEncoding("UTF-8")
                    .setMessageId(messageId)
                    .setTimestamp(System.currentTimeMillis())
                    .setHeader("x-delay", delayMs)
                    .build();

            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            log.debug("发送延迟消息成功: exchange={}, routingKey={}, messageId={}, delay={}ms",
                    exchange, routingKey, messageId, delayMs);
        } catch (Exception e) {
            log.error("发送延迟消息失败: exchange={}, routingKey={}, data={}, delay={}ms",
                    exchange, routingKey, data, delayMs, e);
            throw new RuntimeException("延迟消息发送失败", e);
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
            priority = Math.max(0, Math.min(10, priority)); // 限制优先级范围

            Message message = MessageBuilder
                    .withBody(JSON.toJSONString(data).getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setContentEncoding("UTF-8")
                    .setMessageId(messageId)
                    .setTimestamp(System.currentTimeMillis())
                    .setPriority(priority)
                    .build();

            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            log.debug("发送优先级消息成功: exchange={}, routingKey={}, messageId={}, priority={}",
                    exchange, routingKey, messageId, priority);
        } catch (Exception e) {
            log.error("发送优先级消息失败: exchange={}, routingKey={}, data={}, priority={}",
                    exchange, routingKey, data, priority, e);
            throw new RuntimeException("优先级消息发送失败", e);
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
}