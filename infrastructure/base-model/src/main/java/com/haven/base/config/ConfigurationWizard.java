package com.haven.base.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置向导工具
 * 根据使用场景自动推荐配置，大幅降低用户配置成本
 *
 * @author HavenButler
 */
@Slf4j
@Component
public class ConfigurationWizard {

    /**
     * 根据使用场景推荐配置
     *
     * @param useCase 使用场景: microservice, monolith, high-concurrency, secure
     * @return 推荐的配置
     */
    public HavenBaseProperties recommendConfig(String useCase) {
        HavenBaseProperties config = new HavenBaseProperties();

        switch (useCase.toLowerCase()) {
            case "microservice":
                return buildMicroserviceConfig(config);
            case "monolith":
                return buildMonolithConfig(config);
            case "high-concurrency":
                return buildHighConcurrencyConfig(config);
            case "secure":
                return buildSecureConfig(config);
            case "simple":
                return buildSimpleConfig(config);
            default:
                return buildDefaultConfig(config);
        }
    }

    /**
     * 微服务配置模板
     * 适用于分布式微服务架构
     */
    private HavenBaseProperties buildMicroserviceConfig(HavenBaseProperties config) {
        HavenBaseProperties.QuickStart quickStart = config.getQuickStart();
        HavenBaseProperties.Advanced advanced = config.getAdvanced();

        // 快速配置
        quickStart.setEnableCache(true);
        quickStart.setEnableMonitoring(true);
        quickStart.setEnableResilience(true);
        quickStart.setEnableSecurity(true);
        quickStart.setEnableServiceDiscovery(true);
        quickStart.setPreset("microservice");

        // 高级配置优化
        advanced.getCache().getLocal().setMaximumSize(5000);
        advanced.getCache().getDistributed().setDefaultTtl(1800);
        advanced.getResilience().getCircuitBreaker().setFailureRateThreshold(30.0f);
        advanced.getResilience().getCircuitBreaker().setWaitDurationInOpenState(60000);
        advanced.getMonitoring().setEnabled(true);
        advanced.getSecurity().getKeyManager().getRotation().setEnabled(true);
        advanced.getServiceClient().setConnectTimeout(3000);
        advanced.getServiceClient().setReadTimeout(8000);

        log.info("已应用微服务配置模板");
        return config;
    }

    /**
     * 单体应用配置模板
     * 适用于传统单体应用
     */
    private HavenBaseProperties buildMonolithConfig(HavenBaseProperties config) {
        HavenBaseProperties.QuickStart quickStart = config.getQuickStart();
        HavenBaseProperties.Advanced advanced = config.getAdvanced();

        // 快速配置
        quickStart.setEnableCache(true);
        quickStart.setEnableMonitoring(false);
        quickStart.setEnableResilience(false);
        quickStart.setEnableSecurity(true);
        quickStart.setEnableServiceDiscovery(false);
        quickStart.setPreset("monolith");

        // 高级配置优化
        advanced.getCache().getLocal().setMaximumSize(20000);
        advanced.getCache().getDistributed().setEnabled(false);
        advanced.getResilience().setEnabled(false);
        advanced.getMonitoring().setEnabled(false);
        advanced.getServiceClient().setConnectTimeout(5000);
        advanced.getServiceClient().setReadTimeout(15000);

        log.info("已应用单体应用配置模板");
        return config;
    }

    /**
     * 高并发配置模板
     * 适用于高并发访问场景
     */
    private HavenBaseProperties buildHighConcurrencyConfig(HavenBaseProperties config) {
        HavenBaseProperties.QuickStart quickStart = config.getQuickStart();
        HavenBaseProperties.Advanced advanced = config.getAdvanced();

        // 快速配置
        quickStart.setEnableCache(true);
        quickStart.setEnableMonitoring(true);
        quickStart.setEnableResilience(true);
        quickStart.setEnableSecurity(true);
        quickStart.setPreset("high-concurrency");

        // 高级配置优化
        advanced.getCache().getLocal().setMaximumSize(50000);
        advanced.getCache().getLocal().setExpireAfterWrite(1800);
        advanced.getCache().getDistributed().setDefaultTtl(900);
        advanced.getResilience().getTimeLimiter().setTimeoutDuration(2000);
        advanced.getResilience().getRetry().setMaxAttempts(2);
        advanced.getResilience().getRetry().setWaitDuration(500);
        advanced.getServiceClient().setMaxTotal(200);
        advanced.getServiceClient().setDefaultMaxPerRoute(50);
        advanced.getDistributedLock().setWaitTime(5000);
        advanced.getDistributedLock().setLeaseTime(15000);

        log.info("已应用高并发配置模板");
        return config;
    }

    /**
     * 安全强化配置模板
     * 适用于对安全要求极高的场景
     */
    private HavenBaseProperties buildSecureConfig(HavenBaseProperties config) {
        HavenBaseProperties.QuickStart quickStart = config.getQuickStart();
        HavenBaseProperties.Advanced advanced = config.getAdvanced();

        // 快速配置
        quickStart.setEnableCache(true);
        quickStart.setEnableMonitoring(true);
        quickStart.setEnableResilience(true);
        quickStart.setEnableSecurity(true);
        quickStart.setPreset("secure");

        // 高级配置优化
        advanced.getSecurity().getKeyManager().getRotation().setEnabled(true);
        advanced.getSecurity().getKeyManager().getRotation().setKeyValidityDays(30);
        advanced.getSecurity().getKeyManager().getStrengthValidation().setMinKeyLength(64);
        advanced.getSecurity().getKeyManager().getStrengthValidation().setRequireSpecialChars(true);
        advanced.getCache().getDistributed().setKeyPrefix("secure:cache:");
        advanced.getMonitoring().getSentry().setTracesSampleRate(1.0);
        advanced.getMonitoring().getSentry().getPerformance().setSlowRequestThreshold(1000);
        advanced.getServiceClient().setConnectTimeout(2000);
        advanced.getServiceClient().setReadTimeout(5000);

        log.info("已应用安全强化配置模板");
        return config;
    }

