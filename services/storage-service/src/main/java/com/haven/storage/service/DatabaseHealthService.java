package com.haven.storage.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据库健康检查服务
 * 监控storage-service管理的所有数据存储系统的健康状态
 * 
 * @author HavenButler
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseHealthService implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;
    private final MongoTemplate mongoTemplate;
    private final RedisConnectionFactory redisConnectionFactory;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${minio.endpoint:http://minio:9000}")
    private String minioEndpoint;
    
    @Value("${minio.console.endpoint:http://minio:9001}")
    private String minioConsoleEndpoint;
    
    // 存储各个数据库的健康状态
    private final Map<String, DatabaseStatus> healthStatusCache = new ConcurrentHashMap<>();
    
    /**
     * 定时检查数据库健康状态
     * 每30秒执行一次
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void checkDatabaseHealth() {
        log.debug("开始检查数据库健康状态");
        
        // 检查PostgreSQL
        checkPostgreSQL();
        
        // 检查MongoDB
        checkMongoDB();
        
        // 检查Redis
        checkRedis();
        
        // 检查MinIO
        checkMinIO();
        
        logHealthSummary();
    }
    
    /**
     * 检查PostgreSQL健康状态
     */
    private void checkPostgreSQL() {
        DatabaseStatus status = new DatabaseStatus();
        status.setName("PostgreSQL");
        status.setType("RDBMS");
        
        try {
            // 执行简单查询来检查连接
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            
            // 获取连接池状态
            Connection conn = jdbcTemplate.getDataSource().getConnection();
            status.setHealthy(true);
            status.setDetails("连接正常");
            conn.close();
            
            // 获取数据库版本
            String version = jdbcTemplate.queryForObject(
                "SELECT version()", String.class
            );
            status.setVersion(version);
            
        } catch (Exception e) {
            log.error("PostgreSQL健康检查失败", e);
            status.setHealthy(false);
            status.setDetails("连接失败: " + e.getMessage());
        }
        
        status.setLastCheck(new Date());
        healthStatusCache.put("PostgreSQL", status);
    }
    
    /**
     * 检查MongoDB健康状态
     */
    private void checkMongoDB() {
        DatabaseStatus status = new DatabaseStatus();
        status.setName("MongoDB");
        status.setType("NoSQL");
        
        try {
            // 执行ping命令
            mongoTemplate.getDb().runCommand(new org.bson.Document("ping", 1));
            
            status.setHealthy(true);
            status.setDetails("连接正常");
            
            // 获取数据库版本
            org.bson.Document serverStatus = mongoTemplate.getDb().runCommand(
                new org.bson.Document("serverStatus", 1)
            );
            status.setVersion(serverStatus.getString("version"));
            
            // 获取集合数量
            Set<String> collections = mongoTemplate.getCollectionNames();
            status.setMetrics(Map.of(
                "collections", collections.size(),
                "database", mongoTemplate.getDb().getName()
            ));
            
        } catch (Exception e) {
            log.error("MongoDB健康检查失败", e);
            status.setHealthy(false);
            status.setDetails("连接失败: " + e.getMessage());
        }
        
        status.setLastCheck(new Date());
        healthStatusCache.put("MongoDB", status);
    }
    
    /**
     * 检查Redis健康状态
     */
    private void checkRedis() {
        DatabaseStatus status = new DatabaseStatus();
        status.setName("Redis");
        status.setType("Cache");
        
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            // 执行ping命令
            String pong = connection.ping();
            
            if ("PONG".equals(pong)) {
                status.setHealthy(true);
                status.setDetails("连接正常");
                
                // 获取Redis信息
                Properties info = connection.serverCommands().info();
                status.setVersion(info.getProperty("redis_version"));
                
                // 获取内存使用情况
                status.setMetrics(Map.of(
                    "used_memory_human", info.getProperty("used_memory_human", "N/A"),
                    "connected_clients", info.getProperty("connected_clients", "0"),
                    "total_commands_processed", info.getProperty("total_commands_processed", "0")
                ));
            } else {
                status.setHealthy(false);
                status.setDetails("Ping响应异常");
            }
            
        } catch (Exception e) {
            log.error("Redis健康检查失败", e);
            status.setHealthy(false);
            status.setDetails("连接失败: " + e.getMessage());
        }
        
        status.setLastCheck(new Date());
        healthStatusCache.put("Redis", status);
    }
    
    /**
     * 检查MinIO健康状态
     */
    private void checkMinIO() {
        DatabaseStatus status = new DatabaseStatus();
        status.setName("MinIO");
        status.setType("ObjectStorage");
        
        try {
            // 检查MinIO健康端点
            String healthUrl = minioEndpoint + "/minio/health/live";
            restTemplate.getForObject(healthUrl, String.class);
            
            status.setHealthy(true);
            status.setDetails("服务运行正常");
            
            // 记录MinIO端点
            status.setMetrics(Map.of(
                "endpoint", minioEndpoint,
                "console", minioConsoleEndpoint
            ));
            
        } catch (Exception e) {
            // MinIO健康端点可能需要认证，尝试基本连通性
            try {
                restTemplate.headForHeaders(minioEndpoint);
                status.setHealthy(true);
                status.setDetails("服务可达");
            } catch (Exception ex) {
                log.error("MinIO健康检查失败", ex);
                status.setHealthy(false);
                status.setDetails("服务不可达: " + ex.getMessage());
            }
        }
        
        status.setLastCheck(new Date());
        healthStatusCache.put("MinIO", status);
    }
    
    /**
     * 输出健康状态摘要
     */
    private void logHealthSummary() {
        long healthyCount = healthStatusCache.values().stream()
            .filter(DatabaseStatus::isHealthy)
            .count();
        long totalCount = healthStatusCache.size();
        
        log.info("数据库健康状态: {}/{} 正常运行", healthyCount, totalCount);
        
        // 记录不健康的服务
        healthStatusCache.values().stream()
            .filter(s -> !s.isHealthy())
            .forEach(s -> log.warn("数据库 {} 状态异常: {}", s.getName(), s.getDetails()));
    }
    
    /**
     * 获取所有数据库状态
     */
    public List<DatabaseStatus> getAllDatabaseStatus() {
        if (healthStatusCache.isEmpty()) {
            // 如果缓存为空，立即执行一次检查
            checkDatabaseHealth();
        }
        return new ArrayList<>(healthStatusCache.values());
    }
    
    /**
     * 获取指定数据库状态
     */
    public DatabaseStatus getDatabaseStatus(String name) {
        return healthStatusCache.get(name);
    }
    
    /**
     * 获取健康状态摘要
     */
    public Map<String, Object> getHealthSummary() {
        List<DatabaseStatus> allStatus = getAllDatabaseStatus();
        
        long healthyCount = allStatus.stream()
            .filter(DatabaseStatus::isHealthy)
            .count();
        
        Map<String, Boolean> statusMap = new HashMap<>();
        for (DatabaseStatus status : allStatus) {
            statusMap.put(status.getName(), status.isHealthy());
        }
        
        return Map.of(
            "total", allStatus.size(),
            "healthy", healthyCount,
            "unhealthy", allStatus.size() - healthyCount,
            "healthRate", allStatus.isEmpty() ? 0 : (healthyCount * 100.0 / allStatus.size()),
            "databases", statusMap,
            "lastCheck", allStatus.isEmpty() ? null : allStatus.get(0).getLastCheck()
        );
    }
    
    @Override
    public Health health() {
        Map<String, Object> summary = getHealthSummary();
        long healthy = ((Number) summary.get("healthy")).longValue();
        long total = ((Number) summary.get("total")).longValue();
        
        Health.Builder builder = (healthy == total) ? Health.up() : Health.down();
        
        return builder
            .withDetail("databases", healthStatusCache)
            .withDetail("summary", summary)
            .build();
    }
    
    /**
     * 数据库状态信息
     */
    @Data
    public static class DatabaseStatus {
        private String name;
        private String type;
        private boolean healthy;
        private String details;
        private String version;
        private Date lastCheck;
        private Map<String, Object> metrics;
    }
}