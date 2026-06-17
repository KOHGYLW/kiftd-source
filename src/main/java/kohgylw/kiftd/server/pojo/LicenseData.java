package kohgylw.kiftd.server.pojo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 企业授权码数据结构
 * 包含客户ID、到期时间、权限列表、硬件绑定等信息
 * 
 * @author 企业安全专家
 * @version 2.0
 */
public class LicenseData {
    
    /** 授权码版本 */
    private String version = "2.0";
    
    /** 客户ID */
    private String customerId;
    
    /** 客户名称 */
    private String customerName;
    
    /** 产品ID */
    private String productId;
    
    /** 产品版本 */
    private String productVersion;
    
    /** 授权类型 */
    private LicenseType licenseType;
    
    /** 授权开始时间 */
    private LocalDateTime startTime;
    
    /** 授权到期时间 */
    private LocalDateTime expirationTime;
    
    /** 最大用户数 */
    private Integer maxUsers;
    
    /** 功能权限列表 */
    private List<String> permissions;
    
    /** 硬件绑定信息 */
    private HardwareFingerprint hardwareFingerprint;
    
    /** 扩展属性 */
    private Map<String, Object> extensions;
    
    /** 授权码生成时间 */
    private LocalDateTime generatedAt;
    
    /** 授权码颁发者 */
    private String issuer;
    
    /** 数字签名 */
    private String digitalSignature;
    
    // 构造函数
    public LicenseData() {
        this.generatedAt = LocalDateTime.now();
    }
    
    public LicenseData(String customerId, String customerName, String productId, 
                      LicenseType licenseType, LocalDateTime expirationTime) {
        this();
        this.customerId = customerId;
        this.customerName = customerName;
        this.productId = productId;
        this.licenseType = licenseType;
        this.expirationTime = expirationTime;
    }
    
    // Getters and Setters
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    public String getCustomerName() {
        return customerName;
    }
    
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public String getProductVersion() {
        return productVersion;
    }
    
    public void setProductVersion(String productVersion) {
        this.productVersion = productVersion;
    }
    
    public LicenseType getLicenseType() {
        return licenseType;
    }
    
    public void setLicenseType(LicenseType licenseType) {
        this.licenseType = licenseType;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getExpirationTime() {
        return expirationTime;
    }
    
    public void setExpirationTime(LocalDateTime expirationTime) {
        this.expirationTime = expirationTime;
    }
    
    public Integer getMaxUsers() {
        return maxUsers;
    }
    
    public void setMaxUsers(Integer maxUsers) {
        this.maxUsers = maxUsers;
    }
    
    public List<String> getPermissions() {
        return permissions;
    }
    
    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
    
    public HardwareFingerprint getHardwareFingerprint() {
        return hardwareFingerprint;
    }
    
    public void setHardwareFingerprint(HardwareFingerprint hardwareFingerprint) {
        this.hardwareFingerprint = hardwareFingerprint;
    }
    
    public Map<String, Object> getExtensions() {
        return extensions;
    }
    
    public void setExtensions(Map<String, Object> extensions) {
        this.extensions = extensions;
    }
    
    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
    
    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
    
    public String getIssuer() {
        return issuer;
    }
    
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
    
    public String getDigitalSignature() {
        return digitalSignature;
    }
    
    public void setDigitalSignature(String digitalSignature) {
        this.digitalSignature = digitalSignature;
    }
    
    // 业务方法
    
    /**
     * 检查授权是否已过期
     */
    public boolean isExpired() {
        if (expirationTime == null) {
            return false; // 永久授权
        }
        return LocalDateTime.now().isAfter(expirationTime);
    }
    
    /**
     * 检查授权是否已生效
     */
    public boolean isActive() {
        if (startTime == null) {
            return !isExpired(); // 如果没有开始时间，只检查是否过期
        }
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(startTime) && !isExpired();
    }
    
    /**
     * 检查是否包含指定权限
     */
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }
    
    /**
     * 获取剩余有效天数
     */
    public long getRemainingDays() {
        if (expirationTime == null) {
            return Long.MAX_VALUE; // 永久授权
        }
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), expirationTime);
    }
    
    /**
     * 转换为JSON字符串（不包含数字签名）
     */
    public String toJsonForSigning() {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
        
        // 创建临时对象，不包含数字签名
        LicenseData temp = new LicenseData();
        temp.version = this.version;
        temp.customerId = this.customerId;
        temp.customerName = this.customerName;
        temp.productId = this.productId;
        temp.productVersion = this.productVersion;
        temp.licenseType = this.licenseType;
        temp.startTime = this.startTime;
        temp.expirationTime = this.expirationTime;
        temp.maxUsers = this.maxUsers;
        temp.permissions = this.permissions;
        temp.hardwareFingerprint = this.hardwareFingerprint;
        temp.extensions = this.extensions;
        temp.generatedAt = this.generatedAt;
        temp.issuer = this.issuer;
        // 不包含 digitalSignature
        
        return gson.toJson(temp);
    }
    
    /**
     * 转换为完整JSON字符串
     */
    public String toJson() {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
        return gson.toJson(this);
    }
    
    /**
     * 从JSON字符串创建对象
     */
    public static LicenseData fromJson(String json) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
        return gson.fromJson(json, LicenseData.class);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LicenseData that = (LicenseData) o;
        return Objects.equals(customerId, that.customerId) &&
               Objects.equals(productId, that.productId) &&
               Objects.equals(generatedAt, that.generatedAt);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(customerId, productId, generatedAt);
    }
    
    @Override
    public String toString() {
        return "LicenseData{" +
                "version='" + version + '\'' +
                ", customerId='" + customerId + '\'' +
                ", customerName='" + customerName + '\'' +
                ", productId='" + productId + '\'' +
                ", licenseType=" + licenseType +
                ", expirationTime=" + expirationTime +
                ", isExpired=" + isExpired() +
                ", isActive=" + isActive() +
                '}';
    }
    
    /**
     * 授权类型枚举
     */
    public enum LicenseType {
        /** 试用版 */
        TRIAL("trial", "试用版"),
        /** 标准版 */
        STANDARD("standard", "标准版"),
        /** 专业版 */
        PROFESSIONAL("professional", "专业版"),
        /** 企业版 */
        ENTERPRISE("enterprise", "企业版"),
        /** 永久授权 */
        PERPETUAL("perpetual", "永久授权"),
        /** 开发版 */
        DEVELOPMENT("development", "开发版");
        
        private final String code;
        private final String description;
        
        LicenseType(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static LicenseType fromCode(String code) {
            for (LicenseType type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown license type code: " + code);
        }
    }
}