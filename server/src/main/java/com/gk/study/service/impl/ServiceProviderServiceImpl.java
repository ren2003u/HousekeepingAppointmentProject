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
public class ServiceProviderServiceImpl extends ServiceImpl<ServiceProviderMapper, ServiceProvider> implements ServiceProviderService {
    @Autowired
    ServiceProviderMapper serviceProviderMapper;

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

    @Override
    public boolean saveBatch(Collection<ServiceProvider> entityList, int batchSize) {
        return false;
    }

    @Override
    public boolean saveOrUpdateBatch(Collection<ServiceProvider> entityList, int batchSize) {
        return false;
    }

    @Override
    public boolean updateBatchById(Collection<ServiceProvider> entityList, int batchSize) {
        return false;
    }

    @Override
    public boolean saveOrUpdate(ServiceProvider entity) {
        return false;
    }

    @Override
    public ServiceProvider getOne(Wrapper<ServiceProvider> queryWrapper, boolean throwEx) {
        return null;
    }

    @Override
    public Map<String, Object> getMap(Wrapper<ServiceProvider> queryWrapper) {
        return Map.of();
    }

    @Override
    public <V> V getObj(Wrapper<ServiceProvider> queryWrapper, Function<? super Object, V> mapper) {
        return null;
    }

    @Override
    public ServiceProviderMapper getBaseMapper() {
        return null;
    }

    @Override
    public Class<ServiceProvider> getEntityClass() {
        return null;
    }
}
