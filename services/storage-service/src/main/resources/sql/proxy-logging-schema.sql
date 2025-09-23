-- Storage Service 代理日志系统数据库表结构
-- 用于创建操作日志、性能日志、安全日志相关表

-- ================================
-- 1. 操作日志表
-- ================================
CREATE TABLE IF NOT EXISTS operation_logs (
    id BIGSERIAL PRIMARY KEY,
    family_id VARCHAR(64) NOT NULL COMMENT '家庭ID',
    service_type VARCHAR(20) NOT NULL COMMENT '服务类型(POSTGRESQL/MONGODB/REDIS/HTTP_API)',
    client_ip INET NOT NULL COMMENT '客户端IP地址',
    user_id VARCHAR(64) COMMENT '用户ID',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型(SELECT/INSERT/UPDATE/DELETE等)',
    database_name VARCHAR(100) COMMENT '数据库名称',
    table_or_collection VARCHAR(100) COMMENT '表名或集合名',
    operation_content TEXT COMMENT '操作内容(脱敏后)',
    execution_time_ms INTEGER COMMENT '执行时间(毫秒)',
    result_status VARCHAR(20) NOT NULL COMMENT '结果状态(SUCCESS/FAILED/BLOCKED/IN_PROGRESS)',
    error_message TEXT COMMENT '错误信息',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    trace_id VARCHAR(64) COMMENT '请求追踪ID',
    session_id VARCHAR(64) COMMENT '会话ID',
    affected_rows INTEGER COMMENT '影响行数',
    metadata TEXT COMMENT '额外元数据(JSON格式)'
);

-- 操作日志表索引
CREATE INDEX IF NOT EXISTS idx_operation_logs_family_created ON operation_logs(family_id, created_at);
CREATE INDEX IF NOT EXISTS idx_operation_logs_service_created ON operation_logs(service_type, created_at);
CREATE INDEX IF NOT EXISTS idx_operation_logs_client_ip ON operation_logs(client_ip);
CREATE INDEX IF NOT EXISTS idx_operation_logs_result_status ON operation_logs(result_status);
CREATE INDEX IF NOT EXISTS idx_operation_logs_user_id ON operation_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_operation_logs_trace_id ON operation_logs(trace_id);

-- 操作日志表分区(按月分区)
-- CREATE TABLE operation_logs_y2024m01 PARTITION OF operation_logs
-- FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

-- ================================
-- 2. 安全日志表
-- ================================
CREATE TABLE IF NOT EXISTS security_logs (
    id BIGSERIAL PRIMARY KEY,
    family_id VARCHAR(64) COMMENT '家庭ID',
    client_ip INET NOT NULL COMMENT '客户端IP地址',
    event_type VARCHAR(50) NOT NULL COMMENT '事件类型',
    service_type VARCHAR(20) COMMENT '服务类型',
    event_details TEXT COMMENT '事件详细信息',
    risk_level VARCHAR(20) NOT NULL COMMENT '风险等级(LOW/MEDIUM/HIGH/CRITICAL)',
    user_id VARCHAR(64) COMMENT '用户ID',
    user_agent VARCHAR(500) COMMENT '用户代理信息',
    request_path VARCHAR(200) COMMENT '请求路径',
    response_code INTEGER COMMENT '响应状态码',
    geo_location VARCHAR(100) COMMENT '地理位置',
    is_handled BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已处理',
    handled_by VARCHAR(64) COMMENT '处理人员',
    handled_at TIMESTAMP WITH TIME ZONE COMMENT '处理时间',
    handling_notes TEXT COMMENT '处理备注',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    trace_id VARCHAR(64) COMMENT '请求追踪ID'
);

-- 安全日志表索引
CREATE INDEX IF NOT EXISTS idx_security_logs_family_risk ON security_logs(family_id, risk_level, created_at);
CREATE INDEX IF NOT EXISTS idx_security_logs_client_ip ON security_logs(client_ip, created_at);
CREATE INDEX IF NOT EXISTS idx_security_logs_event_type ON security_logs(event_type, created_at);
CREATE INDEX IF NOT EXISTS idx_security_logs_risk_level ON security_logs(risk_level, created_at);
CREATE INDEX IF NOT EXISTS idx_security_logs_unhandled ON security_logs(is_handled, risk_level) WHERE is_handled = FALSE;

