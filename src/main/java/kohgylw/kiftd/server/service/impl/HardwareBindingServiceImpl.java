package kohgylw.kiftd.server.service.impl;

import kohgylw.kiftd.server.service.HardwareBindingService;
import kohgylw.kiftd.server.pojo.HardwareFingerprint;
import kohgylw.kiftd.printer.Printer;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;

/**
 * 硬件绑定服务实现
 * 提供跨平台的硬件信息获取和验证功能
 * 
 * @author 企业安全专家
 * @version 2.0
 */
@Service
public class HardwareBindingServiceImpl implements HardwareBindingService {
    
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final boolean IS_WINDOWS = OS_NAME.contains("windows");
    private static final boolean IS_LINUX = OS_NAME.contains("linux");
    private static final boolean IS_MAC = OS_NAME.contains("mac");
    
    @Override
    public HardwareFingerprint getCurrentHardwareFingerprint(HardwareFingerprint.BindingPolicy bindingPolicy) throws Exception {
        HardwareFingerprint fingerprint = new HardwareFingerprint();
        
        try {
            // 根据绑定策略获取相应的硬件信息
            switch (bindingPolicy) {
                case STRICT:
                    fingerprint.setCpuId(getCpuId());
                    fingerprint.setMotherboardSerial(getMotherboardSerial());
                    fingerprint.setDiskSerial(getDiskSerial());
                    fingerprint.setMacAddress(getMacAddress());
                    break;
                case MODERATE:
                    fingerprint.setCpuId(getCpuId());
                    fingerprint.setMotherboardSerial(getMotherboardSerial());
                    fingerprint.setDiskSerial(getDiskSerial());
                    break;
                case LOOSE:
                    fingerprint.setCpuId(getCpuId());
                    fingerprint.setMacAddress(getMacAddress());
                    break;
                case NONE:
                    // 无绑定，返回空指纹
                    break;
            }
            
            // 设置系统信息
            fingerprint.setOsInfo(getOsInfo());
            fingerprint.setMachineName(getMachineName());
            fingerprint.setUserName(getUserName());
            fingerprint.setBindingPolicy(bindingPolicy);
            
            Printer.instance.print("获取硬件指纹完成，绑定策略: " + bindingPolicy);
            return fingerprint;
            
        } catch (Exception e) {
            Printer.instance.print("获取硬件指纹失败: " + e.getMessage());
            throw new Exception("Failed to get hardware fingerprint: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean verifyHardwareBinding(HardwareFingerprint expected, HardwareFingerprint actual) {
        if (expected == null || actual == null) {
            return false;
        }
        
        if (expected.getBindingPolicy() == HardwareFingerprint.BindingPolicy.NONE) {
            return true; // 无绑定策略，总是通过
        }
        
        boolean matches = expected.matches(actual);
        int similarityScore = calculateSimilarityScore(expected, actual);
        
        Printer.instance.print("硬件绑定验证: " + (matches ? "通过" : "失败") + 
                             ", 相似度评分: " + similarityScore + "%");
        
        return matches;
    }
    
    @Override
    public String getCpuId() throws Exception {
        try {
            if (IS_WINDOWS) {
                return executeCommand("wmic cpu get ProcessorId /format:list")
                        .lines()
                        .filter(line -> line.startsWith("ProcessorId="))
                        .map(line -> line.substring("ProcessorId=".length()).trim())
                        .filter(id -> !id.isEmpty())
                        .findFirst()
                        .orElse("UNKNOWN_CPU_ID");
            } else if (IS_LINUX) {
                // Linux下获取CPU信息
                String cpuInfo = executeCommand("cat /proc/cpuinfo");
                return cpuInfo.lines()
                        .filter(line -> line.startsWith("cpu family") || line.startsWith("model"))
                        .map(line -> line.split(":")[1].trim())
                        .reduce("", (a, b) -> a + b)
                        .replaceAll("\\s+", "");
            } else if (IS_MAC) {
                return executeCommand("system_profiler SPHardwareDataType")
                        .lines()
                        .filter(line -> line.contains("Serial Number") || line.contains("Chip"))
                        .map(line -> line.split(":")[1].trim())
                        .reduce("", (a, b) -> a + b)
                        .replaceAll("\\s+", "");
            }
            
            return "UNSUPPORTED_OS_CPU_ID";
            
        } catch (Exception e) {
            Printer.instance.print("获取CPU ID失败: " + e.getMessage());
            return "ERROR_CPU_ID";
        }
    }
    
    @Override
    public String getMotherboardSerial() throws Exception {
        try {
            if (IS_WINDOWS) {
                return executeCommand("wmic baseboard get SerialNumber /format:list")
                        .lines()
                        .filter(line -> line.startsWith("SerialNumber="))
                        .map(line -> line.substring("SerialNumber=".length()).trim())
                        .filter(serial -> !serial.isEmpty())
                        .findFirst()
                        .orElse("UNKNOWN_MOTHERBOARD_SERIAL");
            } else if (IS_LINUX) {
                try {
                    return executeCommand("sudo dmidecode -s baseboard-serial-number").trim();
                } catch (Exception e) {
                    // 如果没有sudo权限，尝试其他方法
                    return executeCommand("cat /sys/class/dmi/id/board_serial").trim();
                }
            } else if (IS_MAC) {
                return executeCommand("system_profiler SPHardwareDataType")
                        .lines()
                        .filter(line -> line.contains("Serial Number"))
                        .map(line -> line.split(":")[1].trim())
                        .findFirst()
                        .orElse("UNKNOWN_MAC_SERIAL");
            }
            
            return "UNSUPPORTED_OS_MOTHERBOARD_SERIAL";
            
        } catch (Exception e) {
            Printer.instance.print("获取主板序列号失败: " + e.getMessage());
            return "ERROR_MOTHERBOARD_SERIAL";
        }
    }
    
    @Override
    public String getDiskSerial() throws Exception {
        try {
            if (IS_WINDOWS) {
                return executeCommand("wmic diskdrive get SerialNumber /format:list")
                        .lines()
                        .filter(line -> line.startsWith("SerialNumber="))
                        .map(line -> line.substring("SerialNumber=".length()).trim())
                        .filter(serial -> !serial.isEmpty())
                        .findFirst()
                        .orElse("UNKNOWN_DISK_SERIAL");
            } else if (IS_LINUX) {
                try {
                    return executeCommand("lsblk -d -o name,serial | head -n 2 | tail -n 1")
                            .split("\\s+")[1];
                } catch (Exception e) {
                    // 尝试其他方法
                    return executeCommand("ls -la /dev/disk/by-id/")
                            .lines()
                            .filter(line -> line.contains("ata-"))
                            .findFirst()
                            .map(line -> line.split("ata-")[1].split("-")[0])
                            .orElse("UNKNOWN_LINUX_DISK_SERIAL");
                }
            } else if (IS_MAC) {
                return executeCommand("system_profiler SPSerialATADataType")
                        .lines()
                        .filter(line -> line.contains("Serial Number"))
                        .map(line -> line.split(":")[1].trim())
                        .findFirst()
                        .orElse("UNKNOWN_MAC_DISK_SERIAL");
            }
            
            return "UNSUPPORTED_OS_DISK_SERIAL";
            
        } catch (Exception e) {
            Printer.instance.print("获取硬盘序列号失败: " + e.getMessage());
            return "ERROR_DISK_SERIAL";
        }
    }
    
    @Override
    public String getMacAddress() throws Exception {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            
            for (NetworkInterface networkInterface : Collections.list(networkInterfaces)) {
                if (!networkInterface.isLoopback() && networkInterface.isUp()) {
                    byte[] mac = networkInterface.getHardwareAddress();
                    if (mac != null && mac.length == 6) {
                        StringBuilder macAddress = new StringBuilder();
                        for (int i = 0; i < mac.length; i++) {
                            macAddress.append(String.format("%02X", mac[i]));
                            if (i < mac.length - 1) {
                                macAddress.append(":");
                            }
                        }
                        return macAddress.toString();
                    }
                }
            }
            
            return "UNKNOWN_MAC_ADDRESS";
            
        } catch (Exception e) {
            Printer.instance.print("获取MAC地址失败: " + e.getMessage());
            return "ERROR_MAC_ADDRESS";
        }
    }
    
    @Override
    public String getOsInfo() {
        return System.getProperty("os.name") + " " + 
               System.getProperty("os.version") + " " + 
               System.getProperty("os.arch");
    }
    
    @Override
    public String getMachineName() {
        try {
            if (IS_WINDOWS) {
                return executeCommand("hostname").trim();
            } else {
                return executeCommand("uname -n").trim();
            }
        } catch (Exception e) {
            return System.getProperty("user.name", "UNKNOWN_MACHINE");
        }
    }
    
    @Override
    public String getUserName() {
        return System.getProperty("user.name", "UNKNOWN_USER");
    }
    
    @Override
    public int calculateSimilarityScore(HardwareFingerprint fingerprint1, HardwareFingerprint fingerprint2) {
        if (fingerprint1 == null || fingerprint2 == null) {
            return 0;
        }
        
        return fingerprint1.calculateSimilarityScore(fingerprint2);
    }
    
    @Override
    public boolean isStrictBinding(HardwareFingerprint.BindingPolicy bindingPolicy) {
        return bindingPolicy == HardwareFingerprint.BindingPolicy.STRICT;
    }
    
    @Override
    public String generateBindingReport(HardwareFingerprint expected, HardwareFingerprint actual) {
        if (expected == null || actual == null) {
            return "无效的硬件指纹数据";
        }
        
        StringBuilder report = new StringBuilder();
        report.append("硬件绑定验证报告\n");
        report.append("==================\n\n");
        
        report.append("绑定策略: ").append(expected.getBindingPolicy().getDescription()).append("\n");
        report.append("相似度评分: ").append(calculateSimilarityScore(expected, actual)).append("%\n");
        report.append("验证结果: ").append(verifyHardwareBinding(expected, actual) ? "通过" : "失败").append("\n\n");
        
        report.append("详细比较:\n");
        report.append("--------\n");
        
        if (expected.getCpuId() != null || actual.getCpuId() != null) {
            report.append("CPU ID: ");
            if (safeEquals(expected.getCpuId(), actual.getCpuId())) {
                report.append("匹配 ✓\n");
            } else {
                report.append("不匹配 ✗\n");
                report.append("  期望: ").append(maskSensitive(expected.getCpuId())).append("\n");
                report.append("  实际: ").append(maskSensitive(actual.getCpuId())).append("\n");
            }
        }
        
        if (expected.getMotherboardSerial() != null || actual.getMotherboardSerial() != null) {
            report.append("主板序列号: ");
            if (safeEquals(expected.getMotherboardSerial(), actual.getMotherboardSerial())) {
                report.append("匹配 ✓\n");
            } else {
                report.append("不匹配 ✗\n");
                report.append("  期望: ").append(maskSensitive(expected.getMotherboardSerial())).append("\n");
                report.append("  实际: ").append(maskSensitive(actual.getMotherboardSerial())).append("\n");
            }
        }
        
        if (expected.getDiskSerial() != null || actual.getDiskSerial() != null) {
            report.append("硬盘序列号: ");
            if (safeEquals(expected.getDiskSerial(), actual.getDiskSerial())) {
                report.append("匹配 ✓\n");
            } else {
                report.append("不匹配 ✗\n");
                report.append("  期望: ").append(maskSensitive(expected.getDiskSerial())).append("\n");
                report.append("  实际: ").append(maskSensitive(actual.getDiskSerial())).append("\n");
            }
        }
        
        if (expected.getMacAddress() != null || actual.getMacAddress() != null) {
            report.append("MAC地址: ");
            if (safeEquals(expected.getMacAddress(), actual.getMacAddress())) {
                report.append("匹配 ✓\n");
            } else {
                report.append("不匹配 ✗\n");
                report.append("  期望: ").append(maskSensitive(expected.getMacAddress())).append("\n");
                report.append("  实际: ").append(maskSensitive(actual.getMacAddress())).append("\n");
            }
        }
        
        report.append("\n系统信息:\n");
        report.append("操作系统: ").append(actual.getOsInfo()).append("\n");
        report.append("机器名称: ").append(actual.getMachineName()).append("\n");
        report.append("用户名: ").append(actual.getUserName()).append("\n");
        
        return report.toString();
    }
    
    /**
     * 执行系统命令
     */
    private String executeCommand(String command) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder();
        
        if (IS_WINDOWS) {
            processBuilder.command("cmd.exe", "/c", command);
        } else {
            processBuilder.command("sh", "-c", command);
        }
        
        Process process = processBuilder.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Command execution failed with exit code: " + exitCode);
        }
        
        return output.toString();
    }
    
    /**
     * 安全比较两个字符串
     */
    private boolean safeEquals(String str1, String str2) {
        if (str1 == null && str2 == null) {
            return true;
        }
        if (str1 == null || str2 == null) {
            return false;
        }
        return str1.equals(str2);
    }
    
    /**
     * 掩码敏感信息
     */
    private String maskSensitive(String value) {
        if (value == null || value.length() < 4) {
            return "***";
        }
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }
}