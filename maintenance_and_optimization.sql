-- =============================================
-- 企业客户授权管理系统 - 维护和优化脚本
-- =============================================

-- 1. 数据库维护脚本
-- =============================================

-- 1.1 表统计信息更新
CREATE OR REPLACE FUNCTION update_table_statistics()
RETURNS void AS $$
BEGIN
    -- 更新所有表的统计信息
    ANALYZE customers;
    ANALYZE licenses;
    ANALYZE license_keys;
    ANALYZE license_validations;
    ANALYZE license_history;
    ANALYZE system_configs;
    
    RAISE NOTICE '数据库统计信息更新完成';
END;
$$ LANGUAGE plpgsql;

-- 1.2 索引维护脚本
CREATE OR REPLACE FUNCTION maintain_indexes()
RETURNS void AS $$
DECLARE
    idx_record RECORD;
    bloat_threshold FLOAT := 0.2; -- 20%膨胀率阈值
BEGIN
    -- 重建膨胀严重的索引
    FOR idx_record IN 
        SELECT schemaname, tablename, indexname 
        FROM pg_stat_user_indexes 
        WHERE idx_scan < 100 OR idx_tup_read > idx_tup_fetch * 100
    LOOP
        EXECUTE format('REINDEX INDEX CONCURRENTLY %I.%I', 
                      idx_record.schemaname, idx_record.indexname);
        RAISE NOTICE '重建索引: %.%', idx_record.schemaname, idx_record.indexname;
    END LOOP;
    
    RAISE NOTICE '索引维护完成';
END;
$$ LANGUAGE plpgsql;

-- 1.3 数据清理脚本
CREATE OR REPLACE FUNCTION cleanup_old_data(
    validation_retention_days INTEGER DEFAULT 365,
    history_retention_days INTEGER DEFAULT 1095
)
RETURNS void AS $$
DECLARE
    deleted_validations INTEGER;
    deleted_history INTEGER;
BEGIN
    -- 清理过期的验证日志
    DELETE FROM license_validations 
    WHERE validation_time < CURRENT_DATE - INTERVAL '1 day' * validation_retention_days;
    GET DIAGNOSTICS deleted_validations = ROW_COUNT;
    
    -- 清理过期的历史记录
    DELETE FROM license_history 
    WHERE operation_time < CURRENT_DATE - INTERVAL '1 day' * history_retention_days;
    GET DIAGNOSTICS deleted_history = ROW_COUNT;
    
    -- 清理空分区
    PERFORM cleanup_empty_partitions('license_validations');
    
    RAISE NOTICE '数据清理完成: 验证日志 % 条, 历史记录 % 条', 
                 deleted_validations, deleted_history;
END;
$$ LANGUAGE plpgsql;

-- 1.4 分区管理脚本
CREATE OR REPLACE FUNCTION cleanup_empty_partitions(table_name TEXT)
RETURNS void AS $$
DECLARE
    partition_record RECORD;
    row_count INTEGER;
BEGIN
    FOR partition_record IN 
        SELECT schemaname, tablename 
        FROM pg_tables 
        WHERE tablename LIKE table_name || '_%'
        AND schemaname = 'public'
    LOOP
        EXECUTE format('SELECT COUNT(*) FROM %I.%I', 
                      partition_record.schemaname, partition_record.tablename)
        INTO row_count;
        
        IF row_count = 0 THEN
            EXECUTE format('DROP TABLE %I.%I', 
                          partition_record.schemaname, partition_record.tablename);
            RAISE NOTICE '删除空分区: %', partition_record.tablename;
        END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- 1.5 自动创建未来分区
CREATE OR REPLACE FUNCTION create_future_partitions(months_ahead INTEGER DEFAULT 3)
RETURNS void AS $$
DECLARE
    i INTEGER;
    partition_date DATE;
