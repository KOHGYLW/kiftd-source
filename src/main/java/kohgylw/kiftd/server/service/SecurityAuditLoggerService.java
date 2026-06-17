package kohgylw.kiftd.server.service;

/**
 * 安全审计日志服务接口
 * 提供企业级的安全事件记录和审计功能
 * 
 * @author 企业安全专家
 * @version 2.0
 */
public interface SecurityAuditLoggerService {
    
    /**
     * 记录授权验证事件
     * @param customerId 客户ID
     * @param productId 产品ID
     * @param result 验证结果
     * @param clientIp 客户端IP
     * @param userAgent 用户代理
     */
    void logLicenseValidation(String customerId, String productId, boolean result, String clientIp, String userAgent);
    
    /**
     * 记录密钥操作事件
     * @param operation 操作类型（生成、轮换、删除等）
     * @param keyId 密钥ID
     * @param operatorId 操作者ID
     * @param result 操作结果
     */
    void logKeyOperation(String operation, String keyId, String operatorId, boolean result);
    
    /**
     * 记录安全事件
     * @param eventType 事件类型
     * @param severity 严重程度
     * @param message 事件消息
     * @param details 详细信息
     * @param sourceIp 来源IP
     */
    void logSecurityEvent(SecurityEventType eventType, SecuritySeverity severity, 
                         String message, String details, String sourceIp);
    
    /**
     * 记录硬件绑定验证事件
     * @param customerId 客户ID
     * @param fingerprintHash 硬件指纹哈希
     * @param result 验证结果
     * @param similarityScore 相似度评分
     */
    void logHardwareBindingValidation(String customerId, String fingerprintHash, 
                                    boolean result, int similarityScore);
    
    /**
     * 记录加密解密操作
     * @param operation 操作类型
     * @param algorithm 加密算法
     * @param dataSize 数据大小
     * @param duration 操作耗时
     * @param result 操作结果
     */
    void logCryptographicOperation(String operation, String algorithm, 
                                 long dataSize, long duration, boolean result);
    
    /**
     * 记录访问控制事件
     * @param userId 用户ID
     * @param resource 访问资源
     * @param action 操作动作
     * @param result 访问结果
     * @param reason 失败原因（如果失败）
     */
    void logAccessControl(String userId, String resource, String action, 
                         boolean result, String reason);
    
    /**
     * 记录配置变更事件
     * @param configKey 配置键
     * @param oldValue 旧值
     * @param newValue 新值
     * @param operatorId 操作者ID
     */
    void logConfigurationChange(String configKey, String oldValue, String newValue, String operatorId);
    
    /**
     * 查询安全日志
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param eventType 事件类型（可选）
     * @param severity 严重程度（可选）
     * @param limit 返回条数限制
     * @return SecurityLogQuery 查询结果
     */
    SecurityLogQuery querySecurityLogs(long startTime, long endTime, 
                                     SecurityEventType eventType, SecuritySeverity severity, int limit);
    
    /**
     * 生成安全审计报告
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return String 审计报告
     */
    String generateAuditReport(long startTime, long endTime);
    
    /**
     * 导出安全日志
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param format 导出格式（JSON、CSV、XML）
     * @return String 导出的日志数据
     */
    String exportSecurityLogs(long startTime, long endTime, String format);
    
    /**
     * 清理过期的安全日志
     * @param retentionDays 保留天数
     * @return int 清理的日志条数
     */
    int cleanupExpiredLogs(int retentionDays);
    
    /**
     * 安全事件类型枚举
     */
    enum SecurityEventType {
        /** 授权验证 */
        LICENSE_VALIDATION("授权验证"),
        /** 密钥操作 */
        KEY_OPERATION("密钥操作"),
        /** 硬件绑定 */
        HARDWARE_BINDING("硬件绑定"),
        /** 加密操作 */
        CRYPTOGRAPHIC_OPERATION("加密操作"),
        /** 访问控制 */
        ACCESS_CONTROL("访问控制"),
        /** 配置变更 */
        CONFIGURATION_CHANGE("配置变更"),
        /** 系统启动 */
        SYSTEM_STARTUP("系统启动"),
        /** 系统关闭 */
        SYSTEM_SHUTDOWN("系统关闭"),
        /** 认证失败 */
        AUTHENTICATION_FAILURE("认证失败"),
        /** 授权失败 */
        AUTHORIZATION_FAILURE("授权失败"),
        /** 可疑活动 */
        SUSPICIOUS_ACTIVITY("可疑活动"),
        /** 数据泄露 */
        DATA_BREACH("数据泄露"),
        /** 系统错误 */
        SYSTEM_ERROR("系统错误");
        
        private final String description;
        
        SecurityEventType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 安全严重程度枚举
     */
    enum SecuritySeverity {
        /** 低 */
        LOW(1, "低"),
        /** 中 */
        MEDIUM(2, "中"),
        /** 高 */
        HIGH(3, "高"),
        /** 严重 */
        CRITICAL(4, "严重");
        
        private final int level;
        private final String description;
        
        SecuritySeverity(int level, String description) {
            this.level = level;
            this.description = description;
        }
        
        public int getLevel() {
            return level;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 安全日志查询结果
     */
    interface SecurityLogQuery {
        /**
         * 获取总记录数
         */
        int getTotalCount();
        
        /**
         * 获取日志条目
         */
        java.util.List<SecurityLogEntry> getEntries();
        
        /**
         * 是否有更多数据
         */
        boolean hasMore();
    }
    
    /**
     * 安全日志条目
     */
    interface SecurityLogEntry {
        /**
         * 获取日志ID
         */
        String getId();
        
        /**
         * 获取时间戳
         */
        long getTimestamp();
        
        /**
         * 获取事件类型
         */
        SecurityEventType getEventType();
        
        /**
         * 获取严重程度
         */
        SecuritySeverity getSeverity();
        
        /**
         * 获取消息
         */
        String getMessage();
        
        /**
         * 获取详细信息
         */
        String getDetails();
        
        /**
         * 获取来源IP
         */
        String getSourceIp();
        
        /**
         * 获取用户ID
         */
        String getUserId();
        
        /**
         * 转换为JSON字符串
         */
        String toJson();
    }
}