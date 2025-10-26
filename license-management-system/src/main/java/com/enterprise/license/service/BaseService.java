package com.enterprise.license.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 基础服务类
 */
@Slf4j
@Service
public class BaseService {

    /**
     * 获取当前时间
     */
    public LocalDateTime getCurrentTime() {
        return LocalDateTime.now();
    }

    /**
     * 记录操作日志
     */
    public void logOperation(String operation, String details) {
        log.info("操作: {} - 详情: {} - 时间: {}", operation, details, getCurrentTime());
    }

}