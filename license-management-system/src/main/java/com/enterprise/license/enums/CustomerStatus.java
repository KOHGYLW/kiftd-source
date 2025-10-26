package com.enterprise.license.enums;

import lombok.Getter;

/**
 * 客户状态枚举
 */
@Getter
public enum CustomerStatus {
    
    /**
     * 激活状态
     */
    ACTIVE("ACTIVE", "激活"),
    
    /**
     * 非激活状态
     */
    INACTIVE("INACTIVE", "非激活"),
    
    /**
     * 暂停状态
     */
    SUSPENDED("SUSPENDED", "暂停"),
    
    /**
     * 已删除状态
     */
    DELETED("DELETED", "已删除");
    
    private final String code;
    private final String description;
    
    CustomerStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * 根据代码获取枚举值
     * @param code 状态代码
     * @return CustomerStatus枚举值
     */
    public static CustomerStatus fromCode(String code) {
        for (CustomerStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的客户状态代码: " + code);
    }
}