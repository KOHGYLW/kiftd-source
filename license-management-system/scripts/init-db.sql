-- 企业授权管理系统数据库初始化脚本
-- 创建时间: 2024-07-30
-- 描述: 初始化数据库结构和基础数据

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS license_management;

-- 使用数据库
\c license_management;

-- 创建扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 创建系统用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    full_name VARCHAR(100),
    phone VARCHAR(20),
    avatar_url VARCHAR(255),
    enabled BOOLEAN DEFAULT TRUE,
    account_non_expired BOOLEAN DEFAULT TRUE,
    account_non_locked BOOLEAN DEFAULT TRUE,
    credentials_non_expired BOOLEAN DEFAULT TRUE,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建角色表
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建用户角色关联表
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- 创建客户表
CREATE TABLE IF NOT EXISTS customers (
    id BIGSERIAL PRIMARY KEY,
    customer_code VARCHAR(50) UNIQUE NOT NULL,
    customer_name VARCHAR(200) NOT NULL,
    contact_person VARCHAR(100),
    contact_phone VARCHAR(20),
    contact_email VARCHAR(100),
    address TEXT,
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id)
);

-- 创建许可证表
CREATE TABLE IF NOT EXISTS licenses (
    id BIGSERIAL PRIMARY KEY,
    license_code VARCHAR(100) UNIQUE NOT NULL,
    customer_id BIGINT REFERENCES customers(id) ON DELETE CASCADE,
    license_type VARCHAR(20) NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    product_version VARCHAR(50) NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    max_users INTEGER NOT NULL DEFAULT 1,
    current_users INTEGER DEFAULT 0,
    hardware_fingerprint TEXT,
    issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id)
);

-- 创建许可证密钥表
CREATE TABLE IF NOT EXISTS license_keys (
    id BIGSERIAL PRIMARY KEY,
    license_id BIGINT REFERENCES licenses(id) ON DELETE CASCADE,
    key_data TEXT NOT NULL,
    signature TEXT NOT NULL,
    public_key TEXT,
    key_version INTEGER DEFAULT 1,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP
);

-- 创建许可证历史表
CREATE TABLE IF NOT EXISTS license_history (
    id BIGSERIAL PRIMARY KEY,
    license_id BIGINT REFERENCES licenses(id) ON DELETE CASCADE,
    operation_type VARCHAR(20) NOT NULL,
    old_value JSONB,
    new_value JSONB,
    operation_reason VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id)
);

-- 创建许可证验证表
CREATE TABLE IF NOT EXISTS license_validations (
    id BIGSERIAL PRIMARY KEY,
    license_id BIGINT REFERENCES licenses(id) ON DELETE CASCADE,
    validation_result VARCHAR(20) NOT NULL,
    client_ip VARCHAR(45),
    hardware_fingerprint TEXT,
    user_agent TEXT,
    validation_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    error_message TEXT,
    request_data JSONB,
    response_data JSONB
);

-- 创建许可证验证日志表
CREATE TABLE IF NOT EXISTS license_validation_logs (
    id BIGSERIAL PRIMARY KEY,
    license_code VARCHAR(100),
    validation_result VARCHAR(20) NOT NULL,
    client_ip VARCHAR(45),
    hardware_fingerprint TEXT,
    product_name VARCHAR(200),
    product_version VARCHAR(50),
    validation_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processing_time INTEGER, -- 处理时间（毫秒）
    error_message TEXT,
    request_data JSONB
);

-- 创建系统配置表
CREATE TABLE IF NOT EXISTS system_configs (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) UNIQUE NOT NULL,
    config_value TEXT NOT NULL,
    config_type VARCHAR(20) DEFAULT 'STRING',
    description TEXT,
    is_public BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT REFERENCES users(id)
);

