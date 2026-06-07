-- =============================================
-- 企业客户授权管理系统 - 示例数据插入脚本
-- =============================================

-- 1. 插入示例客户数据
INSERT INTO customers (
    customer_code, customer_name, company_name, company_type, 
    contact_person, contact_phone, contact_email, contact_address,
    business_license, industry, company_scale, status, remark
) VALUES 
-- 大型企业客户
('CUST001', '阿里巴巴集团', '阿里巴巴集团控股有限公司', 'enterprise',
 '张三', '13800138001', 'zhangsan@alibaba.com', '浙江省杭州市余杭区文一西路969号',
 '91330000MA27XF4H6X', '互联网/电商', 'enterprise', 'active', '重要企业客户'),

-- 中型企业客户  
('CUST002', '小米科技', '小米科技有限责任公司', 'enterprise',
 '李四', '13800138002', 'lisi@mi.com', '北京市海淀区清河中街68号华润五彩城',
 '91110000802100433B', '消费电子', 'large', 'active', '手机制造商'),

-- 政府客户
('CUST003', '上海市政府', '上海市人民政府', 'government',
 '王五', '13800138003', 'wangwu@sh.gov.cn', '上海市黄浦区人民大道200号',
 'GOV_SH_001', '政府机构', 'enterprise', 'active', '政府数字化项目'),

-- 中小企业客户
('CUST004', '创新科技公司', '北京创新科技有限公司', 'enterprise',
 '赵六', '13800138004', 'zhaoliu@innovation.com', '北京市朝阳区望京SOHO T1',
 '91110000MA01234567', '软件开发', 'medium', 'active', '软件开发公司'),

-- 试用客户
('CUST005', '测试企业', '测试企业有限公司', 'enterprise',
 '孙七', '13800138005', 'sunqi@test.com', '广州市天河区珠江新城',
 '91440000MA9876543X', '制造业', 'small', 'active', '试用客户');

-- 2. 插入示例密钥数据（实际使用时需要真实的密钥对）
INSERT INTO license_keys (
    key_name, key_alias, algorithm_type, key_strength,
    public_key, private_key, fingerprint, key_purpose, status
) VALUES 
('production-rsa-2048', 'prod-2048', 'RSA2048', 2048,
 '-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1234567890ABCDEF...
-----END PUBLIC KEY-----',
 '-----BEGIN ENCRYPTED PRIVATE KEY-----
MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDXNZQ...
-----END ENCRYPTED PRIVATE KEY-----',
 'sha256:prod2048_fingerprint_hash_value_here', 'license_signing', 'active'),

('backup-rsa-4096', 'backup-4096', 'RSA4096', 4096,
 '-----BEGIN PUBLIC KEY-----
MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEA987654321FEDCBA...
-----END PUBLIC KEY-----',
 '-----BEGIN ENCRYPTED PRIVATE KEY-----
MIIJQgIBADANBgkqhkiG9w0BAQEFAASCCSwwggkoAgEAAoICAQD3vZQq...
-----END ENCRYPTED PRIVATE KEY-----',
 'sha256:backup4096_fingerprint_hash_value_here', 'backup', 'active');

-- 3. 插入示例授权数据
INSERT INTO licenses (
    customer_id, key_id, license_code, product_name, product_version,
    license_type, license_model, issued_at, effective_at, expires_at,
    max_users, concurrent_users, used_users,
    feature_permissions, module_permissions, api_permissions,
    allowed_ips, status, auto_renewal, warning_days, remark
) VALUES 
-- 阿里巴巴的企业级永久授权
((SELECT customer_id FROM customers WHERE customer_code = 'CUST001'),
 (SELECT key_id FROM license_keys WHERE key_alias = 'prod-2048'),
 'LIC-ALIBABA-ENT-2025-001', '企业ERP系统', 'v3.2.1',
 'enterprise', 'perpetual', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL,
 1000, 500, 250,
 '{"user_management": true, "advanced_analytics": true, "api_access": {"read": true, "write": true, "admin": true}, "custom_reports": true, "data_export": true}',
 '{"finance": ["view", "edit", "admin"], "hr": ["view", "edit"], "inventory": ["view", "edit", "delete"], "settings": ["admin"]}',
 '{"user_api": true, "report_api": true, "admin_api": true, "webhook": true}',
 '["192.168.1.0/24", "10.0.0.0/16"]', 'active', false, 30, '企业级永久授权'),

