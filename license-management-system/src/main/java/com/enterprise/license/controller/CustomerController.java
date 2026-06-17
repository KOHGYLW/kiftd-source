package com.enterprise.license.controller;

import com.enterprise.license.dto.ApiResponse;
import com.enterprise.license.dto.PageResponse;
import com.enterprise.license.dto.customer.*;
import com.enterprise.license.service.CustomerService;
import com.enterprise.license.util.Constants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 客户管理控制器
 */
@RestController
@RequestMapping(Constants.ApiPaths.CUSTOMERS)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "客户管理", description = "客户管理相关接口")
@PreAuthorize("isAuthenticated()")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @Operation(summary = "创建客户", description = "创建新的客户记录")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "201", description = "创建成功"),
        @SwaggerApiResponse(responseCode = "400", description = "请求参数错误"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证"),
        @SwaggerApiResponse(responseCode = "409", description = "客户编码已存在")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @Valid @RequestBody CustomerCreateRequest request,
            Authentication authentication) {
        
        log.info("创建客户: {}, 操作用户: {}", request.getName(), authentication.getName());
        
        CustomerResponse response = customerService.createCustomer(request, authentication.getName());
        
        log.info("客户创建成功: {}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("客户创建成功", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取客户详情", description = "根据ID获取客户详细信息")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "获取成功"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证"),
        @SwaggerApiResponse(responseCode = "404", description = "客户不存在")
    })
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomer(
            @Parameter(description = "客户ID", required = true)
            @PathVariable Long id) {
        
        log.info("获取客户详情: {}", id);
        
        CustomerResponse response = customerService.getCustomerById(id);
        
        return ResponseEntity.ok(ApiResponse.success("获取客户详情成功", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新客户", description = "更新客户信息")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "更新成功"),
        @SwaggerApiResponse(responseCode = "400", description = "请求参数错误"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证"),
        @SwaggerApiResponse(responseCode = "403", description = "权限不足"),
        @SwaggerApiResponse(responseCode = "404", description = "客户不存在")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @Parameter(description = "客户ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody CustomerUpdateRequest request,
            Authentication authentication) {
        
        log.info("更新客户: {}, 操作用户: {}", id, authentication.getName());
        
        CustomerResponse response = customerService.updateCustomer(id, request, authentication.getName());
        
        log.info("客户更新成功: {}", id);
        return ResponseEntity.ok(ApiResponse.success("客户更新成功", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除客户", description = "删除客户记录")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "删除成功"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证"),
        @SwaggerApiResponse(responseCode = "403", description = "权限不足"),
        @SwaggerApiResponse(responseCode = "404", description = "客户不存在"),
        @SwaggerApiResponse(responseCode = "409", description = "客户存在关联数据，无法删除")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(
            @Parameter(description = "客户ID", required = true)
            @PathVariable Long id,
            Authentication authentication) {
        
        log.info("删除客户: {}, 操作用户: {}", id, authentication.getName());
        
        customerService.deleteCustomer(id, authentication.getName());
        
        log.info("客户删除成功: {}", id);
        return ResponseEntity.ok(ApiResponse.success("客户删除成功"));
    }

    @GetMapping
    @Operation(summary = "搜索客户", description = "根据条件搜索客户列表")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "搜索成功"),
        @SwaggerApiResponse(responseCode = "400", description = "请求参数错误"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证")
    })
    public ResponseEntity<ApiResponse<PageResponse<CustomerResponse>>> searchCustomers(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "客户类型") @RequestParam(required = false) String type,
            @Parameter(description = "客户状态") @RequestParam(required = false) String status,
            @Parameter(description = "行业") @RequestParam(required = false) String industry,
            @Parameter(description = "负责用户ID") @RequestParam(required = false) Long assignedUserId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") String sortField,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortDirection) {
        
        log.info("搜索客户: keyword={}, type={}, status={}, page={}, size={}", 
                keyword, type, status, page, size);
        
        CustomerSearchRequest searchRequest = new CustomerSearchRequest();
        searchRequest.setKeyword(keyword);
        searchRequest.setType(type);
        searchRequest.setStatus(status);
        searchRequest.setIndustry(industry);
        searchRequest.setAssignedUserId(assignedUserId);
        searchRequest.setPage(page);
        searchRequest.setSize(size);
        searchRequest.setSortField(sortField);
        searchRequest.setSortDirection(sortDirection);
        
        PageResponse<CustomerResponse> response = customerService.searchCustomers(searchRequest);
        
        return ResponseEntity.ok(ApiResponse.success("搜索客户成功", response));
    }

    @PostMapping("/batch")
    @Operation(summary = "批量操作客户", description = "对多个客户执行批量操作")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "操作成功"),
        @SwaggerApiResponse(responseCode = "400", description = "请求参数错误"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证"),
        @SwaggerApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<BatchOperationResult>> batchOperateCustomers(
            @Valid @RequestBody CustomerBatchRequest request,
            Authentication authentication) {
        
        log.info("批量操作客户: operation={}, count={}, 操作用户: {}", 
                request.getOperation(), request.getCustomerIds().size(), authentication.getName());
        
        BatchOperationResult result = customerService.batchOperateCustomers(request, authentication.getName());
        
        log.info("批量操作完成: 成功={}, 失败={}", result.getSuccessCount(), result.getFailureCount());
        return ResponseEntity.ok(ApiResponse.success("批量操作完成", result));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新客户状态", description = "更新客户的状态")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "状态更新成功"),
        @SwaggerApiResponse(responseCode = "400", description = "请求参数错误"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证"),
        @SwaggerApiResponse(responseCode = "403", description = "权限不足"),
        @SwaggerApiResponse(responseCode = "404", description = "客户不存在")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomerStatus(
            @Parameter(description = "客户ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "新状态", required = true)
            @RequestParam String status,
            @Parameter(description = "状态更新原因")
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        
        log.info("更新客户状态: id={}, status={}, 操作用户: {}", id, status, authentication.getName());
        
        CustomerResponse response = customerService.updateCustomerStatus(id, status, reason, authentication.getName());
        
        log.info("客户状态更新成功: {}", id);
        return ResponseEntity.ok(ApiResponse.success("客户状态更新成功", response));
    }

    @PutMapping("/{id}/assign")
    @Operation(summary = "分配客户", description = "将客户分配给指定用户负责")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "分配成功"),
        @SwaggerApiResponse(responseCode = "400", description = "请求参数错误"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证"),
        @SwaggerApiResponse(responseCode = "403", description = "权限不足"),
        @SwaggerApiResponse(responseCode = "404", description = "客户或用户不存在")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<CustomerResponse>> assignCustomer(
            @Parameter(description = "客户ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "负责用户ID", required = true)
            @RequestParam Long assignedUserId,
            Authentication authentication) {
        
        log.info("分配客户: id={}, assignedUserId={}, 操作用户: {}", 
                id, assignedUserId, authentication.getName());
        
        CustomerResponse response = customerService.assignCustomer(id, assignedUserId, authentication.getName());
        
        log.info("客户分配成功: {}", id);
        return ResponseEntity.ok(ApiResponse.success("客户分配成功", response));
    }

    @GetMapping("/{id}/licenses")
    @Operation(summary = "获取客户授权列表", description = "获取指定客户的所有授权")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "获取成功"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证"),
        @SwaggerApiResponse(responseCode = "404", description = "客户不存在")
    })
    public ResponseEntity<ApiResponse<List<CustomerLicenseInfo>>> getCustomerLicenses(
            @Parameter(description = "客户ID", required = true)
            @PathVariable Long id) {
        
        log.info("获取客户授权列表: {}", id);
        
        List<CustomerLicenseInfo> licenses = customerService.getCustomerLicenses(id);
        
        return ResponseEntity.ok(ApiResponse.success("获取客户授权列表成功", licenses));
    }

    @PostMapping("/{id}/contact")
    @Operation(summary = "更新联系记录", description = "更新客户的最后联系时间")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "更新成功"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证"),
        @SwaggerApiResponse(responseCode = "404", description = "客户不存在")
    })
    public ResponseEntity<ApiResponse<Void>> updateContactRecord(
            @Parameter(description = "客户ID", required = true)
            @PathVariable Long id,
            Authentication authentication) {
        
        log.info("更新客户联系记录: id={}, 操作用户: {}", id, authentication.getName());
        
        customerService.updateContactRecord(id, authentication.getName());
        
        return ResponseEntity.ok(ApiResponse.success("联系记录更新成功"));
    }

}