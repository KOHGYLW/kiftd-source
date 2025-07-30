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
 * 授权历史记录实体类
 * 记录授权的所有变更历史，用于审计追踪
 */
@Entity
@Table(name = "license_histories", indexes = {
    @Index(name = "idx_license_history_license_id", columnList = "license_id"),
    @Index(name = "idx_license_history_operation_type", columnList = "operation_type"),
    @Index(name = "idx_license_history_operation_time", columnList = "operation_time"),
    @Index(name = "idx_license_history_operator", columnList = "operator"),
    @Index(name = "idx_license_history_created_time", columnList = "created_time")
})
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"license"})
@ToString(callSuper = true, exclude = {"license"})
@Comment("授权历史记录表")
public class LicenseHistory extends BaseEntity {

    /**
     * 所属授权
     */
    @NotNull(message = "所属授权不能为空")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "license_id", nullable = false, foreignKey = @ForeignKey(name = "fk_license_history_license"))
    @JsonBackReference
    @Comment("所属授权")
    private License license;

    /**
     * 操作类型
     */
    @NotBlank(message = "操作类型不能为空")
    @Size(max = 50, message = "操作类型长度不能超过50个字符")
    @Column(name = "operation_type", length = 50, nullable = false)
    @Comment("操作类型")
    private String operationType;

    /**
     * 操作时间
     */
    @NotNull(message = "操作时间不能为空")
    @Column(name = "operation_time", nullable = false)
    @Comment("操作时间")
    private LocalDateTime operationTime;

    /**
     * 操作人员
     */
    @NotBlank(message = "操作人员不能为空")
    @Size(max = 100, message = "操作人员长度不能超过100个字符")
    @Column(name = "operator", length = 100, nullable = false)
    @Comment("操作人员")
    private String operator;

    /**
     * 操作人员角色
     */
    @Size(max = 50, message = "操作人员角色长度不能超过50个字符")
    @Column(name = "operator_role", length = 50)
    @Comment("操作人员角色")
    private String operatorRole;

    /**
     * 操作描述
     */
    @NotBlank(message = "操作描述不能为空")
    @Size(max = 500, message = "操作描述长度不能超过500个字符")
    @Column(name = "operation_description", length = 500, nullable = false)
    @Comment("操作描述")
    private String operationDescription;

    /**
     * 变更前的值（JSON格式）
     */
    @Size(max = 2000, message = "变更前的值长度不能超过2000个字符")
    @Column(name = "old_value", length = 2000)
    @Comment("变更前的值")
    private String oldValue;

    /**
     * 变更后的值（JSON格式）
     */
    @Size(max = 2000, message = "变更后的值长度不能超过2000个字符")
    @Column(name = "new_value", length = 2000)
    @Comment("变更后的值")
    private String newValue;

    /**
     * 变更的字段名
     */
    @Size(max = 100, message = "变更的字段名长度不能超过100个字符")
    @Column(name = "changed_fields", length = 100)
    @Comment("变更的字段名")
    private String changedFields;

    /**
     * 客户端IP地址
     */
    @Size(max = 45, message = "客户端IP地址长度不能超过45个字符")
    @Pattern(regexp = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$|^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$", message = "IP地址格式不正确")
    @Column(name = "client_ip", length = 45)
    @Comment("客户端IP地址")
    private String clientIp;

    /**
     * 用户代理信息
     */
    @Size(max = 500, message = "用户代理信息长度不能超过500个字符")
    @Column(name = "user_agent", length = 500)
    @Comment("用户代理信息")
    private String userAgent;

    /**
     * 会话ID
     */
    @Size(max = 64, message = "会话ID长度不能超过64个字符")
    @Column(name = "session_id", length = 64)
    @Comment("会话ID")
    private String sessionId;

    /**
     * 业务流水号
     */
    @Size(max = 64, message = "业务流水号长度不能超过64个字符")
    @Column(name = "business_transaction_id", length = 64)
    @Comment("业务流水号")
    private String businessTransactionId;

    /**
     * 操作来源
     */
    @Size(max = 50, message = "操作来源长度不能超过50个字符")
    @Column(name = "operation_source", length = 50)
    @Comment("操作来源")
    private String operationSource;

    /**
     * 操作级别
     */
    @Size(max = 20, message = "操作级别长度不能超过20个字符")
    @Column(name = "operation_level", length = 20)
    @Comment("操作级别")
    private String operationLevel = "INFO";

    /**
     * 是否敏感操作
     */
    @Column(name = "is_sensitive", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Comment("是否敏感操作")
    private Boolean isSensitive = false;

    /**
     * 操作结果
     */
    @NotBlank(message = "操作结果不能为空")
    @Size(max = 20, message = "操作结果长度不能超过20个字符")
    @Column(name = "operation_result", length = 20, nullable = false)
    @Comment("操作结果")
    private String operationResult = "SUCCESS";

    /**
     * 错误信息
     */
    @Size(max = 1000, message = "错误信息长度不能超过1000个字符")
    @Column(name = "error_message", length = 1000)
    @Comment("错误信息")
    private String errorMessage;

    /**
     * 执行时间（毫秒）
     */
    @Min(value = 0, message = "执行时间不能为负数")
    @Column(name = "execution_time_ms")
    @Comment("执行时间（毫秒）")
    private Long executionTimeMs;

    /**
     * 相关实体ID
     */
    @Size(max = 64, message = "相关实体ID长度不能超过64个字符")
    @Column(name = "related_entity_id", length = 64)
    @Comment("相关实体ID")
    private String relatedEntityId;

    /**
     * 相关实体类型
     */
    @Size(max = 50, message = "相关实体类型长度不能超过50个字符")
    @Column(name = "related_entity_type", length = 50)
    @Comment("相关实体类型")
    private String relatedEntityType;

    /**
     * 审批流程ID
     */
    @Size(max = 64, message = "审批流程ID长度不能超过64个字符")
    @Column(name = "approval_process_id", length = 64)
    @Comment("审批流程ID")
    private String approvalProcessId;

    /**
     * 备注信息
     */
    @Size(max = 1000, message = "备注信息长度不能超过1000个字符")
    @Column(name = "remarks", length = 1000)
    @Comment("备注信息")
    private String remarks;

    /**
     * 扩展信息（JSON格式）
     */
    @Size(max = 2000, message = "扩展信息长度不能超过2000个字符")
    @Column(name = "extended_info", length = 2000)
    @Comment("扩展信息")
    private String extendedInfo;

    /**
     * 判断操作是否成功
     * @return true如果操作成功
     */
    public boolean isOperationSuccessful() {
        return "SUCCESS".equals(operationResult);
    }

    /**
     * 判断是否为敏感操作
     * @return true如果是敏感操作
     */
    public boolean isSensitiveOperation() {
        return Boolean.TRUE.equals(isSensitive);
    }

    /**
     * 设置操作成功
     */
    public void setOperationSuccess() {
        this.operationResult = "SUCCESS";
        this.errorMessage = null;
    }

    /**
     * 设置操作失败
     * @param errorMessage 错误信息
     */
    public void setOperationFailure(String errorMessage) {
        this.operationResult = "FAILURE";
        this.errorMessage = errorMessage;
    }

    /**
     * 标记为敏感操作
     */
    public void markAsSensitive() {
        this.isSensitive = true;
        if ("INFO".equals(this.operationLevel)) {
            this.operationLevel = "WARN";
        }
    }

    /**
     * 计算执行时间（秒）
     * @return 执行时间（秒）
     */
    public double getExecutionTimeSeconds() {
        return executionTimeMs != null ? executionTimeMs / 1000.0 : 0.0;
    }

    /**
     * 创建标准的操作记录
     * @param license 授权对象
     * @param operationType 操作类型
     * @param operator 操作人员
     * @param description 操作描述
     * @return LicenseHistory实例
     */
    public static LicenseHistory createOperationRecord(License license, String operationType, 
                                                      String operator, String description) {
        LicenseHistory history = new LicenseHistory();
        history.setLicense(license);
        history.setOperationType(operationType);
        history.setOperationTime(LocalDateTime.now());
        history.setOperator(operator);
        history.setOperationDescription(description);
        history.setOperationResult("SUCCESS");
        return history;
    }

    /**
     * 创建变更记录
     * @param license 授权对象
     * @param operator 操作人员
     * @param description 操作描述
     * @param changedFields 变更字段
     * @param oldValue 旧值
     * @param newValue 新值
     * @return LicenseHistory实例
     */
    public static LicenseHistory createChangeRecord(License license, String operator, 
                                                   String description, String changedFields,
                                                   String oldValue, String newValue) {
        LicenseHistory history = createOperationRecord(license, "UPDATE", operator, description);
        history.setChangedFields(changedFields);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        return history;
    }
}