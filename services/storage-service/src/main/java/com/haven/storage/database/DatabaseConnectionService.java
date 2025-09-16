package com.haven.storage.database;

import com.haven.base.annotation.TraceLog;
import com.haven.base.utils.TraceIdUtil;
import com.haven.base.utils.EncryptUtil;
import com.haven.common.redis.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据库连接服务
 *
 * 🎯 核心功能：
 * - 为所有个人项目提供统一的数据库连接
 * - 多项目数据库隔离和管理
 * - 连接池统一管理
 * - 连接健康检查
 *
 * 💡 使用场景：
 * - account-service 通过此服务获取用户数据库连接
 * - message-service 通过此服务获取消息数据库连接
 * - ai-service 通过此服务获取AI对话数据库连接
 *
 * @author HavenButler
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseConnectionService {

    @Value("${storage.database.default-timeout:30}")
    private Integer defaultConnectionTimeout;

    @Value("${storage.database.max-connections:100}")
    private Integer maxConnections;

    private final RedisUtils redisUtils;

    // 项目数据库连接池缓存
    private final Map<String, ProjectDatabaseConfig> projectDatabases = new ConcurrentHashMap<>();

    // 连接池统计
    private final Map<String, DatabaseConnectionStats> connectionStats = new ConcurrentHashMap<>();

    /**
     * 获取项目数据库连接
     *
     * @param projectId 项目ID（如：havenbutler, personal-blog, etc）
     * @param familyId  家庭ID（数据隔离用）
     * @return 数据库连接信息
     */
    @TraceLog(value = "获取数据库连接", module = "db-connection", type = "CONNECTION")
    public DatabaseConnectionInfo getDatabaseConnection(String projectId, String familyId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            // 参数验证
            if (projectId == null || familyId == null) {
                throw new IllegalArgumentException("projectId和familyId不能为空");
            }

            // 获取项目数据库配置
            ProjectDatabaseConfig dbConfig = getProjectDatabaseConfig(projectId);
            if (dbConfig == null) {
                throw new IllegalStateException("项目数据库配置不存在: " + projectId);
            }

            // 构建连接信息（包含家庭隔离）
            DatabaseConnectionInfo connectionInfo = new DatabaseConnectionInfo();
            connectionInfo.setProjectId(projectId);
            connectionInfo.setFamilyId(familyId);
            connectionInfo.setJdbcUrl(buildJdbcUrl(dbConfig, familyId));
            connectionInfo.setUsername(dbConfig.getUsername());
            connectionInfo.setPassword(dbConfig.getPassword());
            connectionInfo.setDriverClassName(dbConfig.getDriverClassName());
            connectionInfo.setMaxConnections(dbConfig.getMaxConnections());
            connectionInfo.setConnectionTimeout(dbConfig.getConnectionTimeout());
            connectionInfo.setTraceId(traceId);

            // 记录连接统计
            recordConnectionRequest(projectId, familyId);

            log.info("数据库连接获取成功: project={}, family={}, TraceID={}",
                    projectId, familyId, traceId);

            return connectionInfo;

        } catch (Exception e) {
            log.error("获取数据库连接失败: project={}, family={}, error={}, TraceID={}",
                    projectId, familyId, e.getMessage(), traceId);
            throw e;
        }
    }

    /**
     * 测试数据库连接
     */
    @TraceLog(value = "测试数据库连接", module = "db-connection", type = "HEALTH_CHECK")
    public DatabaseHealthStatus testDatabaseConnection(String projectId, String familyId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            DatabaseConnectionInfo connectionInfo = getDatabaseConnection(projectId, familyId);

            // 尝试获取连接并测试
            try (Connection connection = createConnection(connectionInfo)) {
                boolean isValid = connection.isValid(5); // 5秒超时

                DatabaseHealthStatus status = new DatabaseHealthStatus();
                status.setProjectId(projectId);
                status.setFamilyId(familyId);
                status.setHealthy(isValid);
                status.setResponseTimeMs(System.currentTimeMillis());
                status.setCheckTime(java.time.LocalDateTime.now());
                status.setTraceId(traceId);

                if (isValid) {
                    status.setMessage("数据库连接正常");
                    log.info("数据库连接测试成功: project={}, family={}, TraceID={}",
                            projectId, familyId, traceId);
                } else {
                    status.setMessage("数据库连接异常");
                    log.warn("数据库连接测试失败: project={}, family={}, TraceID={}",
                            projectId, familyId, traceId);
                }

                return status;
            }

        } catch (Exception e) {
            log.error("数据库连接测试异常: project={}, family={}, error={}, TraceID={}",
                    projectId, familyId, e.getMessage(), traceId);

            DatabaseHealthStatus status = new DatabaseHealthStatus();
            status.setProjectId(projectId);
            status.setFamilyId(familyId);
            status.setHealthy(false);
            status.setMessage("连接测试异常: " + e.getMessage());
            status.setCheckTime(java.time.LocalDateTime.now());
            status.setTraceId(traceId);

            return status;
        }
    }

    /**
     * 获取家庭所有项目数据库连接信息
     */
    @TraceLog(value = "获取家庭项目列表", module = "db-connection", type = "LIST_FAMILY_PROJECTS")
    public List<DatabaseConnectionInfo> getFamilyProjects(String familyId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            List<DatabaseConnectionInfo> familyProjects = new ArrayList<>();

            // 为每个注册的项目生成该家庭的连接信息
            for (String projectId : projectDatabases.keySet()) {
                try {
                    DatabaseConnectionInfo connectionInfo = getDatabaseConnection(projectId, familyId);
                    familyProjects.add(connectionInfo);
                } catch (Exception e) {
                    log.warn("获取项目数据库连接失败: project={}, family={}, error={}",
                            projectId, familyId, e.getMessage());
                }
            }

            log.info("获取家庭项目列表成功: family={}, projects={}, TraceID={}",
                    familyId, familyProjects.size(), traceId);

            return familyProjects;

        } catch (Exception e) {
            log.error("获取家庭项目列表失败: family={}, error={}, TraceID={}",
                    familyId, e.getMessage(), traceId);
            return new ArrayList<>();
        }
    }

    /**
     * 创建项目数据库
     */
    @TraceLog(value = "创建项目数据库", module = "db-connection", type = "CREATE_PROJECT_DB")
    public DatabaseConnectionInfo createProjectDatabase(CreateProjectDatabaseRequest request) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            // 参数验证
            if (request.getProjectId() == null || request.getFamilyId() == null) {
                throw new IllegalArgumentException("projectId和familyId不能为空");
            }

            // 创建项目数据库配置
            ProjectDatabaseConfig config = new ProjectDatabaseConfig();
            config.setProjectId(request.getProjectId());
            config.setDatabaseName(request.getProjectName());
            config.setHost("localhost"); // 默认配置，可以从配置文件读取
            config.setPort(5432);
            config.setUsername("postgres");
            config.setPassword("password");
            config.setDriverClassName("org.postgresql.Driver");
            config.setMaxConnections(30);
            config.setConnectionTimeout(30);

            // 注册项目配置
            registerProjectDatabase(request.getProjectId(), config);

            // 获取连接信息
            DatabaseConnectionInfo connectionInfo = getDatabaseConnection(
                    request.getProjectId(), request.getFamilyId());

            log.info("项目数据库创建成功: project={}, family={}, TraceID={}",
                    request.getProjectId(), request.getFamilyId(), traceId);

            return connectionInfo;

        } catch (Exception e) {
            log.error("项目数据库创建失败: project={}, family={}, error={}, TraceID={}",
                    request.getProjectId(), request.getFamilyId(), e.getMessage(), traceId);
            throw e;
        }
    }

    /**
     * 获取所有项目的数据库连接统计
     */
    @TraceLog(value = "获取连接统计", module = "db-connection", type = "STATS")
    public Map<String, DatabaseConnectionStats> getAllConnectionStats() {
        return new ConcurrentHashMap<>(connectionStats);
    }

    /**
     * 注册项目数据库配置
     */
    public void registerProjectDatabase(String projectId, ProjectDatabaseConfig config) {
        projectDatabases.put(projectId, config);

        // 初始化统计信息
        DatabaseConnectionStats stats = new DatabaseConnectionStats();
        stats.setProjectId(projectId);
        stats.setTotalConnections(0);
        stats.setActiveConnections(0);
        stats.setCreatedAt(java.time.LocalDateTime.now());
        connectionStats.put(projectId, stats);

        log.info("项目数据库配置注册成功: project={}, database={}",
                projectId, config.getDatabaseName());
    }

    /**
     * 初始化默认项目数据库配置
     */
    public void initializeDefaultProjects() {
        // HavenButler主项目
        ProjectDatabaseConfig havenbutlerConfig = new ProjectDatabaseConfig();
        havenbutlerConfig.setProjectId("havenbutler");
        havenbutlerConfig.setDatabaseName("havenbutler_main");
        havenbutlerConfig.setHost("localhost");
        havenbutlerConfig.setPort(5432);
        havenbutlerConfig.setUsername("postgres");
        havenbutlerConfig.setPassword("password");
        havenbutlerConfig.setDriverClassName("org.postgresql.Driver");
        havenbutlerConfig.setMaxConnections(50);
        havenbutlerConfig.setConnectionTimeout(30);
        registerProjectDatabase("havenbutler", havenbutlerConfig);

        // 个人博客项目
        ProjectDatabaseConfig blogConfig = new ProjectDatabaseConfig();
        blogConfig.setProjectId("personal-blog");
        blogConfig.setDatabaseName("personal_blog");
        blogConfig.setHost("localhost");
        blogConfig.setPort(5432);
        blogConfig.setUsername("postgres");
        blogConfig.setPassword("password");
        blogConfig.setDriverClassName("org.postgresql.Driver");
        blogConfig.setMaxConnections(20);
        blogConfig.setConnectionTimeout(30);
        registerProjectDatabase("personal-blog", blogConfig);

        log.info("默认项目数据库配置初始化完成");
    }

    private ProjectDatabaseConfig getProjectDatabaseConfig(String projectId) {
        return projectDatabases.get(projectId);
    }

    private String buildJdbcUrl(ProjectDatabaseConfig config, String familyId) {
        // 构建包含family隔离的JDBC URL
        // 使用schema或database前缀进行隔离
        String databaseName = config.getDatabaseName() + "_" + familyId;
        return String.format("jdbc:postgresql://%s:%d/%s",
                config.getHost(), config.getPort(), databaseName);
    }

    private Connection createConnection(DatabaseConnectionInfo connectionInfo) throws SQLException {
        // 简化实现：这里应该使用连接池
        return java.sql.DriverManager.getConnection(
                connectionInfo.getJdbcUrl(),
                connectionInfo.getUsername(),
                connectionInfo.getPassword()
        );
    }

    private void recordConnectionRequest(String projectId, String familyId) {
        DatabaseConnectionStats stats = connectionStats.get(projectId);
        if (stats != null) {
            stats.setTotalConnections(stats.getTotalConnections() + 1);
            stats.setLastAccessTime(java.time.LocalDateTime.now());
        }
    }
}