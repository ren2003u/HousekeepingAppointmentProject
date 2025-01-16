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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/serviceProvider")
public class ServiceProviderController {

    private final static Logger logger = LoggerFactory.getLogger(ServiceProviderController.class);

    @Autowired
    ServiceProviderService serviceProviderService;

    @Autowired
    UserService userService;

    /**
     * 服务提供者注册（关联用户）
     */
    @PostMapping("/register")
    @Transactional
    public APIResponse register(@RequestBody ServiceProviderRegisterRequest request) {
        logger.info("Registering service provider for user ID: {}", request.getUserId());

        // 检查用户是否存在且是普通用户
        User user = userService.getUserDetail(request.getUserId().toString());
        if(user == null || user.getRole() != 1){
            return new APIResponse(ResponeCode.FAIL, "用户不存在或非普通用户");
        }

        // 检查是否已注册为服务提供者
        ServiceProvider existingProvider = serviceProviderService.getServiceProviderByUserId(user.getId());
        if(existingProvider != null){
            return new APIResponse(ResponeCode.FAIL, "用户已注册为服务提供者");
        }

        // 创建服务提供者记录
        ServiceProvider provider = new ServiceProvider();
        provider.setUserId(user.getId());
        provider.setName(request.getName());
        provider.setAvatar(request.getAvatar());
        provider.setDescription(request.getDescription());
        provider.setRating(BigDecimal.valueOf(0.00));
        provider.setStatus("1"); // 可用
        provider.setCreateTime(System.currentTimeMillis());

        serviceProviderService.createServiceProvider(provider);

        // 更新用户角色
        user.setRole(2); // 2 = 服务提供者
        userService.updateUser(user);

        return new APIResponse(ResponeCode.SUCCESS, "服务提供者注册成功");
    }

    /**
     * 服务提供者信息更新
     */
    @Access(level = AccessLevel.LOGIN)
    @PostMapping("/update")
    @Transactional
    public APIResponse update(@RequestBody ServiceProviderUpdateRequest request) {
        logger.info("Updating service provider info for provider ID: {}", request.getId());

        ServiceProvider provider = serviceProviderService.getById(request.getId());
        if(provider == null){
            return new APIResponse(ResponeCode.FAIL, "服务提供者不存在");
        }

        provider.setName(request.getName());
        provider.setAvatar(request.getAvatar());
        provider.setDescription(request.getDescription());
        // 其他可更新字段...

        serviceProviderService.updateServiceProvider(provider);

        return new APIResponse(ResponeCode.SUCCESS, "服务提供者信息更新成功");
    }

    /**
     * 获取服务提供者详情
     */
    @GetMapping("/detail")
    public APIResponse detail(@RequestParam Long id){
        logger.info("Fetching detail for service provider ID: {}", id);
        ServiceProvider provider = serviceProviderService.getById(id);
        if(provider == null){
            return new APIResponse(ResponeCode.FAIL, "服务提供者不存在");
        }
        return new APIResponse(ResponeCode.SUCCESS, "查询成功", provider);
    }

    /**
     * 获取所有服务提供者列表（可分页）
     */
    @GetMapping("/list")
    public APIResponse list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort, // 如: ratingDesc, recent
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size
    ){
        logger.info("Listing service providers with filters - keyword: {}, sort: {}, page: {}, size: {}", keyword, sort, page, size);
        Page<ServiceProvider> providerPage = new Page<>(page, size);
        QueryWrapper<ServiceProvider> queryWrapper = new QueryWrapper<>();

        if(StringUtils.isNotBlank(keyword)){
            queryWrapper.like("name", keyword).or().like("description", keyword);
        }

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

        IPage<ServiceProvider> resultPage = serviceProviderService.page(providerPage, queryWrapper);
        return new APIResponse(ResponeCode.SUCCESS, "查询成功", resultPage);
    }
}