-- 创建密钥管理表
CREATE TABLE IF NOT EXISTS key_management (
    id BIGSERIAL PRIMARY KEY,
    key_id VARCHAR(100) UNIQUE NOT NULL,
    key_type VARCHAR(20) NOT NULL,
    public_key TEXT NOT NULL,
    private_key TEXT,
    key_algorithm VARCHAR(50) NOT NULL,
    key_length INTEGER NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    rotated_at TIMESTAMP,
    created_by BIGINT REFERENCES users(id)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_customers_code ON customers(customer_code);
CREATE INDEX IF NOT EXISTS idx_customers_status ON customers(status);
CREATE INDEX IF NOT EXISTS idx_customers_created_at ON customers(created_at);

CREATE INDEX IF NOT EXISTS idx_licenses_code ON licenses(license_code);
CREATE INDEX IF NOT EXISTS idx_licenses_customer_id ON licenses(customer_id);
CREATE INDEX IF NOT EXISTS idx_licenses_status ON licenses(status);
CREATE INDEX IF NOT EXISTS idx_licenses_type ON licenses(license_type);
CREATE INDEX IF NOT EXISTS idx_licenses_expires_at ON licenses(expires_at);
CREATE INDEX IF NOT EXISTS idx_licenses_product ON licenses(product_name, product_version);

CREATE INDEX IF NOT EXISTS idx_license_keys_license_id ON license_keys(license_id);
CREATE INDEX IF NOT EXISTS idx_license_keys_status ON license_keys(status);

CREATE INDEX IF NOT EXISTS idx_license_validations_license_id ON license_validations(license_id);
CREATE INDEX IF NOT EXISTS idx_license_validations_time ON license_validations(validation_time);
CREATE INDEX IF NOT EXISTS idx_license_validations_result ON license_validations(validation_result);

CREATE INDEX IF NOT EXISTS idx_license_validation_logs_code ON license_validation_logs(license_code);
CREATE INDEX IF NOT EXISTS idx_license_validation_logs_time ON license_validation_logs(validation_time);
CREATE INDEX IF NOT EXISTS idx_license_validation_logs_ip ON license_validation_logs(client_ip);

CREATE INDEX IF NOT EXISTS idx_system_configs_key ON system_configs(config_key);
CREATE INDEX IF NOT EXISTS idx_key_management_key_id ON key_management(key_id);
CREATE INDEX IF NOT EXISTS idx_key_management_status ON key_management(status);

-- 创建触发器函数：自动更新 updated_at 字段
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为相关表创建触发器
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_customers_updated_at BEFORE UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_licenses_updated_at BEFORE UPDATE ON licenses
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_system_configs_updated_at BEFORE UPDATE ON system_configs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 插入默认角色
INSERT INTO roles (name, description) VALUES 
    ('ADMIN', '系统管理员'),
    ('MANAGER', '业务管理员'),
    ('USER', '普通用户')
ON CONFLICT (name) DO NOTHING;

-- 插入默认管理员用户（密码: admin123）
INSERT INTO users (username, password, email, full_name, enabled) VALUES 
    ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9P6gEeKgBQuKdWy', 'admin@enterprise.com', '系统管理员', true)
ON CONFLICT (username) DO NOTHING;

-- 为管理员分配角色
INSERT INTO user_roles (user_id, role_id) 
SELECT u.id, r.id FROM users u, roles r 
WHERE u.username = 'admin' AND r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

-- 插入系统配置
INSERT INTO system_configs (config_key, config_value, config_type, description, is_public) VALUES 
    ('system.name', '企业授权管理系统', 'STRING', '系统名称', true),
    ('system.version', '1.0.0', 'STRING', '系统版本', true),
    ('system.company', '企业科技有限公司', 'STRING', '公司名称', true),
    ('license.default_expiry_days', '365', 'INTEGER', '默认许可证有效期（天）', false),
    ('license.max_validation_requests_per_minute', '100', 'INTEGER', '每分钟最大验证请求数', false),
    ('security.password_min_length', '8', 'INTEGER', '密码最小长度', false),
    ('security.session_timeout_minutes', '30', 'INTEGER', '会话超时时间（分钟）', false),
    ('notification.email_enabled', 'false', 'BOOLEAN', '邮件通知是否启用', false),
    ('backup.auto_backup_enabled', 'true', 'BOOLEAN', '是否启用自动备份', false),
    ('backup.retention_days', '30', 'INTEGER', '备份保留天数', false)
ON CONFLICT (config_key) DO NOTHING;

-- 创建视图：客户许可证统计
CREATE OR REPLACE VIEW customer_license_stats AS
SELECT 
    c.id as customer_id,
    c.customer_code,
    c.customer_name,
    COUNT(l.id) as total_licenses,
    COUNT(CASE WHEN l.status = 'ACTIVE' THEN 1 END) as active_licenses,
    COUNT(CASE WHEN l.status = 'EXPIRED' THEN 1 END) as expired_licenses,
    COUNT(CASE WHEN l.status = 'INACTIVE' THEN 1 END) as inactive_licenses,
    COUNT(CASE WHEN l.expires_at > CURRENT_TIMESTAMP AND l.expires_at < CURRENT_TIMESTAMP + INTERVAL '30 days' THEN 1 END) as expiring_soon_licenses
FROM customers c
LEFT JOIN licenses l ON c.id = l.customer_id
GROUP BY c.id, c.customer_code, c.customer_name;

-- 创建视图：许可证验证统计
CREATE OR REPLACE VIEW license_validation_stats AS
SELECT 
    DATE(validation_time) as validation_date,
    COUNT(*) as total_validations,
    COUNT(CASE WHEN validation_result = 'SUCCESS' THEN 1 END) as successful_validations,
    COUNT(CASE WHEN validation_result = 'FAILED' THEN 1 END) as failed_validations,
    AVG(processing_time) as avg_processing_time,
    COUNT(DISTINCT client_ip) as unique_clients
FROM license_validation_logs
WHERE validation_time >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY DATE(validation_time)
ORDER BY validation_date DESC;

-- 创建存储过程：清理过期数据
CREATE OR REPLACE FUNCTION cleanup_expired_data()
RETURNS void AS $$
BEGIN
    -- 清理90天前的验证日志
    DELETE FROM license_validation_logs 
    WHERE validation_time < CURRENT_TIMESTAMP - INTERVAL '90 days';
    
    -- 清理过期的许可证密钥
    UPDATE license_keys 
    SET status = 'EXPIRED' 
    WHERE expires_at < CURRENT_TIMESTAMP AND status = 'ACTIVE';
    
    -- 清理180天前的许可证历史记录
    DELETE FROM license_history 
    WHERE created_at < CURRENT_TIMESTAMP - INTERVAL '180 days';
    
    RAISE NOTICE '过期数据清理完成';
END;
$$ LANGUAGE plpgsql;

-- 授权
GRANT ALL PRIVILEGES ON DATABASE license_management TO license_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO license_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO license_user;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO license_user;

-- 输出初始化完成信息
DO $$
BEGIN
    RAISE NOTICE '===========================================';
    RAISE NOTICE '企业授权管理系统数据库初始化完成！';
    RAISE NOTICE '===========================================';
    RAISE NOTICE '默认管理员账号: admin';
    RAISE NOTICE '默认管理员密码: admin123';
    RAISE NOTICE '请在首次登录后及时修改默认密码！';
    RAISE NOTICE '===========================================';
END $$;