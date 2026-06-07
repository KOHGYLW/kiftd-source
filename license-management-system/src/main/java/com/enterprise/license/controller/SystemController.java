package com.enterprise.license.controller;

import com.enterprise.license.dto.ApiResponse;
import com.enterprise.license.dto.PageResponse;
import com.enterprise.license.dto.system.*;
import com.enterprise.license.service.SystemService;
import com.enterprise.license.util.Constants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 系统管理控制器
 */
@RestController
@RequestMapping(Constants.ApiPaths.SYSTEM)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "系统管理", description = "系统管理相关接口")
@PreAuthorize("hasRole('ADMIN')")
public class SystemController {

    private final SystemService systemService;

    @GetMapping("/config")
    @Operation(summary = "获取系统配置", description = "获取系统配置信息")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "获取成功"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证"),
        @SwaggerApiResponse(responseCode = "403", description = "权限不足")
    })
    public ResponseEntity<ApiResponse<SystemConfig>> getSystemConfig() {
        
        log.info("获取系统配置");
        
        SystemConfig config = systemService.getSystemConfig();
        
        return ResponseEntity.ok(ApiResponse.success("获取系统配置成功", config));
    }

    @PutMapping("/config")
    @Operation(summary = "更新系统配置", description = "更新系统配置信息")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "更新成功"),
        @SwaggerApiResponse(responseCode = "400", description = "请求参数错误"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证"),
        @SwaggerApiResponse(responseCode = "403", description = "权限不足")
    })
    public ResponseEntity<ApiResponse<SystemConfig>> updateSystemConfig(
            @Valid @RequestBody SystemConfigUpdateRequest request,
            Authentication authentication) {
        
        log.info("更新系统配置, 操作用户: {}", authentication.getName());
        
        SystemConfig config = systemService.updateSystemConfig(request, authentication.getName());
        
        log.info("系统配置更新成功");
        return ResponseEntity.ok(ApiResponse.success("系统配置更新成功", config));
    }

    @GetMapping("/keys")
    @Operation(summary = "获取密钥列表", description = "获取系统中的密钥信息")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "获取成功"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证"),
        @SwaggerApiResponse(responseCode = "403", description = "权限不足")
    })
    public ResponseEntity<ApiResponse<List<KeyInfo>>> getKeys() {
        
        log.info("获取密钥列表");
        
        List<KeyInfo> keys = systemService.getKeys();
        
        return ResponseEntity.ok(ApiResponse.success("获取密钥列表成功", keys));
    }

    @PostMapping("/keys/generate")
    @Operation(summary = "生成新密钥", description = "生成新的加密密钥")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "生成成功"),
        @SwaggerApiResponse(responseCode = "400", description = "请求参数错误"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证"),
        @SwaggerApiResponse(responseCode = "403", description = "权限不足")
    })
    public ResponseEntity<ApiResponse<KeyInfo>> generateKey(
            @Valid @RequestBody KeyGenerationRequest request,
            Authentication authentication) {
        
        log.info("生成新密钥: type={}, 操作用户: {}", request.getKeyType(), authentication.getName());
        
        KeyInfo keyInfo = systemService.generateKey(request, authentication.getName());
        
        log.info("密钥生成成功: {}", keyInfo.getId());
        return ResponseEntity.ok(ApiResponse.success("密钥生成成功", keyInfo));
    }

    @PostMapping("/keys/{id}/rotate")
    @Operation(summary = "轮换密钥", description = "轮换指定的密钥")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "轮换成功"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证"),
        @SwaggerApiResponse(responseCode = "403", description = "权限不足"),
        @SwaggerApiResponse(responseCode = "404", description = "密钥不存在")
    })
    public ResponseEntity<ApiResponse<KeyInfo>> rotateKey(
            @Parameter(description = "密钥ID", required = true)
            @PathVariable Long id,
            Authentication authentication) {
        
        log.info("轮换密钥: {}, 操作用户: {}", id, authentication.getName());
        
        KeyInfo keyInfo = systemService.rotateKey(id, authentication.getName());
        
        log.info("密钥轮换成功: {}", id);
        return ResponseEntity.ok(ApiResponse.success("密钥轮换成功", keyInfo));
    }

    @DeleteMapping("/keys/{id}")
    @Operation(summary = "删除密钥", description = "删除指定的密钥")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "删除成功"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证"),
        @SwaggerApiResponse(responseCode = "403", description = "权限不足"),
        @SwaggerApiResponse(responseCode = "404", description = "密钥不存在"),
        @SwaggerApiResponse(responseCode = "409", description = "密钥正在使用中，无法删除")
    })
    public ResponseEntity<ApiResponse<Void>> deleteKey(
            @Parameter(description = "密钥ID", required = true)
            @PathVariable Long id,
            Authentication authentication) {
        
        log.info("删除密钥: {}, 操作用户: {}", id, authentication.getName());
        
        systemService.deleteKey(id, authentication.getName());
        
        log.info("密钥删除成功: {}", id);
        return ResponseEntity.ok(ApiResponse.success("密钥删除成功"));
    }

    @GetMapping("/logs")
    @Operation(summary = "查询系统日志", description = "根据条件查询系统操作日志")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "查询成功"),
        @SwaggerApiResponse(responseCode = "400", description = "请求参数错误"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证"),
        @SwaggerApiResponse(responseCode = "403", description = "权限不足")
    })
    public ResponseEntity<ApiResponse<PageResponse<SystemLog>>> getLogs(
            @Parameter(description = "开始日期") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) LocalDate endDate,
            @Parameter(description = "日志级别") @RequestParam(required = false) String level,
            @Parameter(description = "操作用户") @RequestParam(required = false) String username,
            @Parameter(description = "操作类型") @RequestParam(required = false) String operationType,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size) {
        
        log.info("查询系统日志: startDate={}, endDate={}, level={}, username={}", 
                startDate, endDate, level, username);
        
        LogSearchRequest searchRequest = new LogSearchRequest();
        searchRequest.setStartDate(startDate);
        searchRequest.setEndDate(endDate);
        searchRequest.setLevel(level);
        searchRequest.setUsername(username);
        searchRequest.setOperationType(operationType);
        searchRequest.setKeyword(keyword);
        searchRequest.setPage(page);
        searchRequest.setSize(size);
        
        PageResponse<SystemLog> logs = systemService.searchLogs(searchRequest);
        
        return ResponseEntity.ok(ApiResponse.success("查询系统日志成功", logs));
    }

    @GetMapping("/info")
    @Operation(summary = "获取系统信息", description = "获取系统运行信息")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "获取成功"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证"),
        @SwaggerApiResponse(responseCode = "403", description = "权限不足")
    })
    public ResponseEntity<ApiResponse<SystemInfo>> getSystemInfo() {
        
        log.info("获取系统信息");
        
        SystemInfo systemInfo = systemService.getSystemInfo();
        
        return ResponseEntity.ok(ApiResponse.success("获取系统信息成功", systemInfo));
    }

    @PostMapping("/backup")
    @Operation(summary = "创建系统备份", description = "创建系统数据备份")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "备份创建成功"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证"),
        @SwaggerApiResponse(responseCode = "403", description = "权限不足"),
        @SwaggerApiResponse(responseCode = "500", description = "备份创建失败")
    })
    public ResponseEntity<ApiResponse<BackupInfo>> createBackup(
            @Valid @RequestBody BackupRequest request,
            Authentication authentication) {
        
        log.info("创建系统备份: type={}, 操作用户: {}", request.getBackupType(), authentication.getName());
        
        BackupInfo backupInfo = systemService.createBackup(request, authentication.getName());
        
        log.info("系统备份创建成功: {}", backupInfo.getId());
        return ResponseEntity.ok(ApiResponse.success("系统备份创建成功", backupInfo));
    }

    @GetMapping("/backups")
    @Operation(summary = "获取备份列表", description = "获取系统备份记录列表")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "获取成功"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证"),
        @SwaggerApiResponse(responseCode = "403", description = "权限不足")
    })
    public ResponseEntity<ApiResponse<List<BackupInfo>>> getBackups() {
        
        log.info("获取备份列表");
        
        List<BackupInfo> backups = systemService.getBackups();
        
        return ResponseEntity.ok(ApiResponse.success("获取备份列表成功", backups));
    }

    @PostMapping("/backups/{id}/restore")
    @Operation(summary = "恢复系统备份", description = "从指定备份恢复系统数据")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "恢复成功"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证"),
        @SwaggerApiResponse(responseCode = "403", description = "权限不足"),
        @SwaggerApiResponse(responseCode = "404", description = "备份不存在"),
        @SwaggerApiResponse(responseCode = "500", description = "恢复失败")
    })
    public ResponseEntity<ApiResponse<Void>> restoreBackup(
            @Parameter(description = "备份ID", required = true)
            @PathVariable Long id,
            Authentication authentication) {
        
        log.info("恢复系统备份: {}, 操作用户: {}", id, authentication.getName());
        
        systemService.restoreBackup(id, authentication.getName());
        
        log.info("系统备份恢复成功: {}", id);
        return ResponseEntity.ok(ApiResponse.success("系统备份恢复成功"));
    }

    @PostMapping("/maintenance")
    @Operation(summary = "系统维护", description = "执行系统维护操作")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "维护成功"),
        @SwaggerApiResponse(responseCode = "400", description = "请求参数错误"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证"),
        @SwaggerApiResponse(responseCode = "403", description = "权限不足")
    })
    public ResponseEntity<ApiResponse<MaintenanceResult>> performMaintenance(
            @Valid @RequestBody MaintenanceRequest request,
            Authentication authentication) {
        
        log.info("执行系统维护: operations={}, 操作用户: {}", 
                request.getOperations(), authentication.getName());
        
        MaintenanceResult result = systemService.performMaintenance(request, authentication.getName());
        
        log.info("系统维护完成");
        return ResponseEntity.ok(ApiResponse.success("系统维护完成", result));
    }

    @PostMapping("/import")
    @Operation(summary = "导入数据", description = "从文件导入系统数据")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "导入成功"),
        @SwaggerApiResponse(responseCode = "400", description = "文件格式错误"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证"),
        @SwaggerApiResponse(responseCode = "403", description = "权限不足")
    })
    public ResponseEntity<ApiResponse<ImportResult>> importData(
            @Parameter(description = "导入文件", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "数据类型") 
            @RequestParam String dataType,
            @Parameter(description = "是否覆盖现有数据") 
            @RequestParam(defaultValue = "false") Boolean overwrite,
            Authentication authentication) {
        
        log.info("导入数据: dataType={}, fileName={}, 操作用户: {}", 
                dataType, file.getOriginalFilename(), authentication.getName());
        
        ImportResult result = systemService.importData(file, dataType, overwrite, authentication.getName());
        
        log.info("数据导入完成: 成功={}, 失败={}", result.getSuccessCount(), result.getFailureCount());
        return ResponseEntity.ok(ApiResponse.success("数据导入完成", result));
    }

    @GetMapping("/export")
    @Operation(summary = "导出数据", description = "导出系统数据到文件")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "导出成功"),
        @SwaggerApiResponse(responseCode = "400", description = "请求参数错误"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证"),
        @SwaggerApiResponse(responseCode = "403", description = "权限不足")
    })
    public ResponseEntity<ApiResponse<ExportResult>> exportData(
            @Parameter(description = "数据类型") @RequestParam String dataType,
            @Parameter(description = "导出格式") @RequestParam(defaultValue = "EXCEL") String format,
            @Parameter(description = "开始日期") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) LocalDate endDate,
            Authentication authentication) {
        
        log.info("导出数据: dataType={}, format={}, 操作用户: {}", 
                dataType, format, authentication.getName());
        
        ExportRequest exportRequest = new ExportRequest();
        exportRequest.setDataType(dataType);
        exportRequest.setFormat(format);
        exportRequest.setStartDate(startDate);
        exportRequest.setEndDate(endDate);
        
        ExportResult result = systemService.exportData(exportRequest, authentication.getName());
        
        log.info("数据导出完成: {}", result.getFileName());
        return ResponseEntity.ok(ApiResponse.success("数据导出完成", result));
    }

    @GetMapping("/health")
    @Operation(summary = "系统健康检查", description = "检查系统各组件的健康状态")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "检查完成"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证"),
        @SwaggerApiResponse(responseCode = "403", description = "权限不足")
    })
    public ResponseEntity<ApiResponse<Map<String, HealthStatus>>> healthCheck() {
        
        log.info("执行系统健康检查");
        
        Map<String, HealthStatus> healthStatus = systemService.performHealthCheck();
        
        return ResponseEntity.ok(ApiResponse.success("系统健康检查完成", healthStatus));
    }

    @PostMapping("/cache/clear")
    @Operation(summary = "清理缓存", description = "清理系统缓存")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "清理成功"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证"),
        @SwaggerApiResponse(responseCode = "403", description = "权限不足")
    })
    public ResponseEntity<ApiResponse<Void>> clearCache(
            @Parameter(description = "缓存类型") @RequestParam(required = false) String cacheType,
            Authentication authentication) {
        
        log.info("清理系统缓存: type={}, 操作用户: {}", cacheType, authentication.getName());
        
        systemService.clearCache(cacheType, authentication.getName());
        
        return ResponseEntity.ok(ApiResponse.success("缓存清理成功"));
    }

}