# 企业客户授权管理系统数据库设计文档

## 1. 数据库概述

本数据库设计用于企业级授权管理系统，支持多客户、多产品的授权管理，包含客户管理、授权管理、密钥管理、验证日志等核心功能。

### 1.1 技术选型
- **数据库**: PostgreSQL 14+
- **主要特性**: UUID主键、JSONB存储、分区表、触发器、视图、行级安全

### 1.2 设计原则
- 高性能：合理的索引设计和分区策略
- 高可用：支持读写分离和数据备份
- 高安全：数据加密、权限控制、审计日志
- 高扩展：JSONB字段支持灵活扩展

## 2. 核心表结构

### 2.1 客户表（customers）
存储企业客户的基本信息和联系方式。

**主要字段**:
- `customer_id`: UUID主键
- `customer_code`: 客户编码（唯一）
- `customer_name`: 客户名称
- `company_name`: 公司名称
- `contact_person`: 联系人
- `contact_email`: 联系邮箱
- `status`: 客户状态（active/inactive/suspended/blacklist）

**业务特点**:
- 支持企业客户分类管理
- 包含完整的联系信息和企业资质
- 状态控制支持客户生命周期管理

### 2.2 密钥表（license_keys）
管理授权签名所需的密钥对。

**主要字段**:
- `key_id`: UUID主键
- `key_name`: 密钥名称
- `algorithm_type`: 算法类型（RSA2048/RSA4096/ECC256/ECC384）
- `public_key`: 公钥（PEM格式）
- `private_key`: 私钥（加密存储）
- `fingerprint`: 密钥指纹（SHA256）

**安全特性**:
- 私钥加密存储
- 支持多种加密算法
- 密钥轮换和过期管理
- 使用次数统计

### 2.3 授权表（licenses）
核心业务表，存储所有授权信息。

**主要字段**:
- `license_id`: UUID主键
- `customer_id`: 客户ID（外键）
- `key_id`: 密钥ID（外键）
- `license_code`: 授权码（唯一）
- `product_name`: 产品名称
- `license_type`: 授权类型（trial/commercial/enterprise/oem）
- `max_users`: 最大用户数
- `feature_permissions`: 功能权限（JSONB）
- `expires_at`: 到期时间

**灵活性设计**:
- JSONB字段存储复杂权限配置
- 支持多种授权模式（永久/订阅/并发）
- 硬件绑定和IP限制
- 数字签名验证

### 2.4 授权验证日志表（license_validations）
记录所有授权验证请求和结果。

**主要字段**:
- `validation_id`: UUID主键
- `license_id`: 授权ID（外键）
- `validation_time`: 验证时间
- `validation_result`: 验证结果
- `client_ip`: 客户端IP
- `hardware_fingerprint`: 硬件指纹
- `additional_info`: 扩展信息（JSONB）

**性能优化**:
- 按月分区存储
- 针对查询场景建立复合索引
- 支持风险评估和异常检测

## 3. 表关系设计

```
customers (1) ----< (N) licenses (N) >---- (1) license_keys
    |                      |
    |                      |
    |                      v
    |               license_history
    |                      |
    v                      v
license_validations <---- licenses
```

### 3.1 主要关系
1. **客户-授权**：一对多关系，一个客户可以有多个授权
2. **密钥-授权**：一对多关系，一个密钥可以签发多个授权
3. **授权-验证日志**：一对多关系，一个授权对应多个验证记录
4. **授权-历史记录**：一对多关系，记录授权的所有变更

### 3.2 引用完整性
- 使用外键约束保证数据一致性
- 级联删除策略：客户删除时级联删除相关授权和日志
- 限制删除策略：密钥被引用时不允许删除

## 4. 索引设计

### 4.1 主要索引

#### 客户表索引
```sql
CREATE INDEX idx_customers_code ON customers(customer_code);
CREATE INDEX idx_customers_name ON customers(customer_name);
CREATE INDEX idx_customers_email ON customers(contact_email);
CREATE INDEX idx_customers_status ON customers(status);
```

#### 授权表索引
```sql
CREATE INDEX idx_licenses_customer ON licenses(customer_id);
CREATE INDEX idx_licenses_code ON licenses(license_code);
CREATE INDEX idx_licenses_expires_at ON licenses(expires_at);
CREATE INDEX idx_licenses_feature_permissions ON licenses USING GIN(feature_permissions);
```

#### 验证日志表索引
```sql
CREATE INDEX idx_license_validations_license ON license_validations(license_id);
CREATE INDEX idx_license_validations_time ON license_validations(validation_time);
CREATE INDEX idx_license_validations_ip ON license_validations(client_ip);
```

### 4.2 复合索引
针对常见查询模式创建复合索引：
- `(customer_id, status, expires_at)`: 查询客户的有效授权
- `(validation_time, validation_result)`: 按时间范围统计验证结果
- `(license_id, validation_time)`: 查询特定授权的验证历史

## 5. 约束条件

