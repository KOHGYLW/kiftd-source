package com.enterprise.license.service;

import com.enterprise.license.dto.LicenseDto;
import com.enterprise.license.entity.License;
import com.enterprise.license.entity.LicenseValidationLog;
import com.enterprise.license.repository.LicenseRepository;
import com.enterprise.license.repository.LicenseValidationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 授权验证服务
 */
@Slf4j
@Service
@Validated
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class LicenseValidationService extends BaseService {

    private final LicenseRepository licenseRepository;
    private final LicenseValidationLogRepository validationLogRepository;

    /**
     * 验证授权请求对象
     */
    public static class ValidationRequest {
        private String licenseCode;
        private String machineFingerprint;
        private String clientIp;
        private String userAgent;
        private Map<String, Object> additionalData;

        // Constructors, getters, setters
        public ValidationRequest() {}

        public ValidationRequest(String licenseCode, String machineFingerprint, String clientIp) {
            this.licenseCode = licenseCode;
            this.machineFingerprint = machineFingerprint;
            this.clientIp = clientIp;
        }

        // Getters and setters
        public String getLicenseCode() { return licenseCode; }
        public void setLicenseCode(String licenseCode) { this.licenseCode = licenseCode; }
        public String getMachineFingerprint() { return machineFingerprint; }
        public void setMachineFingerprint(String machineFingerprint) { this.machineFingerprint = machineFingerprint; }
        public String getClientIp() { return clientIp; }
        public void setClientIp(String clientIp) { this.clientIp = clientIp; }
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        public Map<String, Object> getAdditionalData() { return additionalData; }
        public void setAdditionalData(Map<String, Object> additionalData) { this.additionalData = additionalData; }
    }

    /**
     * 验证授权响应对象
     */
    public static class ValidationResponse {
        private boolean valid;
        private String message;
        private LicenseValidationLog.ValidationStatus status;
        private LicenseDto license;
        private Map<String, Object> validationData;
        private LocalDateTime validationTime;

        // Constructors, getters, setters
        public ValidationResponse() {
            this.validationTime = LocalDateTime.now();
        }

        public ValidationResponse(boolean valid, String message, LicenseValidationLog.ValidationStatus status) {
            this();
            this.valid = valid;
            this.message = message;
            this.status = status;
        }

        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public LicenseValidationLog.ValidationStatus getStatus() { return status; }
        public void setStatus(LicenseValidationLog.ValidationStatus status) { this.status = status; }
        public LicenseDto getLicense() { return license; }
        public void setLicense(LicenseDto license) { this.license = license; }
        public Map<String, Object> getValidationData() { return validationData; }
        public void setValidationData(Map<String, Object> validationData) { this.validationData = validationData; }
        public LocalDateTime getValidationTime() { return validationTime; }
        public void setValidationTime(LocalDateTime validationTime) { this.validationTime = validationTime; }
    }

    /**
     * 验证授权
     */
    public ValidationResponse validateLicense(ValidationRequest request) {
        log.info("开始验证授权: {}", request.getLicenseCode());

        ValidationResponse response = new ValidationResponse();
        LicenseValidationLog validationLog = createValidationLog(request);

        try {
            // 基础参数验证
            if (StringUtils.isBlank(request.getLicenseCode())) {
                return createFailedResponse(response, validationLog, "授权编码不能为空", 
                        LicenseValidationLog.ValidationStatus.INVALID);
            }

            if (StringUtils.isBlank(request.getClientIp())) {
                return createFailedResponse(response, validationLog, "客户端IP不能为空", 
                        LicenseValidationLog.ValidationStatus.INVALID);
            }

            // 查找授权
            Optional<License> licenseOpt = licenseRepository.findByLicenseCodeAndDeletedFalse(request.getLicenseCode());
            if (!licenseOpt.isPresent()) {
                return createFailedResponse(response, validationLog, "授权不存在", 
                        LicenseValidationLog.ValidationStatus.INVALID);
            }

            License license = licenseOpt.get();
            validationLog.setLicenseId(license.getId());

            // 验证授权状态
            ValidationResponse statusValidation = validateLicenseStatus(license);
            if (!statusValidation.isValid()) {
                return createFailedResponse(response, validationLog, statusValidation.getMessage(), 
                        statusValidation.getStatus());
            }

            // 验证授权时间
            ValidationResponse timeValidation = validateLicenseTime(license);
            if (!timeValidation.isValid()) {
                return createFailedResponse(response, validationLog, timeValidation.getMessage(), 
                        timeValidation.getStatus());
            }

            // 验证机器指纹（如果设置了）
            if (StringUtils.isNotBlank(license.getMachineFingerprint()) && 
                StringUtils.isNotBlank(request.getMachineFingerprint())) {
                ValidationResponse fingerprintValidation = validateMachineFingerprint(license, request.getMachineFingerprint());
                if (!fingerprintValidation.isValid()) {
                    return createFailedResponse(response, validationLog, fingerprintValidation.getMessage(), 
                            fingerprintValidation.getStatus());
                }
            }

            // 验证成功
            response = createSuccessResponse(license, request);
            validationLog.setValidationStatus(LicenseValidationLog.ValidationStatus.SUCCESS);
            validationLog.setValidationResult("验证成功");

            // 更新授权验证信息
            updateLicenseValidationInfo(license.getId());

            logOperation("授权验证成功", "授权编码: " + request.getLicenseCode());

        } catch (Exception e) {
            log.error("授权验证异常: {}", e.getMessage(), e);
            response = createFailedResponse(response, validationLog, "系统错误: " + e.getMessage(), 
                    LicenseValidationLog.ValidationStatus.ERROR);
        } finally {
            // 保存验证日志
            saveValidationLog(validationLog, request, response);
        }

        return response;
    }

    /**
     * 批量验证授权
     */
    public Map<String, ValidationResponse> batchValidateLicenses(List<ValidationRequest> requests) {
        log.info("批量验证授权，数量: {}", requests.size());

        Map<String, ValidationResponse> results = new HashMap<>();
        
        for (ValidationRequest request : requests) {
            try {
                ValidationResponse response = validateLicense(request);
                results.put(request.getLicenseCode(), response);
            } catch (Exception e) {
                log.error("批量验证授权异常: {}", e.getMessage(), e);
                ValidationResponse errorResponse = new ValidationResponse(false, 
                        "验证异常: " + e.getMessage(), LicenseValidationLog.ValidationStatus.ERROR);
                results.put(request.getLicenseCode(), errorResponse);
            }
        }

        return results;
    }

    /**
     * 获取授权验证日志
     */
    public Page<LicenseValidationLog> getValidationLogs(Long licenseId, int page, int size) {
        log.debug("获取授权验证日志: {}", licenseId);

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "validationTime"));
        return validationLogRepository.findByLicenseIdAndDeletedFalse(licenseId, pageable);
    }

    /**
     * 获取失败的验证记录
     */
    public List<LicenseValidationLog> getFailedValidations(LocalDateTime since) {
        log.debug("获取失败的验证记录: {}", since);

        return validationLogRepository.findFailedValidationsSince(since);
    }

    /**
     * 获取验证统计信息
     */
    public Map<String, Long> getValidationStatistics() {
        log.debug("获取验证统计信息");

        List<Object[]> results = validationLogRepository.countByValidationStatus();
        Map<String, Long> statistics = new HashMap<>();
        
        for (Object[] result : results) {
            LicenseValidationLog.ValidationStatus status = (LicenseValidationLog.ValidationStatus) result[0];
            Long count = (Long) result[1];
            statistics.put(status.name(), count);
        }

        return statistics;
    }

    /**
     * 获取每日验证统计
     */
    public Map<String, Long> getDailyValidationStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("获取每日验证统计: {} - {}", startTime, endTime);

        List<Object[]> results = validationLogRepository.countDailyValidations(startTime, endTime);
        Map<String, Long> statistics = new HashMap<>();
        
        for (Object[] result : results) {
            String date = result[0].toString();
            Long count = (Long) result[1];
            statistics.put(date, count);
        }

        return statistics;
    }

    /**
     * 获取IP访问统计
     */
    public Map<String, Long> getIpAccessStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("获取IP访问统计: {} - {}", startTime, endTime);

        List<Object[]> results = validationLogRepository.countByClientIp(startTime, endTime);
        Map<String, Long> statistics = new HashMap<>();
        
        for (Object[] result : results) {
            String ip = (String) result[0];
            Long count = (Long) result[1];
            statistics.put(ip, count);
        }

        return statistics;
    }

    /**
     * 清理过期的验证日志
     */
    @Transactional
    public int cleanupExpiredLogs(LocalDateTime beforeTime) {
        log.info("清理过期的验证日志: {}", beforeTime);

        int deletedCount = validationLogRepository.deleteByValidationTimeBefore(beforeTime);
        logOperation("清理验证日志", "删除数量: " + deletedCount);

        return deletedCount;
    }

    /**
     * 验证授权状态
     */
    private ValidationResponse validateLicenseStatus(License license) {
        switch (license.getStatus()) {
            case ACTIVE:
                return new ValidationResponse(true, "授权状态正常", LicenseValidationLog.ValidationStatus.SUCCESS);
            case INACTIVE:
                return new ValidationResponse(false, "授权未激活", LicenseValidationLog.ValidationStatus.INVALID);
            case SUSPENDED:
                return new ValidationResponse(false, "授权已暂停", LicenseValidationLog.ValidationStatus.SUSPENDED);
            case EXPIRED:
                return new ValidationResponse(false, "授权已过期", LicenseValidationLog.ValidationStatus.EXPIRED);
            case REVOKED:
                return new ValidationResponse(false, "授权已撤销", LicenseValidationLog.ValidationStatus.REVOKED);
            default:
                return new ValidationResponse(false, "授权状态异常", LicenseValidationLog.ValidationStatus.INVALID);
        }
    }

    /**
     * 验证授权时间
     */
    private ValidationResponse validateLicenseTime(License license) {
        LocalDateTime now = getCurrentTime();
        
        if (license.getStartTime().isAfter(now)) {
            return new ValidationResponse(false, "授权尚未生效", LicenseValidationLog.ValidationStatus.INVALID);
        }
        
        if (license.getExpireTime().isBefore(now)) {
            return new ValidationResponse(false, "授权已过期", LicenseValidationLog.ValidationStatus.EXPIRED);
        }
        
        return new ValidationResponse(true, "授权时间有效", LicenseValidationLog.ValidationStatus.SUCCESS);
    }

    /**
     * 验证机器指纹
     */
    private ValidationResponse validateMachineFingerprint(License license, String requestFingerprint) {
        if (!license.getMachineFingerprint().equals(requestFingerprint)) {
            return new ValidationResponse(false, "机器指纹不匹配", LicenseValidationLog.ValidationStatus.INVALID);
        }
        
        return new ValidationResponse(true, "机器指纹验证通过", LicenseValidationLog.ValidationStatus.SUCCESS);
    }

    /**
     * 创建成功响应
     */
    private ValidationResponse createSuccessResponse(License license, ValidationRequest request) {
        ValidationResponse response = new ValidationResponse(true, "验证成功", LicenseValidationLog.ValidationStatus.SUCCESS);
        
        // 设置授权信息
        LicenseDto licenseDto = convertLicenseToDto(license);
        response.setLicense(licenseDto);
        
        // 设置验证数据
        Map<String, Object> validationData = new HashMap<>();
        validationData.put("licenseCode", license.getLicenseCode());
        validationData.put("productName", license.getProductName());
        validationData.put("productVersion", license.getProductVersion());
        validationData.put("licenseType", license.getLicenseType().name());
        validationData.put("maxUsers", license.getMaxUsers());
        validationData.put("expireTime", license.getExpireTime());
        validationData.put("features", license.getFeatures());
        validationData.put("validationHash", generateValidationHash(license, request));
        response.setValidationData(validationData);
        
        return response;
    }

    /**
     * 创建失败响应
     */
    private ValidationResponse createFailedResponse(ValidationResponse response, LicenseValidationLog validationLog, 
                                                   String message, LicenseValidationLog.ValidationStatus status) {
        response.setValid(false);
        response.setMessage(message);
        response.setStatus(status);
        
        validationLog.setValidationStatus(status);
        validationLog.setValidationResult("验证失败");
        validationLog.setErrorMessage(message);
        
        return response;
    }

    /**
     * 创建验证日志
     */
    private LicenseValidationLog createValidationLog(ValidationRequest request) {
        LicenseValidationLog log = new LicenseValidationLog();
        log.setClientIp(request.getClientIp());
        log.setMachineFingerprint(request.getMachineFingerprint());
        log.setValidationTime(getCurrentTime());
        log.setUserAgent(request.getUserAgent());
        
        if (request.getAdditionalData() != null) {
            log.setRequestData(request.getAdditionalData().toString());
        }
        
        return log;
    }

    /**
     * 保存验证日志
     */
    private void saveValidationLog(LicenseValidationLog validationLog, ValidationRequest request, ValidationResponse response) {
        try {
            if (response.getValidationData() != null) {
                validationLog.setResponseData(response.getValidationData().toString());
            }
            
            // 设置审计字段
            validationLog.setCreatedTime(getCurrentTime());
            validationLog.setCreatedBy("system");
            validationLog.setDeleted(false);
            
            validationLogRepository.save(validationLog);
        } catch (Exception e) {
            log.error("保存验证日志失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 更新授权验证信息
     */
    private void updateLicenseValidationInfo(Long licenseId) {
        try {
            licenseRepository.updateValidationInfo(licenseId, getCurrentTime());
        } catch (Exception e) {
            log.error("更新授权验证信息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 生成验证哈希值
     */
    private String generateValidationHash(License license, ValidationRequest request) {
        String data = license.getLicenseCode() + license.getCustomerId() + 
                     request.getClientIp() + request.getMachineFingerprint() + 
                     System.currentTimeMillis();
        return DigestUtils.sha256Hex(data);
    }

    /**
     * 授权实体转DTO
     */
    private LicenseDto convertLicenseToDto(License license) {
        LicenseDto dto = new LicenseDto();
        dto.setId(license.getId());
        dto.setLicenseCode(license.getLicenseCode());
        dto.setCustomerId(license.getCustomerId());
        dto.setProductName(license.getProductName());
        dto.setProductVersion(license.getProductVersion());
        dto.setLicenseType(license.getLicenseType());
        dto.setStatus(license.getStatus());
        dto.setStartTime(license.getStartTime());
        dto.setExpireTime(license.getExpireTime());
        dto.setMaxUsers(license.getMaxUsers());
        dto.setFeatures(license.getFeatures());
        dto.setPrice(license.getPrice());
        dto.setLastValidationTime(license.getLastValidationTime());
        dto.setValidationCount(license.getValidationCount());
        return dto;
    }
}