package com.enterprise.license.controller;

import com.enterprise.license.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 */
@Tag(name = "系统健康检查", description = "系统状态和健康检查相关接口")
@RestController
@RequestMapping("/health")
public class HealthController {

    @Operation(summary = "系统健康检查", description = "检查系统运行状态")
    @GetMapping("/check")
    public ApiResponse<Map<String, Object>> healthCheck() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("timestamp", LocalDateTime.now());
        healthInfo.put("version", "1.0.0");
        healthInfo.put("description", "License Management System is running");
        
        return ApiResponse.success("系统运行正常", healthInfo);
    }

}