-- 小米的标准商业授权（1年期）
((SELECT customer_id FROM customers WHERE customer_code = 'CUST002'),
 (SELECT key_id FROM license_keys WHERE key_alias = 'prod-2048'),
 'LIC-XIAOMI-COM-2025-002', '生产管理系统', 'v2.8.5',
 'commercial', 'subscription', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '1 year',
 500, 200, 150,
 '{"user_management": true, "basic_reports": true, "api_access": {"read": true, "write": true, "admin": false}, "data_export": false}',
 '{"production": ["view", "edit"], "quality": ["view", "edit"], "inventory": ["view"], "settings": ["view"]}',
 '{"user_api": true, "report_api": false, "admin_api": false}',
 '["10.1.0.0/16"]', 'active', true, 30, '标准商业授权，自动续费'),

-- 政府客户的定制授权（3年期）
((SELECT customer_id FROM customers WHERE customer_code = 'CUST003'),
 (SELECT key_id FROM license_keys WHERE key_alias = 'prod-2048'),
 'LIC-SHANGHAI-GOV-2025-003', '政务信息系统', 'v4.1.0',
 'enterprise', 'subscription', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '3 years',
 2000, 1000, 0,
 '{"user_management": true, "government_reports": true, "security_audit": true, "api_access": {"read": true, "write": true, "admin": true}}',
 '{"citizen_service": ["view", "edit", "admin"], "internal_affairs": ["view", "edit"], "finance": ["view"], "audit": ["view", "edit"]}',
 '{"public_api": true, "internal_api": true, "admin_api": true}',
 '["172.16.0.0/12"]', 'active', false, 90, '政府定制版，3年期'),

-- 中小企业试用授权（30天）
((SELECT customer_id FROM customers WHERE customer_code = 'CUST004'),
 (SELECT key_id FROM license_keys WHERE key_alias = 'prod-2048'),
 'LIC-INNOVATION-TRL-2025-004', '中小企业管理系统', 'v2.5.3',
 'trial', 'subscription', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '30 days',
 50, 20, 5,
 '{"user_management": true, "basic_reports": true, "api_access": {"read": true, "write": false, "admin": false}}',
 '{"basic": ["view", "edit"], "reports": ["view"]}',
 '{"user_api": true}',
 '["192.168.0.0/24"]', 'active', false, 7, '30天试用授权'),

-- 即将到期的授权（用于测试告警）
((SELECT customer_id FROM customers WHERE customer_code = 'CUST005'),
 (SELECT key_id FROM license_keys WHERE key_alias = 'prod-2048'),
 'LIC-TEST-EXP-2025-005', '测试系统', 'v1.0.0',
 'trial', 'subscription', CURRENT_TIMESTAMP - INTERVAL '25 days', CURRENT_TIMESTAMP - INTERVAL '25 days', CURRENT_TIMESTAMP + INTERVAL '5 days',
 10, 5, 2,
 '{"basic_features": true}',
 '{"test": ["view"]}',
 '{}',
 '[]', 'active', false, 7, '即将到期的测试授权');

-- 4. 插入示例验证日志数据
INSERT INTO license_validations (
    license_id, customer_id, validation_time, validation_result, result_code, result_message,
    client_ip, client_mac, client_hostname, client_os, client_version,
    hardware_fingerprint, application_name, application_version, validation_type,
    session_id, validation_duration, additional_info, risk_score
) VALUES 
-- 成功验证记录
((SELECT license_id FROM licenses WHERE license_code = 'LIC-ALIBABA-ENT-2025-001'),
 (SELECT customer_id FROM customers WHERE customer_code = 'CUST001'),
 CURRENT_TIMESTAMP - INTERVAL '1 hour', 'success', 'VALIDATION_SUCCESS', '授权验证成功',
 '192.168.1.100', '00:1B:44:11:3A:B7', 'ali-server-01', 'Ubuntu 20.04 LTS', 'v3.2.1',
 'hw_fingerprint_alibaba_001', 'ERP系统', 'v3.2.1', 'startup',
 'sess_ali_001_' || extract(epoch from CURRENT_TIMESTAMP), 150,
 '{"cpu_count": 16, "memory_gb": 32, "location": "hangzhou"}', 10),

-- 警告验证记录（用户数接近上限）
((SELECT license_id FROM licenses WHERE license_code = 'LIC-XIAOMI-COM-2025-002'),
 (SELECT customer_id FROM customers WHERE customer_code = 'CUST002'),
 CURRENT_TIMESTAMP - INTERVAL '30 minutes', 'warning', 'USER_LIMIT_WARNING', '用户数接近授权上限',
 '10.1.1.50', '00:1B:44:11:3A:C8', 'mi-app-server', 'CentOS 8', 'v2.8.5',
 'hw_fingerprint_xiaomi_001', '生产管理系统', 'v2.8.5', 'periodic',
 'sess_mi_001_' || extract(epoch from CURRENT_TIMESTAMP), 200,
 '{"current_users": 190, "max_users": 200, "usage_rate": 0.95}', 35),

