package com.enterprise.license.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 授权密钥实体类
 * 存储授权密钥的详细信息，包括密钥内容、签名等
 */
@Entity
@Table(name = "license_keys", indexes = {
    @Index(name = "idx_license_key_license_id", columnList = "license_id"),
    @Index(name = "idx_license_key_key_hash", columnList = "key_hash", unique = true),
    @Index(name = "idx_license_key_is_active", columnList = "is_active"),
    @Index(name = "idx_license_key_created_time", columnList = "created_time")
})
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"license"})
@ToString(callSuper = true, exclude = {"license", "keyContent", "privateKey"})
@Comment("授权密钥表")
public class LicenseKey extends BaseEntity {

    /**
     * 所属授权
     */
    @NotNull(message = "所属授权不能为空")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "license_id", nullable = false, foreignKey = @ForeignKey(name = "fk_license_key_license"))
    @JsonBackReference
    @Comment("所属授权")
    private License license;

    /**
     * 密钥内容（加密后的授权信息）
     */
    @NotBlank(message = "密钥内容不能为空")
    @Lob
    @Column(name = "key_content", nullable = false, columnDefinition = "TEXT")
    @Comment("密钥内容")
    private String keyContent;

    /**
     * 密钥哈希值（用于快速检索和验证）
     */
    @NotBlank(message = "密钥哈希值不能为空")
    @Size(max = 128, message = "密钥哈希值长度不能超过128个字符")
    @Column(name = "key_hash", length = 128, nullable = false, unique = true)
    @Comment("密钥哈希值")
    private String keyHash;

    /**
     * 公钥
     */
    @NotBlank(message = "公钥不能为空")
    @Lob
    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
    @Comment("公钥")
    private String publicKey;

    /**
     * 私钥（用于签名，加密存储）
     */
    @Lob
    @Column(name = "private_key", columnDefinition = "TEXT")
    @Comment("私钥")
    private String privateKey;

    /**
     * 数字签名
     */
    @NotBlank(message = "数字签名不能为空")
    @Lob
    @Column(name = "digital_signature", nullable = false, columnDefinition = "TEXT")
    @Comment("数字签名")
    private String digitalSignature;

    /**
     * 签名算法
     */
    @NotBlank(message = "签名算法不能为空")
    @Size(max = 50, message = "签名算法长度不能超过50个字符")
    @Column(name = "signature_algorithm", length = 50, nullable = false)
    @Comment("签名算法")
    private String signatureAlgorithm = "SHA256withRSA";

    /**
     * 加密算法
     */
    @NotBlank(message = "加密算法不能为空")
    @Size(max = 50, message = "加密算法长度不能超过50个字符")
    @Column(name = "encryption_algorithm", length = 50, nullable = false)
    @Comment("加密算法")
    private String encryptionAlgorithm = "RSA";

    /**
     * 密钥长度
     */
    @NotNull(message = "密钥长度不能为空")
    @Min(value = 1024, message = "密钥长度不能小于1024位")
    @Column(name = "key_length", nullable = false)
    @Comment("密钥长度")
    private Integer keyLength = 2048;

    /**
     * 密钥版本
     */
    @NotBlank(message = "密钥版本不能为空")
    @Size(max = 20, message = "密钥版本长度不能超过20个字符")
    @Pattern(regexp = "^v\\d+\\.\\d+(\\.\\d+)?$", message = "密钥版本格式不正确，应为v1.0或v1.0.1格式")
    @Column(name = "key_version", length = 20, nullable = false)
    @Comment("密钥版本")
    private String keyVersion = "v1.0";

    /**
     * 是否为主密钥
     */
    @Column(name = "is_primary", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Comment("是否为主密钥")
    private Boolean isPrimary = false;

    /**
     * 是否激活
     */
    @Column(name = "is_active", columnDefinition = "BOOLEAN DEFAULT TRUE")
    @Comment("是否激活")
    private Boolean isActive = true;

    /**
     * 密钥生成时间
     */
    @Column(name = "generated_time")
    @Comment("密钥生成时间")
    private LocalDateTime generatedTime;

    /**
     * 密钥激活时间
     */
    @Column(name = "activated_time")
    @Comment("密钥激活时间")
    private LocalDateTime activatedTime;

    /**
     * 密钥撤销时间
     */
    @Column(name = "revoked_time")
    @Comment("密钥撤销时间")
    private LocalDateTime revokedTime;

    /**
     * 最后使用时间
     */
    @Column(name = "last_used_time")
    @Comment("最后使用时间")
    private LocalDateTime lastUsedTime;

    /**
     * 使用次数
     */
    @Min(value = 0, message = "使用次数不能为负数")
    @Column(name = "usage_count")
    @Comment("使用次数")
    private Integer usageCount = 0;

    /**
     * 验证失败次数
     */
    @Min(value = 0, message = "验证失败次数不能为负数")
    @Column(name = "validation_failure_count")
    @Comment("验证失败次数")
    private Integer validationFailureCount = 0;

    /**
     * 最大验证失败次数（超过后自动撤销）
     */
    @Min(value = 1, message = "最大验证失败次数必须大于0")
    @Column(name = "max_validation_failures")
    @Comment("最大验证失败次数")
    private Integer maxValidationFailures = 10;

    /**
     * 客户端标识（生成密钥的客户端信息）
     */
    @Size(max = 200, message = "客户端标识长度不能超过200个字符")
    @Column(name = "client_identifier", length = 200)
    @Comment("客户端标识")
    private String clientIdentifier;

    /**
     * 绑定的硬件指纹
     */
    @Size(max = 500, message = "硬件指纹长度不能超过500个字符")
    @Column(name = "hardware_fingerprint", length = 500)
    @Comment("绑定的硬件指纹")
    private String hardwareFingerprint;

    /**
     * 授权范围（JSON格式，描述密钥可访问的功能）
     */
    @Size(max = 2000, message = "授权范围长度不能超过2000个字符")
    @Column(name = "authorization_scope", length = 2000)
    @Comment("授权范围")
    private String authorizationScope;

    /**
     * 密钥元数据（JSON格式，存储额外信息）
     */
    @Size(max = 2000, message = "密钥元数据长度不能超过2000个字符")
    @Column(name = "key_metadata", length = 2000)
    @Comment("密钥元数据")
    private String keyMetadata;

    /**
     * 撤销原因
     */
    @Size(max = 500, message = "撤销原因长度不能超过500个字符")
    @Column(name = "revocation_reason", length = 500)
    @Comment("撤销原因")
    private String revocationReason;

    /**
     * 备注信息
     */
    @Size(max = 1000, message = "备注信息长度不能超过1000个字符")
    @Column(name = "remarks", length = 1000)
    @Comment("备注信息")
    private String remarks;

    /**
     * 判断密钥是否有效
     * @return true如果密钥有效
     */
    public boolean isValid() {
        return Boolean.TRUE.equals(isActive) && revokedTime == null;
    }

    /**
     * 判断密钥是否已被撤销
     * @return true如果已被撤销
     */
    public boolean isRevoked() {
        return revokedTime != null;
    }

    /**
     * 增加使用次数
     */
    public void incrementUsageCount() {
        if (usageCount == null) {
            usageCount = 0;
        }
        usageCount++;
        lastUsedTime = LocalDateTime.now();
    }

    /**
     * 增加验证失败次数
     */
    public void incrementValidationFailureCount() {
        if (validationFailureCount == null) {
            validationFailureCount = 0;
        }
        validationFailureCount++;
        
        // 如果验证失败次数超过最大限制，自动撤销密钥
        if (maxValidationFailures != null && validationFailureCount >= maxValidationFailures) {
            revoke("验证失败次数超过限制");
        }
    }

    /**
     * 撤销密钥
     * @param reason 撤销原因
     */
    public void revoke(String reason) {
        isActive = false;
        revokedTime = LocalDateTime.now();
        revocationReason = reason;
    }

    /**
     * 激活密钥
     */
    public void activate() {
        isActive = true;
        activatedTime = LocalDateTime.now();
        revokedTime = null;
        revocationReason = null;
    }

    /**
     * 重置验证失败计数
     */
    public void resetValidationFailureCount() {
        validationFailureCount = 0;
    }

    /**
     * 判断是否可以继续验证（未超过失败次数限制）
     * @return true如果可以继续验证
     */
    public boolean canValidate() {
        if (maxValidationFailures == null) {
            return true;
        }
        return validationFailureCount == null || validationFailureCount < maxValidationFailures;
    }

    /**
     * 设置为主密钥
     */
    public void setPrimaryKey() {
        isPrimary = true;
    }

    /**
     * 取消主密钥状态
     */
    public void unsetPrimaryKey() {
        isPrimary = false;
    }
}