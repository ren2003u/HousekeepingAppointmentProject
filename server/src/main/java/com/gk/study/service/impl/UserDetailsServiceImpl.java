package com.gk.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.study.entity.User;
import com.gk.study.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserMapper userMapper; // 直接操作DB, 或者复用你已有的UserService

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 这里的username可以是账号、手机号、邮箱(看你怎么设计)
        // 简单示例：先按username字段查
        User user = userMapper.selectOne(
                new QueryWrapper<User>().eq("username", username)
        );
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        // 返回一个Spring Security内置的UserDetails对象，也可自定义
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                getAuthorities(user.getRole())  // 根据role返回权限列表
        );
    }

    // 根据自定义role返回权限
    private Collection<? extends GrantedAuthority> getAuthorities(Integer role) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (role != null && role > 1) {
            // 角色>1代表管理员
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        return authorities;
    }
}