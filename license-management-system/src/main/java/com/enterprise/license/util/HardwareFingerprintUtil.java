package com.enterprise.license.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 硬件指纹工具类
 * 用于生成和验证硬件指纹，确保授权与特定设备绑定
 */
@Component
public class HardwareFingerprintUtil {

    @Autowired
    private HashUtil hashUtil;

    /**
     * 硬件指纹组件
     */
    public static class HardwareInfo {
        private String cpuId;
        private String motherboardSerial;
        private String macAddress;
        private String diskSerial;
        private String systemUuid;
        private String hostname;
        private String osInfo;

        // Getters and Setters
        public String getCpuId() { return cpuId; }
        public void setCpuId(String cpuId) { this.cpuId = cpuId; }
        
        public String getMotherboardSerial() { return motherboardSerial; }
        public void setMotherboardSerial(String motherboardSerial) { this.motherboardSerial = motherboardSerial; }
        
        public String getMacAddress() { return macAddress; }
        public void setMacAddress(String macAddress) { this.macAddress = macAddress; }
        
        public String getDiskSerial() { return diskSerial; }
        public void setDiskSerial(String diskSerial) { this.diskSerial = diskSerial; }
        
        public String getSystemUuid() { return systemUuid; }
        public void setSystemUuid(String systemUuid) { this.systemUuid = systemUuid; }
        
        public String getHostname() { return hostname; }
        public void setHostname(String hostname) { this.hostname = hostname; }
        
        public String getOsInfo() { return osInfo; }
        public void setOsInfo(String osInfo) { this.osInfo = osInfo; }
    }

    /**
     * 获取当前系统的硬件信息
     */
    public HardwareInfo getHardwareInfo() {
        HardwareInfo info = new HardwareInfo();
        
        try {
            info.setCpuId(getCpuId());
            info.setMotherboardSerial(getMotherboardSerial());
            info.setMacAddress(getMacAddress());
            info.setDiskSerial(getDiskSerial());
            info.setSystemUuid(getSystemUuid());
            info.setHostname(getHostname());
            info.setOsInfo(getOsInfo());
        } catch (Exception e) {
            // 记录错误但不抛出异常，确保指纹生成的稳定性
            System.err.println("获取硬件信息时发生错误: " + e.getMessage());
        }
        
        return info;
    }

    /**
     * 生成硬件指纹
     */
    public String generateHardwareFingerprint() {
        HardwareInfo info = getHardwareInfo();
        return generateFingerprint(info);
    }

    /**
     * 根据硬件信息生成指纹
     */
    public String generateFingerprint(HardwareInfo info) {
        StringBuilder sb = new StringBuilder();
        
        // 按固定顺序组合硬件信息，确保一致性
        appendIfNotNull(sb, info.getCpuId());
        appendIfNotNull(sb, info.getMotherboardSerial());
        appendIfNotNull(sb, info.getMacAddress());
        appendIfNotNull(sb, info.getDiskSerial());
        appendIfNotNull(sb, info.getSystemUuid());
        appendIfNotNull(sb, info.getHostname());
        appendIfNotNull(sb, info.getOsInfo());
        
        // 生成SHA-256哈希
        return hashUtil.sha256(sb.toString());
    }

    /**
     * 生成轻量级指纹（仅使用关键硬件信息）
     */
    public String generateLightweightFingerprint() {
        HardwareInfo info = getHardwareInfo();
        StringBuilder sb = new StringBuilder();
        
        // 仅使用最稳定的硬件信息
        appendIfNotNull(sb, info.getCpuId());
        appendIfNotNull(sb, info.getMacAddress());
        appendIfNotNull(sb, info.getDiskSerial());
        
        return hashUtil.sha256(sb.toString());
    }

    /**
     * 验证硬件指纹
     */
    public boolean verifyHardwareFingerprint(String expectedFingerprint) {
        String currentFingerprint = generateHardwareFingerprint();
        return currentFingerprint.equals(expectedFingerprint);
    }

    /**
     * 计算指纹相似度（用于硬件变更检测）
     */
    public double calculateSimilarity(String fingerprint1, String fingerprint2) {
        if (fingerprint1 == null || fingerprint2 == null) {
            return 0.0;
        }
        
        if (fingerprint1.equals(fingerprint2)) {
            return 1.0;
        }
        
        // 简单的字符相似度计算
        int commonChars = 0;
        int minLength = Math.min(fingerprint1.length(), fingerprint2.length());
        
        for (int i = 0; i < minLength; i++) {
            if (fingerprint1.charAt(i) == fingerprint2.charAt(i)) {
                commonChars++;
            }
        }
        
        return (double) commonChars / Math.max(fingerprint1.length(), fingerprint2.length());
    }

