package com.haven.admin.service;

import com.haven.admin.model.Alert;
import com.haven.admin.model.AlertRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * 通知服务
 * 处理邮件、Webhook等告警通知发送
 *
 * @author HavenButler
 */
@Slf4j
@Service
public class NotificationService {

    @Value("${alert.notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${alert.notification.email.smtp.host:}")
    private String smtpHost;

    @Value("${alert.notification.email.smtp.port:587}")
    private int smtpPort;

    @Value("${alert.notification.email.username:}")
    private String emailUsername;

    @Value("${alert.notification.email.password:}")
    private String emailPassword;

    @Value("${alert.notification.email.from:}")
    private String emailFrom;

    @Value("${alert.notification.email.to:}")
    private String emailTo;

    @Value("${alert.notification.webhook.url:}")
    private String webhookUrl;

    @Value("${alert.notification.webhook.timeout:10000}")
    private int webhookTimeout;

    @Autowired(required = false)
    private RestTemplate restTemplate;

    /**
     * 发送邮件通知
     */
    public boolean sendEmailNotification(Alert alert) {
        if (!emailEnabled || !isEmailConfigured()) {
            log.warn("邮件通知未启用或配置不完整，跳过发送");
            return false;
        }

        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", smtpPort);

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(emailUsername, emailPassword);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(emailFrom));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailTo));

            // 设置邮件主题和内容
            String subject = String.format("【%s】HavenButler告警通知 - %s",
                alert.getLevel().name(), alert.getServiceName());
            message.setSubject(subject, "UTF-8");

            String content = buildEmailContent(alert);
            message.setContent(content, "text/html; charset=utf-8");

            // 发送邮件
            Transport.send(message);

            log.info("邮件通知发送成功: alertId={}, to={}", alert.getId(), emailTo);
            return true;

        } catch (Exception e) {
            log.error("邮件通知发送失败: alertId={}, error={}", alert.getId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 发送Webhook通知
     */
    public boolean sendWebhookNotification(Alert alert) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            log.warn("Webhook URL未配置，跳过发送");
            return false;
        }

        try {
            if (restTemplate == null) {
                restTemplate = new RestTemplate();
            }

            // 构建Webhook请求体
            WebhookPayload payload = buildWebhookPayload(alert);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<WebhookPayload> entity = new HttpEntity<>(payload, headers);

            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                webhookUrl,
                HttpMethod.POST,
                entity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Webhook通知发送成功: alertId={}, response={}", alert.getId(), response.getStatusCode());
                return true;
            } else {
                log.error("Webhook通知发送失败: alertId={}, status={}", alert.getId(), response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            log.error("Webhook通知发送失败: alertId={}, error={}", alert.getId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 发送钉钉机器人通知
     */
    public boolean sendDingtalkNotification(Alert alert, String webhookUrl) {
        try {
            if (restTemplate == null) {
                restTemplate = new RestTemplate();
            }

            DingtalkPayload payload = DingtalkPayload.builder()
                .msgtype("markdown")
                .markdown(DingtalkPayload.Markdown.builder()
                    .title(String.format("HavenButler告警 - %s", alert.getServiceName()))
                    .text(buildDingtalkContent(alert))
                    .build())
                .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<DingtalkPayload> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                webhookUrl,
                HttpMethod.POST,
                entity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("钉钉通知发送成功: alertId={}", alert.getId());
                return true;
            } else {
                log.error("钉钉通知发送失败: alertId={}, status={}", alert.getId(), response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            log.error("钉钉通知发送失败: alertId={}, error={}", alert.getId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查邮件配置是否完整
     */
    private boolean isEmailConfigured() {
        return smtpHost != null && !smtpHost.trim().isEmpty() &&
               emailUsername != null && !emailUsername.trim().isEmpty() &&
               emailPassword != null && !emailPassword.trim().isEmpty() &&
               emailFrom != null && !emailFrom.trim().isEmpty() &&
               emailTo != null && !emailTo.trim().isEmpty();
    }

    /**
     * 构建邮件内容
     */
    private String buildEmailContent(Alert alert) {
        StringBuilder content = new StringBuilder();
        content.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>HavenButler告警通知</title></head><body>");
        content.append("<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>");

        // 标题
        String levelColor = getLevelColor(alert.getLevel());
        content.append("<div style='background-color: ").append(levelColor)
               .append("; color: white; padding: 20px; text-align: center;'>");
        content.append("<h1 style='margin: 0;'>HavenButler 智能家庭服务平台</h1>");
        content.append("<h2 style='margin: 10px 0 0 0;'>告警通知</h2>");
        content.append("</div>");

        // 告警信息
        content.append("<div style='padding: 20px; background-color: #f9f9f9;'>");
        content.append("<h3>告警详情</h3>");
        content.append("<table style='width: 100%; border-collapse: collapse;'>");

        content.append("<tr><td style='padding: 8px; border: 1px solid #ddd; font-weight: bold;'>服务名称</td>");
        content.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(alert.getServiceName()).append("</td></tr>");

        content.append("<tr><td style='padding: 8px; border: 1px solid #ddd; font-weight: bold;'>告警级别</td>");
        content.append("<td style='padding: 8px; border: 1px solid #ddd; color: ").append(levelColor).append(";'>");
        content.append(alert.getLevel().name()).append("</td></tr>");

        content.append("<tr><td style='padding: 8px; border: 1px solid #ddd; font-weight: bold;'>告警消息</td>");
        content.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(alert.getMessage()).append("</td></tr>");

        content.append("<tr><td style='padding: 8px; border: 1px solid #ddd; font-weight: bold;'>指标名称</td>");
        content.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(alert.getMetricName()).append("</td></tr>");

        content.append("<tr><td style='padding: 8px; border: 1px solid #ddd; font-weight: bold;'>当前值</td>");
        content.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(alert.getValue()).append("</td></tr>");

        content.append("<tr><td style='padding: 8px; border: 1px solid #ddd; font-weight: bold;'>阈值</td>");
        content.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(alert.getThreshold()).append("</td></tr>");

        content.append("<tr><td style='padding: 8px; border: 1px solid #ddd; font-weight: bold;'>实例ID</td>");
        content.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(alert.getInstanceId()).append("</td></tr>");

        content.append("<tr><td style='padding: 8px; border: 1px solid #ddd; font-weight: bold;'>触发时间</td>");
        content.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(alert.getTimestamp()).append("</td></tr>");

        content.append("</table>");
        content.append("</div>");

        // 操作建议
        content.append("<div style='padding: 20px;'>");
        content.append("<h3>操作建议</h3>");
        content.append("<ul>");
        content.append("<li>请登录管理控制台查看详细信息：<a href='http://localhost:8888'>http://localhost:8888</a></li>");
        content.append("<li>检查相关服务状态和日志</li>");
        content.append("<li>根据告警级别进行相应处理</li>");
        content.append("</ul>");
        content.append("</div>");

        // 页脚
        content.append("<div style='background-color: #f0f0f0; padding: 20px; text-align: center; color: #666;'>");
        content.append("<p>此邮件由 HavenButler 智能家庭服务平台自动发送</p>");
        content.append("<p>如无需接收此类邮件，请联系系统管理员</p>");
        content.append("</div>");

        content.append("</div></body></html>");

        return content.toString();
    }

    /**
     * 构建Webhook请求体
     */
    private WebhookPayload buildWebhookPayload(Alert alert) {
        return WebhookPayload.builder()
            .alertId(alert.getId())
            .serviceName(alert.getServiceName())
            .instanceId(alert.getInstanceId())
            .level(alert.getLevel().name())
            .message(alert.getMessage())
            .metricName(alert.getMetricName())
            .value(alert.getValue())
            .threshold(alert.getThreshold())
            .timestamp(alert.getTimestamp())
            .status(alert.getStatus().name())
            .build();
    }

    /**
     * 构建钉钉消息内容
     */
    private String buildDingtalkContent(Alert alert) {
        StringBuilder content = new StringBuilder();
        content.append("## HavenButler 告警通知\n\n");
        content.append("### 告警信息\n");
        content.append("- **服务名称**: ").append(alert.getServiceName()).append("\n");
        content.append("- **告警级别**: <font color=").append(getLevelColor(alert.getLevel())).append(">")
               .append(alert.getLevel().name()).append("</font>\n");
        content.append("- **告警消息**: ").append(alert.getMessage()).append("\n");
        content.append("- **指标名称**: ").append(alert.getMetricName()).append("\n");
        content.append("- **当前值**: ").append(alert.getValue()).append("\n");
        content.append("- **阈值**: ").append(alert.getThreshold()).append("\n");
        content.append("- **实例ID**: ").append(alert.getInstanceId()).append("\n");
        content.append("- **触发时间**: ").append(alert.getTimestamp()).append("\n\n");
        content.append("### 操作建议\n");
        content.append("1. 请登录管理控制台查看详细信息\n");
        content.append("2. 检查相关服务状态和日志\n");
        content.append("3. 根据告警级别进行相应处理\n\n");
        content.append("---\n");
        content.append("*HavenButler 智能家庭服务平台*");

        return content.toString();
    }

    /**
     * 获取告警级别对应的颜色
     */
    private String getLevelColor(AlertRule.AlertLevel level) {
        switch (level) {
            case CRITICAL: return "#ff4d4f";
            case WARNING: return "#faad14";
            case INFO: return "#1890ff";
            default: return "#666666";
        }
    }

    // Webhook请求体
    public static class WebhookPayload {
        private Long alertId;
        private String serviceName;
        private String instanceId;
        private String level;
        private String message;
        private String metricName;
        private Double value;
        private Double threshold;
        private Instant timestamp;
        private String status;

        // Builder模式
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private WebhookPayload payload = new WebhookPayload();

            public Builder alertId(Long alertId) {
                payload.alertId = alertId;
                return this;
            }

            public Builder serviceName(String serviceName) {
                payload.serviceName = serviceName;
                return this;
            }

            public Builder instanceId(String instanceId) {
                payload.instanceId = instanceId;
                return this;
            }

            public Builder level(String level) {
                payload.level = level;
                return this;
            }

            public Builder message(String message) {
                payload.message = message;
                return this;
            }

            public Builder metricName(String metricName) {
                payload.metricName = metricName;
                return this;
            }

            public Builder value(Double value) {
                payload.value = value;
                return this;
            }

            public Builder threshold(Double threshold) {
                payload.threshold = threshold;
                return this;
            }

            public Builder timestamp(Instant timestamp) {
                payload.timestamp = timestamp;
                return this;
            }

            public Builder status(String status) {
                payload.status = status;
                return this;
            }

            public WebhookPayload build() {
                return payload;
            }
        }

        // Getters and Setters
        public Long getAlertId() { return alertId; }
        public void setAlertId(Long alertId) { this.alertId = alertId; }
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        public String getInstanceId() { return instanceId; }
        public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getMetricName() { return metricName; }
        public void setMetricName(String metricName) { this.metricName = metricName; }
        public Double getValue() { return value; }
        public void setValue(Double value) { this.value = value; }
        public Double getThreshold() { return threshold; }
        public void setThreshold(Double threshold) { this.threshold = threshold; }
        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    // 钉钉请求体
    public static class DingtalkPayload {
        private String msgtype;
        private Markdown markdown;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private DingtalkPayload payload = new DingtalkPayload();

            public Builder msgtype(String msgtype) {
                payload.msgtype = msgtype;
                return this;
            }

            public Builder markdown(Markdown markdown) {
                payload.markdown = markdown;
                return this;
            }

            public DingtalkPayload build() {
                return payload;
            }
        }

        public static class Markdown {
            private String title;
            private String text;

            public static Builder builder() {
                return new Builder();
            }

            public static class Builder {
                private Markdown markdown = new Markdown();

                public Builder title(String title) {
                    markdown.title = title;
                    return this;
                }

                public Builder text(String text) {
                    markdown.text = text;
                    return this;
                }

                public Markdown build() {
                    return markdown;
                }
            }

            public String getTitle() { return title; }
            public void setTitle(String title) { this.title = title; }
            public String getText() { return text; }
            public void setText(String text) { this.text = text; }
        }

        public String getMsgtype() { return msgtype; }
        public void setMsgtype(String msgtype) { this.msgtype = msgtype; }
        public Markdown getMarkdown() { return markdown; }
        public void setMarkdown(Markdown markdown) { this.markdown = markdown; }
    }
}