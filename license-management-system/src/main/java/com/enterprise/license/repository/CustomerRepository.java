package com.enterprise.license.repository;

import com.enterprise.license.entity.Customer;
import com.enterprise.license.enums.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 客户Repository接口
 * 提供客户数据访问功能
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID>, JpaSpecificationExecutor<Customer> {

    /**
     * 根据客户编码查找客户
     * @param customerCode 客户编码
     * @return 客户信息
     */
    Optional<Customer> findByCustomerCode(String customerCode);

    /**
     * 根据邮箱地址查找客户
     * @param email 邮箱地址
     * @return 客户信息
     */
    Optional<Customer> findByEmail(String email);

    /**
     * 根据客户状态查找客户列表
     * @param status 客户状态
     * @param pageable 分页参数
     * @return 客户分页列表
     */
    Page<Customer> findByStatus(CustomerStatus status, Pageable pageable);

    /**
     * 根据客户状态查找客户列表
     * @param status 客户状态
     * @return 客户列表
     */
    List<Customer> findByStatus(CustomerStatus status);

    /**
     * 根据客户名称模糊查询
     * @param customerName 客户名称关键字
     * @param pageable 分页参数
     * @return 客户分页列表
     */
    Page<Customer> findByCustomerNameContainingIgnoreCase(String customerName, Pageable pageable);

    /**
     * 根据行业分类查找客户
     * @param industry 行业分类
     * @param pageable 分页参数
     * @return 客户分页列表
     */
    Page<Customer> findByIndustry(String industry, Pageable pageable);

    /**
     * 根据客户类型查找客户
     * @param customerType 客户类型
     * @param pageable 分页参数
     * @return 客户分页列表
     */
    Page<Customer> findByCustomerType(String customerType, Pageable pageable);

    /**
     * 查找活跃客户列表
     * @param pageable 分页参数
     * @return 活跃客户分页列表
     */
    @Query("SELECT c FROM Customer c WHERE c.status = 'ACTIVE' AND c.deleted = false")
    Page<Customer> findActiveCustomers(Pageable pageable);

    /**
     * 查找合同即将到期的客户
     * @param fromDate 开始日期
     * @param toDate 结束日期
     * @return 客户列表
     */
    @Query("SELECT c FROM Customer c WHERE c.contractEndDate BETWEEN :fromDate AND :toDate AND c.status = 'ACTIVE' AND c.deleted = false")
    List<Customer> findCustomersWithContractExpiringSoon(@Param("fromDate") LocalDate fromDate, 
                                                        @Param("toDate") LocalDate toDate);

    /**
     * 根据销售代表查找客户
     * @param salesRepresentative 销售代表
     * @param pageable 分页参数
     * @return 客户分页列表
     */
    Page<Customer> findBySalesRepresentative(String salesRepresentative, Pageable pageable);

    /**
     * 根据客户经理查找客户
     * @param accountManager 客户经理
     * @param pageable 分页参数
     * @return 客户分页列表
     */
    Page<Customer> findByAccountManager(String accountManager, Pageable pageable);

    /**
     * 根据国家查找客户
     * @param country 国家
     * @param pageable 分页参数
     * @return 客户分页列表
     */
    Page<Customer> findByCountry(String country, Pageable pageable);

    /**
     * 根据城市查找客户
     * @param city 城市
     * @param pageable 分页参数
     * @return 客户分页列表
     */
    Page<Customer> findByCity(String city, Pageable pageable);

    /**
     * 检查客户编码是否存在
     * @param customerCode 客户编码
     * @return true如果存在
     */
    boolean existsByCustomerCode(String customerCode);

    /**
     * 检查邮箱地址是否存在
     * @param email 邮箱地址
     * @return true如果存在
     */
    boolean existsByEmail(String email);

    /**
     * 检查客户编码是否存在（排除指定客户）
     * @param customerCode 客户编码
     * @param customerId 要排除的客户ID
     * @return true如果存在
     */
    @Query("SELECT COUNT(c) > 0 FROM Customer c WHERE c.customerCode = :customerCode AND c.id != :customerId")
    boolean existsByCustomerCodeAndIdNot(@Param("customerCode") String customerCode, 
                                        @Param("customerId") UUID customerId);

    /**
     * 检查邮箱地址是否存在（排除指定客户）
     * @param email 邮箱地址
     * @param customerId 要排除的客户ID
     * @return true如果存在
     */
    @Query("SELECT COUNT(c) > 0 FROM Customer c WHERE c.email = :email AND c.id != :customerId")
    boolean existsByEmailAndIdNot(@Param("email") String email, 
                                 @Param("customerId") UUID customerId);

    /**
     * 统计各状态的客户数量
     * @return 状态统计结果
     */
    @Query("SELECT c.status, COUNT(c) FROM Customer c WHERE c.deleted = false GROUP BY c.status")
    List<Object[]> countByStatus();

    /**
     * 统计各行业的客户数量
     * @return 行业统计结果
     */
    @Query("SELECT c.industry, COUNT(c) FROM Customer c WHERE c.deleted = false AND c.industry IS NOT NULL GROUP BY c.industry ORDER BY COUNT(c) DESC")
    List<Object[]> countByIndustry();

    /**
     * 统计各国家的客户数量
     * @return 国家统计结果
     */
    @Query("SELECT c.country, COUNT(c) FROM Customer c WHERE c.deleted = false AND c.country IS NOT NULL GROUP BY c.country ORDER BY COUNT(c) DESC")
    List<Object[]> countByCountry();

    /**
     * 查找最近创建的客户
     * @param limit 限制数量
     * @return 客户列表
     */
    @Query("SELECT c FROM Customer c WHERE c.deleted = false ORDER BY c.createdTime DESC")
    List<Customer> findRecentCustomers(Pageable pageable);

    /**
     * 查找有授权的客户
     * @param pageable 分页参数
     * @return 客户分页列表
     */
    @Query("SELECT DISTINCT c FROM Customer c JOIN c.licenses l WHERE c.deleted = false")
    Page<Customer> findCustomersWithLicenses(Pageable pageable);

    /**
     * 查找没有授权的客户
     * @param pageable 分页参数
     * @return 客户分页列表
     */
    @Query("SELECT c FROM Customer c WHERE c.deleted = false AND c.id NOT IN (SELECT DISTINCT l.customer.id FROM License l)")
    Page<Customer> findCustomersWithoutLicenses(Pageable pageable);

    /**
     * 根据多个条件搜索客户
     * @param keyword 关键字（搜索客户名称、编码、联系人、邮箱）
     * @param status 客户状态（可选）
     * @param industry 行业（可选）
     * @param country 国家（可选）
     * @param pageable 分页参数
     * @return 客户分页列表
     */
    @Query("SELECT c FROM Customer c WHERE c.deleted = false " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "     c.customerName LIKE %:keyword% OR " +
           "     c.customerCode LIKE %:keyword% OR " +
           "     c.contactName LIKE %:keyword% OR " +
           "     c.email LIKE %:keyword%) " +
           "AND (:status IS NULL OR c.status = :status) " +
           "AND (:industry IS NULL OR :industry = '' OR c.industry = :industry) " +
           "AND (:country IS NULL OR :country = '' OR c.country = :country)")
    Page<Customer> searchCustomers(@Param("keyword") String keyword,
                                  @Param("status") CustomerStatus status,
                                  @Param("industry") String industry,
                                  @Param("country") String country,
                                  Pageable pageable);

    /**
     * 统计总客户数
     * @return 总客户数
     */
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.deleted = false")
    long countTotalCustomers();

    /**
     * 统计活跃客户数
     * @return 活跃客户数
     */
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.deleted = false AND c.status = 'ACTIVE'")
    long countActiveCustomers();

    /**
     * 统计本月新增客户数
     * @param startOfMonth 月初日期
     * @param endOfMonth 月末日期
     * @return 新增客户数
     */
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.deleted = false AND c.createdTime BETWEEN :startOfMonth AND :endOfMonth")
    long countNewCustomersThisMonth(@Param("startOfMonth") LocalDate startOfMonth, 
                                   @Param("endOfMonth") LocalDate endOfMonth);

    /**
     * 获取所有行业列表
     * @return 行业列表
     */
    @Query("SELECT DISTINCT c.industry FROM Customer c WHERE c.deleted = false AND c.industry IS NOT NULL ORDER BY c.industry")
    List<String> findAllIndustries();

    /**
     * 获取所有国家列表
     * @return 国家列表
     */
    @Query("SELECT DISTINCT c.country FROM Customer c WHERE c.deleted = false AND c.country IS NOT NULL ORDER BY c.country")
    List<String> findAllCountries();

    /**
     * 获取所有销售代表列表
     * @return 销售代表列表
     */
    @Query("SELECT DISTINCT c.salesRepresentative FROM Customer c WHERE c.deleted = false AND c.salesRepresentative IS NOT NULL ORDER BY c.salesRepresentative")
    List<String> findAllSalesRepresentatives();
}