### 5.1 检查约束
```sql
-- 状态值约束
ALTER TABLE customers ADD CONSTRAINT chk_customers_status 
    CHECK (status IN ('active', 'inactive', 'suspended', 'blacklist'));

-- 邮箱格式约束
ALTER TABLE customers ADD CONSTRAINT chk_customers_email 
    CHECK (contact_email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

-- 用户数约束
ALTER TABLE licenses ADD CONSTRAINT chk_licenses_used_users 
    CHECK (used_users >= 0 AND used_users <= max_users);

-- 时间逻辑约束
ALTER TABLE licenses ADD CONSTRAINT chk_licenses_dates 
    CHECK (effective_at <= expires_at);
```

### 5.2 唯一约束
- 客户编码全局唯一
- 授权码全局唯一
- 密钥名称和别名唯一
- 密钥指纹唯一

## 6. 分区策略

### 6.1 验证日志分区
采用按月范围分区策略：
```sql
ALTER TABLE license_validations PARTITION BY RANGE (partition_date);
```

**分区优势**:
- 提高大表查询性能
- 简化历史数据清理
- 支持并行查询和维护

### 6.2 分区管理
- 自动创建未来月份分区
- 定期清理过期分区数据
- 支持分区级别的并行操作

## 7. 视图设计

### 7.1 活跃授权视图（v_active_licenses）
```sql
CREATE VIEW v_active_licenses AS
SELECT 
    l.*,
    c.customer_name,
    c.company_name,
    CASE 
        WHEN l.expires_at > CURRENT_TIMESTAMP THEN 'valid'
        ELSE 'expired'
    END as license_status_desc
FROM licenses l
JOIN customers c ON l.customer_id = c.customer_id
WHERE l.status = 'active';
```

### 7.2 授权统计视图（v_license_statistics）
提供客户维度的授权统计信息，支持管理决策。

## 8. 触发器设计

### 8.1 自动更新时间戳
```sql
CREATE TRIGGER trigger_customers_updated_at 
    BEFORE UPDATE ON customers 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

### 8.2 授权变更审计
```sql
CREATE TRIGGER trigger_license_history
    AFTER INSERT OR UPDATE ON licenses
    FOR EACH ROW EXECUTE FUNCTION log_license_changes();
```

## 9. 安全设计

### 9.1 数据加密
- 敏感字段加密存储（私钥、客户信息）
- 传输过程SSL/TLS加密
- 数据库连接加密

### 9.2 访问控制
```sql
-- 只读角色
CREATE ROLE license_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO license_readonly;

-- 应用角色
CREATE ROLE license_app;
GRANT SELECT, INSERT, UPDATE ON licenses, license_validations TO license_app;
```

### 9.3 审计日志
- 完整的操作审计记录
- 敏感操作强制审计
- 支持合规性要求

## 10. 性能优化建议

### 10.1 查询优化
1. **分页查询**: 使用 LIMIT/OFFSET 或游标分页
2. **索引覆盖**: 设计覆盖索引减少回表查询
3. **查询缓存**: 缓存热点数据和统计信息

### 10.2 存储优化
1. **数据归档**: 定期归档历史数据
2. **压缩存储**: 启用表级压缩
3. **分区剪枝**: 利用分区提高查询效率

### 10.3 维护策略
```sql
-- 定期清理过期验证日志
DELETE FROM license_validations 
WHERE validation_time < CURRENT_DATE - INTERVAL '1 year';

-- 更新表统计信息
ANALYZE customers, licenses, license_validations;

-- 重建索引
REINDEX INDEX CONCURRENTLY idx_licenses_expires_at;
```

## 11. 扩展性考虑

### 11.1 水平扩展
- 支持读写分离架构
- 验证日志表可按客户或时间分片
- 支持多租户架构

### 11.2 功能扩展
- JSONB字段支持灵活的权限配置
- 元数据字段支持业务扩展
- 插件化的验证规则

### 11.3 集成扩展
- 标准化的API接口
- 消息队列支持异步处理
- 监控和告警集成

## 12. 数据字典

### 12.1 状态枚举值

#### 客户状态（customers.status）
- `active`: 活跃客户
- `inactive`: 非活跃客户
- `suspended`: 暂停服务
- `blacklist`: 黑名单客户

#### 授权状态（licenses.status）
- `active`: 有效授权
- `inactive`: 无效授权
- `suspended`: 暂停授权
- `expired`: 已过期
- `revoked`: 已撤销

#### 验证结果（license_validations.validation_result）
- `success`: 验证成功
- `failure`: 验证失败
- `warning`: 验证警告
- `error`: 验证错误

### 12.2 权限配置示例

#### 功能权限（feature_permissions）
```json
{
  "user_management": true,
  "report_export": false,
  "advanced_analytics": true,
  "api_access": {
    "read": true,
    "write": false,
    "admin": false
  }
}
```

#### 模块权限（module_permissions）
```json
{
  "finance": ["view", "edit"],
  "hr": ["view"],
  "inventory": ["view", "edit", "delete"],
  "settings": ["admin"]
}
```

这个数据库设计提供了完整的企业级授权管理能力，支持高并发、高可用的生产环境需求。