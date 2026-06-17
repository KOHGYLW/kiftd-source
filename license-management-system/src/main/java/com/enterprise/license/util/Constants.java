package com.enterprise.license.util;

/**
 * 系统常量
 */
public class Constants {

    /**
     * JWT相关常量
     */
    public static class JWT {
        public static final String TOKEN_HEADER = "Authorization";
        public static final String TOKEN_PREFIX = "Bearer ";
        public static final String SECRET_KEY = "license-management-system-secret-key-2024";
        public static final long EXPIRATION_TIME = 86400000L; // 24小时
        public static final String AUTHORITIES_KEY = "authorities";
    }

    /**
     * Redis相关常量
     */
    public static class Redis {
        public static final String TOKEN_PREFIX = "token:";
        public static final String USER_PREFIX = "user:";
        public static final String LICENSE_PREFIX = "license:";
        public static final long DEFAULT_TIMEOUT = 3600L; // 1小时
    }

    /**
     * 缓存相关常量
     */
    public static final class Cache {
        public static final String CUSTOMERS = "customers";
        public static final String LICENSES = "licenses";
        public static final String CUSTOMER_STATISTICS = "customer_statistics";
        public static final String LICENSE_STATISTICS = "license_statistics";
        public static final String DASHBOARD_DATA = "dashboard_data";
        public static final String CUSTOMER_ACTIVITY = "customer_activity";
        public static final String PRODUCT_USAGE = "product_usage";
        public static final String REGION_DISTRIBUTION = "region_distribution";
        
        // 缓存TTL (秒)
        public static final int SHORT_TTL = 300;      // 5分钟
        public static final int MEDIUM_TTL = 1800;    // 30分钟
        public static final int LONG_TTL = 3600;      // 1小时
    }

    /**
     * 业务常量
     */
    public static final class Business {
        public static final int DEFAULT_PAGE_SIZE = 10;
        public static final int MAX_PAGE_SIZE = 100;
        public static final int MAX_BATCH_SIZE = 1000;
        public static final int LICENSE_EXPIRING_DAYS = 30;
        public static final int VALIDATION_LOG_RETENTION_DAYS = 90;
    }

    /**
     * 系统配置常量
     */
    public static final class System {
        public static final String DEFAULT_TIMEZONE = "Asia/Shanghai";
        public static final String DATE_FORMAT = "yyyy-MM-dd";
        public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
        public static final String SYSTEM_USER = "system";
    }

    /**
     * 导出相关常量
     */
    public static final class Export {
        public static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        public static final String CSV_CONTENT_TYPE = "text/csv; charset=UTF-8";
        public static final int MAX_EXPORT_RECORDS = 10000;
    }

    /**
     * 授权验证常量
     */
    public static final class Validation {
        public static final int MAX_VALIDATION_ATTEMPTS = 100;
        public static final int VALIDATION_RATE_LIMIT_WINDOW = 3600; // 1小时
        public static final String VALIDATION_HASH_ALGORITHM = "SHA-256";
    }

    /**
     * 用户状态
     */
    public static class UserStatus {
        public static final String ACTIVE = "ACTIVE";
        public static final String INACTIVE = "INACTIVE";
        public static final String LOCKED = "LOCKED";
    }

    /**
     * 授权状态
     */
    public static class LicenseStatus {
        public static final String ACTIVE = "ACTIVE";
        public static final String EXPIRED = "EXPIRED";
        public static final String REVOKED = "REVOKED";
        public static final String PENDING = "PENDING";
    }

    /**
     * 授权类型
     */
    public static class LicenseType {
        public static final String TRIAL = "TRIAL";
        public static final String STANDARD = "STANDARD";
        public static final String PREMIUM = "PREMIUM";
        public static final String ENTERPRISE = "ENTERPRISE";
    }

    /**
     * 角色类型
     */
    public static class Role {
        public static final String ADMIN = "ADMIN";
        public static final String MANAGER = "MANAGER";
        public static final String USER = "USER";
    }

    /**
     * API路径常量
     */
    public static class ApiPaths {
        public static final String AUTH = "/api/auth";
        public static final String CUSTOMERS = "/api/customers";
        public static final String LICENSES = "/api/licenses";
        public static final String DASHBOARD = "/api/dashboard";
        public static final String SYSTEM = "/api/system";
    }

    /**
     * 分页相关常量
     */
    public static class Pagination {
        public static final int DEFAULT_PAGE = 1;
        public static final int DEFAULT_SIZE = 20;
        public static final int MAX_SIZE = 100;
    }

    /**
     * 授权验证错误码
     */
    public static class LicenseValidationErrors {
        public static final String LICENSE_NOT_FOUND = "LICENSE_NOT_FOUND";
        public static final String LICENSE_EXPIRED = "LICENSE_EXPIRED";
        public static final String LICENSE_INACTIVE = "LICENSE_INACTIVE";
        public static final String LICENSE_REVOKED = "LICENSE_REVOKED";
        public static final String LICENSE_SUSPENDED = "LICENSE_SUSPENDED";
        public static final String HARDWARE_MISMATCH = "HARDWARE_MISMATCH";
        public static final String IP_RESTRICTED = "IP_RESTRICTED";
        public static final String DOMAIN_RESTRICTED = "DOMAIN_RESTRICTED";
        public static final String MAX_USERS_EXCEEDED = "MAX_USERS_EXCEEDED";
        public static final String MAX_DEVICES_EXCEEDED = "MAX_DEVICES_EXCEEDED";
    }

    /**
     * 错误码常量
     */
    public static final class ErrorCode {
        public static final String BUSINESS_ERROR = "BUSINESS_ERROR";
        public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
        public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
        public static final String LICENSE_VALIDATION_ERROR = "LICENSE_VALIDATION_ERROR";
        public static final String UNAUTHORIZED = "UNAUTHORIZED";
        public static final String FORBIDDEN = "FORBIDDEN";
        public static final String SYSTEM_ERROR = "SYSTEM_ERROR";
    }

}