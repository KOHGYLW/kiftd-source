package com.enterprise.license.service;

import com.enterprise.license.dto.CustomerDto;
import com.enterprise.license.dto.LicenseDto;
import com.enterprise.license.dto.QueryParam;
import com.enterprise.license.entity.Customer;
import com.enterprise.license.entity.License;
import com.enterprise.license.entity.LicenseValidationLog;
import com.enterprise.license.repository.CustomerRepository;
import com.enterprise.license.repository.LicenseRepository;
import com.enterprise.license.repository.LicenseValidationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据导出服务
 */
@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class ExportService extends BaseService {

    private final CustomerRepository customerRepository;
    private final LicenseRepository licenseRepository;
    private final LicenseValidationLogRepository validationLogRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 导出客户数据到Excel
     */
    public byte[] exportCustomersToExcel(QueryParam.CustomerQueryParam queryParam, String[] fields) throws IOException {
        log.info("开始导出客户数据到Excel");

        // 查询数据
        Specification<Customer> spec = buildCustomerSpecification(queryParam);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdTime");
        List<Customer> customers = customerRepository.findAll(spec, sort);

        // 创建工作簿
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("客户数据");

            // 设置样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            // 创建标题行
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("客户数据导出报表");
            titleCell.setCellStyle(createTitleStyle(workbook));
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, getCustomerFieldCount(fields) - 1));

            // 创建信息行
            Row infoRow = sheet.createRow(1);
            Cell infoCell = infoRow.createCell(0);
            infoCell.setCellValue("导出时间: " + LocalDateTime.now().format(DATE_FORMATTER) + 
                               " | 数据总数: " + customers.size());
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, getCustomerFieldCount(fields) - 1));

            // 创建表头
            Row headerRow = sheet.createRow(3);
            createCustomerHeader(headerRow, headerStyle, fields);

            // 填充数据
            int rowNum = 4;
            for (Customer customer : customers) {
                Row dataRow = sheet.createRow(rowNum++);
                fillCustomerData(dataRow, customer, dataStyle, fields);
            }

            // 自动调整列宽
            for (int i = 0; i < getCustomerFieldCount(fields); i++) {
                sheet.autoSizeColumn(i);
            }

            // 转换为字节数组
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            
            logOperation("导出客户数据", "Excel格式，数量: " + customers.size());
            return outputStream.toByteArray();
        }
    }

    /**
     * 导出授权数据到Excel
     */
    public byte[] exportLicensesToExcel(QueryParam.LicenseQueryParam queryParam, String[] fields) throws IOException {
        log.info("开始导出授权数据到Excel");

        // 查询数据
        Specification<License> spec = buildLicenseSpecification(queryParam);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdTime");
        List<License> licenses = licenseRepository.findAll(spec, sort);

        // 创建工作簿
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("授权数据");

            // 设置样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            // 创建标题行
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("授权数据导出报表");
            titleCell.setCellStyle(createTitleStyle(workbook));
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, getLicenseFieldCount(fields) - 1));

            // 创建信息行
            Row infoRow = sheet.createRow(1);
            Cell infoCell = infoRow.createCell(0);
            infoCell.setCellValue("导出时间: " + LocalDateTime.now().format(DATE_FORMATTER) + 
                               " | 数据总数: " + licenses.size());
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, getLicenseFieldCount(fields) - 1));

            // 创建表头
            Row headerRow = sheet.createRow(3);
            createLicenseHeader(headerRow, headerStyle, fields);

            // 填充数据
            int rowNum = 4;
            for (License license : licenses) {
                Row dataRow = sheet.createRow(rowNum++);
                fillLicenseData(dataRow, license, dataStyle, fields);
            }

            // 自动调整列宽
            for (int i = 0; i < getLicenseFieldCount(fields); i++) {
                sheet.autoSizeColumn(i);
            }

            // 转换为字节数组
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            
            logOperation("导出授权数据", "Excel格式，数量: " + licenses.size());
            return outputStream.toByteArray();
        }
    }

    /**
     * 导出验证日志到Excel
     */
    public byte[] exportValidationLogsToExcel(QueryParam.ValidationLogQueryParam queryParam, String[] fields) throws IOException {
        log.info("开始导出验证日志到Excel");

        // 查询数据
        Specification<LicenseValidationLog> spec = buildValidationLogSpecification(queryParam);
        Sort sort = Sort.by(Sort.Direction.DESC, "validationTime");
        List<LicenseValidationLog> logs = validationLogRepository.findAll(spec, sort);

        // 创建工作簿
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("验证日志");

            // 设置样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            // 创建标题行
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("验证日志导出报表");
            titleCell.setCellStyle(createTitleStyle(workbook));
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, getValidationLogFieldCount(fields) - 1));

            // 创建信息行
            Row infoRow = sheet.createRow(1);
            Cell infoCell = infoRow.createCell(0);
            infoCell.setCellValue("导出时间: " + LocalDateTime.now().format(DATE_FORMATTER) + 
                               " | 数据总数: " + logs.size());
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, getValidationLogFieldCount(fields) - 1));

            // 创建表头
            Row headerRow = sheet.createRow(3);
            createValidationLogHeader(headerRow, headerStyle, fields);

            // 填充数据
            int rowNum = 4;
            for (LicenseValidationLog log : logs) {
                Row dataRow = sheet.createRow(rowNum++);
                fillValidationLogData(dataRow, log, dataStyle, fields);
            }

            // 自动调整列宽
            for (int i = 0; i < getValidationLogFieldCount(fields); i++) {
                sheet.autoSizeColumn(i);
            }

            // 转换为字节数组
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            
            logOperation("导出验证日志", "Excel格式，数量: " + logs.size());
            return outputStream.toByteArray();
        }
    }

    /**
     * 导出客户数据到CSV
     */
    public String exportCustomersToCSV(QueryParam.CustomerQueryParam queryParam, String[] fields) {
        log.info("开始导出客户数据到CSV");

        // 查询数据
        Specification<Customer> spec = buildCustomerSpecification(queryParam);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdTime");
        List<Customer> customers = customerRepository.findAll(spec, sort);

        StringBuilder csv = new StringBuilder();
        
        // 添加BOM以支持中文
        csv.append("\uFEFF");
        
        // 创建表头
        List<String> headers = getCustomerHeaders(fields);
        csv.append(String.join(",", headers)).append("\n");

        // 填充数据
        for (Customer customer : customers) {
            List<String> values = getCustomerValues(customer, fields);
            csv.append(String.join(",", values)).append("\n");
        }

        logOperation("导出客户数据", "CSV格式，数量: " + customers.size());
        return csv.toString();
    }

    /**
     * 导出授权数据到CSV
     */
    public String exportLicensesToCSV(QueryParam.LicenseQueryParam queryParam, String[] fields) {
        log.info("开始导出授权数据到CSV");

        // 查询数据
        Specification<License> spec = buildLicenseSpecification(queryParam);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdTime");
        List<License> licenses = licenseRepository.findAll(spec, sort);

        StringBuilder csv = new StringBuilder();
        
        // 添加BOM以支持中文
        csv.append("\uFEFF");
        
        // 创建表头
        List<String> headers = getLicenseHeaders(fields);
        csv.append(String.join(",", headers)).append("\n");

        // 填充数据
        for (License license : licenses) {
            List<String> values = getLicenseValues(license, fields);
            csv.append(String.join(",", values)).append("\n");
        }

        logOperation("导出授权数据", "CSV格式，数量: " + licenses.size());
        return csv.toString();
    }

    /**
     * 设置响应头进行文件下载
     */
    public void setDownloadResponseHeaders(HttpServletResponse response, String fileName, String contentType) {
        response.setContentType(contentType);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
    }

    // ============ 私有方法 ============

    /**
     * 构建客户查询条件
     */
    private Specification<Customer> buildCustomerSpecification(QueryParam.CustomerQueryParam queryParam) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("deleted"), false));

            if (StringUtils.isNotBlank(queryParam.getKeyword())) {
                String keyword = "%" + queryParam.getKeyword().toLowerCase() + "%";
                Predicate namePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("customerName")), keyword);
                Predicate codePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("customerCode")), keyword);
                predicates.add(criteriaBuilder.or(namePredicate, codePredicate));
            }

            if (queryParam.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), queryParam.getStatus()));
            }

            if (queryParam.getType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), queryParam.getType()));
            }

            if (queryParam.getStartTime() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("createdTime"), queryParam.getStartTime()));
            }

            if (queryParam.getEndTime() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("createdTime"), queryParam.getEndTime()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 构建授权查询条件
     */
    private Specification<License> buildLicenseSpecification(QueryParam.LicenseQueryParam queryParam) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("deleted"), false));

            if (StringUtils.isNotBlank(queryParam.getKeyword())) {
                String keyword = "%" + queryParam.getKeyword().toLowerCase() + "%";
                Predicate codePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("licenseCode")), keyword);
                Predicate productPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("productName")), keyword);
                predicates.add(criteriaBuilder.or(codePredicate, productPredicate));
            }

            if (queryParam.getCustomerId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("customerId"), queryParam.getCustomerId()));
            }

            if (queryParam.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), queryParam.getStatus()));
            }

            if (queryParam.getLicenseType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("licenseType"), queryParam.getLicenseType()));
            }

            if (queryParam.getStartTime() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("createdTime"), queryParam.getStartTime()));
            }

            if (queryParam.getEndTime() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("createdTime"), queryParam.getEndTime()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 构建验证日志查询条件
     */
    private Specification<LicenseValidationLog> buildValidationLogSpecification(QueryParam.ValidationLogQueryParam queryParam) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("deleted"), false));

            if (queryParam.getLicenseId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("licenseId"), queryParam.getLicenseId()));
            }

            if (StringUtils.isNotBlank(queryParam.getClientIp())) {
                predicates.add(criteriaBuilder.like(root.get("clientIp"), 
                        "%" + queryParam.getClientIp() + "%"));
            }

            if (StringUtils.isNotBlank(queryParam.getValidationStatus())) {
                predicates.add(criteriaBuilder.equal(root.get("validationStatus"), 
                        LicenseValidationLog.ValidationStatus.valueOf(queryParam.getValidationStatus())));
            }

            if (queryParam.getValidationStartTime() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("validationTime"), queryParam.getValidationStartTime()));
            }

            if (queryParam.getValidationEndTime() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("validationTime"), queryParam.getValidationEndTime()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 创建标题样式
     */
    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * 创建表头样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 创建数据样式
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 创建客户表头
     */
    private void createCustomerHeader(Row row, CellStyle style, String[] fields) {
        List<String> headers = getCustomerHeaders(fields);
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(style);
        }
    }

    /**
     * 填充客户数据
     */
    private void fillCustomerData(Row row, Customer customer, CellStyle style, String[] fields) {
        List<String> values = getCustomerValues(customer, fields);
        for (int i = 0; i < values.size(); i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(values.get(i));
            cell.setCellStyle(style);
        }
    }

    /**
     * 获取客户表头
     */
    private List<String> getCustomerHeaders(String[] fields) {
        List<String> allHeaders = Arrays.asList(
                "客户ID", "客户编码", "客户名称", "联系人", "邮箱", "电话", 
                "地址", "状态", "类型", "备注", "最后登录时间", "创建时间", "更新时间"
        );
        
        if (fields != null && fields.length > 0) {
            // 根据指定字段返回对应表头
            return Arrays.stream(fields)
                    .map(this::getCustomerHeaderByField)
                    .collect(Collectors.toList());
        }
        
        return allHeaders;
    }

    /**
     * 获取客户数据值
     */
    private List<String> getCustomerValues(Customer customer, String[] fields) {
        List<String> allValues = Arrays.asList(
                String.valueOf(customer.getId()),
                customer.getCustomerCode(),
                customer.getCustomerName(),
                customer.getContactPerson(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getAddress(),
                customer.getStatus() != null ? customer.getStatus().getDescription() : "",
                customer.getType() != null ? customer.getType().getDescription() : "",
                customer.getRemark(),
                customer.getLastLoginTime() != null ? customer.getLastLoginTime().format(DATE_FORMATTER) : "",
                customer.getCreatedTime() != null ? customer.getCreatedTime().format(DATE_FORMATTER) : "",
                customer.getUpdatedTime() != null ? customer.getUpdatedTime().format(DATE_FORMATTER) : ""
        );
        
        if (fields != null && fields.length > 0) {
            return Arrays.stream(fields)
                    .map(field -> getCustomerValueByField(customer, field))
                    .collect(Collectors.toList());
        }
        
        return allValues;
    }

    /**
     * 根据字段名获取客户表头
     */
    private String getCustomerHeaderByField(String field) {
        switch (field) {
            case "id": return "客户ID";
            case "customerCode": return "客户编码";
            case "customerName": return "客户名称";
            case "contactPerson": return "联系人";
            case "email": return "邮箱";
            case "phone": return "电话";
            case "address": return "地址";
            case "status": return "状态";
            case "type": return "类型";
            case "remark": return "备注";
            case "lastLoginTime": return "最后登录时间";
            case "createdTime": return "创建时间";
            case "updatedTime": return "更新时间";
            default: return field;
        }
    }

    /**
     * 根据字段名获取客户数据值
     */
    private String getCustomerValueByField(Customer customer, String field) {
        switch (field) {
            case "id": return String.valueOf(customer.getId());
            case "customerCode": return customer.getCustomerCode();
            case "customerName": return customer.getCustomerName();
            case "contactPerson": return customer.getContactPerson();
            case "email": return customer.getEmail();
            case "phone": return customer.getPhone();
            case "address": return customer.getAddress();
            case "status": return customer.getStatus() != null ? customer.getStatus().getDescription() : "";
            case "type": return customer.getType() != null ? customer.getType().getDescription() : "";
            case "remark": return customer.getRemark();
            case "lastLoginTime": return customer.getLastLoginTime() != null ? customer.getLastLoginTime().format(DATE_FORMATTER) : "";
            case "createdTime": return customer.getCreatedTime() != null ? customer.getCreatedTime().format(DATE_FORMATTER) : "";
            case "updatedTime": return customer.getUpdatedTime() != null ? customer.getUpdatedTime().format(DATE_FORMATTER) : "";
            default: return "";
        }
    }

    // 类似的方法用于License和ValidationLog的导出...
    // 由于篇幅限制，这里仅展示框架，实际使用时需要完整实现

    private void createLicenseHeader(Row row, CellStyle style, String[] fields) {
        // 实现授权表头创建
    }

    private void fillLicenseData(Row row, License license, CellStyle style, String[] fields) {
        // 实现授权数据填充
    }

    private void createValidationLogHeader(Row row, CellStyle style, String[] fields) {
        // 实现验证日志表头创建
    }

    private void fillValidationLogData(Row row, LicenseValidationLog log, CellStyle style, String[] fields) {
        // 实现验证日志数据填充
    }

    private List<String> getLicenseHeaders(String[] fields) {
        return Arrays.asList("授权ID", "授权编码", "客户ID", "产品名称", "产品版本", "授权类型", 
                           "状态", "开始时间", "到期时间", "最大用户数", "价格", "创建时间");
    }

    private List<String> getLicenseValues(License license, String[] fields) {
        return Arrays.asList(
                String.valueOf(license.getId()),
                license.getLicenseCode(),
                String.valueOf(license.getCustomerId()),
                license.getProductName(),
                license.getProductVersion(),
                license.getLicenseType().getDescription(),
                license.getStatus().getDescription(),
                license.getStartTime().format(DATE_FORMATTER),
                license.getExpireTime().format(DATE_FORMATTER),
                String.valueOf(license.getMaxUsers()),
                license.getPrice() != null ? license.getPrice().toString() : "",
                license.getCreatedTime().format(DATE_FORMATTER)
        );
    }

    private int getCustomerFieldCount(String[] fields) {
        return fields != null && fields.length > 0 ? fields.length : 13;
    }

    private int getLicenseFieldCount(String[] fields) {
        return fields != null && fields.length > 0 ? fields.length : 12;
    }

    private int getValidationLogFieldCount(String[] fields) {
        return fields != null && fields.length > 0 ? fields.length : 8;
    }
}