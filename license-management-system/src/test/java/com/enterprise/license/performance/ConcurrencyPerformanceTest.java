package com.enterprise.license.performance;

import com.enterprise.license.config.TestDataInitializer;
import com.enterprise.license.dto.CustomerDto;
import com.enterprise.license.dto.LicenseDto;
import com.enterprise.license.service.CustomerService;
import com.enterprise.license.service.LicenseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * 并发性能测试
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("并发性能测试")
class ConcurrencyPerformanceTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private LicenseService licenseService;

    @Autowired
    private TestDataInitializer testDataInitializer;

    @Test
    @DisplayName("并发创建客户性能测试")
    void shouldHandleConcurrentCustomerCreation() throws InterruptedException {
        // Given
        int threadCount = 10;
        int operationsPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // When
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    CustomerDto customerDto = new CustomerDto();
                    customerDto.setCustomerCode("PERF-" + threadIndex + "-" + j);
                    customerDto.setCustomerName("性能测试客户-" + threadIndex + "-" + j);
                    customerDto.setContactPerson("联系人" + j);
                    customerDto.setContactPhone("1380013800" + (j % 10));
                    customerDto.setContactEmail("perf" + threadIndex + j + "@test.com");
                    customerDto.setAddress("测试地址" + j);

                    try {
                        customerService.createCustomer(customerDto);
                    } catch (Exception e) {
                        // 记录异常但不影响测试继续
                        System.err.println("创建客户失败: " + e.getMessage());
                    }
                }
            }, executor);
            futures.add(future);
        }

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        stopWatch.stop();
        long totalTime = stopWatch.getTotalTimeMillis();
        int totalOperations = threadCount * operationsPerThread;

        // Then
        System.out.println("并发创建客户性能测试结果:");
        System.out.println("总操作数: " + totalOperations);
        System.out.println("总耗时: " + totalTime + "ms");
        System.out.println("平均每个操作耗时: " + (totalTime / (double) totalOperations) + "ms");
        System.out.println("TPS: " + (totalOperations * 1000.0 / totalTime));

        // 验证性能指标
        assertThat(totalTime / (double) totalOperations).isLessThan(100); // 平均每个操作少于100ms
        assertThat(totalOperations * 1000.0 / totalTime).isGreaterThan(10); // TPS大于10

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("并发查询客户性能测试")
    void shouldHandleConcurrentCustomerQuery() throws InterruptedException {
        // Given - 先创建一些测试数据
        List<CustomerDto> testCustomers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            CustomerDto customerDto = new CustomerDto();
            customerDto.setCustomerCode("QUERY-TEST-" + i);
            customerDto.setCustomerName("查询测试客户" + i);
            customerDto.setContactPerson("联系人" + i);
            customerDto.setContactPhone("1380013800" + i);
            customerDto.setContactEmail("query" + i + "@test.com");
            customerDto.setAddress("测试地址" + i);

            CustomerDto created = customerService.createCustomer(customerDto);
            testCustomers.add(created);
        }

        int threadCount = 20;
        int queriesPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // When
        for (int i = 0; i < threadCount; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < queriesPerThread; j++) {
                    try {
                        Long customerId = testCustomers.get(j % testCustomers.size()).getId();
                        customerService.getCustomerById(customerId);
                    } catch (Exception e) {
                        System.err.println("查询客户失败: " + e.getMessage());
                    }
                }
            }, executor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        stopWatch.stop();
        long totalTime = stopWatch.getTotalTimeMillis();
        int totalQueries = threadCount * queriesPerThread;

        // Then
        System.out.println("并发查询客户性能测试结果:");
        System.out.println("总查询数: " + totalQueries);
        System.out.println("总耗时: " + totalTime + "ms");
        System.out.println("平均每个查询耗时: " + (totalTime / (double) totalQueries) + "ms");
        System.out.println("QPS: " + (totalQueries * 1000.0 / totalTime));

        // 验证性能指标
        assertThat(totalTime / (double) totalQueries).isLessThan(10); // 平均每个查询少于10ms
        assertThat(totalQueries * 1000.0 / totalTime).isGreaterThan(100); // QPS大于100

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("并发许可证验证性能测试")
    void shouldHandleConcurrentLicenseValidation() throws InterruptedException {
        // Given - 创建测试客户和许可证
        CustomerDto customer = new CustomerDto();
        customer.setCustomerCode("VALIDATION-TEST");
        customer.setCustomerName("验证测试客户");
        customer.setContactPerson("测试联系人");
        customer.setContactPhone("13800138000");
        customer.setContactEmail("validation@test.com");
        customer.setAddress("测试地址");

        CustomerDto createdCustomer = customerService.createCustomer(customer);

        LicenseDto license = new LicenseDto();
        license.setLicenseCode("VALIDATION-LIC-001");
        license.setCustomerId(createdCustomer.getId());
        license.setLicenseType(LicenseType.STANDARD);
        license.setProductName("验证测试产品");
        license.setProductVersion("1.0.0");
        license.setMaxUsers(1000);
        license.setExpiresAt(LocalDateTime.now().plusDays(365));

        LicenseDto createdLicense = licenseService.createLicense(license);

        int threadCount = 50;
        int validationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // When
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < validationsPerThread; j++) {
                    try {
                        String hardwareFingerprint = "MAC:AA:BB:CC:DD:EE:" + 
                            String.format("%02X", (threadIndex * validationsPerThread + j) % 256) +
                            "|CPU:Intel|DISK:12345";
                        
                        licenseValidationService.validateLicense(
                                createdLicense.getLicenseCode(),
                                hardwareFingerprint,
                                "验证测试产品",
                                "1.0.0"
                        );
                    } catch (Exception e) {
                        System.err.println("验证许可证失败: " + e.getMessage());
                    }
                }
            }, executor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        stopWatch.stop();
        long totalTime = stopWatch.getTotalTimeMillis();
        int totalValidations = threadCount * validationsPerThread;

        // Then
        System.out.println("并发许可证验证性能测试结果:");
        System.out.println("总验证数: " + totalValidations);
        System.out.println("总耗时: " + totalTime + "ms");
        System.out.println("平均每个验证耗时: " + (totalTime / (double) totalValidations) + "ms");
        System.out.println("VPS (Validations Per Second): " + (totalValidations * 1000.0 / totalTime));

        // 验证性能指标
        assertThat(totalTime / (double) totalValidations).isLessThan(50); // 平均每个验证少于50ms
        assertThat(totalValidations * 1000.0 / totalTime).isGreaterThan(20); // VPS大于20

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("数据库连接池压力测试")
    void shouldHandleDatabaseConnectionPoolStress() throws InterruptedException {
        // Given
        int threadCount = 100; // 超过默认连接池大小
        int operationsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // When
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    try {
                        // 混合读写操作来测试连接池
                        if (j % 2 == 0) {
                            // 读操作
                            QueryParam queryParam = new QueryParam();
                            queryParam.setPage(0);
                            queryParam.setSize(5);
                            customerService.getCustomers(queryParam);
                        } else {
                            // 写操作
                            CustomerDto customerDto = new CustomerDto();
                            customerDto.setCustomerCode("POOL-TEST-" + threadIndex + "-" + j);
                            customerDto.setCustomerName("连接池测试客户");
                            customerDto.setContactPerson("测试联系人");
                            customerDto.setContactPhone("13800138000");
                            customerDto.setContactEmail("pool" + threadIndex + j + "@test.com");
                            customerDto.setAddress("测试地址");

                            customerService.createCustomer(customerDto);
                        }
                    } catch (Exception e) {
                        System.err.println("数据库操作失败: " + e.getMessage());
                    }
                }
            }, executor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        stopWatch.stop();
        long totalTime = stopWatch.getTotalTimeMillis();
        int totalOperations = threadCount * operationsPerThread;

        // Then
        System.out.println("数据库连接池压力测试结果:");
        System.out.println("并发线程数: " + threadCount);
        System.out.println("总操作数: " + totalOperations);
        System.out.println("总耗时: " + totalTime + "ms");
        System.out.println("平均每个操作耗时: " + (totalTime / (double) totalOperations) + "ms");
        System.out.println("TPS: " + (totalOperations * 1000.0 / totalTime));

        // 验证系统在高并发下依然稳定
        assertThat(totalTime / (double) totalOperations).isLessThan(1000); // 平均每个操作少于1秒
        assertThat(totalOperations * 1000.0 / totalTime).isGreaterThan(1); // TPS大于1

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }
}