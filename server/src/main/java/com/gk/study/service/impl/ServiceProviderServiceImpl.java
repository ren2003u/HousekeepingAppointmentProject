package com.gk.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gk.study.entity.ServiceProvider;
import com.gk.study.mapper.ServiceProviderMapper;
import com.gk.study.service.ServiceProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

@Service
public class ServiceProviderServiceImpl extends ServiceImpl<ServiceProviderMapper, ServiceProvider>
        implements ServiceProviderService {

    @Autowired
    private ServiceProviderMapper serviceProviderMapper;

    @Override
    public ServiceProvider getServiceProviderByUserId(Long userId) {
        return serviceProviderMapper.selectOne(new QueryWrapper<ServiceProvider>().eq("user_id", userId));
    }

    @Override
    public void createServiceProvider(ServiceProvider serviceProvider) {
        serviceProviderMapper.insert(serviceProvider);
    }

    @Override
    public void updateServiceProvider(ServiceProvider serviceProvider) {
        serviceProviderMapper.updateById(serviceProvider);
    }

    @Override
    public void deleteServiceProvider(Long id) {
        serviceProviderMapper.deleteById(id);
    }
}

