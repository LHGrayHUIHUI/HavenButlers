package com.haven.admin.service;

import com.haven.admin.entity.AlertHistoryEntity;
import com.haven.admin.entity.MonitoringDataEntity;
import com.haven.admin.model.Alert;
import com.haven.admin.model.AlertRule;
import com.haven.admin.repository.AlertHistoryRepository;
import com.haven.admin.repository.MonitoringDataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 监控数据持久化服务
 * 负责将监控数据和告警信息保存到数据库
 *
 * @author HavenButler
 */
@Slf4j
@Service
public class MonitoringPersistenceService {

    @Autowired
    private MonitoringDataRepository monitoringDataRepository;

    @Autowired
    private AlertHistoryRepository alertHistoryRepository;

    @Value("${admin.monitoring.persistence.enabled:true}")
    private boolean persistenceEnabled;

    @Value("${admin.monitoring.persistence.retention.days:7}") // 默认保留7天
    private int dataRetentionDays;

    /**
     * 保存监控数据
     */
    @Transactional
    public void saveMonitoringData(String serviceName, String instanceId, String metricName,
                                   Double metricValue, String metricType, String unit, String tags) {
        if (!persistenceEnabled) {
            return;
        }

        try {
            MonitoringDataEntity entity = MonitoringDataEntity.builder()
                    .serviceName(serviceName)
                    .instanceId(instanceId)
                    .metricName(metricName)
                    .metricValue(metricValue)
                    .metricType(metricType)
                    .unit(unit)
                    .tags(tags)
                    .timestamp(Instant.now())
                    .build();

            monitoringDataRepository.save(entity);

            log.debug("监控数据已保存: service={}, metric={}, value={}", serviceName, metricName, metricValue);

        } catch (Exception e) {
            log.error("保存监控数据失败: service={}, metric={}, error={}", serviceName, metricName, e.getMessage(), e);
        }
    }

    /**
     * 保存告警记录
     */
    @Transactional
    public void saveAlertRecord(Alert alert, AlertRule rule) {
        if (!persistenceEnabled) {
            return;
        }

        try {
            AlertHistoryEntity entity = AlertHistoryEntity.builder()
                    .alertId(alert.getId())
                    .ruleId(rule.getId())
                    .ruleName(rule.getName())
                    .serviceName(alert.getServiceName())
                    .instanceId(alert.getInstanceId())
                    .level(alert.getLevel())
                    .message(alert.getMessage())
                    .metricName(alert.getMetricName())
                    .currentValue(alert.getValue())
                    .threshold(alert.getThreshold())
                    .status(alert.getStatus().name())
                    .triggerTime(alert.getTimestamp())
                    .notificationStatus("PENDING")
                    .build();

            alertHistoryRepository.save(entity);

            log.debug("告警记录已保存: service={}, level={}, message={}", alert.getServiceName(), alert.getLevel(), alert.getMessage());

        } catch (Exception e) {
            log.error("保存告警记录失败: service={}, error={}", alert.getServiceName(), e.getMessage(), e);
        }
    }

    /**
     * 更新告警处理状态
     */
    @Transactional
    public void updateAlertStatus(Long alertId, String status, String handler, String handleRemark) {
        if (!persistenceEnabled) {
            return;
        }

        try {
            List<AlertHistoryEntity> alerts = alertHistoryRepository.findByAlertId(alertId);
            for (AlertHistoryEntity alert : alerts) {
                alert.setStatus(status);
                alert.setHandler(handler);
                alert.setHandleRemark(handleRemark);
                alert.setHandleTime(java.time.LocalDateTime.now());
            }

            alertHistoryRepository.saveAll(alerts);

            log.debug("告警状态已更新: alertId={}, status={}, handler={}", alertId, status, handler);

        } catch (Exception e) {
            log.error("更新告警状态失败: alertId={}, error={}", alertId, e.getMessage(), e);
        }
    }

    /**
     * 定期清理过期数据
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    @Transactional
    public void cleanupExpiredData() {
        if (!persistenceEnabled) {
            return;
        }

        try {
            Instant cutoffTime = Instant.now().minus(dataRetentionDays, ChronoUnit.DAYS);

            // 清理过期的监控数据
            int deletedMonitoringData = monitoringDataRepository.deleteDataBefore(cutoffTime);
            log.info("清理过期监控数据: {} 条", deletedMonitoringData);

            // 清理过期的告警历史记录
            int deletedAlertHistory = alertHistoryRepository.deleteAlertsBefore(cutoffTime);
            log.info("清理过期告警历史: {} 条", deletedAlertHistory);

            log.info("数据清理完成，保留数据天数: {}", dataRetentionDays);

        } catch (Exception e) {
            log.error("数据清理失败", e);
        }
    }

    /**
     * 批量保存监控数据
     */
    @Transactional
    public void batchSaveMonitoringData(List<MonitoringDataEntity> entities) {
        if (!persistenceEnabled || entities.isEmpty()) {
            return;
        }

        try {
            monitoringDataRepository.saveAll(entities);
            log.debug("批量保存监控数据: {} 条", entities.size());

        } catch (Exception e) {
            log.error("批量保存监控数据失败: error={}", e.getMessage(), e);
        }
    }

    /**
     * 获取监控数据统计信息
     */
    public long getMonitoringDataCount() {
        if (!persistenceEnabled) {
            return 0;
        }

        try {
            return monitoringDataRepository.count();
        } catch (Exception e) {
            log.error("获取监控数据统计失败", e);
            return 0;
        }
    }

    /**
     * 获取告警历史统计信息
     */
    public long getAlertHistoryCount() {
        if (!persistenceEnabled) {
            return 0;
        }

        try {
            return alertHistoryRepository.count();
        } catch (Exception e) {
            log.error("获取告警历史统计失败", e);
            return 0;
        }
    }

    /**
     * 获取未处理告警数量
     */
    public long getPendingAlertCount() {
        if (!persistenceEnabled) {
            return 0;
        }

        try {
            return alertHistoryRepository.countPendingAlerts();
        } catch (Exception e) {
            log.error("获取未处理告警数量失败", e);
            return 0;
        }
    }

    /**
     * 查询指定服务的监控数据
     */
    public List<MonitoringDataEntity> getMonitoringDataByService(String serviceName, Instant startTime, Instant endTime) {
        if (!persistenceEnabled) {
            return List.of();
        }

        try {
            if (startTime == null || endTime == null) {
                return monitoringDataRepository.findByServiceName(serviceName);
            } else {
                return monitoringDataRepository.findByServiceNameAndTimeRange(serviceName, startTime, endTime);
            }
        } catch (Exception e) {
            log.error("查询监控数据失败: service={}, error={}", serviceName, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 查询告警历史记录
     */
    public List<AlertHistoryEntity> getAlertHistory(String serviceName, AlertRule.AlertLevel level,
                                                   Instant startTime, Instant endTime) {
        if (!persistenceEnabled) {
            return List.of();
        }

        try {
            if (serviceName != null && level != null) {
                return alertHistoryRepository.findByServiceNameAndLevelAndTimeRange(serviceName, level, startTime, endTime);
            } else if (serviceName != null) {
                return alertHistoryRepository.findByServiceNameAndTimeRange(serviceName, startTime, endTime);
            } else if (level != null) {
                return alertHistoryRepository.findByLevel(level);
            } else {
                return alertHistoryRepository.findByTimeRange(startTime, endTime);
            }
        } catch (Exception e) {
            log.error("查询告警历史失败: error={}", e.getMessage(), e);
            return List.of();
        }
    }
}