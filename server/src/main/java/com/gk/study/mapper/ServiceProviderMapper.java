package com.gk.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gk.study.entity.ServiceProvider;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ServiceProviderMapper extends BaseMapper<ServiceProvider> {
    // 可以根据需要自定义查询方法
    // 例如，根据 user_id 查询服务提供商
}
