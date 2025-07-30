package kohgylw.kiftd.server.service;

import kohgylw.kiftd.server.pojo.HardwareFingerprint;

/**
 * 硬件绑定服务接口
 * 提供硬件指纹生成和验证功能
 * 
 * @author 企业安全专家
 * @version 2.0
 */
public interface HardwareBindingService {
    
    /**
     * 获取当前系统的硬件指纹
     * @param bindingPolicy 绑定策略
     * @return HardwareFingerprint 硬件指纹
     * @throws Exception 获取异常
     */
    HardwareFingerprint getCurrentHardwareFingerprint(HardwareFingerprint.BindingPolicy bindingPolicy) throws Exception;
    
    /**
     * 验证硬件指纹是否匹配
     * @param expected 期望的硬件指纹
     * @param actual 实际的硬件指纹
     * @return boolean 是否匹配
     */
    boolean verifyHardwareBinding(HardwareFingerprint expected, HardwareFingerprint actual);
    
    /**
     * 获取CPU序列号
     * @return String CPU序列号
     * @throws Exception 获取异常
     */
    String getCpuId() throws Exception;
    
    /**
     * 获取主板序列号
     * @return String 主板序列号
     * @throws Exception 获取异常
     */
    String getMotherboardSerial() throws Exception;
    
    /**
     * 获取硬盘序列号
     * @return String 硬盘序列号
     * @throws Exception 获取异常
     */
    String getDiskSerial() throws Exception;
    
    /**
     * 获取MAC地址
     * @return String MAC地址
     * @throws Exception 获取异常
     */
    String getMacAddress() throws Exception;
    
    /**
     * 获取操作系统信息
     * @return String 操作系统信息
     */
    String getOsInfo();
    
    /**
     * 获取机器名称
     * @return String 机器名称
     */
    String getMachineName();
    
    /**
     * 获取用户名
     * @return String 用户名
     */
    String getUserName();
    
    /**
     * 计算硬件指纹相似度评分
     * @param fingerprint1 硬件指纹1
     * @param fingerprint2 硬件指纹2
     * @return int 相似度评分 (0-100)
     */
    int calculateSimilarityScore(HardwareFingerprint fingerprint1, HardwareFingerprint fingerprint2);
    
    /**
     * 检查硬件绑定策略的严格程度
     * @param bindingPolicy 绑定策略
     * @return boolean 是否为严格绑定
     */
    boolean isStrictBinding(HardwareFingerprint.BindingPolicy bindingPolicy);
    
    /**
     * 生成硬件绑定报告
     * @param expected 期望的硬件指纹
     * @param actual 实际的硬件指纹
     * @return String 绑定验证报告
     */
    String generateBindingReport(HardwareFingerprint expected, HardwareFingerprint actual);
}