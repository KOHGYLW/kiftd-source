package com.enterprise.license.performance;

import com.enterprise.license.config.TestDataInitializer;
import com.enterprise.license.entity.Customer;
import com.enterprise.license.entity.License;
import com.enterprise.license.service.CustomerService;
import com.enterprise.license.service.LicenseService;
import com.enterprise.license.service.LicenseValidationService;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 性能基准测试
 * 使用JMH框架测试关键业务操作的性能
 */
@SpringBootTest
@ActiveProfiles("test")
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
public class PerformanceBenchmarkTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private LicenseService licenseService;

    @Autowired
    private LicenseValidationService licenseValidationService;

    @Autowired
    private TestDataInitializer testDataInitializer;

    private List<Customer> testCustomers;
    private List<License> testLicenses;

    @Setup(Level.Trial)
    public void setUp() {
        // 初始化测试数据
        testCustomers = testDataInitializer.initializeTestCustomers();
        testLicenses = testDataInitializer.initializeTestLicenses(testCustomers);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        // 清理测试数据
        testDataInitializer.cleanupTestData();
    }

    /**
     * 测试客户查询性能
     */
    @Benchmark
    public void benchmarkCustomerQuery() {
        customerService.getCustomerById(testCustomers.get(0).getId());
    }

    /**
     * 测试许可证查询性能
     */
    @Benchmark
    public void benchmarkLicenseQuery() {
        licenseService.getLicenseById(testLicenses.get(0).getId());
    }

    /**
     * 测试许可证验证性能
     */
    @Benchmark
    public void benchmarkLicenseValidation() {
        License license = testLicenses.get(0);
        String hardwareFingerprint = "MAC:AA:BB:CC:DD:EE:FF|CPU:Intel|DISK:12345";
        
        licenseValidationService.validateLicense(
                license.getLicenseCode(),
                hardwareFingerprint,
                license.getProductName(),
                license.getProductVersion()
        );
    }

    /**
     * 测试客户分页查询性能
     */
    @Benchmark
    public void benchmarkCustomerPageQuery() {
        QueryParam queryParam = new QueryParam();
        queryParam.setPage(0);
        queryParam.setSize(10);
        customerService.getCustomers(queryParam);
    }

    /**
     * 测试许可证分页查询性能
     */
    @Benchmark
    public void benchmarkLicensePageQuery() {
        QueryParam queryParam = new QueryParam();
        queryParam.setPage(0);
        queryParam.setSize(10);
        licenseService.getLicenses(queryParam);
    }

    /**
     * 运行基准测试
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(PerformanceBenchmarkTest.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}