    /**
     * 简单配置模板
     * 适用于快速原型和开发测试
     */
    private HavenBaseProperties buildSimpleConfig(HavenBaseProperties config) {
        HavenBaseProperties.QuickStart quickStart = config.getQuickStart();
        HavenBaseProperties.Advanced advanced = config.getAdvanced();

        // 快速配置
        quickStart.setEnableCache(true);
        quickStart.setEnableMonitoring(false);
        quickStart.setEnableResilience(false);
        quickStart.setEnableSecurity(false);
        quickStart.setPreset("simple");

        // 高级配置优化
        advanced.getCache().getLocal().setMaximumSize(1000);
        advanced.getCache().getDistributed().setEnabled(false);
        advanced.getResilience().setEnabled(false);
        advanced.getMonitoring().setEnabled(false);
        advanced.getSecurity().setEnabled(false);

        log.info("已应用简单配置模板");
        return config;
    }

    /**
     * 默认配置模板
     */
    private HavenBaseProperties buildDefaultConfig(HavenBaseProperties config) {
        // 保持默认配置不变
        log.info("使用默认配置");
        return config;
    }

    /**
     * 配置验证和优化建议
     *
     * @param config 当前配置
     * @return 优化建议列表
     */
    public List<ConfigSuggestion> validateAndSuggest(HavenBaseProperties config) {
        List<ConfigSuggestion> suggestions = new ArrayList<>();

        HavenBaseProperties.Advanced advanced = config.getAdvanced();

        // 缓存配置建议
        if (advanced.getCache().getLocal().getMaximumSize() < 1000) {
            suggestions.add(new ConfigSuggestion(
                "cache.local.maximum-size",
                "建议增加本地缓存大小以提升性能",
                "1000-10000",
                "performance"
            ));
        }

        // 容错配置建议
        if (advanced.getResilience().getTimeLimiter().getTimeoutDuration() > 10000) {
            suggestions.add(new ConfigSuggestion(
                "resilience.time-limiter.timeout-duration",
                "超时时间过长可能影响用户体验",
                "3000-5000",
                "performance"
            ));
        }

        // 安全配置建议
        if (!advanced.getSecurity().getKeyManager().getRotation().isEnabled()) {
            suggestions.add(new ConfigSuggestion(
                "security.key-manager.rotation.enabled",
                "建议启用密钥轮换以提升安全性",
                "true",
                "security"
            ));
        }

        // 监控配置建议
        if (!advanced.getMonitoring().isEnabled()) {
            suggestions.add(new ConfigSuggestion(
                "monitoring.enabled",
                "建议启用监控以便及时发现和解决问题",
                "true",
                "operations"
            ));
        }

        return suggestions;
    }

    /**
     * 生成配置预览
     *
     * @param config 配置对象
     * @return YAML格式的配置预览
     */
    public String generateConfigPreview(HavenBaseProperties config) {
        StringBuilder yaml = new StringBuilder();
        HavenBaseProperties.QuickStart quickStart = config.getQuickStart();
        HavenBaseProperties.Advanced advanced = config.getAdvanced();

        yaml.append("# HavenBase 配置预览\n");
        yaml.append("haven:\n");
        yaml.append("  base:\n");

        // 快速配置
        yaml.append("    # 快速配置\n");
        yaml.append("    quick-start:\n");
        yaml.append("      enabled: ").append(quickStart.isEnabled()).append("\n");
        yaml.append("      profile: \"").append(quickStart.getProfile()).append("\"\n");
        yaml.append("      enable-cache: ").append(quickStart.isEnableCache()).append("\n");
        yaml.append("      enable-monitoring: ").append(quickStart.isEnableMonitoring()).append("\n");
        yaml.append("      enable-resilience: ").append(quickStart.isEnableResilience()).append("\n");
        yaml.append("      enable-security: ").append(quickStart.isEnableSecurity()).append("\n");
        yaml.append("      enable-service-discovery: ").append(quickStart.isEnableServiceDiscovery()).append("\n");
        yaml.append("      preset: \"").append(quickStart.getPreset()).append("\"\n");

        // 关键高级配置
        yaml.append("    # 高级配置 (仅显示关键项)\n");
        yaml.append("    advanced:\n");
        yaml.append("      cache:\n");
        yaml.append("        local:\n");
        yaml.append("          maximum-size: ").append(advanced.getCache().getLocal().getMaximumSize()).append("\n");
        yaml.append("      resilience:\n");
        yaml.append("        time-limiter:\n");
        yaml.append("          timeout-duration: ").append(advanced.getResilience().getTimeLimiter().getTimeoutDuration()).append("\n");

        return yaml.toString();
    }

    /**
     * 配置建议类
     */
    public static class ConfigSuggestion {
        private final String key;
        private final String suggestion;
        private final String recommendedValue;
        private final String category;

        public ConfigSuggestion(String key, String suggestion, String recommendedValue, String category) {
            this.key = key;
            this.suggestion = suggestion;
            this.recommendedValue = recommendedValue;
            this.category = category;
        }

        public String getKey() {
            return key;
        }

        public String getSuggestion() {
            return suggestion;
        }

        public String getRecommendedValue() {
            return recommendedValue;
        }

        public String getCategory() {
            return category;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s: %s (建议值: %s)",
                category, key, suggestion, recommendedValue);
        }
    }
}