-- 失败验证记录（授权过期）
((SELECT license_id FROM licenses WHERE license_code = 'LIC-TEST-EXP-2025-005'),
 (SELECT customer_id FROM customers WHERE customer_code = 'CUST005'),
 CURRENT_TIMESTAMP - INTERVAL '10 minutes', 'failure', 'LICENSE_EXPIRED', '授权已过期',
 '192.168.0.100', '00:1B:44:11:3A:D9', 'test-client-01', 'Windows 10', 'v1.0.0',
 'hw_fingerprint_test_001', '测试系统', 'v1.0.0', 'normal',
 'sess_test_001_' || extract(epoch from CURRENT_TIMESTAMP), 50,
 '{"days_expired": 1, "auto_renewal": false}', 80);

-- 插入更多验证日志数据（用于性能测试）
INSERT INTO license_validations (
    license_id, customer_id, validation_time, validation_result, result_code,
    client_ip, validation_type, validation_duration, risk_score
)
SELECT 
    (SELECT license_id FROM licenses WHERE license_code = 'LIC-ALIBABA-ENT-2025-001'),
    (SELECT customer_id FROM customers WHERE customer_code = 'CUST001'),
    CURRENT_TIMESTAMP - (random() * INTERVAL '30 days'),
    CASE WHEN random() < 0.9 THEN 'success' ELSE 'warning' END,
    CASE WHEN random() < 0.9 THEN 'VALIDATION_SUCCESS' ELSE 'MINOR_WARNING' END,
    ('192.168.1.' || (100 + (random() * 50)::int))::inet,
    'periodic',
    (50 + random() * 200)::int,
    (random() * 30)::int
FROM generate_series(1, 1000);

-- 5. 插入授权历史记录（通过UPDATE触发器自动生成部分记录）
UPDATE licenses 
SET used_users = used_users + 10, 
    updated_by = (SELECT customer_id FROM customers WHERE customer_code = 'CUST001' LIMIT 1)
WHERE license_code = 'LIC-ALIBABA-ENT-2025-001';

UPDATE licenses 
SET status = 'suspended', 
    remark = '临时暂停，等待续费确认',
    updated_by = (SELECT customer_id FROM customers WHERE customer_code = 'CUST005' LIMIT 1)
WHERE license_code = 'LIC-TEST-EXP-2025-005';

-- 6. 验证数据插入结果
SELECT 
    '客户数据' as table_type,
    COUNT(*) as record_count
FROM customers
UNION ALL
SELECT 
    '密钥数据' as table_type,
    COUNT(*) as record_count
FROM license_keys
UNION ALL
SELECT 
    '授权数据' as table_type,
    COUNT(*) as record_count
FROM licenses
UNION ALL
SELECT 
    '验证日志' as table_type,
    COUNT(*) as record_count
FROM license_validations
UNION ALL
SELECT 
    '历史记录' as table_type,
    COUNT(*) as record_count
FROM license_history;

-- 7. 查询示例
-- 查看活跃授权统计
SELECT * FROM v_license_statistics WHERE total_licenses > 0;

-- 查看即将到期的授权
SELECT 
    c.customer_name,
    l.license_code,
    l.product_name,
    l.expires_at,
    EXTRACT(DAYS FROM (l.expires_at - CURRENT_TIMESTAMP)) as days_remaining
FROM licenses l
JOIN customers c ON l.customer_id = c.customer_id
WHERE l.expires_at IS NOT NULL 
  AND l.expires_at BETWEEN CURRENT_TIMESTAMP AND CURRENT_TIMESTAMP + INTERVAL '30 days'
  AND l.status = 'active'
ORDER BY l.expires_at;

-- 查看最近的验证活动
SELECT 
    c.customer_name,
    l.license_code,
    lv.validation_time,
    lv.validation_result,
    lv.client_ip,
    lv.result_message
FROM license_validations lv
JOIN licenses l ON lv.license_id = l.license_id
JOIN customers c ON lv.customer_id = c.customer_id
WHERE lv.validation_time >= CURRENT_TIMESTAMP - INTERVAL '24 hours'
ORDER BY lv.validation_time DESC
LIMIT 20;