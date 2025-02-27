package com.gk.study.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gk.study.common.APIResponse;
import com.gk.study.common.ResponeCode;
import com.gk.study.entity.ServiceProvider;
import com.gk.study.entity.ServiceProviderRegisterRequest;
import com.gk.study.entity.ServiceProviderUpdateRequest;
import com.gk.study.entity.User;
import com.gk.study.permission.Access;
import com.gk.study.permission.AccessLevel;
import com.gk.study.service.ServiceProviderService;
import com.gk.study.service.UserService;
import com.gk.study.userenum.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Parameter;
import java.math.BigDecimal;

@RestController
@RequestMapping("/serviceProvider")
public class ServiceProviderController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceProviderController.class);

    @Autowired
    private ServiceProviderService serviceProviderService;

    @Autowired
    private UserService userService;

    /**
     * 服务提供者注册（关联用户）
     */
    @Operation(
            summary = "服务提供者注册",
            description = "为普通用户注册服务提供者，成功后用户角色变更为服务提供者。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "服务提供者注册成功"),
                    @ApiResponse(responseCode = "400", description = "用户不存在或非普通用户")
            }
    )
    @PostMapping("/register")
    @Transactional
    public ResponseEntity<APIResponse<?>> register(@Parameter(description = "服务提供者注册请求数据", required = true) @RequestBody ServiceProviderRegisterRequest request) {
        logger.info("Registering service provider for user ID: {}", request.getUserId());

        // 1. 检查用户是否存在且是普通用户 (role=1)
        User user = userService.getUserDetail(request.getUserId().toString());
        if (user == null || user.getRole() != UserRole.NORMAL_USER.getCode()) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "用户不存在或非普通用户")
            );
        }

        // 2. 检查是否已注册为服务提供者
        ServiceProvider existingProvider = serviceProviderService.getServiceProviderByUserId(user.getId());
        if (existingProvider != null) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "用户已注册为服务提供者")
            );
        }

        // 3. 创建服务提供者记录
        ServiceProvider provider = new ServiceProvider();
        provider.setUserId(user.getId());
        provider.setName(request.getName());
        provider.setAvatar(request.getAvatar());
        provider.setDescription(request.getDescription());
        provider.setRating(BigDecimal.valueOf(0.00));
        provider.setStatus("1"); // 可用
        provider.setCreateTime(System.currentTimeMillis());

        serviceProviderService.createServiceProvider(provider);

        // 4. 更新用户角色 (1 = 服务提供者)
        user.setRole(UserRole.SERVICE_PROVIDER.getCode());
        userService.updateUser(user);

        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "服务提供者注册成功",request.getName())
        );
    }

    /**
     * 服务提供者信息更新
     */
    @Operation(
            summary = "更新服务提供者信息",
            description = "服务提供者更新自己的信息，包括名称、头像、描述等。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "服务提供者信息更新成功"),
                    @ApiResponse(responseCode = "404", description = "服务提供者不存在")
            }
    )
    @Access(level = AccessLevel.LOGIN)
    @PostMapping("/update")
    @Transactional
    public ResponseEntity<APIResponse<?>> update(@Parameter(description = "服务提供者更新请求数据", required = true) @RequestBody ServiceProviderUpdateRequest request) {
        logger.info("Updating service provider info for provider ID: {}", request.getId());

        ServiceProvider provider = serviceProviderService.getById(request.getId());
        if (provider == null) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "服务提供者不存在")
            );
        }

        provider.setName(request.getName());
        provider.setAvatar(request.getAvatar());
        provider.setDescription(request.getDescription());


        serviceProviderService.updateServiceProvider(provider);

        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "服务提供者信息更新成功")
        );
    }

    /**
     * 获取服务提供者详情
     */
    @Operation(
            summary = "获取服务提供者详情",
            description = "根据服务提供者ID查询服务提供者的详细信息。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功"),
                    @ApiResponse(responseCode = "404", description = "服务提供者不存在")
            }
    )
    @GetMapping("/detail")
    public ResponseEntity<APIResponse<?>> detail(@Parameter(description = "服务提供者ID", required = true) @RequestParam Long id){
        logger.info("Fetching detail for service provider ID: {}", id);
        ServiceProvider provider = serviceProviderService.getById(id);
        if (provider == null) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "服务提供者不存在")
            );
        }
        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "查询成功", provider)
        );
    }

    /**
     * 获取所有服务提供者列表（可分页）
     */
    @Operation(
            summary = "获取服务提供者列表",
            description = "根据筛选条件（关键词、排序、分页）查询服务提供者列表。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功"),
                    @ApiResponse(responseCode = "404", description = "没有符合条件的服务提供者")
            }
    )
    @GetMapping("/list")
    public ResponseEntity<APIResponse<?>> list(
            @Parameter(description = "搜索关键词", required = false) @RequestParam(required = false) String keyword,
            @Parameter(description = "排序方式，如：ratingDesc, recent", required = false) @RequestParam(required = false) String sort,
            @Parameter(description = "当前页码", required = false) @RequestParam(required = false, defaultValue = "1") int page,
            @Parameter(description = "每页记录数", required = false) @RequestParam(required = false, defaultValue = "10") int size
    ){
        logger.info("Listing service providers with filters - keyword: {}, sort: {}, page: {}, size: {}",
                keyword, sort, page, size);

        Page<ServiceProvider> providerPage = new Page<>(page, size);
        QueryWrapper<ServiceProvider> queryWrapper = new QueryWrapper<>();

        // 1. 关键字搜索
        if(StringUtils.isNotBlank(keyword)){
            queryWrapper.lambda()
                    .like(ServiceProvider::getName, keyword)
                    .or()
                    .like(ServiceProvider::getDescription, keyword);
        }

        // 2. 排序
        if(StringUtils.isNotBlank(sort)){
            switch(sort){
                case "ratingDesc":
                    queryWrapper.orderByDesc("rating");
                    break;
                case "recent":
                    queryWrapper.orderByDesc("create_time");
                    break;
                default:
                    queryWrapper.orderByDesc("create_time");
            }
        } else {
            queryWrapper.orderByDesc("create_time");
        }

        // 3. 分页查询
        IPage<ServiceProvider> resultPage = serviceProviderService.page(providerPage, queryWrapper);
        // 如果没有查到任何记录
        if(resultPage.getRecords() == null || resultPage.getRecords().isEmpty()){
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.SUCCESS, "暂无服务提供者信息", resultPage)
            );
        }

        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "查询成功", resultPage)
        );
    }
}