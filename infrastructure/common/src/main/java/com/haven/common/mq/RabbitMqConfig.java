package com.haven.common.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类 - 基于base-model规范
 * 支持配置前缀迁移和向后兼容
 *
 * @author HavenButler
 * @version 2.0.0 - 对齐base-model消息规范
 */
@Slf4j
@Configuration
@ConditionalOnProperty(
    prefix = "base-model.messaging",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class RabbitMqConfig {

    /**
     * 交换机名称前缀
     */
    private static final String EXCHANGE_PREFIX = "haven.";

    /**
     * 队列名称前缀
     */
    private static final String QUEUE_PREFIX = "haven.";

    /**
     * 死信交换机
     */
    public static final String DLX_EXCHANGE = EXCHANGE_PREFIX + "dlx";

    /**
     * 死信队列
     */
    public static final String DLX_QUEUE = QUEUE_PREFIX + "dlx";

    /**
     * 设备控制交换机
     */
    public static final String DEVICE_EXCHANGE = EXCHANGE_PREFIX + "device";

    /**
     * 设备控制队列
     */
    public static final String DEVICE_CONTROL_QUEUE = QUEUE_PREFIX + "device.control";

    /**
     * 设备状态队列
     */
    public static final String DEVICE_STATUS_QUEUE = QUEUE_PREFIX + "device.status";

    /**
     * 消息通知交换机
     */
    public static final String MESSAGE_EXCHANGE = EXCHANGE_PREFIX + "message";

    /**
     * 消息通知队列
     */
    public static final String MESSAGE_QUEUE = QUEUE_PREFIX + "message.notification";

    /**
     * AI处理交换机
     */
    public static final String AI_EXCHANGE = EXCHANGE_PREFIX + "ai";

    /**
     * AI任务队列
     */
    public static final String AI_TASK_QUEUE = QUEUE_PREFIX + "ai.task";

    /**
     * 日志交换机
     */
    public static final String LOG_EXCHANGE = EXCHANGE_PREFIX + "log";

    /**
     * 日志队列
     */
    public static final String LOG_QUEUE = QUEUE_PREFIX + "log";

    /**
     * JSON消息转换器
     */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());

        // 设置确认回调
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.debug("消息发送成功: {}", correlationData);
            } else {
                log.error("消息发送失败: {}, 原因: {}", correlationData, cause);
            }
        });

        // 设置返回回调
        template.setReturnsCallback(returned -> {
            log.error("消息被退回: {}", returned);
        });

        template.setMandatory(true);
        return template;
    }

    /**
     * 死信交换机
     */
    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    /**
     * 死信队列
     */
    @Bean
    public Queue dlxQueue() {
        return QueueBuilder.durable(DLX_QUEUE)
                .ttl(86400000) // 24小时
                .build();
    }

    /**
     * 死信绑定
     */
    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlxQueue())
                .to(dlxExchange())
                .with("dlx");
    }

    /**
     * 设备控制交换机
     */
    @Bean
    public TopicExchange deviceExchange() {
        return new TopicExchange(DEVICE_EXCHANGE, true, false);
    }

    /**
     * 设备控制队列
     */
    @Bean
    public Queue deviceControlQueue() {
        return QueueBuilder.durable(DEVICE_CONTROL_QUEUE)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey("dlx")
                .maxLength(10000)
                .build();
    }

    /**
     * 设备状态队列
     */
    @Bean
    public Queue deviceStatusQueue() {
        return QueueBuilder.durable(DEVICE_STATUS_QUEUE)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey("dlx")
                .maxLength(10000)
                .build();
    }

    /**
     * 设备控制绑定
     */
    @Bean
    public Binding deviceControlBinding() {
        return BindingBuilder.bind(deviceControlQueue())
                .to(deviceExchange())
                .with("device.control.*");
    }

    /**
     * 设备状态绑定
     */
    @Bean
    public Binding deviceStatusBinding() {
        return BindingBuilder.bind(deviceStatusQueue())
                .to(deviceExchange())
                .with("device.status.*");
    }

    /**
     * 消息通知交换机
     */
    @Bean
    public TopicExchange messageExchange() {
        return new TopicExchange(MESSAGE_EXCHANGE, true, false);
    }

    /**
     * 消息通知队列
     */
    @Bean
    public Queue messageQueue() {
        return QueueBuilder.durable(MESSAGE_QUEUE)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey("dlx")
                .maxLength(50000)
                .build();
    }

    /**
     * 消息通知绑定
     */
    @Bean
    public Binding messageBinding() {
        return BindingBuilder.bind(messageQueue())
                .to(messageExchange())
                .with("message.*");
    }

    /**
     * AI处理交换机
     */
    @Bean
    public TopicExchange aiExchange() {
        return new TopicExchange(AI_EXCHANGE, true, false);
    }

    /**
     * AI任务队列
     */
    @Bean
    public Queue aiTaskQueue() {
        return QueueBuilder.durable(AI_TASK_QUEUE)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey("dlx")
                .maxLength(1000)
                .maxPriority(10)
                .build();
    }

    /**
     * AI任务绑定
     */
    @Bean
    public Binding aiTaskBinding() {
        return BindingBuilder.bind(aiTaskQueue())
                .to(aiExchange())
                .with("ai.task.*");
    }

    /**
     * 日志交换机
     */
    @Bean
    public FanoutExchange logExchange() {
        return new FanoutExchange(LOG_EXCHANGE, true, false);
    }

    /**
     * 日志队列
     */
    @Bean
    public Queue logQueue() {
        return QueueBuilder.durable(LOG_QUEUE)
                .ttl(604800000) // 7天
                .maxLength(100000)
                .build();
    }

    /**
     * 日志绑定
     */
    @Bean
    public Binding logBinding() {
        return BindingBuilder.bind(logQueue())
                .to(logExchange());
    }
}