BEGIN
    FOR i IN 1..months_ahead LOOP
        partition_date := (date_trunc('month', CURRENT_DATE) + (i || ' month')::INTERVAL)::DATE;
        PERFORM create_monthly_partition('license_validations', partition_date);
        RAISE NOTICE '创建分区: license_validations_%', to_char(partition_date, 'YYYY_MM');
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- 2. 性能监控脚本
-- =============================================

-- 2.1 慢查询监控视图
CREATE OR REPLACE VIEW v_slow_queries AS
SELECT 
    query,
    calls,
    total_time,
    mean_time,
    rows,
    100.0 * shared_blks_hit / nullif(shared_blks_hit + shared_blks_read, 0) AS hit_percent
FROM pg_stat_statements 
WHERE mean_time > 100 -- 平均执行时间超过100ms
ORDER BY mean_time DESC
LIMIT 20;

-- 2.2 表大小监控视图
CREATE OR REPLACE VIEW v_table_sizes AS
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as total_size,
    pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) as table_size,
    pg_size_pretty(pg_indexes_size(schemaname||'.'||tablename)) as index_size,
    n_tup_ins + n_tup_upd + n_tup_del as total_operations,
    n_live_tup as live_tuples,
    n_dead_tup as dead_tuples
FROM pg_stat_user_tables 
JOIN pg_tables USING (schemaname, tablename)
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- 2.3 索引使用情况监控视图
CREATE OR REPLACE VIEW v_index_usage AS
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch,
    pg_size_pretty(pg_relation_size(indexrelid)) as index_size,
    CASE 
        WHEN idx_scan = 0 THEN '未使用'
        WHEN idx_scan < 100 THEN '低频使用'
        ELSE '正常使用'
    END as usage_status
FROM pg_stat_user_indexes
ORDER BY idx_scan ASC, pg_relation_size(indexrelid) DESC;

-- 3. 业务监控脚本
-- =============================================

-- 3.1 授权健康状况检查
CREATE OR REPLACE FUNCTION check_license_health()
RETURNS TABLE (
    check_type TEXT,
    status TEXT,
    count INTEGER,
    details TEXT
) AS $$
BEGIN
    -- 即将到期的授权
    RETURN QUERY
    SELECT 
        '即将到期授权'::TEXT,
        CASE WHEN COUNT(*) > 0 THEN '警告' ELSE '正常' END::TEXT,
        COUNT(*)::INTEGER,
        '未来30天内到期的授权数量'::TEXT
    FROM licenses l
    WHERE l.expires_at BETWEEN CURRENT_TIMESTAMP AND CURRENT_TIMESTAMP + INTERVAL '30 days'
      AND l.status = 'active';
    
    -- 已过期的授权
    RETURN QUERY
    SELECT 
        '已过期授权'::TEXT,
        CASE WHEN COUNT(*) > 0 THEN '错误' ELSE '正常' END::TEXT,
        COUNT(*)::INTEGER,
        '需要续费或停用的授权数量'::TEXT
    FROM licenses l
    WHERE l.expires_at < CURRENT_TIMESTAMP 
      AND l.status = 'active';
    
    -- 用户数超限的授权
    RETURN QUERY
    SELECT 
        '用户数超限'::TEXT,
        CASE WHEN COUNT(*) > 0 THEN '错误' ELSE '正常' END::TEXT,
        COUNT(*)::INTEGER,
        '已使用用户数超过最大用户数的授权'::TEXT
    FROM licenses l
    WHERE l.used_users > l.max_users;
    
    -- 验证失败率高的授权
    RETURN QUERY
    WITH validation_stats AS (
        SELECT 
            lv.license_id,
            COUNT(*) as total_validations,
            COUNT(CASE WHEN validation_result != 'success' THEN 1 END) as failed_validations
        FROM license_validations lv
        WHERE lv.validation_time >= CURRENT_TIMESTAMP - INTERVAL '24 hours'
        GROUP BY lv.license_id
        HAVING COUNT(*) >= 10
    )
    SELECT 
        '验证失败率高'::TEXT,
        CASE WHEN COUNT(*) > 0 THEN '警告' ELSE '正常' END::TEXT,
        COUNT(*)::INTEGER,
        '24小时内验证失败率超过20%的授权数量'::TEXT
    FROM validation_stats
    WHERE failed_validations * 1.0 / total_validations > 0.2;
