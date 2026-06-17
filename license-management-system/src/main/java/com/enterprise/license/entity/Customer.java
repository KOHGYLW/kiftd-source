package com.enterprise.license.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 客户实体类
 */
@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {

    @Column(name = "customer_id", unique = true, nullable = false, length = 50)
    private String customerId;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "contact_name", length = 100)
    private String contactName;

    @Column(name = "contact_email", nullable = false, length = 200)
    private String contactEmail;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "industry", length = 100)
    private String industry;

    @Column(name = "customer_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CustomerStatus customerStatus;

    @Column(name = "customer_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CustomerType customerType;

    @Column(name = "credit_limit")
    private Double creditLimit;

    @Column(name = "payment_terms", length = 50)
    private String paymentTerms;

    @Column(name = "tax_id", length = 100)
    private String taxId;

    @Column(name = "website", length = 200)
    private String website;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;

    @Column(name = "last_contact_date")
    private LocalDateTime lastContactDate;

    @OneToMany(mappedBy = "customerId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<License> licenses = new ArrayList<>();

    /**
     * 客户状态枚举
     */
    public enum CustomerStatus {
        ACTIVE("活跃"),
        INACTIVE("不活跃"),
        SUSPENDED("暂停"),
        BLACKLISTED("黑名单");

        private final String description;

        CustomerStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 客户类型枚举
     */
    public enum CustomerType {
        INDIVIDUAL("个人"),
        SMALL_BUSINESS("小企业"),
        MEDIUM_BUSINESS("中型企业"),
        LARGE_ENTERPRISE("大企业"),
        GOVERNMENT("政府"),
        EDUCATIONAL("教育机构"),
        NON_PROFIT("非营利组织");

        private final String description;

        CustomerType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Constructors
    public Customer() {
        this.customerStatus = CustomerStatus.ACTIVE;
        this.registrationDate = LocalDateTime.now();
    }

    public Customer(String customerId, String companyName, String contactEmail) {
        this();
        this.customerId = customerId;
        this.companyName = companyName;
        this.contactEmail = contactEmail;
    }

    // Getters and Setters
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public CustomerStatus getCustomerStatus() { return customerStatus; }
    public void setCustomerStatus(CustomerStatus customerStatus) { this.customerStatus = customerStatus; }

    public CustomerType getCustomerType() { return customerType; }
    public void setCustomerType(CustomerType customerType) { this.customerType = customerType; }

    public Double getCreditLimit() { return creditLimit; }
    public void setCreditLimit(Double creditLimit) { this.creditLimit = creditLimit; }

    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }

    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDateTime registrationDate) { this.registrationDate = registrationDate; }

    public LocalDateTime getLastContactDate() { return lastContactDate; }
    public void setLastContactDate(LocalDateTime lastContactDate) { this.lastContactDate = lastContactDate; }

    public List<License> getLicenses() { return licenses; }
    public void setLicenses(List<License> licenses) { this.licenses = licenses; }

    // Business methods
    public boolean isActive() {
        return customerStatus == CustomerStatus.ACTIVE;
    }

    public boolean isBlacklisted() {
        return customerStatus == CustomerStatus.BLACKLISTED;
    }

    public int getActiveLicenseCount() {
        return (int) licenses.stream()
                .filter(License::isActive)
                .count();
    }

    public int getExpiredLicenseCount() {
        return (int) licenses.stream()
                .filter(License::isExpired)
                .count();
    }

    public void updateLastContactDate() {
        this.lastContactDate = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Customer{" +
                "customerId='" + customerId + '\'' +
                ", companyName='" + companyName + '\'' +
                ", contactEmail='" + contactEmail + '\'' +
                ", customerStatus=" + customerStatus +
                ", customerType=" + customerType +
                '}';
    }
}