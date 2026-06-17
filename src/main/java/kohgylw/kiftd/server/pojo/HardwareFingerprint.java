package kohgylw.kiftd.server.pojo;

import java.util.Objects;

/**
 * 硬件指纹数据结构
 * 用于硬件绑定验证
 * 
 * @author 企业安全专家
 * @version 2.0
 */
public class HardwareFingerprint {
    
    /** CPU序列号 */
    private String cpuId;
    
    /** 主板序列号 */
    private String motherboardSerial;
    
    /** 硬盘序列号 */
    private String diskSerial;
    
    /** MAC地址 */
    private String macAddress;
    
    /** 操作系统信息 */
    private String osInfo;
    
    /** 机器名称 */
    private String machineName;
    
    /** 用户名 */
    private String userName;
    
    /** 硬件指纹哈希值 */
    private String fingerprintHash;
    
    /** 绑定策略 */
    private BindingPolicy bindingPolicy;
    
    // 构造函数
    public HardwareFingerprint() {}
    
    public HardwareFingerprint(String cpuId, String motherboardSerial, String diskSerial, 
                              String macAddress, BindingPolicy bindingPolicy) {
        this.cpuId = cpuId;
        this.motherboardSerial = motherboardSerial;
        this.diskSerial = diskSerial;
        this.macAddress = macAddress;
        this.bindingPolicy = bindingPolicy;
        this.fingerprintHash = calculateFingerprintHash();
    }
    
    // Getters and Setters
    public String getCpuId() {
        return cpuId;
    }
    
    public void setCpuId(String cpuId) {
        this.cpuId = cpuId;
        this.fingerprintHash = calculateFingerprintHash();
    }
    
    public String getMotherboardSerial() {
        return motherboardSerial;
    }
    
    public void setMotherboardSerial(String motherboardSerial) {
        this.motherboardSerial = motherboardSerial;
        this.fingerprintHash = calculateFingerprintHash();
    }
    
    public String getDiskSerial() {
        return diskSerial;
    }
    
    public void setDiskSerial(String diskSerial) {
        this.diskSerial = diskSerial;
        this.fingerprintHash = calculateFingerprintHash();
    }
    
    public String getMacAddress() {
        return macAddress;
    }
    
    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
        this.fingerprintHash = calculateFingerprintHash();
    }
    
    public String getOsInfo() {
        return osInfo;
    }
    
    public void setOsInfo(String osInfo) {
        this.osInfo = osInfo;
    }
    
    public String getMachineName() {
        return machineName;
    }
    
    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getFingerprintHash() {
        return fingerprintHash;
    }
    
    public void setFingerprintHash(String fingerprintHash) {
        this.fingerprintHash = fingerprintHash;
    }
    
    public BindingPolicy getBindingPolicy() {
        return bindingPolicy;
    }
    
    public void setBindingPolicy(BindingPolicy bindingPolicy) {
        this.bindingPolicy = bindingPolicy;
    }
    
    // 业务方法
    
    /**
     * 计算硬件指纹哈希值
     */
    private String calculateFingerprintHash() {
        StringBuilder sb = new StringBuilder();
        
        if (bindingPolicy != null) {
            switch (bindingPolicy) {
                case STRICT:
                    // 严格模式：所有硬件信息都参与计算
                    sb.append(cpuId != null ? cpuId : "")
                      .append(motherboardSerial != null ? motherboardSerial : "")
                      .append(diskSerial != null ? diskSerial : "")
                      .append(macAddress != null ? macAddress : "");
                    break;
                case MODERATE:
                    // 中等模式：CPU + 主板 或 CPU + 硬盘
                    sb.append(cpuId != null ? cpuId : "")
                      .append(motherboardSerial != null ? motherboardSerial : "")
                      .append(diskSerial != null ? diskSerial : "");
                    break;
                case LOOSE:
                    // 宽松模式：仅CPU或MAC地址
                    sb.append(cpuId != null ? cpuId : "")
                      .append(macAddress != null ? macAddress : "");
                    break;
                case NONE:
                    // 无绑定
                    return "NO_BINDING";
            }
        }
        
        String data = sb.toString();
        if (data.isEmpty()) {
            return "EMPTY_FINGERPRINT";
        }
        
        // 使用简单的哈希计算（实际应用中应使用SHA-256等）
        return String.valueOf(data.hashCode());
    }
    
    /**
     * 验证当前硬件指纹是否与目标指纹匹配
     */
    public boolean matches(HardwareFingerprint target) {
        if (target == null || bindingPolicy == BindingPolicy.NONE) {
            return true;
        }
        
        if (fingerprintHash == null || target.fingerprintHash == null) {
            return false;
        }
        
        return fingerprintHash.equals(target.fingerprintHash);
    }
    
    /**
     * 计算与目标指纹的相似度评分 (0-100)
     */
    public int calculateSimilarityScore(HardwareFingerprint target) {
        if (target == null) {
            return 0;
        }
        
        int totalChecks = 0;
        int matches = 0;
        
        // 检查CPU ID
        if (cpuId != null && target.cpuId != null) {
            totalChecks++;
            if (cpuId.equals(target.cpuId)) {
                matches++;
            }
        }
        
        // 检查主板序列号
        if (motherboardSerial != null && target.motherboardSerial != null) {
            totalChecks++;
            if (motherboardSerial.equals(target.motherboardSerial)) {
                matches++;
            }
        }
        
        // 检查硬盘序列号
        if (diskSerial != null && target.diskSerial != null) {
            totalChecks++;
            if (diskSerial.equals(target.diskSerial)) {
                matches++;
            }
        }
        
        // 检查MAC地址
        if (macAddress != null && target.macAddress != null) {
            totalChecks++;
            if (macAddress.equals(target.macAddress)) {
                matches++;
            }
        }
        
        return totalChecks > 0 ? (matches * 100 / totalChecks) : 0;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HardwareFingerprint that = (HardwareFingerprint) o;
        return Objects.equals(fingerprintHash, that.fingerprintHash);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(fingerprintHash);
    }
    
    @Override
    public String toString() {
        return "HardwareFingerprint{" +
                "cpuId='" + maskSensitive(cpuId) + '\'' +
                ", motherboardSerial='" + maskSensitive(motherboardSerial) + '\'' +
                ", diskSerial='" + maskSensitive(diskSerial) + '\'' +
                ", macAddress='" + maskSensitive(macAddress) + '\'' +
                ", bindingPolicy=" + bindingPolicy +
                ", fingerprintHash='" + fingerprintHash + '\'' +
                '}';
    }
    
    /**
     * 掩码敏感信息用于日志输出
     */
    private String maskSensitive(String value) {
        if (value == null || value.length() < 4) {
            return "***";
        }
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }
    
    /**
     * 硬件绑定策略枚举
     */
    public enum BindingPolicy {
        /** 无绑定 */
        NONE("none", "无硬件绑定"),
        /** 宽松绑定 - 仅验证关键硬件 */
        LOOSE("loose", "宽松绑定"),
        /** 中等绑定 - 验证多项硬件信息 */
        MODERATE("moderate", "中等绑定"),
        /** 严格绑定 - 验证所有硬件信息 */
        STRICT("strict", "严格绑定");
        
        private final String code;
        private final String description;
        
        BindingPolicy(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static BindingPolicy fromCode(String code) {
            for (BindingPolicy policy : values()) {
                if (policy.code.equals(code)) {
                    return policy;
                }
            }
            throw new IllegalArgumentException("Unknown binding policy code: " + code);
        }
    }
}