    /**
     * 获取CPU ID
     */
    private String getCpuId() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("windows")) {
                return executeCommand("wmic cpu get ProcessorId").trim();
            } else if (os.contains("linux")) {
                String result = executeCommand("cat /proc/cpuinfo | grep 'processor' | wc -l");
                String cpuModel = executeCommand("cat /proc/cpuinfo | grep 'model name' | head -1");
                return result + "_" + cpuModel;
            } else if (os.contains("mac")) {
                return executeCommand("system_profiler SPHardwareDataType | grep 'Serial Number'");
            }
        } catch (Exception e) {
            System.err.println("获取CPU ID失败: " + e.getMessage());
        }
        return "UNKNOWN_CPU";
    }

    /**
     * 获取主板序列号
     */
    private String getMotherboardSerial() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("windows")) {
                return executeCommand("wmic baseboard get SerialNumber").trim();
            } else if (os.contains("linux")) {
                return executeCommand("sudo dmidecode -s baseboard-serial-number").trim();
            } else if (os.contains("mac")) {
                return executeCommand("system_profiler SPHardwareDataType | grep 'Serial Number'");
            }
        } catch (Exception e) {
            System.err.println("获取主板序列号失败: " + e.getMessage());
        }
        return "UNKNOWN_MOTHERBOARD";
    }

    /**
     * 获取MAC地址
     */
    private String getMacAddress() {
        try {
            StringBuilder sb = new StringBuilder();
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                byte[] hardwareAddress = ni.getHardwareAddress();
                
                if (hardwareAddress != null && hardwareAddress.length > 0 && !ni.isLoopback() && ni.isUp()) {
                    StringBuilder mac = new StringBuilder();
                    for (byte b : hardwareAddress) {
                        mac.append(String.format("%02x", b));
                    }
                    sb.append(mac.toString());
                }
            }
            
            return sb.toString();
        } catch (Exception e) {
            System.err.println("获取MAC地址失败: " + e.getMessage());
            return "UNKNOWN_MAC";
        }
    }

    /**
     * 获取磁盘序列号
     */
    private String getDiskSerial() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("windows")) {
                return executeCommand("wmic diskdrive get SerialNumber").trim();
            } else if (os.contains("linux")) {
                return executeCommand("lsblk -d -o name,serial | grep -v 'SERIAL'").trim();
            } else if (os.contains("mac")) {
                return executeCommand("system_profiler SPSerialATADataType | grep 'Serial Number'");
            }
        } catch (Exception e) {
            System.err.println("获取磁盘序列号失败: " + e.getMessage());
        }
        return "UNKNOWN_DISK";
    }

    /**
     * 获取系统UUID
     */
    private String getSystemUuid() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("windows")) {
                return executeCommand("wmic csproduct get UUID").trim();
            } else if (os.contains("linux")) {
                return executeCommand("cat /sys/class/dmi/id/product_uuid").trim();
            } else if (os.contains("mac")) {
                return executeCommand("system_profiler SPHardwareDataType | grep 'Hardware UUID'");
            }
        } catch (Exception e) {
            System.err.println("获取系统UUID失败: " + e.getMessage());
        }
        return "UNKNOWN_UUID";
    }

    /**
     * 获取主机名
     */
    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            System.err.println("获取主机名失败: " + e.getMessage());
            return "UNKNOWN_HOSTNAME";
        }
    }

    /**
     * 获取操作系统信息
     */
    private String getOsInfo() {
        try {
            return System.getProperty("os.name") + "_" + 
                   System.getProperty("os.version") + "_" + 
                   System.getProperty("os.arch");
        } catch (Exception e) {
            System.err.println("获取操作系统信息失败: " + e.getMessage());
            return "UNKNOWN_OS";
        }
    }

    /**
     * 执行系统命令
     */
    private String executeCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            String result = reader.lines()
                    .filter(line -> !line.trim().isEmpty())
                    .collect(Collectors.joining(" "));
            
            process.waitFor();
            return result;
        } catch (Exception e) {
            System.err.println("执行命令失败: " + command + ", 错误: " + e.getMessage());
            return "";
        }
    }

    /**
     * 安全地追加非空字符串
     */
    private void appendIfNotNull(StringBuilder sb, String value) {
        if (value != null && !value.trim().isEmpty() && !value.equals("UNKNOWN")) {
            sb.append(value.trim());
        }
    }

    /**
     * 生成设备标识符
     */
    public String generateDeviceId() {
        HardwareInfo info = getHardwareInfo();
        String fingerprint = generateFingerprint(info);
        
        // 取哈希的前16位作为设备ID
        return fingerprint.substring(0, 16).toUpperCase();
    }

    /**
     * 检查硬件是否与指纹匹配（允许一定的容错）
     */
    public boolean isHardwareCompatible(String expectedFingerprint, double threshold) {
        String currentFingerprint = generateHardwareFingerprint();
        double similarity = calculateSimilarity(currentFingerprint, expectedFingerprint);
        return similarity >= threshold;
    }
}