-- =============================================
-- 企业客户授权管理系统数据库设计
-- 数据库：PostgreSQL 14+
-- 设计时间：2025-07-30
-- =============================================

-- 创建数据库（如果需要）
-- CREATE DATABASE enterprise_license_system;

-- 使用数据库
-- \c enterprise_license_system;

-- 启用UUID扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =============================================
-- 1. 客户表（customers）
-- =============================================
CREATE TABLE customers (
    customer_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_code VARCHAR(50) UNIQUE NOT NULL,              -- 客户编码
    customer_name VARCHAR(200) NOT NULL,                    -- 客户名称
    company_name VARCHAR(300) NOT NULL,                     -- 公司名称
    company_type VARCHAR(50) DEFAULT 'enterprise',          -- 公司类型：enterprise/government/individual
    contact_person VARCHAR(100) NOT NULL,                   -- 联系人姓名
    contact_phone VARCHAR(50),                              -- 联系电话
    contact_email VARCHAR(255) NOT NULL,                    -- 联系邮箱
    contact_address TEXT,                                   -- 联系地址
    business_license VARCHAR(100),                          -- 营业执照号
    tax_number VARCHAR(50),                                 -- 税号
    industry VARCHAR(100),                                  -- 所属行业
    company_scale VARCHAR(50),                              -- 公司规模：small/medium/large/enterprise
    status VARCHAR(20) DEFAULT 'active',                    -- 状态：active/inactive/suspended/blacklist
    remark TEXT,                                           -- 备注信息
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,                                       -- 创建人ID
    updated_by UUID,                                       -- 更新人ID
    version INTEGER DEFAULT 1                              -- 乐观锁版本号
);

-- 客户表索引
CREATE INDEX idx_customers_code ON customers(customer_code);
CREATE INDEX idx_customers_name ON customers(customer_name);
CREATE INDEX idx_customers_company ON customers(company_name);
CREATE INDEX idx_customers_email ON customers(contact_email);
CREATE INDEX idx_customers_status ON customers(status);
CREATE INDEX idx_customers_created_at ON customers(created_at);

-- 客户表约束
ALTER TABLE customers ADD CONSTRAINT chk_customers_status 
    CHECK (status IN ('active', 'inactive', 'suspended', 'blacklist'));
