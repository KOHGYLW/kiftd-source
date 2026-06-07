package com.enterprise.license.config;

import com.enterprise.license.security.LicenseUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 应用初始化配置
 */
@Component
public class ApplicationInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationInitializer.class);

    @Autowired
    private LicenseUserDetailsService userDetailsService;

    @Override
    public void run(String... args) throws Exception {
        logger.info("开始初始化应用配置...");
        
        // 初始化默认管理员用户
        initializeDefaultUsers();
        
        logger.info("应用配置初始化完成");
    }

    /**
     * 初始化默认用户
     */
    private void initializeDefaultUsers() {
        try {
            userDetailsService.initializeDefaultAdmin();
            logger.info("默认用户初始化完成");
        } catch (Exception e) {
            logger.error("默认用户初始化失败", e);
        }
    }
}