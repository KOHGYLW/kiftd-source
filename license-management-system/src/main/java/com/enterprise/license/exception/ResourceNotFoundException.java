package com.enterprise.license.exception;

/**
 * 资源未找到异常
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message);
    }

    public ResourceNotFoundException(String resourceType, Object resourceId) {
        super("RESOURCE_NOT_FOUND", resourceType + " not found with id: " + resourceId);
    }
}