-- ================================
-- 3. 性能日志表
-- ================================
CREATE TABLE IF NOT EXISTS performance_logs (
    id BIGSERIAL PRIMARY KEY,
    service_type VARCHAR(20) NOT NULL COMMENT '服务类型',
    metric_name VARCHAR(50) NOT NULL COMMENT '指标名称',
    metric_value NUMERIC(10,2) NOT NULL COMMENT '指标值',
    metric_unit VARCHAR(20) COMMENT '指标单位',
    instance_id VARCHAR(100) COMMENT '实例ID',
    hostname VARCHAR(100) COMMENT '主机名',
    tags TEXT COMMENT '标签信息(JSON格式)',
    threshold_warning NUMERIC(10,2) COMMENT '警告阈值',
    threshold_critical NUMERIC(10,2) COMMENT '严重阈值',
    is_threshold_exceeded BOOLEAN DEFAULT FALSE COMMENT '是否超过阈值',
    collection_interval INTEGER COMMENT '采集间隔(秒)',
    data_source VARCHAR(20) COMMENT '数据来源',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    metadata TEXT COMMENT '额外元数据'
);

-- 性能日志表索引
CREATE INDEX IF NOT EXISTS idx_performance_logs_service_metric ON performance_logs(service_type, metric_name, created_at);
CREATE INDEX IF NOT EXISTS idx_performance_logs_instance_created ON performance_logs(instance_id, created_at);
CREATE INDEX IF NOT EXISTS idx_performance_logs_created_at ON performance_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_performance_logs_threshold ON performance_logs(is_threshold_exceeded, created_at) WHERE is_threshold_exceeded = TRUE;

-- ================================
-- 4. 代理连接状态表(可选)
-- ================================
CREATE TABLE IF NOT EXISTS proxy_connections (
    id BIGSERIAL PRIMARY KEY,
    client_ip INET NOT NULL COMMENT '客户端IP',
    service_type VARCHAR(20) NOT NULL COMMENT '代理服务类型',
    family_id VARCHAR(64) COMMENT '家庭ID',
    user_id VARCHAR(64) COMMENT '用户ID',
    session_id VARCHAR(64) COMMENT '会话ID',
    backend_host VARCHAR(100) NOT NULL COMMENT '后端主机',
    backend_port INTEGER NOT NULL COMMENT '后端端口',
    connection_status VARCHAR(20) NOT NULL COMMENT '连接状态(ACTIVE/INACTIVE/ERROR)',
    connected_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '连接时间',
    disconnected_at TIMESTAMP WITH TIME ZONE COMMENT '断开时间',
    total_operations INTEGER DEFAULT 0 COMMENT '总操作数',
    total_bytes_sent BIGINT DEFAULT 0 COMMENT '发送字节数',
    total_bytes_received BIGINT DEFAULT 0 COMMENT '接收字节数',
    last_activity_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP COMMENT '最后活动时间'
);

-- 代理连接状态表索引
CREATE INDEX IF NOT EXISTS idx_proxy_connections_client_ip ON proxy_connections(client_ip);
CREATE INDEX IF NOT EXISTS idx_proxy_connections_service_type ON proxy_connections(service_type);
CREATE INDEX IF NOT EXISTS idx_proxy_connections_status ON proxy_connections(connection_status);
CREATE INDEX IF NOT EXISTS idx_proxy_connections_family_id ON proxy_connections(family_id);

-- ================================
-- 5. 视图定义
-- ================================

-- 实时操作统计视图
CREATE OR REPLACE VIEW v_realtime_operation_stats AS
SELECT
    service_type,
    COUNT(*) as total_operations,
    COUNT(CASE WHEN result_status = 'SUCCESS' THEN 1 END) as successful_operations,
    COUNT(CASE WHEN result_status = 'FAILED' THEN 1 END) as failed_operations,
    COUNT(CASE WHEN result_status = 'BLOCKED' THEN 1 END) as blocked_operations,
    ROUND(AVG(execution_time_ms), 2) as avg_execution_time,
    MAX(execution_time_ms) as max_execution_time
