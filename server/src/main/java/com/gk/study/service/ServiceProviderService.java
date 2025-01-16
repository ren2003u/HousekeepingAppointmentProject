package com.gk.study.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gk.study.entity.ServiceProvider;

public interface ServiceProviderService extends IService<ServiceProvider> {
    ServiceProvider getServiceProviderByUserId(Long userId);
    void createServiceProvider(ServiceProvider serviceProvider);
    void updateServiceProvider(ServiceProvider serviceProvider);
    void deleteServiceProvider(Long id);
}