END;
$$ LANGUAGE plpgsql;

-- 3.2 系统性能检查
CREATE OR REPLACE FUNCTION check_system_performance()
RETURNS TABLE (
    metric_name TEXT,
    current_value TEXT,
    status TEXT,
    recommendation TEXT
) AS $$
DECLARE
    db_size BIGINT;
    active_connections INTEGER;
    cache_hit_ratio FLOAT;
BEGIN
    -- 数据库大小检查
    SELECT pg_database_size(current_database()) INTO db_size;
    RETURN QUERY
    SELECT 
        '数据库大小'::TEXT,
        pg_size_pretty(db_size),
        CASE WHEN db_size > 100 * 1024^3 THEN '警告' ELSE '正常' END::TEXT,
        CASE WHEN db_size > 100 * 1024^3 THEN '考虑数据归档' ELSE '无需操作' END::TEXT;
    
    -- 活跃连接数检查
    SELECT COUNT(*) INTO active_connections 
    FROM pg_stat_activity 
    WHERE state = 'active' AND datname = current_database();
    
    RETURN QUERY
    SELECT 
        '活跃连接数'::TEXT,
        active_connections::TEXT,
        CASE WHEN active_connections > 50 THEN '警告' ELSE '正常' END::TEXT,
        CASE WHEN active_connections > 50 THEN '检查连接池配置' ELSE '无需操作' END::TEXT;
    
    -- 缓存命中率检查
    SELECT 
        100.0 * sum(blks_hit) / (sum(blks_hit) + sum(blks_read)) INTO cache_hit_ratio
    FROM pg_stat_database 
    WHERE datname = current_database();
    
    RETURN QUERY
    SELECT 
        '缓存命中率'::TEXT,
        ROUND(cache_hit_ratio, 2)::TEXT || '%',
        CASE WHEN cache_hit_ratio < 95 THEN '警告' ELSE '正常' END::TEXT,
        CASE WHEN cache_hit_ratio < 95 THEN '考虑增加shared_buffers' ELSE '无需操作' END::TEXT;
END;
$$ LANGUAGE plpgsql;

-- 4. 报表生成脚本
-- =============================================

