package com.haven.base.config;

import com.haven.base.i18n.I18nUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置验证工具
 * 验证配置的有效性并提供改进建议
 *
 * @author HavenButler
 */
@Slf4j
@Component
public class ConfigurationValidator {

    @Autowired
    private I18nUtil i18nUtil;

    /**
     * 验证配置并生成报告
     *
     * @param properties 配置对象
     * @return 验证报告
     */
    public ValidationReport validate(HavenBaseProperties properties) {
        ValidationReport report = new ValidationReport();
        HavenBaseProperties.QuickStart quickStart = properties.getQuickStart();
        HavenBaseProperties.Advanced advanced = properties.getAdvanced();

        // 验证快速配置
        validateQuickStart(quickStart, report);

        // 验证高级配置
        validateAdvanced(advanced, report);

        // 生成总体评估
        generateOverallAssessment(report);

        return report;
    }

    /**
     * 验证快速配置
     */
    private void validateQuickStart(HavenBaseProperties.QuickStart quickStart, ValidationReport report) {
        // 验证环境配置
        if (!isValidProfile(quickStart.getProfile())) {
            String message = i18nUtil.getConfigMessage("validation.profile.invalid",
                    new Object[]{quickStart.getProfile()});
            report.addError(message);
        }

        // 验证预设模板
        if (!isValidPreset(quickStart.getPreset())) {
            String message = i18nUtil.getConfigMessage("validation.preset.invalid",
                    new Object[]{quickStart.getPreset()});
            report.addError(message);
        }

        // 配置一致性检查
        if (quickStart.getProfile().equals("production") && !quickStart.isEnableMonitoring()) {
            String message = i18nUtil.getConfigMessage("validation.production.monitoring");
            report.addWarning(message);
        }

        if (quickStart.getProfile().equals("production") && !quickStart.isEnableResilience()) {
            String message = i18nUtil.getConfigMessage("validation.production.resilience");
            report.addWarning(message);
        }
    }

    /**
     * 验证高级配置
     */
    private void validateAdvanced(HavenBaseProperties.Advanced advanced, ValidationReport report) {
        // 验证缓存配置
        validateCacheConfig(advanced.getCache(), report);

        // 验证容错配置
        validateResilienceConfig(advanced.getResilience(), report);

        // 验证安全配置
        validateSecurityConfig(advanced.getSecurity(), report);

        // 验证监控配置
        validateMonitoringConfig(advanced.getMonitoring(), report);

        // 验证性能配置
        validatePerformanceConfig(advanced, report);
    }

    /**
     * 验证缓存配置
     */
    private void validateCacheConfig(HavenBaseProperties.Advanced.Cache cache, ValidationReport report) {
        if (cache.isEnabled()) {
            // 本地缓存验证
            HavenBaseProperties.Advanced.Cache.Local local = cache.getLocal();
            if (local.isEnabled()) {
                if (local.getMaximumSize() < 100) {
                    report.addWarning("本地缓存容量过小(" + local.getMaximumSize() + ")，建议至少100");
                }

                if (local.getExpireAfterWrite() < 60) {
                    report.addWarning("本地缓存过期时间过短(" + local.getExpireAfterWrite() + "s)，建议至少60s");
                }
            }

            // 分布式缓存验证
            HavenBaseProperties.Advanced.Cache.Distributed distributed = cache.getDistributed();
            if (distributed.isEnabled()) {
                if (distributed.getDefaultTtl() < 300) {
                    report.addWarning("分布式缓存TTL过短(" + distributed.getDefaultTtl() + "s)，建议至少300s");
                }

                if (distributed.getKeyPrefix().isEmpty()) {
                    report.addWarning("分布式缓存缺少键前缀，可能导致缓存冲突");
                }
            }
        }
    }

    /**
     * 验证容错配置
     */
    private void validateResilienceConfig(HavenBaseProperties.Advanced.Resilience resilience, ValidationReport report) {
        if (resilience.isEnabled()) {
            HavenBaseProperties.Advanced.Resilience.TimeLimiter timeLimiter = resilience.getTimeLimiter();
            if (timeLimiter.isEnabled()) {
                if (timeLimiter.getTimeoutDuration() < 1000) {
                    report.addWarning("超时时间过短(" + timeLimiter.getTimeoutDuration() + "ms)，可能导致正常请求失败");
                }

                if (timeLimiter.getTimeoutDuration() > 30000) {
                    report.addWarning("超时时间过长(" + timeLimiter.getTimeoutDuration() + "ms)，可能影响系统响应性");
                }
            }

            HavenBaseProperties.Advanced.Resilience.Retry retry = resilience.getRetry();
            if (retry.isEnabled()) {
                if (retry.getMaxAttempts() > 5) {
                    report.addWarning("重试次数过多(" + retry.getMaxAttempts() + ")，可能导致雪崩效应");
                }

                if (retry.getWaitDuration() < 100) {
                    report.addWarning("重试间隔过短(" + retry.getWaitDuration() + "ms)，可能加重系统负载");
                }
            }

            HavenBaseProperties.Advanced.Resilience.CircuitBreaker circuitBreaker = resilience.getCircuitBreaker();
            if (circuitBreaker.isEnabled()) {
                if (circuitBreaker.getFailureRateThreshold() > 80) {
                    report.addWarning("熔断失败率阈值过高(" + circuitBreaker.getFailureRateThreshold() + "%)，可能影响系统稳定性");
                }

                if (circuitBreaker.getFailureRateThreshold() < 10) {
                    report.addWarning("熔断失败率阈值过低(" + circuitBreaker.getFailureRateThreshold() + "%)，可能导致频繁熔断");
                }
            }
        }
    }

