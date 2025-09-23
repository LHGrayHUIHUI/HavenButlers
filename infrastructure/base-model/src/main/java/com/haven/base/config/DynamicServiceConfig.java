package com.haven.base.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 动态服务配置
 * 支持从Nacos动态获取和刷新配置
 *
 * 注意：如果需要动态刷新功能，请添加 @RefreshScope 注解
 * 并在项目中引入 spring-cloud-context 依赖
 *
 * @author HavenButler
 */
@Data
@Component
@ConfigurationProperties(prefix = "haven.services")
public class DynamicServiceConfig {

    /**
     * 存储服务配置
     */
    private StorageService storage = new StorageService();

    /**
     * 账户服务配置
     */
    private AccountService account = new AccountService();

    /**
     * 网关服务配置
     */
    private GatewayService gateway = new GatewayService();

    /**
     * NLP服务配置
     */
    private NlpService nlp = new NlpService();

    /**
     * AI服务配置
     */
    private AiService ai = new AiService();

    /**
     * 文件管理服务配置
     */
    private FileManagerService fileManager = new FileManagerService();

    /**
     * 消息服务配置
     */
    private MessageService message = new MessageService();

    @Data
    public static class StorageService {
        private String url = "http://storage-service:8081";
        private int timeout = 5000;
        private int retryCount = 3;
        private boolean enabled = true;
    }

    @Data
    public static class AccountService {
        private String url = "http://account-service:8082";
        private int timeout = 5000;
        private int retryCount = 3;
        private boolean enabled = true;
    }

    @Data
    public static class GatewayService {
        private String url = "http://gateway:8080";
        private int timeout = 30000;
        private int retryCount = 2;
        private boolean enabled = true;
    }

    @Data
    public static class NlpService {
        private String url = "http://nlp-service:8085";
        private int timeout = 10000;
        private int retryCount = 3;
        private boolean enabled = true;
    }

    @Data
    public static class AiService {
        private String url = "http://ai-service:8084";
        private int timeout = 30000;
        private int retryCount = 2;
        private boolean enabled = true;
    }

    @Data
    public static class FileManagerService {
        private String url = "http://file-manager-service:8086";
        private int timeout = 15000;
        private int retryCount = 3;
        private boolean enabled = true;
    }

    @Data
    public static class MessageService {
        private String url = "http://message-service:8083";
        private int timeout = 5000;
        private int retryCount = 3;
        private boolean enabled = true;
    }

    /**
     * 获取服务URL的便捷方法
     */
    public String getServiceUrl(String serviceName) {
        switch (serviceName.toLowerCase()) {
            case "storage":
                return storage.getUrl();
            case "account":
                return account.getUrl();
            case "gateway":
                return gateway.getUrl();
            case "nlp":
                return nlp.getUrl();
            case "ai":
                return ai.getUrl();
            case "file-manager":
                return fileManager.getUrl();
            case "message":
                return message.getUrl();
            default:
                throw new IllegalArgumentException("未知的服务名称: " + serviceName);
        }
    }

    /**
     * 检查服务是否启用
     */
    public boolean isServiceEnabled(String serviceName) {
        switch (serviceName.toLowerCase()) {
            case "storage":
                return storage.isEnabled();
            case "account":
                return account.isEnabled();
            case "gateway":
                return gateway.isEnabled();
            case "nlp":
                return nlp.isEnabled();
            case "ai":
                return ai.isEnabled();
            case "file-manager":
                return fileManager.isEnabled();
            case "message":
                return message.isEnabled();
            default:
                return false;
        }
    }
}