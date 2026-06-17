package com.enterprise.license.enums;

import lombok.Getter;

/**
 * 授权类型枚举
 */
@Getter
public enum LicenseType {
    
    /**
     * 试用版授权
     */
    TRIAL("TRIAL", "试用版", 30),
    
    /**
     * 基础版授权
     */
    BASIC("BASIC", "基础版", 365),
    
    /**
     * 专业版授权
     */
    PROFESSIONAL("PROFESSIONAL", "专业版", 365),
    
    /**
     * 企业版授权
     */
    ENTERPRISE("ENTERPRISE", "企业版", 365),
    
    /**
     * 永久授权
     */
    PERPETUAL("PERPETUAL", "永久授权", 0),
    
    /**
     * 开发者授权
     */
    DEVELOPER("DEVELOPER", "开发者版", 365),
    
    /**
     * 教育版授权
     */
    EDUCATIONAL("EDUCATIONAL", "教育版", 365);
    
    private final String code;
    private final String description;
    private final int defaultValidityDays; // 默认有效期天数，0表示永久
    
    LicenseType(String code, String description, int defaultValidityDays) {
        this.code = code;
        this.description = description;
        this.defaultValidityDays = defaultValidityDays;
    }
    
    /**
     * 根据代码获取枚举值
     * @param code 类型代码
     * @return LicenseType枚举值
     */
    public static LicenseType fromCode(String code) {
        for (LicenseType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的授权类型代码: " + code);
    }
    
    /**
     * 判断是否为永久授权
     * @return true如果是永久授权
     */
    public boolean isPerpetual() {
        return this == PERPETUAL;
    }
    
    /**
     * 判断是否为试用版
     * @return true如果是试用版
     */
    public boolean isTrial() {
        return this == TRIAL;
    }
}