package kohgylw.kiftd.server.service.impl;

import kohgylw.kiftd.server.service.SecurityAuditLoggerService;
import kohgylw.kiftd.printer.Printer;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 安全审计日志服务实现
 * 提供高性能的安全事件记录和查询功能
 * 
 * @author 企业安全专家
 * @version 2.0
 */
@Service
public class SecurityAuditLoggerServiceImpl implements SecurityAuditLoggerService {
    
    @Value("${security.audit.log.path:./logs/security/}")
    private String logPath;
    
    @Value("${security.audit.log.max.size:10485760}") // 10MB
    private long maxLogFileSize;
    
    @Value("${security.audit.log.retention.days:90}")
    private int logRetentionDays;
    
    @Value("${security.audit.log.async.enabled:true}")
    private boolean asyncLoggingEnabled;
    
    @Value("${security.audit.log.flush.interval:5}")
    private int flushIntervalSeconds;
    
    // 异步日志队列
    private final Queue<SecurityLogEntryImpl> logQueue = new ConcurrentLinkedQueue<>();
    private final AtomicLong logIdGenerator = new AtomicLong(1);
    
    // 定时任务执行器
    private ScheduledExecutorService scheduler;
    
    // 日志文件路径
    private Path currentLogFile;
    private final DateTimeFormatter fileNameFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    @PostConstruct
    public void initialize() {
        try {
            // 创建日志目录
            Path logDir = Paths.get(logPath);
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }
            
            // 初始化当前日志文件
            initializeCurrentLogFile();
            
            // 启动异步日志处理器
            if (asyncLoggingEnabled) {
                startAsyncLogProcessor();
            }
            
            // 启动定时清理任务
            startLogCleanupScheduler();
            
            logSecurityEvent(SecurityEventType.SYSTEM_STARTUP, SecuritySeverity.LOW,
                           "安全审计日志服务已启动", "日志路径: " + logPath, "localhost");
            
            Printer.instance.print("安全审计日志服务初始化完成");
            
        } catch (Exception e) {
            Printer.instance.print("安全审计日志服务初始化失败: " + e.getMessage());
            throw new RuntimeException("Failed to initialize Security Audit Logger Service", e);
        }
    }
    
    @Override
    public void logLicenseValidation(String customerId, String productId, boolean result, String clientIp, String userAgent) {
        String message = String.format("授权验证%s - 客户ID: %s, 产品ID: %s", 
                                     result ? "成功" : "失败", customerId, productId);
        String details = String.format("客户ID: %s, 产品ID: %s, 客户端IP: %s, 用户代理: %s", 
                                     customerId, productId, clientIp, userAgent);
        
        SecuritySeverity severity = result ? SecuritySeverity.LOW : SecuritySeverity.MEDIUM;
        logSecurityEvent(SecurityEventType.LICENSE_VALIDATION, severity, message, details, clientIp);
    }
    
    @Override
    public void logKeyOperation(String operation, String keyId, String operatorId, boolean result) {
        String message = String.format("密钥操作%s - 操作: %s, 密钥ID: %s", 
                                     result ? "成功" : "失败", operation, keyId);
        String details = String.format("操作: %s, 密钥ID: %s, 操作者: %s", operation, keyId, operatorId);
        
        SecuritySeverity severity = result ? SecuritySeverity.MEDIUM : SecuritySeverity.HIGH;
        
        SecurityLogEntryImpl logEntry = new SecurityLogEntryImpl(
            generateLogId(), System.currentTimeMillis(), SecurityEventType.KEY_OPERATION,
            severity, message, details, "localhost", operatorId
        );
        
        writeLogEntry(logEntry);
    }
    
    @Override
    public void logSecurityEvent(SecurityEventType eventType, SecuritySeverity severity, 
                                String message, String details, String sourceIp) {
        SecurityLogEntryImpl logEntry = new SecurityLogEntryImpl(
            generateLogId(), System.currentTimeMillis(), eventType,
            severity, message, details, sourceIp, null
        );
        
        writeLogEntry(logEntry);
    }
    
    @Override
    public void logHardwareBindingValidation(String customerId, String fingerprintHash, 
                                           boolean result, int similarityScore) {
        String message = String.format("硬件绑定验证%s - 客户ID: %s, 相似度: %d%%", 
                                     result ? "通过" : "失败", customerId, similarityScore);
        String details = String.format("客户ID: %s, 指纹哈希: %s, 相似度评分: %d%%", 
                                     customerId, maskSensitive(fingerprintHash), similarityScore);
        
        SecuritySeverity severity = result ? SecuritySeverity.LOW : 
                                   (similarityScore > 50 ? SecuritySeverity.MEDIUM : SecuritySeverity.HIGH);
        
        logSecurityEvent(SecurityEventType.HARDWARE_BINDING, severity, message, details, "localhost");
    }
    
    @Override
    public void logCryptographicOperation(String operation, String algorithm, 
                                        long dataSize, long duration, boolean result) {
        String message = String.format("加密操作%s - %s, 数据大小: %d字节, 耗时: %dms", 
                                     result ? "成功" : "失败", operation, dataSize, duration);
        String details = String.format("操作: %s, 算法: %s, 数据大小: %d, 耗时: %dms", 
                                     operation, algorithm, dataSize, duration);
        
        SecuritySeverity severity = result ? SecuritySeverity.LOW : SecuritySeverity.MEDIUM;
        logSecurityEvent(SecurityEventType.CRYPTOGRAPHIC_OPERATION, severity, message, details, "localhost");
    }
    
    @Override
    public void logAccessControl(String userId, String resource, String action, 
                               boolean result, String reason) {
        String message = String.format("访问控制%s - 用户: %s, 资源: %s, 动作: %s", 
                                     result ? "允许" : "拒绝", userId, resource, action);
        String details = String.format("用户ID: %s, 资源: %s, 动作: %s, 原因: %s", 
                                     userId, resource, action, reason != null ? reason : "N/A");
        
        SecuritySeverity severity = result ? SecuritySeverity.LOW : SecuritySeverity.MEDIUM;
        
        SecurityLogEntryImpl logEntry = new SecurityLogEntryImpl(
            generateLogId(), System.currentTimeMillis(), SecurityEventType.ACCESS_CONTROL,
            severity, message, details, "localhost", userId
        );
        
        writeLogEntry(logEntry);
    }
    
    @Override
    public void logConfigurationChange(String configKey, String oldValue, String newValue, String operatorId) {
        String message = String.format("配置变更 - 配置项: %s", configKey);
        String details = String.format("配置项: %s, 旧值: %s, 新值: %s, 操作者: %s", 
                                     configKey, maskSensitive(oldValue), maskSensitive(newValue), operatorId);
        
        SecurityLogEntryImpl logEntry = new SecurityLogEntryImpl(
            generateLogId(), System.currentTimeMillis(), SecurityEventType.CONFIGURATION_CHANGE,
            SecuritySeverity.MEDIUM, message, details, "localhost", operatorId
        );
        
        writeLogEntry(logEntry);
    }
    
    @Override
    public SecurityLogQuery querySecurityLogs(long startTime, long endTime, 
                                            SecurityEventType eventType, SecuritySeverity severity, int limit) {
        try {
            List<SecurityLogEntry> results = new ArrayList<>();
            List<Path> logFiles = getLogFilesByTimeRange(startTime, endTime);
            
            for (Path logFile : logFiles) {
                if (Files.exists(logFile)) {
                    List<String> lines = Files.readAllLines(logFile);
                    for (String line : lines) {
                        try {
                            SecurityLogEntryImpl entry = parseLogLine(line);
                            if (entry != null && 
                                entry.getTimestamp() >= startTime && 
                                entry.getTimestamp() <= endTime &&
                                (eventType == null || entry.getEventType() == eventType) &&
                                (severity == null || entry.getSeverity() == severity)) {
                                
                                results.add(entry);
                                if (results.size() >= limit) {
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            // 忽略解析失败的行
                        }
                    }
                    if (results.size() >= limit) {
                        break;
                    }
                }
            }
            
            return new SecurityLogQueryImpl(results, results.size() >= limit);
            
        } catch (Exception e) {
            Printer.instance.print("查询安全日志失败: " + e.getMessage());
            return new SecurityLogQueryImpl(Collections.emptyList(), false);
        }
    }
    
    @Override
    public String generateAuditReport(long startTime, long endTime) {
        SecurityLogQuery query = querySecurityLogs(startTime, endTime, null, null, 10000);
        List<SecurityLogEntry> entries = query.getEntries();
        
        StringBuilder report = new StringBuilder();
        report.append("安全审计报告\n");
        report.append("==================\n\n");
        
        report.append("报告时间范围: ").append(formatTimestamp(startTime))
              .append(" - ").append(formatTimestamp(endTime)).append("\n");
        report.append("总事件数: ").append(entries.size()).append("\n\n");
        
        // 按事件类型统计
        Map<SecurityEventType, Long> eventTypeStats = entries.stream()
            .collect(Collectors.groupingBy(SecurityLogEntry::getEventType, Collectors.counting()));
        
        report.append("事件类型统计:\n");
        for (Map.Entry<SecurityEventType, Long> entry : eventTypeStats.entrySet()) {
            report.append("  ").append(entry.getKey().getDescription())
                  .append(": ").append(entry.getValue()).append("\n");
        }
        
        // 按严重程度统计
        Map<SecuritySeverity, Long> severityStats = entries.stream()
            .collect(Collectors.groupingBy(SecurityLogEntry::getSeverity, Collectors.counting()));
        
        report.append("\n严重程度统计:\n");
        for (Map.Entry<SecuritySeverity, Long> entry : severityStats.entrySet()) {
            report.append("  ").append(entry.getKey().getDescription())
                  .append(": ").append(entry.getValue()).append("\n");
        }
        
        // 高风险事件
        List<SecurityLogEntry> highRiskEvents = entries.stream()
            .filter(e -> e.getSeverity() == SecuritySeverity.HIGH || e.getSeverity() == SecuritySeverity.CRITICAL)
            .limit(20)
            .collect(Collectors.toList());
        
        if (!highRiskEvents.isEmpty()) {
            report.append("\n高风险事件 (最近20条):\n");
            for (SecurityLogEntry entry : highRiskEvents) {
                report.append("  [").append(formatTimestamp(entry.getTimestamp())).append("] ")
                      .append(entry.getSeverity().getDescription()).append(" - ")
                      .append(entry.getMessage()).append("\n");
            }
        }
        
        return report.toString();
    }
    
    @Override
    public String exportSecurityLogs(long startTime, long endTime, String format) {
        SecurityLogQuery query = querySecurityLogs(startTime, endTime, null, null, Integer.MAX_VALUE);
        List<SecurityLogEntry> entries = query.getEntries();
        
        switch (format.toUpperCase()) {
            case "JSON":
                return exportAsJson(entries);
            case "CSV":
                return exportAsCsv(entries);
            case "XML":
                return exportAsXml(entries);
            default:
                throw new IllegalArgumentException("Unsupported export format: " + format);
        }
    }
    
    @Override
    public int cleanupExpiredLogs(int retentionDays) {
        try {
            long cutoffTime = System.currentTimeMillis() - (retentionDays * 24L * 60 * 60 * 1000);
            Path logDir = Paths.get(logPath);
            
            int deletedCount = 0;
            if (Files.exists(logDir)) {
                List<Path> logFiles = Files.list(logDir)
                    .filter(path -> path.toString().endsWith(".log"))
                    .collect(Collectors.toList());
                
                for (Path logFile : logFiles) {
                    try {
                        if (Files.getLastModifiedTime(logFile).toMillis() < cutoffTime) {
                            Files.delete(logFile);
                            deletedCount++;
                        }
                    } catch (Exception e) {
                        Printer.instance.print("删除过期日志文件失败: " + logFile + ", " + e.getMessage());
                    }
                }
            }
            
            logSecurityEvent(SecurityEventType.SYSTEM_STARTUP, SecuritySeverity.LOW,
                           "清理过期日志完成", "删除文件数: " + deletedCount, "localhost");
            
            return deletedCount;
            
        } catch (Exception e) {
            Printer.instance.print("清理过期日志失败: " + e.getMessage());
            return 0;
        }
    }
    
    // ==================== 私有方法 ====================
    
    private void initializeCurrentLogFile() throws IOException {
        String fileName = "security-" + LocalDateTime.now().format(fileNameFormatter) + ".log";
        currentLogFile = Paths.get(logPath, fileName);
        
        if (!Files.exists(currentLogFile)) {
            Files.createFile(currentLogFile);
        }
    }
    
    private void startAsyncLogProcessor() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Security-Audit-Logger");
            t.setDaemon(true);
            return t;
        });
        
        scheduler.scheduleAtFixedRate(this::processLogQueue, 
                                    flushIntervalSeconds, flushIntervalSeconds, TimeUnit.SECONDS);
    }
    
    private void startLogCleanupScheduler() {
        if (scheduler == null) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "Security-Log-Cleanup");
                t.setDaemon(true);
                return t;
            });
        }
        
        // 每天凌晨2点执行清理任务
        long initialDelay = calculateInitialDelayForCleanup();
        scheduler.scheduleAtFixedRate(() -> cleanupExpiredLogs(logRetentionDays), 
                                    initialDelay, 24 * 60 * 60, TimeUnit.SECONDS);
    }
    
    private void writeLogEntry(SecurityLogEntryImpl logEntry) {
        if (asyncLoggingEnabled) {
            logQueue.offer(logEntry);
        } else {
            writeLogEntrySync(logEntry);
        }
    }
    
    private void processLogQueue() {
        List<SecurityLogEntryImpl> entries = new ArrayList<>();
        SecurityLogEntryImpl entry;
        
        while ((entry = logQueue.poll()) != null) {
            entries.add(entry);
        }
        
        if (!entries.isEmpty()) {
            writeLogEntriesSync(entries);
        }
    }
    
    private void writeLogEntrySync(SecurityLogEntryImpl logEntry) {
        writeLogEntriesSync(Collections.singletonList(logEntry));
    }
    
    private void writeLogEntriesSync(List<SecurityLogEntryImpl> entries) {
        try {
            // 检查是否需要轮换日志文件
            checkAndRotateLogFile();
            
            List<String> lines = entries.stream()
                .map(this::formatLogEntry)
                .collect(Collectors.toList());
            
            Files.write(currentLogFile, lines, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            
        } catch (Exception e) {
            Printer.instance.print("写入安全日志失败: " + e.getMessage());
        }
    }
    
    private void checkAndRotateLogFile() throws IOException {
        if (Files.exists(currentLogFile) && Files.size(currentLogFile) > maxLogFileSize) {
            // 重命名当前文件
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
            String rotatedFileName = currentLogFile.getFileName().toString().replace(".log", "_" + timestamp + ".log");
            Path rotatedFile = currentLogFile.getParent().resolve(rotatedFileName);
            Files.move(currentLogFile, rotatedFile);
            
            // 创建新的日志文件
            initializeCurrentLogFile();
        }
    }
    
    private String formatLogEntry(SecurityLogEntryImpl entry) {
        return String.format("[%s] [%s] [%s] [%s] [%s] %s | %s",
                           formatTimestamp(entry.getTimestamp()),
                           entry.getId(),
                           entry.getEventType().name(),
                           entry.getSeverity().name(),
                           entry.getSourceIp(),
                           entry.getMessage(),
                           entry.getDetails());
    }
    
    private SecurityLogEntryImpl parseLogLine(String line) {
        // 简化的日志解析实现
        // 实际应用中应该使用更严格的解析逻辑
        if (line.startsWith("[") && line.contains("]")) {
            try {
                String[] parts = line.split("\\] \\[");
                if (parts.length >= 6) {
                    String timestampStr = parts[0].substring(1);
                    long timestamp = parseTimestamp(timestampStr);
                    String id = parts[1];
                    SecurityEventType eventType = SecurityEventType.valueOf(parts[2]);
                    SecuritySeverity severity = SecuritySeverity.valueOf(parts[3]);
                    String sourceIp = parts[4];
                    
                    String remaining = parts[5];
                    String[] messageParts = remaining.split(" \\| ", 2);
                    String message = messageParts[0];
                    String details = messageParts.length > 1 ? messageParts[1] : "";
                    
                    return new SecurityLogEntryImpl(id, timestamp, eventType, severity, 
                                                  message, details, sourceIp, null);
                }
            } catch (Exception e) {
                // 解析失败，返回null
            }
        }
        return null;
    }
    
    private String generateLogId() {
        return "LOG-" + logIdGenerator.getAndIncrement();
    }
    
    private String formatTimestamp(long timestamp) {
        return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(timestamp), 
                                     java.time.ZoneId.systemDefault())
                           .format(timestampFormatter);
    }
    
    private long parseTimestamp(String timestampStr) {
        return LocalDateTime.parse(timestampStr, timestampFormatter)
                           .atZone(java.time.ZoneId.systemDefault())
                           .toInstant()
                           .toEpochMilli();
    }
    
    private String maskSensitive(String value) {
        if (value == null || value.length() < 8) {
            return "***";
        }
        return value.substring(0, 4) + "***" + value.substring(value.length() - 4);
    }
    
    private List<Path> getLogFilesByTimeRange(long startTime, long endTime) {
        // 简化实现：返回所有日志文件
        // 实际应用中应该根据时间范围智能筛选文件
        try {
            Path logDir = Paths.get(logPath);
            if (Files.exists(logDir)) {
                return Files.list(logDir)
                    .filter(path -> path.toString().endsWith(".log"))
                    .sorted()
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            Printer.instance.print("获取日志文件列表失败: " + e.getMessage());
        }
        return Collections.emptyList();
    }
    
    private long calculateInitialDelayForCleanup() {
        // 计算到下一个凌晨2点的秒数
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextCleanup = now.toLocalDate().atTime(2, 0);
        if (now.isAfter(nextCleanup)) {
            nextCleanup = nextCleanup.plusDays(1);
        }
        return java.time.Duration.between(now, nextCleanup).getSeconds();
    }
    
    private String exportAsJson(List<SecurityLogEntry> entries) {
        StringBuilder json = new StringBuilder();
        json.append("[\n");
        for (int i = 0; i < entries.size(); i++) {
            json.append(entries.get(i).toJson());
            if (i < entries.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("]");
        return json.toString();
    }
    
    private String exportAsCsv(List<SecurityLogEntry> entries) {
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Timestamp,EventType,Severity,Message,Details,SourceIP,UserID\n");
        
        for (SecurityLogEntry entry : entries) {
            csv.append(csvEscape(entry.getId())).append(",")
               .append(formatTimestamp(entry.getTimestamp())).append(",")
               .append(entry.getEventType().name()).append(",")
               .append(entry.getSeverity().name()).append(",")
               .append(csvEscape(entry.getMessage())).append(",")
               .append(csvEscape(entry.getDetails())).append(",")
               .append(csvEscape(entry.getSourceIp())).append(",")
               .append(csvEscape(entry.getUserId())).append("\n");
        }
        
        return csv.toString();
    }
    
    private String exportAsXml(List<SecurityLogEntry> entries) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<SecurityLogs>\n");
        
        for (SecurityLogEntry entry : entries) {
            xml.append("  <LogEntry>\n")
               .append("    <ID>").append(xmlEscape(entry.getId())).append("</ID>\n")
               .append("    <Timestamp>").append(formatTimestamp(entry.getTimestamp())).append("</Timestamp>\n")
               .append("    <EventType>").append(entry.getEventType().name()).append("</EventType>\n")
               .append("    <Severity>").append(entry.getSeverity().name()).append("</Severity>\n")
               .append("    <Message>").append(xmlEscape(entry.getMessage())).append("</Message>\n")
               .append("    <Details>").append(xmlEscape(entry.getDetails())).append("</Details>\n")
               .append("    <SourceIP>").append(xmlEscape(entry.getSourceIp())).append("</SourceIP>\n")
               .append("    <UserID>").append(xmlEscape(entry.getUserId())).append("</UserID>\n")
               .append("  </LogEntry>\n");
        }
        
        xml.append("</SecurityLogs>");
        return xml.toString();
    }
    
    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
    
    private String xmlEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
    
    // ==================== 内部类 ====================
    
    private static class SecurityLogEntryImpl implements SecurityLogEntry {
        private final String id;
        private final long timestamp;
        private final SecurityEventType eventType;
        private final SecuritySeverity severity;
        private final String message;
        private final String details;
        private final String sourceIp;
        private final String userId;
        
        public SecurityLogEntryImpl(String id, long timestamp, SecurityEventType eventType,
                                  SecuritySeverity severity, String message, String details,
                                  String sourceIp, String userId) {
            this.id = id;
            this.timestamp = timestamp;
            this.eventType = eventType;
            this.severity = severity;
            this.message = message;
            this.details = details;
            this.sourceIp = sourceIp;
            this.userId = userId;
        }
        
        @Override
        public String getId() {
            return id;
        }
        
        @Override
        public long getTimestamp() {
            return timestamp;
        }
        
        @Override
        public SecurityEventType getEventType() {
            return eventType;
        }
        
        @Override
        public SecuritySeverity getSeverity() {
            return severity;
        }
        
        @Override
        public String getMessage() {
            return message;
        }
        
        @Override
        public String getDetails() {
            return details;
        }
        
        @Override
        public String getSourceIp() {
            return sourceIp;
        }
        
        @Override
        public String getUserId() {
            return userId;
        }
        
        @Override
        public String toJson() {
            return String.format(
                "{\"id\":\"%s\",\"timestamp\":%d,\"eventType\":\"%s\",\"severity\":\"%s\"," +
                "\"message\":\"%s\",\"details\":\"%s\",\"sourceIp\":\"%s\",\"userId\":\"%s\"}",
                id, timestamp, eventType.name(), severity.name(),
                jsonEscape(message), jsonEscape(details), sourceIp, userId != null ? userId : ""
            );
        }
        
        private String jsonEscape(String value) {
            if (value == null) {
                return "";
            }
            return value.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t");
        }
    }
    
    private static class SecurityLogQueryImpl implements SecurityLogQuery {
        private final List<SecurityLogEntry> entries;
        private final boolean hasMore;
        
        public SecurityLogQueryImpl(List<SecurityLogEntry> entries, boolean hasMore) {
            this.entries = entries;
            this.hasMore = hasMore;
        }
        
        @Override
        public int getTotalCount() {
            return entries.size();
        }
        
        @Override
        public List<SecurityLogEntry> getEntries() {
            return entries;
        }
        
        @Override
        public boolean hasMore() {
            return hasMore;
        }
    }
}