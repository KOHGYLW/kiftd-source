package com.enterprise.license;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * License Management System Application
 * 
 * @author Enterprise Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableTransactionManagement
public class LicenseManagementSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(LicenseManagementSystemApplication.class, args);
    }

}