-- 4.1 授权使用情况日报
CREATE OR REPLACE FUNCTION generate_daily_license_report(report_date DATE DEFAULT CURRENT_DATE)
RETURNS TABLE (
    customer_name TEXT,
    license_code TEXT,
    product_name TEXT,
    total_validations BIGINT,
    success_validations BIGINT,
    failed_validations BIGINT,
    success_rate NUMERIC,
    unique_ips BIGINT,
    max_concurrent_users INTEGER
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        c.customer_name::TEXT,
        l.license_code::TEXT,
        l.product_name::TEXT,
        COUNT(lv.validation_id) as total_validations,
        COUNT(CASE WHEN lv.validation_result = 'success' THEN 1 END) as success_validations,
        COUNT(CASE WHEN lv.validation_result != 'success' THEN 1 END) as failed_validations,
        ROUND(
            100.0 * COUNT(CASE WHEN lv.validation_result = 'success' THEN 1 END) / 
            NULLIF(COUNT(lv.validation_id), 0), 2
        ) as success_rate,
        COUNT(DISTINCT lv.client_ip) as unique_ips,
        l.max_users
    FROM licenses l
    JOIN customers c ON l.customer_id = c.customer_id
    LEFT JOIN license_validations lv ON l.license_id = lv.license_id 
        AND lv.validation_time::DATE = report_date
    WHERE l.status = 'active'
    GROUP BY c.customer_name, l.license_code, l.product_name, l.max_users
    ORDER BY total_validations DESC;
END;
$$ LANGUAGE plpgsql;

-- 4.2 客户授权汇总报表
CREATE OR REPLACE FUNCTION generate_customer_summary_report()
RETURNS TABLE (
    customer_name TEXT,
    company_name TEXT,
    total_licenses INTEGER,
    active_licenses INTEGER,
    expired_licenses INTEGER,
    total_users INTEGER,
    used_users INTEGER,
    utilization_rate NUMERIC,
    last_validation TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        c.customer_name::TEXT,
        c.company_name::TEXT,
        COUNT(l.license_id)::INTEGER as total_licenses,
        COUNT(CASE WHEN l.status = 'active' THEN 1 END)::INTEGER as active_licenses,
        COUNT(CASE WHEN l.expires_at < CURRENT_TIMESTAMP THEN 1 END)::INTEGER as expired_licenses,
        COALESCE(SUM(l.max_users), 0)::INTEGER as total_users,
        COALESCE(SUM(l.used_users), 0)::INTEGER as used_users,
        ROUND(
            100.0 * COALESCE(SUM(l.used_users), 0) / 
            NULLIF(COALESCE(SUM(l.max_users), 0), 0), 2
        ) as utilization_rate,
        MAX(lv.validation_time) as last_validation
    FROM customers c
    LEFT JOIN licenses l ON c.customer_id = l.customer_id
    LEFT JOIN license_validations lv ON l.license_id = lv.license_id
    WHERE c.status = 'active'
    GROUP BY c.customer_id, c.customer_name, c.company_name
    ORDER BY total_licenses DESC, customer_name;
END;
$$ LANGUAGE plpgsql;

-- 5. 定时任务脚本
-- =============================================

-- 5.1 每日维护任务
CREATE OR REPLACE FUNCTION daily_maintenance_task()
RETURNS void AS $$
BEGIN
    -- 更新统计信息
    PERFORM update_table_statistics();
    
    -- 创建未来分区
    PERFORM create_future_partitions(2);
    
    -- 更新过期授权状态
    UPDATE licenses 
    SET status = 'expired', 
        updated_at = CURRENT_TIMESTAMP
    WHERE expires_at < CURRENT_TIMESTAMP 
      AND status = 'active';
    
    -- 记录维护日志
    INSERT INTO system_configs (config_key, config_value, config_type, config_group, description)
    VALUES ('maintenance.last_run', CURRENT_TIMESTAMP::TEXT, 'string', 'maintenance', '上次维护时间')
    ON CONFLICT (config_key) 
    DO UPDATE SET 
        config_value = EXCLUDED.config_value,
        updated_at = CURRENT_TIMESTAMP;
    
    RAISE NOTICE '每日维护任务完成: %', CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql;

-- 5.2 每周维护任务
CREATE OR REPLACE FUNCTION weekly_maintenance_task()
RETURNS void AS $$
BEGIN
    -- 维护索引
    PERFORM maintain_indexes();
    
    -- 清理7天前的验证日志（保留1年）
    PERFORM cleanup_old_data(365, 1095);
    
    -- 更新表的自动VACUUM设置
    ALTER TABLE license_validations SET (autovacuum_vacuum_scale_factor = 0.1);
    ALTER TABLE license_history SET (autovacuum_vacuum_scale_factor = 0.2);
    
    RAISE NOTICE '每周维护任务完成: %', CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql;

-- 6. 一键执行脚本
-- =============================================

-- 执行健康检查
SELECT * FROM check_license_health();
SELECT * FROM check_system_performance();

-- 查看系统监控信息
SELECT 'Table Sizes' as report_type;
SELECT * FROM v_table_sizes LIMIT 10;

SELECT 'Index Usage' as report_type;
SELECT * FROM v_index_usage WHERE usage_status = '未使用' LIMIT 10;

-- 生成今日报表
SELECT 'Daily License Report' as report_type;
SELECT * FROM generate_daily_license_report(CURRENT_DATE) LIMIT 10;

SELECT 'Customer Summary Report' as report_type;
SELECT * FROM generate_customer_summary_report() LIMIT 10;