FROM operation_logs
WHERE created_at >= NOW() - INTERVAL '1 hour'
GROUP BY service_type;

-- 安全事件统计视图
CREATE OR REPLACE VIEW v_security_event_stats AS
SELECT
    service_type,
    event_type,
    risk_level,
    COUNT(*) as event_count,
    COUNT(CASE WHEN is_handled = TRUE THEN 1 END) as handled_count,
    ROUND(COUNT(CASE WHEN is_handled = TRUE THEN 1 END) * 100.0 / COUNT(*), 2) as handling_rate
FROM security_logs
WHERE created_at >= NOW() - INTERVAL '24 hours'
GROUP BY service_type, event_type, risk_level;

-- 性能指标汇总视图
CREATE OR REPLACE VIEW v_performance_metrics_summary AS
SELECT
    service_type,
    metric_name,
    ROUND(AVG(metric_value), 2) as avg_value,
    ROUND(MIN(metric_value), 2) as min_value,
    ROUND(MAX(metric_value), 2) as max_value,
    COUNT(*) as sample_count,
    COUNT(CASE WHEN is_threshold_exceeded = TRUE THEN 1 END) as threshold_violations
FROM performance_logs
WHERE created_at >= NOW() - INTERVAL '1 hour'
GROUP BY service_type, metric_name;

-- ================================
-- 6. 存储过程定义
-- ================================

-- 清理过期日志的存储过程
CREATE OR REPLACE FUNCTION cleanup_expired_logs(retention_days INTEGER DEFAULT 30)
RETURNS TABLE(
    operation_logs_deleted INTEGER,
    security_logs_deleted INTEGER,
    performance_logs_deleted INTEGER
) AS $$
DECLARE
    op_deleted INTEGER := 0;
    sec_deleted INTEGER := 0;
    perf_deleted INTEGER := 0;
    cutoff_time TIMESTAMP WITH TIME ZONE;
    security_cutoff_time TIMESTAMP WITH TIME ZONE;
BEGIN
    -- 计算截止时间
    cutoff_time := NOW() - (retention_days || ' days')::INTERVAL;
    security_cutoff_time := NOW() - (retention_days * 6 || ' days')::INTERVAL; -- 安全日志保留更久

    -- 清理操作日志
    DELETE FROM operation_logs WHERE created_at < cutoff_time;
    GET DIAGNOSTICS op_deleted = ROW_COUNT;

    -- 清理安全日志
    DELETE FROM security_logs WHERE created_at < security_cutoff_time;
    GET DIAGNOSTICS sec_deleted = ROW_COUNT;

    -- 清理性能日志
    DELETE FROM performance_logs WHERE created_at < cutoff_time;
    GET DIAGNOSTICS perf_deleted = ROW_COUNT;

    -- 返回清理结果
    RETURN QUERY SELECT op_deleted, sec_deleted, perf_deleted;
END;
$$ LANGUAGE plpgsql;

-- 计算安全评分的函数
CREATE OR REPLACE FUNCTION calculate_security_score(
    p_family_id VARCHAR(64),
    p_start_time TIMESTAMP WITH TIME ZONE,
    p_end_time TIMESTAMP WITH TIME ZONE
) RETURNS INTEGER AS $$
DECLARE
    score INTEGER := 0;
    critical_count INTEGER := 0;
    high_count INTEGER := 0;
    medium_count INTEGER := 0;
    low_count INTEGER := 0;