    /**
     * 验证安全配置
     */
    private void validateSecurityConfig(HavenBaseProperties.Advanced.Security security, ValidationReport report) {
        if (security.isEnabled()) {
            HavenBaseProperties.Advanced.Security.KeyManager keyManager = security.getKeyManager();
            if (keyManager.isEnabled()) {
                HavenBaseProperties.Advanced.Security.KeyManager.StrengthValidation strengthValidation = keyManager.getStrengthValidation();
                if (strengthValidation.isEnabled()) {
                    if (strengthValidation.getMinKeyLength() < 32) {
                        report.addWarning("密钥最小长度过短(" + strengthValidation.getMinKeyLength() + ")，建议至少32");
                    }

                    if (!strengthValidation.isRequireNumbers()) {
                        report.addWarning("建议密钥包含数字以提高安全性");
                    }
                }

                HavenBaseProperties.Advanced.Security.KeyManager.Rotation rotation = keyManager.getRotation();
                if (rotation.isEnabled()) {
                    if (rotation.getKeyValidityDays() > 90) {
                        report.addWarning("密钥有效期过长(" + rotation.getKeyValidityDays() + "天)，建议不超过90天");
                    }
                } else {
                    report.addWarning("建议启用密钥轮换以提高安全性");
                }
            }
        }
    }

    /**
     * 验证监控配置
     */
    private void validateMonitoringConfig(HavenBaseProperties.Advanced.Monitoring monitoring, ValidationReport report) {
        if (monitoring.isEnabled()) {
            HavenBaseProperties.Advanced.Monitoring.Sentry sentry = monitoring.getSentry();

            // 检查Sentry DSN配置
            if (!sentry.getDsn().isEmpty()) {
                // Sentry配置完整，检查性能配置
                HavenBaseProperties.Advanced.Monitoring.Sentry.Performance performance = sentry.getPerformance();
                if (performance.isEnabled()) {
                    if (performance.getSlowRequestThreshold() > 5000) {
                        report.addWarning("慢请求阈值过高(" + performance.getSlowRequestThreshold() + "ms)，建议5000ms以下");
                    }
                }
            } else {
                report.addWarning("Sentry DSN为空，监控功能可能无法正常工作");
            }
        }
    }

    /**
     * 验证性能配置
     */
    private void validatePerformanceConfig(HavenBaseProperties.Advanced advanced, ValidationReport report) {
        HavenBaseProperties.Advanced.ServiceClient serviceClient = advanced.getServiceClient();

        if (serviceClient.getConnectTimeout() > 10000) {
            report.addWarning("连接超时时间过长(" + serviceClient.getConnectTimeout() + "ms)，建议控制在10秒以内");
        }

        if (serviceClient.getReadTimeout() > 30000) {
            report.addWarning("读取超时时间过长(" + serviceClient.getReadTimeout() + "ms)，建议控制在30秒以内");
        }

        if (serviceClient.getMaxTotal() > 500) {
            report.addWarning("连接池最大连接数过多(" + serviceClient.getMaxTotal() + ")，建议控制在500以内");
        }

        HavenBaseProperties.Advanced.DistributedLock distributedLock = advanced.getDistributedLock();
        if (distributedLock.getLeaseTime() > 60000) {
            report.addWarning("分布式锁持有时间过长(" + distributedLock.getLeaseTime() + "ms)，可能导致死锁风险");
        }
    }

    /**
     * 生成总体评估
     */
    private void generateOverallAssessment(ValidationReport report) {
        int errorCount = report.getErrors().size();
        int warningCount = report.getWarnings().size();

        if (errorCount == 0 && warningCount == 0) {
            report.setAssessment("配置验证通过，配置合理且无潜在问题");
            report.setScore(100);
        } else if (errorCount == 0) {
            report.setAssessment("配置基本合理，存在" + warningCount + "个可优化项");
            report.setScore(Math.max(80, 100 - warningCount * 2));
        } else {
            report.setAssessment("配置存在" + errorCount + "个错误和" + warningCount + "个警告，需要修复");
            report.setScore(Math.max(0, 80 - errorCount * 10 - warningCount * 2));
        }
    }

    /**
     * 验证环境配置
     */
    private boolean isValidProfile(String profile) {
        return "development".equals(profile) ||
               "testing".equals(profile) ||
               "production".equals(profile);
    }

    /**
     * 验证预设模板
     */
    private boolean isValidPreset(String preset) {
        return "microservice".equals(preset) ||
               "monolith".equals(preset) ||
               "high-concurrency".equals(preset) ||
               "secure".equals(preset) ||
               "simple".equals(preset);
    }

    /**
     * 验证报告
     */
    @Data
    public static class ValidationReport {
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        private List<String> suggestions = new ArrayList<>();
        private String assessment;
        private int score;

        public void addError(String error) {
            errors.add(error);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public void addSuggestion(String suggestion) {
            suggestions.add(suggestion);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("配置验证报告:\n");
            sb.append("评分: ").append(score).append("/100\n");
            sb.append("评估: ").append(assessment).append("\n");

            if (!errors.isEmpty()) {
                sb.append("\n错误 (").append(errors.size()).append("):\n");
                for (String error : errors) {
                    sb.append("❌ ").append(error).append("\n");
                }
            }

            if (!warnings.isEmpty()) {
                sb.append("\n警告 (").append(warnings.size()).append("):\n");
                for (String warning : warnings) {
                    sb.append("⚠️ ").append(warning).append("\n");
                }
            }

            if (!suggestions.isEmpty()) {
                sb.append("\n建议 (").append(suggestions.size()).append("):\n");
                for (String suggestion : suggestions) {
                    sb.append("💡 ").append(suggestion).append("\n");
                }
            }

            return sb.toString();
        }
    }
}