ALTER TABLE customers ADD CONSTRAINT chk_customers_email 
    CHECK (contact_email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

-- =============================================
-- 2. 密钥表（license_keys）
-- =============================================
CREATE TABLE license_keys (
    key_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    key_name VARCHAR(100) UNIQUE NOT NULL,                 -- 密钥名称
    key_alias VARCHAR(50) UNIQUE NOT NULL,                 -- 密钥别名
    algorithm_type VARCHAR(20) DEFAULT 'RSA2048',          -- 算法类型：RSA2048/RSA4096/ECC256/ECC384
    public_key TEXT NOT NULL,                              -- 公钥（PEM格式）
    private_key TEXT NOT NULL,                             -- 私钥（加密存储）
    key_purpose VARCHAR(50) DEFAULT 'license_signing',     -- 密钥用途：license_signing/verification/backup
    key_strength INTEGER DEFAULT 2048,                     -- 密钥强度
    fingerprint VARCHAR(128) UNIQUE NOT NULL,              -- 密钥指纹（SHA256）
    status VARCHAR(20) DEFAULT 'active',                   -- 状态：active/inactive/revoked/expired
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,                   -- 密钥过期时间
    last_used_at TIMESTAMP WITH TIME ZONE,                 -- 最后使用时间
    usage_count INTEGER DEFAULT 0,                         -- 使用次数
    created_by UUID,                                       -- 创建人ID
    remark TEXT                                            -- 备注信息
);

-- 密钥表索引
CREATE INDEX idx_license_keys_name ON license_keys(key_name);
CREATE INDEX idx_license_keys_alias ON license_keys(key_alias);
CREATE INDEX idx_license_keys_algorithm ON license_keys(algorithm_type);
CREATE INDEX idx_license_keys_status ON license_keys(status);
CREATE INDEX idx_license_keys_fingerprint ON license_keys(fingerprint);
CREATE INDEX idx_license_keys_expires_at ON license_keys(expires_at);

-- 密钥表约束
ALTER TABLE license_keys ADD CONSTRAINT chk_license_keys_status 
    CHECK (status IN ('active', 'inactive', 'revoked', 'expired'));
ALTER TABLE license_keys ADD CONSTRAINT chk_license_keys_algorithm 
    CHECK (algorithm_type IN ('RSA2048', 'RSA4096', 'ECC256', 'ECC384'));
ALTER TABLE license_keys ADD CONSTRAINT chk_license_keys_strength 
    CHECK (key_strength IN (2048, 4096, 256, 384));

-- =============================================
-- 3. 授权表（licenses）
-- =============================================
CREATE TABLE licenses (
    license_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id UUID NOT NULL REFERENCES customers(customer_id) ON DELETE CASCADE,
    key_id UUID NOT NULL REFERENCES license_keys(key_id) ON DELETE RESTRICT,
    license_code VARCHAR(100) UNIQUE NOT NULL,             -- 授权码
    product_name VARCHAR(200) NOT NULL,                    -- 产品名称
    product_version VARCHAR(50),                           -- 产品版本
    license_type VARCHAR(50) DEFAULT 'commercial',         -- 授权类型：trial/commercial/enterprise/oem
    license_model VARCHAR(50) DEFAULT 'perpetual',         -- 授权模式：perpetual/subscription/concurrent
    
    -- 时间相关
    issued_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,  -- 签发时间
    effective_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP, -- 生效时间
    expires_at TIMESTAMP WITH TIME ZONE,                   -- 到期时间
    
    -- 用户限制
    max_users INTEGER DEFAULT 1,                           -- 最大用户数
    concurrent_users INTEGER DEFAULT 1,                    -- 并发用户数
    used_users INTEGER DEFAULT 0,                          -- 已使用用户数
    
    -- 功能权限（JSON格式存储）
    feature_permissions JSONB DEFAULT '{}',                -- 功能权限列表
    module_permissions JSONB DEFAULT '{}',                 -- 模块权限列表
    api_permissions JSONB DEFAULT '{}',                    -- API权限列表
    
    -- 客户端限制
    allowed_ips JSONB DEFAULT '[]',                        -- 允许的IP地址列表
    allowed_macs JSONB DEFAULT '[]',                       -- 允许的MAC地址列表
    hardware_fingerprint TEXT,                            -- 硬件指纹
    machine_code VARCHAR(100),                            -- 机器码
    
    -- 状态和控制
    status VARCHAR(20) DEFAULT 'active',                   -- 状态：active/inactive/suspended/expired/revoked
    auto_renewal BOOLEAN DEFAULT FALSE,                    -- 是否自动续期
    renewal_days INTEGER DEFAULT 0,                        -- 续期天数
    warning_days INTEGER DEFAULT 30,                       -- 到期提醒天数
    
    -- 元数据
    metadata JSONB DEFAULT '{}',                           -- 扩展元数据
    digital_signature TEXT,                               -- 数字签名
    checksum VARCHAR(128),                                -- 校验和
    
    -- 审计字段
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,                                       -- 创建人ID
    updated_by UUID,                                       -- 更新人ID
    approved_by UUID,                                      -- 审批人ID
    approved_at TIMESTAMP WITH TIME ZONE,                 -- 审批时间
    version INTEGER DEFAULT 1,                            -- 版本号
    remark TEXT                                           -- 备注信息
);

-- 授权表索引
CREATE INDEX idx_licenses_customer ON licenses(customer_id);
CREATE INDEX idx_licenses_key ON licenses(key_id);
CREATE INDEX idx_licenses_code ON licenses(license_code);
CREATE INDEX idx_licenses_product ON licenses(product_name);
CREATE INDEX idx_licenses_type ON licenses(license_type);
CREATE INDEX idx_licenses_status ON licenses(status);
CREATE INDEX idx_licenses_expires_at ON licenses(expires_at);
CREATE INDEX idx_licenses_effective_at ON licenses(effective_at);
CREATE INDEX idx_licenses_created_at ON licenses(created_at);
CREATE INDEX idx_licenses_feature_permissions ON licenses USING GIN(feature_permissions);
CREATE INDEX idx_licenses_metadata ON licenses USING GIN(metadata);

-- 授权表约束
ALTER TABLE licenses ADD CONSTRAINT chk_licenses_status 
    CHECK (status IN ('active', 'inactive', 'suspended', 'expired', 'revoked'));
ALTER TABLE licenses ADD CONSTRAINT chk_licenses_type 
    CHECK (license_type IN ('trial', 'commercial', 'enterprise', 'oem', 'educational'));
ALTER TABLE licenses ADD CONSTRAINT chk_licenses_model 
    CHECK (license_model IN ('perpetual', 'subscription', 'concurrent', 'floating'));
ALTER TABLE licenses ADD CONSTRAINT chk_licenses_max_users 
    CHECK (max_users > 0);
ALTER TABLE licenses ADD CONSTRAINT chk_licenses_concurrent_users 
    CHECK (concurrent_users > 0);
ALTER TABLE licenses ADD CONSTRAINT chk_licenses_used_users 
    CHECK (used_users >= 0 AND used_users <= max_users);
ALTER TABLE licenses ADD CONSTRAINT chk_licenses_dates 
    CHECK (effective_at <= expires_at);

-- =============================================
-- 4. 授权验证日志表（license_validations）
-- =============================================
CREATE TABLE license_validations (
    validation_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    license_id UUID NOT NULL REFERENCES licenses(license_id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES customers(customer_id) ON DELETE CASCADE,
    
    -- 验证信息
    validation_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    validation_result VARCHAR(20) NOT NULL,               -- 验证结果：success/failure/warning/error
    result_code VARCHAR(50),                              -- 结果码
    result_message TEXT,                                  -- 结果消息
    
    -- 客户端信息
    client_ip INET,                                       -- 客户端IP
    client_mac VARCHAR(17),                               -- 客户端MAC地址
    client_hostname VARCHAR(255),                         -- 客户端主机名
    client_os VARCHAR(100),                               -- 客户端操作系统
    client_version VARCHAR(50),                           -- 客户端版本
    user_agent TEXT,                                      -- 用户代理
    
    -- 硬件信息
    hardware_fingerprint TEXT,                           -- 硬件指纹
    cpu_info VARCHAR(500),                               -- CPU信息
    memory_info VARCHAR(200),                            -- 内存信息
    disk_info VARCHAR(500),                              -- 磁盘信息
    network_info VARCHAR(500),                           -- 网络信息
    
    -- 应用信息
    application_name VARCHAR(200),                        -- 应用名称
    application_version VARCHAR(50),                      -- 应用版本
    module_name VARCHAR(100),                            -- 模块名称
    feature_name VARCHAR(100),                           -- 功能名称
    
    -- 验证详情
    validation_type VARCHAR(50) DEFAULT 'normal',         -- 验证类型：normal/startup/periodic/feature
    session_id VARCHAR(100),                             -- 会话ID
    request_id VARCHAR(100),                             -- 请求ID
    validation_duration INTEGER,                         -- 验证耗时（毫秒）
    
    -- 地理位置（可选）
    country VARCHAR(50),                                 -- 国家
    region VARCHAR(100),                                 -- 地区
    city VARCHAR(100),                                   -- 城市
    timezone VARCHAR(50),                                -- 时区
    
    -- 扩展信息
    additional_info JSONB DEFAULT '{}',                  -- 额外信息
    risk_score INTEGER DEFAULT 0,                       -- 风险评分（0-100）
    is_suspicious BOOLEAN DEFAULT FALSE,                 -- 是否可疑
    
    -- 分区字段（按月分区）
    partition_date DATE DEFAULT CURRENT_DATE
);

-- 授权验证日志表索引
CREATE INDEX idx_license_validations_license ON license_validations(license_id);
CREATE INDEX idx_license_validations_customer ON license_validations(customer_id);
CREATE INDEX idx_license_validations_time ON license_validations(validation_time);
CREATE INDEX idx_license_validations_result ON license_validations(validation_result);
CREATE INDEX idx_license_validations_ip ON license_validations(client_ip);
CREATE INDEX idx_license_validations_mac ON license_validations(client_mac);
CREATE INDEX idx_license_validations_session ON license_validations(session_id);
CREATE INDEX idx_license_validations_partition ON license_validations(partition_date);
CREATE INDEX idx_license_validations_additional ON license_validations USING GIN(additional_info);

-- 授权验证日志表约束
ALTER TABLE license_validations ADD CONSTRAINT chk_license_validations_result 
    CHECK (validation_result IN ('success', 'failure', 'warning', 'error'));
ALTER TABLE license_validations ADD CONSTRAINT chk_license_validations_type 
    CHECK (validation_type IN ('normal', 'startup', 'periodic', 'feature', 'heartbeat'));
ALTER TABLE license_validations ADD CONSTRAINT chk_license_validations_risk_score 
    CHECK (risk_score >= 0 AND risk_score <= 100);

-- =============================================
-- 5. 授权历史表（license_history）
-- =============================================
CREATE TABLE license_history (
    history_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    license_id UUID NOT NULL REFERENCES licenses(license_id) ON DELETE CASCADE,
    operation_type VARCHAR(50) NOT NULL,                  -- 操作类型：create/update/activate/deactivate/extend/revoke
    operation_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    operator_id UUID,                                     -- 操作人ID
    operator_name VARCHAR(100),                           -- 操作人姓名
    old_values JSONB,                                     -- 变更前的值
    new_values JSONB,                                     -- 变更后的值
    change_reason TEXT,                                   -- 变更原因
    change_description TEXT,                              -- 变更描述
    ip_address INET,                                      -- 操作IP
    user_agent TEXT                                       -- 用户代理
);

-- 授权历史表索引
CREATE INDEX idx_license_history_license ON license_history(license_id);
CREATE INDEX idx_license_history_operation ON license_history(operation_type);
CREATE INDEX idx_license_history_time ON license_history(operation_time);
CREATE INDEX idx_license_history_operator ON license_history(operator_id);

-- =============================================
-- 6. 系统配置表（system_configs）
-- =============================================
CREATE TABLE system_configs (
    config_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    config_key VARCHAR(100) UNIQUE NOT NULL,              -- 配置键
    config_value TEXT,                                    -- 配置值
    config_type VARCHAR(20) DEFAULT 'string',            -- 配置类型：string/number/boolean/json
    config_group VARCHAR(50) DEFAULT 'general',          -- 配置分组
    description TEXT,                                     -- 配置描述
    is_encrypted BOOLEAN DEFAULT FALSE,                  -- 是否加密存储
    is_system BOOLEAN DEFAULT FALSE,                     -- 是否系统配置
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

-- 系统配置表索引
CREATE INDEX idx_system_configs_key ON system_configs(config_key);
CREATE INDEX idx_system_configs_group ON system_configs(config_group);
CREATE INDEX idx_system_configs_type ON system_configs(config_type);

-- =============================================
-- 7. 创建视图
-- =============================================

-- 活跃授权视图
CREATE VIEW v_active_licenses AS
SELECT 
    l.*,
    c.customer_name,
    c.company_name,
    c.contact_email,
    lk.key_name,
    lk.algorithm_type,
    CASE 
        WHEN l.expires_at IS NULL THEN 'permanent'
        WHEN l.expires_at > CURRENT_TIMESTAMP THEN 'valid'
        ELSE 'expired'
    END as license_status_desc,
    CASE 
        WHEN l.expires_at IS NOT NULL THEN 
            EXTRACT(DAYS FROM (l.expires_at - CURRENT_TIMESTAMP))
        ELSE NULL
    END as days_until_expiry
FROM licenses l
JOIN customers c ON l.customer_id = c.customer_id
JOIN license_keys lk ON l.key_id = lk.key_id
WHERE l.status = 'active' AND c.status = 'active';

-- 授权统计视图
CREATE VIEW v_license_statistics AS
SELECT 
    c.customer_id,
    c.customer_name,
    c.company_name,
    COUNT(l.license_id) as total_licenses,
    COUNT(CASE WHEN l.status = 'active' THEN 1 END) as active_licenses,
    COUNT(CASE WHEN l.expires_at < CURRENT_TIMESTAMP THEN 1 END) as expired_licenses,
    COUNT(CASE WHEN l.expires_at BETWEEN CURRENT_TIMESTAMP AND CURRENT_TIMESTAMP + INTERVAL '30 days' THEN 1 END) as expiring_soon,
    SUM(l.max_users) as total_authorized_users,
    SUM(l.used_users) as total_used_users
FROM customers c
LEFT JOIN licenses l ON c.customer_id = l.customer_id
GROUP BY c.customer_id, c.customer_name, c.company_name;

-- =============================================
-- 8. 创建触发器函数
-- =============================================

-- 更新时间戳触发器函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为相关表创建更新时间戳触发器
CREATE TRIGGER trigger_customers_updated_at 
    BEFORE UPDATE ON customers 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_licenses_updated_at 
    BEFORE UPDATE ON licenses 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_system_configs_updated_at 
    BEFORE UPDATE ON system_configs 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 授权历史记录触发器函数
CREATE OR REPLACE FUNCTION log_license_changes()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        INSERT INTO license_history (
            license_id, 
            operation_type, 
            old_values, 
            new_values,
            change_description
        ) VALUES (
            NEW.license_id,
            'update',
            row_to_json(OLD),
            row_to_json(NEW),
            'License updated'
        );
        RETURN NEW;
    ELSIF TG_OP = 'INSERT' THEN
        INSERT INTO license_history (
            license_id, 
            operation_type, 
            new_values,
            change_description
        ) VALUES (
            NEW.license_id,
            'create',
            row_to_json(NEW),
            'License created'
        );
        RETURN NEW;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- 创建授权变更日志触发器
CREATE TRIGGER trigger_license_history
    AFTER INSERT OR UPDATE ON licenses
    FOR EACH ROW EXECUTE FUNCTION log_license_changes();

-- =============================================
-- 9. 创建分区表（按月分区验证日志）
-- =============================================

-- 创建分区函数
CREATE OR REPLACE FUNCTION create_monthly_partition(table_name TEXT, start_date DATE)
RETURNS VOID AS $$
DECLARE
    partition_name TEXT;
    end_date DATE;
BEGIN
    partition_name := table_name || '_' || to_char(start_date, 'YYYY_MM');
    end_date := start_date + INTERVAL '1 month';
    
    EXECUTE format('CREATE TABLE IF NOT EXISTS %I PARTITION OF %I 
                    FOR VALUES FROM (%L) TO (%L)',
                   partition_name, table_name, start_date, end_date);
                   
    EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I (validation_time)',
                   partition_name || '_time_idx', partition_name);
END;
$$ LANGUAGE plpgsql;

-- 将验证日志表转换为分区表
ALTER TABLE license_validations 
PARTITION BY RANGE (partition_date);

-- 创建当前月份和未来几个月的分区
SELECT create_monthly_partition('license_validations', date_trunc('month', CURRENT_DATE)::DATE);
SELECT create_monthly_partition('license_validations', (date_trunc('month', CURRENT_DATE) + INTERVAL '1 month')::DATE);
SELECT create_monthly_partition('license_validations', (date_trunc('month', CURRENT_DATE) + INTERVAL '2 month')::DATE);

-- =============================================
-- 10. 初始化数据
-- =============================================

-- 插入默认系统配置
INSERT INTO system_configs (config_key, config_value, config_type, config_group, description) VALUES
('license.validation.timeout', '30', 'number', 'validation', '授权验证超时时间（秒）'),
('license.max.concurrent.validations', '1000', 'number', 'validation', '最大并发验证数'),
('license.warning.days', '30', 'number', 'notification', '授权到期提醒天数'),
('license.auto.cleanup.enabled', 'true', 'boolean', 'maintenance', '是否启用自动清理过期数据'),
('license.log.retention.days', '365', 'number', 'maintenance', '日志保留天数'),
('security.encryption.algorithm', 'AES-256-GCM', 'string', 'security', '数据加密算法'),
('security.key.rotation.days', '90', 'number', 'security', '密钥轮换周期（天）'),
('api.rate.limit.per.minute', '100', 'number', 'api', 'API请求频率限制（每分钟）'),
('notification.email.enabled', 'true', 'boolean', 'notification', '是否启用邮件通知'),
('audit.detailed.logging', 'true', 'boolean', 'audit', '是否启用详细审计日志');

-- 创建默认RSA密钥对（示例）
INSERT INTO license_keys (key_name, key_alias, algorithm_type, public_key, private_key, fingerprint, created_by) VALUES
('default-rsa-2048', 'default', 'RSA2048', 
 '-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...(示例公钥)\n-----END PUBLIC KEY-----',
 '-----BEGIN ENCRYPTED PRIVATE KEY-----\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC...(示例私钥)\n-----END ENCRYPTED PRIVATE KEY-----',
 'sha256:1234567890abcdef1234567890abcdef12345678',
 uuid_generate_v4());

-- =============================================
-- 11. 权限和安全设置
-- =============================================

-- 创建只读用户角色
CREATE ROLE license_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO license_readonly;

-- 创建应用用户角色  
CREATE ROLE license_app;
GRANT SELECT, INSERT, UPDATE ON customers, licenses, license_validations, license_history TO license_app;
GRANT SELECT ON license_keys, system_configs TO license_app;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO license_app;

-- 创建管理员角色
CREATE ROLE license_admin;
GRANT ALL ON ALL TABLES IN SCHEMA public TO license_admin;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO license_admin;

-- 启用行级安全（RLS）示例
-- ALTER TABLE customers ENABLE ROW LEVEL SECURITY;
-- CREATE POLICY customer_isolation ON customers FOR ALL TO license_app USING (customer_id = current_setting('app.current_customer_id')::UUID);

COMMENT ON DATABASE CURRENT_DATABASE() IS '企业客户授权管理系统数据库';