BEGIN
    -- 统计各风险级别的事件数量
    SELECT
        COUNT(CASE WHEN risk_level = 'CRITICAL' THEN 1 END),
        COUNT(CASE WHEN risk_level = 'HIGH' THEN 1 END),
        COUNT(CASE WHEN risk_level = 'MEDIUM' THEN 1 END),
        COUNT(CASE WHEN risk_level = 'LOW' THEN 1 END)
    INTO critical_count, high_count, medium_count, low_count
    FROM security_logs
    WHERE (p_family_id IS NULL OR family_id = p_family_id)
    AND created_at BETWEEN p_start_time AND p_end_time;

    -- 计算安全评分(分数越高表示风险越大)
    score := critical_count * 10 + high_count * 5 + medium_count * 2 + low_count * 1;

    RETURN score;
END;
$$ LANGUAGE plpgsql;

-- ================================
-- 7. 触发器定义
-- ================================

-- 自动更新性能日志阈值状态的触发器函数
CREATE OR REPLACE FUNCTION update_threshold_status()
RETURNS TRIGGER AS $$
BEGIN
    -- 检查是否超过阈值
    IF NEW.threshold_critical IS NOT NULL AND NEW.metric_value >= NEW.threshold_critical THEN
        NEW.is_threshold_exceeded = TRUE;
    ELSIF NEW.threshold_warning IS NOT NULL AND NEW.metric_value >= NEW.threshold_warning THEN
        NEW.is_threshold_exceeded = TRUE;
    ELSE
        NEW.is_threshold_exceeded = FALSE;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 创建触发器
DROP TRIGGER IF EXISTS trigger_update_threshold_status ON performance_logs;
CREATE TRIGGER trigger_update_threshold_status
    BEFORE INSERT OR UPDATE ON performance_logs
    FOR EACH ROW
    EXECUTE FUNCTION update_threshold_status();

-- ================================
-- 8. 权限设置
-- ================================

-- 创建专用的日志用户(可选)
-- CREATE USER log_service WITH PASSWORD 'secure_password';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON operation_logs TO log_service;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON security_logs TO log_service;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON performance_logs TO log_service;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON proxy_connections TO log_service;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO log_service;

-- ================================
-- 9. 注释和文档
-- ================================

COMMENT ON TABLE operation_logs IS '操作日志表 - 记录所有数据库操作和API调用';
COMMENT ON TABLE security_logs IS '安全日志表 - 记录安全事件和异常行为';
COMMENT ON TABLE performance_logs IS '性能日志表 - 记录系统性能指标';
COMMENT ON TABLE proxy_connections IS '代理连接表 - 记录代理连接状态';

COMMENT ON VIEW v_realtime_operation_stats IS '实时操作统计视图 - 最近1小时的操作统计';
COMMENT ON VIEW v_security_event_stats IS '安全事件统计视图 - 最近24小时的安全事件统计';
COMMENT ON VIEW v_performance_metrics_summary IS '性能指标汇总视图 - 最近1小时的性能指标汇总';

-- ================================
-- 10. 初始化数据
-- ================================

-- 插入一些测试数据(可选)
-- INSERT INTO operation_logs (family_id, service_type, client_ip, operation_type, result_status, operation_content)
-- VALUES ('family_test_001', 'POSTGRESQL', '127.0.0.1', 'SELECT', 'SUCCESS', 'SELECT 1');

-- 显示创建完成信息
DO $$
BEGIN
    RAISE NOTICE '============================================';
    RAISE NOTICE 'Storage Service 代理日志系统表结构创建完成!';
    RAISE NOTICE '============================================';
    RAISE NOTICE '创建的表：';
    RAISE NOTICE '  - operation_logs (操作日志表)';
    RAISE NOTICE '  - security_logs (安全日志表)';
    RAISE NOTICE '  - performance_logs (性能日志表)';
    RAISE NOTICE '  - proxy_connections (代理连接表)';
    RAISE NOTICE '';
    RAISE NOTICE '创建的视图：';
    RAISE NOTICE '  - v_realtime_operation_stats';
    RAISE NOTICE '  - v_security_event_stats';
    RAISE NOTICE '  - v_performance_metrics_summary';
    RAISE NOTICE '';
    RAISE NOTICE '创建的函数：';
    RAISE NOTICE '  - cleanup_expired_logs()';
    RAISE NOTICE '  - calculate_security_score()';
    RAISE NOTICE '============================================';
END $$;