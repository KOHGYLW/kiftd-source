package com.enterprise.license.enums;

import lombok.Getter;

/**
 * 授权状态枚举
 */
@Getter
public enum LicenseStatus {
    
    /**
     * 激活状态
     */
    ACTIVE("ACTIVE", "激活"),
    
    /**
     * 非激活状态
     */
    INACTIVE("INACTIVE", "非激活"),
    
    /**
     * 已过期状态
     */
    EXPIRED("EXPIRED", "已过期"),
    
    /**
     * 暂停状态
     */
    SUSPENDED("SUSPENDED", "暂停"),
    
    /**
     * 已撤销状态
     */
    REVOKED("REVOKED", "已撤销"),
    
    /**
     * 待激活状态
     */
    PENDING_ACTIVATION("PENDING_ACTIVATION", "待激活");
    
    private final String code;
    private final String description;
    
    LicenseStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * 根据代码获取枚举值
     * @param code 状态代码
     * @return LicenseStatus枚举值
     */
    public static LicenseStatus fromCode(String code) {
        for (LicenseStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的授权状态代码: " + code);
    }
    
    /**
     * 判断是否为有效状态（可以正常使用）
     * @return true如果是有效状态
     */
    public boolean isValid() {
        return this == ACTIVE;
    }
}