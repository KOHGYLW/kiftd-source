package com.enterprise.license.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 系统配置实体类
 * 存储系统的各种配置参数
 */
@Entity
@Table(name = "system_configs", indexes = {
    @Index(name = "idx_system_config_key", columnList = "config_key", unique = true),
    @Index(name = "idx_system_config_category", columnList = "config_category"),
    @Index(name = "idx_system_config_is_active", columnList = "is_active"),
    @Index(name = "idx_system_config_created_time", columnList = "created_time")
})
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Comment("系统配置表")
public class SystemConfig extends BaseEntity {

    /**
     * 配置键（唯一标识）
     */
    @NotBlank(message = "配置键不能为空")
    @Size(max = 100, message = "配置键长度不能超过100个字符")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9._-]*$", message = "配置键格式不正确，只能包含字母、数字、点、下划线和连字符，且必须以字母开头")
    @Column(name = "config_key", length = 100, nullable = false, unique = true)
    @Comment("配置键")
    private String configKey;

    /**
     * 配置值
     */
    @Size(max = 2000, message = "配置值长度不能超过2000个字符")
    @Column(name = "config_value", length = 2000)
    @Comment("配置值")
    private String configValue;

    /**
     * 配置类别
     */
    @NotBlank(message = "配置类别不能为空")
    @Size(max = 50, message = "配置类别长度不能超过50个字符")
    @Column(name = "config_category", length = 50, nullable = false)
    @Comment("配置类别")
    private String configCategory;

    /**
     * 配置名称
     */
    @NotBlank(message = "配置名称不能为空")
    @Size(max = 200, message = "配置名称长度不能超过200个字符")
    @Column(name = "config_name", length = 200, nullable = false)
    @Comment("配置名称")
    private String configName;

    /**
     * 配置描述
     */
    @Size(max = 500, message = "配置描述长度不能超过500个字符")
    @Column(name = "config_description", length = 500)
    @Comment("配置描述")
    private String configDescription;

    /**
     * 数据类型
     */
    @NotBlank(message = "数据类型不能为空")
    @Size(max = 20, message = "数据类型长度不能超过20个字符")
    @Column(name = "data_type", length = 20, nullable = false)
    @Comment("数据类型")
    private String dataType = "STRING";

    /**
     * 默认值
     */
    @Size(max = 2000, message = "默认值长度不能超过2000个字符")
    @Column(name = "default_value", length = 2000)
    @Comment("默认值")
    private String defaultValue;

    /**
     * 是否激活
     */
    @Column(name = "is_active", columnDefinition = "BOOLEAN DEFAULT TRUE")
    @Comment("是否激活")
    private Boolean isActive = true;

    /**
     * 是否只读
     */
    @Column(name = "is_readonly", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Comment("是否只读")
    private Boolean isReadonly = false;

    /**
     * 是否敏感数据
     */
    @Column(name = "is_sensitive", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Comment("是否敏感数据")
    private Boolean isSensitive = false;

    /**
     * 是否系统内置
     */
    @Column(name = "is_system", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Comment("是否系统内置")
    private Boolean isSystem = false;

    /**
     * 优先级（数值越小优先级越高）
     */
    @Min(value = 0, message = "优先级不能为负数")
    @Column(name = "priority")
    @Comment("优先级")
    private Integer priority = 0;

    /**
     * 排序序号
     */
    @Min(value = 0, message = "排序序号不能为负数")
    @Column(name = "sort_order")
    @Comment("排序序号")
    private Integer sortOrder = 0;

    /**
     * 验证规则（正则表达式）
     */
    @Size(max = 500, message = "验证规则长度不能超过500个字符")
    @Column(name = "validation_rule", length = 500)
    @Comment("验证规则")
    private String validationRule;

    /**
     * 可选值列表（JSON格式）
     */
    @Size(max = 1000, message = "可选值列表长度不能超过1000个字符")
    @Column(name = "option_values", length = 1000)
    @Comment("可选值列表")
    private String optionValues;

    /**
     * 最小值
     */
    @Size(max = 50, message = "最小值长度不能超过50个字符")
    @Column(name = "min_value", length = 50)
    @Comment("最小值")
    private String minValue;

    /**
     * 最大值
     */
    @Size(max = 50, message = "最大值长度不能超过50个字符")
    @Column(name = "max_value", length = 50)
    @Comment("最大值")
    private String maxValue;

    /**
     * 单位
     */
    @Size(max = 20, message = "单位长度不能超过20个字符")
    @Column(name = "unit", length = 20)
    @Comment("单位")
    private String unit;

    /**
     * 配置组
     */
    @Size(max = 50, message = "配置组长度不能超过50个字符")
    @Column(name = "config_group", length = 50)
    @Comment("配置组")
    private String configGroup;

    /**
     * 标签（用于分类标记）
     */
    @Size(max = 200, message = "标签长度不能超过200个字符")
    @Column(name = "tags", length = 200)
    @Comment("标签")
    private String tags;

    /**
     * 生效时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "effective_time")
    @Comment("生效时间")
    private LocalDateTime effectiveTime;

    /**
     * 失效时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "expiry_time")
    @Comment("失效时间")
    private LocalDateTime expiryTime;

    /**
     * 最后修改人
     */
    @Size(max = 100, message = "最后修改人长度不能超过100个字符")
    @Column(name = "last_modified_by", length = 100)
    @Comment("最后修改人")
    private String lastModifiedBy;

    /**
     * 最后修改时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "last_modified_time")
    @Comment("最后修改时间")
    private LocalDateTime lastModifiedTime;

    /**
     * 版本号
     */
    @Min(value = 1, message = "版本号必须大于0")
    @Column(name = "version")
    @Comment("版本号")
    private Integer version = 1;

    /**
     * 环境（dev/test/prod）
     */
    @Size(max = 20, message = "环境长度不能超过20个字符")
    @Column(name = "environment", length = 20)
    @Comment("环境")
    private String environment = "prod";

    /**
     * 应用名称
     */
    @Size(max = 100, message = "应用名称长度不能超过100个字符")
    @Column(name = "application_name", length = 100)
    @Comment("应用名称")
    private String applicationName;

    /**
     * 备注信息
     */
    @Size(max = 1000, message = "备注信息长度不能超过1000个字符")
    @Column(name = "remarks", length = 1000)
    @Comment("备注信息")
    private String remarks;

    /**
     * 扩展属性（JSON格式）
     */
    @Size(max = 2000, message = "扩展属性长度不能超过2000个字符")
    @Column(name = "extended_properties", length = 2000)
    @Comment("扩展属性")
    private String extendedProperties;

    /**
     * 判断配置是否激活
     * @return true如果配置激活
     */
    public boolean isConfigActive() {
        return Boolean.TRUE.equals(isActive);
    }

    /**
     * 判断配置是否只读
     * @return true如果配置只读
     */
    public boolean isConfigReadonly() {
        return Boolean.TRUE.equals(isReadonly);
    }

    /**
     * 判断是否为敏感配置
     * @return true如果是敏感配置
     */
    public boolean isSensitiveConfig() {
        return Boolean.TRUE.equals(isSensitive);
    }

    /**
     * 判断是否为系统内置配置
     * @return true如果是系统内置配置
     */
    public boolean isSystemConfig() {
        return Boolean.TRUE.equals(isSystem);
    }

    /**
     * 判断配置是否有效（在有效期内）
     * @return true如果配置有效
     */
    public boolean isConfigValid() {
        LocalDateTime now = LocalDateTime.now();
        
        if (effectiveTime != null && now.isBefore(effectiveTime)) {
            return false; // 未到生效时间
        }
        
        if (expiryTime != null && now.isAfter(expiryTime)) {
            return false; // 已过期
        }
        
        return isConfigActive();
    }

    /**
     * 获取配置值，如果为空则返回默认值
     * @return 配置值或默认值
     */
    public String getValueOrDefault() {
        return configValue != null ? configValue : defaultValue;
    }

    /**
     * 根据数据类型获取强类型的配置值
     * @return 转换后的配置值
     */
    public Object getTypedValue() {
        String value = getValueOrDefault();
        if (value == null) {
            return null;
        }

        switch (dataType.toUpperCase()) {
            case "INTEGER":
                return Integer.valueOf(value);
            case "LONG":
                return Long.valueOf(value);
            case "DOUBLE":
                return Double.valueOf(value);
            case "BOOLEAN":
                return Boolean.valueOf(value);
            case "STRING":
            default:
                return value;
        }
    }

    /**
     * 验证配置值是否符合规则
     * @param value 要验证的值
     * @return true如果验证通过
     */
    public boolean validateValue(String value) {
        if (value == null) {
            return true; // 允许空值
        }

        // 验证正则表达式
        if (validationRule != null && !value.matches(validationRule)) {
            return false;
        }

        // 验证数据类型和范围
        try {
            switch (dataType.toUpperCase()) {
                case "INTEGER":
                    int intVal = Integer.parseInt(value);
                    if (minValue != null && intVal < Integer.parseInt(minValue)) {
                        return false;
                    }
                    if (maxValue != null && intVal > Integer.parseInt(maxValue)) {
                        return false;
                    }
                    break;
                case "LONG":
                    long longVal = Long.parseLong(value);
                    if (minValue != null && longVal < Long.parseLong(minValue)) {
                        return false;
                    }
                    if (maxValue != null && longVal > Long.parseLong(maxValue)) {
                        return false;
                    }
                    break;
                case "DOUBLE":
                    double doubleVal = Double.parseDouble(value);
                    if (minValue != null && doubleVal < Double.parseDouble(minValue)) {
                        return false;
                    }
                    if (maxValue != null && doubleVal > Double.parseDouble(maxValue)) {
                        return false;
                    }
                    break;
                case "BOOLEAN":
                    // 布尔值自动验证
                    Boolean.parseBoolean(value);
                    break;
            }
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }

    /**
     * 增加版本号
     */
    public void incrementVersion() {
        if (version == null) {
            version = 1;
        } else {
            version++;
        }
    }

    /**
     * 更新配置值
     * @param newValue 新的配置值
     * @param modifier 修改人
     */
    public void updateValue(String newValue, String modifier) {
        this.configValue = newValue;
        this.lastModifiedBy = modifier;
        this.lastModifiedTime = LocalDateTime.now();
        incrementVersion();
    }

    /**
     * 创建系统配置
     * @param key 配置键
     * @param value 配置值
     * @param category 配置类别
     * @param name 配置名称
     * @param description 配置描述
     * @return SystemConfig实例
     */
    public static SystemConfig createSystemConfig(String key, String value, String category, 
                                                 String name, String description) {
        SystemConfig config = new SystemConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setConfigCategory(category);
        config.setConfigName(name);
        config.setConfigDescription(description);
        config.setIsSystem(true);
        config.setEffectiveTime(LocalDateTime.now());
        return config;
    }
}